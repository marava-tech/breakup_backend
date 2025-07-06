package com.breakupstories.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for story analysis service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryAnalysisRequest {
    private String story;
    private String language;
} 