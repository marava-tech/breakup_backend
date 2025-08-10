package com.breakupstories.dto;

import com.breakupstories.enums.CoinHistoryEntityType;
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
public class AddCoinHistoryRequest {
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotNull(message = "Coin count is required")
    private Integer count;
    
    @NotBlank(message = "Reason is required")
    private String reason;
    
    // Optional: for linking to specific entities (story, user, etc.)
    private String relatedEntityId;
    
    // Optional: type of the related entity for better context
    private CoinHistoryEntityType relatedEntityType;
    
    // Optional: Set invalidation status at creation time
    @Builder.Default
    private Boolean invalidate = false;
    
    // Optional: Set invalidation reason if creating invalidated entry
    private String invalidationReason;
    
    // Optional: Set refund status
    @Builder.Default
    private Boolean refund = false;
}
