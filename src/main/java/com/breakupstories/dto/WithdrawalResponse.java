package com.breakupstories.dto;

import com.breakupstories.model.Withdrawal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalResponse {
    
    private String id;
    private String userId;
    private String username;
    private Integer coins;
    private BigDecimal moneyInRs;
    private String upiId;
    private Withdrawal.WithdrawalStatus status;
    private String withdrawalProofImageUrl;
    private String comments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static WithdrawalResponse fromWithdrawal(Withdrawal withdrawal, String username) {
        return WithdrawalResponse.builder()
                .id(withdrawal.getId())
                .userId(withdrawal.getUserId())
                .username(username)
                .coins(withdrawal.getCoins())
                .moneyInRs(withdrawal.getMoneyInRs())
                .upiId(withdrawal.getUpiId())
                .status(withdrawal.getStatus())
                .withdrawalProofImageUrl(withdrawal.getWithdrawalProofImageUrl())
                .comments(withdrawal.getComments())
                .createdAt(withdrawal.getCreatedAt())
                .updatedAt(withdrawal.getUpdatedAt())
                .build();
    }
} 