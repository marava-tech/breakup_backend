package com.breakupstories.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalOptionResponse {
    
    private BigDecimal amount;
    private Integer coins;
    private boolean isEligible;
    
    public static WithdrawalOptionResponse of(BigDecimal amount, Integer coins, boolean isEligible) {
        return WithdrawalOptionResponse.builder()
                .amount(amount)
                .coins(coins)
                .isEligible(isEligible)
                .build();
    }
} 