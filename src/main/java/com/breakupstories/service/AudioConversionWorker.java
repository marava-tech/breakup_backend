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
            
            // Remove TTS audio data from upload metadata to save storage
            if (dataStore.getUploadMetadata() != null) {
                dataStore.getUploadMetadata().remove("ttsAudioData");
                log.info("Removed TTS audio data from upload metadata for story: {} (Request ID: {})", dataStore.getId(), requestId);
            }
            
            // Mark as COMPLETED for final conversion
            dataStore.setProcessingStatus(StoryDataStore.ProcessingStatus.COMPLETED);
            storyDataStoreRepository.save(dataStore);
            
            log.info("Story {} marked as COMPLETED after TTS audio upload (Request ID: {})", 
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
            
            // Validate base64 string
            if (!ttsAudioData.matches("^[A-Za-z0-9+/]*={0,2}$")) {
                log.error("Invalid base64 format for TTS audio data in story: {}", dataStore.getId());
                return null;
            }
            
            // Log the prefix for analysis (don't remove it yet)
            if (ttsAudioData.startsWith("//")) {
                int prefixEnd = ttsAudioData.indexOf("UklGR"); // Common WAV file header in base64
                if (prefixEnd == -1) {
                    prefixEnd = ttsAudioData.indexOf("SUQz"); // Common MP3 file header in base64
                }
                if (prefixEnd != -1) {
                    String prefix = ttsAudioData.substring(0, prefixEnd);
                    log.info("Found audio prefix '{}' for story: {} - length: {} chars", 
                            prefix, dataStore.getId(), prefix.length());
                }
            }
            
            // Decode base64 data (keeping prefix for now)
            byte[] audioBytes = java.util.Base64.getDecoder().decode(ttsAudioData);
            
            // Validate decoded data
            if (audioBytes == null || audioBytes.length == 0) {
                log.error("Decoded TTS audio data is null or empty for story: {}", dataStore.getId());
                return null;
            }
            
            log.info("Successfully extracted TTS audio data for story: {} - size: {} bytes", 
                    dataStore.getId(), audioBytes.length);
            
            return audioBytes;
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid base64 data for TTS audio in story: {} - {}", dataStore.getId(), e.getMessage());
            return null;
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
            
            // Validate audio data
            if (audioData == null || audioData.length == 0) {
                throw new RuntimeException("Audio data is null or empty");
            }
            
            // Check minimum size for valid audio (at least 1KB)
            if (audioData.length < 1024) {
                log.warn("Audio data size is very small: {} bytes for file: {}", audioData.length, fileName);
            }
            
            // Generate unique public ID with proper extension
            String basePublicId = "breakup/audio/" + UUID.randomUUID().toString();
            String publicId = basePublicId + ".mp3"; // Ensure .mp3 extension
            
            log.info("Uploading audio data to Cloudinary - size: {} bytes, public_id: {}", audioData.length, publicId);
            
            // Upload to Cloudinary with proper audio configuration
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    audioData,
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "resource_type", "video", // Cloudinary treats audio as video
                            "format", "mp3",
                            "overwrite", true,
                            "invalidate", true
                    )
            );
            
            String audioUrl = (String) uploadResult.get("secure_url");
            if (audioUrl == null || audioUrl.isEmpty()) {
                throw new RuntimeException("Cloudinary upload succeeded but returned null/empty URL");
            }
            
            log.info("Successfully uploaded TTS audio to Cloudinary: {} -> {} (size: {} bytes)", 
                    fileName, audioUrl, audioData.length);
            
            return audioUrl;
            
        } catch (Exception e) {
            log.error("Error uploading TTS audio to Cloudinary for file {} (size: {} bytes): {}", 
                    fileName, audioData != null ? audioData.length : 0, e.getMessage());
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