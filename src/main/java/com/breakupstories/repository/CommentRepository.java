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
     * Find comments created after the specified time
     * @param fromTime The start time
     * @return List of comments created after the specified time
     */
    List<Comment> findByCreatedAtAfter(LocalDateTime fromTime);
    
    /**
     * Find comments created after the specified time with null category, explanation, and confidence
     * @param fromTime The start time
     * @return List of comments created after the specified time with null abuse detection fields
     */
    @Query("{'createdAt': {$gt: ?0}, 'category': null, 'explanation': null, 'confidence': null}")
    List<Comment> findByCreatedAtAfterAndCategoryIsNullAndExplanationIsNullAndConfidenceIsNull(LocalDateTime fromTime);
    
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
    
    // Abuse detection related methods
    Page<Comment> findByIsAbusiveTrue(Pageable pageable);
    
    Page<Comment> findByIsAbusiveTrueAndActiveTrue(Pageable pageable);
    
    List<Comment> findByIsAbusiveTrueAndActiveTrue();
    
    Page<Comment> findByCategoryAndActiveTrue(String category, Pageable pageable);
    
    List<Comment> findByCategoryAndActiveTrue(String category);
    
    long countByIsAbusiveTrue();
    
    long countByIsAbusiveTrueAndActiveTrue();
    
    long countByCategoryAndActiveTrue(String category);
    
    long countByActiveTrue();
    
    // Count method for admin statistics
    long countByCreatedAtAfter(LocalDateTime date);
    
    // Date range methods for dashboard statistics
    long countByCreatedAtBetween(LocalDateTime fromDate, LocalDateTime toDate);
    
    long countByActiveTrueAndCreatedAtBetween(LocalDateTime fromDate, LocalDateTime toDate);
    
    long countByIsAbusiveTrueAndCreatedAtBetween(LocalDateTime fromDate, LocalDateTime toDate);
    
    List<Comment> findByCreatedAtBetween(LocalDateTime fromDate, LocalDateTime toDate);
    
    // Count comments by user ID
    long countByUserId(String userId);
    
    // Admin filtering methods
    Page<Comment> findByIsAbusive(Boolean isAbusive, Pageable pageable);
    
    Page<Comment> findByCategory(String category, Pageable pageable);
    
    Page<Comment> findByActive(Boolean active, Pageable pageable);
    
    // Combined filtering methods
    Page<Comment> findByStoryIdAndUserId(String storyId, String userId, Pageable pageable);
    
    Page<Comment> findByStoryIdAndIsAbusive(String storyId, Boolean isAbusive, Pageable pageable);
    
    Page<Comment> findByStoryIdAndCategory(String storyId, String category, Pageable pageable);
    
    Page<Comment> findByStoryIdAndActive(String storyId, Boolean active, Pageable pageable);
    
    Page<Comment> findByUserIdAndIsAbusive(String userId, Boolean isAbusive, Pageable pageable);
    
    Page<Comment> findByUserIdAndCategory(String userId, String category, Pageable pageable);
    
    Page<Comment> findByUserIdAndActive(String userId, Boolean active, Pageable pageable);
    
    Page<Comment> findByIsAbusiveAndCategory(Boolean isAbusive, String category, Pageable pageable);
    
    Page<Comment> findByIsAbusiveAndActive(Boolean isAbusive, Boolean active, Pageable pageable);
    
    Page<Comment> findByCategoryAndActive(String category, Boolean active, Pageable pageable);
    
    // Three-way combinations
    Page<Comment> findByStoryIdAndUserIdAndIsAbusive(String storyId, String userId, Boolean isAbusive, Pageable pageable);
    
    Page<Comment> findByStoryIdAndUserIdAndCategory(String storyId, String userId, String category, Pageable pageable);
    
    Page<Comment> findByStoryIdAndUserIdAndActive(String storyId, String userId, Boolean active, Pageable pageable);
    
    Page<Comment> findByStoryIdAndIsAbusiveAndCategory(String storyId, Boolean isAbusive, String category, Pageable pageable);
    
    Page<Comment> findByStoryIdAndIsAbusiveAndActive(String storyId, Boolean isAbusive, Boolean active, Pageable pageable);
    
    Page<Comment> findByStoryIdAndCategoryAndActive(String storyId, String category, Boolean active, Pageable pageable);
    
    Page<Comment> findByUserIdAndIsAbusiveAndCategory(String userId, Boolean isAbusive, String category, Pageable pageable);
    
    Page<Comment> findByUserIdAndIsAbusiveAndActive(String userId, Boolean isAbusive, Boolean active, Pageable pageable);
    
    Page<Comment> findByUserIdAndCategoryAndActive(String userId, String category, Boolean active, Pageable pageable);
    
    Page<Comment> findByIsAbusiveAndCategoryAndActive(Boolean isAbusive, String category, Boolean active, Pageable pageable);
    
    // Four-way combinations
    Page<Comment> findByStoryIdAndUserIdAndIsAbusiveAndCategory(String storyId, String userId, Boolean isAbusive, String category, Pageable pageable);
    
    Page<Comment> findByStoryIdAndUserIdAndIsAbusiveAndActive(String storyId, String userId, Boolean isAbusive, Boolean active, Pageable pageable);
    
    Page<Comment> findByStoryIdAndUserIdAndCategoryAndActive(String storyId, String userId, String category, Boolean active, Pageable pageable);
    
    Page<Comment> findByStoryIdAndIsAbusiveAndCategoryAndActive(String storyId, Boolean isAbusive, String category, Boolean active, Pageable pageable);
    
    Page<Comment> findByUserIdAndIsAbusiveAndCategoryAndActive(String userId, Boolean isAbusive, String category, Boolean active, Pageable pageable);
    
    // All filters
    Page<Comment> findByStoryIdAndUserIdAndIsAbusiveAndCategoryAndActive(String storyId, String userId, Boolean isAbusive, String category, Boolean active, Pageable pageable);
} 