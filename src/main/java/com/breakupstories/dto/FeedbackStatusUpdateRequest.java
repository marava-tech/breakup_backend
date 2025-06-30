package com.breakupstories.dto;

import com.breakupstories.model.Feedback;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackStatusUpdateRequest {
    
    @NotNull(message = "Status is required")
    private Feedback.FeedbackStatus status;
    
    private String adminResponse;
} 