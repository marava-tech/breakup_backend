package com.breakupstories.service;

import com.breakupstories.model.Story;
import com.breakupstories.model.StoryDataStore;
import com.breakupstories.repository.StoryDataStoreRepository;
import com.breakupstories.repository.StoryRepository;
import com.breakupstories.util.RequestIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.breakupstories.util.TimestampUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Background worker that converts processed stories to final Story entities
 * 
 * Workflow:
 * 1. Fetches stories with PROCESSED status
 * 2. Sets status to CONVERTING
 * 3. Converts StoryDataStore to final Story entity
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StoryConversionWorker {
    
    private final StoryDataStoreRepository storyDataStoreRepository;
    private final StoryRepository storyRepository;
    private final StoryStatusService storyStatusService;
    private final DefaultConfigService defaultConfigService;
    private final FirstStoryRewardService firstStoryRewardService;
    
    /**
     * Process conversions every 5 minutes
     */
    @Scheduled(fixedRate = 60000) // 1 minutes
    public void convertStories() {
        String requestId = RequestIdGenerator.generateRequestId();
        log.info("Starting story conversion worker (Request ID: {})", requestId);
        
        try {
            // Fetch stories with PROCESSED status, ordered by creation time (oldest first)
            List<StoryDataStore> processedStories = storyDataStoreRepository.findByProcessingStatusOrderByCreatedAtAscLimit(StoryDataStore.ProcessingStatus.PROCESSED, 10);
            
            // Fetch stories with FAILED status, ordered by creation time (oldest first)
            List<StoryDataStore> failedStories = storyDataStoreRepository.findByProcessingStatusOrderByCreatedAtAscLimit(StoryDataStore.ProcessingStatus.FAILED, 10);
            
            if (processedStories.isEmpty() && failedStories.isEmpty()) {
                log.info("No stories pending conversion (Request ID: {})", requestId);
                return;
            }
            
            log.info("Found {} processed stories and {} failed stories pending conversion (Request ID: {})", 
                    processedStories.size(), failedStories.size(), requestId);
            
            // Process each processed story
            for (StoryDataStore dataStore : processedStories) {
                try {
                    convertStory(dataStore, requestId);
                } catch (Exception e) {
                    log.error("Error converting story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage(), e);
                    markStoryAsFailed(dataStore, "Conversion failed: " + e.getMessage());
                }
            }
            
            // Process each failed story
            for (StoryDataStore dataStore : failedStories) {
                try {
                    convertFailedStory(dataStore, requestId);
                } catch (Exception e) {
                    log.error("Error converting failed story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage(), e);
                    markStoryAsFailed(dataStore, "Conversion failed: " + e.getMessage());
                }
            }
            
            log.info("Story conversion worker completed (Request ID: {})", requestId);
            
        } catch (Exception e) {
            log.error("Error in story conversion worker (Request ID: {}): {}", requestId, e.getMessage(), e);
        }
    }
    
    /**
     * Convert a single processed story to final Story entity
     */
    private void convertStory(StoryDataStore dataStore, String requestId) {
        log.info("Converting story: {} (Request ID: {})", dataStore.getId(), requestId);
        
        try {
            // Update status to CONVERTING using StoryStatusService
            storyStatusService.updateStatusInBothCollections(dataStore.getStoryId(), Story.StoryStatus.CONVERTING);
            
            // Check if story is valid based on story analysis
            if (dataStore.getStoryAnalysisResponse() != null && 
                dataStore.getStoryAnalysisResponse().getAnalysis() != null &&
                dataStore.getStoryAnalysisResponse().getAnalysis().getIs_valid_story() != null &&
                !dataStore.getStoryAnalysisResponse().getAnalysis().getIs_valid_story()) {
                
                log.warn("Story {} marked as invalid by AI analysis - not a love/breakup related story (Request ID: {})", 
                        dataStore.getId(), requestId);
                
                // Create rejected story with proper rejection reason
                createRejectedStoryFromDataStore(dataStore, requestId, 
                    "Story rejected: Not a love or breakup related story. The content does not meet our platform's criteria for relationship-focused narratives.");
                
                // Update status to REJECTED using StoryStatusService
                storyStatusService.updateStatusInBothCollections(dataStore.getStoryId(), Story.StoryStatus.REJECTED);
                
            } else {
                // Story is valid, proceed with normal conversion
                createStoryFromDataStore(dataStore, requestId);
                
                // Update status to COMPLETED using StoryStatusService
                storyStatusService.updateStatusInBothCollections(dataStore.getStoryId(), Story.StoryStatus.ACTIVE);
            }
            
            log.info("Successfully converted story: {} to final Story entity (Request ID: {})", dataStore.getId(), requestId);
            
        } catch (Exception e) {
            log.error("Error converting story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage(), e);
            markStoryAsFailed(dataStore, "Conversion failed: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Create Story entity from StoryDataStore
     */
    private void createStoryFromDataStore(StoryDataStore dataStore, String requestId) {
        log.info("Creating Story from StoryDataStore for story: {} (Request ID: {})", dataStore.getId(), requestId);
        
        try {
            // Build rejection reasons from all error fields
            StringBuilder rejectionReasons = new StringBuilder();
            
            // Add general errors
            if (dataStore.getErrors() != null && !dataStore.getErrors().isEmpty()) {
                rejectionReasons.append("General errors: ").append(String.join(", ", dataStore.getErrors())).append("; ");
            }
            
            // Add step-specific errors
            if (dataStore.getTranscriptionError() != null) {
                rejectionReasons.append("Transcription error: ").append(dataStore.getTranscriptionError()).append("; ");
            }
            if (dataStore.getRewriteError() != null) {
                rejectionReasons.append("Rewrite error: ").append(dataStore.getRewriteError()).append("; ");
            }
            if (dataStore.getParagraphError() != null) {
                rejectionReasons.append("Paragraph error: ").append(dataStore.getParagraphError()).append("; ");
            }
            if (dataStore.getAnalysisError() != null) {
                rejectionReasons.append("Analysis error: ").append(dataStore.getAnalysisError()).append("; ");
            }
            
            // Add step errors from map
            if (dataStore.getStepErrors() != null && !dataStore.getStepErrors().isEmpty()) {
                dataStore.getStepErrors().forEach((step, error) -> 
                    rejectionReasons.append(step).append(" error: ").append(error).append("; "));
            }

            // Determine creation type from metadata
            Story.CreationType creationType = Story.CreationType.UPLOADED; // Default
            if (dataStore.getUploadMetadata() != null && dataStore.getUploadMetadata().get("creationType") != null) {
                String creationTypeStr = dataStore.getUploadMetadata().get("creationType");
                try {
                    creationType = Story.CreationType.valueOf(creationTypeStr);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid creation type in metadata: {}, using default UPLOADED", creationTypeStr);
                }
            }
            
            // Create Story entity
            Story story = Story.builder()
                    .id(dataStore.getStoryId())
                    .userId(dataStore.getUserId())
                    .title(dataStore.getTitle())
                    .contents(extractContentsFromDataStore(dataStore))
                    .tags(extractTagsFromDataStore(dataStore))
                    .emotions(extractEmotionsFromDataStore(dataStore))
                    .language(dataStore.getLanguage())
                    .audioUrl(dataStore.getAudioUrl())
                    .thumbnailUrl(defaultConfigService.getDefaultThumbnailUrl())
                    .storyImages(defaultConfigService.getDefaultStoryImages())
                    .duration(dataStore.getDuration())
                    .rejectionReasons(!rejectionReasons.isEmpty() ? List.of(rejectionReasons.toString()) : null)
                    .status(Story.StoryStatus.ACTIVE) // All converted stories are active
                    .creationType(creationType)
                    .createdAt(dataStore.getCreatedAt())
                    .updatedAt(TimestampUtil.currentLocalDateTime())
                    .build();
            
              storyRepository.save(story);
            
            // Check for first story reward
            boolean rewardGiven = firstStoryRewardService.checkAndRewardFirstStory(dataStore.getUserId(), dataStore.getStoryId());
            if (rewardGiven) {
                log.info("First story reward check completed for story: {} (Request ID: {})", dataStore.getId(), requestId);
            }
            
            log.info("Successfully created Story from StoryDataStore for story: {} (Request ID: {})", dataStore.getId(), requestId);

        } catch (Exception e) {
            log.error("Error creating Story from StoryDataStore for story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to create Story from StoryDataStore: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extract contents from data store
     */
    private List<com.breakupstories.model.Content> extractContentsFromDataStore(StoryDataStore dataStore) {
        List<com.breakupstories.model.Content> contents = new ArrayList<>();
        
        // First, try to extract from paragraphRewriteResponse (preferred)
        if (dataStore.getParagraphRewriteResponse() != null && 
            dataStore.getParagraphRewriteResponse().getContents() != null) {
            
            log.info("Extracting {} paragraphs from paragraphRewriteResponse for story: {}", 
                    dataStore.getParagraphRewriteResponse().getContents().size(), dataStore.getId());
            
            for (int i = 0; i < dataStore.getParagraphRewriteResponse().getContents().size(); i++) {
                var paragraphContent = dataStore.getParagraphRewriteResponse().getContents().get(i);
                
                com.breakupstories.model.Content content = com.breakupstories.model.Content.builder()
                        .type(com.breakupstories.model.Content.ContentType.valueOf(paragraphContent.getType().toUpperCase()))
                        .data(paragraphContent.getData())
                        .orderIndex(paragraphContent.getOrderIndex() != null ? paragraphContent.getOrderIndex() : i + 1)
                        .build();
                
                contents.add(content);
            }
            
            log.info("Successfully extracted {} contents from paragraphRewriteResponse for story: {}", 
                    contents.size(), dataStore.getId());
            
        } else if (dataStore.getStoryRewriteResponse() != null && 
                   dataStore.getStoryRewriteResponse().getRewrittenStory() != null) {
            
            // Fallback to storyRewriteResponse if paragraphRewriteResponse is not available
            log.info("Using storyRewriteResponse as fallback for story: {}", dataStore.getId());
            
            com.breakupstories.model.Content content = com.breakupstories.model.Content.builder()
                    .type(com.breakupstories.model.Content.ContentType.TEXT)
                    .data(dataStore.getStoryRewriteResponse().getRewrittenStory())
                    .orderIndex(1)
                    .build();
            
            contents.add(content);
            
        } else {
            // Last resort - create a placeholder content
            log.warn("No content available for story: {}, creating placeholder", dataStore.getId());
            
            com.breakupstories.model.Content content = com.breakupstories.model.Content.builder()
                    .type(com.breakupstories.model.Content.ContentType.TEXT)
                    .data("Story content not available")
                    .orderIndex(1)
                    .build();
            
            contents.add(content);
        }
        
        return contents;
    }
    
    /**
     * Extract tags from data store
     */
    private List<String> extractTagsFromDataStore(StoryDataStore dataStore) {
        if (dataStore.getStoryAnalysisResponse() != null && 
            dataStore.getStoryAnalysisResponse().getAnalysis() != null &&
            dataStore.getStoryAnalysisResponse().getAnalysis().getTags() != null) {
            
            List<String> tags = dataStore.getStoryAnalysisResponse().getAnalysis().getTags();
            // Filter out empty strings as an additional safety measure
            List<String> filteredTags = tags.stream()
                    .filter(tag -> tag != null && !tag.trim().isEmpty())
                    .map(String::trim)
                    .toList();
            
            log.info("Extracted {} tags from story analysis for story: {} (filtered from {} original tags)", 
                    filteredTags.size(), dataStore.getId(), tags.size());
            return filteredTags;
        }
        
        log.warn("No tags available from story analysis for story: {}", dataStore.getId());
        return new ArrayList<>();
    }
    
    /**
     * Extract emotions from data store
     */
    private List<com.breakupstories.model.Emotion> extractEmotionsFromDataStore(StoryDataStore dataStore) {
        List<com.breakupstories.model.Emotion> emotions = new ArrayList<>();
        
        if (dataStore.getStoryAnalysisResponse() != null && 
            dataStore.getStoryAnalysisResponse().getAnalysis() != null &&
            dataStore.getStoryAnalysisResponse().getAnalysis().getEmotions_with_scores() != null) {
            
            Map<String, Double> emotionScores = dataStore.getStoryAnalysisResponse().getAnalysis().getEmotions_with_scores();
            
            for (Map.Entry<String, Double> entry : emotionScores.entrySet()) {
                try {
                    // Use emotion type as string directly
                    String emotionType = entry.getKey();
                    
                    com.breakupstories.model.Emotion emotion = com.breakupstories.model.Emotion.builder()
                            .type(emotionType)
                            .score(entry.getValue())
                            .build();
                    
                    emotions.add(emotion);
                } catch (Exception e) {
                    log.warn("Error processing emotion type: {} for story: {}", entry.getKey(), dataStore.getId(), e);
                }
            }
            
            log.info("Extracted {} emotions from story analysis for story: {}", emotions.size(), dataStore.getId());
        } else {
            log.warn("No emotions available from story analysis for story: {}", dataStore.getId());
        }
        
        return emotions;
    }
    
    /**
     * Mark story as failed
     */
    private void markStoryAsFailed(StoryDataStore dataStore, String errorMessage) {
        log.error("Marking story as failed: {} - {}", dataStore.getId(), errorMessage);
        
        try {
            // Use StoryStatusService to mark story as failed in both collections
            storyStatusService.markStoryAsFailed(dataStore.getStoryId(), errorMessage);
            
            log.info("Story marked as failed: {}", dataStore.getId());
            
        } catch (Exception e) {
            log.error("Error marking story as failed: {}", dataStore.getId(), e);
        }
    }
    
    /**
     * Convert a single failed story to final Story entity with rejection reasons
     */
    private void convertFailedStory(StoryDataStore dataStore, String requestId) {
        log.info("Converting failed story: {} (Request ID: {})", dataStore.getId(), requestId);
        
        try {
            // Update status to CONVERTING using StoryStatusService
            storyStatusService.updateStatusInBothCollections(dataStore.getStoryId(), Story.StoryStatus.CONVERTING);
            
            // Create final Story entity with rejection reasons
            Story story = createFailedStoryFromDataStore(dataStore, requestId);
            
            // Update status to REJECTED using StoryStatusService
            storyStatusService.updateStatusInBothCollections(dataStore.getStoryId(), Story.StoryStatus.REJECTED);
            
            // Save the Story entity
            Story savedStory = storyRepository.save(story);
            
            log.info("Successfully converted failed story: {} to final Story entity with rejection reasons (Request ID: {})", dataStore.getId(), requestId);
            
        } catch (Exception e) {
            log.error("Error converting failed story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage(), e);
            markStoryAsFailed(dataStore, "Conversion failed: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Create Story entity from failed StoryDataStore
     */
    private Story createFailedStoryFromDataStore(StoryDataStore dataStore, String requestId) {
        log.info("Creating failed Story from StoryDataStore for story: {} (Request ID: {})", dataStore.getId(), requestId);
        
        try {
            // Build rejection reasons from all error fields
            StringBuilder rejectionReasons = new StringBuilder();
            
            // Add general error message
            if (dataStore.getErrorMessage() != null && !dataStore.getErrorMessage().trim().isEmpty()) {
                rejectionReasons.append("General error: ").append(dataStore.getErrorMessage()).append("; ");
            }
            
            // Add general errors
            if (dataStore.getErrors() != null && !dataStore.getErrors().isEmpty()) {
                rejectionReasons.append("General errors: ").append(String.join(", ", dataStore.getErrors())).append("; ");
            }
            
            // Add step-specific errors
            if (dataStore.getTranscriptionError() != null) {
                rejectionReasons.append("Transcription error: ").append(dataStore.getTranscriptionError()).append("; ");
            }
            if (dataStore.getRewriteError() != null) {
                rejectionReasons.append("Rewrite error: ").append(dataStore.getRewriteError()).append("; ");
            }
            if (dataStore.getParagraphError() != null) {
                rejectionReasons.append("Paragraph error: ").append(dataStore.getParagraphError()).append("; ");
            }
            if (dataStore.getAnalysisError() != null) {
                rejectionReasons.append("Analysis error: ").append(dataStore.getAnalysisError()).append("; ");
            }
            
            // Add step errors from map
            if (dataStore.getStepErrors() != null && !dataStore.getStepErrors().isEmpty()) {
                dataStore.getStepErrors().forEach((step, error) -> 
                    rejectionReasons.append(step).append(" error: ").append(error).append("; "));
            }
            
            // If no specific errors found, add a generic rejection reason
            if (rejectionReasons.length() == 0) {
                rejectionReasons.append("Story processing failed - no specific error details available; ");
            }
            
            // Create Story entity with REJECTED status
            Story story = Story.builder()
                    .id(dataStore.getStoryId())
                    .userId(dataStore.getUserId())
                    .title(dataStore.getTitle() != null ? dataStore.getTitle() : "Failed Story")
                    .contents(extractContentsFromDataStore(dataStore))
                    .tags(extractTagsFromDataStore(dataStore))
                    .emotions(extractEmotionsFromDataStore(dataStore))
                    .language(dataStore.getLanguage())
                    .audioUrl(dataStore.getAudioUrl())
                    .thumbnailUrl(defaultConfigService.getDefaultThumbnailUrl())
                    .storyImages(defaultConfigService.getDefaultStoryImages())
                    .duration(dataStore.getDuration())
                    .rejectionReasons(List.of(rejectionReasons.toString()))
                    .status(Story.StoryStatus.REJECTED) // Failed stories are rejected
                    .createdAt(dataStore.getCreatedAt())
                    .updatedAt(TimestampUtil.currentLocalDateTime())
                    .build();
            
            // Save the Story entity
            Story savedStory = storyRepository.save(story);
            
            log.info("Successfully created failed Story from StoryDataStore for story: {} (Request ID: {})", dataStore.getId(), requestId);
            
            return savedStory;
            
        } catch (Exception e) {
            log.error("Error creating failed Story from StoryDataStore for story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to create failed Story from StoryDataStore: " + e.getMessage(), e);
        }
    }

    /**
     * Create rejected Story entity from StoryDataStore with custom rejection reason
     */
    private Story createRejectedStoryFromDataStore(StoryDataStore dataStore, String requestId, String rejectionReason) {
        log.info("Creating rejected Story from StoryDataStore for story: {} (Request ID: {})", dataStore.getId(), requestId);
        
        try {
            // Build rejection reasons from all error fields
            StringBuilder rejectionReasons = new StringBuilder();
            
            // Add the custom rejection reason first
            rejectionReasons.append(rejectionReason).append("; ");
            
            // Add general errors
            if (dataStore.getErrors() != null && !dataStore.getErrors().isEmpty()) {
                rejectionReasons.append("General errors: ").append(String.join(", ", dataStore.getErrors())).append("; ");
            }
            
            // Add step-specific errors
            if (dataStore.getTranscriptionError() != null) {
                rejectionReasons.append("Transcription error: ").append(dataStore.getTranscriptionError()).append("; ");
            }
            if (dataStore.getRewriteError() != null) {
                rejectionReasons.append("Rewrite error: ").append(dataStore.getRewriteError()).append("; ");
            }
            if (dataStore.getParagraphError() != null) {
                rejectionReasons.append("Paragraph error: ").append(dataStore.getParagraphError()).append("; ");
            }
            if (dataStore.getAnalysisError() != null) {
                rejectionReasons.append("Analysis error: ").append(dataStore.getAnalysisError()).append("; ");
            }
            
            // Add step errors from map
            if (dataStore.getStepErrors() != null && !dataStore.getStepErrors().isEmpty()) {
                dataStore.getStepErrors().forEach((step, error) -> 
                    rejectionReasons.append(step).append(" error: ").append(error).append("; "));
            }
            
            // Create Story entity with REJECTED status
            Story story = Story.builder()
                    .id(dataStore.getStoryId())
                    .userId(dataStore.getUserId())
                    .title(dataStore.getTitle() != null ? dataStore.getTitle() : "Rejected Story")
                    .contents(extractContentsFromDataStore(dataStore))
                    .tags(extractTagsFromDataStore(dataStore))
                    .emotions(extractEmotionsFromDataStore(dataStore))
                    .language(dataStore.getLanguage())
                    .audioUrl(dataStore.getAudioUrl())
                    .thumbnailUrl(defaultConfigService.getDefaultThumbnailUrl())
                    .storyImages(defaultConfigService.getDefaultStoryImages())
                    .duration(dataStore.getDuration())
                    .rejectionReasons(List.of(rejectionReasons.toString()))
                    .status(Story.StoryStatus.REJECTED) // Invalid stories are rejected
                    .createdAt(dataStore.getCreatedAt())
                    .updatedAt(TimestampUtil.currentLocalDateTime())
                    .build();
            
            // Save the Story entity
            Story savedStory = storyRepository.save(story);
            
            log.info("Successfully created rejected Story from StoryDataStore for story: {} (Request ID: {})", dataStore.getId(), requestId);
            
            return savedStory;
            
        } catch (Exception e) {
            log.error("Error creating rejected Story from StoryDataStore for story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to create rejected Story from StoryDataStore: " + e.getMessage(), e);
        }
    }
} 