package com.breakupstories.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryCreationEligibilityResponse {
    private boolean isStoryCreationEnabled;
    private int dailyLimit;
    private int currentStoryCount;
    private int remainingStories;
    private LocalDateTime nextEligibilityTime;
    private String message;
} 