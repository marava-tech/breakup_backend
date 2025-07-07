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
    
    Page<Story> findByStatus(Story.StoryStatus status, Pageable pageable);
    
    List<Story> findByStatus(Story.StoryStatus status);
    
    // New method with limit and ordering by createdAt (ascending - oldest first)
    @Query(value = "{'status': ?0}", sort = "{'createdAt': 1}")
    List<Story> findByStatusOrderByCreatedAtAscLimit(Story.StoryStatus status, int limit);
    
    Page<Story> findByStatusOrderByViewCountDesc(Story.StoryStatus status, Pageable pageable);
    
    Page<Story> findByStatusOrderByCreatedAtDesc(Story.StoryStatus status, Pageable pageable);
    
    Page<Story> findByLanguageAndStatus(String language, Story.StoryStatus status, Pageable pageable);
    

    
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
    
    // Query for filtering by language only
    @Query("{'status': 'ACTIVE', 'metadata.language': ?0}")
    Page<Story> findByLanguage(String language, Pageable pageable);
    
    // Query for filtering by title contains
    @Query("{'status': 'ACTIVE', 'title': { $regex: ?0, $options: 'i' }}")
    Page<Story> findByTitleContaining(String titleContains, Pageable pageable);
    
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
} 