package com.breakupstories.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for abuse detection service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbuseDetectionRequest {
    private String comment;
    private String language;
} 