package com.breakupstories.service;

import com.breakupstories.dto.LikeRequest;
import com.breakupstories.dto.LikeResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.dto.StoryResponse;
import com.breakupstories.dto.CommentResponse;
import com.breakupstories.dto.StorySearchRequest;
import com.breakupstories.dto.StorySearchResponse;
import com.breakupstories.dto.StoryWithTrendingScore;
import com.breakupstories.model.Story;
import com.breakupstories.model.StoryDataStore;
import com.breakupstories.model.StoryMetadata;
import com.breakupstories.model.User;
import com.breakupstories.repository.StoryRepository;
import com.breakupstories.repository.StoryDataStoreRepository;
import com.breakupstories.repository.UserRepository;
import com.breakupstories.util.ApplicationContextProvider;
import com.breakupstories.util.RequestContext;
import com.breakupstories.util.ListUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import com.breakupstories.util.TimestampUtil;
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
    private final UserRepository userRepository;
    private final LikeService likeService;
    private final CommentService commentService;
    @Lazy
    private final UserService userService;
    @Lazy
    private final BookmarkService bookmarkService;
    private final StoryDataStoreService storyDataStoreService;
    private final ClientInfoService clientInfoService;
    private final DefaultConfigService defaultConfigService;

    public StoryResponse createStory(User user, MultipartHttpServletRequest request, Map<String,String> uploadMetadata) {
        String requestId = RequestContext.getRequestId();
        String userId = user.getId();
        log.info("Creating story for user: {} [RequestID: {}]", userId, requestId);
        
        try {
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
            
            // Step 3: Create initial Story with UPLOAD_PENDING status
            Story story = Story.builder()
                    .userId(userId)
                    .title("Analyzing....") // Will be updated after audio upload and AI processing
                    .audioUrl(null) // Will be set after async upload
                    .thumbnailUrl(defaultConfigService.getDefaultThumbnailUrl())
                    .viewCount(0L)
                    .status(Story.StoryStatus.UPLOAD_PENDING) // Initial status
                    .contents(new ArrayList<>()) // Will be populated after AI processing
                    .rejectionReasons(new ArrayList<>()) // Will be populated if AI processing fails
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
     * Get trending stories sorted by trending score (for unauthenticated users)
     * Trending score = (likes * 1.0) + (views * 0.4) + (comments * 0.6)
     * @param page Page number
     * @param size Page size
     * @return PagedResponse of trending stories
     */
    public PagedResponse<StoryResponse> getTrendingStories(int page, int size) {
        String requestId = RequestContext.getRequestId();
        log.info("Getting trending stories for unauthenticated user [RequestID: {}]", requestId);
        
        try {
            // Get all active stories
            List<Story> allActiveStories = storyRepository.findByStatus(Story.StoryStatus.ACTIVE);
            
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
            
            log.info("Calculated trending scores for {} stories [RequestID: {}]", storiesWithScores.size(), requestId);
            
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
            
            log.info("Returning {} trending stories for page {} [RequestID: {}]", 
                    paginatedStories.size(), page, requestId);
            
            return PagedResponse.of(paginatedStories, page, size, (long) storiesWithScores.size());
            
        } catch (Exception e) {
            log.error("Error getting trending stories [RequestID: {}]: {}", requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to get trending stories: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get trending stories sorted by trending score (for authenticated users)
     * Trending score = (likes * 1.0) + (views * 0.4) + (comments * 0.6)
     * @param currentUserId The current user ID
     * @param page Page number
     * @param size Page size
     * @return PagedResponse of trending stories with user context
     */
    public PagedResponse<StoryResponse> getTrendingStories(String currentUserId, int page, int size) {
        String requestId = RequestContext.getRequestId();
        log.info("Getting trending stories for user: {} [RequestID: {}]", currentUserId, requestId);
        
        try {
            // Get all active stories
            List<Story> allActiveStories = storyRepository.findByStatus(Story.StoryStatus.ACTIVE);
            
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
            
            log.info("Calculated trending scores for {} stories [RequestID: {}]", storiesWithScores.size(), requestId);
            
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
            
            log.info("Returning {} trending stories for page {} [RequestID: {}]", 
                    paginatedStories.size(), page, requestId);
            
            return PagedResponse.of(paginatedStories, page, size, (long) storiesWithScores.size());
            
        } catch (Exception e) {
            log.error("Error getting trending stories for user {} [RequestID: {}]: {}", 
                    currentUserId, requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to get trending stories: " + e.getMessage(), e);
        }
    }


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
            
            return PagedResponse.of(paginatedStories, page, size, (long) nearbyStories.size());
            
        } catch (com.breakupstories.exception.LocationNotProvidedException e) {
            log.error("Location not provided for nearby stories [RequestID: {}]: {}", requestId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error getting nearby stories efficiently for user {} [RequestID: {}]: {}", 
                    currentUserId, requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to get nearby stories: " + e.getMessage(), e);
        }
    }


    //take story id
    public PagedResponse<StoryResponse> getSimilarStories(String currentUserId, int page, int size) {
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
     * Get stories by language with user context (includes likedByMe status)
     * @param language The language to filter by
     * @param currentUserId The current user ID
     * @param page Page number
     * @param size Page size
     * @return PagedResponse of stories in the specified language with user context
     */
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
    
    public StoryResponse getStoryById(String storyId, String currentUserId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found with ID: " + storyId));
        
        User user = userService.getUserEntityById(story.getUserId());
        
        // Check if the current user liked this story
        boolean likedByMe = likeService.isLiked(currentUserId, storyId);
        boolean bookmarkedByMe = bookmarkService.isBookmarked(currentUserId, storyId);
        
        long likeCount = getLikeCount(storyId);
        long commentCount = getCommentCount(storyId);
        
        return StoryResponse.fromStory(story, user, likedByMe, bookmarkedByMe, likeCount, commentCount);
    }
    
    public StoryResponse getStoryById(String storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found with ID: " + storyId));
        
        User user = userService.getUserEntityById(story.getUserId());
        
        long likeCount = getLikeCount(storyId);
        long commentCount = getCommentCount(storyId);
        
        return StoryResponse.fromStory(story, user, false, false, likeCount, commentCount); // Default to false when no user context
    }
    
    /**
     * Like a story
     * @param userId The user ID who is liking the story
     * @param storyId The story ID to like
     * @return LikeResponse with like details
     */
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
    
    /**
     * Unlike a story
     * @param userId The user ID who is unliking the story
     * @param storyId The story ID to unlike
     */
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
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found with ID: " + storyId));
        
        if (story.getViewCount() == null) {
            story.setViewCount(1L);
        } else {
            story.setViewCount(story.getViewCount() + 1);
        }
        
        storyRepository.save(story);
        log.debug("View count incremented for story: {}", storyId);
        
        // Check for views milestone reward
        RewardService rewardService = ApplicationContextProvider.getBean(RewardService.class);
        rewardService.checkViewsMilestoneReward(storyId);
    }

    /**
     * Get stories by user's preferred language
     * @param currentUserId The current user ID
     * @param page Page number
     * @param size Page size
     * @return PagedResponse of stories in the user's preferred language
     */
    public PagedResponse<StoryResponse> getStoriesByUserPreferredLanguage(String currentUserId, int page, int size) {
        // Get user's preferred language
        User user = userService.getUserEntityById(currentUserId);
        String preferredLanguage = user.getPreferredStoryLanguage();
        
        if (preferredLanguage == null || preferredLanguage.trim().isEmpty()) {
            // If no preferred language, return all stories
            return getStories(currentUserId, page, size);
        }
        
        // Get stories by preferred language
        return getStoriesByLanguage(preferredLanguage, currentUserId, page, size);
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
            
            return PagedResponse.of(paginatedStories, page, size, (long) storiesWithScores.size());
            
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
            
            return PagedResponse.of(paginatedStories, page, size, (long) matchingStories.size());
            
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
            
            return PagedResponse.of(paginatedStories, page, size, (long) matchingStories.size());
            
        } catch (Exception e) {
            log.error("Error searching stories by content [RequestID: {}]: {}", requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to search stories: " + e.getMessage(), e);
        }
    }
} 