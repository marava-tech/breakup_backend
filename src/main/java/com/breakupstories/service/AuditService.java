package com.breakupstories.service;

import com.breakupstories.dto.AuditRequest;
import com.breakupstories.dto.AuditResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.model.Audit;
import com.breakupstories.model.Story;
import com.breakupstories.repository.AuditRepository;
import com.breakupstories.repository.StoryRepository;
import com.breakupstories.util.TimestampUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditService {
    
    private final AuditRepository auditRepository;
    private final StoryRepository storyRepository;
    
    public AuditResponse createAudit(AuditRequest request) {
        Audit audit = Audit.builder()
                .userId(request.getUserId())
                .entityType(request.getEntityType())
                .actionType(request.getActionType())
                .entityId(request.getEntityId())
                .userAgent(request.getUserAgent())
                .ipAddress(request.getIpAddress())
                .sessionId(request.getSessionId())
                .metadata(request.getMetadata())
                .build();
        
        Audit savedAudit = auditRepository.save(audit);
        return AuditResponse.fromAudit(savedAudit);
    }
    
    public void logAudit(String userId, Audit.EntityType entityType, Audit.ActionType actionType, String entityId) {
        logAudit(userId, entityType, actionType, entityId, null, null, null, null);
    }
    
    public void logAudit(String userId, Audit.EntityType entityType, Audit.ActionType actionType, String entityId, 
                        String userAgent, String ipAddress, String sessionId, Map<String, Object> metadata) {
        Audit audit = Audit.builder()
                .userId(userId)
                .entityType(entityType)
                .actionType(actionType)
                .entityId(entityId)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .sessionId(sessionId)
                .metadata(metadata)
                .build();
        
        auditRepository.save(audit);
    }
    
    // Convenience methods for common interactions
    public void logStoryView(String userId, String storyId, String userAgent, String ipAddress, String sessionId) {
        Map<String, Object> metadata = Map.of("interaction_type", "story_view");
        logAudit(userId, Audit.EntityType.STORY, Audit.ActionType.VIEW, storyId, userAgent, ipAddress, sessionId, metadata);
    }
    
    /**
     * Log story view with additional metadata about whether it's the user's own story
     * @param userId The user ID
     * @param storyId The story ID
     * @param userAgent The user agent
     * @param ipAddress The IP address
     * @param sessionId The session ID
     * @param isOwnStory Whether the user is viewing their own story
     */
    public void logStoryView(String userId, String storyId, String userAgent, String ipAddress, String sessionId, boolean isOwnStory) {
        Map<String, Object> metadata = Map.of(
            "interaction_type", "story_view",
            "is_own_story", isOwnStory
        );
        logAudit(userId, Audit.EntityType.STORY, Audit.ActionType.VIEW, storyId, userAgent, ipAddress, sessionId, metadata);
    }
    
    public void logStoryLike(String userId, String storyId, String userAgent, String ipAddress, String sessionId) {
        Map<String, Object> metadata = Map.of("interaction_type", "story_like");
        logAudit(userId, Audit.EntityType.STORY, Audit.ActionType.LIKE, storyId, userAgent, ipAddress, sessionId, metadata);
    }
    
    public void logStoryUnlike(String userId, String storyId, String userAgent, String ipAddress, String sessionId) {
        Map<String, Object> metadata = Map.of("interaction_type", "story_unlike");
        logAudit(userId, Audit.EntityType.STORY, Audit.ActionType.UNLIKE, storyId, userAgent, ipAddress, sessionId, metadata);
    }
    
    public void logCommentCreate(String userId, String storyId, String userAgent, String ipAddress, String sessionId) {
        Map<String, Object> metadata = Map.of(
            "interaction_type", "comment_create",
            "story_id", storyId
        );
        logAudit(userId, Audit.EntityType.COMMENT, Audit.ActionType.CREATE, storyId, userAgent, ipAddress, sessionId, metadata);
    }
    

    
    public void logBookmarkCreate(String userId, String storyId, String userAgent, String ipAddress, String sessionId) {
        Map<String, Object> metadata = Map.of("interaction_type", "bookmark_create");
        logAudit(userId, Audit.EntityType.BOOKMARK, Audit.ActionType.CREATE, storyId, userAgent, ipAddress, sessionId, metadata);
    }
    
    public void logBookmarkDelete(String userId, String storyId, String userAgent, String ipAddress, String sessionId) {
        Map<String, Object> metadata = Map.of("interaction_type", "bookmark_delete");
        logAudit(userId, Audit.EntityType.BOOKMARK, Audit.ActionType.DELETE, storyId, userAgent, ipAddress, sessionId, metadata);
    }
    
    public PagedResponse<AuditResponse> getAudits(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Audit> auditPage = auditRepository.findAll(pageable);
        
        List<AuditResponse> audits = auditPage.getContent().stream()
                .map(AuditResponse::fromAudit)
                .collect(Collectors.toList());
        
        return PagedResponse.of(audits, page, size, auditPage.getTotalElements());
    }
    
    public PagedResponse<AuditResponse> getAuditsByUser(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Audit> auditPage = auditRepository.findByUserId(userId, pageable);
        
        List<AuditResponse> audits = auditPage.getContent().stream()
                .map(AuditResponse::fromAudit)
                .collect(Collectors.toList());
        
        return PagedResponse.of(audits, page, size, auditPage.getTotalElements());
    }
    
    public PagedResponse<AuditResponse> getAuditsByEntityType(Audit.EntityType entityType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Audit> auditPage = auditRepository.findByEntityType(entityType, pageable);
        
        List<AuditResponse> audits = auditPage.getContent().stream()
                .map(AuditResponse::fromAudit)
                .collect(Collectors.toList());
        
        return PagedResponse.of(audits, page, size, auditPage.getTotalElements());
    }
    
    public PagedResponse<AuditResponse> getAuditsByEntityId(String entityId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Audit> auditPage = auditRepository.findByEntityId(entityId, pageable);
        
        List<AuditResponse> audits = auditPage.getContent().stream()
                .map(AuditResponse::fromAudit)
                .collect(Collectors.toList());
        
        return PagedResponse.of(audits, page, size, auditPage.getTotalElements());
    }
    
    public PagedResponse<AuditResponse> getAuditsByActionType(Audit.ActionType actionType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Audit> auditPage = auditRepository.findByActionType(actionType, pageable);
        
        List<AuditResponse> audits = auditPage.getContent().stream()
                .map(AuditResponse::fromAudit)
                .collect(Collectors.toList());
        
        return PagedResponse.of(audits, page, size, auditPage.getTotalElements());
    }
    
    public PagedResponse<AuditResponse> getAuditsByUserAndEntityType(String userId, Audit.EntityType entityType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Audit> auditPage = auditRepository.findByUserIdAndEntityType(userId, entityType, pageable);
        
        List<AuditResponse> audits = auditPage.getContent().stream()
                .map(AuditResponse::fromAudit)
                .collect(Collectors.toList());
        
        return PagedResponse.of(audits, page, size, auditPage.getTotalElements());
    }
    
    public AuditResponse getAuditById(String auditId) {
        Audit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new RuntimeException("Audit not found with ID: " + auditId));
        
        return AuditResponse.fromAudit(audit);
    }
    
    public void deleteAudit(String auditId) {
        if (!auditRepository.existsById(auditId)) {
            throw new RuntimeException("Audit not found with ID: " + auditId);
        }
        
        auditRepository.deleteById(auditId);
    }
    
    public void logNotificationView(String userId, String userAgent, String ipAddress, String sessionId) {
        Map<String, Object> metadata = Map.of("interaction_type", "notification_view");
        logAudit(userId, Audit.EntityType.NOTIFICATION, Audit.ActionType.VIEW, userId, userAgent, ipAddress, sessionId, metadata);
    }
    
    public Long getLastNotificationView(String userId) {
        return auditRepository.findTopByUserIdAndEntityTypeAndActionTypeOrderByCreatedAtDesc(
                userId, Audit.EntityType.NOTIFICATION, Audit.ActionType.VIEW)
                .map(Audit::getCreatedAt)
                .orElse(null);
    }
    
    /**
     * Get story IDs for the current user
     * @param userId The user ID
     * @return List of story IDs owned by the user
     */
    private List<String> getUserStoryIds(String userId) {
        Page<Story> userStories = storyRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, Integer.MAX_VALUE));
        return userStories.getContent().stream()
                .map(Story::getId)
                .collect(Collectors.toList());
    }
    
    /**
     * Get total likes count for stories owned by the current user
     * Only counts likes that don't have corresponding unlikes
     * @param userId The user ID
     * @param since Optional timestamp to count likes since
     * @return Total likes count for user's stories
     */
    public long getLikesCountForUserStories(String userId, Long since) {
        List<String> userStoryIds = getUserStoryIds(userId);
        if (userStoryIds.isEmpty()) {
            return 0;
        }
        
        if (since == null) {
            return getEffectiveLikesCountForStories(userStoryIds);
        }
        return getEffectiveLikesCountForStoriesSince(userStoryIds, since);
    }
    
    /**
     * Get effective likes count for multiple stories (likes without corresponding unlikes)
     * @param storyIds List of story IDs
     * @return Effective likes count
     */
    private long getEffectiveLikesCountForStories(List<String> storyIds) {
        List<Audit> allLikes = auditRepository.findLikesByEntityIdsAndEntityType(storyIds, Audit.EntityType.STORY);
        List<Audit> allUnlikes = auditRepository.findUnlikesByEntityIdsAndEntityType(storyIds, Audit.EntityType.STORY);
        
        return calculateEffectiveLikes(allLikes, allUnlikes);
    }
    
    /**
     * Get effective likes count for multiple stories since a given date (likes without corresponding unlikes)
     * @param storyIds List of story IDs
     * @param since Timestamp to count since
     * @return Effective likes count
     */
    private long getEffectiveLikesCountForStoriesSince(List<String> storyIds, Long since) {
        List<Audit> allLikes = auditRepository.findLikesByEntityIdsAndEntityTypeAndCreatedAtAfter(storyIds, Audit.EntityType.STORY, since);
        List<Audit> allUnlikes = auditRepository.findUnlikesByEntityIdsAndEntityType(storyIds, Audit.EntityType.STORY);
        
        return calculateEffectiveLikes(allLikes, allUnlikes);
    }
    
    /**
     * Calculate effective likes by removing likes that have corresponding unlikes
     * @param likes List of like audits
     * @param unlikes List of unlike audits
     * @return Effective likes count
     */
    private long calculateEffectiveLikes(List<Audit> likes, List<Audit> unlikes) {
        // Filter out any records with null userId or entityId to prevent grouping errors
        List<Audit> validLikes = likes.stream()
                .filter(like -> like.getUserId() != null && like.getEntityId() != null)
                .collect(Collectors.toList());
        
        List<Audit> validUnlikes = unlikes.stream()
                .filter(unlike -> unlike.getUserId() != null && unlike.getEntityId() != null)
                .collect(Collectors.toList());
        
        // Create a map of user -> entity -> latest unlike timestamp for quick lookup
        // Handle multiple unlikes by taking the latest one
        Map<String, Map<String, Long>> userEntityUnlikes = validUnlikes.stream()
                .collect(Collectors.groupingBy(
                    Audit::getUserId,
                    Collectors.groupingBy(
                        Audit::getEntityId,
                        Collectors.collectingAndThen(
                            Collectors.maxBy(java.util.Comparator.comparing(Audit::getCreatedAt)),
                            opt -> opt.map(Audit::getCreatedAt).orElse(null)
                        )
                    )
                ));
        
        // Count likes that don't have corresponding unlikes
        return validLikes.stream()
                .filter(like -> {
                    String userId = like.getUserId();
                    String entityId = like.getEntityId();
                    Long likeTime = like.getCreatedAt();
                    
                    // Additional null checks for safety
                    if (userId == null || entityId == null || likeTime == null) {
                        return false; // Skip invalid records
                    }
                    
                    // Check if this user has unliked this entity
                    Map<String, Long> userUnlikes = userEntityUnlikes.get(userId);
                    if (userUnlikes == null) {
                        return true; // No unlikes by this user, so like is valid
                    }
                    
                    Long unlikeTime = userUnlikes.get(entityId);
                    if (unlikeTime == null) {
                        return true; // No unlike for this entity by this user, so like is valid
                    }
                    
                    // If unlike happened after like, then like is invalid
                    return unlikeTime < likeTime;
                })
                .count();
    }
    
    /**
     * Get total views count for stories owned by the current user
     * Excludes views where the viewer is the same as the story owner
     * Excludes views that are within 1-minute cooldown period
     * @param userId The user ID
     * @param since Optional timestamp to count views since
     * @return Total views count for user's stories (excluding self-views and cooldown views)
     */
    public long getViewsCountForUserStories(String userId, Long since) {
        List<String> userStoryIds = getUserStoryIds(userId);
        if (userStoryIds.isEmpty()) {
            return 0;
        }
        
        // Get all view audits for user's stories
        List<Audit> viewAudits;
        if (since == null) {
            viewAudits = auditRepository.findByEntityIdInAndEntityTypeAndActionType(
                    userStoryIds, Audit.EntityType.STORY, Audit.ActionType.VIEW);
        } else {
            viewAudits = auditRepository.findByEntityIdInAndEntityTypeAndActionTypeAndCreatedAtAfter(
                    userStoryIds, Audit.EntityType.STORY, Audit.ActionType.VIEW, since);
        }
        
        // Filter out self-views and cooldown views
        long validViewCount = viewAudits.stream()
                .filter(audit -> {
                    String viewerId = audit.getUserId();
                    String storyId = audit.getEntityId();
                    
                    // Get the story to check if viewer is the owner
                    try {
                        Story story = storyRepository.findById(storyId).orElse(null);
                        if (story != null) {
                            // Exclude self-views
                            if (viewerId.equals(story.getUserId())) {
                                return false;
                            }
                            
                            // Check for cooldown views (1 minute)
                            long oneMinuteAgo = TimestampUtil.currentEpochMillis() - (1 * 60 * 1000);
                            if (audit.getCreatedAt() != null && audit.getCreatedAt() < oneMinuteAgo) {
                                // Check if there's a more recent view by the same user/IP
                                List<Audit> recentViews;
                                if (viewerId.contains(".")) {
                                    // IP address (unauthenticated user)
                                    recentViews = auditRepository.findByIpAddressAndEntityIdAndEntityTypeAndActionTypeAndCreatedAtAfter(
                                            viewerId, storyId, Audit.EntityType.STORY, Audit.ActionType.VIEW, oneMinuteAgo);
                                } else {
                                    // User ID (authenticated user)
                                    recentViews = auditRepository.findByUserIdAndEntityIdAndEntityTypeAndActionTypeAndCreatedAtAfter(
                                            viewerId, storyId, Audit.EntityType.STORY, Audit.ActionType.VIEW, oneMinuteAgo);
                                }
                                
                                // If there's a more recent view, exclude this one (cooldown)
                                if (!recentViews.isEmpty()) {
                                    return false;
                                }
                            }
                            
                            return true;
                        }
                    } catch (Exception e) {
                        // If there's an error getting the story, exclude the view to be safe
                        return false;
                    }
                    return false;
                })
                .count();
        
        return validViewCount;
    }
    
    /**
     * Get total comments count for stories owned by the current user
     * @param userId The user ID
     * @param since Optional timestamp to count comments since
     * @return Total comments count for user's stories
     */
    public long getCommentsCountForUserStories(String userId, Long since) {
        List<String> userStoryIds = getUserStoryIds(userId);
        if (userStoryIds.isEmpty()) {
            return 0;
        }

        if (since == null) {
            return auditRepository.countByEntityIdInAndEntityTypeAndActionType(
                    userStoryIds, Audit.EntityType.COMMENT, Audit.ActionType.CREATE);
        }
        return auditRepository.countByEntityIdInAndEntityTypeAndActionTypeAndCreatedAtAfter(
                userStoryIds, Audit.EntityType.COMMENT, Audit.ActionType.CREATE, since);
    }
    
    /**
     * Get detailed statistics for stories owned by the current user
     * @param userId The user ID
     * @return Map containing likes, views, and comments counts
     */
    public Map<String, Long> getStoryStatisticsForUser(String userId) {
        return Map.of(
            "likes", getLikesCountForUserStories(userId, null),
            "views", getViewsCountForUserStories(userId, null),
            "comments", getCommentsCountForUserStories(userId, null)
        );
    }
    
    /**
     * Get story-wise statistics for stories owned by the current user
     * @param userId The user ID
     * @param since Optional timestamp to count interactions since
     * @return Map with story ID as key and statistics as value
     */
    public Map<String, Map<String, Long>> getStoryWiseStatisticsForUser(String userId, Long since) {
        List<String> userStoryIds = getUserStoryIds(userId);
        if (userStoryIds.isEmpty()) {
            return Map.of();
        }
        
        return userStoryIds.stream()
                .collect(Collectors.toMap(
                    storyId -> storyId,
                    storyId -> Map.of(
                        "likes", getLikesCountForStory(storyId, since),
                        "views", getViewsCountForStory(storyId, since),
                        "comments", getCommentsCountForStory(storyId, since)
                    )
                ));
    }
    
    /**
     * Get likes count for a specific story
     * Only counts likes that don't have corresponding unlikes
     * @param storyId The story ID
     * @param since Optional timestamp to count likes since
     * @return Likes count for the story
     */
    private long getLikesCountForStory(String storyId, Long since) {
        if (since == null) {
            return getEffectiveLikesCountForStory(storyId);
        }
        return getEffectiveLikesCountForStorySince(storyId, since);
    }
    
    /**
     * Get effective likes count for a single story (likes without corresponding unlikes)
     * @param storyId The story ID
     * @return Effective likes count
     */
    public long getEffectiveLikesCountForStory(String storyId) {
        List<Audit> likes = auditRepository.findLikesByEntityIdAndEntityType(storyId, Audit.EntityType.STORY);
        List<Audit> unlikes = auditRepository.findUnlikesByEntityIdAndEntityType(storyId, Audit.EntityType.STORY);
        
        return calculateEffectiveLikes(likes, unlikes);
    }
    
    /**
     * Get effective likes count for a single story since a given date (likes without corresponding unlikes)
     * @param storyId The story ID
     * @param since Timestamp to count since
     * @return Effective likes count
     */
    public long getEffectiveLikesCountForStorySince(String storyId, Long since) {
        List<Audit> likes = auditRepository.findLikesByEntityIdAndEntityTypeAndCreatedAtAfter(storyId, Audit.EntityType.STORY, since);
        List<Audit> unlikes = auditRepository.findUnlikesByEntityIdAndEntityType(storyId, Audit.EntityType.STORY);
        
        return calculateEffectiveLikes(likes, unlikes);
    }
    
    /**
     * Get views count for a specific story
     * Excludes views where the viewer is the same as the story owner
     * Excludes views that are within 1-minute cooldown period
     * @param storyId The story ID
     * @param since Optional timestamp to count views since
     * @return Views count for the story (excluding self-views and cooldown views)
     */
    private long getViewsCountForStory(String storyId, Long since) {
        List<Audit> viewAudits;
        if (since == null) {
            viewAudits = auditRepository.findByEntityIdAndEntityTypeAndActionType(
                    storyId, Audit.EntityType.STORY, Audit.ActionType.VIEW);
        } else {
            viewAudits = auditRepository.findByEntityIdAndEntityTypeAndActionTypeAndCreatedAtAfter(
                    storyId, Audit.EntityType.STORY, Audit.ActionType.VIEW, since);
        }
        
        // Get the story to check ownership
        Story story = storyRepository.findById(storyId).orElse(null);
        if (story == null) {
            return 0;
        }
        
        String storyOwnerId = story.getUserId();
        
        // Filter out self-views and cooldown views
        long validViewCount = viewAudits.stream()
                .filter(audit -> {
                    String viewerId = audit.getUserId();
                    
                    // Exclude self-views
                    if (viewerId.equals(storyOwnerId)) {
                        return false;
                    }
                    
                    // Check for cooldown views (1 minute)
                    long oneMinuteAgo = TimestampUtil.currentEpochMillis() - (1 * 60 * 1000);
                    if (audit.getCreatedAt() != null && audit.getCreatedAt() < oneMinuteAgo) {
                        // Check if there's a more recent view by the same user/IP
                        List<Audit> recentViews;
                        if (viewerId.contains(".")) {
                            // IP address (unauthenticated user)
                            recentViews = auditRepository.findByIpAddressAndEntityIdAndEntityTypeAndActionTypeAndCreatedAtAfter(
                                    viewerId, storyId, Audit.EntityType.STORY, Audit.ActionType.VIEW, oneMinuteAgo);
                        } else {
                            // User ID (authenticated user)
                            recentViews = auditRepository.findByUserIdAndEntityIdAndEntityTypeAndActionTypeAndCreatedAtAfter(
                                    viewerId, storyId, Audit.EntityType.STORY, Audit.ActionType.VIEW, oneMinuteAgo);
                        }
                        
                        // If there's a more recent view, exclude this one (cooldown)
                        if (!recentViews.isEmpty()) {
                            return false;
                        }
                    }
                    
                    return true;
                })
                .count();
        
        return validViewCount;
    }
    
    /**
     * Get comments count for a specific story
     * @param storyId The story ID
     * @param since Optional timestamp to count comments since
     * @return Comments count for the story
     */
    private long getCommentsCountForStory(String storyId, Long since) {
        if (since == null) {
            return auditRepository.countByEntityIdAndEntityTypeAndActionType(
                    storyId, Audit.EntityType.COMMENT, Audit.ActionType.CREATE);
        }
        return auditRepository.countByEntityIdAndEntityTypeAndActionTypeAndCreatedAtAfter(
                storyId, Audit.EntityType.COMMENT, Audit.ActionType.CREATE, since);
    }
    
    /**
     * Get story matches for a user since a given date
     * @param userId The user ID
     * @param since Optional timestamp to get matches since
     * @return List of audit records for story matches
     */
    public List<Audit> getStoryMatchesForUser(String userId, Long since) {
        if (since == null) {
            // If no since date provided, get all matches
            return auditRepository.findByUserIdAndEntityTypeAndActionType(
                    userId, Audit.EntityType.STORY, Audit.ActionType.MATCH, PageRequest.of(0, Integer.MAX_VALUE))
                    .getContent();
        }
        return auditRepository.findStoryMatchesByUserIdAndCreatedAtAfter(userId, since);
    }
    
    /**
     * Check if a user has viewed a story within the last 1 minute
     * @param userId The user ID
     * @param storyId The story ID
     * @return true if user has viewed the story within 1 minute, false otherwise
     */
    public boolean hasViewedStoryRecently(String userId, String storyId) {
        if (userId == null) {
            return false; // Unauthenticated users don't have cooldown
        }
        
        // Calculate timestamp for 1 minute ago
        long oneMinuteAgo = System.currentTimeMillis() - (1 * 60 * 1000);
        
        // Check if there's a recent view audit
        List<Audit> recentViews = auditRepository.findByUserIdAndEntityIdAndEntityTypeAndActionTypeAndCreatedAtAfter(
                userId, storyId, Audit.EntityType.STORY, Audit.ActionType.VIEW, oneMinuteAgo);
        
        return !recentViews.isEmpty();
    }
    
    /**
     * Check if a user has viewed a story within the last 1 minute (for unauthenticated users by IP)
     * @param ipAddress The IP address
     * @param storyId The story ID
     * @return true if IP has viewed the story within 1 minute, false otherwise
     */
    public boolean hasViewedStoryRecentlyByIP(String ipAddress, String storyId) {
        if (ipAddress == null) {
            return false;
        }
        
        // Calculate timestamp for 1 minute ago
        long oneMinuteAgo = System.currentTimeMillis() - (1 * 60 * 1000);
        
        // Check if there's a recent view audit by IP (for unauthenticated users)
        List<Audit> recentViews = auditRepository.findByIpAddressAndEntityIdAndEntityTypeAndActionTypeAndCreatedAtAfter(
                ipAddress, storyId, Audit.EntityType.STORY, Audit.ActionType.VIEW, oneMinuteAgo);
        
        return !recentViews.isEmpty();
    }
    
    /**
     * Log story view for unauthenticated users (by IP address)
     * @param ipAddress The IP address
     * @param storyId The story ID
     * @param userAgent The user agent
     * @param sessionId The session ID
     */
    public void logStoryViewByIP(String ipAddress, String storyId, String userAgent, String sessionId) {
        Map<String, Object> metadata = Map.of(
            "interaction_type", "story_view",
            "is_own_story", false,
            "is_unauthenticated", true
        );
        // Use IP address as userId for unauthenticated users
        logAudit(ipAddress, Audit.EntityType.STORY, Audit.ActionType.VIEW, storyId, userAgent, ipAddress, sessionId, metadata);
    }
} 