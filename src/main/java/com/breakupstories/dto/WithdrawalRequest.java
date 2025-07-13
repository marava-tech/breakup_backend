package com.breakupstories.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalRequest {
    
    @NotNull(message = "Coins amount is required")
    @Min(value = 1, message = "Coins must be at least 1")
    private Integer coins;
    
    @NotBlank(message = "UPI ID is required")
    private String upiId;
} 