package com.breakupstories.service;

import com.breakupstories.dto.WithdrawalRequest;
import com.breakupstories.dto.WithdrawalResponse;
import com.breakupstories.model.CoinHistory;
import com.breakupstories.model.User;
import com.breakupstories.model.Withdrawal;
import com.breakupstories.repository.CoinHistoryRepository;
import com.breakupstories.repository.UserRepository;
import com.breakupstories.repository.WithdrawalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.breakupstories.dto.WithdrawalOptionResponse;
import com.breakupstories.dto.WithdrawalOptionsResponse;

@Service
@RequiredArgsConstructor
public class WithdrawalService {
    
    private final WithdrawalRepository withdrawalRepository;
    private final UserRepository userRepository;
    private final CoinHistoryRepository coinHistoryRepository;
    private final DefaultConfigService defaultConfigService;
    private final RewardService rewardService;
    
    @Transactional
    public WithdrawalResponse createWithdrawal(String userId, WithdrawalRequest request) {
        // Check if user exists and is active
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!user.getIsActive()) {
            throw new RuntimeException("User account is not active. Cannot create withdrawal.");
        }
        
        // Check if user has enough coins using coin history calculation
        int availableCoins = rewardService.getValidTotalCoins(userId);
        if (availableCoins < request.getCoins()) {
            throw new RuntimeException("Insufficient coins. Available: " + availableCoins + ", Required: " + request.getCoins());
        }
        
        // Get conversion rate from config
        BigDecimal coinToRupeeRate;
        try {
            String rateString = defaultConfigService.getByKey("1_rupee_equals_in_coins").getValue();
            coinToRupeeRate = new BigDecimal(rateString);
        } catch (Exception e) {
            // Fallback to default rate if config not found
            coinToRupeeRate = BigDecimal.valueOf(2);
        }
        
        // Calculate money in rupees
        BigDecimal moneyInRs = BigDecimal.valueOf(request.getCoins()).divide(coinToRupeeRate, 2, RoundingMode.HALF_UP);
        
        // Create withdrawal
        Withdrawal withdrawal = Withdrawal.builder()
                .userId(userId)
                .coins(request.getCoins())
                .moneyInRs(moneyInRs)
                .upiId(request.getUpiId())
                .status(Withdrawal.WithdrawalStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        Withdrawal savedWithdrawal = withdrawalRepository.save(withdrawal);
        
        // Add coin history entry for the withdrawal (negative coins)
        CoinHistory coinHistory = CoinHistory.builder()
                .userId(userId)
                .count(-request.getCoins()) // Negative for withdrawal
                .reason("Withdrawal")
                .createdAt(System.currentTimeMillis())
                .build();
        
        coinHistoryRepository.save(coinHistory);
        
        return WithdrawalResponse.fromWithdrawal(savedWithdrawal, user.getName());
    }
    
    public WithdrawalResponse updateWithdrawalStatus(String withdrawalId, Withdrawal.WithdrawalStatus newStatus, String proofImageUrl) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Withdrawal not found"));
        
        withdrawal.setStatus(newStatus);
        withdrawal.setUpdatedAt(LocalDateTime.now());
        
        // Set proof image URL if provided
        if (proofImageUrl != null && !proofImageUrl.trim().isEmpty()) {
            withdrawal.setWithdrawalProofImageUrl(proofImageUrl);
        }
        
        Withdrawal updatedWithdrawal = withdrawalRepository.save(withdrawal);
        
        // If status is being rejected and it was previously pending/processing, don't return coins
        // (as per requirement: on reject withdrawal we won't add the coins again to his account)
        
        User user = userRepository.findById(withdrawal.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return WithdrawalResponse.fromWithdrawal(updatedWithdrawal, user.getName());
    }
    
    public WithdrawalResponse updateWithdrawalStatus(String withdrawalId, Withdrawal.WithdrawalStatus newStatus, String proofImageUrl, String comments) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Withdrawal not found"));
        
        withdrawal.setStatus(newStatus);
        withdrawal.setUpdatedAt(LocalDateTime.now());
        
        // Set proof image URL if provided
        if (proofImageUrl != null && !proofImageUrl.trim().isEmpty()) {
            withdrawal.setWithdrawalProofImageUrl(proofImageUrl);
        }
        
        // Set comments if provided
        if (comments != null && !comments.trim().isEmpty()) {
            withdrawal.setComments(comments);
        }
        
        Withdrawal updatedWithdrawal = withdrawalRepository.save(withdrawal);
        
        // If status is being rejected and it was previously pending/processing, don't return coins
        // (as per requirement: on reject withdrawal we won't add the coins again to his account)
        
        User user = userRepository.findById(withdrawal.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return WithdrawalResponse.fromWithdrawal(updatedWithdrawal, user.getName());
    }
    
    public WithdrawalResponse getWithdrawalById(String withdrawalId) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Withdrawal not found"));
        
        User user = userRepository.findById(withdrawal.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return WithdrawalResponse.fromWithdrawal(withdrawal, user.getName());
    }
    
    public List<WithdrawalResponse> getWithdrawalsByUser(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Withdrawal> withdrawalPage = withdrawalRepository.findByUserId(userId, pageable);
        
        return withdrawalPage.getContent().stream()
                .map(withdrawal -> {
                    User user = userRepository.findById(withdrawal.getUserId())
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    return WithdrawalResponse.fromWithdrawal(withdrawal, user.getName());
                })
                .collect(Collectors.toList());
    }
    
    public List<WithdrawalResponse> getWithdrawalsByStatus(Withdrawal.WithdrawalStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Withdrawal> withdrawalPage = withdrawalRepository.findByStatus(status, pageable);
        
        return withdrawalPage.getContent().stream()
                .map(withdrawal -> {
                    User user = userRepository.findById(withdrawal.getUserId())
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    return WithdrawalResponse.fromWithdrawal(withdrawal, user.getName());
                })
                .collect(Collectors.toList());
    }
    
    public List<WithdrawalResponse> getAllWithdrawals(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Withdrawal> withdrawalPage = withdrawalRepository.findAll(pageable);
        
        return withdrawalPage.getContent().stream()
                .map(withdrawal -> {
                    User user = userRepository.findById(withdrawal.getUserId())
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    return WithdrawalResponse.fromWithdrawal(withdrawal, user.getName());
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Get withdrawal options for a user
     * 
     * @param userId The user ID to check eligibility
     * @return Withdrawal options response with amounts, coins, eligibility, processing time, and pause configuration
     */
    public WithdrawalOptionsResponse getWithdrawalOptions(String userId) {
        // Hardcoded amounts as per requirement (with 2 decimal places for consistency)
        BigDecimal[] amounts = {
            BigDecimal.valueOf(30.00),
            BigDecimal.valueOf(90.00),
            BigDecimal.valueOf(190.00),
            BigDecimal.valueOf(500.00)
        };
        
        // Get conversion rate from config
        BigDecimal coinToRupeeRate = getCoinToRupeeRate();
        
        // Get default processing time from config
        String defaultProcessingTime = getDefaultProcessingTime();
        
        // Get pause configuration from config
        boolean pauseWithdrawals = getPauseWithdrawals();
        String pauseWithdrawalsReason = getPauseWithdrawalsReason();
        
        // Get withdrawal conditions from config
        String withdrawalConditions = getWithdrawalConditions();
        
        List<WithdrawalOptionResponse> options = Stream.of(amounts)
                .map(amount -> {
                    // Calculate coins needed for this amount
                    Integer coins = amount.multiply(coinToRupeeRate).intValue();
                    
                    // Format amount to match the stored format (2 decimal places)
                    BigDecimal formattedAmount = amount.setScale(2, RoundingMode.HALF_UP);
                    
                    // User is eligible if:
                    // 1. No existing withdrawal for this amount, OR
                    // 2. Existing withdrawal is REJECTED (user can try again)
                    boolean hasNonRejectedWithdrawal = withdrawalRepository.existsByUserIdAndMoneyInRsAndStatusNot(
                        userId, 
                        formattedAmount, 
                        Withdrawal.WithdrawalStatus.REJECTED
                    );
                    
                    boolean isEligible = !hasNonRejectedWithdrawal;
                    
                    return WithdrawalOptionResponse.of(amount, coins, isEligible);
                })
                .collect(Collectors.toList());
        
        return WithdrawalOptionsResponse.of(options, defaultProcessingTime, pauseWithdrawals, pauseWithdrawalsReason, withdrawalConditions);
    }
    
    /**
     * Get coin to rupee conversion rate from config
     */
    private BigDecimal getCoinToRupeeRate() {
        try {
            String rateString = defaultConfigService.getByKey("1_rupee_equals_in_coins").getValue();
            return new BigDecimal(rateString);
        } catch (Exception e) {
            // Fallback to default rate if config not found
            return BigDecimal.valueOf(2);
        }
    }
    
    /**
     * Get default processing time from config
     */
    private String getDefaultProcessingTime() {
        try {
            return defaultConfigService.getByKey("default_payment_processing_time").getValue();
        } catch (Exception e) {
            // Fallback to default processing time if config not found
            return "3-5 business days";
        }
    }
    
    /**
     * Get pause withdrawals configuration from config
     */
    private boolean getPauseWithdrawals() {
        try {
            String pauseWithdrawalsString = defaultConfigService.getByKey("pause_withdrawls").getValue();
            return Boolean.parseBoolean(pauseWithdrawalsString);
        } catch (Exception e) {
            // Fallback to false if config not found
            return false;
        }
    }
    
    /**
     * Get pause withdrawals reason from config
     */
    private String getPauseWithdrawalsReason() {
        try {
            return defaultConfigService.getByKey("pause_withdrawls_reason").getValue();
        } catch (Exception e) {
            // Fallback to default reason if config not found
            return "Withdrawals are temporarily paused";
        }
    }

    /**
     * Get withdrawal conditions from config
     */
    private String getWithdrawalConditions() {
        try {
            return defaultConfigService.getByKey("withdrawal_conditions").getValue();
        } catch (Exception e) {
            // Fallback to default conditions if config not found
            return "No specific conditions for withdrawal.";
        }
    }
} 