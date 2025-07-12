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
    
    public static WithdrawalOptionsResponse of(List<WithdrawalOptionResponse> options, String defaultProcessingTime) {
        return WithdrawalOptionsResponse.builder()
                .options(options)
                .defaultProcessingTime(defaultProcessingTime)
                .build();
    }
} 