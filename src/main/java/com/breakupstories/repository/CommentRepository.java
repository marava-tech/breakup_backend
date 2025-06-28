package com.breakupstories.repository;

import com.breakupstories.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {
    
    Page<Comment> findByStoryId(String storyId, Pageable pageable);
    
    List<Comment> findByStoryIdAndParentIdIsNull(String storyId);
    
    Page<Comment> findByStoryIdAndParentIdIsNull(String storyId, Pageable pageable);
    
    List<Comment> findByParentId(String parentId);
    
    long countByStoryId(String storyId);
    
    Page<Comment> findByUserId(String userId, Pageable pageable);
    
    /**
     * Find comments created in the last 10 minutes
     * @param fromTime The start time (10 minutes ago)
     * @return List of comments created after the specified time
     */
    List<Comment> findByCreatedAtAfter(LocalDateTime fromTime);
    
    // Active comments only
    Page<Comment> findByStoryIdAndActiveTrue(String storyId, Pageable pageable);
    
    List<Comment> findByStoryIdAndParentIdIsNullAndActiveTrue(String storyId);
    
    Page<Comment> findByStoryIdAndParentIdIsNullAndActiveTrue(String storyId, Pageable pageable);
    
    List<Comment> findByParentIdAndActiveTrue(String parentId);
    
    long countByStoryIdAndActiveTrue(String storyId);
    
    Page<Comment> findByUserIdAndActiveTrue(String userId, Pageable pageable);
    
    // Count comments for all stories by a user
    @Query("{'storyId': { $in: ?0 }}")
    long countByStoryUserId(String userId);
} 