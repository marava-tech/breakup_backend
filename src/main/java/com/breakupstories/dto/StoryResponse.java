package com.breakupstories.dto;

import com.breakupstories.model.Content;
import com.breakupstories.model.Emotion;
import com.breakupstories.model.Keyword;
import com.breakupstories.model.Story;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryResponse {
    
    private String id;
    private String userId;
    private String title;
    private String audioUrl;
    private String shareLink;
    private Integer viewCount;
    private Story.StoryStatus status;
    private List<Content> contents;
    private List<String> tags;
    private List<Emotion> emotions;
    private List<Keyword> keywords;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static StoryResponse fromStory(Story story) {
        return StoryResponse.builder()
                .id(story.getId())
                .userId(story.getUserId())
                .title(story.getTitle())
                .audioUrl(story.getAudioUrl())
                .shareLink(story.getShareLink())
                .viewCount(story.getViewCount())
                .status(story.getStatus())
                .contents(story.getContents())
                .tags(story.getTags())
                .emotions(story.getEmotions())
                .keywords(story.getKeywords())
                .createdAt(story.getCreatedAt())
                .updatedAt(story.getUpdatedAt())
                .build();
    }
} 