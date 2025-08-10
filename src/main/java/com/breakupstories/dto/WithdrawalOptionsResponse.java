package com.breakupstories.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalOptionsResponse {
    
    private List<WithdrawalOptionResponse> options;
    private String defaultProcessingTime;
    private boolean pauseWithdrawals;
    private String pauseWithdrawalsReason;
    private String withdrawalConditions;
    
    public static WithdrawalOptionsResponse of(List<WithdrawalOptionResponse> options, String defaultProcessingTime, boolean pauseWithdrawals, String pauseWithdrawalsReason, String withdrawalConditions) {
        return WithdrawalOptionsResponse.builder()
                .options(options)
                .defaultProcessingTime(defaultProcessingTime)
                .pauseWithdrawals(pauseWithdrawals)
                .pauseWithdrawalsReason(pauseWithdrawalsReason)
                .withdrawalConditions(withdrawalConditions)
                .build();
    }
} 