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
    
    // Statistics
    private Long totalStories;
    private Long totalLikes;
    private Long totalViews;
    private Long totalComments;
    
    public static UserProfileResponse fromUser(User user, Long totalStories, Long totalLikes, Long totalViews, Long totalComments) {
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
                .totalStories(totalStories)
                .totalLikes(totalLikes)
                .totalViews(totalViews)
                .totalComments(totalComments)
                .build();
    }
} 