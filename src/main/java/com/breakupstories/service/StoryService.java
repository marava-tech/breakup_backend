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

import com.breakupstories.util.ApplicationContextProvider;
import com.breakupstories.util.RequestContext;
import com.breakupstories.util.ListUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import com.breakupstories.service.TrendingCacheService;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

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

    public StoryResponse createStory(User user, MultipartHttpServletRequest request, Map<String,String> uploadMetadata, String creationType) {
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
            
            // Store file bytes as base64 (for demo purposes - in production, use proper file storage)
            try {
                byte[] fileBytes = audioFile.getBytes();
                String base64File = java.util.Base64.getEncoder().encodeToString(fileBytes);
                uploadMetadata.put("audioFileData", base64File);
                log.info("Audio file stored temporarily for background processing [RequestID: {}]", requestId);
            } catch (Exception e) {
                log.error("Failed to store audio file for background processing [RequestID: {}]: {}", requestId, e.getMessage());
                throw new RuntimeException("Failed to store audio file: " + e.getMessage(), e);
            }
            
            // Determine creation type
            Story.CreationType storyCreationType = Story.CreationType.UPLOADED; // Default
            if (creationType != null && !creationType.trim().isEmpty()) {
                try {
                    storyCreationType = Story.CreationType.valueOf(creationType.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid creation type provided: {}. Using default UPLOADED [RequestID: {}]", creationType, requestId);
                }
            }
            
            // Store creation type in upload metadata
            uploadMetadata.put("creationType", storyCreationType.name());
            
            // Step 3: Create initial Story with UPLOAD_PENDING status
            Story story = Story.builder()
                    .userId(userId)
                    .title("Analyzing....") // Will be updated after audio upload and AI processing
                    .audioUrl(null) // Will be set after async upload
                    .thumbnailUrl(defaultConfigService.getDefaultThumbnailUrl())
                    .storyImages(defaultConfigService.getDefaultStoryImages())
                    .viewCount(0L)
                    .status(Story.StoryStatus.UPLOAD_PENDING) // Initial status
                    .contents(new ArrayList<>()) // Will be populated after AI processing
                    .rejectionReasons(new ArrayList<>()) // Will be populated if AI processing fails
                    .creationType(storyCreationType) // Use determined creation type
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

    public StoryResponse createWrittenStory(User user, WrittenStoryRequest request, Map<String,String> uploadMetadata) {
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
            
            // Step 3: Create initial Story with PROCESSING_PENDING status (no upload needed)
            Story story = Story.builder()
                    .userId(userId)
                    .title("Processing....") // Will be updated after AI processing
                    .audioUrl(null) // Will be set after audio generation
                    .thumbnailUrl(defaultConfigService.getDefaultThumbnailUrl())
                    .storyImages(defaultConfigService.getDefaultStoryImages())
                    .viewCount(0L)
                    .status(Story.StoryStatus.PROCESSING_PENDING) // Skip upload step
                    .contents(new ArrayList<>()) // Will be populated after AI processing
                    .rejectionReasons(new ArrayList<>()) // Will be populated if AI processing fails
                    .creationType(Story.CreationType.WRITTEN)
                    .language(request.getLanguage())
                    .build();
            
            Story savedStory = storyRepository.save(story);
            String storyId = savedStory.getId();
            log.info("Initial written story created with ID: {} for user: {} [RequestID: {}]", storyId, userId, requestId);
            
            // Step 5: Create StoryDataStore with PROCESSING_PENDING status and transcription response
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
            log.error("Error creating written story for user {}: {} [RequestID: {}]", userId, e.getMessage(), requestId, e);
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
     * Uses precomputed cache from TrendingCacheService to avoid full collection scan.
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
            log.warn("Trending cache read failed, falling back to full scan [RequestID: {}]: {}", requestId, e.getMessage());
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
            log.warn("Trending cache read failed, falling back to full scan [RequestID: {}]: {}", requestId, e.getMessage());
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
                    return StoryResponse.fromStory(story, user, false, false, sws.getLikeCount(), sws.getCommentCount());
                })
                .collect(Collectors.toList());
        return PagedResponse.of(responses, page, size, withScores.size());
    }


    @Cacheable(value = "stories-type", key = "'FOR_YOU:' + #currentUserId + ':' + #page + ':' + #size")
    public PagedResponse<StoryResponse> getForYouStories(String currentUserId,int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> storyPage = storyRepository.findByStatusOrderByViewCountDesc(Story.StoryStatus.ACTIVE, pageable);

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
     * Get for you stories by language sorted by view count (for authenticated users)
     * @param language The language to filter by
     * @param currentUserId The current user ID
     * @param page Page number
     * @param size Page size
     * @return PagedResponse of for you stories in the specified language with user context
     */
    @Cacheable(value = "stories-type", key = "'FOR_YOU:' + #language + ':' + #currentUserId + ':' + #page + ':' + #size")
    public PagedResponse<StoryResponse> getForYouStoriesByLanguage(String language, String currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> storyPage = storyRepository.findByLanguageAndStatusOrderByViewCountDesc(language, Story.StoryStatus.ACTIVE, pageable);

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
     * Get nearby stories efficiently by filtering StoryDataStore first
     * @param currentUserId The current user ID
     * @param request HttpServletRequest to extract location coordinates
     * @param page Page number
     * @param size Page size
     * @return PagedResponse of nearby stories with user context
     */
    public PagedResponse<StoryResponse> getNearbyStoriesEfficient(String currentUserId, HttpServletRequest request, int page, int size) {
        String requestId = RequestContext.getRequestId();
        log.info("Getting nearby stories efficiently for user: {} [RequestID: {}]", currentUserId, requestId);
        
        try {
            // Extract latitude and longitude from request headers
            String latitudeStr = request.getHeader("X-Latitude");
            String longitudeStr = request.getHeader("X-Longitude");
            
            // Validate location coordinates
            if (latitudeStr == null || longitudeStr == null || 
                latitudeStr.trim().isEmpty() || longitudeStr.trim().isEmpty()) {
                log.error("Location coordinates not provided for nearby stories [RequestID: {}]", requestId);
                throw new com.breakupstories.exception.LocationNotProvidedException(
                    "Location coordinates are required for nearby stories. Please provide X-Latitude and X-Longitude headers.");
            }
            
            // Parse coordinates
            Double userLatitude, userLongitude;
            try {
                userLatitude = Double.parseDouble(latitudeStr.trim());
                userLongitude = Double.parseDouble(longitudeStr.trim());
            } catch (NumberFormatException e) {
                log.error("Invalid coordinate format for nearby stories - lat: {}, long: {} [RequestID: {}]", 
                        latitudeStr, longitudeStr, requestId);
                throw new com.breakupstories.exception.LocationNotProvidedException(
                    "Invalid coordinate format. Please provide valid latitude and longitude values.");
            }
            
            log.info("User location for nearby stories - lat: {}, long: {} [RequestID: {}]", 
                    userLatitude, userLongitude, requestId);
            
            // Step 1: Get all StoryDataStore entries with location coordinates
            List<StoryDataStore> dataStoresWithLocation = storyDataStoreRepository.findStoriesWithLocationCoordinatesAndStatus(
                    StoryDataStore.ProcessingStatus.COMPLETED);
            
            log.info("Found {} StoryDataStore entries with location coordinates [RequestID: {}]", 
                    dataStoresWithLocation.size(), requestId);
            
            // Step 2: Filter by distance and get story IDs
            List<String> nearbyStoryIds = dataStoresWithLocation.stream()
                    .filter(dataStore -> {
                        try {
                            String storyLatStr = dataStore.getUploadMetadata().get("lat");
                            String storyLongStr = dataStore.getUploadMetadata().get("long");
                            
                            if (storyLatStr != null && storyLongStr != null && 
                                !storyLatStr.trim().isEmpty() && !storyLongStr.trim().isEmpty()) {
                                
                                double storyLat = Double.parseDouble(storyLatStr.trim());
                                double storyLong = Double.parseDouble(storyLongStr.trim());
                                
                                // Calculate distance between user and story location
                                double distance = com.breakupstories.util.DistanceCalculator.calculateDistance(
                                        userLatitude, userLongitude, storyLat, storyLong);
                                
                                // Check if story is within 100km radius
                                boolean isNearby = distance <= 100.0;
                                
                                if (isNearby) {
                                    log.debug("Story {} is within 100km radius - distance: {}km [RequestID: {}]", 
                                            dataStore.getStoryId(), distance, requestId);
                                } else {
                                    log.debug("Story {} is outside 100km radius - distance: {}km [RequestID: {}]", 
                                            dataStore.getStoryId(), distance, requestId);
                                }
                                
                                return isNearby;
                            }
                            return false;
                        } catch (Exception e) {
                            log.warn("Error calculating distance for story {} [RequestID: {}]: {}", 
                                    dataStore.getStoryId(), requestId, e.getMessage());
                            return false;
                        }
                    })
                    .map(StoryDataStore::getStoryId)
                    .collect(Collectors.toList());
            
            log.info("Found {} stories within 100km radius [RequestID: {}]", nearbyStoryIds.size(), requestId);
            
            // Step 3: If no nearby stories found, return empty response
            if (nearbyStoryIds.isEmpty()) {
                log.info("No nearby stories found within 100km radius [RequestID: {}]", requestId);
                return PagedResponse.of(new ArrayList<>(), page, size, 0L);
            }
            
            // Step 4: Fetch Story entities for the filtered story IDs
            List<Story> nearbyStories = storyRepository.findByIdInAndStatus(nearbyStoryIds, Story.StoryStatus.ACTIVE);
            
            log.info("Retrieved {} active stories from {} nearby story IDs [RequestID: {}]", 
                    nearbyStories.size(), nearbyStoryIds.size(), requestId);
            
            // Step 5: Convert to StoryResponse with pagination
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, nearbyStories.size());
            
            List<StoryResponse> paginatedStories = nearbyStories.stream()
                    .skip(startIndex)
                    .limit(size)
                    .map(story -> {
                        try {
                            User user = userService.getUserEntityById(story.getUserId());
                            boolean likedByMe = likeService.isLiked(currentUserId, story.getId());
                            boolean bookmarkedByMe = bookmarkService.isBookmarked(currentUserId, story.getId());
                            long likeCount = getLikeCount(story.getId());
                            long commentCount = getCommentCount(story.getId());
                            return StoryResponse.fromStory(story, user, likedByMe, bookmarkedByMe, likeCount, commentCount);
                        } catch (Exception e) {
                            log.error("Error creating StoryResponse for story {} [RequestID: {}]: {}", 
                                    story.getId(), requestId, e.getMessage());
                            return null;
                        }
                    })
                    .filter(story -> story != null)
                    .collect(Collectors.toList());
            
            log.info("Returning {} nearby stories for page {} [RequestID: {}]", 
                    paginatedStories.size(), page, requestId);
            
            return PagedResponse.of(paginatedStories, page, size, nearbyStories.size());
            
        } catch (com.breakupstories.exception.LocationNotProvidedException e) {
            log.error("Location not provided for nearby stories [RequestID: {}]: {}", requestId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error getting nearby stories efficiently for user {} [RequestID: {}]: {}", 
                    currentUserId, requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to get nearby stories: " + e.getMessage(), e);
        }
    }


    /**
     * Returns stories similar to the given story, ranked by tag-overlap score then viewCount.
     * Falls back to trending if the source story has no tags.
     */
    @Cacheable(value = "stories-type", key = "'SIMILAR:' + #storyId + ':' + (#currentUserId != null ? #currentUserId : 'anon') + ':' + #page + ':' + #size")
    public PagedResponse<StoryResponse> getSimilarStories(String storyId, String currentUserId, int page, int size) {
        Story source = storyRepository.findById(storyId).orElse(null);
        if (source == null || source.getTags() == null || source.getTags().isEmpty()) {
            return getTrendingStories(currentUserId, page, size);
        }

        List<String> sourceTags = source.getTags();

        // Stage 1: narrow to ACTIVE stories sharing ≥1 tag (uses compound index)
        MatchOperation matchOp = Aggregation.match(
                Criteria.where("status").is(Story.StoryStatus.ACTIVE)
                        .and("_id").ne(storyId)
                        .and("tags").in(sourceTags));

        // Stage 2: tagScore = |intersection(story.tags, sourceTags)|
        AggregationOperation addTagScore = ctx -> new Document("$addFields",
                new Document("tagScore", new Document("$size",
                        new Document("$ifNull", Arrays.asList(
                                new Document("$setIntersection", Arrays.asList("$tags", sourceTags)),
                                Collections.emptyList())))));

        // Stage 3: highest tag overlap first, break ties by popularity
        SortOperation sortOp = Aggregation.sort(
                Sort.by(Sort.Direction.DESC, "tagScore")
                        .and(Sort.by(Sort.Direction.DESC, "viewCount")));

        SkipOperation skipOp = Aggregation.skip((long) page * size);
        LimitOperation limitOp = Aggregation.limit(size);

        List<Story> results = mongoTemplate.aggregate(
                Aggregation.newAggregation(matchOp, addTagScore, sortOp, skipOp, limitOp),
                "stories", Story.class).getMappedResults();

        long total = storyRepository.countSimilarByTags(storyId, sourceTags);

        List<StoryResponse> responses = results.stream()
                .map(s -> toStoryResponse(s, currentUserId, currentUserId != null))
                .collect(Collectors.toList());

        return PagedResponse.of(responses, page, size, total);
    }

    /**
     * Same as getSimilarStories but restricts candidates to a specific language.
     */
    @Cacheable(value = "stories-type", key = "'SIMILAR:' + #storyId + ':lang:' + #language + ':' + (#currentUserId != null ? #currentUserId : 'anon') + ':' + #page + ':' + #size")
    public PagedResponse<StoryResponse> getSimilarStoriesByLanguage(String storyId, String language, String currentUserId, int page, int size) {
        Story source = storyRepository.findById(storyId).orElse(null);
        if (source == null || source.getTags() == null || source.getTags().isEmpty()) {
            return getTrendingStoriesByLanguage(language, currentUserId, page, size);
        }

        List<String> sourceTags = source.getTags();

        MatchOperation matchOp = Aggregation.match(
                Criteria.where("status").is(Story.StoryStatus.ACTIVE)
                        .and("_id").ne(storyId)
                        .and("language").is(language)
                        .and("tags").in(sourceTags));

        AggregationOperation addTagScore = ctx -> new Document("$addFields",
                new Document("tagScore", new Document("$size",
                        new Document("$ifNull", Arrays.asList(
                                new Document("$setIntersection", Arrays.asList("$tags", sourceTags)),
                                Collections.emptyList())))));

        SortOperation sortOp = Aggregation.sort(
                Sort.by(Sort.Direction.DESC, "tagScore")
                        .and(Sort.by(Sort.Direction.DESC, "viewCount")));

        SkipOperation skipOp = Aggregation.skip((long) page * size);
        LimitOperation limitOp = Aggregation.limit(size);

        List<Story> results = mongoTemplate.aggregate(
                Aggregation.newAggregation(matchOp, addTagScore, sortOp, skipOp, limitOp),
                "stories", Story.class).getMappedResults();

        long total = storyRepository.countSimilarByTagsAndLanguage(storyId, sourceTags, language);

        List<StoryResponse> responses = results.stream()
                .map(s -> toStoryResponse(s, currentUserId, currentUserId != null))
                .collect(Collectors.toList());

        return PagedResponse.of(responses, page, size, total);
    }


    /**
     * Get stories with user context (includes likedByMe status)
     * @param currentUserId The current user ID
     * @param page Page number
     * @param size Page size
     * @return PagedResponse of stories with user context
     */
    public PagedResponse<StoryResponse> getStories(String currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> storyPage = storyRepository.findByUserIdOrderByCreatedAtDesc(currentUserId, pageable);
        
        List<StoryResponse> stories = storyPage.getContent().stream()
                .map(story -> {
                    User user = userService.getUserEntityById(story.getUserId());
                    boolean likedByMe = likeService.isLiked(currentUserId, story.getId());
                    boolean bookmarkedByMe = bookmarkService.isBookmarked(currentUserId, story.getId());
                    long likeCount = getLikeCount(story.getId());
                    long commentCount = getCommentCount(story.getId());
                    return StoryResponse.fromStory(story, user, likedByMe, bookmarkedByMe, likeCount, commentCount);
                })
                .sorted(Comparator.comparing(StoryResponse::getCreatedAt,Comparator.reverseOrder()))
                .collect(Collectors.toList());
        
        return PagedResponse.of(stories, page, size, storyPage.getTotalElements());
    }
    
    /**
     * Get my stories from Story entity only (no StoryDataStore data)
     * @param currentUserId The current user ID
     * @param page Page number
     * @param size Page size
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
                            // Create StoryResponse using only Story entity data
                            return StoryResponse.fromStory(story, user);
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
     * Get my stories from StoryDataStore collection (fetching status from StoryDataStore instead of Story)
     * @param currentUserId The current user ID
     * @param page Page number
     * @param size Page size
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
                            boolean bookmarkedByMe = bookmarkService.isBookmarked(currentUserId, dataStore.getStoryId());
                            long likeCount = getLikeCount(dataStore.getStoryId());
                            long commentCount = getCommentCount(dataStore.getStoryId());
                            
                            // Convert ProcessingStatus to StoryStatus
                            Story.StoryStatus storyStatus = convertProcessingStatusToStoryStatus(dataStore.getProcessingStatus());
                            
                            // Create StoryResponse with status from StoryDataStore
                            // Use status from StoryDataStore

                            return StoryResponse.builder()
                                    .id(dataStore.getStoryId())
                                    .userId(dataStore.getUserId())
                                    .username(user != null ? user.getName() : null)
                                    .title(dataStore.getTitle() != null ? dataStore.getTitle() : (story != null ? story.getTitle() : "Processing..."))
                                    .audioUrl(dataStore.getAudioUrl() != null ? dataStore.getAudioUrl() : (story != null ? story.getAudioUrl() : null))
                                    .thumbnailUrl(story != null ? story.getThumbnailUrl() : null)
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
     * @param language The language to filter by
     * @param currentUserId The current user ID
     * @param page Page number
     * @param size Page size
     * @return PagedResponse of stories in the specified language with user context
     */
    @Cacheable(value = "stories-feed", key = "#language + ':' + #currentUserId + ':' + #page + ':' + #size")
    public PagedResponse<StoryResponse> getStoriesByLanguage(String language, String currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> storyPage = storyRepository.findByLanguageAndStatus(language, Story.StoryStatus.ACTIVE, pageable);
        
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
     * @param language The language to filter by
     * @param page Page number
     * @param size Page size
     * @return PagedResponse of stories in the specified language
     */
    @Cacheable(value = "stories-feed", key = "#language + ':anon:' + #page + ':' + #size")
    public PagedResponse<StoryResponse> getStoriesByLanguage(String language, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> storyPage = storyRepository.findByLanguageAndStatus(language, Story.StoryStatus.ACTIVE, pageable);
        
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
        
        return StoryResponse.fromStory(story, user, false, false, likeCount, commentCount); // Default to false when no user context
    }
    
    /**
     * Like a story
     * @param userId The user ID who is liking the story
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
        
        // Check for likes milestone reward
        RewardService rewardService = ApplicationContextProvider.getBean(RewardService.class);
        rewardService.checkLikesMilestoneReward(storyId);
        
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
     * @param userId The user ID who is unliking the story
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
     * @param storyId The story ID
     * @return Number of likes
     */
    public long getLikeCount(String storyId) {
        return likeService.getLikeCount(storyId);
    }
    
    /**
     * Get stories liked by a user
     * @param userId The user ID
     * @param page Page number
     * @param size Page size
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
     * @param storyId The story ID
     * @param page Page number
     * @param size Page size
     * @return PagedResponse of comments with nested replies
     */
    public PagedResponse<CommentResponse> getStoryComments(String storyId, int page, int size) {
        return commentService.getCommentsByStory(storyId, page, size);
    }
    
    /**
     * Get all comments for a story (including replies)
     * @param storyId The story ID
     * @return List of all comments with nested replies
     */
    public List<CommentResponse> getAllStoryComments(String storyId) {
        return commentService.getAllCommentsByStory(storyId);
    }
    
    /**
     * Get comment count for a story
     * @param storyId The story ID
     * @return Total number of comments (including replies)
     */
    public long getCommentCount(String storyId) {
        return commentService.getCommentCount(storyId);
    }

    
    /**
     * Get Story entity by ID
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
        
        // Check for views milestone reward (async-safe: reads after update)
        RewardService rewardService = ApplicationContextProvider.getBean(RewardService.class);
        rewardService.checkViewsMilestoneReward(storyId);
    }

    /**
     * Async view count increment — removes synchronous MongoDB write from read path.
     * Fire-and-forget from controller after returning story response.
     */
    /**
     * Async view count — increments in Redis; batched flush to MongoDB every 60s.
     */
    @Async("storyOpsExecutor")
    public void incrementViewCountAsync(String storyId) {
        viewCountBatchService.incrementInRedis(storyId);
    }

    /**
     * Get latest stories with cursor-based pagination (efficient for deep pages).
     * Pass cursor from previous response's nextCursor. Size+1 fetched to determine hasMore.
     */
    public PagedResponse<StoryResponse> getLatestStoriesWithCursor(String currentUserId, String cursor, int size) {
        java.time.LocalDateTime before = cursor != null && !cursor.isBlank()
                ? java.time.LocalDateTime.parse(cursor) : null;
        if (before == null) {
            List<Story> stories = storyRepository.findByStatusOrderByCreatedAtDesc(
                    Story.StoryStatus.ACTIVE, PageRequest.of(0, size + 1)).getContent();
            return buildCursorResponse(stories, size, currentUserId, true);
        }
        List<Story> stories = storyRepository.findByStatusAndCreatedAtBeforeOrderByCreatedAtDesc(
                Story.StoryStatus.ACTIVE, before, PageRequest.of(0, size + 1));
        return buildCursorResponse(stories, size, currentUserId, true);
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
     * @param currentUserId The current user ID
     * @param page Page number
     * @param size Page size
     * @return PagedResponse of latest stories with user context
     */
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
     * Get latest stories sorted by creation date (newest first) - for unauthenticated users
     * @param page Page number
     * @param size Page size
     * @return PagedResponse of latest stories
     */
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
     * Get latest stories by language sorted by creation date (newest first) - for unauthenticated users
     * @param language The language to filter by
     * @param page Page number
     * @param size Page size
     * @return PagedResponse of latest stories in the specified language
     */
    public PagedResponse<StoryResponse> getLatestStoriesByLanguage(String language, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> storyPage = storyRepository.findByLanguageAndStatusOrderByCreatedAtDesc(language, Story.StoryStatus.ACTIVE, pageable);
        
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
     * Get latest stories by language sorted by creation date (newest first) - for authenticated users
     * @param language The language to filter by
     * @param currentUserId The current user ID
     * @param page Page number
     * @param size Page size
     * @return PagedResponse of latest stories in the specified language with user context
     */
    public PagedResponse<StoryResponse> getLatestStoriesByLanguage(String language, String currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> storyPage = storyRepository.findByLanguageAndStatusOrderByCreatedAtDesc(language, Story.StoryStatus.ACTIVE, pageable);
        
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
     * Get voice stories (stories with UPLOADED creation type) sorted by creation date (newest first) - for authenticated users
     * @param currentUserId The current user ID
     * @param page Page number
     * @param size Page size
     * @return PagedResponse of voice stories with user context
     */
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
     * Get voice stories (stories with UPLOADED creation type) sorted by creation date (newest first) - for unauthenticated users
     * @param page Page number
     * @param size Page size
     * @return PagedResponse of voice stories
     */
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
     * Get voice stories by language (stories with UPLOADED creation type) sorted by creation date (newest first) - for unauthenticated users
     * @param language The language to filter by
     * @param page Page number
     * @param size Page size
     * @return PagedResponse of voice stories in the specified language
     */
    public PagedResponse<StoryResponse> getVoiceStoriesByLanguage(String language, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> storyPage = storyRepository.findByCreationTypeAndStatusAndLanguageOrderByCreatedAtDesc(
                Story.CreationType.UPLOADED, Story.StoryStatus.ACTIVE, language, pageable);
        
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
     * Get voice stories by language (stories with UPLOADED creation type) sorted by creation date (newest first) - for authenticated users
     * @param language The language to filter by
     * @param currentUserId The current user ID
     * @param page Page number
     * @param size Page size
     * @return PagedResponse of voice stories in the specified language with user context
     */
    public PagedResponse<StoryResponse> getVoiceStoriesByLanguage(String language, String currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> storyPage = storyRepository.findByCreationTypeAndStatusAndLanguageOrderByCreatedAtDesc(
                Story.CreationType.UPLOADED, Story.StoryStatus.ACTIVE, language, pageable);
        
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
     * Search stories by title contains (case-insensitive)
     * @param titleContains Text to search in story titles
     * @param currentUserId The current user ID
     * @param page Page number
     * @param size Page size
     * @return PagedResponse of stories with matching titles
     */
    public PagedResponse<StoryResponse> searchStoriesByTitle(String titleContains, String currentUserId, int page, int size) {
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
     * Search stories by title contains (case-insensitive) - for unauthenticated users
     * @param titleContains Text to search in story titles
     * @param page Page number
     * @param size Page size
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
     * @param existingMetadata Existing metadata (can be null)
     * @param newLocations New locations to add
     * @param newNames New names to add
     * @param newPincodes New pincodes to add
     * @param newState New state (overwrites if not null)
     * @param newDistrict New district (overwrites if not null)
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
     * Get nearby stories within 100km radius based on user's location
     * @param currentUserId The current user ID
     * @param request HttpServletRequest to extract location coordinates
     * @param page Page number
     * @param size Page size
     * @return PagedResponse of nearby stories with user context
     */
    public PagedResponse<StoryResponse> getNearbyStories(String currentUserId, HttpServletRequest request, int page, int size) {
        // Use the efficient implementation
        return getNearbyStoriesEfficient(currentUserId, request, page, size);
    }

    /**
     * Get trending stories with custom weights
     * @param currentUserId The current user ID (can be null for unauthenticated users)
     * @param page Page number
     * @param size Page size
     * @param likesWeight Weight for likes
     * @param viewsWeight Weight for views
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
                            double trendingScore = com.breakupstories.util.TrendingScoreCalculator.calculateTrendingScore(
                                    likeCount, viewCount, commentCount, likesWeight, viewsWeight, commentsWeight);
                            
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
                    .sorted((s1, s2) -> Double.compare(s2.getTrendingScore(), s1.getTrendingScore())) // Sort by trending score descending
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
     * @param searchTerm Search term for title (optional)
     * @param tags List of tags to search (optional)
     * @param currentUserId The current user ID (can be null for unauthenticated users)
     * @param page Page number
     * @param size Page size
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
                                            .anyMatch(storyTag -> storyTag.toLowerCase().contains(searchTag.toLowerCase())));
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
                                return StoryResponse.fromStory(story, user, likedByMe, bookmarkedByMe, likeCount, commentCount);
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
     * @param searchContent Search term to look for in title and tags
     * @param currentUserId The current user ID (can be null for unauthenticated users)
     * @param page Page number
     * @param size Page size
     * @return PagedResponse of stories matching the search criteria
     */
    public PagedResponse<StoryResponse> searchStoriesByContent(String searchContent, 
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
            
            for (Story story : allActiveStories) {
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
                                return StoryResponse.fromStory(story, user, likedByMe, bookmarkedByMe, likeCount, commentCount);
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
     * Get trending stories by language sorted by trending score (for unauthenticated users)
     * Trending score = (likes * 1.0) + (views * 0.4) + (comments * 0.6)
     * @param language The language to filter by
     * @param page Page number
     * @param size Page size
     * @return PagedResponse of trending stories in the specified language
     */
    public PagedResponse<StoryResponse> getTrendingStoriesByLanguage(String language, int page, int size) {
        String requestId = RequestContext.getRequestId();
        log.info("Getting trending stories by language for unauthenticated user - language: {} [RequestID: {}]", language, requestId);
        
        try {
            // Get all active stories by language
            Page<Story> storyPage = storyRepository.findByLanguageAndStatus(language, Story.StoryStatus.ACTIVE, PageRequest.of(0, Integer.MAX_VALUE));
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
                    .sorted((s1, s2) -> Double.compare(s2.getTrendingScore(), s1.getTrendingScore())) // Sort by trending score descending
                    .collect(Collectors.toList());
            
            log.info("Calculated trending scores for {} stories in language {} [RequestID: {}]", storiesWithScores.size(), language, requestId);
            
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
            log.error("Error getting trending stories by language {} [RequestID: {}]: {}", language, requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to get trending stories by language: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get trending stories by language sorted by trending score (for authenticated users)
     * Trending score = (likes * 1.0) + (views * 0.4) + (comments * 0.6)
     * @param language The language to filter by
     * @param currentUserId The current user ID
     * @param page Page number
     * @param size Page size
     * @return PagedResponse of trending stories in the specified language with user context
     */
    public PagedResponse<StoryResponse> getTrendingStoriesByLanguage(String language, String currentUserId, int page, int size) {
        String requestId = RequestContext.getRequestId();
        log.info("Getting trending stories by language for user: {} - language: {} [RequestID: {}]", currentUserId, language, requestId);
        
        try {
            // Get all active stories by language
            Page<Story> storyPage = storyRepository.findByLanguageAndStatus(language, Story.StoryStatus.ACTIVE, PageRequest.of(0, Integer.MAX_VALUE));
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
                    .sorted((s1, s2) -> Double.compare(s2.getTrendingScore(), s1.getTrendingScore())) // Sort by trending score descending
                    .collect(Collectors.toList());
            
            log.info("Calculated trending scores for {} stories in language {} [RequestID: {}]", storiesWithScores.size(), language, requestId);
            
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
                Story.CreationType.UPLOADED
            );
            
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
} 