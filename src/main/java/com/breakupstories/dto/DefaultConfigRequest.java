package com.breakupstories.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefaultConfigRequest {
    @NotBlank(message = "Key is required")
    private String key;
    @NotBlank(message = "Value is required")
    private String value;
    private String description;
    private Boolean active;
} 