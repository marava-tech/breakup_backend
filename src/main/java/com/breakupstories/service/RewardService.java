package com.breakupstories.service;

import com.breakupstories.dto.CoinBalanceResponse;
import com.breakupstories.dto.ReferralStatsResponse;
import com.breakupstories.model.CoinHistory;
import com.breakupstories.model.Story;
import com.breakupstories.model.User;
import com.breakupstories.enums.CoinHistoryEntityType;
import com.breakupstories.repository.CoinHistoryRepository;
import com.breakupstories.repository.StoryRepository;
import com.breakupstories.repository.UserRepository;
import com.breakupstories.repository.FeedbackRepository;
import com.breakupstories.util.ApplicationContextProvider;
import com.breakupstories.util.RequestContext;
import com.breakupstories.dto.RewardConfigResponse;
import com.breakupstories.model.Feedback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RewardService {
    
    private final CoinHistoryRepository coinHistoryRepository;
    private final UserRepository userRepository;
    private final StoryRepository storyRepository;
    private final FeedbackRepository feedbackRepository;
    private final DefaultConfigService defaultConfigService;

    private static final int LIKES_MILESTONE = 100;
    private static final int VIEWS_MILESTONE = 1000;
    private static final long STORY_DURATION_THRESHOLD = 300000; // 5 minutes in milliseconds
    
    /**
     * Get total coins for a user by calculating from history
     */
    public int getTotalCoins(String userId) {
        List<CoinHistory> coinHistory = coinHistoryRepository.findByUserId(userId);
        return coinHistory.stream()
                .mapToInt(CoinHistory::getCount)
                .sum();
    }
    
    /**
     * Get valid total coins for a user (excluding invalidated entries unless refunded)
     * 
     * Calculation rules:
     * - Include entries where invalidate is false, null, or missing (backward compatibility)
     * - Include entries where refund is true (even if invalidated)
     * - Exclude entries where invalidate is true AND refund is false/null
     */
    public int getValidTotalCoins(String userId) {
        List<CoinHistory> validCoinHistory = coinHistoryRepository.findValidCoinHistoryByUserId(userId);
        return validCoinHistory.stream()
                .mapToInt(CoinHistory::getCount)
                .sum();
    }
    
    /**
     * Get coin balance and history for a user (using valid coins calculation)
     * 
     * Backward compatible: Old coin history records without invalidate/refund fields
     * are automatically included in the total calculation.
     */
    @Cacheable(value = "coin-balance", key = "#userId")
    public CoinBalanceResponse getCoinBalance(String userId) {
        List<CoinHistory> coinHistory = coinHistoryRepository.findByUserId(userId);
        coinHistory = coinHistory.stream().sorted(Comparator.comparingLong(CoinHistory::getCreatedAt).reversed()).toList();
        
        // Calculate total coins excluding invalidated entries (unless refunded)
        // Includes all old records that don't have invalidate/refund fields
        int totalCoins = getValidTotalCoins(userId);
        
        return CoinBalanceResponse.builder()
                .totalCoins(totalCoins)
                .coinHistory(coinHistory)
                .build();
    }
    
    /**
     * Add coins to user's balance
     */
    @CacheEvict(value = "coin-balance", key = "#userId")
    public void addCoins(String userId, int count, String reason, String relatedEntityId) {
        addCoins(userId, count, reason, relatedEntityId, null);
    }

    /**
     * Add coins to user's balance with entity type
     */
    @CacheEvict(value = "coin-balance", key = "#userId")
    public void addCoins(String userId, int count, String reason, String relatedEntityId, CoinHistoryEntityType relatedEntityType) {
        // Check if user already received this reward
        if (relatedEntityId != null && 
            coinHistoryRepository.existsByUserIdAndReasonAndRelatedEntityId(userId, reason, relatedEntityId)) {
            log.info("User {} already received reward for {} with entity {}", userId, reason, relatedEntityId);
            return;
        }
        
        // Create coin history entry
        CoinHistory coinHistory = CoinHistory.builder()
                .userId(userId)
                .count(count)
                .reason(reason)
                .relatedEntityId(relatedEntityId)
                .relatedEntityType(relatedEntityType)
                .build();
        
        coinHistoryRepository.save(coinHistory);
        
        log.info("Added {} coins to user {} for reason: {} (entity: {} - {})", 
                count, userId, reason, relatedEntityType, relatedEntityId);
    }
    
    /**
     * Deduct coins from user's balance
     */
    @CacheEvict(value = "coin-balance", key = "#userId")
    public void deductCoins(String userId, int count, String reason) {
        // Check if user has enough valid coins
        int availableCoins = getValidTotalCoins(userId);
        if (availableCoins < count) {
            throw new RuntimeException("Insufficient coin balance. Available: " + availableCoins + ", Required: " + count);
        }
        
        // Create coin history entry (negative count for deduction)
        CoinHistory coinHistory = CoinHistory.builder()
                .userId(userId)
                .count(-count)
                .reason(reason)
                .build();
        
        coinHistoryRepository.save(coinHistory);
        
        log.info("Deducted {} coins from user {} for reason: {}", count, userId, reason);
    }
    
    /**
     * Check and reward for story active milestone (duration > 10 minutes)
     */
    public void checkStoryActiveReward(String storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found with ID: " + storyId));
        
        // Check if story duration is greater than threshold
        if (story.getDuration() != null && story.getDuration() >= STORY_DURATION_THRESHOLD) {
            addCoins(story.getUserId(), Integer.parseInt(defaultConfigService.getByKey("default_story_active_points").getValue()), "story_active", storyId, CoinHistoryEntityType.STORY);
        }
    }
    
    /**
     * Check and reward for likes milestone (100 likes)
     */
    public void checkLikesMilestoneReward(String storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found with ID: " + storyId));
        LikeService likeService = ApplicationContextProvider.getBean(LikeService.class);
        long likeCount = likeService.getLikeCount(storyId);
        
        if (likeCount >= LIKES_MILESTONE) {
            addCoins(story.getUserId(), Integer.parseInt(defaultConfigService.getByKey("default_100_likes_points").getValue()), "100_plus_likes_milestone", storyId, CoinHistoryEntityType.STORY);
        }
    }
    
    /**
     * Check and reward for views milestone (1000 views)
     */
    public void checkViewsMilestoneReward(String storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found with ID: " + storyId));
        
        if (story.getViewCount() != null && story.getViewCount() >= VIEWS_MILESTONE) {
            addCoins(story.getUserId(), Integer.parseInt(defaultConfigService.getByKey("default_1000_views_points").getValue()), "1000_plus_views_milestone", storyId, CoinHistoryEntityType.STORY);
        }
    }
    
    /**
     * Generate unique referral code for a user
     */
    public String generateReferralCode(String userId) {
        String referralCode;
        do {
            referralCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (userRepository.existsByReferralCode(referralCode));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        user.setReferralCode(referralCode);
        userRepository.save(user);
        
        log.info("Generated referral code {} for user {}", referralCode, userId);
        return referralCode;
    }
    
    /**
     * Process referral when a new user signs up (device-based)
     */
    public void processReferral(String newUserId, String referralCode, String deviceId) {
        String requestId = RequestContext.getRequestId();
        log.info("Processing device-based referral for user: {}, device: {}, referral code: {} [RequestID: {}]", 
                newUserId, deviceId, referralCode, requestId);
        
        // Validate device ID
        if (deviceId == null || deviceId.trim().isEmpty()) {
            log.warn("Device ID is required for referral processing [RequestID: {}]", requestId);
            return;
        }
        
        // Get the current user to check their email
        User newUser = userRepository.findById(newUserId)
                .orElseThrow(() -> new RuntimeException("User not found: " + newUserId));
        
        // Check if this exact device-email combination has already processed a referral
        // This prevents the same user from claiming referral rewards multiple times
        if (userRepository.existsByDeviceId(deviceId) && newUser.getReferredBy() != null) {
            log.info("Device {} with email {} has already used a referral code, skipping referral processing [RequestID: {}]", 
                    deviceId, newUser.getEmail(), requestId);
            return;
        }
        
        // Check if this device has already been used by a different email address
        // We'll still process the referral relationship but give 0 coins
        boolean deviceUsedByDifferentEmail = userRepository.existsByDeviceIdAndEmailNot(deviceId, newUser.getEmail());
        if (deviceUsedByDifferentEmail) {
            log.info("Device {} has already been used by a different email address, will map referredBy but give 0 coins [RequestID: {}]", 
                    deviceId, requestId);
        }
        
        // Find the referrer by referral code
        Optional<User> referrer = userRepository.findByReferralCode(referralCode);
        
        if (referrer.isPresent()) {
            
            // Check if referrer has reached maximum referrals limit by counting referred users
            long referrerReferralCount = userRepository.findByReferredBy(referrer.get().getId()).size();
            int maxReferralsPerUser = Integer.parseInt(defaultConfigService.getByKey("max_referrals_per_user").getValue());
            
            if (referrerReferralCount >= maxReferralsPerUser) {
                log.warn("Referrer {} has reached maximum referrals limit ({}), skipping referral [RequestID: {}]", 
                        referrer.get().getId(), maxReferralsPerUser, requestId);
                return;
            }
            
            // Update new user's referred by field and device ID
            newUser.setReferredBy(referrer.get().getId());
            newUser.setDeviceId(deviceId);
            userRepository.save(newUser);
            log.info("Updated user {} with referrer {} and device ID {} [RequestID: {}]", 
                    newUserId, referrer.get().getId(), deviceId, requestId);

            // Determine coin amounts based on whether device was used by different email
            int referralRewardPoints;
            int referralWelcomePoints;
            
            if (!deviceUsedByDifferentEmail) {
                // Normal referral - give full coins
                referralRewardPoints = Integer.parseInt(defaultConfigService.getByKey("default_referral_reward_points").getValue());
                referralWelcomePoints = Integer.parseInt(defaultConfigService.getByKey("default_referral_welcome_points").getValue());
                
                log.info("Processing normal referral with full coin rewards [RequestID: {}]", requestId);
            } else {
                // Device used by different email - give 0 coins but still map relationship
                referralRewardPoints = 0;
                referralWelcomePoints = 0;
                
                log.info("Processing referral with 0 coins due to device being used by different email [RequestID: {}]", requestId);
            }

            // Reward the referrer (0 if device used by different email)
            addCoins(referrer.get().getId(), referralRewardPoints, "referral_reward_"+newUser.getName(), newUserId, CoinHistoryEntityType.USER);
            
            // Reward the referred user (0 if device used by different email)
            addCoins(newUserId, referralWelcomePoints, "referral_welcome", referrer.get().getId(), CoinHistoryEntityType.USER);
            
            log.info("Processed device-based referral: {} referred {} (referrer: {} coins, referred: {} coins) [RequestID: {}]", 
                    referrer.get().getId(), newUserId, referralRewardPoints, referralWelcomePoints, requestId);
        } else {
            log.warn("Invalid referral code: {} [RequestID: {}]", referralCode, requestId);
        }
    }

    
    /**
     * Get referral statistics for a user (updated to use user-based referrals)
     */
    public ReferralStatsResponse getReferralStats(String userId) {
        List<User> referredUsers = userRepository.findByReferredBy(userId);
        List<String> referredUserIds = referredUsers.stream()
                .map(User::getId)
                .collect(Collectors.toList());
        
        return ReferralStatsResponse.builder()
                .referralCode(userRepository.findById(userId).map(User::getReferralCode).orElse(null))
                .referredBy(userRepository.findById(userId).map(User::getReferredBy).orElse(null))
                .referredUsersCount(referredUsers.size())
                .referredUsers(referredUserIds)
                .build();
    }
    
    /**
     * Check if a device has already used a referral code
     */
    public boolean hasDeviceUsedReferral(String deviceId) {
        return userRepository.existsByDeviceId(deviceId);
    }
    
    /**
     * Check if a device has already used a referral code with a different email
     */
    public boolean hasDeviceUsedReferralWithDifferentEmail(String deviceId, String email) {
        return userRepository.existsByDeviceIdAndEmailNot(deviceId, email);
    }
    
    /**
     * Get device referral information
     */
    public Optional<User> getDeviceReferral(String deviceId) {
        return userRepository.findByDeviceId(deviceId);
    }
    
    /**
     * Invalidate a coin history entry
     */
    @CacheEvict(value = "coin-balance", allEntries = true)
    public void invalidateCoinHistory(String coinHistoryId, String invalidationReason, Boolean refund) {
        CoinHistory coinHistory = coinHistoryRepository.findById(coinHistoryId)
                .orElseThrow(() -> new RuntimeException("Coin history not found with ID: " + coinHistoryId));
        
        if (coinHistory.getInvalidate() != null && coinHistory.getInvalidate()) {
            throw new RuntimeException("Coin history is already invalidated");
        }
        
        coinHistory.setInvalidate(true);
        coinHistory.setInvalidationReason(invalidationReason);
        coinHistory.setRefund(refund);
        
        coinHistoryRepository.save(coinHistory);
        
        String userId = coinHistory.getUserId();
        log.info("Invalidated coin history {} for user {} with reason: {} (refund: {})", 
                coinHistoryId, userId, invalidationReason, refund);
        
        // If refund is true, add the coins back to the user's balance
        if (refund != null && refund) {
            // Create a refund entry with the opposite coin count to reverse the effect
            // If original was +100, refund should be +100 (same)
            // If original was -100 (withdrawal), refund should be +100 (opposite)
            String refundReason = "refund_" + coinHistory.getReason();
            
            // Calculate the refund amount: multiply by -1 to reverse the effect
            int refundAmount = -coinHistory.getCount();
            
            CoinHistory refundEntry = CoinHistory.builder()
                    .userId(userId)
                    .count(refundAmount) // Opposite count to reverse the original entry
                    .reason(refundReason)
                    .relatedEntityId(coinHistoryId) // Link to the invalidated entry
                    .relatedEntityType(CoinHistoryEntityType.SYSTEM)
                    .invalidate(false)
                    .refund(false)
                    .build();
            
            coinHistoryRepository.save(refundEntry);
            
            log.info("Added refund entry: {} coins for user {} with reason: {} (refunded from: {}, original: {})", 
                    refundAmount, userId, refundReason, coinHistoryId, coinHistory.getCount());
        }
    }
    
    /**
     * Manually add coin history entry (for admin operations, corrections, special rewards)
     */
    public CoinHistory addCoinHistoryManually(String userId, int count, String reason, 
                                            String relatedEntityId, CoinHistoryEntityType relatedEntityType,
                                            Boolean invalidate, String invalidationReason, Boolean refund) {
        // Validate user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        // Create coin history entry
        CoinHistory coinHistory = CoinHistory.builder()
                .userId(userId)
                .count(count)
                .reason(reason)
                .relatedEntityId(relatedEntityId)
                .relatedEntityType(relatedEntityType)
                .invalidate(invalidate != null ? invalidate : false)
                .invalidationReason(invalidationReason)
                .refund(refund != null ? refund : false)
                .build();
        
        CoinHistory savedCoinHistory = coinHistoryRepository.save(coinHistory);
        
        log.info("Manually added coin history: {} coins for user {} with reason: {} (entity: {} - {}, invalidate: {}, refund: {})", 
                count, userId, reason, relatedEntityType, relatedEntityId, invalidate, refund);
        
        return savedCoinHistory;
    }
    
    /**
     * Check and reward for feedback pro milestone (5 resolved feedbacks of type BUG_REPORT or FEATURE_REQUEST)
     */
    public void checkFeedbackProReward(String userId) {
        // Check if user already received this reward
        if (coinHistoryRepository.existsByUserIdAndReason(userId, "feedback_pro")) {
            log.info("User {} already received feedback pro reward", userId);
            return;
        }
        
        // Count resolved feedbacks of type BUG_REPORT or FEATURE_REQUEST
        long resolvedBugReports = feedbackRepository.countByUserIdAndTypeAndStatus(
            userId, Feedback.FeedbackType.BUG_REPORT, Feedback.FeedbackStatus.RESOLVED);
        long resolvedFeatureRequests = feedbackRepository.countByUserIdAndTypeAndStatus(
            userId, Feedback.FeedbackType.FEATURE_REQUEST, Feedback.FeedbackStatus.RESOLVED);
        
        long totalResolvedFeedbacks = resolvedBugReports + resolvedFeatureRequests;
        
        if (totalResolvedFeedbacks >= 5) {
            int rewardPoints = Integer.parseInt(defaultConfigService.getByKey("5_feedbacks_points").getValue());
            addCoins(userId, rewardPoints, "feedback_pro", null, CoinHistoryEntityType.SYSTEM);
            log.info("Awarded {} coins to user {} for feedback pro milestone ({} resolved feedbacks)", 
                rewardPoints, userId, totalResolvedFeedbacks);
        }
    }
    
    /**
     * Get all reward and referral configurations as key-value pairs for frontend display
     */
    public RewardConfigResponse getRewardConfigurations() {
        Map<String, String> rewardConfigs = new HashMap<>();
        Map<String, String> referralConfigs = new HashMap<>();
        
        try {
            // Reward configurations
            rewardConfigs.put("storyActiveReward", 
                defaultConfigService.getByKey("default_story_active_points").getValue());
            rewardConfigs.put("hundredLikesMilestone", 
                defaultConfigService.getByKey("default_100_likes_points").getValue());
            rewardConfigs.put("thousandViewsMilestone", 
                defaultConfigService.getByKey("default_1000_views_points").getValue());
            rewardConfigs.put("feedbackProReward", 
                defaultConfigService.getByKey("5_feedbacks_points").getValue());
            rewardConfigs.put("firstStoryReward", 
                defaultConfigService.getByKey("first_story_5min_reward_coins").getValue());
            rewardConfigs.put("firstStoryMinDuration", 
                defaultConfigService.getByKey("first_story_min_duration_minutes").getValue());
            
            // Referral configurations
            referralConfigs.put("referrerReward", 
                defaultConfigService.getByKey("default_referral_reward_points").getValue());
            referralConfigs.put("referredUserWelcomeBonus", 
                defaultConfigService.getByKey("default_referral_welcome_points").getValue());
            referralConfigs.put("maxReferralsPerUser", 
                defaultConfigService.getByKey("max_referrals_per_user").getValue());
            referralConfigs.put("oneRupeeEqualsInCoins",
                    defaultConfigService.getByKey("1_rupee_equals_in_coins").getValue());
            
            log.info("Retrieved reward configurations: {} reward configs, {} referral configs", 
                rewardConfigs.size(), referralConfigs.size());
            
        } catch (Exception e) {
            log.error("Error retrieving reward configurations: {}", e.getMessage(), e);
            // Set default values if config retrieval fails
            rewardConfigs.put("storyActiveReward", "50");
            rewardConfigs.put("hundredLikesMilestone", "15");
            rewardConfigs.put("thousandViewsMilestone", "15");
            rewardConfigs.put("feedbackProReward", "60");
            rewardConfigs.put("firstStoryReward", "90");
            rewardConfigs.put("firstStoryMinDuration", "5");
            referralConfigs.put("referrerReward", "50");
            referralConfigs.put("referredUserWelcomeBonus", "30");
            referralConfigs.put("maxReferralsPerUser", "100");
        }
        
        return RewardConfigResponse.fromConfigMaps(rewardConfigs, referralConfigs);
    }
}