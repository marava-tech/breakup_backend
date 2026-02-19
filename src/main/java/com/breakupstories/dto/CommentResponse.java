package com.breakupstories.dto;

import com.breakupstories.model.Comment;
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
public class CommentResponse {
    private String id;
    private String storyId;
    private String userId;
    private String username;
    private String text;
    private String parentId;
    private List<CommentResponse> replies;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Support (Like) fields
    private long supportCount;
    private boolean isSupportedByMe;

    // Abuse detection fields
    private boolean isAbusive;
    private String category;
    private String explanation;
    private Double confidence;

    public static CommentResponse fromComment(Comment comment, User user) {
        return CommentResponse.builder()
                .id(comment.getId())
                .storyId(comment.getStoryId())
                .userId(comment.getUserId())
                .username(user != null ? user.getName() : null)
                .text(comment.getText())
                .parentId(comment.getParentId())
                .isAbusive(comment.isAbusive())
                .confidence(comment.getConfidence())
                .category(comment.getCategory())
                .explanation(comment.getExplanation())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    public static CommentResponse fromComment(Comment comment, User user, long supportCount, boolean isSupportedByMe) {
        CommentResponse response = fromComment(comment, user);
        response.setSupportCount(supportCount);
        response.setSupportedByMe(isSupportedByMe);
        return response;
    }
}