package com.breakupstories.dto;

import com.breakupstories.model.Content;
import com.breakupstories.model.Feedback;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequest {
    
    @NotBlank(message = "Story ID is required")
    private String storyId;
    
    @NotNull(message = "Tone is required")
    private Feedback.FeedbackTone tone;
    
    private List<Content> contents;
} 