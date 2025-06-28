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

        return NotificationResponse.builder()
                .userId(userId)
                .totalNewLikes(newLikesCount)
                .totalNewViews(newViewsCount)
                .totalNewComments(newCommentsCount)
                .notify(newLikesCount>0 || newViewsCount>0 || newCommentsCount>0)
                .lastNotificationView(lastNotificationView) // Keep epoch timestamp for audit consistency
                .build();
    }

} 