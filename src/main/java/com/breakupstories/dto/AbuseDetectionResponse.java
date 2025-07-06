package com.breakupstories.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for abuse detection service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbuseDetectionResponse {
    private Boolean success;
    
    @JsonProperty("is_abusive")
    private Boolean is_abusive;
    
    private Double confidence;
    private String category;
    private String explanation;
    private String error;
} 