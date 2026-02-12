package com.breakupstories.controller;

import com.breakupstories.dto.CommentRequest;
import com.breakupstories.dto.CommentResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.service.AuditService;
import com.breakupstories.service.ClientInfoService;
import com.breakupstories.service.CommentService;
import com.breakupstories.service.StoryService;
import com.breakupstories.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Comments", description = "Comment management APIs")
public class CommentController {
    
    private final CommentService commentService;
    private final StoryService storyService;
    private final UserService userService;
    private final AuditService auditService;
    private final ClientInfoService clientInfoService;
    
    @PostMapping
    @Operation(summary = "Add a comment", description = "Add a comment to a story or reply to another comment")
    public ResponseEntity<CommentResponse> addComment(
            @RequestBody CommentRequest request,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String email = authentication.getName();
        String userId = userService.getUserEntityByEmail(email).getId();
        
        log.info("User {} adding comment to story {}", userId, request.getStoryId());
        CommentResponse response = commentService.createComment(userId, request);
        
        // Audit comment creation (async)
        ClientInfoService.ClientInfo clientInfo = clientInfoService.extractClientInfo();
        auditService.logCommentCreateAsync(userId, request.getStoryId(),
                clientInfo.getUserAgent(), clientInfo.getIpAddress(), clientInfo.getSessionId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/story/{storyId}")
    @Operation(summary = "Get comments for a story", description = "Get paginated comments for a story with nested replies")
    public ResponseEntity<PagedResponse<CommentResponse>> getStoryComments(
            @PathVariable String storyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        PagedResponse<CommentResponse> response = storyService.getStoryComments(storyId, page, size);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{commentId}")
    @Operation(summary = "Delete a comment", description = "Delete a comment by the authenticated user")
    public ResponseEntity<Void> deleteComment(
            @PathVariable String commentId,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String email = authentication.getName();
        String userId = userService.getUserEntityByEmail(email).getId();
        
        log.info("User {} deleting comment {}", userId, commentId);
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.noContent().build();
    }
    
} 