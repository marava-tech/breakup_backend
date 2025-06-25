package com.breakupstories.dto;

import com.breakupstories.model.Content;
import com.breakupstories.model.Feedback;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse {
    
    private String id;
    private String storyId;
    private String userId;
    private Feedback.FeedbackTone tone;
    private List<Content> contents;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static FeedbackResponse fromFeedback(Feedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .storyId(feedback.getStoryId())
                .userId(feedback.getUserId())
                .tone(feedback.getTone())
                .contents(feedback.getContents())
                .createdAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .build();
    }
} 