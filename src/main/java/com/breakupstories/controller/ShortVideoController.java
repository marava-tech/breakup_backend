package com.breakupstories.controller;

import com.breakupstories.dto.PagedResponse;
import com.breakupstories.dto.ShortVideoCommentRequest;
import com.breakupstories.dto.ShortVideoCommentResponse;
import com.breakupstories.dto.ShortVideoResponse;
import com.breakupstories.model.User;
import com.breakupstories.service.ShortVideoInteractionService;
import com.breakupstories.service.ShortVideoRecommendationService;
import com.breakupstories.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.breakupstories.util.LanguageUtils;

@RestController
@RequestMapping("/api/v1/short-videos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Short Videos", description = "User endpoints for short video reels feed and interactions")
public class ShortVideoController {

    private final ShortVideoRecommendationService recommendationService;
    private final ShortVideoInteractionService interactionService;
    private final UserService userService;

    @GetMapping("/feed")
    @Operation(summary = "Get personalized video feed", description = "Returns unpredictable short videos the user hasn't seen")
    public ResponseEntity<PagedResponse<ShortVideoResponse>> getFeed(
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String language,
            Authentication authentication) {

        String userId = getUserIdFromAuth(authentication);
        String normalizedLanguage = LanguageUtils.normalizeLanguage(language);
        return ResponseEntity.ok(recommendationService.getFeed(userId, normalizedLanguage, Math.min(size, 100)));
    }

    @PostMapping("/{videoId}/view")
    @Operation(summary = "Record a video view", description = "Marks video as seen by user so it won't be recommended again")
    public ResponseEntity<Void> recordView(@PathVariable String videoId, Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        interactionService.recordView(userId, videoId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{videoId}/like")
    @Operation(summary = "Like a video")
    public ResponseEntity<Void> likeVideo(@PathVariable String videoId, Authentication authentication) {
        String userId = requireUserId(authentication);
        interactionService.likeVideo(userId, videoId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{videoId}/like")
    @Operation(summary = "Unlike a video")
    public ResponseEntity<Void> unlikeVideo(@PathVariable String videoId, Authentication authentication) {
        String userId = requireUserId(authentication);
        interactionService.unlikeVideo(userId, videoId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{videoId}/share")
    @Operation(summary = "Record a video share")
    public ResponseEntity<Void> recordShare(@PathVariable String videoId, Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        interactionService.recordShare(userId, videoId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{videoId}/comments")
    @Operation(summary = "Add a comment to a video")
    public ResponseEntity<ShortVideoCommentResponse> addComment(
            @PathVariable String videoId,
            @Valid @RequestBody ShortVideoCommentRequest request,
            Authentication authentication) {
        String userId = requireUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interactionService.addComment(userId, videoId, request));
    }

    @GetMapping("/{videoId}/comments")
    @Operation(summary = "Get comments for a video")
    public ResponseEntity<PagedResponse<ShortVideoCommentResponse>> getComments(
            @PathVariable String videoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(interactionService.getComments(videoId, page, size));
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "Delete user's own comment")
    public ResponseEntity<Void> deleteComment(@PathVariable String commentId, Authentication authentication) {
        String userId = requireUserId(authentication);
        interactionService.deleteComment(userId, commentId);
        return ResponseEntity.noContent().build();
    }

    // --- Helper methods ---
    private String getUserIdFromAuth(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            User user = userService.getUserEntityByEmail(authentication.getName());
            return user != null ? user.getId() : null;
        }
        return null;
    }

    private String requireUserId(Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        if (userId == null) {
            log.warn("Unauthorized access attempt — no userId resolved from auth token");
            throw new RuntimeException("Unauthorized: User must be logged in");
        }
        return userId;
    }
}
