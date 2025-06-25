package com.breakupstories.dto;

import com.breakupstories.model.Audit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditResponse {
    
    private String id;
    private String userId;
    private Audit.EntityType entityType;
    private Audit.ActionType actionType;
    private String entityId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static AuditResponse fromAudit(Audit audit) {
        return AuditResponse.builder()
                .id(audit.getId())
                .userId(audit.getUserId())
                .entityType(audit.getEntityType())
                .actionType(audit.getActionType())
                .entityId(audit.getEntityId())
                .createdAt(audit.getCreatedAt())
                .updatedAt(audit.getUpdatedAt())
                .build();
    }
} 