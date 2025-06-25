package com.breakupstories.dto;

import com.breakupstories.model.Audit;
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
public class AuditRequest {
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotNull(message = "Entity type is required")
    private Audit.EntityType entityType;
    
    @NotNull(message = "Action type is required")
    private Audit.ActionType actionType;
    
    @NotBlank(message = "Entity ID is required")
    private String entityId;
} 