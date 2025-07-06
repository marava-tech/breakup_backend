package com.breakupstories.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for location info service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationInfoResponse {
    private Boolean success;
    private String pincode;
    private String latitude;
    private String longitude;
    private String district;
    private String state;
    private String country;
    
    @JsonProperty("full_address")
    private String full_address;
    
    private String error;
} 