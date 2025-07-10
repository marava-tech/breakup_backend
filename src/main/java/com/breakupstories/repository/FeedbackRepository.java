package com.breakupstories.repository;

import com.breakupstories.model.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface FeedbackRepository extends MongoRepository<Feedback, String> {
    
    Page<Feedback> findByStoryId(String storyId, Pageable pageable);
    
    Page<Feedback> findByUserId(String userId, Pageable pageable);
    
    Page<Feedback> findByType(Feedback.FeedbackType type, Pageable pageable);
    
    Page<Feedback> findByStatus(Feedback.FeedbackStatus status, Pageable pageable);
    
    Page<Feedback> findByTypeAndStatus(Feedback.FeedbackType type, Feedback.FeedbackStatus status, Pageable pageable);
    
    long countByUserIdAndTypeAndStatus(String userId, Feedback.FeedbackType type, Feedback.FeedbackStatus status);
    
    // Count methods for admin statistics
    long countByStatus(Feedback.FeedbackStatus status);
    
    long countByCreatedAtAfter(LocalDateTime date);
} 