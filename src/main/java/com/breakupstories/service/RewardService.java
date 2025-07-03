package com.breakupstories.service;

import com.breakupstories.dto.CoinBalanceResponse;
import com.breakupstories.dto.ReferralStatsResponse;
import com.breakupstories.model.CoinHistory;
import com.breakupstories.model.Story;
import com.breakupstories.model.User;
import com.breakupstories.repository.CoinHistoryRepository;
import com.breakupstories.repository.StoryRepository;
import com.breakupstories.repository.UserRepository;
import com.breakupstories.repository.FeedbackRepository;
import com.breakupstories.service.DefaultConfigService;
import com.breakupstories.util.ApplicationContextProvider;
import com.breakupstories.dto.RewardConfigResponse;
import com.breakupstories.model.Feedback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
    private static final long STORY_DURATION_THRESHOLD = 600000; // 10 minutes in milliseconds
    
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
     * Get coin balance and history for a user
     */
    public CoinBalanceResponse getCoinBalance(String userId) {
        List<CoinHistory> coinHistory = coinHistoryRepository.findByUserId(userId);
        int totalCoins = coinHistory.stream()
                .mapToInt(CoinHistory::getCount)
                .sum();
        
        return CoinBalanceResponse.builder()
                .totalCoins(totalCoins)
                .coinHistory(coinHistory)
                .build();
    }
    
    /**
     * Add coins to user's balance
     */
    public void addCoins(String userId, int count, String reason, String relatedEntityId) {
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
                .build();
        
        coinHistoryRepository.save(coinHistory);
        
        // Update user's coin balance
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        user.setCoinBalance(user.getCoinBalance() + count);
        userRepository.save(user);
        
        log.info("Added {} coins to user {} for reason: {}", count, userId, reason);
    }
    
    /**
     * Deduct coins from user's balance
     */
    public boolean deductCoins(String userId, int count, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        if (user.getCoinBalance() < count) {
            log.warn("Insufficient coins for user {}. Required: {}, Available: {}", 
                    userId, count, user.getCoinBalance());
            return false;
        }
        
        // Create coin history entry (negative count for deduction)
        CoinHistory coinHistory = CoinHistory.builder()
                .userId(userId)
                .count(-count)
                .reason(reason)
                .build();
        
        coinHistoryRepository.save(coinHistory);
        
        // Update user's coin balance
        user.setCoinBalance(user.getCoinBalance() - count);
        userRepository.save(user);
        
        log.info("Deducted {} coins from user {} for reason: {}", count, userId, reason);
        return true;
    }
    
    /**
     * Check and reward for story becoming active with duration > 10 minutes
     */
    public void checkStoryActiveReward(String storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found with ID: " + storyId));
        
        if (story.getStatus() == Story.StoryStatus.ACTIVE && 
            story.getDuration() != null &&
            story.getDuration() > STORY_DURATION_THRESHOLD) {
            
            addCoins(story.getUserId(),Integer.parseInt(defaultConfigService.getByKey("default_story_active_points").getValue()) , "10m_plus_story_active", storyId);
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
            addCoins(story.getUserId(), Integer.parseInt(defaultConfigService.getByKey("default_100_likes_points").getValue()), "100_plus_likes_milestone", storyId);
        }
    }
    
    /**
     * Check and reward for views milestone (1000 views)
     */
    public void checkViewsMilestoneReward(String storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found with ID: " + storyId));
        
        if (story.getViewCount() != null && story.getViewCount() >= VIEWS_MILESTONE) {
            addCoins(story.getUserId(), Integer.parseInt(defaultConfigService.getByKey("default_1000_views_points").getValue()), "1000_plus_views_milestone", storyId);
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
     * Process referral when a new user signs up
     */
    public void processReferral(String newUserId, String referralCode) {
        Optional<User> referrer = userRepository.findByReferralCode(referralCode);
        
        if (referrer.isPresent()) {
            User newUser = userRepository.findById(newUserId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + newUserId));
            
            // Update new user's referred by field
            newUser.setReferredBy(referrer.get().getId());
            userRepository.save(newUser);

           int referralRewardPoints =  Integer.parseInt(defaultConfigService.getByKey("default_referral_reward_points").getValue());
            int referralWelcomePoints =  Integer.parseInt(defaultConfigService.getByKey("default_referral_welcome_points").getValue());

            // Reward the referrer
            addCoins(referrer.get().getId(), referralRewardPoints, "referral_reward_"+newUser.getName(), newUserId);
            
            // Reward the referred user
            addCoins(newUserId, referralWelcomePoints, "referral_welcome", referrer.get().getId());
            
            log.info("Processed referral: {} referred {} (referrer: {} coins, referred: {} coins)", 
                    referrer.get().getId(), newUserId, referralRewardPoints, referralWelcomePoints);
        } else {
            log.warn("Invalid referral code: {}", referralCode);
        }
    }
    
    /**
     * Get referral statistics for a user
     */
    public ReferralStatsResponse getReferralStats(String userId) {
        List<User> referredUsers = userRepository.findByReferredBy(userId);
        
        return ReferralStatsResponse.builder()
                .referralCode(userRepository.findById(userId).map(User::getReferralCode).orElse(null))
                .referredBy(userRepository.findById(userId).map(User::getReferredBy).orElse(null))
                .referredUsersCount(referredUsers.size())
                .referredUsers(referredUsers.stream().map(User::getId).toList())
                .build();
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
            addCoins(userId, rewardPoints, "feedback_pro", null);
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
            referralConfigs.put("referrerReward", "50");
            referralConfigs.put("referredUserWelcomeBonus", "30");
            referralConfigs.put("maxReferralsPerUser", "100");
        }
        
        return RewardConfigResponse.fromConfigMaps(rewardConfigs, referralConfigs);
    }
    
} 