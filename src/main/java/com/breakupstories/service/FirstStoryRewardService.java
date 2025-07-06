package com.breakupstories.service;

import com.breakupstories.model.Story;
import com.breakupstories.model.StoryDataStore;
import com.breakupstories.repository.StoryRepository;
import com.breakupstories.repository.StoryDataStoreRepository;
import com.breakupstories.service.RewardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service to handle first story reward logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FirstStoryRewardService {
    
    private final StoryRepository storyRepository;
    private final StoryDataStoreRepository storyDataStoreRepository;
    private final RewardService rewardService;
    private final DefaultConfigService defaultConfigService;
    
    /**
     * Check and reward for first story that meets criteria
     * @param userId The user ID
     * @param storyId The story ID
     * @return true if reward was given, false otherwise
     */
    public boolean checkAndRewardFirstStory(String userId, String storyId) {
        log.info("Checking first story reward for user: {} and story: {}", userId, storyId);
        
        try {
            // Check if this is the user's first story
            if (!isFirstStory(userId, storyId)) {
                log.info("Not user's first story - no reward given for user: {}", userId);
                return false;
            }
            
            // Check if story is active
            Story story = storyRepository.findById(storyId)
                    .orElseThrow(() -> new RuntimeException("Story not found: " + storyId));
            
            if (story.getStatus() != Story.StoryStatus.ACTIVE) {
                log.info("Story is not active - no reward given for story: {}", storyId);
                return false;
            }
            
            // Check if story duration meets minimum requirement
            if (!meetsDurationRequirement(story)) {
                log.info("Story duration does not meet requirement - no reward given for story: {}", storyId);
                return false;
            }
            
            // Give reward
            int rewardCoins = defaultConfigService.getFirstStoryRewardCoins();
            rewardService.addCoins(userId, rewardCoins, "First story reward (5+ minutes)", storyId);
            
            log.info("First story reward given - User: {}, Story: {}, Coins: {}", userId, storyId, rewardCoins);
            return true;
            
        } catch (Exception e) {
            log.error("Error checking first story reward for user {} and story {}: {}", userId, storyId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Check if this is the user's first story
     * @param userId The user ID
     * @param storyId The story ID
     * @return true if this is the first story, false otherwise
     */
    private boolean isFirstStory(String userId, String storyId) {
        // Get all stories by this user
        List<Story> userStories = storyRepository.findByUserIdAndStatus(userId, Story.StoryStatus.ACTIVE);
        
        // If this is the only active story, it's the first one
        if (userStories.size() == 1 && userStories.get(0).getId().equals(storyId)) {
            return true;
        }
        
        // If there are multiple stories, check if this is the oldest one
        if (userStories.size() > 1) {
            Story oldestStory = userStories.stream()
                    .min((s1, s2) -> s1.getCreatedAt().compareTo(s2.getCreatedAt()))
                    .orElse(null);
            
            return oldestStory != null && oldestStory.getId().equals(storyId);
        }
        
        return false;
    }
    
    /**
     * Check if story duration meets the minimum requirement
     * @param story The story to check
     * @return true if duration meets requirement, false otherwise
     */
    private boolean meetsDurationRequirement(Story story) {
        if (story.getDuration() == null) {
            log.warn("Story duration is null for story: {}", story.getId());
            return false;
        }
        
        int minDurationMinutes = defaultConfigService.getFirstStoryMinDurationMinutes();
        long minDurationMillis = minDurationMinutes * 60 * 1000; // Convert minutes to milliseconds
        
        boolean meetsRequirement = story.getDuration() >= minDurationMillis;
        
        log.info("Story duration check - Story: {}, Duration: {}ms, Min Required: {}ms, Meets Requirement: {}", 
                story.getId(), story.getDuration(), minDurationMillis, meetsRequirement);
        
        return meetsRequirement;
    }
} 