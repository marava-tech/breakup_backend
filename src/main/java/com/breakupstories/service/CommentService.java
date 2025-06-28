package com.breakupstories.service;

import com.breakupstories.dto.CommentRequest;
import com.breakupstories.dto.CommentResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.model.Comment;
import com.breakupstories.model.User;
import com.breakupstories.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {
    
    private final CommentRepository commentRepository;
    private final UserService userService;
    
    public CommentResponse createComment(String userId, CommentRequest request) {
        log.info("User {} creating comment on story {}", userId, request.getStoryId());
        
        Comment comment = Comment.builder()
                .storyId(request.getStoryId())
                .userId(userId)
                .text(request.getText())
                .active(true)
                .parentId(request.getParentId())
                .build();
        
        Comment savedComment = commentRepository.save(comment);
        log.info("Comment created with ID: {}", savedComment.getId());
        
        // Fetch user information to include username
        User user = userService.getUserEntityById(userId);
        return CommentResponse.fromComment(savedComment, user);
    }
    
    /**
     * Get comments for a story with nested replies
     * @param storyId The story ID
     * @param page Page number
     * @param size Page size
     * @return PagedResponse of comments with nested replies
     */
    public PagedResponse<CommentResponse> getCommentsByStory(String storyId, int page, int size) {
        log.info("Getting comments for story {} (page: {}, size: {})", storyId, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> commentPage = commentRepository.findByStoryIdAndParentIdIsNullAndActiveTrue(storyId, pageable);
        
        List<CommentResponse> comments = commentPage.getContent().stream()
                .map(this::buildCommentWithReplies)
                .collect(Collectors.toList());
        
        return PagedResponse.of(comments, page, size, commentPage.getTotalElements());
    }
    
    /**
     * Get all comments for a story (including replies) without pagination
     * @param storyId The story ID
     * @return List of all comments with nested replies
     */
    public List<CommentResponse> getAllCommentsByStory(String storyId) {
        log.info("Getting all comments for story {}", storyId);
        
        // Get all top-level comments (active only)
        List<Comment> topLevelComments = commentRepository.findByStoryIdAndParentIdIsNullAndActiveTrue(storyId);
        
        return topLevelComments.stream()
                .map(this::buildCommentWithReplies)
                .collect(Collectors.toList());
    }
    
    /**
     * Build a comment with all its nested replies
     * @param comment The parent comment
     * @return CommentResponse with nested replies
     */
    private CommentResponse buildCommentWithReplies(Comment comment) {
        // Fetch user information to include username
        User user = userService.getUserEntityById(comment.getUserId());
        CommentResponse response = CommentResponse.fromComment(comment, user);
        
        // Get all replies for this comment (active only)
        List<Comment> replies = commentRepository.findByParentIdAndActiveTrue(comment.getId());
        
        // Recursively build replies (for nested replies)
        List<CommentResponse> replyResponses = replies.stream()
                .map(this::buildCommentWithReplies)
                .collect(Collectors.toList());
        
        response.setReplies(replyResponses);
        return response;
    }
    
    /**
     * Get comment count for a story
     * @param storyId The story ID
     * @return Total number of comments (including replies)
     */
    public long getCommentCount(String storyId) {
        return commentRepository.countByStoryIdAndActiveTrue(storyId);
    }
    
    public PagedResponse<CommentResponse> getComments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> commentPage = commentRepository.findAll(pageable);
        
        List<CommentResponse> comments = commentPage.getContent().stream()
                .filter(Comment::isActive) // Filter out inactive comments
                .map(comment -> {
                    User user = userService.getUserEntityById(comment.getUserId());
                    return CommentResponse.fromComment(comment, user);
                })
                .collect(Collectors.toList());
        
        return PagedResponse.of(comments, page, size, commentPage.getTotalElements());
    }
    
    public PagedResponse<CommentResponse> getCommentsByUser(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> commentPage = commentRepository.findByUserIdAndActiveTrue(userId, pageable);
        
        List<CommentResponse> comments = commentPage.getContent().stream()
                .map(comment -> {
                    User user = userService.getUserEntityById(comment.getUserId());
                    return CommentResponse.fromComment(comment, user);
                })
                .collect(Collectors.toList());
        
        return PagedResponse.of(comments, page, size, commentPage.getTotalElements());
    }
    
    public List<CommentResponse> getRepliesByComment(String commentId) {
        List<Comment> replies = commentRepository.findByParentIdAndActiveTrue(commentId);
        return replies.stream()
                .map(comment -> {
                    User user = userService.getUserEntityById(comment.getUserId());
                    return CommentResponse.fromComment(comment, user);
                })
                .collect(Collectors.toList());
    }
    
    public CommentResponse getCommentById(String commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found with ID: " + commentId));
        
        return buildCommentWithReplies(comment);
    }
    
    public CommentResponse updateComment(String commentId, String userId, CommentRequest request) {
        log.info("User {} updating comment {}", userId, commentId);
        
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found with ID: " + commentId));
        
        // Check if user owns this comment
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("You can only update your own comments");
        }
        
        comment.setText(request.getText());
        
        Comment updatedComment = commentRepository.save(comment);
        log.info("Comment {} updated successfully", commentId);
        
        // Fetch user information to include username
        User user = userService.getUserEntityById(userId);
        return CommentResponse.fromComment(updatedComment, user);
    }
    
    public void deleteComment(String commentId, String userId) {
        log.info("User {} deleting comment {}", userId, commentId);
        
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found with ID: " + commentId));
        
        // Check if user owns this comment
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("You can only delete your own comments");
        }
        
        // Delete all replies first (recursively)
        deleteCommentAndReplies(commentId);
        
        log.info("Comment {} and all its replies deleted successfully", commentId);
    }
    
    /**
     * Recursively delete a comment and all its replies
     * @param commentId The comment ID to delete
     */
    private void deleteCommentAndReplies(String commentId) {
        // Get all replies
        List<Comment> replies = commentRepository.findByParentId(commentId);
        
        // Recursively delete all replies
        for (Comment reply : replies) {
            deleteCommentAndReplies(reply.getId());
        }
        
        // Delete the comment itself
        commentRepository.deleteById(commentId);
    }
} 