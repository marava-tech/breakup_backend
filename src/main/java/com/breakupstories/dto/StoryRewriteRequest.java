package com.breakupstories.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for story rewrite requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryRewriteRequest {
    
    private String transcript;
    private String language;
} 