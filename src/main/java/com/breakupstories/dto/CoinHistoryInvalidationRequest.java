package com.breakupstories.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoinHistoryInvalidationRequest {
    
    @NotBlank(message = "Coin history ID is required")
    private String coinHistoryId;
    
    @NotBlank(message = "Invalidation reason is required")
    private String invalidationReason;
    
    @NotNull(message = "Refund flag is required")
    private Boolean refund;
}
