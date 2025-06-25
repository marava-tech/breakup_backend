package com.breakupstories.dto;

import com.breakupstories.model.Comment;
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
    private String text;
    private String parentId;
    private List<CommentResponse> replies;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CommentResponse fromComment(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .storyId(comment.getStoryId())
                .userId(comment.getUserId())
                .text(comment.getText())
                .parentId(comment.getParentId())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
} 