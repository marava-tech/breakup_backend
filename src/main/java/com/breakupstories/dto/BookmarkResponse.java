package com.breakupstories.dto;

import com.breakupstories.model.Bookmark;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkResponse {
    private String id;
    private String userId;
    private String storyId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BookmarkResponse fromBookmark(Bookmark bookmark) {
        return BookmarkResponse.builder()
                .id(bookmark.getId())
                .userId(bookmark.getUserId())
                .storyId(bookmark.getStoryId())
                .createdAt(bookmark.getCreatedAt())
                .updatedAt(bookmark.getUpdatedAt())
                .build();
    }
} 