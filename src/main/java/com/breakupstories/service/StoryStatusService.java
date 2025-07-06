package com.breakupstories.service;

import com.breakupstories.model.Story;
import com.breakupstories.model.StoryDataStore;
import com.breakupstories.repository.StoryDataStoreRepository;
import com.breakupstories.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service to handle status updates in both Story and StoryDataStore collections
 * This ensures both collections stay in sync during the processing workflow
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StoryStatusService {
    
    private final StoryRepository storyRepository;
    private final StoryDataStoreRepository storyDataStoreRepository;
    
    /**
     * Update status in both Story and StoryDataStore collections
     * @param storyId The story ID
     * @param storyStatus The new status for Story
     * @param processingStatus The new status for StoryDataStore
     */
    public void updateStatusInBothCollections(String storyId, Story.StoryStatus storyStatus, StoryDataStore.ProcessingStatus processingStatus) {
        log.info("Updating status for story: {} - Story: {}, DataStore: {}", storyId, storyStatus, processingStatus);
        
        try {
            // Update Story status
            Story story = storyRepository.findById(storyId)
                    .orElseThrow(() -> new RuntimeException("Story not found: " + storyId));
            story.setStatus(storyStatus);
            storyRepository.save(story);
            
            // Update StoryDataStore status
            StoryDataStore dataStore = storyDataStoreRepository.findById(storyId)
                    .orElseThrow(() -> new RuntimeException("StoryDataStore not found: " + storyId));
            dataStore.setProcessingStatus(processingStatus);
            storyDataStoreRepository.save(dataStore);
            
            log.info("Successfully updated status for story: {}", storyId);
            
        } catch (Exception e) {
            log.error("Error updating status for story: {}", storyId, e);
            throw e;
        }
    }
    
    /**
     * Update status in both collections using Story.StoryStatus
     * @param storyId The story ID
     * @param status The new status
     */
    public void updateStatusInBothCollections(String storyId, Story.StoryStatus status) {
        StoryDataStore.ProcessingStatus processingStatus = convertStoryStatusToProcessingStatus(status);
        updateStatusInBothCollections(storyId, status, processingStatus);
    }
    
    /**
     * Convert Story.StoryStatus to StoryDataStore.ProcessingStatus
     */
    private StoryDataStore.ProcessingStatus convertStoryStatusToProcessingStatus(Story.StoryStatus storyStatus) {
        return switch (storyStatus) {
            case UPLOAD_PENDING -> StoryDataStore.ProcessingStatus.UPLOAD_PENDING;
            case UPLOADING -> StoryDataStore.ProcessingStatus.UPLOADING;
            case PROCESSING_PENDING -> StoryDataStore.ProcessingStatus.PROCESSING_PENDING;
            case PROCESSING -> StoryDataStore.ProcessingStatus.PROCESSING;
            case PROCESSED -> StoryDataStore.ProcessingStatus.PROCESSED;
            case CONVERTING -> StoryDataStore.ProcessingStatus.CONVERTING;
            case ACTIVE -> StoryDataStore.ProcessingStatus.COMPLETED;
            case INACTIVE -> StoryDataStore.ProcessingStatus.UPLOAD_PENDING; // Default to initial state
            case FAILED -> StoryDataStore.ProcessingStatus.FAILED;
            case REJECTED -> StoryDataStore.ProcessingStatus.REJECTED;
        };
    }
    
    /**
     * Mark story as failed
     * @param storyId The story ID
     * @param errorMessage The error message
     */
    public void markStoryAsFailed(String storyId, String errorMessage) {
        log.error("Marking story as failed: {} - {}", storyId, errorMessage);
        
        try {
            // Update Story status
            Story story = storyRepository.findById(storyId)
                    .orElseThrow(() -> new RuntimeException("Story not found: " + storyId));
            story.setStatus(Story.StoryStatus.FAILED);
            storyRepository.save(story);
            
            // Update StoryDataStore status
            StoryDataStore dataStore = storyDataStoreRepository.findById(storyId)
                    .orElseThrow(() -> new RuntimeException("StoryDataStore not found: " + storyId));
            dataStore.setProcessingStatus(StoryDataStore.ProcessingStatus.FAILED);
            dataStore.setErrorMessage(errorMessage);
            storyDataStoreRepository.save(dataStore);
            
            log.info("Successfully marked story as failed: {}", storyId);
            
        } catch (Exception e) {
            log.error("Error marking story as failed: {}", storyId, e);
            throw e;
        }
    }
} 