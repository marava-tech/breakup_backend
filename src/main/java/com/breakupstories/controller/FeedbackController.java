package com.breakupstories.controller;

import com.breakupstories.dto.FeedbackRequest;
import com.breakupstories.dto.FeedbackResponse;
import com.breakupstories.dto.FeedbackStatusUpdateRequest;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.dto.UploadResponse;
import com.breakupstories.model.Feedback;
import com.breakupstories.service.FeedbackService;
import com.breakupstories.service.UploadService;
import com.breakupstories.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/feedbacks")
@RequiredArgsConstructor
@Tag(name = "Feedbacks", description = "Feedback management APIs")
public class FeedbackController {
    
    private final FeedbackService feedbackService;
    private final UserService userService;
    private final UploadService uploadService;
    private final ObjectMapper objectMapper;
    
    @PostMapping
    @Operation(summary = "Create a new feedback", description = "Create a new feedback (story-specific or general) with optional file upload")
    public ResponseEntity<FeedbackResponse> createFeedback(
            Authentication authentication,
            MultipartHttpServletRequest request) {
        
        try {
            String email = authentication.getName();
            String userId = userService.getUserEntityByEmail(email).getId();
            
            // Extract feedback data from form data
            String feedbackJson = request.getParameter("feedback");
            FeedbackRequest feedbackRequest = objectMapper.readValue(feedbackJson, FeedbackRequest.class);
            
            // Handle file upload
            String fileUrl = null;
            MultipartFile file = request.getFile("file");
            if (file != null && !file.isEmpty()) {
                UploadResponse uploadResponse = uploadService.uploadFile(file);
                if (uploadResponse.getData() != null && !uploadResponse.getData().isEmpty()) {
                    fileUrl = uploadResponse.getData().get(0);
                }
            }
            
            FeedbackResponse response = feedbackService.createFeedback(userId, feedbackRequest, fileUrl);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping
    @Operation(summary = "Get all feedbacks", description = "Retrieve paginated list of all feedbacks")
    public ResponseEntity<PagedResponse<FeedbackResponse>> getFeedbacks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        PagedResponse<FeedbackResponse> response = feedbackService.getFeedbacks(page, size);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/types/{type}")
    @Operation(summary = "Get feedbacks by type", description = "Retrieve paginated feedbacks by type (BUG_REPORT, FEATURE_REQUEST, etc.)")
    public ResponseEntity<PagedResponse<FeedbackResponse>> getFeedbacksByType(
            @PathVariable Feedback.FeedbackType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        PagedResponse<FeedbackResponse> response = feedbackService.getFeedbacksByType(type, page, size);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/status/{status}")
    @Operation(summary = "Get feedbacks by status", description = "Retrieve paginated feedbacks by status (PENDING, IN_REVIEW, etc.)")
    public ResponseEntity<PagedResponse<FeedbackResponse>> getFeedbacksByStatus(
            @PathVariable Feedback.FeedbackStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        PagedResponse<FeedbackResponse> response = feedbackService.getFeedbacksByStatus(status, page, size);
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
    
    @GetMapping("/my-feedbacks")
    @Operation(summary = "Get my feedbacks", description = "Retrieve paginated feedbacks by the authenticated user")
    public ResponseEntity<PagedResponse<FeedbackResponse>> getMyFeedbacks(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        String email = authentication.getName();
        String userId = userService.getUserEntityByEmail(email).getId();
        
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
    @Operation(summary = "Update feedback", description = "Update an existing feedback with optional file upload")
    public ResponseEntity<FeedbackResponse> updateFeedback(
            @PathVariable String feedbackId,
            Authentication authentication,
            MultipartHttpServletRequest request) {
        
        try {
            String email = authentication.getName();
            String userId = userService.getUserEntityByEmail(email).getId();
            
            // Extract feedback data from form data
            String feedbackJson = request.getParameter("feedback");
            FeedbackRequest feedbackRequest = objectMapper.readValue(feedbackJson, FeedbackRequest.class);
            
            // Handle file upload
            String fileUrl = null;
            MultipartFile file = request.getFile("file");
            if (file != null && !file.isEmpty()) {
                UploadResponse uploadResponse = uploadService.uploadFile(file);
                if (uploadResponse.getData() != null && !uploadResponse.getData().isEmpty()) {
                    fileUrl = uploadResponse.getData().get(0);
                }
            }
            
            FeedbackResponse response = feedbackService.updateFeedback(feedbackId, userId, feedbackRequest, fileUrl);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Admin endpoints
    @PutMapping("/{feedbackId}/status")
    @Operation(summary = "Update feedback status (Admin)", description = "Update feedback status and add admin response")
    public ResponseEntity<FeedbackResponse> updateFeedbackStatus(
            @PathVariable String feedbackId,
            @Valid @RequestBody FeedbackStatusUpdateRequest request) {
        
        FeedbackResponse response = feedbackService.updateFeedbackStatus(feedbackId, request.getStatus(), request.getAdminResponse());
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