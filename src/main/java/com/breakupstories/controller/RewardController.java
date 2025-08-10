package com.breakupstories.controller;

import com.breakupstories.dto.AddCoinHistoryRequest;
import com.breakupstories.dto.AddCoinHistoryResponse;
import com.breakupstories.dto.CoinBalanceResponse;
import com.breakupstories.dto.CoinHistoryInvalidationRequest;
import com.breakupstories.dto.ReferralStatsResponse;
import com.breakupstories.dto.RewardConfigResponse;
import com.breakupstories.model.CoinHistory;
import com.breakupstories.service.RewardService;
import com.breakupstories.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
    
    @PutMapping("/coin-history/invalidate")
    @Operation(summary = "Invalidate coin history", description = "Invalidate a coin history entry with reason and refund option (Admin only)")
    public ResponseEntity<String> invalidateCoinHistory(@Valid @RequestBody CoinHistoryInvalidationRequest request) {
        log.info("Invalidating coin history {} with reason: {} (refund: {})", 
                request.getCoinHistoryId(), request.getInvalidationReason(), request.getRefund());
        
        try {
            rewardService.invalidateCoinHistory(
                request.getCoinHistoryId(), 
                request.getInvalidationReason(), 
                request.getRefund()
            );
            
            return ResponseEntity.ok("Coin history invalidated successfully");
        } catch (Exception e) {
            log.error("Error invalidating coin history: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    @PostMapping("/coin-history")
    @Operation(summary = "Add coin history", description = "Manually add a coin history entry (Admin only)")
    public ResponseEntity<AddCoinHistoryResponse> addCoinHistory(@Valid @RequestBody AddCoinHistoryRequest request) {
        log.info("Adding coin history: {} coins for user {} with reason: {}", 
                request.getCount(), request.getUserId(), request.getReason());
        
        try {
            CoinHistory coinHistory = rewardService.addCoinHistoryManually(
                request.getUserId(),
                request.getCount(),
                request.getReason(),
                request.getRelatedEntityId(),
                request.getRelatedEntityType(),
                request.getInvalidate(),
                request.getInvalidationReason(),
                request.getRefund()
            );
            
            // Get updated user balance
            int newUserBalance = rewardService.getValidTotalCoins(request.getUserId());
            
            AddCoinHistoryResponse response = AddCoinHistoryResponse.fromCoinHistory(coinHistory, newUserBalance);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error adding coin history: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                AddCoinHistoryResponse.builder()
                    .message("Error: " + e.getMessage())
                    .build()
            );
        }
    }
} 