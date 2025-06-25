package com.breakupstories.service;

import com.breakupstories.dto.CommentRequest;
import com.breakupstories.dto.CommentResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.model.Comment;
import com.breakupstories.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {
    
    private final CommentRepository commentRepository;
    
    public CommentResponse createComment(String userId, CommentRequest request) {
        Comment comment = Comment.builder()
                .storyId(request.getStoryId())
                .userId(userId)
                .text(request.getText())
                .parentId(request.getParentId())
                .build();
        
        Comment savedComment = commentRepository.save(comment);
        return CommentResponse.fromComment(savedComment);
    }
    
    public PagedResponse<CommentResponse> getComments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> commentPage = commentRepository.findAll(pageable);
        
        List<CommentResponse> comments = commentPage.getContent().stream()
                .map(CommentResponse::fromComment)
                .collect(Collectors.toList());
        
        return PagedResponse.of(comments, page, size, commentPage.getTotalElements());
    }
    
    public PagedResponse<CommentResponse> getCommentsByStory(String storyId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> commentPage = commentRepository.findByStoryId(storyId, pageable);
        
        List<CommentResponse> comments = commentPage.getContent().stream()
                .map(CommentResponse::fromComment)
                .collect(Collectors.toList());
        
        return PagedResponse.of(comments, page, size, commentPage.getTotalElements());
    }
    
    public PagedResponse<CommentResponse> getCommentsByUser(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> commentPage = commentRepository.findByUserId(userId, pageable);
        
        List<CommentResponse> comments = commentPage.getContent().stream()
                .map(CommentResponse::fromComment)
                .collect(Collectors.toList());
        
        return PagedResponse.of(comments, page, size, commentPage.getTotalElements());
    }
    
    public List<CommentResponse> getRepliesByComment(String commentId) {
        List<Comment> replies = commentRepository.findByParentId(commentId);
        return replies.stream()
                .map(CommentResponse::fromComment)
                .collect(Collectors.toList());
    }
    
    public CommentResponse getCommentById(String commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found with ID: " + commentId));
        
        return CommentResponse.fromComment(comment);
    }
    
    public CommentResponse updateComment(String commentId, String userId, CommentRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found with ID: " + commentId));
        
        // Check if user owns this comment
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("You can only update your own comments");
        }
        
        comment.setText(request.getText());
        
        Comment updatedComment = commentRepository.save(comment);
        return CommentResponse.fromComment(updatedComment);
    }
    
    public void deleteComment(String commentId, String userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found with ID: " + commentId));
        
        // Check if user owns this comment
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("You can only delete your own comments");
        }
        
        // Delete all replies first
        List<Comment> replies = commentRepository.findByParentId(commentId);
        commentRepository.deleteAll(replies);
        
        // Delete the comment
        commentRepository.deleteById(commentId);
    }
} 