package com.breakupstories.repository;

import com.breakupstories.model.Like;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LikeRepository extends MongoRepository<Like, String> {
    
    List<Like> findByStoryId(String storyId);
    
    Page<Like> findByUserId(String userId, Pageable pageable);
    
    Page<Like> findByStoryId(String storyId, Pageable pageable);
    
    Optional<Like> findByStoryIdAndUserId(String storyId, String userId);
    
    boolean existsByStoryIdAndUserId(String storyId, String userId);
    
    boolean existsByUserIdAndStoryId(String userId, String storyId);
    
    void deleteByUserIdAndStoryId(String userId, String storyId);
    
    long countByStoryId(String storyId);
    
    // Count likes for all stories by a user
    @Query("{'storyId': { $in: ?0 }}")
    long countByStoryUserId(String userId);
    
    // Date range methods for dashboard statistics
    long countByCreatedAtBetween(java.time.LocalDateTime fromDate, java.time.LocalDateTime toDate);
} 