package com.breakupstories.service;

import com.breakupstories.model.StoryDataStore;
import com.breakupstories.repository.StoryDataStoreRepository;
import com.breakupstories.util.RequestIdGenerator;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Background worker that handles TTS audio upload for written stories
 * 
 * Workflow:
 * 1. Fetches stories with WRITTEN_UPLOAD_PENDING status
 * 2. Marks them as PROCESSING
 * 3. Extracts TTS audio data from upload metadata
 * 4. Uploads audio to Cloudinary
 * 5. Updates story with audio URL
 * 6. Marks as STORY_CONVERSION_PENDING for final conversion
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AudioConversionWorker {
    
    private final StoryDataStoreRepository storyDataStoreRepository;
    private final Cloudinary cloudinary;
    
    /**
     * Process TTS audio uploads every 7 minutes
     */
    @Scheduled(fixedRate = 60000) // 7 minutes = 420,000 milliseconds
    public void processTTSAudioUploads() {
        String requestId = RequestIdGenerator.generateRequestId();
        log.info("Starting TTS audio conversion worker (Request ID: {})", requestId);
        
        try {
            // Fetch stories with WRITTEN_UPLOAD_PENDING status, ordered by creation time (oldest first)
            List<StoryDataStore> pendingStories = storyDataStoreRepository.findByProcessingStatusOrderByCreatedAtAscLimit(
                StoryDataStore.ProcessingStatus.WRITTEN_UPLOAD_PENDING, 10);
            
            if (pendingStories.isEmpty()) {
                log.info("No stories pending TTS audio upload (Request ID: {})", requestId);
                return;
            }
            
            log.info("Found {} stories pending TTS audio upload (Request ID: {})", pendingStories.size(), requestId);

            pendingStories.forEach(dataStore-> {
                        // Mark as PROCESSING immediately after fetching to prevent duplicate processing
                        dataStore.setProcessingStatus(StoryDataStore.ProcessingStatus.PROCESSING);
                        storyDataStoreRepository.save(dataStore);
                        log.info("Marked story {} as PROCESSING before TTS audio upload (Request ID: {})",
                                dataStore.getId(), requestId);
                    });
            // Process each pending story
            for (StoryDataStore dataStore : pendingStories) {
                try {
                    // Process TTS audio upload
                    processTTSAudioUpload(dataStore, requestId);
                } catch (Exception e) {
                    log.error("Error processing TTS audio upload for story {} (Request ID: {}): {}", 
                            dataStore.getId(), requestId, e.getMessage(), e);
                    markStoryAsFailed(dataStore, "TTS audio upload failed: " + e.getMessage());
                }
            }
            
            log.info("TTS audio conversion worker completed (Request ID: {})", requestId);
            
        } catch (Exception e) {
            log.error("Error in TTS audio conversion worker (Request ID: {}): {}", requestId, e.getMessage(), e);
        }
    }
    
    /**
     * Process TTS audio upload for a single story
     */
    private void processTTSAudioUpload(StoryDataStore dataStore, String requestId) {
        log.info("Processing TTS audio upload for story: {} (Request ID: {})", dataStore.getId(), requestId);
        
        try {
            // Extract TTS audio data from upload metadata
            byte[] audioData = extractTTSAudioData(dataStore);
            if (audioData == null || audioData.length == 0) {
                throw new RuntimeException("TTS audio data not found or empty in upload metadata");
            }
            
            log.info("Extracted TTS audio data for story: {} - size: {} bytes (Request ID: {})", 
                    dataStore.getId(), audioData.length, requestId);
            
            // Upload audio to Cloudinary
            String fileName = dataStore.getStoryId() + ".mp3";
            String audioUrl = uploadAudio(audioData, fileName);
            
            log.info("Successfully uploaded TTS audio for story: {} - URL: {} (Request ID: {})", 
                    dataStore.getId(), audioUrl, requestId);
            
            // Update story data store with audio URL
            dataStore.setAudioUrl(audioUrl);
            
            // Mark as STORY_CONVERSION_PENDING for final conversion
            dataStore.setProcessingStatus(StoryDataStore.ProcessingStatus.STORY_CONVERSION_PENDING);
            storyDataStoreRepository.save(dataStore);
            
            log.info("Story {} marked as STORY_CONVERSION_PENDING after TTS audio upload (Request ID: {})", 
                    dataStore.getId(), requestId);
            
        } catch (Exception e) {
            log.error("Error processing TTS audio upload for story {} (Request ID: {}): {}", 
                    dataStore.getId(), requestId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Extract TTS audio data from upload metadata
     */
    private byte[] extractTTSAudioData(StoryDataStore dataStore) {
        try {
            Map<String, String> uploadMetadata = dataStore.getUploadMetadata();
            if (uploadMetadata == null) {
                log.error("Upload metadata is null for story: {}", dataStore.getId());
                return null;
            }
            
            String ttsAudioData = uploadMetadata.get("ttsAudioData");
            if (ttsAudioData == null || ttsAudioData.isEmpty()) {
                log.error("TTS audio data not found in upload metadata for story: {}", dataStore.getId());
                return null;
            }
            
            // Decode base64 data
            byte[] audioBytes = java.util.Base64.getDecoder().decode(ttsAudioData);
            
            log.info("Successfully extracted TTS audio data for story: {} - size: {} bytes", 
                    dataStore.getId(), audioBytes.length);
            
            return audioBytes;
            
        } catch (Exception e) {
            log.error("Error extracting TTS audio data for story: {}", dataStore.getId(), e);
            return null;
        }
    }
    
    /**
     * Upload audio to Cloudinary
     */
    public String uploadAudio(byte[] audioData, String fileName) {
        try {
            log.info("Uploading TTS audio to Cloudinary: {}", fileName);
            
            // Generate unique public ID
            String publicId = "breakup/audio/" + UUID.randomUUID().toString();
            
            // Upload to Cloudinary
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    audioData,
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "resource_type", "video", // Cloudinary treats audio as video
                            "folder", "breakup/audio",
                            "format", "mp3"
                    )
            );
            
            String audioUrl = (String) uploadResult.get("secure_url");
            log.info("Successfully uploaded TTS audio to Cloudinary: {}", audioUrl);
            
            return audioUrl;
            
        } catch (Exception e) {
            log.error("Error uploading TTS audio to Cloudinary: {}", e.getMessage());
            throw new RuntimeException("Failed to upload TTS audio to Cloudinary: " + e.getMessage(), e);
        }
    }
    
    /**
     * Mark story as failed
     */
    private void markStoryAsFailed(StoryDataStore dataStore, String errorMessage) {
        log.error("Marking story as failed: {} - {}", dataStore.getId(), errorMessage);
        
        try {
            // Update the StoryDataStore status to FAILED
            dataStore.setProcessingStatus(StoryDataStore.ProcessingStatus.FAILED);
            dataStore.setErrorMessage(errorMessage);
            storyDataStoreRepository.save(dataStore);
            
            log.info("Story marked as failed: {}", dataStore.getId());
            
        } catch (Exception e) {
            log.error("Error marking story as failed: {}", dataStore.getId(), e);
        }
    }
} 