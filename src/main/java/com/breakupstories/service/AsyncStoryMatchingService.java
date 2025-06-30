package com.breakupstories.service;

import com.breakupstories.model.Audit;
import com.breakupstories.model.Story;
import com.breakupstories.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncStoryMatchingService {
    
    private final StoryMatchingService storyMatchingService;
    private final AuditService auditService;
    private final StoryRepository storyRepository;
    
    /**
     * Process story matching asynchronously after story status is changed to active
     * @param storyId The story ID that was activated
     * @param userId The user ID who owns the story
     */
    @Async
    public void processStoryMatchingAsync(String storyId, String userId) {
        try {
            log.info("Starting async story matching process for storyId: {}, userId: {}", storyId, userId);
            
            // Find the highest matching story
            StoryMatchingService.StoryMatchResult matchResult = storyMatchingService.findHighestMatchingStory(storyId);
            
            // Get the matched story to find its owner
            Story matchedStory = storyRepository.findById(matchResult.getMatchedStoryId()).orElse(null);
            
            if (matchedStory == null) {
                log.warn("Matched story not found: {}", matchResult.getMatchedStoryId());
                return;
            }
            
            String matchedStoryUserId = matchedStory.getUserId();
            
            // Ignore if both user IDs are the same
            if (userId.equals(matchedStoryUserId)) {
                log.info("Ignoring story match - same user: {}", userId);
                return;
            }
            
            // Create metadata with percentage
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("percentage", String.valueOf(matchResult.getPercentage()));
            metadata.put("matched_story_id", matchResult.getMatchedStoryId());
            metadata.put("original_story_id", matchResult.getOriginalStoryId());
            
            // Create audit log for the original story owner
            log.info("Creating audit log for original story owner: {}", userId);
            auditService.logAudit(
                userId,
                Audit.EntityType.STORY,
                Audit.ActionType.MATCH,
                storyId,
                null, // userAgent
                null, // ipAddress
                null, // sessionId
                metadata
            );
            
            // Create audit log for the matched story owner
            log.info("Creating audit log for matched story owner: {}", matchedStoryUserId);
            auditService.logAudit(
                matchedStoryUserId,
                Audit.EntityType.STORY,
                Audit.ActionType.MATCH,
                matchResult.getMatchedStoryId(),
                null, // userAgent
                null, // ipAddress
                null, // sessionId
                metadata
            );
            
            log.info("Successfully completed story matching process for storyId: {}", storyId);
            
        } catch (Exception e) {
            log.error("Error in async story matching process for storyId: {}, userId: {}", storyId, userId, e);
        }
    }
} 