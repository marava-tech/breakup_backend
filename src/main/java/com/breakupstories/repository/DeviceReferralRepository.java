package com.breakupstories.repository;

import com.breakupstories.model.DeviceReferral;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceReferralRepository extends MongoRepository<DeviceReferral, String> {
    
    /**
     * Check if a device has already used a referral code
     */
    boolean existsByDeviceId(String deviceId);
    
    /**
     * Find device referral by device ID
     */
    Optional<DeviceReferral> findByDeviceId(String deviceId);
    
    /**
     * Find all device referrals by referrer user ID
     */
    List<DeviceReferral> findByReferrerUserId(String referrerUserId);
    
    /**
     * Find all device referrals by referred user ID
     */
    List<DeviceReferral> findByReferredUserId(String referredUserId);
    
    /**
     * Find device referrals by referral code
     */
    List<DeviceReferral> findByReferralCode(String referralCode);
    
    /**
     * Check if a device has already used a specific referral code
     */
    boolean existsByDeviceIdAndReferralCode(String deviceId, String referralCode);
    
    /**
     * Find device referrals where reward has been claimed
     */
    List<DeviceReferral> findByRewardClaimedTrue();
    
    /**
     * Count device referrals by referrer user ID
     */
    long countByReferrerUserId(String referrerUserId);
} 