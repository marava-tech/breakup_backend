package com.breakupstories.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Story update request with flexible fields")
public class StoryUpdateRequest {
    
    @Schema(description = "Story title", example = "Updated Story Title")
    private String title;
    
    @Schema(description = "Story status", example = "ACTIVE")
    private String status;
    
    @Schema(description = "Story language", example = "ENGLISH")
    private String language;
    
    @Schema(description = "Story tags as array", example = "[\"breakup\", \"healing\"]")
    private String[] tags;
    
    @Schema(description = "Story rejection reasons", example = "[\"inappropriate content\"]")
    private String[] rejectionReasons;
    
    @Schema(description = "Story audio URL", example = "https://example.com/audio.mp3")
    private String audioUrl;
    
    @Schema(description = "Story thumbnail URL", example = "https://example.com/thumbnail.jpg")
    private String thumbnailUrl;
    
    @Schema(description = "Story images as array", example = "[\"https://example.com/image1.jpg\"]")
    private String[] storyImages;
} 