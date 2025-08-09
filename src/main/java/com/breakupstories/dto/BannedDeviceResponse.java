package com.breakupstories.dto;

import com.breakupstories.model.BannedDevice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BannedDeviceResponse {
    
    private String id;
    private String deviceId;
    private String reason;
    private List<String> emails;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static BannedDeviceResponse fromBannedDevice(BannedDevice bannedDevice) {
        return BannedDeviceResponse.builder()
                .id(bannedDevice.getId())
                .deviceId(bannedDevice.getDeviceId())
                .reason(bannedDevice.getReason())
                .emails(bannedDevice.getEmails())
                .createdAt(bannedDevice.getCreatedAt())
                .updatedAt(bannedDevice.getUpdatedAt())
                .build();
    }
}
