package com.breakupstories.dto;

import com.breakupstories.model.Audit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditResponse {
    
    private String id;
    private String userId;
    private String username; // Username for display
    private Audit.EntityType entityType;
    private Audit.ActionType actionType;
    private String entityId;
    
    // Additional metadata fields
    private String userAgent;
    private String ipAddress;
    private String sessionId;
    private Map<String, Object> metadata;
    
    private Long createdAt;
    private Long updatedAt;
    
    public static AuditResponse fromAudit(Audit audit) {
        return AuditResponse.builder()
                .id(audit.getId())
                .userId(audit.getUserId())
                .username(audit.getUsername())
                .entityType(audit.getEntityType())
                .actionType(audit.getActionType())
                .entityId(audit.getEntityId())
                .userAgent(audit.getUserAgent())
                .ipAddress(audit.getIpAddress())
                .sessionId(audit.getSessionId())
                .metadata(audit.getMetadata())
                .createdAt(audit.getCreatedAt())
                .updatedAt(audit.getUpdatedAt())
                .build();
    }
} 