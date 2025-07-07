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
@Document(collection = "comments")
public class Comment {
    
    @Id
    private String id;
    
    private String storyId;
    private String userId;
    private String parentId; // nullable for replies
    private String text;
    private boolean active = true; // default to true for new comments
    
    // Abuse detection fields
    @Builder.Default
    private boolean isAbusive = false; // default to false for new comments
    private Double confidence;
    private String category; // category of abuse if detected
    private String explanation; // explanation of why comment was flagged as abusive
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
} 