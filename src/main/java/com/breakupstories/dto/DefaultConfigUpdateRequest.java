package com.breakupstories.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefaultConfigUpdateRequest {
    private String value;
    private String description;
    private Boolean active;
} 