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
    
    Page<Story> findByUserId(String userId, Pageable pageable);
    
    Page<Story> findByStatus(Story.StoryStatus status, Pageable pageable);
    
    Page<Story> findByStatusOrderByViewCountDesc(Story.StoryStatus status, Pageable pageable);
    
    Page<Story> findByStatusOrderByCreatedAtDesc(Story.StoryStatus status, Pageable pageable);
    
    Page<Story> findByMetadataLanguageAndStatus(String language, Story.StoryStatus status, Pageable pageable);
    
    List<Story> findByTagsContaining(String tag);
    
    Page<Story> findByTagsContaining(String tag, Pageable pageable);
    
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
} 