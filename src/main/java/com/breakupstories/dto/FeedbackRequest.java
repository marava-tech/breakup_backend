package com.breakupstories.dto;

import com.breakupstories.model.Feedback;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequest {
    
    // Optional - only required for story-specific feedback
    private String storyId;
    
    @NotNull(message = "Feedback type is required")
    private Feedback.FeedbackType type;
    
    // For general feedback
    @Size(max = 200, message = "Subject must not exceed 200 characters")
    private String subject;
    
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;
} 