package com.breakupstories.controller;

import com.breakupstories.dto.FeedbackRequest;
import com.breakupstories.dto.FeedbackResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.service.FeedbackService;
import com.breakupstories.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedbacks")
@RequiredArgsConstructor
@Tag(name = "Feedbacks", description = "Feedback management APIs")
public class FeedbackController {
    
    private final FeedbackService feedbackService;
    private final UserService userService;
    
    @PostMapping
    @Operation(summary = "Create a new feedback", description = "Create a new feedback for a story")
    public ResponseEntity<FeedbackResponse> createFeedback(
            Authentication authentication,
            @Valid @RequestBody FeedbackRequest request) {
        
        String email = authentication.getName();
        String userId = userService.getUserEntityByEmail(email).getId();
        
        FeedbackResponse response = feedbackService.createFeedback(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    @Operation(summary = "Get all feedbacks", description = "Retrieve paginated list of all feedbacks")
    public ResponseEntity<PagedResponse<FeedbackResponse>> getFeedbacks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        PagedResponse<FeedbackResponse> response = feedbackService.getFeedbacks(page, size);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/story/{storyId}")
    @Operation(summary = "Get feedbacks by story", description = "Retrieve paginated feedbacks for a specific story")
    public ResponseEntity<PagedResponse<FeedbackResponse>> getFeedbacksByStory(
            @PathVariable String storyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        PagedResponse<FeedbackResponse> response = feedbackService.getFeedbacksByStory(storyId, page, size);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get feedbacks by user", description = "Retrieve paginated feedbacks by a specific user")
    public ResponseEntity<PagedResponse<FeedbackResponse>> getFeedbacksByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        PagedResponse<FeedbackResponse> response = feedbackService.getFeedbacksByUser(userId, page, size);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{feedbackId}")
    @Operation(summary = "Get feedback by ID", description = "Retrieve a specific feedback by its ID")
    public ResponseEntity<FeedbackResponse> getFeedbackById(@PathVariable String feedbackId) {
        FeedbackResponse response = feedbackService.getFeedbackById(feedbackId);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{feedbackId}")
    @Operation(summary = "Update feedback", description = "Update an existing feedback")
    public ResponseEntity<FeedbackResponse> updateFeedback(
            @PathVariable String feedbackId,
            Authentication authentication,
            @Valid @RequestBody FeedbackRequest request) {
        
        String email = authentication.getName();
        String userId = userService.getUserEntityByEmail(email).getId();
        
        FeedbackResponse response = feedbackService.updateFeedback(feedbackId, userId, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{feedbackId}")
    @Operation(summary = "Delete feedback", description = "Delete a feedback by its ID")
    public ResponseEntity<Void> deleteFeedback(
            @PathVariable String feedbackId,
            Authentication authentication) {
        
        String email = authentication.getName();
        String userId = userService.getUserEntityByEmail(email).getId();
        
        feedbackService.deleteFeedback(feedbackId, userId);
        return ResponseEntity.noContent().build();
    }
} 