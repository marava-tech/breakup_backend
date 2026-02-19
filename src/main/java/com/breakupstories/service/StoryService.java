package com.breakupstories.service;

import com.breakupstories.dto.LikeRequest;
import com.breakupstories.dto.LikeResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.dto.StoryResponse;
import com.breakupstories.dto.CommentResponse;
import com.breakupstories.dto.StorySearchRequest;
import com.breakupstories.dto.StorySearchResponse;
import com.breakupstories.dto.StoryWithTrendingScore;
import com.breakupstories.dto.WithdrawalEligibilityResponse;

import com.breakupstories.model.Story;
import com.breakupstories.model.StoryDataStore;
import com.breakupstories.model.StoryMetadata;
import com.breakupstories.model.User;
import com.breakupstories.repository.StoryRepository;
import com.breakupstories.repository.StoryDataStoreRepository;

// import com.breakupstories.util.ApplicationContextProvider;
import com.breakupstories.util.RequestContext;
import com.breakupstories.util.ListUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.breakupstories.util.TimestampUtil;
import com.breakupstories.dto.WrittenStoryRequest;
import java.util.*;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoryService {

    private final StoryRepository storyRepository;
    private final StoryDataStoreRepository storyDataStoreRepository;
    private final MongoTemplate mongoTemplate;
    private final LikeService likeService;
    private final CommentService commentService;
    @Lazy
    private final UserService userService;
    @Lazy
    private final TrendingCacheService trendingCacheService;
    @Lazy
    private final BookmarkService bookmarkService;
    private final ViewCountBatchService viewCountBatchService;
    private final StoryDataStoreService storyDataStoreService;
    private final DefaultConfigService defaultConfigService;
    private final RecommendationService recommendationService;
    private final CuratedFeedService curatedFeedService;

    public StoryResponse createStory(User user, MultipartHttpServletRequest request, Map<String, String> uploadMetadata,
            String creationType) {
        String requestId = RequestContext.getRequestId();
        String userId = user.getId();
        log.info("Creating story for user: {} [RequestID: {}]", userId, requestId);

        try {
            // Step 0: Check story creation eligibility
            defaultConfigService.checkStoryCreationEligibility(userId);

            // Step 1: Validate audio file
            MultipartFile audioFile = request.getFile("audio");
            if (audioFile == null || audioFile.isEmpty()) {
                throw new IllegalArgumentException("Audio file is required");
            }

            log.info("Starting story creation for user: {} with file: {} ({} bytes) [RequestID: {}]",
                    userId, audioFile.getOriginalFilename(), audioFile.getSize(), requestId);

            // Step 2: Store audio file temporarily for background processing
            // In a real implementation, you might store this in a temporary storage service
            // For now, we'll store the file bytes in the uploadMetadata
            if (uploadMetadata == null) {
                uploadMetadata = new HashMap<>();
            }

            // Store file information for background processing
            uploadMetadata.put("audioFileName", audioFile.getOriginalFilename());
            uploadMetadata.put("audioFileSize", String.valueOf(audioFile.getSize()));
            uploadMetadata.put("audioContentType", audioFile.getContentType());

            // Store file bytes as base64 (for demo purposes - in production, use proper
            // file storage)
            try {
                byte[] fileBytes = audioFile.getBytes();
                String base64File = java.util.Base64.getEncoder().encodeToString(fileBytes);
                uploadMetadata.put("audioFileData", base64File);
                log.info("Audio file stored temporarily for background processing [RequestID: {}]", requestId);
            } catch (Exception e) {
                log.error("Failed to store audio file for background processing [RequestID: {}]: {}", requestId,
                        e.getMessage());
                throw new RuntimeException("Failed to store audio file: " + e.getMessage(), e);
            }

            // Determine creation type
            Story.CreationType storyCreationType = Story.CreationType.UPLOADED; // Default
            if (creationType != null && !creationType.trim().isEmpty()) {
                try {
                    storyCreationType = Story.CreationType.valueOf(creationType.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid creation type provided: {}. Using default UPLOADED [RequestID: {}]", creationType,
                            requestId);
                }
            }

            // Determine category
            Story.Category storyCategory = null;
            String categoryStr = uploadMetadata.get("category");
            if (categoryStr != null && !categoryStr.trim().isEmpty()) {
                try {
                    storyCategory = Story.Category.valueOf(categoryStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid category provided: {}. [RequestID: {}]", categoryStr, requestId);
                }
            }

            // Store creation type in upload metadata
            uploadMetadata.put("creationType", storyCreationType.name());

            // Step 3: Create initial Story with UPLOAD_PENDING status
            Story story = Story.builder()
                    .userId(userId)
                    .title("Analyzing....") // Will be updated after audio upload and AI processing
                    .audioUrl(null) // Will be set after async upload
                    .coverImageUrl(defaultConfigService.getDefaultThumbnailUrl())
                    .storyImages(defaultConfigService.getDefaultStoryImages())
                    .viewCount(0L)
                    .status(Story.StoryStatus.UPLOAD_PENDING) // Initial status
                    .contents(new ArrayList<>()) // Will be populated after AI processing
                    .rejectionReasons(new ArrayList<>()) // Will be populated if AI processing fails
                    .creationType(storyCreationType) // Use determined creation type
                    .category(storyCategory) // Set category
                    .language(user.getPreferredStoryLanguage()) // Set language from user preference
                    .build();

            Story savedStory = storyRepository.save(story);
            String storyId = savedStory.getId();
            log.info("Initial story created with ID: {} for user: {} [RequestID: {}]", storyId, userId, requestId);

            // Step 4: Create StoryDataStore with the same ID and UPLOAD_PENDING status
            StoryDataStore dataStore = StoryDataStore.builder()
                    .id(storyId)
                    .storyId(storyId)
                    .userId(userId)
                    .language(user.getPreferredStoryLanguage())
                    .processingStatus(StoryDataStore.ProcessingStatus.UPLOAD_PENDING)
                    .uploadMetadata(uploadMetadata)
                    .createdAt(TimestampUtil.currentLocalDateTime())
                    .updatedAt(TimestampUtil.currentLocalDateTime())
                    .build();

            StoryDataStore savedDataStore = storyDataStoreRepository.save(dataStore);
            log.info("StoryDataStore created with ID: {} for user: {} [RequestID: {}]",
                    savedDataStore.getId(), userId, requestId);

            return StoryResponse.fromStory(savedStory, user);

        } catch (Exception e) {
            log.error("Error creating story for user {}: {} [RequestID: {}]", userId, e.getMessage(), requestId, e);
            throw new RuntimeException("Failed to create story: " + e.getMessage(), e);
        }
    }

    public StoryResponse createWrittenStory(User user, WrittenStoryRequest request,
            Map<String, String> uploadMetadata) {
        String requestId = RequestContext.getRequestId();
        String userId = user.getId();
        log.info("Creating written story for user: {} [RequestID: {}]", userId, requestId);

        try {
            // Step 0: Check story creation eligibility
            defaultConfigService.checkStoryCreationEligibility(userId);

            // Step 1: Validate request
            if (request.getStoryText() == null || request.getStoryText().trim().isEmpty()) {
                throw new IllegalArgumentException("Story text is required");
            }

            if (request.getLanguage() == null || request.getLanguage().trim().isEmpty()) {
                throw new IllegalArgumentException("Language is required");
            }

            log.info("Starting written story creation for user: {} with text length: {} [RequestID: {}]",
                    userId, request.getStoryText().length(), requestId);

            // Step 2: Store story text and metadata for background processing
            if (uploadMetadata == null) {
                uploadMetadata = new HashMap<>();
            }

            // Store story text and metadata for background processing
            uploadMetadata.put("storyText", request.getStoryText());
            uploadMetadata.put("storyLanguage", request.getLanguage());
            uploadMetadata.put("creationType", "WRITTEN");

            // Step 3: Create initial Story with PROCESSING_PENDING status (no upload
            // needed)
            // Determine category
            Story.Category storyCategory = null;
            if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
                try {
                    storyCategory = Story.Category.valueOf(request.getCategory().toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid category provided: {}. [RequestID: {}]", request.getCategory(), requestId);
                }
            }

            // Step 3: Create initial Story with PROCESSING_PENDING status (no upload
            // needed)
            Story story = Story.builder()
                    .userId(userId)
                    .title("Processing....") // Will be updated after AI processing
                    .audioUrl(null) // Will be set after audio generation
                    .coverImageUrl(defaultConfigService.getDefaultThumbnailUrl())
                    .storyImages(defaultConfigService.getDefaultStoryImages())
                    .viewCount(0L)
                    .status(Story.StoryStatus.PROCESSING_PENDING) // Skip upload step
                    .contents(new ArrayList<>()) // Will be populated after AI processing
                    .rejectionReasons(new ArrayList<>()) // Will be populated if AI processing fails
                    .creationType(Story.CreationType.WRITTEN)
                    .language(request.getLanguage())
                    .category(storyCategory)
                    .build();

            Story savedStory = storyRepository.save(story);
            String storyId = savedStory.getId();
            log.info("Initial written story created with ID: {} for user: {} [RequestID: {}]", storyId, userId,
                    requestId);

            // Step 5: Create StoryDataStore with PROCESSING_PENDING status and
            // transcription response
            StoryDataStore dataStore = StoryDataStore.builder()
                    .id(storyId)
                    .storyId(storyId)
                    .userId(userId)
                    .language(request.getLanguage())
                    .processingStatus(StoryDataStore.ProcessingStatus.PROCESSING_PENDING) // Skip upload step
                    .uploadMetadata(uploadMetadata)
                    .transcriptionCompletedAt(TimestampUtil.currentLocalDateTime()) // Mark transcription as completed
                    .createdAt(TimestampUtil.currentLocalDateTime())
                    .updatedAt(TimestampUtil.currentLocalDateTime())
                    .build();

            StoryDataStore savedDataStore = storyDataStoreRepository.save(dataStore);
            log.info("StoryDataStore created for written story with ID: {} for user: {} [RequestID: {}]",
                    savedDataStore.getId(), userId, requestId);

            return StoryResponse.fromStory(savedStory, user);

        } catch (Exception e) {
            log.error("Error creating written story for user {}: {} [RequestID: {}]", userId, e.getMessage(), requestId,
                    e);
            throw new RuntimeException("Failed to create written story: " + e.getMessage(), e);
        }
    }

    public PagedResponse<StoryResponse> getStories(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> storyPage = storyRepository.findAll(pageable);

        List<StoryResponse> stories = storyPage.getContent().stream()
                .map(story -> {
                    User user = userService.getUserEntityById(story.getUserId());
                    long likeCount = getLikeCount(story.getId());
                    long commentCount = getCommentCount(story.getId());
                    return StoryResponse.fromStory(story, user, false, false, likeCount, commentCount);
                })
                .collect(Collectors.toList());

        return PagedResponse.of(stories, page, size, storyPage.getTotalElements());
    }

    /**
     * Get trending stories sorted by trending score (for unauthenticated users).
     * Uses precomputed cache from TrendingCacheService to avoid full collection
     * scan.
     */
    public PagedResponse<StoryResponse> getTrendingStories(int page, int size) {
        String requestId = RequestContext.getRequestId();
        log.debug("Getting trending stories for unauthenticated user [RequestID: {}]", requestId);

        try {
            List<String> ids = trendingCacheService.getTrendingStoryIds();
            if (ids == null || ids.isEmpty()) {
                trendingCacheService.refreshTrendingCache();
                ids = trendingCacheService.getTrendingStoryIds();
            }
            if (ids == null || ids.isEmpty()) {
                return getTrendingStoriesFallback(page, size, null);
            }

            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, ids.size());
            if (startIndex >= ids.size()) {
                return PagedResponse.of(List.of(), page, size, ids.size());
            }
            List<String> pageIds = ids.subList(startIndex, endIndex);
            List<Story> stories = storyRepository.findByIdInAndStatus(pageIds, Story.StoryStatus.ACTIVE);
            Map<String, Story> byId = stories.stream().collect(Collectors.toMap(Story::getId, s -> s));
            List<Story> ordered = pageIds.stream().map(byId::get).filter(Objects::nonNull).toList();

            List<StoryResponse> responses = ordered.stream()
                    .map(story -> {
                        User user = userService.getUserEntityById(story.getUserId());
                        long likeCount = getLikeCount(story.getId());
                        long commentCount = getCommentCount(story.getId());
                        return StoryResponse.fromStory(story, user, false, false, likeCount, commentCount);
                    })
                    .collect(Collectors.toList());

            return PagedResponse.of(responses, page, size, ids.size());
        } catch (Exception e) {
            log.warn("Trending cache read failed, falling back to full scan [RequestID: {}]: {}", requestId,
                    e.getMessage());
            return getTrendingStoriesFallback(page, size, null);
        }
    }

    /**
     * Get trending stories sorted by trending score (for authenticated users).
     * Uses precomputed cache from TrendingCacheService.
     */
    public PagedResponse<StoryResponse> getTrendingStories(String currentUserId, int page, int size) {
        String requestId = RequestContext.getRequestId();
        log.debug("Getting trending stories for user: {} [RequestID: {}]", currentUserId, requestId);

        try {
            List<String> ids = trendingCacheService.getTrendingStoryIds();
            if (ids == null || ids.isEmpty()) {
                trendingCacheService.refreshTrendingCache();
                ids = trendingCacheService.getTrendingStoryIds();
            }
            if (ids == null || ids.isEmpty()) {
                return getTrendingStoriesFallback(page, size, currentUserId);
            }

            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, ids.size());
            if (startIndex >= ids.size()) {
                return PagedResponse.of(List.of(), page, size, ids.size());
            }
            List<String> pageIds = ids.subList(startIndex, endIndex);
            List<Story> stories = storyRepository.findByIdInAndStatus(pageIds, Story.StoryStatus.ACTIVE);
            Map<String, Story> byId = stories.stream().collect(Collectors.toMap(Story::getId, s -> s));
            List<Story> ordered = pageIds.stream().map(byId::get).filter(Objects::nonNull).toList();

            List<StoryResponse> responses = ordered.stream()
                    .map(story -> {
                        User user = userService.getUserEntityById(story.getUserId());
                        boolean likedByMe = likeService.isLiked(currentUserId, story.getId());
                        boolean bookmarkedByMe = bookmarkService.isBookmarked(currentUserId, story.getId());
                        long likeCount = getLikeCount(story.getId());
                        long commentCount = getCommentCount(story.getId());
                        return StoryResponse.fromStory(story, user, likedByMe, bookmarkedByMe, likeCount, commentCount);
                    })
                    .collect(Collectors.toList());

            return PagedResponse.of(responses, page, size, ids.size());
        } catch (Exception e) {
            log.warn("Trending cache read failed, falling back to full scan [RequestID: {}]: {}", requestId,
                    e.getMessage());
            return getTrendingStoriesFallback(page, size, currentUserId);
        }
    }

    /** Fallback: full collection scan when cache unavailable */
    private PagedResponse<StoryResponse> getTrendingStoriesFallback(int page, int size, String currentUserId) {
        List<Story> allActive = storyRepository.findByStatus(Story.StoryStatus.ACTIVE);
        List<StoryWithTrendingScore> withScores = allActive.stream()
                .map(s -> StoryWithTrendingScore.fromStory(s, getLikeCount(s.getId()),
                        s.getViewCount() != null ? s.getViewCount() : 0L, getCommentCount(s.getId())))
                .sorted((a, b) -> Double.compare(b.getTrendingScore(), a.getTrendingScore()))
                .collect(Collectors.toList());

        int start = page * size;
        List<StoryWithTrendingScore> pageItems = withScores.stream().skip(start).limit(size).toList();
        List<StoryResponse> responses = pageItems.stream()
                .map(sws -> {
                    Story story = sws.getStory();
                    User user = userService.getUserEntityById(story.getUserId());
                    if (currentUserId != null) {
                        boolean likedByMe = likeService.isLiked(currentUserId, story.getId());
                        boolean bookmarkedByMe = bookmarkService.isBookmarked(currentUserId, story.getId());
                        return StoryResponse.fromStory(story, user, likedByMe, bookmarkedByMe,
                                sws.getLikeCount(), sws.getCommentCount());
                    }
                    return StoryResponse.fromStory(story, user, false, false, sws.getLikeCount(),
                            sws.getCommentCount());
                })
                .collect(Collectors.toList());
        return PagedResponse.of(responses, page, size, withScores.size());
    }

    @Cacheable(value = "stories-type", key = "'FOR_YOU:' + #currentUserId + ':' + #page + ':' + #size")
    public PagedResponse<StoryResponse> getForYouStories(String currentUserId, int page, int size) {
        User user = userService.getUserEntityById(currentUserId);

        // Fetch a pool of active stories to rank
        // In a larger system, this would be precomputed or use a vector search
        List<Story> pool = storyRepository.findByStatus(Story.StoryStatus.ACTIVE);

        List<Story> sorted = pool.stream()
                .sorted((s1, s2) -> Double.compare(
                        recommendationService.calculateAffinityScore(user, s2),
                        recommendationService.calculateAffinityScore(user, s1)))
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());

        List<StoryResponse> stories = sorted.stream()
                .map(story -> toStoryResponse(story, currentUserId, true))
                .collect(Collectors.toList());

        return PagedResponse.of(stories, page, size, pool.size());
    }

    /**
     * Get for you stories by language sorted by affinity score (for authenticated
     * users)
     */
    @Cacheable(value = "stories-type", key = "'FOR_YOU_LANG:' + #currentUserId + ':' + #language + ':' + #page + ':' + #size")
    public PagedResponse<StoryResponse> getForYouStoriesByLanguage(String language, String currentUserId, int page,
            int size) {
        String normalizedLang = normalizeLanguage(language);
        User user = userService.getUserEntityById(currentUserId);

        Pageable poolPageable = PageRequest.of(0, 500); // Fetch top pool
        List<Story> pool = storyRepository
                .findByLanguageAndStatus(normalizedLang, Story.StoryStatus.ACTIVE, poolPageable)
                .getContent();

        List<Story> sorted = pool.stream()
                .sorted((s1, s2) -> Double.compare(
                        recommendationService.calculateAffinityScore(user, s2),
                        recommendationService.calculateAffinityScore(user, s1)))
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());

        List<StoryResponse> stories = sorted.stream()
                .map(story -> toStoryResponse(story, currentUserId, true))
                .collect(Collectors.toList());

        return PagedResponse.of(stories, page, size, pool.size());
    }

    /**
     * Get curated stories from the nightly precomputed feed
     */
    @Cacheable(value = "stories-type", key = "'CURATED:' + #userId + ':' + #page + ':' + #size")
    public PagedResponse<StoryResponse> getCuratedStories(String userId, int page, int size) {
        List<String> storyIds = curatedFeedService.getCuratedStoryIds();

        // If cache is empty, fall back to trending
        if (storyIds.isEmpty()) {
            log.info("Curated feed cache is empty, falling back to trending");
            return getTrendingStories(userId, page, size);
        }

        // Apply pagination to the cached IDs
        int start = Math.min(page * size, storyIds.size());
        int end = Math.min(start + size, storyIds.size());
        List<String> pageIds = storyIds.subList(start, end);

        List<StoryResponse> stories = pageIds.stream()
                .map(id -> storyRepository.findById(id))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(story -> toStoryResponse(story, userId, true))
                .collect(Collectors.toList());

        return PagedResponse.of(stories, page, size, storyIds.size());
    }

    /**
     * Get my stories from Story entity only (no StoryDataStore data)
     * 
     * @param currentUserId The current user ID
     * @param page          Page number
     * @param size          Page size
     * @return PagedResponse of stories with status from Story entity only
     */
    @Cacheable(value = "stories-mine", key = "#currentUserId + ':' + #page + ':' + #size")
    public PagedResponse<StoryResponse> getMyStories(String currentUserId, int page, int size) {
        String requestId = RequestContext.getRequestId();
        log.info("Getting my stories from Story entity for user: {} [RequestID: {}]", currentUserId, requestId);

        try {
            // Get all Story entities for the user with pagination
            Pageable pageable = PageRequest.of(page, size);
            Page<Story> storyPage = storyRepository.findByUserIdOrderByCreatedAtDesc(currentUserId, pageable);

            // Convert to StoryResponse objects using only Story entity data
            List<StoryResponse> stories = storyPage.getContent().stream()
                    .map(story -> {
                        try {
                            User user = userService.getUserEntityById(story.getUserId());

                            // Fetch interaction data
                            long likeCount = getLikeCount(story.getId());
                            long commentCount = getCommentCount(story.getId());
                            boolean likedByMe = likeService.isLiked(currentUserId, story.getId());
                            boolean bookmarkedByMe = bookmarkService.isBookmarked(currentUserId, story.getId());

                            // Create StoryResponse with all data
                            return StoryResponse.fromStory(story, user, likedByMe, bookmarkedByMe, likeCount,
                                    commentCount);
                        } catch (Exception e) {
                            log.error("Error creating StoryResponse for story {} [RequestID: {}]: {}",
                                    story.getId(), requestId, e.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            log.info("Returning {} my stories from Story entity for user {} [RequestID: {}]",
                    stories.size(), currentUserId, requestId);

            return PagedResponse.of(stories, page, size, storyPage.getTotalElements());

        } catch (Exception e) {
            log.error("Error getting my stories from Story entity for user {} [RequestID: {}]: {}",
                    currentUserId, requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to get my stories: " + e.getMessage(), e);
        }
    }

    /**
     * Get my stories from StoryDataStore collection (fetching status from
     * StoryDataStore instead of Story)
     * 
     * @param currentUserId The current user ID
     * @param page          Page number
     * @param size          Page size
     * @return PagedResponse of stories with status from StoryDataStore
     */
    public PagedResponse<StoryResponse> getMyStoriesFromDataStore(String currentUserId, int page, int size) {
        String requestId = RequestContext.getRequestId();
        log.info("Getting my stories from StoryDataStore for user: {} [RequestID: {}]", currentUserId, requestId);

        try {
            // Get all StoryDataStore entries for the user
            List<StoryDataStore> userDataStores = storyDataStoreRepository.findByUserId(currentUserId);

            // Sort by creation date (newest first)
            userDataStores.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

            // Apply pagination
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, userDataStores.size());

            List<StoryDataStore> paginatedDataStores = userDataStores.subList(startIndex, endIndex);

            // Convert to StoryResponse objects
            List<StoryResponse> stories = paginatedDataStores.stream()
                    .map(dataStore -> {
                        try {
                            // Get the corresponding Story entity for additional data
                            Story story = storyRepository.findById(dataStore.getStoryId()).orElse(null);
                            User user = userService.getUserEntityById(dataStore.getUserId());

                            // Get interaction data
                            boolean likedByMe = likeService.isLiked(currentUserId, dataStore.getStoryId());
                            boolean bookmarkedByMe = bookmarkService.isBookmarked(currentUserId,
                                    dataStore.getStoryId());
                            long likeCount = getLikeCount(dataStore.getStoryId());
                            long commentCount = getCommentCount(dataStore.getStoryId());

                            // Convert ProcessingStatus to StoryStatus
                            Story.StoryStatus storyStatus = convertProcessingStatusToStoryStatus(
                                    dataStore.getProcessingStatus());

                            // Create StoryResponse with status from StoryDataStore
                            // Use status from StoryDataStore

                            return StoryResponse.builder()
                                    .id(dataStore.getStoryId())
                                    .userId(dataStore.getUserId())
                                    .username(user != null ? user.getName() : null)
                                    .title(dataStore.getTitle() != null ? dataStore.getTitle()
                                            : (story != null ? story.getTitle() : "Processing..."))
                                    .audioUrl(dataStore.getAudioUrl() != null ? dataStore.getAudioUrl()
                                            : (story != null ? story.getAudioUrl() : null))
                                    .coverImageUrl(
                                            story != null ? (story.getCoverImageUrl() != null ? story.getCoverImageUrl()
                                                    : story.getThumbnailUrl()) : null)
                                    .storyImages(story != null ? story.getStoryImages() : null)
                                    .viewCount(story != null ? story.getViewCount() : 0L)
                                    .likeCount(likeCount)
                                    .commentCount(commentCount)
                                    .status(storyStatus) // Use status from StoryDataStore
                                    .language(dataStore.getLanguage())
                                    .rejectionReasons(story != null ? story.getRejectionReasons() : null)
                                    .contents(story != null ? story.getContents() : null)
                                    .tags(story != null ? story.getTags() : null)
                                    .createdAt(dataStore.getCreatedAt())
                                    .updatedAt(dataStore.getUpdatedAt())
                                    .isLikedByMe(likedByMe)
                                    .isBookmarkedByMe(bookmarkedByMe)
                                    .creationType(story != null ? story.getCreationType() : Story.CreationType.UPLOADED)
                                    .category(story != null ? story.getCategory() : null)
                                    .build();
                        } catch (Exception e) {
                            log.error("Error creating StoryResponse for dataStore {} [RequestID: {}]: {}",
                                    dataStore.getStoryId(), requestId, e.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            log.info("Returning {} my stories from StoryDataStore for user {} [RequestID: {}]",
                    stories.size(), currentUserId, requestId);

            return PagedResponse.of(stories, page, size, userDataStores.size());

        } catch (Exception e) {
            log.error("Error getting my stories from StoryDataStore for user {} [RequestID: {}]: {}",
                    currentUserId, requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to get my stories: " + e.getMessage(), e);
        }
    }

    /**
     * Convert StoryDataStore.ProcessingStatus to Story.StoryStatus
     */
    private Story.StoryStatus convertProcessingStatusToStoryStatus(StoryDataStore.ProcessingStatus processingStatus) {
        if (processingStatus == null) {
            return Story.StoryStatus.UPLOAD_PENDING;
        }

        return switch (processingStatus) {
            case UPLOAD_PENDING -> Story.StoryStatus.UPLOAD_PENDING;
            case UPLOADING -> Story.StoryStatus.UPLOADING;
            case PROCESSING_PENDING -> Story.StoryStatus.PROCESSING_PENDING;
            case PROCESSING -> Story.StoryStatus.PROCESSING;
            case PROCESSED -> Story.StoryStatus.PROCESSED;
            case CONVERTING -> Story.StoryStatus.CONVERTING;
            case COMPLETED -> Story.StoryStatus.ACTIVE;
            case FAILED -> Story.StoryStatus.FAILED;
            case REJECTED -> Story.StoryStatus.REJECTED;
        };
    }

    /**
     * Get stories by language with user context (includes likedByMe status)
     * 
     * @param language      The language to filter by
     * @param currentUserId The current user ID
     * @param page          Page number
     * @param size          Page size
     * @return PagedResponse of stories in the specified language with user context
     */
    @Cacheable(value = "stories-feed", key = "#language + ':' + #currentUserId + ':' + #page + ':' + #size")
    public PagedResponse<StoryResponse> getStoriesByLanguage(String language, String currentUserId, int page,
            int size) {
        String normalizedLang = normalizeLanguage(language);
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> storyPage = storyRepository.findByLanguageAndStatus(normalizedLang, Story.StoryStatus.ACTIVE,
                pageable);

        List<StoryResponse> stories = storyPage.getContent().stream()
                .map(story -> {
                    User user = userService.getUserEntityById(story.getUserId());
                    boolean likedByMe = likeService.isLiked(currentUserId, story.getId());
                    boolean bookmarkedByMe = bookmarkService.isBookmarked(currentUserId, story.getId());
                    long likeCount = getLikeCount(story.getId());
                    long commentCount = getCommentCount(story.getId());
                    return StoryResponse.fromStory(story, user, likedByMe, bookmarkedByMe, likeCount, commentCount);
                })
                .collect(Collectors.toList());

        return PagedResponse.of(stories, page, size, storyPage.getTotalElements());
    }

    /**
     * Get stories by language (for unauthenticated users)
     * 
     * @param language The language to filter by
     * @param page     Page number
     * @param size     Page size
     * @return PagedResponse of stories in the specified language
     */
    @Cacheable(value = "stories-feed", key = "#language + ':anon:' + #page + ':' + #size")
    public PagedResponse<StoryResponse> getStoriesByLanguage(String language, int page, int size) {
        String normalizedLang = normalizeLanguage(language);
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> storyPage = storyRepository.findByLanguageAndStatus(normalizedLang, Story.StoryStatus.ACTIVE,
                pageable);

        List<StoryResponse> stories = storyPage.getContent().stream()
                .map(story -> {
                    User user = userService.getUserEntityById(story.getUserId());
                    long likeCount = getLikeCount(story.getId());
                    long commentCount = getCommentCount(story.getId());
                    return StoryResponse.fromStory(story, user, false, false, likeCount, commentCount);
                })
                .collect(Collectors.toList());

        return PagedResponse.of(stories, page, size, storyPage.getTotalElements());
    }

    @Cacheable(value = "story", key = "#storyId + ':' + (#currentUserId != null ? #currentUserId : 'anon')")
    public StoryResponse getStoryById(String storyId, String currentUserId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found with ID: " + storyId));

        User user = userService.getUserEntityById(story.getUserId());

        // Check if the current user liked this story
        boolean likedByMe = likeService.isLiked(currentUserId, storyId);
        boolean bookmarkedByMe = bookmarkService.isBookmarked(currentUserId, storyId);

        long likeCount = getLikeCount(storyId);
        long commentCount = getCommentCount(story.getId());

        return StoryResponse.fromStory(story, user, likedByMe, bookmarkedByMe, likeCount, commentCount);
    }

    @Cacheable(value = "story", key = "#storyId + ':anon'")
    public StoryResponse getStoryById(String storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found with ID: " + storyId));

        User user = userService.getUserEntityById(story.getUserId());

        long likeCount = getLikeCount(storyId);
        long commentCount = getCommentCount(story.getId());

        return StoryResponse.fromStory(story, user, false, false, likeCount, commentCount); // Default to false when no
                                                                                            // user context
    }

    /**
     * Like a story
     * 
     * @param userId  The user ID who is liking the story
     * @param storyId The story ID to like
     * @return LikeResponse with like details
     */
    @CacheEvict(value = "story", allEntries = true)
    public LikeResponse likeStory(String userId, String storyId) {
        log.info("User {} attempting to like story {}", userId, storyId);

        // Check if story exists and is active
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found with ID: " + storyId));

        if (story.getStatus() != Story.StoryStatus.ACTIVE) {
            throw new RuntimeException("Cannot like a story that is not active");
        }

        // Check if user already liked this story
        if (likeService.isLiked(userId, storyId)) {
            throw new RuntimeException("User already liked this story");
        }

        // Create like
        LikeRequest likeRequest = LikeRequest.builder()
                .storyId(storyId)
                .build();
        LikeResponse likeResponse = likeService.createLike(userId, likeRequest);

        // Track interaction for recommendation scoring
        recommendationService.trackInteraction(userId, storyId, RecommendationService.InteractionType.LIKE);

        log.info("User {} successfully liked story {}", userId, storyId);
        return likeResponse;
    }

    /** Evict story cache (call after admin update/delete). */
    @CacheEvict(value = "story", allEntries = true)
    public void evictStoryCache() {
        // No-op; annotation triggers cache eviction when called from another bean
    }

    /**
     * Unlike a story
     * 
     * @param userId  The user ID who is unliking the story
     * @param storyId The story ID to unlike
     */
    @CacheEvict(value = "story", allEntries = true)
    public void unlikeStory(String userId, String storyId) {
        log.info("User {} attempting to unlike story {}", userId, storyId);

        // Check if story exists
        if (!storyRepository.existsById(storyId)) {
            throw new RuntimeException("Story not found with ID: " + storyId);
        }

        // Check if user liked this story
        if (!likeService.isLiked(userId, storyId)) {
            throw new RuntimeException("User has not liked this story");
        }

        // Remove like
        likeService.deleteLikeByUserAndStory(userId, storyId);

        log.info("User {} successfully unliked story {}", userId, storyId);
    }

    /**
     * Get like count for a story
     * 
     * @param storyId The story ID
     * @return Number of likes
     */
    public long getLikeCount(String storyId) {
        return likeService.getLikeCount(storyId);
    }

    /**
     * Get stories liked by a user
     * 
     * @param userId The user ID
     * @param page   Page number
     * @param size   Page size
     * @return PagedResponse of liked stories
     */
    public PagedResponse<StoryResponse> getLikedStories(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> storyPage = storyRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<StoryResponse> stories = storyPage.getContent().stream()
                .filter(story -> likeService.isLiked(userId, story.getId()))
                .map(story -> {
                    User user = userService.getUserEntityById(story.getUserId());
                    boolean bookmarkedByMe = bookmarkService.isBookmarked(userId, story.getId());
                    long likeCount = getLikeCount(story.getId());
                    long commentCount = getCommentCount(story.getId());
                    return StoryResponse.fromStory(story, user, true, bookmarkedByMe, likeCount, commentCount);
                })
                .collect(Collectors.toList());

        return PagedResponse.of(stories, page, size, storyPage.getTotalElements());
    }

    /**
     * Get comments for a story
     * 
     * @param storyId The story ID
     * @param page    Page number
     * @param size    Page size
     * @return PagedResponse of comments with nested replies
     */
    public PagedResponse<CommentResponse> getStoryComments(String storyId, String userId, int page, int size) {
        return commentService.getCommentsByStory(storyId, userId, page, size);
    }

    /**
     * Get all comments for a story (including replies)
     * 
     * @param storyId The story ID
     * @return List of all comments with nested replies
     */
    public List<CommentResponse> getAllStoryComments(String storyId) {
        return commentService.getAllCommentsByStory(storyId);
    }

    /**
     * Get comment count for a story
     * 
     * @param storyId The story ID
     * @return Total number of comments (including replies)
     */
    public long getCommentCount(String storyId) {
        return commentService.getCommentCount(storyId);
    }

    /**
     * Get Story entity by ID
     * 
     * @param storyId The story ID
     * @return Story entity
     */
    public Story getStoryEntityById(String storyId) {
        return storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found with ID: " + storyId));
    }

    public void incrementViewCount(String storyId) {
        Query query = Query.query(Criteria.where("id").is(storyId));
        Update update = new Update().inc("viewCount", 1);
        mongoTemplate.updateFirst(query, update, Story.class);
        log.debug("View count incremented for story: {}", storyId);

        // // Check for views milestone reward (async-safe: reads after update)
        // RewardService rewardService =
        // ApplicationContextProvider.getBean(RewardService.class);
        // rewardService.checkViewsMilestoneReward(storyId);
    }

    public void incrementPlayCount(String storyId) {
        Query query = Query.query(Criteria.where("id").is(storyId));
        Update update = new Update().inc("playCount", 1);
        mongoTemplate.updateFirst(query, update, Story.class);
        log.debug("Play count incremented for story: {}", storyId);
    }

    @Async("storyOpsExecutor")
    public void incrementPlayCountAsync(String storyId) {
        incrementPlayCount(storyId);
    }

    public void incrementCompletionCount(String storyId) {
        Query query = Query.query(Criteria.where("id").is(storyId));
        Update update = new Update().inc("completionCount", 1);
        mongoTemplate.updateFirst(query, update, Story.class);
        log.debug("Completion count incremented for story: {}", storyId);
    }

    @Async("storyOpsExecutor")
    public void incrementCompletionCountAsync(String storyId) {
        incrementCompletionCount(storyId);
    }

    /**
     * Async, fire-and-forget. Records a deduplicated view:
     * - self-views are excluded
     * - same user/IP is counted at most once per 24 h per story
     * - Redis INCR is batched and flushed to MongoDB every 60 s
     */
    @Async("storyOpsExecutor")
    public void recordViewAsync(String storyId, String viewerId, String ipAddress, boolean isOwnStory) {
        viewCountBatchService.recordView(storyId, viewerId, ipAddress, isOwnStory);
    }

    /**
     * Get latest stories with cursor-based pagination (efficient for deep pages).
     * Pass cursor from previous response's nextCursor. Size+1 fetched to determine
     * hasMore.
     */
    public PagedResponse<StoryResponse> getStoriesWithCursor(String currentUserId, String language, String category,
            String cursor, int size) {
        java.time.LocalDateTime before = cursor != null && !cursor.isBlank()
                ? java.time.LocalDateTime.parse(cursor)
                : null;

        Story.Category storyCategory = null;
        if (category != null && !category.isBlank()) {
            try {
                storyCategory = Story.Category.valueOf(category.toUpperCase());
            } catch (Exception e) {
                log.warn("Invalid category for cursor pagination: {}", category);
            }
        }

        List<Story> stories;
        if (before == null) {
            if (storyCategory != null) {
                if (language != null && !language.isBlank()) {
                    stories = storyRepository.findByStatusAndLanguageAndCategory(
                            Story.StoryStatus.ACTIVE, language, storyCategory, PageRequest.of(0, size + 1))
                            .getContent();
                } else {
                    stories = storyRepository.findByStatusAndCategory(
                            Story.StoryStatus.ACTIVE, storyCategory, PageRequest.of(0, size + 1)).getContent();
                }
            } else if (language != null && !language.isBlank()) {
                stories = storyRepository.findByLanguageAndStatusOrderByCreatedAtDesc(
                        language, Story.StoryStatus.ACTIVE, PageRequest.of(0, size + 1)).getContent();
            } else {
                stories = storyRepository.findByStatusOrderByCreatedAtDesc(
                        Story.StoryStatus.ACTIVE, PageRequest.of(0, size + 1)).getContent();
            }
        } else {
            if (storyCategory != null) {
                if (language != null && !language.isBlank()) {
                    stories = storyRepository.findByStatusAndLanguageAndCategoryAndCreatedAtBeforeOrderByCreatedAtDesc(
                            Story.StoryStatus.ACTIVE, language, storyCategory, before, PageRequest.of(0, size + 1));
                } else {
                    stories = storyRepository.findByStatusAndCategoryAndCreatedAtBeforeOrderByCreatedAtDesc(
                            Story.StoryStatus.ACTIVE, storyCategory, before, PageRequest.of(0, size + 1));
                }
            } else if (language != null && !language.isBlank()) {
                stories = storyRepository.findByLanguageAndStatusAndCreatedAtBeforeOrderByCreatedAtDesc(
                        language, Story.StoryStatus.ACTIVE, before, PageRequest.of(0, size + 1));
            } else {
                stories = storyRepository.findByStatusAndCreatedAtBeforeOrderByCreatedAtDesc(
                        Story.StoryStatus.ACTIVE, before, PageRequest.of(0, size + 1));
            }
        }
        return buildCursorResponse(stories, size, currentUserId, true);
    }

    @Deprecated
    public PagedResponse<StoryResponse> getLatestStoriesWithCursor(String currentUserId, String cursor, int size) {
        return getStoriesWithCursor(currentUserId, null, null, cursor, size);
    }

    private PagedResponse<StoryResponse> buildCursorResponse(List<Story> stories, int size,
            String currentUserId, boolean withUserContext) {
        boolean hasMore = stories.size() > size;
        List<Story> pageStories = hasMore ? stories.subList(0, size) : stories;
        String nextCursor = hasMore && !pageStories.isEmpty()
                ? pageStories.get(pageStories.size() - 1).getCreatedAt().toString()
                : null;
        List<StoryResponse> responses = pageStories.stream()
                .map(s -> toStoryResponse(s, currentUserId, withUserContext))
                .collect(Collectors.toList());
        return PagedResponse.ofWithCursor(responses, size, nextCursor);
    }

    private StoryResponse toStoryResponse(Story story, String userId, boolean withContext) {
        User user = userService.getUserEntityById(story.getUserId());
        if (withContext && userId != null) {
            boolean likedByMe = likeService.isLiked(userId, story.getId());
            boolean bookmarkedByMe = bookmarkService.isBookmarked(userId, story.getId());
            return StoryResponse.fromStory(story, user, likedByMe, bookmarkedByMe,
                    getLikeCount(story.getId()), getCommentCount(story.getId()));
        }
        return StoryResponse.fromStory(story, user, false, false,
                getLikeCount(story.getId()), getCommentCount(story.getId()));
    }

    /**
     * Get latest stories sorted by creation date (newest first)
     * 
     * @param currentUserId The current user ID
     * @param page          Page number
     * @param size          Page size
     * @return PagedResponse of latest stories with user context
     */
    @Cacheable(value = "stories-feed", key = "'LATEST:' + #currentUserId + ':' + #page + ':' + #size")
    public PagedResponse<StoryResponse> getLatestStories(String currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> storyPage = storyRepository.findByStatusOrderByCreatedAtDesc(Story.StoryStatus.ACTIVE, pageable);

        List<StoryResponse> stories = storyPage.getContent().stream()
                .map(story -> {
                    User user = userService.getUserEntityById(story.getUserId());
                    boolean likedByMe = likeService.isLiked(currentUserId, story.getId());
                    boolean bookmarkedByMe = bookmarkService.isBookmarked(currentUserId, story.getId());
                    long likeCount = getLikeCount(story.getId());
                    long commentCount = getCommentCount(story.getId());
                    return StoryResponse.fromStory(story, user, likedByMe, bookmarkedByMe, likeCount, commentCount);
                })
                .collect(Collectors.toList());

        return PagedResponse.of(stories, page, size, storyPage.getTotalElements());
    }

    /**
     * Get latest stories sorted by creation date (newest first) - for
     * unauthenticated users
     * 
     * @param page Page number
     * @param size Page size
     * @return PagedResponse of latest stories
     */
    @Cacheable(value = "stories-feed", key = "'LATEST:anon:' + #page + ':' + #size")
    public PagedResponse<StoryResponse> getLatestStories(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> storyPage = storyRepository.findByStatusOrderByCreatedAtDesc(Story.StoryStatus.ACTIVE, pageable);

        List<StoryResponse> stories = storyPage.getContent().stream()
                .map(story -> {
                    User user = userService.getUserEntityById(story.getUserId());
                    long likeCount = getLikeCount(story.getId());
                    long commentCount = getCommentCount(story.getId());
                    return StoryResponse.fromStory(story, user, false, false, likeCount, commentCount);
                })
                .collect(Collectors.toList());

        return PagedResponse.of(stories, page, size, storyPage.getTotalElements());
    }

    /**
     * Get latest stories by language sorted by creation date (newest first) - for
     * unauthenticated users
     * 
     * @param language The language to filter by
     * @param page     Page number
     * @param size     Page size
     * @return PagedResponse of latest stories in the specified language
     */
    public PagedResponse<StoryResponse> getLatestStoriesByLanguage(String language, int page, int size) {
        String normalizedLang = normalizeLanguage(language);
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> storyPage = storyRepository.findByLanguageAndStatusOrderByCreatedAtDesc(normalizedLang,
                Story.StoryStatus.ACTIVE, pageable);

        List<StoryResponse> stories = storyPage.getContent().stream()
                .map(story -> {
                    User user = userService.getUserEntityById(story.getUserId());
                    long likeCount = getLikeCount(story.getId());
                    long commentCount = getCommentCount(story.getId());
                    return StoryResponse.fromStory(story, user, false, false, likeCount, commentCount);
                })
                .collect(Collectors.toList());

        return PagedResponse.of(stories, page, size, storyPage.getTotalElements());
    }

    /**
     * Get latest stories by language sorted by creation date (newest first) - for
     * authenticated users
     * 
     * @param language      The language to filter by
     * @param currentUserId The current user ID
     * @param page          Page number
     * @param size          Page size
     * @return PagedResponse of latest stories in the specified language with user
     *         context
     */
    public PagedResponse<StoryResponse> getLatestStoriesByLanguage(String language, String currentUserId, int page,
            int size) {
        String normalizedLang = normalizeLanguage(language);
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> storyPage = storyRepository.findByLanguageAndStatusOrderByCreatedAtDesc(normalizedLang,
                Story.StoryStatus.ACTIVE, pageable);

        List<StoryResponse> stories = storyPage.getContent().stream()
                .map(story -> {
                    User user = userService.getUserEntityById(story.getUserId());
                    boolean likedByMe = likeService.isLiked(currentUserId, story.getId());
                    boolean bookmarkedByMe = bookmarkService.isBookmarked(currentUserId, story.getId());
                    long likeCount = getLikeCount(story.getId());
                    long commentCount = getCommentCount(story.getId());
                    return StoryResponse.fromStory(story, user, likedByMe, bookmarkedByMe, likeCount, commentCount);
                })
                .collect(Collectors.toList());

        return PagedResponse.of(stories, page, size, storyPage.getTotalElements());
    }

    /**
     * Get voice stories (stories with UPLOADED creation type) sorted by creation
     * date (newest first) - for authenticated users
     * 
     * @param currentUserId The current user ID
     * @param page          Page number
     * @param size          Page size
     * @return PagedResponse of voice stories with user context
     */
    @Cacheable(value = "stories-feed", key = "'VOICE:' + #currentUserId + ':' + #page + ':' + #size")
    public PagedResponse<StoryResponse> getVoiceStories(String currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> storyPage = storyRepository.findByCreationTypeAndStatusOrderByCreatedAtDesc(
                Story.CreationType.UPLOADED, Story.StoryStatus.ACTIVE, pageable);

        List<StoryResponse> stories = storyPage.getContent().stream()
                .map(story -> {
                    User user = userService.getUserEntityById(story.getUserId());
                    boolean likedByMe = likeService.isLiked(currentUserId, story.getId());
                    boolean bookmarkedByMe = bookmarkService.isBookmarked(currentUserId, story.getId());
                    long likeCount = getLikeCount(story.getId());
                    long commentCount = getCommentCount(story.getId());
                    return StoryResponse.fromStory(story, user, likedByMe, bookmarkedByMe, likeCount, commentCount);
                })
                .collect(Collectors.toList());

        return PagedResponse.of(stories, page, size, storyPage.getTotalElements());
    }

    /**
     * Get voice stories (stories with UPLOADED creation type) sorted by creation
     * date (newest first) - for unauthenticated users
     * 
     * @param page Page number
     * @param size Page size
     * @return PagedResponse of voice stories
     */
    @Cacheable(value = "stories-feed", key = "'VOICE:anon:' + #page + ':' + #size")
    public PagedResponse<StoryResponse> getVoiceStories(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> storyPage = storyRepository.findByCreationTypeAndStatusOrderByCreatedAtDesc(
                Story.CreationType.UPLOADED, Story.StoryStatus.ACTIVE, pageable);

        List<StoryResponse> stories = storyPage.getContent().stream()
                .map(story -> {
                    User user = userService.getUserEntityById(story.getUserId());
                    long likeCount = getLikeCount(story.getId());
                    long commentCount = getCommentCount(story.getId());
                    return StoryResponse.fromStory(story, user, false, false, likeCount, commentCount);
                })
                .collect(Collectors.toList());

        return PagedResponse.of(stories, page, size, storyPage.getTotalElements());
    }

    /**
     * Get voice stories by language (stories with UPLOADED creation type) sorted by
     * creation date (newest first) - for unauthenticated users
     * 
     * @param language The language to filter by
     * @param page     Page number
     * @param size     Page size
     * @return PagedResponse of voice stories in the specified language
     */
    public PagedResponse<StoryResponse> getVoiceStoriesByLanguage(String language, int page, int size) {
        String normalizedLang = normalizeLanguage(language);
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> storyPage = storyRepository.findByCreationTypeAndStatusAndLanguageOrderByCreatedAtDesc(
                Story.CreationType.UPLOADED, Story.StoryStatus.ACTIVE, normalizedLang, pageable);

        List<StoryResponse> stories = storyPage.getContent().stream()
                .map(story -> {
                    User user = userService.getUserEntityById(story.getUserId());
                    long likeCount = getLikeCount(story.getId());
                    long commentCount = getCommentCount(story.getId());
                    return StoryResponse.fromStory(story, user, false, false, likeCount, commentCount);
                })
                .collect(Collectors.toList());

        return PagedResponse.of(stories, page, size, storyPage.getTotalElements());
    }

    /**
     * Get voice stories by language (stories with UPLOADED creation type) sorted by
     * creation date (newest first) - for authenticated users
     * 
     * @param language      The language to filter by
     * @param currentUserId The current user ID
     * @param page          Page number
     * @param size          Page size
     * @return PagedResponse of voice stories in the specified language with user
     *         context
     */
    public PagedResponse<StoryResponse> getVoiceStoriesByLanguage(String language, String currentUserId, int page,
            int size) {
        String normalizedLang = normalizeLanguage(language);
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> storyPage = storyRepository.findByCreationTypeAndStatusAndLanguageOrderByCreatedAtDesc(
                Story.CreationType.UPLOADED, Story.StoryStatus.ACTIVE, normalizedLang, pageable);

        List<StoryResponse> stories = storyPage.getContent().stream()
                .map(story -> toStoryResponse(story, currentUserId, true))
                .collect(Collectors.toList());

        return PagedResponse.of(stories, page, size, storyPage.getTotalElements());
    }

    /**
     * Get featured stories (consolidated logic, currently trending based)
     */
    public PagedResponse<StoryResponse> getFeaturedStories(String userId, String language, int page, int size) {
        return getTrendingStoriesByLanguage(language, userId, page, size);
    }

    /**
     * Get most listened stories sorted by play count
     */
    public PagedResponse<StoryResponse> getMostListenedStories(String userId, String language, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> storyPage;

        if (language != null && !language.trim().isEmpty()) {
            storyPage = storyRepository.findByLanguageAndStatusOrderByCompletionCountDescPlayCountDesc(language,
                    Story.StoryStatus.ACTIVE,
                    pageable);
        } else {
            storyPage = storyRepository.findByStatusOrderByCompletionCountDescPlayCountDesc(Story.StoryStatus.ACTIVE,
                    pageable);
        }

        List<StoryResponse> stories = storyPage.getContent().stream()
                .map(story -> toStoryResponse(story, userId, true))
                .collect(Collectors.toList());

        return PagedResponse.of(stories, page, size, storyPage.getTotalElements());
    }

    public StoryResponse getNextStory(String currentId, String userId, String language) {
        // Simple autoplay ranking logic:
        // 1. Get current story to find tags
        Story currentStory = storyRepository.findById(currentId)
                .orElse(null);

        List<Story> candidates = new ArrayList<>();

        if (currentStory != null && currentStory.getTags() != null && !currentStory.getTags().isEmpty()) {
            // Try similar stories by tags
            Page<Story> similar = (language != null && !language.isBlank())
                    ? storyRepository.findByStatusAndLanguageAndTagsInAndIdNot(
                            Story.StoryStatus.ACTIVE, language, currentStory.getTags(), currentId, PageRequest.of(0, 5))
                    : storyRepository.findByStatusAndTagsInAndIdNot(
                            Story.StoryStatus.ACTIVE, currentStory.getTags(), currentId, PageRequest.of(0, 5));
            candidates.addAll(similar.getContent());
        }

        // 2. If not enough similar stories, try trending/latest in same
        // language/category
        if (candidates.size() < 5) {
            Page<Story> latest;
            if (language != null && !language.isBlank()) {
                if (currentStory != null && currentStory.getCategory() != null) {
                    latest = storyRepository.findByStatusAndLanguageAndCategory(
                            Story.StoryStatus.ACTIVE, language, currentStory.getCategory(), PageRequest.of(0, 10));
                } else {
                    latest = storyRepository.findByLanguageAndStatusOrderByCreatedAtDesc(
                            language, Story.StoryStatus.ACTIVE, PageRequest.of(0, 10));
                }
            } else {
                latest = storyRepository.findByStatusOrderByCreatedAtDesc(Story.StoryStatus.ACTIVE,
                        PageRequest.of(0, 10));
            }

            for (Story s : latest.getContent()) {
                if (!s.getId().equals(currentId) && candidates.stream().noneMatch(c -> c.getId().equals(s.getId()))) {
                    candidates.add(s);
                }
            }
        }

        if (candidates.isEmpty()) {
            // Last resort: any active stories
            List<Story> fallback = storyRepository.findByStatus(Story.StoryStatus.ACTIVE);
            if (!fallback.isEmpty()) {
                candidates.add(fallback.get(0));
            }
        }

        if (candidates.isEmpty()) {
            throw new RuntimeException("No next story found");
        }

        // Pick the first candidate (best match)
        Story next = candidates.get(0);
        return toStoryResponse(next, userId, true);
    }

    /**
     * Search stories by title contains (case-insensitive)
     * 
     * @param titleContains Text to search in story titles
     * @param currentUserId The current user ID
     * @param page          Page number
     * @param size          Page size
     * @return PagedResponse of stories with matching titles
     */
    public PagedResponse<StoryResponse> searchStoriesByTitle(String titleContains, String currentUserId, int page,
            int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> storyPage = storyRepository.findByTitleContaining(titleContains, pageable);

        List<StoryResponse> stories = storyPage.getContent().stream()
                .map(story -> {
                    User user = userService.getUserEntityById(story.getUserId());
                    boolean likedByMe = likeService.isLiked(currentUserId, story.getId());
                    boolean bookmarkedByMe = bookmarkService.isBookmarked(currentUserId, story.getId());
                    long likeCount = getLikeCount(story.getId());
                    long commentCount = getCommentCount(story.getId());
                    return StoryResponse.fromStory(story, user, likedByMe, bookmarkedByMe, likeCount, commentCount);
                })
                .collect(Collectors.toList());

        return PagedResponse.of(stories, page, size, storyPage.getTotalElements());
    }

    /**
     * Search stories by title contains (case-insensitive) - for unauthenticated
     * users
     * 
     * @param titleContains Text to search in story titles
     * @param page          Page number
     * @param size          Page size
     * @return PagedResponse of stories with matching titles
     */
    public PagedResponse<StoryResponse> searchStoriesByTitle(String titleContains, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> storyPage = storyRepository.findByTitleContaining(titleContains, pageable);

        List<StoryResponse> stories = storyPage.getContent().stream()
                .map(story -> {
                    User user = userService.getUserEntityById(story.getUserId());
                    long likeCount = getLikeCount(story.getId());
                    long commentCount = getCommentCount(story.getId());
                    return StoryResponse.fromStory(story, user, false, false, likeCount, commentCount);
                })
                .collect(Collectors.toList());

        return PagedResponse.of(stories, page, size, storyPage.getTotalElements());
    }

    /**
     * Update StoryMetadata with new information using utility methods
     * 
     * @param existingMetadata Existing metadata (can be null)
     * @param newLocations     New locations to add
     * @param newNames         New names to add
     * @param newPincodes      New pincodes to add
     * @param newState         New state (overwrites if not null)
     * @param newDistrict      New district (overwrites if not null)
     * @return Updated StoryMetadata
     */
    public StoryMetadata updateStoryMetadata(StoryMetadata existingMetadata,
            List<String> newLocations,
            List<String> newNames,
            List<String> newPincodes,
            String newState,
            String newDistrict) {

        if (existingMetadata == null) {
            return StoryMetadata.builder()
                    .locations(ListUtils.addOrCreateStrings(null, newLocations))
                    .names(ListUtils.addOrCreateStrings(null, newNames))
                    .pincodes(ListUtils.addOrCreateStrings(null, newPincodes))
                    .state(newState)
                    .district(newDistrict)
                    .build();
        }

        return StoryMetadata.builder()
                .locations(ListUtils.addOrCreateStrings(existingMetadata.getLocations(), newLocations))
                .names(ListUtils.addOrCreateStrings(existingMetadata.getNames(), newNames))
                .pincodes(ListUtils.addOrCreateStrings(existingMetadata.getPincodes(), newPincodes))
                .state(newState != null ? newState : existingMetadata.getState())
                .district(newDistrict != null ? newDistrict : existingMetadata.getDistrict())
                .language(existingMetadata.getLanguage())
                .deviceInfo(existingMetadata.getDeviceInfo())
                .build();
    }

    /**
     * Search stories
     */
    public StorySearchResponse searchStories(StorySearchRequest request, String currentUserId) {
        return storyDataStoreService.comprehensiveSearch(request, null);
    }

    /**
     * Get story processing details
     */
    public Map<String, Object> getStoryProcessingDetails(String storyId) {
        Optional<StoryDataStore> dataStore = storyDataStoreService.getDataStoreByStoryId(storyId);

        if (dataStore.isEmpty()) {
            return Map.of("error", "Story not found");
        }

        StoryDataStore data = dataStore.get();

        Map<String, Object> details = new HashMap<>();
        details.put("storyId", data.getStoryId());
        details.put("status", data.getProcessingStatus().toString());
        details.put("createdAt", data.getCreatedAt());
        details.put("updatedAt", data.getUpdatedAt());
        details.put("audioUrl", data.getAudioUrl());
        details.put("duration", data.getDuration());
        details.put("transcriptionCompletedAt", data.getTranscriptionCompletedAt());
        details.put("rewriteCompletedAt", data.getRewriteCompletedAt());
        details.put("paragraphCompletedAt", data.getParagraphCompletedAt());
        details.put("analysisCompletedAt", data.getAnalysisCompletedAt());
        details.put("processingStartedAt", data.getProcessingStartedAt());
        details.put("processingCompletedAt", data.getProcessingCompletedAt());
        details.put("errors", data.getErrors());
        details.put("errorMessage", data.getErrorMessage());
        details.put("transcriptionError", data.getTranscriptionError());
        details.put("rewriteError", data.getRewriteError());
        details.put("paragraphError", data.getParagraphError());
        details.put("analysisError", data.getAnalysisError());
        details.put("stepErrors", data.getStepErrors());

        return details;
    }

    /**
     * Get trending stories with custom weights
     * 
     * @param currentUserId  The current user ID (can be null for unauthenticated
     *                       users)
     * @param page           Page number
     * @param size           Page size
     * @param likesWeight    Weight for likes
     * @param viewsWeight    Weight for views
     * @param commentsWeight Weight for comments
     * @return PagedResponse of trending stories
     */
    public PagedResponse<StoryResponse> getTrendingStoriesWithCustomWeights(String currentUserId, int page, int size,
            double likesWeight, double viewsWeight, double commentsWeight) {
        String requestId = RequestContext.getRequestId();
        log.info("Getting trending stories with custom weights - likes: {}, views: {}, comments: {} [RequestID: {}]",
                likesWeight, viewsWeight, commentsWeight, requestId);

        try {
            // Get all active stories
            List<Story> allActiveStories = storyRepository.findByStatus(Story.StoryStatus.ACTIVE);

            // Calculate trending scores with custom weights
            List<StoryWithTrendingScore> storiesWithScores = allActiveStories.stream()
                    .map(story -> {
                        try {
                            long likeCount = getLikeCount(story.getId());
                            long viewCount = story.getViewCount() != null ? story.getViewCount() : 0L;
                            long commentCount = getCommentCount(story.getId());

                            // Calculate trending score with custom weights
                            double trendingScore = com.breakupstories.util.TrendingScoreCalculator
                                    .calculateTrendingScore(
                                            likeCount, viewCount, commentCount, likesWeight, viewsWeight,
                                            commentsWeight);

                            return StoryWithTrendingScore.builder()
                                    .story(story)
                                    .trendingScore(trendingScore)
                                    .likeCount(likeCount)
                                    .viewCount(viewCount)
                                    .commentCount(commentCount)
                                    .build();
                        } catch (Exception e) {
                            log.warn("Error calculating trending score for story {} [RequestID: {}]: {}",
                                    story.getId(), requestId, e.getMessage());
                            return null;
                        }
                    })
                    .filter(storyWithScore -> storyWithScore != null)
                    .sorted((s1, s2) -> Double.compare(s2.getTrendingScore(), s1.getTrendingScore())) // Sort by
                                                                                                      // trending score
                                                                                                      // descending
                    .collect(Collectors.toList());

            log.info("Calculated trending scores for {} stories with custom weights [RequestID: {}]",
                    storiesWithScores.size(), requestId);

            // Apply pagination
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, storiesWithScores.size());

            List<StoryResponse> paginatedStories = storiesWithScores.stream()
                    .skip(startIndex)
                    .limit(size)
                    .map(storyWithScore -> {
                        try {
                            Story story = storyWithScore.getStory();
                            User user = userService.getUserEntityById(story.getUserId());

                            if (currentUserId != null) {
                                // For authenticated users
                                boolean likedByMe = likeService.isLiked(currentUserId, story.getId());
                                boolean bookmarkedByMe = bookmarkService.isBookmarked(currentUserId, story.getId());
                                return StoryResponse.fromStory(story, user, likedByMe, bookmarkedByMe,
                                        storyWithScore.getLikeCount(), storyWithScore.getCommentCount());
                            } else {
                                // For unauthenticated users
                                return StoryResponse.fromStory(story, user, false, false,
                                        storyWithScore.getLikeCount(), storyWithScore.getCommentCount());
                            }
                        } catch (Exception e) {
                            log.error("Error creating StoryResponse for trending story {} [RequestID: {}]: {}",
                                    storyWithScore.getStory().getId(), requestId, e.getMessage());
                            return null;
                        }
                    })
                    .filter(story -> story != null)
                    .collect(Collectors.toList());

            log.info("Returning {} trending stories for page {} with custom weights [RequestID: {}]",
                    paginatedStories.size(), page, requestId);

            return PagedResponse.of(paginatedStories, page, size, storiesWithScores.size());

        } catch (Exception e) {
            log.error("Error getting trending stories with custom weights [RequestID: {}]: {}",
                    requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to get trending stories: " + e.getMessage(), e);
        }
    }

    /**
     * Search stories by title and tags
     * 
     * @param searchTerm    Search term for title (optional)
     * @param tags          List of tags to search (optional)
     * @param currentUserId The current user ID (can be null for unauthenticated
     *                      users)
     * @param page          Page number
     * @param size          Page size
     * @return PagedResponse of stories matching the search criteria
     */
    public PagedResponse<StoryResponse> searchStoriesByTitleAndTags(String searchTerm, List<String> tags,
            String currentUserId, int page, int size) {
        String requestId = RequestContext.getRequestId();
        log.info("Searching stories by title and tags - searchTerm: {}, tags: {}, user: {} [RequestID: {}]",
                searchTerm, tags, currentUserId, requestId);

        try {
            List<Story> matchingStories = new ArrayList<>();

            // Get all active stories
            List<Story> allActiveStories = storyRepository.findByStatus(Story.StoryStatus.ACTIVE);

            // Filter stories based on search criteria
            matchingStories = allActiveStories.stream()
                    .filter(story -> {
                        boolean matchesTitle = true;
                        boolean matchesTags = true;

                        // Check title match if searchTerm is provided
                        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                            String title = story.getTitle() != null ? story.getTitle().toLowerCase() : "";
                            matchesTitle = title.contains(searchTerm.toLowerCase().trim());
                        }

                        // Check tags match if tags are provided
                        if (tags != null && !tags.isEmpty()) {
                            List<String> storyTags = story.getTags() != null ? story.getTags() : new ArrayList<>();
                            // Check if any of the search tags match any of the story tags
                            matchesTags = tags.stream()
                                    .anyMatch(searchTag -> storyTags.stream()
                                            .anyMatch(storyTag -> storyTag.toLowerCase()
                                                    .contains(searchTag.toLowerCase())));
                        }

                        return matchesTitle && matchesTags;
                    })
                    .collect(Collectors.toList());

            log.info("Found {} stories matching search criteria [RequestID: {}]", matchingStories.size(), requestId);

            // Apply pagination
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, matchingStories.size());

            List<StoryResponse> paginatedStories = matchingStories.stream()
                    .skip(startIndex)
                    .limit(size)
                    .map(story -> {
                        try {
                            User user = userService.getUserEntityById(story.getUserId());

                            if (currentUserId != null) {
                                // For authenticated users
                                boolean likedByMe = likeService.isLiked(currentUserId, story.getId());
                                boolean bookmarkedByMe = bookmarkService.isBookmarked(currentUserId, story.getId());
                                long likeCount = getLikeCount(story.getId());
                                long commentCount = getCommentCount(story.getId());
                                return StoryResponse.fromStory(story, user, likedByMe, bookmarkedByMe, likeCount,
                                        commentCount);
                            } else {
                                // For unauthenticated users
                                long likeCount = getLikeCount(story.getId());
                                long commentCount = getCommentCount(story.getId());
                                return StoryResponse.fromStory(story, user, false, false, likeCount, commentCount);
                            }
                        } catch (Exception e) {
                            log.error("Error creating StoryResponse for search result {} [RequestID: {}]: {}",
                                    story.getId(), requestId, e.getMessage());
                            return null;
                        }
                    })
                    .filter(story -> story != null)
                    .collect(Collectors.toList());

            log.info("Returning {} search results for page {} [RequestID: {}]",
                    paginatedStories.size(), page, requestId);

            return PagedResponse.of(paginatedStories, page, size, matchingStories.size());

        } catch (Exception e) {
            log.error("Error searching stories by title and tags [RequestID: {}]: {}", requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to search stories: " + e.getMessage(), e);
        }
    }

    /**
     * Search stories by content (title and tags) with priority on title matches
     * 
     * @param searchContent Search term to look for in title and tags
     * @param currentUserId The current user ID (can be null for unauthenticated
     *                      users)
     * @param page          Page number
     * @param size          Page size
     * @return PagedResponse of stories matching the search criteria
     */
    public PagedResponse<StoryResponse> searchStoriesByContent(String searchContent, String language,
            String currentUserId, int page, int size) {
        String requestId = RequestContext.getRequestId();
        log.info("Searching stories by content - searchContent: {}, user: {} [RequestID: {}]",
                searchContent, currentUserId, requestId);

        try {
            if (searchContent == null || searchContent.trim().isEmpty()) {
                log.warn("Empty search content provided [RequestID: {}]", requestId);
                return PagedResponse.of(new ArrayList<>(), page, size, 0L);
            }

            String searchTerm = searchContent.toLowerCase().trim();
            List<Story> allActiveStories = storyRepository.findByStatus(Story.StoryStatus.ACTIVE);

            // Separate stories into priority groups
            List<Story> titleMatches = new ArrayList<>();
            List<Story> tagMatches = new ArrayList<>();
            List<Story> otherMatches = new ArrayList<>();

            String normalizedSearchLang = normalizeLanguage(language);

            for (Story story : allActiveStories) {
                // Filter by language if provided
                if (normalizedSearchLang != null && !normalizedSearchLang.isEmpty()) {
                    String storyLang = normalizeLanguage(story.getLanguage());
                    if (storyLang == null || !storyLang.equals(normalizedSearchLang)) {
                        continue;
                    }
                }

                boolean titleMatch = false;
                boolean tagMatch = false;

                // Check title match
                if (story.getTitle() != null && story.getTitle().toLowerCase().contains(searchTerm)) {
                    titleMatch = true;
                    titleMatches.add(story);
                }

                // Check tag matches (only if not already matched by title)
                if (!titleMatch && story.getTags() != null) {
                    for (String tag : story.getTags()) {
                        if (tag != null && tag.toLowerCase().contains(searchTerm)) {
                            tagMatch = true;
                            tagMatches.add(story);
                            break; // Found a tag match, no need to check other tags
                        }
                    }
                }

                // If neither title nor tag match, check other fields (optional)
                if (!titleMatch && !tagMatch) {
                    // You can add more search fields here if needed
                    // For example: content, description, etc.
                }
            }

            // Combine results with priority: title matches first, then tag matches
            List<Story> matchingStories = new ArrayList<>();
            matchingStories.addAll(titleMatches);
            matchingStories.addAll(tagMatches);

            log.info("Search results - Title matches: {}, Tag matches: {}, Total: {} [RequestID: {}]",
                    titleMatches.size(), tagMatches.size(), matchingStories.size(), requestId);

            // Apply pagination
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, matchingStories.size());

            List<StoryResponse> paginatedStories = matchingStories.stream()
                    .skip(startIndex)
                    .limit(size)
                    .map(story -> {
                        try {
                            User user = userService.getUserEntityById(story.getUserId());

                            if (currentUserId != null) {
                                // For authenticated users
                                boolean likedByMe = likeService.isLiked(currentUserId, story.getId());
                                boolean bookmarkedByMe = bookmarkService.isBookmarked(currentUserId, story.getId());
                                long likeCount = getLikeCount(story.getId());
                                long commentCount = getCommentCount(story.getId());
                                return StoryResponse.fromStory(story, user, likedByMe, bookmarkedByMe, likeCount,
                                        commentCount);
                            } else {
                                // For unauthenticated users
                                long likeCount = getLikeCount(story.getId());
                                long commentCount = getCommentCount(story.getId());
                                return StoryResponse.fromStory(story, user, false, false, likeCount, commentCount);
                            }
                        } catch (Exception e) {
                            log.error("Error creating StoryResponse for search result {} [RequestID: {}]: {}",
                                    story.getId(), requestId, e.getMessage());
                            return null;
                        }
                    })
                    .filter(story -> story != null)
                    .collect(Collectors.toList());

            log.info("Returning {} search results for page {} [RequestID: {}]",
                    paginatedStories.size(), page, requestId);

            return PagedResponse.of(paginatedStories, page, size, matchingStories.size());

        } catch (Exception e) {
            log.error("Error searching stories by content [RequestID: {}]: {}", requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to search stories: " + e.getMessage(), e);
        }
    }

    /**
     * Get trending stories by language sorted by trending score (for
     * unauthenticated users)
     * Trending score = (likes * 1.0) + (views * 0.4) + (comments * 0.6)
     * 
     * @param language The language to filter by
     * @param page     Page number
     * @param size     Page size
     * @return PagedResponse of trending stories in the specified language
     */
    public PagedResponse<StoryResponse> getTrendingStoriesByLanguage(String language, int page, int size) {
        String normalizedLang = normalizeLanguage(language);
        String requestId = RequestContext.getRequestId();
        log.info("Getting trending stories by language for unauthenticated user - language: {} [RequestID: {}]",
                normalizedLang, requestId);

        try {
            // Get all active stories by language
            Page<Story> storyPage = storyRepository.findByLanguageAndStatus(normalizedLang, Story.StoryStatus.ACTIVE,
                    PageRequest.of(0, Integer.MAX_VALUE));
            List<Story> allActiveStories = storyPage.getContent();

            // Calculate trending scores and create StoryWithTrendingScore objects
            List<StoryWithTrendingScore> storiesWithScores = allActiveStories.stream()
                    .map(story -> {
                        try {
                            long likeCount = getLikeCount(story.getId());
                            long viewCount = story.getViewCount() != null ? story.getViewCount() : 0L;
                            long commentCount = getCommentCount(story.getId());

                            return StoryWithTrendingScore.fromStory(story, likeCount, viewCount, commentCount);
                        } catch (Exception e) {
                            log.warn("Error calculating trending score for story {} [RequestID: {}]: {}",
                                    story.getId(), requestId, e.getMessage());
                            return null;
                        }
                    })
                    .filter(storyWithScore -> storyWithScore != null)
                    .sorted((s1, s2) -> Double.compare(s2.getTrendingScore(), s1.getTrendingScore())) // Sort by
                                                                                                      // trending score
                                                                                                      // descending
                    .collect(Collectors.toList());

            log.info("Calculated trending scores for {} stories in language {} [RequestID: {}]",
                    storiesWithScores.size(), language, requestId);

            // Apply pagination
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, storiesWithScores.size());

            List<StoryResponse> paginatedStories = storiesWithScores.stream()
                    .skip(startIndex)
                    .limit(size)
                    .map(storyWithScore -> {
                        try {
                            Story story = storyWithScore.getStory();
                            User user = userService.getUserEntityById(story.getUserId());
                            return StoryResponse.fromStory(story, user, false, false,
                                    storyWithScore.getLikeCount(), storyWithScore.getCommentCount());
                        } catch (Exception e) {
                            log.error("Error creating StoryResponse for trending story {} [RequestID: {}]: {}",
                                    storyWithScore.getStory().getId(), requestId, e.getMessage());
                            return null;
                        }
                    })
                    .filter(story -> story != null)
                    .collect(Collectors.toList());

            log.info("Returning {} trending stories in language {} for page {} [RequestID: {}]",
                    paginatedStories.size(), language, page, requestId);

            return PagedResponse.of(paginatedStories, page, size, storiesWithScores.size());

        } catch (Exception e) {
            log.error("Error getting trending stories by language {} [RequestID: {}]: {}", language, requestId,
                    e.getMessage(), e);
            throw new RuntimeException("Failed to get trending stories by language: " + e.getMessage(), e);
        }
    }

    /**
     * Get trending stories by language sorted by trending score (for authenticated
     * users)
     * Trending score = (likes * 1.0) + (views * 0.4) + (comments * 0.6)
     * 
     * @param language      The language to filter by
     * @param currentUserId The current user ID
     * @param page          Page number
     * @param size          Page size
     * @return PagedResponse of trending stories in the specified language with user
     *         context
     */
    public PagedResponse<StoryResponse> getTrendingStoriesByLanguage(String language, String currentUserId, int page,
            int size) {
        String normalizedLang = normalizeLanguage(language);
        String requestId = RequestContext.getRequestId();
        log.info("Getting trending stories by language for user {} - language: {} [RequestID: {}]",
                currentUserId, normalizedLang, requestId);

        try {
            // Get all active stories by language
            Page<Story> storyPage = storyRepository.findByLanguageAndStatus(normalizedLang, Story.StoryStatus.ACTIVE,
                    PageRequest.of(0, Integer.MAX_VALUE));
            List<Story> allActiveStories = storyPage.getContent();

            // Calculate trending scores and create StoryWithTrendingScore objects
            List<StoryWithTrendingScore> storiesWithScores = allActiveStories.stream()
                    .map(story -> {
                        try {
                            long likeCount = getLikeCount(story.getId());
                            long viewCount = story.getViewCount() != null ? story.getViewCount() : 0L;
                            long commentCount = getCommentCount(story.getId());

                            return StoryWithTrendingScore.fromStory(story, likeCount, viewCount, commentCount);
                        } catch (Exception e) {
                            log.warn("Error calculating trending score for story {} [RequestID: {}]: {}",
                                    story.getId(), requestId, e.getMessage());
                            return null;
                        }
                    })
                    .filter(storyWithScore -> storyWithScore != null)
                    .sorted((s1, s2) -> Double.compare(s2.getTrendingScore(), s1.getTrendingScore())) // Sort by
                                                                                                      // trending score
                                                                                                      // descending
                    .collect(Collectors.toList());

            log.info("Calculated trending scores for {} stories in language {} [RequestID: {}]",
                    storiesWithScores.size(), language, requestId);

            // Apply pagination
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, storiesWithScores.size());

            List<StoryResponse> paginatedStories = storiesWithScores.stream()
                    .skip(startIndex)
                    .limit(size)
                    .map(storyWithScore -> {
                        try {
                            Story story = storyWithScore.getStory();
                            User user = userService.getUserEntityById(story.getUserId());
                            boolean likedByMe = likeService.isLiked(currentUserId, story.getId());
                            boolean bookmarkedByMe = bookmarkService.isBookmarked(currentUserId, story.getId());
                            return StoryResponse.fromStory(story, user, likedByMe, bookmarkedByMe,
                                    storyWithScore.getLikeCount(), storyWithScore.getCommentCount());
                        } catch (Exception e) {
                            log.error("Error creating StoryResponse for trending story {} [RequestID: {}]: {}",
                                    storyWithScore.getStory().getId(), requestId, e.getMessage());
                            return null;
                        }
                    })
                    .filter(story -> story != null)
                    .collect(Collectors.toList());

            log.info("Returning {} trending stories in language {} for page {} [RequestID: {}]",
                    paginatedStories.size(), language, page, requestId);

            return PagedResponse.of(paginatedStories, page, size, storiesWithScores.size());

        } catch (Exception e) {
            log.error("Error getting trending stories by language {} for user {} [RequestID: {}]: {}",
                    language, currentUserId, requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to get trending stories by language: " + e.getMessage(), e);
        }
    }

    /**
     * Check if a user has an active story with UPLOADED creation type
     * This is used to determine withdrawal eligibility
     * 
     * @param userId The user ID to check
     * @return WithdrawalEligibilityResponse with eligibility status
     */
    public WithdrawalEligibilityResponse checkWithdrawalEligibility(String userId) {
        String requestId = RequestContext.getRequestId();
        log.info("Checking withdrawal eligibility for user: {} [RequestID: {}]", userId, requestId);

        try {
            boolean hasUploadedActiveStory = storyRepository.existsByUserIdAndStatusAndCreationType(
                    userId,
                    Story.StoryStatus.ACTIVE,
                    Story.CreationType.UPLOADED);

            String message = hasUploadedActiveStory
                    ? "User has uploaded an active story and is eligible for withdrawal"
                    : "no active uploaded stories so not eligible for withdrawal";

            WithdrawalEligibilityResponse response = WithdrawalEligibilityResponse.builder()
                    .userId(userId)
                    .hasUploadedActiveStory(hasUploadedActiveStory)
                    .message(message)
                    .build();

            log.info("Withdrawal eligibility check completed for user: {} - Eligible: {} [RequestID: {}]",
                    userId, hasUploadedActiveStory, requestId);

            return response;

        } catch (Exception e) {
            log.error("Error checking withdrawal eligibility for user {} [RequestID: {}]: {}",
                    userId, requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to check withdrawal eligibility: " + e.getMessage(), e);
        }
    }

    public PagedResponse<StoryResponse> getStories(String userId, int page, int size, String language,
            String category) {
        String requestId = RequestContext.getRequestId();
        log.info("Getting stories for user: {} [RequestID: {}] - language: {}, category: {}", userId, requestId,
                language, category);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Story> storyPage;

            Story.Category storyCategory = null;
            if (category != null && !category.trim().isEmpty()) {
                try {
                    storyCategory = Story.Category.valueOf(category.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid category: {} [RequestID: {}]", category, requestId);
                }
            }

            if (storyCategory != null) {
                if (language != null && !language.trim().isEmpty()) {
                    storyPage = storyRepository.findByStatusAndLanguageAndCategory(
                            Story.StoryStatus.ACTIVE, language, storyCategory, pageable);
                } else {
                    storyPage = storyRepository.findByStatusAndCategory(
                            Story.StoryStatus.ACTIVE, storyCategory, pageable);
                }
            } else if (language != null && !language.trim().isEmpty()) {
                storyPage = storyRepository.findByLanguageAndStatusOrderByCreatedAtDesc(
                        language, Story.StoryStatus.ACTIVE, pageable);
            } else {
                storyPage = storyRepository.findByStatusOrderByCreatedAtDesc(Story.StoryStatus.ACTIVE, pageable);
            }

            List<StoryResponse> stories = storyPage.getContent().stream()
                    .map(story -> {
                        try {
                            User user = userService.getUserEntityById(story.getUserId());
                            boolean likedByMe = likeService.isLiked(userId, story.getId());
                            boolean bookmarkedByMe = bookmarkService.isBookmarked(userId, story.getId());
                            long likeCount = getLikeCount(story.getId());
                            long commentCount = getCommentCount(story.getId());
                            return StoryResponse.fromStory(story, user, likedByMe, bookmarkedByMe, likeCount,
                                    commentCount);
                        } catch (Exception e) {
                            log.error("Error creating StoryResponse for story {} [RequestID: {}]: {}",
                                    story.getId(), requestId, e.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return PagedResponse.of(stories, page, size, storyPage.getTotalElements());

        } catch (Exception e) {
            log.error("Error getting stories [RequestID: {}]: {}", requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to get stories: " + e.getMessage(), e);
        }
    }

    public PagedResponse<StoryResponse> getStories(int page, int size, String language, String category) {
        String requestId = RequestContext.getRequestId();
        log.info("Getting stories (unauthenticated) [RequestID: {}] - language: {}, category: {}", requestId, language,
                category);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Story> storyPage;

            Story.Category storyCategory = null;
            if (category != null && !category.trim().isEmpty()) {
                try {
                    storyCategory = Story.Category.valueOf(category.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid category: {} [RequestID: {}]", category, requestId);
                }
            }

            if (storyCategory != null) {
                if (language != null && !language.trim().isEmpty()) {
                    storyPage = storyRepository.findByStatusAndLanguageAndCategory(
                            Story.StoryStatus.ACTIVE, language, storyCategory, pageable);
                } else {
                    storyPage = storyRepository.findByStatusAndCategory(
                            Story.StoryStatus.ACTIVE, storyCategory, pageable);
                }
            } else if (language != null && !language.trim().isEmpty()) {
                storyPage = storyRepository.findByLanguageAndStatusOrderByCreatedAtDesc(
                        language, Story.StoryStatus.ACTIVE, pageable);
            } else {
                storyPage = storyRepository.findByStatusOrderByCreatedAtDesc(Story.StoryStatus.ACTIVE, pageable);
            }

            List<StoryResponse> stories = storyPage.getContent().stream()
                    .map(story -> {
                        try {
                            User user = userService.getUserEntityById(story.getUserId());
                            long likeCount = getLikeCount(story.getId());
                            long commentCount = getCommentCount(story.getId());
                            return StoryResponse.fromStory(story, user, false, false, likeCount, commentCount);
                        } catch (Exception e) {
                            log.error("Error creating StoryResponse for story {} [RequestID: {}]: {}",
                                    story.getId(), requestId, e.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return PagedResponse.of(stories, page, size, storyPage.getTotalElements());

        } catch (Exception e) {
            log.error("Error getting stories [RequestID: {}]: {}", requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to get stories: " + e.getMessage(), e);
        }
    }

    public PagedResponse<StoryResponse> getStories(String userId, int page, int size) {
        return getStories(userId, page, size, null, null);
    }

    public PagedResponse<StoryResponse> getSimilarStories(String storyId, String userId, int page, int size) {
        return getSimilarStoriesInternal(storyId, null, userId, page, size);
    }

    public PagedResponse<StoryResponse> getSimilarStoriesByLanguage(String storyId, String language, String userId,
            int page, int size) {
        String normalizedLang = normalizeLanguage(language);
        return getSimilarStoriesInternal(storyId, normalizedLang, userId, page, size);
    }

    private PagedResponse<StoryResponse> getSimilarStoriesInternal(String storyId, String language, String userId,
            int page, int size) {
        String requestId = RequestContext.getRequestId();
        try {
            Story sourceStory = storyRepository.findById(storyId).orElse(null);
            if (sourceStory == null) {
                log.warn("Source story not found for similarity search: {} [RequestID: {}]", storyId, requestId);
                return PagedResponse.of(new ArrayList<>(), page, size, 0);
            }

            List<String> tags = sourceStory.getTags();
            if (tags == null || tags.isEmpty()) {
                return PagedResponse.of(new ArrayList<>(), page, size, 0);
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<Story> storyPage;

            if (language != null && !language.trim().isEmpty()) {
                storyPage = storyRepository.findByStatusAndLanguageAndTagsInAndIdNot(Story.StoryStatus.ACTIVE, language,
                        tags, storyId, pageable);
            } else {
                storyPage = storyRepository.findByStatusAndTagsInAndIdNot(Story.StoryStatus.ACTIVE, tags, storyId,
                        pageable);
            }

            List<StoryResponse> stories = storyPage.getContent().stream()
                    .map(story -> {
                        try {
                            User user = userService.getUserEntityById(story.getUserId());
                            boolean likedByMe = userId != null && likeService.isLiked(userId, story.getId());
                            boolean bookmarkedByMe = userId != null
                                    && bookmarkService.isBookmarked(userId, story.getId());
                            long likeCount = getLikeCount(story.getId());
                            long commentCount = getCommentCount(story.getId());
                            return StoryResponse.fromStory(story, user, likedByMe, bookmarkedByMe, likeCount,
                                    commentCount);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return PagedResponse.of(stories, page, size, storyPage.getTotalElements());

        } catch (Exception e) {
            log.error("Error getting similar stories [RequestID: {}]: {}", requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to get similar stories", e);
        }
    }

    private static final Map<String, String> LANGUAGE_CODE_MAP = Map.ofEntries(
            Map.entry("telugu", "te"),
            Map.entry("english", "en"),
            Map.entry("hindi", "hi"),
            Map.entry("tamil", "ta"),
            Map.entry("kannada", "kn"),
            Map.entry("malayalam", "ml"),
            Map.entry("marathi", "mr"),
            Map.entry("gujarati", "gu"),
            Map.entry("bengali", "bn"),
            Map.entry("punjabi", "pa"),
            Map.entry("odia", "or"),
            Map.entry("assamese", "as"));

    private String normalizeLanguage(String lang) {
        if (lang == null)
            return null;
        String lower = lang.toLowerCase().trim();
        return LANGUAGE_CODE_MAP.getOrDefault(lower, lower);
    }
}
