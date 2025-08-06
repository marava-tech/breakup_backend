package com.breakupstories.model;

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
@Document(collection = "device_referrals")
public class DeviceReferral {
    
    @Id
    private String id;
    
    private String deviceId;           // Android device ID
    private String referralCode;       // Referral code used
    private String referrerUserId;     // User who provided the referral code
    private String referredUserId;     // User who used the referral code
    private boolean rewardClaimed;     // Whether the referral reward has been claimed
    private LocalDateTime rewardClaimedAt; // When the reward was claimed
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
} 