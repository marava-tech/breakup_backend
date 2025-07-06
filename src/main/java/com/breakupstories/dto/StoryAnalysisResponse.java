package com.breakupstories.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for story analysis service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryAnalysisResponse {
    private Boolean success;
    private StoryAnalysis analysis;
    private String error;
} 