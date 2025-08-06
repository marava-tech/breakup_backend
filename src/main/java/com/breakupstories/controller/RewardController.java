package com.breakupstories.controller;

import com.breakupstories.dto.CoinBalanceResponse;
import com.breakupstories.dto.ReferralStatsResponse;
import com.breakupstories.dto.RewardConfigResponse;
import com.breakupstories.service.RewardService;
import com.breakupstories.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rewards")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Rewards", description = "Reward and referral management APIs")
public class RewardController {
    
    private final RewardService rewardService;
    private final UserService userService;
    
    @GetMapping("/coins")
    @Operation(summary = "Get coin balance", description = "Get user's coin balance and transaction history")
    public ResponseEntity<CoinBalanceResponse> getCoinBalance(Authentication authentication) {
        String email = authentication.getName();
        String userId = userService.getUserEntityByEmail(email).getId();
        
        log.info("Coin balance request for user: {}", userId);
        CoinBalanceResponse response = rewardService.getCoinBalance(userId);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/referral-stats")
    @Operation(summary = "Get referral statistics", description = "Get user's referral code and statistics")
    public ResponseEntity<ReferralStatsResponse> getReferralStats(Authentication authentication) {
        String email = authentication.getName();
        String userId = userService.getUserEntityByEmail(email).getId();
        
        log.info("Referral stats request for user: {}", userId);
        ReferralStatsResponse response = rewardService.getReferralStats(userId);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/coins/{userId}")
    @Operation(summary = "Get coin balance by user ID", description = "Get coin balance for a specific user (Admin only)")
    public ResponseEntity<CoinBalanceResponse> getCoinBalanceByUserId(@PathVariable String userId) {
        log.info("Coin balance request for user ID: {}", userId);
        CoinBalanceResponse response = rewardService.getCoinBalance(userId);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/referral-stats/{userId}")
    @Operation(summary = "Get referral statistics by user ID", description = "Get referral stats for a specific user (Admin only)")
    public ResponseEntity<ReferralStatsResponse> getReferralStatsByUserId(@PathVariable String userId) {
        log.info("Referral stats request for user ID: {}", userId);
        ReferralStatsResponse response = rewardService.getReferralStats(userId);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/configurations")
    @Operation(summary = "Get reward configurations", description = "Get all reward and referral configurations as key-value pairs for frontend display")
    public ResponseEntity<RewardConfigResponse> getRewardConfigurations() {
        log.info("Reward configurations request");
        RewardConfigResponse response = rewardService.getRewardConfigurations();
        
        return ResponseEntity.ok(response);
    }
} 