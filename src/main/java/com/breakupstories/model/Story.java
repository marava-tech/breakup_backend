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
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "stories")
public class Story {
    
    @Id
    private String id;
    
    private String userId;
    private String title;
    private String audioUrl;
    private String thumbnailUrl;
    private List<String> storyImages;
    @Builder.Default
    private Long viewCount=0L;
    private Long duration; // Duration in milliseconds
    private StoryStatus status;
    
    private List<Content> contents;
    private List<String> tags;
    private List<Emotion> emotions;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;

    private List<String> rejectionReasons;

    private String language;
    
    private CreationType creationType;
    
    public enum StoryStatus {
        UPLOAD_PENDING, UPLOADING, PROCESSING_PENDING, PROCESSING, PROCESSED, CONVERTING, ACTIVE, INACTIVE, FAILED, REJECTED
    }
    
    public enum CreationType {
        UPLOADED, WRITTEN
    }
} 