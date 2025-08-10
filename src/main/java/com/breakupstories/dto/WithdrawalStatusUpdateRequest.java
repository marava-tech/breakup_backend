package com.breakupstories.dto;

import com.breakupstories.model.Withdrawal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalStatusUpdateRequest {
    
    @NotNull(message = "Status is required")
    private Withdrawal.WithdrawalStatus status;
    
    // Optional comments for the status update
    private String comments;
} 