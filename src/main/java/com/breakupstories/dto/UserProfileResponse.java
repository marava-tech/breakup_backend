package com.breakupstories.dto;

import com.breakupstories.enums.GENDER;
import com.breakupstories.enums.Role;
import com.breakupstories.model.User;
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
public class UserProfileResponse {
    
    private String id;
    private String name;
    private String email;
    private String profileImageUrl;
    private GENDER gender;
    private Integer age;
    private String preferredStoryLanguage;
    private Role role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Referral and coins information
    private String referralCode;
    private String referredBy;
    private String referredByUserName;
    private int totalCoins;
    
    // Statistics
    private Long totalStories;
    private Long totalLikes;
    private Long totalViews;
    private Long totalComments;
    
    public static UserProfileResponse fromUser(User user, Long totalStories, Long totalLikes, Long totalViews, Long totalComments, int totalCoins) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .gender(user.getGender())
                .age(user.getAge())
                .preferredStoryLanguage(user.getPreferredStoryLanguage())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .referralCode(user.getReferralCode())
                .referredBy(user.getReferredBy())
                .totalCoins(totalCoins)
                .totalStories(totalStories)
                .totalLikes(totalLikes)
                .totalViews(totalViews)
                .totalComments(totalComments)
                .build();
    }
    
    public static UserProfileResponse fromUserWithReferralInfo(User user, Long totalStories, Long totalLikes, Long totalViews, Long totalComments, 
                                                             String referredByUserName, List<CoinHistoryResponse> referralHistory, int totalCoins) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .gender(user.getGender())
                .age(user.getAge())
                .preferredStoryLanguage(user.getPreferredStoryLanguage())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .referralCode(user.getReferralCode())
                .referredBy(user.getReferredBy())
                .referredByUserName(referredByUserName)
                .totalCoins(totalCoins)
                .totalStories(totalStories)
                .totalLikes(totalLikes)
                .totalViews(totalViews)
                .totalComments(totalComments)
                .build();
    }
} 