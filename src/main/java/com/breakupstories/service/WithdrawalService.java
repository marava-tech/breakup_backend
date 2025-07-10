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

@Service
@RequiredArgsConstructor
public class WithdrawalService {
    
    private final WithdrawalRepository withdrawalRepository;
    private final UserRepository userRepository;
    private final CoinHistoryRepository coinHistoryRepository;
    private final DefaultConfigService defaultConfigService;
    
    @Transactional
    public WithdrawalResponse createWithdrawal(String userId, WithdrawalRequest request) {
        // Check if user exists and is active
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!user.getIsActive()) {
            throw new RuntimeException("User account is not active. Cannot create withdrawal.");
        }
        
        // Check if user has enough coins
        if (user.getCoinBalance() < request.getCoins()) {
            throw new RuntimeException("Insufficient coins. Available: " + user.getCoinBalance() + ", Required: " + request.getCoins());
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
        
        // Deduct coins from user account
        user.setCoinBalance(user.getCoinBalance() - request.getCoins());
        userRepository.save(user);
        
        // Add coin history entry
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
        
        Withdrawal.WithdrawalStatus oldStatus = withdrawal.getStatus();
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
} 