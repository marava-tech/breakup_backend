package com.breakupstories.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for location info service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationInfoRequest {
    private Double latitude;
    private Double longitude;
} 