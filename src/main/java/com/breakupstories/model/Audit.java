package com.breakupstories.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

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
    
    // Additional metadata for detailed tracking
    private String userAgent;
    private String ipAddress;
    private String sessionId;
    private Map<String, Object> metadata;
    
    @CreatedDate
    private Long createdAt;
    
    @LastModifiedDate
    private Long updatedAt;
    
    public enum EntityType {
        STORY, COMMENT, BOOKMARK, FEEDBACK, USER, AUDIO,  NOTIFICATION
    }
    
    public enum ActionType {
        CREATE, UPDATE, DELETE, VIEW, LIKE, UNLIKE, COMMENT, PLAY, PAUSE, STOP, SHARE, DOWNLOAD , MATCH
    }
    
    // Helper methods for common metadata
    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new java.util.HashMap<>();
        }
        this.metadata.put(key, value);
    }
    
    public Object getMetadata(String key) {
        return this.metadata != null ? this.metadata.get(key) : null;
    }
    
    // Standard getter for the entire metadata map (Lombok will generate this, but we need to ensure it exists)
    public Map<String, Object> getMetadata() {
        return this.metadata;
    }
} 