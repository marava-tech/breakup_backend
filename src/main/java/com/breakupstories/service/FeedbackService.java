package com.breakupstories.service;

import com.breakupstories.dto.FeedbackRequest;
import com.breakupstories.dto.FeedbackResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.model.Feedback;
import com.breakupstories.model.User;
import com.breakupstories.repository.FeedbackRepository;
import com.breakupstories.repository.UserRepository;
import com.breakupstories.util.ApplicationContextProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedbackService {
    
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    
    public FeedbackResponse createFeedback(String userId, FeedbackRequest request, String fileUrl) {
        // Validate request based on feedback type
        validateFeedbackRequest(request);
        
        Feedback feedback = Feedback.builder()
                .storyId(request.getStoryId())
                .userId(userId)
                .type(request.getType())
                .subject(request.getSubject())
                .description(request.getDescription())
                .fileUrl(fileUrl)
                .status(Feedback.FeedbackStatus.PENDING) // Default status for new feedback
                .build();
        
        Feedback savedFeedback = feedbackRepository.save(feedback);
        return FeedbackResponse.fromFeedback(savedFeedback);
    }
    
    private void validateFeedbackRequest(FeedbackRequest request) {
        if (request.getType() == null) {
            throw new IllegalArgumentException("Feedback type is required");
        }
        
        // For story-specific feedback, validate required fields
        if (request.getType() == Feedback.FeedbackType.STORY_FEEDBACK) {
            if (request.getStoryId() == null || request.getStoryId().trim().isEmpty()) {
                throw new IllegalArgumentException("Story ID is required for story feedback");
            }
        }
        
        // For general feedback, validate required fields
        if (request.getType() != Feedback.FeedbackType.STORY_FEEDBACK) {
            if (request.getSubject() == null || request.getSubject().trim().isEmpty()) {
                throw new IllegalArgumentException("Subject is required for general feedback");
            }
            if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
                throw new IllegalArgumentException("Description is required for general feedback");
            }
        }
    }
    
    public PagedResponse<FeedbackResponse> getFeedbacks(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Feedback> feedbackPage = feedbackRepository.findAll(pageable);
        
        List<FeedbackResponse> feedbacks = feedbackPage.getContent().stream()
                .map(this::enrichFeedbackResponse)
                .collect(Collectors.toList());
        
        return PagedResponse.of(feedbacks, page, size, feedbackPage.getTotalElements());
    }
    
    public PagedResponse<FeedbackResponse> getFeedbacksByStory(String storyId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Feedback> feedbackPage = feedbackRepository.findByStoryId(storyId, pageable);
        
        List<FeedbackResponse> feedbacks = feedbackPage.getContent().stream()
                .map(this::enrichFeedbackResponse)
                .collect(Collectors.toList());
        
        return PagedResponse.of(feedbacks, page, size, feedbackPage.getTotalElements());
    }
    
    public PagedResponse<FeedbackResponse> getFeedbacksByUser(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Feedback> feedbackPage = feedbackRepository.findByUserId(userId, pageable);
        
        List<FeedbackResponse> feedbacks = feedbackPage.getContent().stream()
                .map(this::enrichFeedbackResponse)
                .collect(Collectors.toList());
        
        return PagedResponse.of(feedbacks, page, size, feedbackPage.getTotalElements());
    }
    
    public PagedResponse<FeedbackResponse> getFeedbacksByType(Feedback.FeedbackType type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Feedback> feedbackPage = feedbackRepository.findByType(type, pageable);
        
        List<FeedbackResponse> feedbacks = feedbackPage.getContent().stream()
                .map(this::enrichFeedbackResponse)
                .collect(Collectors.toList());
        
        return PagedResponse.of(feedbacks, page, size, feedbackPage.getTotalElements());
    }
    
    public PagedResponse<FeedbackResponse> getFeedbacksByStatus(Feedback.FeedbackStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Feedback> feedbackPage = feedbackRepository.findByStatus(status, pageable);
        
        List<FeedbackResponse> feedbacks = feedbackPage.getContent().stream()
                .map(this::enrichFeedbackResponse)
                .collect(Collectors.toList());
        
        return PagedResponse.of(feedbacks, page, size, feedbackPage.getTotalElements());
    }
    
    public FeedbackResponse getFeedbackById(String feedbackId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Feedback not found with ID: " + feedbackId));
        
        return enrichFeedbackResponse(feedback);
    }
    
    public FeedbackResponse updateFeedback(String feedbackId, String userId, FeedbackRequest request, String fileUrl) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Feedback not found with ID: " + feedbackId));
        
        // Check if user owns this feedback
        if (!feedback.getUserId().equals(userId)) {
            throw new RuntimeException("You can only update your own feedback");
        }
        
        // Validate request
        validateFeedbackRequest(request);
        
        feedback.setStoryId(request.getStoryId());
        feedback.setType(request.getType());
        feedback.setSubject(request.getSubject());
        feedback.setDescription(request.getDescription());
        feedback.setFileUrl(fileUrl);
        
        Feedback updatedFeedback = feedbackRepository.save(feedback);
        return enrichFeedbackResponse(updatedFeedback);
    }
    
    // Admin method to update feedback status and add response
    public FeedbackResponse updateFeedbackStatus(String feedbackId, Feedback.FeedbackStatus status, String adminResponse) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Feedback not found with ID: " + feedbackId));
        
        feedback.setStatus(status);
        feedback.setAdminResponse(adminResponse);
        
        Feedback updatedFeedback = feedbackRepository.save(feedback);
        
        // Check for feedback pro reward if status is changed to RESOLVED
        if (status == Feedback.FeedbackStatus.RESOLVED && 
            (feedback.getType() == Feedback.FeedbackType.BUG_REPORT || 
             feedback.getType() == Feedback.FeedbackType.FEATURE_REQUEST)) {
            
            // Get RewardService from ApplicationContext to avoid circular dependency
            com.breakupstories.service.RewardService rewardService = 
                ApplicationContextProvider.getBean(com.breakupstories.service.RewardService.class);
            rewardService.checkFeedbackProReward(feedback.getUserId());
        }
        
        return enrichFeedbackResponse(updatedFeedback);
    }
    
    public void deleteFeedback(String feedbackId, String userId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Feedback not found with ID: " + feedbackId));
        
        // Check if user owns this feedback
        if (!feedback.getUserId().equals(userId)) {
            throw new RuntimeException("You can only delete your own feedback");
        }
        
        feedbackRepository.deleteById(feedbackId);
    }
    
    // Helper method to enrich feedback response with user information
    public FeedbackResponse enrichFeedbackResponse(Feedback feedback) {
        FeedbackResponse response = FeedbackResponse.fromFeedback(feedback);
        
        // Add username if available
        try {
            userRepository.findById(feedback.getUserId()).ifPresent(user -> response.setUsername(user.getName()));
        } catch (Exception e) {
            // Log error but don't fail the request
            System.err.println("Error fetching user for feedback: " + e.getMessage());
        }
        
        return response;
    }
} 