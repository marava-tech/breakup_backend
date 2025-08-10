package com.breakupstories.repository;


import com.breakupstories.model.StoryDataStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for story data store and search index operations
 */
@Repository
public interface StoryDataStoreRepository extends MongoRepository<StoryDataStore, String> {
    
    /**
     * Find by story ID
     */
    Optional<StoryDataStore> findByStoryId(String storyId);
    
    /**
     * Find by processing status
     */
    List<StoryDataStore> findByProcessingStatus(StoryDataStore.ProcessingStatus processingStatus);
    
    /**
     * Find by user ID
     */
    List<StoryDataStore> findByUserId(String userId);
    
    /**
     * Find by language
     */
    List<StoryDataStore> findByLanguage(String language);
    
    /**
     * Find by emotion type
     */
    @Query("{'emotions.type': ?0}")
    List<StoryDataStore> findByEmotionType(String emotionType);
    
    /**
     * Find by emotion type with minimum score
     */
    @Query("{'emotions': {$elemMatch: {'type': ?0, 'score': {$gte: ?1}}}}")
    List<StoryDataStore> findByEmotionTypeAndMinScore(String emotionType, double minScore);
    
    /**
     * Find by location (case-insensitive)
     */
    @Query("{'metadata.locations': {$regex: ?0, $options: 'i'}}")
    List<StoryDataStore> findByLocation(String location);
    
    /**
     * Find by state
     */
    @Query("{'metadata.state': ?0}")
    List<StoryDataStore> findByState(String state);
    
    /**
     * Find by district
     */
    @Query("{'metadata.district': ?0}")
    List<StoryDataStore> findByDistrict(String district);
    
    /**
     * Find by pincode
     */
    @Query("{'metadata.pincodes': ?0}")
    List<StoryDataStore> findByPincode(String pincode);
    
    /**
     * Find by name (case-insensitive)
     */
    @Query("{'metadata.names': {$regex: ?0, $options: 'i'}}")
    List<StoryDataStore> findByName(String name);
    
    /**
     * Full-text search in search text
     */
    @Query("{'searchText': {$regex: ?0, $options: 'i'}}")
    Page<StoryDataStore> findBySearchTextContaining(String searchText, Pageable pageable);
    
    /**
     * Search by title (case-insensitive)
     */
    @Query("{'title': {$regex: ?0, $options: 'i'}}")
    Page<StoryDataStore> findByTitleContaining(String title, Pageable pageable);
    
    /**
     * Find stories created after a specific date
     */
    List<StoryDataStore> findByCreatedAtAfter(LocalDateTime date);
    
    /**
     * Find stories created between two dates
     */
    List<StoryDataStore> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find by user ID and processing status
     */
    List<StoryDataStore> findByUserIdAndProcessingStatus(String userId, StoryDataStore.ProcessingStatus processingStatus);
    
    /**
     * Find by language and processing status
     */
    List<StoryDataStore> findByLanguageAndProcessingStatus(String language, StoryDataStore.ProcessingStatus processingStatus);
    
    /**
     * Find stories with high emotion scores
     */
    @Query("{'emotions.score': {$gte: ?0}}")
    List<StoryDataStore> findByEmotionScoreGreaterThan(double minScore);
    
    /**
     * Complex search combining multiple criteria
     */
    @Query("{'$and': [" +
            "{'processingStatus': ?0}, " +
            "{'$or': [" +
            "{'title': {$regex: ?1, $options: 'i'}}, " +
            "{'metadata.locations': {$regex: ?1, $options: 'i'}}" +
            "]}" +
            "]}")
    Page<StoryDataStore> findByComplexSearch(
            StoryDataStore.ProcessingStatus processingStatus, 
            String searchTerm, 
            Pageable pageable
    );
    
    /**
     * Delete by story ID
     */
    void deleteByStoryId(String storyId);
    
    /**
     * Count by processing status
     */
    long countByProcessingStatus(StoryDataStore.ProcessingStatus processingStatus);
    
    /**
     * Count by user ID
     */
    long countByUserId(String userId);
    
    /**
     * Find by processing status ordered by creation time
     */
    List<StoryDataStore> findByProcessingStatusOrderByCreatedAtAsc(StoryDataStore.ProcessingStatus processingStatus);
    
    /**
     * Find by processing status with limit and ordering by createdAt ascending
     */
    @Query(value = "{'processingStatus': ?0}", sort = "{'createdAt': 1}")
    List<StoryDataStore> findByProcessingStatusOrderByCreatedAtAscLimit(StoryDataStore.ProcessingStatus processingStatus, int limit);
    
    /**
     * Find stories with location coordinates in upload metadata
     * This query finds StoryDataStore entries where uploadMetadata contains both "lat" and "long" keys
     */
    @Query("{'uploadMetadata.lat': {$exists: true, $ne: null, $ne: ''}, 'uploadMetadata.long': {$exists: true, $ne: null, $ne: ''}}")
    List<StoryDataStore> findStoriesWithLocationCoordinates();
    
    /**
     * Find stories with location coordinates in upload metadata and specific processing status
     */
    @Query("{'uploadMetadata.lat': {$exists: true, $ne: null, $ne: ''}, 'uploadMetadata.long': {$exists: true, $ne: null, $ne: ''}, 'processingStatus': ?0}")
    List<StoryDataStore> findStoriesWithLocationCoordinatesAndStatus(StoryDataStore.ProcessingStatus processingStatus);
    
    /**
     * Find stories with isConversionPending = true, ordered by creation time (oldest first)
     */
    @Query(value = "{'isConversionPending': true}", sort = "{'createdAt': 1}")
    List<StoryDataStore> findByIsConversionPendingTrueOrderByCreatedAtAsc();
    
    /**
     * Find stories with isConversionPending = true with limit, ordered by creation time (oldest first)
     */
    @Query(value = "{'isConversionPending': true}", sort = "{'createdAt': 1}")
    List<StoryDataStore> findByIsConversionPendingTrueOrderByCreatedAtAscLimit(int limit);
} 