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
 * Background worker that converts stories with STORY_CONVERSION_PENDING status to final Story entities
 * 
 * Workflow:
 * 1. Fetches stories with STORY_CONVERSION_PENDING status
 * 2. Marks them as COMPLETED immediately after fetching to prevent duplicate processing
 * 3. Converts StoryDataStore to final Story entity
 * 4. Any errors mark the story as FAILED with error details
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
    private final AudioConversionWorker audioConversionWorker;
    
    /**
     * Process conversions every 1 minute
     */
    @Scheduled(fixedRate = 60000) // 1 minute
    public void convertStories() {
        String requestId = RequestIdGenerator.generateRequestId();
        log.info("Starting story conversion worker (Request ID: {})", requestId);
        
        try {
            // Fetch stories with isConversionPending = true, ordered by creation time (oldest first)
            List<StoryDataStore> pendingStories = storyDataStoreRepository.findByIsConversionPendingTrueOrderByCreatedAtAscLimit(10);
            
            if (pendingStories.isEmpty()) {
                log.info("No stories pending conversion (Request ID: {})", requestId);
                return;
            }
            
            log.info("Found {} stories pending conversion (Request ID: {})", pendingStories.size(), requestId);
            
            // First, mark all stories as isConversionPending = false to prevent race conditions
            List<StoryDataStore> storiesToProcess = new ArrayList<>();
            for (StoryDataStore dataStore : pendingStories) {
                try {
                    // Mark isConversionPending as false immediately to prevent duplicate processing
                    dataStore.setConversionPending(false);
                    storyDataStoreRepository.save(dataStore);
                    log.info("Marked story {} isConversionPending as false (Request ID: {})", dataStore.getId(), requestId);
                    storiesToProcess.add(dataStore);
                } catch (Exception e) {
                    log.error("Error marking story {} isConversionPending as false (Request ID: {}): {}", 
                            dataStore.getId(), requestId, e.getMessage(), e);
                }
            }
            
            log.info("Marked {} stories as not pending conversion, now processing them (Request ID: {})", 
                    storiesToProcess.size(), requestId);
            
            // Now process each story based on their status and creation type
            for (StoryDataStore dataStore : storiesToProcess) {
                try {
                    processStoryBasedOnStatus(dataStore, requestId);
                } catch (Exception e) {
                    log.error("Error processing story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage(), e);
                    markStoryAsFailed(dataStore, "Processing failed: " + e.getMessage());
                }
            }
            
            log.info("Story conversion worker completed (Request ID: {})", requestId);
            
        } catch (Exception e) {
            log.error("Error in story conversion worker (Request ID: {}): {}", requestId, e.getMessage(), e);
        }
    }
    
    /**
     * Process story based on status and creation type
     */
    private void processStoryBasedOnStatus(StoryDataStore dataStore, String requestId) {
        log.info("Processing story based on status: {} (Request ID: {})", dataStore.getId(), requestId);
        
        try {
            // Check the current processing status
            StoryDataStore.ProcessingStatus currentStatus = dataStore.getProcessingStatus();
            
            // Handle failed or rejected status
            if (currentStatus == StoryDataStore.ProcessingStatus.FAILED || 
                currentStatus == StoryDataStore.ProcessingStatus.REJECTED) {
                
                processFailedOrRejectedStory(dataStore, requestId);
                return;
            }
            
            // Handle completed status
            if (currentStatus == StoryDataStore.ProcessingStatus.COMPLETED) {
                // Determine creation type from metadata
                Story.CreationType creationType = getCreationType(dataStore);
                
                if (creationType == Story.CreationType.UPLOADED) {
                    processCompletedUploadedStory(dataStore, requestId);
                } else if (creationType == Story.CreationType.WRITTEN) {
                    processCompletedWrittenStory(dataStore, requestId);
                }
            } else {
                log.warn("Story {} has unexpected status: {} (Request ID: {})", 
                        dataStore.getId(), currentStatus, requestId);
            }
            
        } catch (Exception e) {
            log.error("Error processing story based on status {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage(), e);
            markStoryAsFailed(dataStore, "Processing failed: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Process failed or rejected stories
     */
    private void processFailedOrRejectedStory(StoryDataStore dataStore, String requestId) {
        log.warn("Processing failed/rejected story: {} (Request ID: {})", dataStore.getId(), requestId);
        
        StoryDataStore.ProcessingStatus currentStatus = dataStore.getProcessingStatus();
        
        if (currentStatus == StoryDataStore.ProcessingStatus.FAILED) {
            createFailedStoryFromDataStore(dataStore, requestId);
        } else if (currentStatus == StoryDataStore.ProcessingStatus.REJECTED) {
            createRejectedStoryFromDataStore(dataStore, requestId);
        }
    }
    
    /**
     * Process completed uploaded stories
     */
    private void processCompletedUploadedStory(StoryDataStore dataStore, String requestId) {
        log.info("Processing completed uploaded story: {} (Request ID: {})", dataStore.getId(), requestId);
        
        // For uploaded stories, proceed with story conversion
        convertStory(dataStore, requestId);
    }
    
    /**
     * Process completed written stories
     */
    private void processCompletedWrittenStory(StoryDataStore dataStore, String requestId) {
        log.info("Processing completed written story: {} (Request ID: {})", dataStore.getId(), requestId);
        
        try {
            // Check if TTS audio data exists in metadata
            if (dataStore.getUploadMetadata() != null && dataStore.getUploadMetadata().get("ttsAudioData") != null) {
                // TTS audio data exists, upload it to Cloudinary
                String audioUrl = uploadTTSAudioToCloudinary(dataStore, requestId);
                dataStore.setAudioUrl(audioUrl);
                
                // Remove TTS audio data from upload metadata to save storage
                dataStore.getUploadMetadata().remove("ttsAudioData");
                // Save the updated StoryDataStore to MongoDB to persist the removal
                storyDataStoreRepository.save(dataStore);
                log.info("Removed TTS audio data from upload metadata for story: {} (Request ID: {})", dataStore.getId(), requestId);
                
                log.info("Successfully uploaded TTS audio for written story: {} - URL: {} (Request ID: {})", 
                        dataStore.getId(), audioUrl, requestId);
            } else {
                // No TTS audio data, use default audio or mark as failed
                log.warn("No TTS audio data found for written story: {} (Request ID: {})", dataStore.getId(), requestId);
                dataStore.setAudioUrl(defaultConfigService.getDefaultAudioUrl());
            }
            
            // Proceed with story conversion
            createStoryFromDataStore(dataStore, requestId);
            
            log.info("Successfully converted written story: {} (Request ID: {})", dataStore.getId(), requestId);
                    
        } catch (Exception e) {
            log.error("Error processing written story {} (Request ID: {}): {}", 
                    dataStore.getId(), requestId, e.getMessage(), e);
            markStoryAsFailed(dataStore, "Written story conversion failed: " + e.getMessage());
        }
    }
    
    /**
     * Upload TTS audio to Cloudinary
     */
    private String uploadTTSAudioToCloudinary(StoryDataStore dataStore, String requestId) {
        try {
            Map<String, String> uploadMetadata = dataStore.getUploadMetadata();
            if (uploadMetadata == null) {
                throw new RuntimeException("Upload metadata is null");
            }
            
            String ttsAudioData = uploadMetadata.get("ttsAudioData");
            if (ttsAudioData == null || ttsAudioData.isEmpty()) {
                throw new RuntimeException("TTS audio data not found in upload metadata");
            }
            
            // Validate base64 string
            if (!ttsAudioData.matches("^[A-Za-z0-9+/]*={0,2}$")) {
                throw new RuntimeException("Invalid base64 format for TTS audio data");
            }
            
            // Log the prefix for analysis (don't remove it yet)
            if (ttsAudioData.startsWith("//")) {
                int prefixEnd = ttsAudioData.indexOf("UklGR"); // Common WAV file header in base64
                if (prefixEnd == -1) {
                    prefixEnd = ttsAudioData.indexOf("SUQz"); // Common MP3 file header in base64
                }
                if (prefixEnd != -1) {
                    String prefix = ttsAudioData.substring(0, prefixEnd);
                    log.info("Found audio prefix '{}' for story: {} (Request ID: {}) - length: {} chars", 
                            prefix, dataStore.getId(), requestId, prefix.length());
                }
            }
            
            // Decode base64 data (keeping prefix for now)
            byte[] audioBytes;
            try {
                audioBytes = java.util.Base64.getDecoder().decode(ttsAudioData);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid base64 data for TTS audio: " + e.getMessage());
            }
            
            // Validate decoded data
            if (audioBytes == null || audioBytes.length == 0) {
                throw new RuntimeException("Decoded TTS audio data is null or empty");
            }
            
            log.info("Extracted TTS audio data for story: {} - size: {} bytes (Request ID: {})", 
                    dataStore.getId(), audioBytes.length, requestId);
            
            // Use the AudioConversionWorker's upload method
            String fileName = dataStore.getStoryId() + ".mp3";
            String audioUrl = audioConversionWorker.uploadAudio(audioBytes, fileName);
            
            log.info("Successfully uploaded TTS audio for story: {} - URL: {} (Request ID: {})", 
                    dataStore.getId(), audioUrl, requestId);
            
            return audioUrl;
            
        } catch (Exception e) {
            log.error("Error uploading TTS audio for story {} (Request ID: {}): {}", 
                    dataStore.getId(), requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to upload TTS audio: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get creation type from story data store
     */
    private Story.CreationType getCreationType(StoryDataStore dataStore) {
        Story.CreationType creationType = Story.CreationType.UPLOADED; // Default
        if (dataStore.getUploadMetadata() != null && dataStore.getUploadMetadata().get("creationType") != null) {
            String creationTypeStr = dataStore.getUploadMetadata().get("creationType");
            try {
                creationType = Story.CreationType.valueOf(creationTypeStr);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid creation type in metadata: {}, using default UPLOADED", creationTypeStr);
            }
        }
        return creationType;
    }
    
    /**
     * Convert a single processed story to final Story entity
     */
    private void convertStory(StoryDataStore dataStore, String requestId) {
        log.info("Converting story: {} (Request ID: {})", dataStore.getId(), requestId);
        
        try {
            // Proceed with story conversion
            createStoryFromDataStore(dataStore, requestId);
            
        } catch (Exception e) {
            log.error("Error converting story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage(), e);
            markStoryAsFailed(dataStore, "Conversion failed: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Create or update Story entity from StoryDataStore
     */
    private void createStoryFromDataStore(StoryDataStore dataStore, String requestId) {
        log.info("Creating/Updating Story from StoryDataStore for story: {} (Request ID: {})", dataStore.getId(), requestId);
        
        try {
            // Build rejection reasons from errors array only
            List<String> rejectionReasons = new ArrayList<>();
            
            // Add errors from StoryDataStore.errors array
            if (dataStore.getErrors() != null && !dataStore.getErrors().isEmpty()) {
                rejectionReasons.addAll(dataStore.getErrors());
            }
            
            // Add step errors from map if available
            if (dataStore.getStepErrors() != null && !dataStore.getStepErrors().isEmpty()) {
                dataStore.getStepErrors().forEach((step, error) -> 
                    rejectionReasons.add(step + " error: " + error));
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
            
            // Check if Story already exists
            Story existingStory = storyRepository.findById(dataStore.getStoryId()).orElse(null);
            Story story;
            
            if (existingStory != null) {
                log.info("Story already exists, updating with StoryDataStore data: {} (Request ID: {})", dataStore.getStoryId(), requestId);
                
                // Update existing story with data from StoryDataStore (overwrite if data exists)
                story = updateExistingStoryWithDataStore(existingStory, dataStore, creationType, rejectionReasons);
            } else {
                log.info("Creating new Story from StoryDataStore: {} (Request ID: {})", dataStore.getStoryId(), requestId);
                
                // Create new story
                story = Story.builder()
                        .id(dataStore.getStoryId())
                        .userId(dataStore.getUserId())
                        .title(dataStore.getTitle())
                        .contents(extractContentsFromDataStore(dataStore))
                        .tags(extractTagsFromDataStore(dataStore))
                        .emotions(extractEmotionsFromDataStore(dataStore))
                        .language(dataStore.getLanguage())
                        .audioUrl(dataStore.getAudioUrl())
                        .thumbnailUrl(extractThumbnailUrl(dataStore))
                        .storyImages(extractStoryImages(dataStore))
                        .duration(dataStore.getDuration())
                        .rejectionReasons(!rejectionReasons.isEmpty() ? List.of(rejectionReasons.toString()) : null)
                        .status(Story.StoryStatus.ACTIVE) // All converted stories are active
                        .creationType(creationType)
                        .createdAt(dataStore.getCreatedAt())
                        .updatedAt(TimestampUtil.currentLocalDateTime())
                        .build();
            }
            
            storyRepository.save(story);
            
            // Set isConversionPending to false for successful conversions
            dataStore.setConversionPending(false);
            storyDataStoreRepository.save(dataStore);
            
            // Check for first story reward
            boolean rewardGiven = firstStoryRewardService.checkAndRewardFirstStory(dataStore.getUserId(), dataStore.getStoryId());
            if (rewardGiven) {
                log.info("First story reward check completed for story: {} (Request ID: {})", dataStore.getId(), requestId);
            }
            
            log.info("Successfully created/updated Story from StoryDataStore for story: {} (Request ID: {})", dataStore.getId(), requestId);

        } catch (Exception e) {
            log.error("Error creating/updating Story from StoryDataStore for story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to create/update Story from StoryDataStore: " + e.getMessage(), e);
        }
    }
    
    /**
     * Update existing story with data from StoryDataStore (overwrite if data exists)
     */
    private Story updateExistingStoryWithDataStore(Story existingStory, StoryDataStore dataStore, 
                                                   Story.CreationType creationType, List<String> rejectionReasons) {
        log.info("Updating existing story with StoryDataStore data: {} (Request ID: {})", dataStore.getStoryId(), dataStore.getId());
        
        // Extract data from StoryDataStore
        List<com.breakupstories.model.Content> contents = extractContentsFromDataStore(dataStore);
        List<String> tags = extractTagsFromDataStore(dataStore);
        List<com.breakupstories.model.Emotion> emotions = extractEmotionsFromDataStore(dataStore);
        String thumbnailUrl = extractThumbnailUrl(dataStore);
        List<String> storyImages = extractStoryImages(dataStore);
        
        // Update existing story - overwrite with StoryDataStore data if it exists
        if (dataStore.getTitle() != null && !dataStore.getTitle().trim().isEmpty()) {
            existingStory.setTitle(dataStore.getTitle());
        }
        
        if (!contents.isEmpty()) {
            existingStory.setContents(contents);
        }
        
        if (!tags.isEmpty()) {
            existingStory.setTags(tags);
        }
        
        if (!emotions.isEmpty()) {
            existingStory.setEmotions(emotions);
        }
        
        if (dataStore.getLanguage() != null && !dataStore.getLanguage().trim().isEmpty()) {
            existingStory.setLanguage(dataStore.getLanguage());
        }
        
        if (dataStore.getAudioUrl() != null && !dataStore.getAudioUrl().trim().isEmpty()) {
            existingStory.setAudioUrl(dataStore.getAudioUrl());
        }
        
        if (thumbnailUrl != null && !thumbnailUrl.trim().isEmpty()) {
            existingStory.setThumbnailUrl(thumbnailUrl);
        }
        
        if (!storyImages.isEmpty()) {
            existingStory.setStoryImages(storyImages);
        }
        
        if (dataStore.getDuration() != null) {
            existingStory.setDuration(dataStore.getDuration());
        }
        
        // Update rejection reasons if any
        if (rejectionReasons != null && !rejectionReasons.isEmpty()) {
            existingStory.setRejectionReasons(rejectionReasons);
        }
        
        // Update status to ACTIVE
        existingStory.setStatus(Story.StoryStatus.ACTIVE);
        
        // Update creation type
        existingStory.setCreationType(creationType);
        
        // Update timestamp
        existingStory.setUpdatedAt(TimestampUtil.currentLocalDateTime());
        
        log.info("Successfully updated existing story with StoryDataStore data: {} (Request ID: {})", 
                dataStore.getStoryId(), dataStore.getId());
        
        return existingStory;
    }
    
    /**
     * Extract contents from data store
     */
    private List<com.breakupstories.model.Content> extractContentsFromDataStore(StoryDataStore dataStore) {
        List<com.breakupstories.model.Content> contents = new ArrayList<>();
        
        // First, try to extract from paragraphRewriteResponse (preferred)
        if (dataStore.getParagraphRewriteResponse() != null && 
            dataStore.getParagraphRewriteResponse().getParagraphs() != null) {
            
            log.info("Extracting {} paragraphs from paragraphRewriteResponse for story: {}", 
                    dataStore.getParagraphRewriteResponse().getParagraphs().size(), dataStore.getId());
            
            for (int i = 0; i < dataStore.getParagraphRewriteResponse().getParagraphs().size(); i++) {
                var paragraphContent = dataStore.getParagraphRewriteResponse().getParagraphs().get(i);
                
                com.breakupstories.model.Content content = com.breakupstories.model.Content.builder()
                        .type(com.breakupstories.model.Content.ContentType.TEXT)
                        .data(paragraphContent.getRewrittenText())
                        .orderIndex(paragraphContent.getParagraphNumber() != null ? paragraphContent.getParagraphNumber() : i + 1)
                        .build();
                
                contents.add(content);
            }
            
            log.info("Successfully extracted {} contents from paragraphRewriteResponse for story: {}", 
                    contents.size(), dataStore.getId());
            
        } else if (dataStore.getStoryRewriteResponse() != null && 
                   dataStore.getStoryRewriteResponse().getRewrittenText() != null) {
            
            // Fallback to storyRewriteResponse if paragraphRewriteResponse is not available
            log.info("Using storyRewriteResponse as fallback for story: {}", dataStore.getId());
            
            com.breakupstories.model.Content content = com.breakupstories.model.Content.builder()
                    .type(com.breakupstories.model.Content.ContentType.TEXT)
                    .data(dataStore.getStoryRewriteResponse().getRewrittenText())
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
     * Extract thumbnail URL from data store
     */
    private String extractThumbnailUrl(StoryDataStore dataStore) {
        if (dataStore.getImagesResponse() != null && 
            dataStore.getImagesResponse().getThumbnailImageUrl() != null) {
            log.info("Using thumbnail from images response for story: {}", dataStore.getId());
            return dataStore.getImagesResponse().getThumbnailImageUrl();
        }
        
        log.warn("No thumbnail available from images response for story: {}, using default", dataStore.getId());
        return defaultConfigService.getDefaultThumbnailUrl();
    }
    
    /**
     * Extract story images from data store
     */
    private List<String> extractStoryImages(StoryDataStore dataStore) {
        if (dataStore.getImagesResponse() != null && 
            dataStore.getImagesResponse().getStoryImageUrls() != null) {
            log.info("Using {} story images from images response for story: {}", 
                    dataStore.getImagesResponse().getStoryImageUrls().size(), dataStore.getId());
            return dataStore.getImagesResponse().getStoryImageUrls();
        }
        
        log.warn("No story images available from images response for story: {}, using default", dataStore.getId());
        return defaultConfigService.getDefaultStoryImages();
    }
    
    /**
     * Extract emotions from data store
     */
    private List<com.breakupstories.model.Emotion> extractEmotionsFromDataStore(StoryDataStore dataStore) {
        List<com.breakupstories.model.Emotion> emotions = new ArrayList<>();
        
        if (dataStore.getStoryAnalysisResponse() != null && 
            dataStore.getStoryAnalysisResponse().getAnalysis() != null &&
            dataStore.getStoryAnalysisResponse().getAnalysis().getEmotionsWithScores() != null) {
            
            Map<String, Double> emotionScores = dataStore.getStoryAnalysisResponse().getAnalysis().getEmotionsWithScores();
            
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
            // Update the StoryDataStore status to FAILED and set isConversionPending to false
            dataStore.setProcessingStatus(StoryDataStore.ProcessingStatus.FAILED);
            dataStore.setErrorMessage(errorMessage);
            dataStore.setConversionPending(false);
            storyDataStoreRepository.save(dataStore);
            
            log.info("Story marked as failed: {}", dataStore.getId());
            
        } catch (Exception e) {
            log.error("Error marking story as failed: {}", dataStore.getId(), e);
        }
    }
    

    


    /**
     * Create failed Story entity from StoryDataStore
     */
    private void createFailedStoryFromDataStore(StoryDataStore dataStore, String requestId) {
        log.info("Creating failed Story from StoryDataStore for story: {} (Request ID: {})", dataStore.getId(), requestId);
        
        try {
            // Build rejection reasons from errors array or use default message
            List<String> rejectionReasons = new ArrayList<>();
            
            // If errors array has data → use actual errors
            if (dataStore.getErrors() != null && !dataStore.getErrors().isEmpty()) {
                rejectionReasons.addAll(dataStore.getErrors());
            } else {
                // Fallback to default message for failed stories
                rejectionReasons.add("Story processing failed due to technical issues");
            }
            
            // Add step errors from map if available
            if (dataStore.getStepErrors() != null && !dataStore.getStepErrors().isEmpty()) {
                dataStore.getStepErrors().forEach((step, error) -> 
                    rejectionReasons.add(step + " error: " + error));
            }
            
            // Create Story entity with REJECTED status (failed stories are also marked as rejected)
            Story story = Story.builder()
                    .id(dataStore.getStoryId())
                    .userId(dataStore.getUserId())
                    .title(dataStore.getTitle() != null ? dataStore.getTitle() : "Failed Story")
                    .contents(extractContentsFromDataStore(dataStore))
                    .tags(extractTagsFromDataStore(dataStore))
                    .emotions(extractEmotionsFromDataStore(dataStore))
                    .language(dataStore.getLanguage())
                    .audioUrl(dataStore.getAudioUrl())
                    .thumbnailUrl(extractThumbnailUrl(dataStore))
                    .storyImages(extractStoryImages(dataStore))
                    .duration(dataStore.getDuration())
                    .rejectionReasons(rejectionReasons)
                    .status(Story.StoryStatus.REJECTED) // Failed stories are marked as rejected
                    .createdAt(dataStore.getCreatedAt())
                    .updatedAt(TimestampUtil.currentLocalDateTime())
                    .build();

            // Save the Story entity
            Story savedStory = storyRepository.save(story);

            // Set isConversionPending to false for failed stories
            dataStore.setConversionPending(false);
            storyDataStoreRepository.save(dataStore);
            
            log.info("Successfully created failed Story from StoryDataStore for story: {} (Request ID: {})", dataStore.getId(), requestId);

        } catch (Exception e) {
            log.error("Error creating failed Story from StoryDataStore for story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to create failed Story from StoryDataStore: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create rejected Story entity from StoryDataStore
     */
    private void createRejectedStoryFromDataStore(StoryDataStore dataStore, String requestId) {
        log.info("Creating rejected Story from StoryDataStore for story: {} (Request ID: {})", dataStore.getId(), requestId);
        
        try {
            // Build rejection reasons from errors array or use default message
            List<String> rejectionReasons = new ArrayList<>();
            
            // If errors array has data → use actual errors
            if (dataStore.getErrors() != null && !dataStore.getErrors().isEmpty()) {
                rejectionReasons.addAll(dataStore.getErrors());
            } else {
                // Fallback to default message for rejected stories
                rejectionReasons.add("Story may not be related to love or breakup");
            }
            
            // Add step errors from map if available
            if (dataStore.getStepErrors() != null && !dataStore.getStepErrors().isEmpty()) {
                dataStore.getStepErrors().forEach((step, error) -> 
                    rejectionReasons.add(error));
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
                    .thumbnailUrl(extractThumbnailUrl(dataStore))
                    .storyImages(extractStoryImages(dataStore))
                    .duration(dataStore.getDuration())
                    .rejectionReasons(rejectionReasons)
                    .status(Story.StoryStatus.REJECTED) // Invalid stories are rejected
                    .createdAt(dataStore.getCreatedAt())
                    .updatedAt(TimestampUtil.currentLocalDateTime())
                    .build();
            
            // Save the Story entity
            Story savedStory = storyRepository.save(story);
            
            // Set isConversionPending to false for rejected stories
            dataStore.setConversionPending(false);
            storyDataStoreRepository.save(dataStore);
            
            log.info("Successfully created rejected Story from StoryDataStore for story: {} (Request ID: {})", dataStore.getId(), requestId);

        } catch (Exception e) {
            log.error("Error creating rejected Story from StoryDataStore for story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to create rejected Story from StoryDataStore: " + e.getMessage(), e);
        }
    }
} 