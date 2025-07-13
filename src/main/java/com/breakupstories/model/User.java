package com.breakupstories.model;

import com.breakupstories.enums.GENDER;
import com.breakupstories.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {
    
    @Id
    private String id;
    
    private String name;
    private String email;
    private GENDER gender;
    private Integer age;
    private String profileImageUrl;
    private String preferredStoryLanguage;
    private String referralCode;
    private String referredBy;
    private int coinBalance;
    
    @Builder.Default
    private Role role = Role.USER;
    
    @Builder.Default
    private Boolean isActive = true;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
} 