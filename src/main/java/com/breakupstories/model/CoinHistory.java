package com.breakupstories.model;

import com.breakupstories.enums.CoinHistoryEntityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "coin_history")
public class CoinHistory {
    
    @Id
    private String id;
    
    private String userId;
    private int count;
    private String reason;
    private String relatedEntityId; // Optional: ID of related entity
    private CoinHistoryEntityType relatedEntityType; // Optional: Type of related entity
    
    @Builder.Default
    private Boolean invalidate = false; // Default false - whether this entry is invalidated
    
    private String invalidationReason; // Default null - reason for invalidation
    
    @Builder.Default
    private Boolean refund = false; // Default false - whether this invalidation should be refunded
    
    @CreatedDate
    private Long createdAt;
} 