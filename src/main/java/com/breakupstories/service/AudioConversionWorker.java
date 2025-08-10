package com.breakupstories.service;




import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;


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
    
    private final Cloudinary cloudinary;
    
    /**
     * Process TTS audio uploads every 7 minutes
     */
  

    

    
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
    

} 