package com.breakupstories.dto;

import com.breakupstories.model.CoinHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoinHistoryResponse {
    
    private String id;
    private int count;
    private String reason;
    private String relatedEntityId;
    private Long createdAt;
    
    public static CoinHistoryResponse fromCoinHistory(CoinHistory coinHistory) {
        return CoinHistoryResponse.builder()
                .id(coinHistory.getId())
                .count(coinHistory.getCount())
                .reason(coinHistory.getReason())
                .relatedEntityId(coinHistory.getRelatedEntityId())
                .createdAt(coinHistory.getCreatedAt())
                .build();
    }
} 