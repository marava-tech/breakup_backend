package com.breakupstories.repository;

import com.breakupstories.model.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackRepository extends MongoRepository<Feedback, String> {
    
    Page<Feedback> findByStoryId(String storyId, Pageable pageable);
    
    Page<Feedback> findByUserId(String userId, Pageable pageable);
} 