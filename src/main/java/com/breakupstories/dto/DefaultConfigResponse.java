package com.breakupstories.dto;

import com.breakupstories.model.DefaultConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefaultConfigResponse {
    private String id;
    private String key;
    private String value;
    private String description;
    private boolean active;

    public static DefaultConfigResponse fromEntity(DefaultConfig config) {
        return DefaultConfigResponse.builder()
                .id(config.getId())
                .key(config.getKey())
                .value(config.getValue())
                .description(config.getDescription())
                .active(config.isActive())
                .build();
    }
} 