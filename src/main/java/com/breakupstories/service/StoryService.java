package com.breakupstories.service;

import com.breakupstories.dto.LikeRequest;
import com.breakupstories.dto.LikeResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.dto.StoryResponse;
import com.breakupstories.dto.CommentRequest;
import com.breakupstories.dto.CommentResponse;
import com.breakupstories.model.Story;
import com.breakupstories.model.StoryMetadata;
import com.breakupstories.model.User;
import com.breakupstories.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoryService {
    
    private final StoryRepository storyRepository;
    private final UploadService uploadService;
    private final LikeService likeService;
    private final CommentService commentService;
    private final UserService userService;
    private final MockAIService mockAIService;

    public StoryResponse createStory(String userId, MultipartHttpServletRequest request) {
        log.info("Creating story for user: {}", userId);
        
        try {
            // Step 1: Upload the audio to upload service and get the URL
            MultipartFile audioFile = request.getFile("audio");
            if (audioFile == null || audioFile.isEmpty()) {
                throw new IllegalArgumentException("Audio file is required");
            }
            
            log.info("Uploading audio file: {} ({} bytes)", audioFile.getOriginalFilename(), audioFile.getSize());
            var uploadResponse = uploadService.uploadFile(audioFile);
            String audioUrl = uploadResponse.getData().get(0);
            log.info("Audio uploaded successfully: {}", audioUrl);
            
            // Step 2: Create initial story with PROCESSING status
            Story story = Story.builder()
                    .userId(userId)
                    .title("Processing...") // Will be updated after AI processing
                    .audioUrl(audioUrl)
                    .shareLink("") // Will be generated after processing
                    .viewCount(0L)
                    .status(Story.StoryStatus.PROCESSING)
                    .contents(new ArrayList<>()) // Will be populated after AI processing
                    .tags(new ArrayList<>()) // Will be populated after AI processing
                    .emotions(new ArrayList<>()) // Will be populated after AI processing
                    .rejectionReasons(new ArrayList<>()) // Will be populated if AI processing fails
                    .metadata(StoryMetadata.builder().build()) // Will be populated after AI processing
                    .build();
            
            Story savedStory = storyRepository.save(story);
            log.info("Initial story created with ID: {}", savedStory.getId());
            
            // Step 3: Start async AI processing
            mockAIService.processStoryWithAIAsync(savedStory.getId());
            
            return StoryResponse.fromStory(savedStory);
            
        } catch (Exception e) {
            log.error("Error creating story for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to create story: " + e.getMessage(), e);
        }
    }

    
    private void updateStoryStatus(String storyId, Story.StoryStatus status) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found: " + storyId));
        
        story.setStatus(status);
        storyRepository.save(story);
        log.info("Story status updated to {}: {}", status, storyId);
    }
    

    public PagedResponse<StoryResponse> getStories(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> storyPage = storyRepository.findByStatus(Story.StoryStatus.ACTIVE, pageable);
        
        List<StoryResponse> stories = storyPage.getContent().stream()
                .map(story -> {
                    long likeCount = getLikeCount(story.getId());
                    long commentCount = getCommentCount(story.getId());
                    return StoryResponse.fromStory(story, false, likeCount, commentCount);
                })
                .collect(Collectors.toList());
        
        return PagedResponse.of(stories, page, size, storyPage.getTotalElements());
    }
    
    /**
     * Get trending stories sorted by view count
     * @param page Page number
     * @param size Page size
     * @return PagedResponse of trending stories
     */
    public PagedResponse<StoryResponse> getTrendingStories(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> storyPage = storyRepository.findByStatusOrderByViewCountDesc(Story.StoryStatus.ACTIVE, pageable);
        
        List<StoryResponse> stories = storyPage.getContent().stream()
                .map(story -> {
                    long likeCount = getLikeCount(story.getId());
                    long commentCount = getCommentCount(story.getId());
                    return StoryResponse.fromStory(story, false, likeCount, commentCount);
                })
                .collect(Collectors.toList());
        
        return PagedResponse.of(stories, page, size, storyPage.getTotalElements());
    }
    
    /**
     * Get trending stories with user context (includes likedByMe status)
     * @param currentUserId The current user ID
     * @param page Page number
     * @param size Page size
     * @return PagedResponse of trending stories with user context
     */
    public PagedResponse<StoryResponse> getTrendingStories(String currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> storyPage = storyRepository.findByStatusOrderByViewCountDesc(Story.StoryStatus.ACTIVE, pageable);
        
        List<StoryResponse> stories = storyPage.getContent().stream()
                .map(story -> {
                    boolean likedByMe = likeService.isLiked(currentUserId, story.getId());
                    long likeCount = getLikeCount(story.getId());
                    long commentCount = getCommentCount(story.getId());
                    return StoryResponse.fromStory(story, likedByMe, likeCount, commentCount);
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
        Page<Story> storyPage = storyRepository.findByStatus(Story.StoryStatus.ACTIVE, pageable);
        
        List<StoryResponse> stories = storyPage.getContent().stream()
                .map(story -> {
                    boolean likedByMe = likeService.isLiked(currentUserId, story.getId());
                    long likeCount = getLikeCount(story.getId());
                    long commentCount = getCommentCount(story.getId());
                    return StoryResponse.fromStory(story, likedByMe, likeCount, commentCount);
                })
                .collect(Collectors.toList());
        
        return PagedResponse.of(stories, page, size, storyPage.getTotalElements());
    }
    
    /**
     * Get stories by language
     * @param language The language to filter by
     * @param page Page number
     * @param size Page size
     * @return PagedResponse of stories in the specified language
     */
    public PagedResponse<StoryResponse> getStoriesByLanguage(String language, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> storyPage = storyRepository.findByMetadataLanguageAndStatus(language, Story.StoryStatus.ACTIVE, pageable);
        
        List<StoryResponse> stories = storyPage.getContent().stream()
                .map(story -> {
                    long likeCount = getLikeCount(story.getId());
                    long commentCount = getCommentCount(story.getId());
                    return StoryResponse.fromStory(story, false, likeCount, commentCount);
                })
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
        Page<Story> storyPage = storyRepository.findByMetadataLanguageAndStatus(language, Story.StoryStatus.ACTIVE, pageable);
        
        List<StoryResponse> stories = storyPage.getContent().stream()
                .map(story -> {
                    boolean likedByMe = likeService.isLiked(currentUserId, story.getId());
                    long likeCount = getLikeCount(story.getId());
                    long commentCount = getCommentCount(story.getId());
                    return StoryResponse.fromStory(story, likedByMe, likeCount, commentCount);
                })
                .collect(Collectors.toList());
        
        return PagedResponse.of(stories, page, size, storyPage.getTotalElements());
    }
    
    public StoryResponse getStoryById(String storyId, String currentUserId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found with ID: " + storyId));
        
        // Check if the current user liked this story
        boolean likedByMe = likeService.isLiked(currentUserId, storyId);
        
        long likeCount = getLikeCount(storyId);
        long commentCount = getCommentCount(storyId);
        
        return StoryResponse.fromStory(story, likedByMe, likeCount, commentCount);
    }
    
    public StoryResponse getStoryById(String storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found with ID: " + storyId));
        
        long likeCount = getLikeCount(storyId);
        long commentCount = getCommentCount(storyId);
        
        return StoryResponse.fromStory(story, false, false, likeCount, commentCount); // Default to false when no user context
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
        Page<Story> storyPage = storyRepository.findByUserId(userId, pageable);
        
        List<StoryResponse> stories = storyPage.getContent().stream()
                .filter(story -> likeService.isLiked(userId, story.getId()))
                .map(story -> {
                    long likeCount = getLikeCount(story.getId());
                    long commentCount = getCommentCount(story.getId());
                    return StoryResponse.fromStory(story, true, likeCount, commentCount);
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
     * Add a comment to a story
     * @param userId The user ID who is commenting
     * @param storyId The story ID to comment on
     * @param text The comment text
     * @param parentId Optional parent comment ID for replies
     * @return CommentResponse with comment details
     */
    public CommentResponse addComment(String userId, String storyId, String text, String parentId) {
        log.info("User {} attempting to add comment to story {}", userId, storyId);
        
        // Check if story exists and is active
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found with ID: " + storyId));
        
        if (story.getStatus() != Story.StoryStatus.ACTIVE) {
            throw new RuntimeException("Cannot comment on a story that is not active");
        }
        
        CommentRequest request = CommentRequest.builder()
                .storyId(storyId)
                .text(text)
                .parentId(parentId)
                .build();
        
        CommentResponse commentResponse = commentService.createComment(userId, request);
        
        log.info("User {} successfully added comment to story {}", userId, storyId);
        return commentResponse;
    }
    
    /**
     * Update a comment
     * @param userId The user ID who owns the comment
     * @param commentId The comment ID to update
     * @param text The new comment text
     * @return Updated CommentResponse
     */
    public CommentResponse updateComment(String userId, String commentId, String text) {
        log.info("User {} attempting to update comment {}", userId, commentId);
        
        CommentRequest request = CommentRequest.builder()
                .text(text)
                .build();
        
        CommentResponse commentResponse = commentService.updateComment(commentId, userId, request);
        
        log.info("User {} successfully updated comment {}", userId, commentId);
        return commentResponse;
    }
    
    /**
     * Delete a comment
     * @param userId The user ID who owns the comment
     * @param commentId The comment ID to delete
     */
    public void deleteComment(String userId, String commentId) {
        log.info("User {} attempting to delete comment {}", userId, commentId);
        
        commentService.deleteComment(commentId, userId);
        
        log.info("User {} successfully deleted comment {}", userId, commentId);
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
} 