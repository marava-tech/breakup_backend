package com.breakupstories.dto;

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
    private String username;
    private Feedback.FeedbackType type;
    private String subject;
    private String description;
    private String fileUrl;
    private Feedback.FeedbackStatus status;
    private String adminResponse;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static FeedbackResponse fromFeedback(Feedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .storyId(feedback.getStoryId())
                .userId(feedback.getUserId())
                .type(feedback.getType())
                .subject(feedback.getSubject())
                .description(feedback.getDescription())
                .fileUrl(feedback.getFileUrl())
                .status(feedback.getStatus())
                .adminResponse(feedback.getAdminResponse())
                .createdAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .build();
    }
} 