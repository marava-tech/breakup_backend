package com.breakupstories.repository;

import com.breakupstories.model.Story;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StoryRepository extends MongoRepository<Story, String> {
    
    Page<Story> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    List<Story> findByStatus(Story.StoryStatus status);
    
    // New method with limit and ordering by createdAt (ascending - oldest first)
    @Query(value = "{'status': ?0}", sort = "{'createdAt': 1}")
    List<Story> findByStatusOrderByCreatedAtAscLimit(Story.StoryStatus status, int limit);
    
    Page<Story> findByStatusOrderByViewCountDesc(Story.StoryStatus status, Pageable pageable);
    
    Page<Story> findByStatusOrderByCreatedAtDesc(Story.StoryStatus status, Pageable pageable);
    
    Page<Story> findByLanguageAndStatus(String language, Story.StoryStatus status, Pageable pageable);
    
    Page<Story> findByLanguageAndStatusOrderByCreatedAtDesc(String language, Story.StoryStatus status, Pageable pageable);
    
    Page<Story> findByLanguageAndStatusOrderByViewCountDesc(String language, Story.StoryStatus status, Pageable pageable);
    
    // Custom query for filtering stories with multiple criteria
    @Query("{'status': 'ACTIVE', " +
           "'$and': [" +
           "  { $or: [ { 'metadata.language': ?0 }, { $expr: { $eq: [?0, null] } } ] }, " +
           "  { $or: [ { 'title': { $regex: ?1, $options: 'i' } }, { $expr: { $eq: [?1, null] } } ] }, " +
           "  { $or: [ { 'createdAt': { $gte: ?2 } }, { $expr: { $eq: [?2, null] } } ] }, " +
           "  { $or: [ { 'createdAt': { $lte: ?3 } }, { $expr: { $eq: [?3, null] } } ] } " +
           "]}")
    Page<Story> findStoriesWithFilters(String language, String titleContains, 
                                      LocalDateTime createdAtStart, LocalDateTime createdAtEnd, 
                                      Pageable pageable);
    
    // Query for filtering by date range
    @Query("{'status': 'ACTIVE', 'createdAt': { $gte: ?0, $lte: ?1 }}")
    Page<Story> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    // Count stories by user ID and status
    long countByUserIdAndStatus(String userId, Story.StoryStatus status);
    
    // Find stories by user ID and status
    List<Story> findByUserIdAndStatus(String userId, Story.StoryStatus status);
    
    // Sum view count by user ID
    @Query("{'userId': ?0}")
    Long sumViewCountByUserId(String userId);
    
    // Find stories by user ID with null audioUrl ordered by createdAt desc
    List<Story> findByUserIdAndAudioUrlIsNullOrderByCreatedAtDesc(String userId);
    
    // Method specifically for UPLOAD_PENDING stories with limit (ascending - oldest first)
    @Query(value = "{'status': 'UPLOAD_PENDING'}", sort = "{'createdAt': 1}")
    List<Story> findUploadPendingStoriesOrderByCreatedAtAscLimit(int limit);
    
    // Find stories by tags (any tag in the list)
    List<Story> findByTagsIn(List<String> tags);
    
    // Find stories by tags (all tags must be present)
    @Query("{'tags': {$all: ?0}}")
    List<Story> findByTagsContainingAll(List<String> tags);
    
    // Find stories by story IDs and status
    List<Story> findByIdInAndStatus(List<String> storyIds, Story.StoryStatus status);
    
    // Admin filtering methods
    Page<Story> findByStatus(Story.StoryStatus status, Pageable pageable);
    
    Page<Story> findByLanguage(String language, Pageable pageable);
    
    @Query("{'title': { $regex: ?0, $options: 'i' }}")
    Page<Story> findByTitleContaining(String title, Pageable pageable);
    
    Page<Story> findByUserId(String userId, Pageable pageable);
    
    Page<Story> findByIdIn(List<String> storyIds, Pageable pageable);
    
    // Find story by single ID with pagination
    @Query("{'_id': ?0}")
    Page<Story> findByIdWithPagination(String storyId, Pageable pageable);
    
    // Find stories by status and language
    Page<Story> findByStatusAndLanguage(Story.StoryStatus status, String language, Pageable pageable);
    
    // Find stories by status and title containing
    @Query("{'status': ?0, 'title': { $regex: ?1, $options: 'i' }}")
    Page<Story> findByStatusAndTitleContaining(Story.StoryStatus status, String title, Pageable pageable);
    
    // Find stories by status and user ID
    Page<Story> findByStatusAndUserId(Story.StoryStatus status, String userId, Pageable pageable);
    
    // Find stories by language and title containing
    @Query("{'language': ?0, 'title': { $regex: ?1, $options: 'i' }}")
    Page<Story> findByLanguageAndTitleContaining(String language, String title, Pageable pageable);
    
    // Find stories by language and user ID
    Page<Story> findByLanguageAndUserId(String language, String userId, Pageable pageable);
    
    // Find stories by title containing and user ID
    @Query("{'title': { $regex: ?0, $options: 'i' }, 'userId': ?1}")
    Page<Story> findByTitleContainingAndUserId(String title, String userId, Pageable pageable);
    
    // Find stories by status, language and title containing
    @Query("{'status': ?0, 'language': ?1, 'title': { $regex: ?2, $options: 'i' }}")
    Page<Story> findByStatusAndLanguageAndTitleContaining(Story.StoryStatus status, String language, String title, Pageable pageable);
    
    // Find stories by status, language and user ID
    Page<Story> findByStatusAndLanguageAndUserId(Story.StoryStatus status, String language, String userId, Pageable pageable);
    
    // Find stories by status, title containing and user ID
    @Query("{'status': ?0, 'title': { $regex: ?1, $options: 'i' }, 'userId': ?2}")
    Page<Story> findByStatusAndTitleContainingAndUserId(Story.StoryStatus status, String title, String userId, Pageable pageable);
    
    // Find stories by language, title containing and user ID
    @Query("{'language': ?0, 'title': { $regex: ?1, $options: 'i' }, 'userId': ?2}")
    Page<Story> findByLanguageAndTitleContainingAndUserId(String language, String title, String userId, Pageable pageable);
    
    // Find stories by all filters except storyIds
    @Query("{'status': ?0, 'language': ?1, 'title': { $regex: ?2, $options: 'i' }, 'userId': ?3}")
    Page<Story> findByStatusAndLanguageAndTitleContainingAndUserId(Story.StoryStatus status, String language, String title, String userId, Pageable pageable);
    
    // Count methods for admin statistics
    long countByStatus(Story.StoryStatus status);
    
    long countByCreatedAtAfter(LocalDateTime date);
    
    // Date range methods for dashboard statistics
    long countByCreatedAtBetween(LocalDateTime fromDate, LocalDateTime toDate);
    
    long countByStatusAndCreatedAtBetween(Story.StoryStatus status, LocalDateTime fromDate, LocalDateTime toDate);
    
    List<Story> findByCreatedAtBetween(LocalDateTime fromDate, LocalDateTime toDate);
    
    // Count stories by user ID
    long countByUserId(String userId);
    
    // Count all user stories (excluding FAILED and REJECTED)
    @Query(value = "{'userId': ?0, 'status': {$nin: ['FAILED', 'REJECTED']}}", count = true)
    long countByUserIdAndStatusNotIn(String userId);
    
    // Check if user has an active story with UPLOADED creation type
    boolean existsByUserIdAndStatusAndCreationType(String userId, Story.StoryStatus status, Story.CreationType creationType);
    
    // Find stories by creation type and status ordered by creation date (for voice stories)
    Page<Story> findByCreationTypeAndStatusOrderByCreatedAtDesc(Story.CreationType creationType, Story.StoryStatus status, Pageable pageable);
    
    // Find stories by creation type, status and language ordered by creation date (for voice stories with language filter)
    Page<Story> findByCreationTypeAndStatusAndLanguageOrderByCreatedAtDesc(Story.CreationType creationType, Story.StoryStatus status, String language, Pageable pageable);
} 