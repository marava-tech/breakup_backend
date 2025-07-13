package com.breakupstories.repository;

import com.breakupstories.model.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
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
    
    // Combined filtering methods for admin
    Page<Feedback> findByUserIdAndType(String userId, Feedback.FeedbackType type, Pageable pageable);
    
    Page<Feedback> findByUserIdAndStatus(String userId, Feedback.FeedbackStatus status, Pageable pageable);
    
    Page<Feedback> findByStoryIdAndType(String storyId, Feedback.FeedbackType type, Pageable pageable);
    
    Page<Feedback> findByStoryIdAndStatus(String storyId, Feedback.FeedbackStatus status, Pageable pageable);
    
    // Three-way combinations
    Page<Feedback> findByUserIdAndTypeAndStatus(String userId, Feedback.FeedbackType type, Feedback.FeedbackStatus status, Pageable pageable);
    
    Page<Feedback> findByStoryIdAndTypeAndStatus(String storyId, Feedback.FeedbackType type, Feedback.FeedbackStatus status, Pageable pageable);
    
    Page<Feedback> findByUserIdAndStoryIdAndType(String userId, String storyId, Feedback.FeedbackType type, Pageable pageable);
    
    Page<Feedback> findByUserIdAndStoryIdAndStatus(String userId, String storyId, Feedback.FeedbackStatus status, Pageable pageable);
    
    // Four-way combinations
    Page<Feedback> findByUserIdAndStoryIdAndTypeAndStatus(String userId, String storyId, Feedback.FeedbackType type, Feedback.FeedbackStatus status, Pageable pageable);
    
    // Additional methods for admin filtering
    Page<Feedback> findByUserIdAndStoryId(String userId, String storyId, Pageable pageable);
    
    // Find feedback by single ID with pagination
    @Query("{'_id': ?0}")
    Page<Feedback> findByIdWithPagination(String feedbackId, Pageable pageable);
    
    // Count methods for admin statistics
    long countByStatus(Feedback.FeedbackStatus status);
    
    long countByCreatedAtAfter(LocalDateTime date);
} 