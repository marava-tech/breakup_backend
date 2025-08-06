package com.breakupstories.dto;

import com.breakupstories.enums.GENDER;
import com.breakupstories.enums.Role;
import com.breakupstories.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    
    private String id;
    private String name;
    private String email;
    private GENDER gender;
    private Integer age;
    private String profileImageUrl;
    private String preferredStoryLanguage;
    private Role role;
    private Integer coinBalance;
    private String referralCode;
    private String referredBy;
    private String deviceId; // Android device ID for referral tracking
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static UserResponse fromUser(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .gender(user.getGender())
                .age(user.getAge())
                .profileImageUrl(user.getProfileImageUrl())
                .preferredStoryLanguage(user.getPreferredStoryLanguage())
                .role(user.getRole())
                .coinBalance(user.getCoinBalance())
                .referralCode(user.getReferralCode())
                .referredBy(user.getReferredBy())
                .deviceId(user.getDeviceId())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
} 