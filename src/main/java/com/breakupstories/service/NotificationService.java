package com.breakupstories.service;

import com.breakupstories.dto.NotificationResponse;
import com.breakupstories.model.Audit;
import com.breakupstories.model.Story;
import com.breakupstories.repository.AuditRepository;
import com.breakupstories.repository.StoryRepository;
import com.breakupstories.util.TimestampUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final AuditService auditService;
    private final AuditRepository auditRepository;
    private final StoryRepository storyRepository;
    private final LikeService likeService;
    private final CommentService commentService;
    
    // Configurable fallback period (default: 1 month)
    private static final int DEFAULT_FALLBACK_MONTHS = 1;
    
    public NotificationResponse getNotifications(String userId) {
        log.info("Getting notifications for user: {}", userId);
        
        // Get last notification view time (epoch timestamp from audit)
        Long lastNotificationView = auditService.getLastNotificationView(userId);
        
        // If no last notification view found, use fallback date
        Long since = lastNotificationView;
        if (since == null) {
            since = TimestampUtil.currentEpochMillis() - (DEFAULT_FALLBACK_MONTHS * 30L * 24L * 60L * 60L * 1000L);
            log.info("No last notification view found for user: {}. Using fallback date: {} ({} months ago)", 
                userId, since, DEFAULT_FALLBACK_MONTHS);
        } else {
            log.debug("Using last notification view time for user {}: {}", userId, since);
        }
        
        // Get counts since last notification view for user's stories
        long newLikesCount = auditService.getLikesCountForUserStories(userId, since);
        long newViewsCount = auditService.getViewsCountForUserStories(userId, since);
        long newCommentsCount = auditService.getCommentsCountForUserStories(userId, since);
        
        log.debug("Notification counts for user {} since {}: likes={}, views={}, comments={}", 
            userId, since, newLikesCount, newViewsCount, newCommentsCount);
        
        // Get story-specific notifications
        List<NotificationResponse.NotificationItem> notifications = getStoryNotifications(userId, since);
        
        log.info("Retrieved {} story notifications for user: {}", notifications.size(), userId);
        
        return NotificationResponse.builder()
                .userId(userId)
                .totalNewLikes(newLikesCount)
                .totalNewViews(newViewsCount)
                .totalNewComments(newCommentsCount)
                .notify(newLikesCount>0 || newViewsCount>0 || newCommentsCount>0)
                .lastNotificationView(lastNotificationView) // Keep epoch timestamp for audit consistency
                .notifications(notifications)
                .build();
    }
    
    private List<NotificationResponse.NotificationItem> getStoryNotifications(String userId, Long since) {
        List<NotificationResponse.NotificationItem> notifications = new ArrayList<>();
        
        // Get all stories by this user (using a large page size to get all stories)
        List<Story> userStories = storyRepository.findByUserId(userId, org.springframework.data.domain.PageRequest.of(0, 1000)).getContent();
        
        for (Story story : userStories) {
            String storyId = story.getId();
            
            // Get counts for this story since last notification view
            long storyLikeCount = likeService.getLikeCount(storyId);
            long storyViewCount = story.getViewCount();
            long storyCommentCount = commentService.getCommentCount(storyId);
            
            // Get last activity time for this story
            Long lastActivity = getLastActivityTime(storyId, since);
            
            if (storyLikeCount > 0 || storyViewCount > 0 || storyCommentCount > 0) {
                notifications.add(NotificationResponse.NotificationItem.builder()
                        .storyId(storyId)
                        .storyTitle(story.getTitle())
                        .likeCount(storyLikeCount)
                        .viewCount(storyViewCount)
                        .commentCount(storyCommentCount)
                        .lastActivity(lastActivity)
                        .build());
            }
        }
        
        return notifications;
    }
    
    private long getStoryLikeCount(String storyId, Long since) {
        // Use the new effective like counting logic from AuditService
        if (since == null) {
            return auditService.getEffectiveLikesCountForStory(storyId);
        }
        return auditService.getEffectiveLikesCountForStorySince(storyId, since);
    }
    
    private long getStoryViewCount(String storyId, Long since) {
        if (since == null) {
            return auditRepository.countByEntityIdAndEntityTypeAndActionType(storyId, Audit.EntityType.STORY, Audit.ActionType.VIEW);
        }
        return auditRepository.countByEntityIdAndEntityTypeAndActionTypeAndCreatedAtAfter(
                storyId, Audit.EntityType.STORY, Audit.ActionType.VIEW, since);
    }
    
    private long getStoryCommentCount(String storyId, Long since) {
        if (since == null) {
            return auditRepository.countByEntityIdAndEntityTypeAndActionType(storyId, Audit.EntityType.COMMENT, Audit.ActionType.CREATE);
        }
        return auditRepository.countByEntityIdAndEntityTypeAndActionTypeAndCreatedAtAfter(
                storyId, Audit.EntityType.COMMENT, Audit.ActionType.CREATE, since);
    }
    
    private Long getLastActivityTime(String storyId, Long since) {
        return auditRepository.findTopByEntityIdOrderByCreatedAtDesc(storyId)
                .map(Audit::getCreatedAt)
                .orElse(null);
    }
} 