package com.breakupstories.service;

import com.breakupstories.dto.FeedbackRequest;
import com.breakupstories.dto.FeedbackResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.model.Feedback;
import com.breakupstories.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedbackService {
    
    private final FeedbackRepository feedbackRepository;
    
    public FeedbackResponse createFeedback(String userId, FeedbackRequest request) {
        Feedback feedback = Feedback.builder()
                .storyId(request.getStoryId())
                .userId(userId)
                .tone(request.getTone())
                .contents(request.getContents())
                .build();
        
        Feedback savedFeedback = feedbackRepository.save(feedback);
        return FeedbackResponse.fromFeedback(savedFeedback);
    }
    
    public PagedResponse<FeedbackResponse> getFeedbacks(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Feedback> feedbackPage = feedbackRepository.findAll(pageable);
        
        List<FeedbackResponse> feedbacks = feedbackPage.getContent().stream()
                .map(FeedbackResponse::fromFeedback)
                .collect(Collectors.toList());
        
        return PagedResponse.of(feedbacks, page, size, feedbackPage.getTotalElements());
    }
    
    public PagedResponse<FeedbackResponse> getFeedbacksByStory(String storyId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Feedback> feedbackPage = feedbackRepository.findByStoryId(storyId, pageable);
        
        List<FeedbackResponse> feedbacks = feedbackPage.getContent().stream()
                .map(FeedbackResponse::fromFeedback)
                .collect(Collectors.toList());
        
        return PagedResponse.of(feedbacks, page, size, feedbackPage.getTotalElements());
    }
    
    public PagedResponse<FeedbackResponse> getFeedbacksByUser(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Feedback> feedbackPage = feedbackRepository.findByUserId(userId, pageable);
        
        List<FeedbackResponse> feedbacks = feedbackPage.getContent().stream()
                .map(FeedbackResponse::fromFeedback)
                .collect(Collectors.toList());
        
        return PagedResponse.of(feedbacks, page, size, feedbackPage.getTotalElements());
    }
    
    public FeedbackResponse getFeedbackById(String feedbackId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Feedback not found with ID: " + feedbackId));
        
        return FeedbackResponse.fromFeedback(feedback);
    }
    
    public FeedbackResponse updateFeedback(String feedbackId, String userId, FeedbackRequest request) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Feedback not found with ID: " + feedbackId));
        
        // Check if user owns this feedback
        if (!feedback.getUserId().equals(userId)) {
            throw new RuntimeException("You can only update your own feedback");
        }
        
        feedback.setStoryId(request.getStoryId());
        feedback.setTone(request.getTone());
        feedback.setContents(request.getContents());
        
        Feedback updatedFeedback = feedbackRepository.save(feedback);
        return FeedbackResponse.fromFeedback(updatedFeedback);
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
} 