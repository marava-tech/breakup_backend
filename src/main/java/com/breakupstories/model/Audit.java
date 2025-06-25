package com.breakupstories.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "audits")
public class Audit {
    
    @Id
    private String id;
    
    private String userId;
    private EntityType entityType;
    private ActionType actionType;
    private String entityId;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    public enum EntityType {
        STORY, COMMENT, LIKE, BOOKMARK, FEEDBACK, USER
    }
    
    public enum ActionType {
        CREATE, UPDATE, DELETE
    }
} 