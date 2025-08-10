package com.breakupstories.dto;

import com.breakupstories.enums.CoinHistoryEntityType;
import com.breakupstories.model.CoinHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddCoinHistoryResponse {
    
    private String coinHistoryId;
    private String userId;
    private Integer count;
    private String reason;
    private String relatedEntityId;
    private CoinHistoryEntityType relatedEntityType;
    private Boolean invalidate;
    private String invalidationReason;
    private Boolean refund;
    private Long createdAt;
    private Integer newUserBalance;
    private String message;
    
    public static AddCoinHistoryResponse fromCoinHistory(CoinHistory coinHistory, Integer newUserBalance) {
        return AddCoinHistoryResponse.builder()
                .coinHistoryId(coinHistory.getId())
                .userId(coinHistory.getUserId())
                .count(coinHistory.getCount())
                .reason(coinHistory.getReason())
                .relatedEntityId(coinHistory.getRelatedEntityId())
                .relatedEntityType(coinHistory.getRelatedEntityType())
                .invalidate(coinHistory.getInvalidate())
                .invalidationReason(coinHistory.getInvalidationReason())
                .refund(coinHistory.getRefund())
                .createdAt(coinHistory.getCreatedAt())
                .newUserBalance(newUserBalance)
                .message("Coin history added successfully")
                .build();
    }
}
