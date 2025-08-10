package com.breakupstories.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceConfigResponse {
    
    private Map<String, Object> configs;
    private int totalConfigs;
    private String message;
    
    // Device-specific information
    private boolean isBanned;
    private String banReason;
    private LocalDateTime bannedAt;
    private List<String> bannedEmails;
    private boolean isEligibleForReferral;
    private String deviceId;
    
    public static DeviceConfigResponse success(Map<String, Object> configs, String deviceId,
                                              boolean isBanned, String banReason, LocalDateTime bannedAt,
                                              java.util.List<String> bannedEmails, boolean isEligibleForReferral) {
        return DeviceConfigResponse.builder()
                .configs(configs)
                .totalConfigs(configs.size())
                .deviceId(deviceId)
                .isBanned(isBanned)
                .banReason(banReason)
                .bannedAt(bannedAt)
                .bannedEmails(bannedEmails)
                .isEligibleForReferral(isEligibleForReferral)
                .message("Device configuration retrieved successfully")
                .build();
    }
    
    public static DeviceConfigResponse error(String message, String deviceId) {
        return DeviceConfigResponse.builder()
                .configs(Map.of())
                .totalConfigs(0)
                .deviceId(deviceId)
                .isBanned(false)
                .isEligibleForReferral(false)
                .message(message)
                .build();
    }
}
