package com.breakupstories.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for story rewrite responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryRewriteResponse {
    
    @JsonProperty("original_transcript")
    private String originalTranscript;
    
    @JsonProperty("rewritten_story")
    private String rewrittenStory;
    
    private String language;
} 