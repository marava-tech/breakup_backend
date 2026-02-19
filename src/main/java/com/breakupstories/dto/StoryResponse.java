package com.breakupstories.dto;

import com.breakupstories.model.Content;
import com.breakupstories.model.Story;
import com.breakupstories.model.User;
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
    private String username;
    private String title;
    private String audioUrl;
    @Deprecated
    private String thumbnailUrl;
    private String coverImageUrl;
    private String author;
    private List<String> storyImages;
    private Long viewCount;
    private Long playCount;
    private Long completionCount;
    private String spotifyUrl;
    private Long likeCount;
    private Long commentCount;
    private Story.StoryStatus status;
    private String language;
    private List<String> rejectionReasons;
    @Deprecated
    private List<Content> contents;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isLikedByMe;
    private boolean isBookmarkedByMe;
    private Story.CreationType creationType;
    private Story.Category category;

    private String shareUrl;

    private static final String SHARE_URL_BASE = "https://breakupstories.com/story/";

    public static StoryResponse fromStory(Story story, User user, boolean isLikedByMe, long likeCount,
            long commentCount) {
        return StoryResponse.builder()
                .id(story.getId())
                .userId(story.getUserId())
                .username(user != null ? user.getName() : null)
                .title(story.getTitle())
                .audioUrl(story.getAudioUrl())
                .thumbnailUrl(story.getThumbnailUrl())
                .coverImageUrl(story.getCoverImageUrl())
                .author(story.getAuthor() != null ? story.getAuthor() : (user != null ? user.getName() : null))
                .storyImages(story.getStoryImages())
                .viewCount(story.getViewCount())
                .playCount(story.getPlayCount())
                .completionCount(story.getCompletionCount())
                .spotifyUrl(story.getSpotifyUrl())
                .shareUrl(SHARE_URL_BASE + story.getId())
                .likeCount(likeCount)
                .commentCount(commentCount)
                .status(story.getStatus())
                .tags(story.getTags())
                .contents(story.getContents())
                .language(story.getLanguage())
                .rejectionReasons(story.getRejectionReasons())
                .isLikedByMe(isLikedByMe)
                .isBookmarkedByMe(false) // Will be set by service layer
                .creationType(story.getCreationType() != null ? story.getCreationType() : Story.CreationType.UPLOADED)
                .category(story.getCategory())
                .createdAt(story.getCreatedAt())
                .updatedAt(story.getUpdatedAt())
                .build();
    }

    public static StoryResponse fromStory(Story story, User user, boolean isLikedByMe, boolean isBookmarkedByMe,
            long likeCount, long commentCount) {
        return StoryResponse.builder()
                .id(story.getId())
                .userId(story.getUserId())
                .username(user != null ? user.getName() : null)
                .title(story.getTitle())
                .audioUrl(story.getAudioUrl())
                .thumbnailUrl(story.getThumbnailUrl())
                .coverImageUrl(story.getCoverImageUrl())
                .author(story.getAuthor() != null ? story.getAuthor() : (user != null ? user.getName() : null))
                .storyImages(story.getStoryImages())
                .language(story.getLanguage())
                .tags(story.getTags())
                .viewCount(story.getViewCount())
                .playCount(story.getPlayCount())
                .completionCount(story.getCompletionCount())
                .spotifyUrl(story.getSpotifyUrl())
                .shareUrl(SHARE_URL_BASE + story.getId())
                .likeCount(likeCount)
                .commentCount(commentCount)
                .status(story.getStatus())
                .contents(story.getContents())
                .rejectionReasons(story.getRejectionReasons())
                .isLikedByMe(isLikedByMe)
                .isBookmarkedByMe(isBookmarkedByMe)
                .creationType(story.getCreationType() != null ? story.getCreationType() : Story.CreationType.UPLOADED)
                .category(story.getCategory())
                .createdAt(story.getCreatedAt())
                .updatedAt(story.getUpdatedAt())
                .build();
    }

    public static StoryResponse fromStory(Story story, User user) {
        return StoryResponse.builder()
                .id(story.getId())
                .userId(story.getUserId())
                .username(user != null ? user.getName() : null)
                .title(story.getTitle())
                .audioUrl(story.getAudioUrl())
                .thumbnailUrl(story.getThumbnailUrl())
                .coverImageUrl(story.getCoverImageUrl())
                .author(story.getAuthor() != null ? story.getAuthor() : (user != null ? user.getName() : null))
                .storyImages(story.getStoryImages())
                .language(story.getLanguage())
                .tags(story.getTags())
                .viewCount(story.getViewCount())
                .playCount(story.getPlayCount())
                .completionCount(story.getCompletionCount())
                .spotifyUrl(story.getSpotifyUrl())
                .shareUrl(SHARE_URL_BASE + story.getId())
                .status(story.getStatus())
                .contents(story.getContents())
                .rejectionReasons(story.getRejectionReasons())
                .creationType(story.getCreationType() != null ? story.getCreationType() : Story.CreationType.UPLOADED)
                .category(story.getCategory())
                .createdAt(story.getCreatedAt())
                .updatedAt(story.getUpdatedAt())
                .build();
    }
}