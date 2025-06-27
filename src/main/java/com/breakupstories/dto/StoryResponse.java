package com.breakupstories.dto;

import com.breakupstories.model.Content;
import com.breakupstories.model.Emotion;
import com.breakupstories.model.Story;
import com.breakupstories.model.StoryMetadata;
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
    private String thumbnailUrl;
    private String shareLink;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private Story.StoryStatus status;
    private String language;
    private List<String> rejectionReasons;
    private Boolean isApproved;
    private Boolean isPublic;
    private List<Content> contents;
    private List<String> tags;
    private List<Emotion> emotions;
    private StoryMetadata metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isLikedByMe = false;
    private boolean isBookmarkedByMe = false;

    public static StoryResponse fromStory(Story story, boolean isLikedByMe, long likeCount, long commentCount) {
        return StoryResponse.builder()
                .id(story.getId())
                .userId(story.getUserId())
                .title(story.getTitle())
                .audioUrl(story.getAudioUrl())
                .thumbnailUrl(story.getThumbnailUrl())
                .shareLink(story.getShareLink())
                .language(story.getMetadata() != null ? story.getMetadata().getLanguage() : null)
                .viewCount(story.getViewCount())
                .likeCount(likeCount)
                .commentCount(commentCount)
                .status(story.getStatus())
                .contents(story.getContents())
                .tags(story.getTags())
                .emotions(story.getEmotions())
                .rejectionReasons(story.getRejectionReasons())
                .metadata(story.getMetadata())
                .isLikedByMe(isLikedByMe)
                .isBookmarkedByMe(false) // Will be set by service layer
                .createdAt(story.getCreatedAt())
                .updatedAt(story.getUpdatedAt())
                .build();
    }
    
    public static StoryResponse fromStory(Story story, boolean isLikedByMe, boolean isBookmarkedByMe, long likeCount, long commentCount) {
        return StoryResponse.builder()
                .id(story.getId())
                .userId(story.getUserId())
                .title(story.getTitle())
                .audioUrl(story.getAudioUrl())
                .thumbnailUrl(story.getThumbnailUrl())
                .shareLink(story.getShareLink())
                .language(story.getMetadata() != null ? story.getMetadata().getLanguage() : null)
                .viewCount(story.getViewCount())
                .likeCount(likeCount)
                .commentCount(commentCount)
                .status(story.getStatus())
                .contents(story.getContents())
                .tags(story.getTags())
                .emotions(story.getEmotions())
                .rejectionReasons(story.getRejectionReasons())
                .metadata(story.getMetadata())
                .isLikedByMe(isLikedByMe)
                .isBookmarkedByMe(isBookmarkedByMe)
                .createdAt(story.getCreatedAt())
                .updatedAt(story.getUpdatedAt())
                .build();
    }
    
    public static StoryResponse fromStory(Story story) {
        return StoryResponse.builder()
                .id(story.getId())
                .userId(story.getUserId())
                .title(story.getTitle())
                .audioUrl(story.getAudioUrl())
                .thumbnailUrl(story.getThumbnailUrl())
                .shareLink(story.getShareLink())
                .language(story.getMetadata() != null ? story.getMetadata().getLanguage() : null)
                .viewCount(story.getViewCount())
                .status(story.getStatus())
                .contents(story.getContents())
                .tags(story.getTags())
                .emotions(story.getEmotions())
                .rejectionReasons(story.getRejectionReasons())
                .metadata(story.getMetadata())
                .createdAt(story.getCreatedAt())
                .updatedAt(story.getUpdatedAt())
                .build();
    }
} 