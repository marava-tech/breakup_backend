package com.breakupstories.controller;

import com.breakupstories.dto.FeedbackRequest;
import com.breakupstories.dto.FeedbackResponse;

import com.breakupstories.dto.PagedResponse;

import com.breakupstories.model.Feedback;
import com.breakupstories.service.FeedbackService;
import com.breakupstories.service.UploadService;
import com.breakupstories.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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
                fileUrl = uploadService.uploadSingleFile(file);
            }
            
            FeedbackResponse response = feedbackService.createFeedback(userId, feedbackRequest, fileUrl);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping
    @Operation(summary = "Get all feedbacks with filters", description = "Retrieve paginated list of feedbacks with comprehensive filtering")
    public ResponseEntity<PagedResponse<FeedbackResponse>> getFeedbacks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String storyId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String feedbackId,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        
        PagedResponse<FeedbackResponse> response = feedbackService.getFeedbacksWithFilters(
            page, size, userId, storyId, type, status, feedbackId, sortBy, sortOrder);
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
    
    // Admin endpoints
    @PutMapping("/{feedbackId}/status")
    @Operation(summary = "Update feedback status (Admin)", description = "Update feedback status and add admin response")
    public ResponseEntity<FeedbackResponse> updateFeedbackStatus(
            @PathVariable String feedbackId,
            @RequestParam Feedback.FeedbackStatus status) {
        
        FeedbackResponse response = feedbackService.updateFeedbackStatus(feedbackId,status);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{feedbackId}/admin-response")
    @Operation(summary = "Submit admin response for feedback", description = "Submit admin response and update status to IN_REVIEW if currently PENDING")
    public ResponseEntity<FeedbackResponse> submitAdminResponse(
            @PathVariable String feedbackId,
            @RequestParam String adminResponse) {
        
        FeedbackResponse response = feedbackService.submitAdminResponse(feedbackId, adminResponse);
        return ResponseEntity.ok(response);
    }
    
} 