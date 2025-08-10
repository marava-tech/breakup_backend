package com.breakupstories.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryRewriteResponse {
    
    private String originalText;
    private String rewrittenText;
    private String style;
    private String tone;
    private String status;
    private String error;
} 