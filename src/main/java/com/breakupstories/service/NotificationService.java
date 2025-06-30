package com.breakupstories.service;

import com.breakupstories.dto.NotificationItem;
import com.breakupstories.dto.NotificationResponse;
import com.breakupstories.enums.DeeplinkType;
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

        // Generate notification items
        List<NotificationItem> notifications = generateNotificationItems(newLikesCount, newViewsCount, newCommentsCount);
        
        // Add story match notifications
        List<NotificationItem> storyMatchNotifications = generateStoryMatchNotifications(userId, since);
        notifications.addAll(storyMatchNotifications);
        
        return NotificationResponse.builder()
                .userId(userId)
                .notifications(notifications)
                .notify(!notifications.isEmpty())
                .lastNotificationView(lastNotificationView) // Keep epoch timestamp for audit consistency
                .build();
    }
    
    private List<NotificationItem> generateNotificationItems(long likesCount, long viewsCount, long commentsCount) {
        List<NotificationItem> items = new ArrayList<>();
        
        // Generate combined message if there are multiple types of notifications
        if (likesCount > 0 || viewsCount > 0 || commentsCount > 0) {
            StringBuilder message = new StringBuilder("You got ");
            List<String> parts = new ArrayList<>();
            
            if (viewsCount > 0) {
                parts.add(viewsCount + " new view" + (viewsCount > 1 ? "s" : ""));
            }
            if (likesCount > 0) {
                parts.add(likesCount + " new  like" + (likesCount > 1 ? "s" : ""));
            }
            if (commentsCount > 0) {
                parts.add(commentsCount + " new comment" + (commentsCount > 1 ? "s" : ""));
            }
            
            // Join parts with commas and "and"
            if (parts.size() == 1) {
                message.append(parts.get(0));
            } else if (parts.size() == 2) {
                message.append(parts.get(0)).append(" and ").append(parts.get(1));
            } else {
                for (int i = 0; i < parts.size() - 1; i++) {
                    message.append(parts.get(i));
                    if (i < parts.size() - 2) {
                        message.append(", ");
                    }
                }
                message.append(" and ").append(parts.get(parts.size() - 1));
            }
            
            // Create notification item with GENERAL deeplink type
            NotificationItem item = NotificationItem.builder()
                    .text(message.toString())
                    .deeplinkType(DeeplinkType.GENERAL)
                    .id("my-stories")
                    .build();
            
            items.add(item);
        }
        
        return items;
    }
    
    private List<NotificationItem> generateStoryMatchNotifications(String userId, Long since) {
        List<NotificationItem> items = new ArrayList<>();
        
        try {
            // Get story matches for the user
            List<Audit> storyMatches = auditService.getStoryMatchesForUser(userId, since);
            
            for (Audit match : storyMatches) {
                // Extract percentage from metadata
                Object percentageObj = match.getMetadata() != null ? match.getMetadata().get("percentage") : null;
                if (percentageObj != null) {
                    String percentage = percentageObj.toString();
                    
                    // Create notification text
                    String text = "Your story matched " + percentage + "% with another story. Want to view?";
                    
                    // Create notification item with STORY deeplink type and story ID
                    NotificationItem item = NotificationItem.builder()
                            .text(text)
                            .deeplinkType(DeeplinkType.STORY)
                            .id(match.getEntityId())
                            .build();
                    
                    items.add(item);
                }
            }
        } catch (Exception e) {
            log.error("Error generating story match notifications for user {}: {}", userId, e.getMessage());
        }
        
        return items;
    }
} 