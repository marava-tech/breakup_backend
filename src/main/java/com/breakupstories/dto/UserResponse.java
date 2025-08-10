package com.breakupstories.dto;

import com.breakupstories.enums.GENDER;
import com.breakupstories.enums.Role;
import com.breakupstories.model.User;
import com.breakupstories.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Optional;

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
    private String referralCode;
    private String referredBy;
    private String referredByUsername; // Username of the user who referred this user
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

                .referralCode(user.getReferralCode())
                .referredBy(user.getReferredBy())
                .deviceId(user.getDeviceId())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
    
    public static UserResponse fromUserWithReferrerName(User user, UserRepository userRepository) {
        String referredByUsername = null;
        
        // Lookup referrer's username if referredBy is not null
        if (user.getReferredBy() != null && !user.getReferredBy().trim().isEmpty()) {
            Optional<User> referrer = userRepository.findById(user.getReferredBy());
            if (referrer.isPresent()) {
                referredByUsername = referrer.get().getName();
            }
        }
        
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .gender(user.getGender())
                .age(user.getAge())
                .profileImageUrl(user.getProfileImageUrl())
                .preferredStoryLanguage(user.getPreferredStoryLanguage())
                .role(user.getRole())

                .referralCode(user.getReferralCode())
                .referredBy(user.getReferredBy())
                .referredByUsername(referredByUsername)
                .deviceId(user.getDeviceId())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
} 