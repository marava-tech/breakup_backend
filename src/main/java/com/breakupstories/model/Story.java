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
    private String shareLink;
    private Integer viewCount;
    private StoryStatus status;
    
    private List<Content> contents;
    private List<String> tags;
    private List<Emotion> emotions;
    private List<Keyword> keywords;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    public enum StoryStatus {
        PROCESSING, ACTIVE, DISABLED
    }
} 