package com.breakupstories.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoryMatchingService {
    
    private final Random random = new Random();
    
    /**
     * Find the highest matching story for a given story
     * @param storyId The story ID to find matches for
     * @return StoryMatchResult containing the matched story ID and percentage
     */
    public StoryMatchResult findHighestMatchingStory(String storyId) {
        log.info("Finding highest matching story for storyId: {}", storyId);
        
        // Mock implementation - will be replaced with AI service later
        // For now, return a random story ID and percentage
        String matchedStoryId = "story_" + random.nextInt(1000);
        int percentage = random.nextInt(101); // 0-100
        
        log.info("Mock story match result - matchedStoryId: {}, percentage: {}%", matchedStoryId, percentage);
        
        return StoryMatchResult.builder()
                .originalStoryId(storyId)
                .matchedStoryId(matchedStoryId)
                .percentage(percentage)
                .build();
    }
    
    /**
     * Result class for story matching
     */
    public static class StoryMatchResult {
        private String originalStoryId;
        private String matchedStoryId;
        private int percentage;
        
        // Builder pattern
        public static StoryMatchResultBuilder builder() {
            return new StoryMatchResultBuilder();
        }
        
        public static class StoryMatchResultBuilder {
            private final StoryMatchResult result = new StoryMatchResult();
            
            public StoryMatchResultBuilder originalStoryId(String originalStoryId) {
                result.originalStoryId = originalStoryId;
                return this;
            }
            
            public StoryMatchResultBuilder matchedStoryId(String matchedStoryId) {
                result.matchedStoryId = matchedStoryId;
                return this;
            }
            
            public StoryMatchResultBuilder percentage(int percentage) {
                result.percentage = percentage;
                return this;
            }
            
            public StoryMatchResult build() {
                return result;
            }
        }
        
        // Getters
        public String getOriginalStoryId() { return originalStoryId; }
        public String getMatchedStoryId() { return matchedStoryId; }
        public int getPercentage() { return percentage; }
    }
} 