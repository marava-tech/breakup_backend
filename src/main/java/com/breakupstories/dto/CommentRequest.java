package com.breakupstories.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {
    @NotBlank(message = "Story ID is required")
    private String storyId;
    
    @NotBlank(message = "Comment text is required")
    @Size(max = 1000, message = "Comment text cannot exceed 1000 characters")
    private String text;
    
    private String parentId; // For replies
} 