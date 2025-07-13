package com.breakupstories.service;

import com.breakupstories.model.Story;
import com.breakupstories.model.StoryDataStore;
import com.breakupstories.model.User;
import com.breakupstories.repository.StoryDataStoreRepository;
import com.breakupstories.repository.StoryRepository;
import com.breakupstories.repository.UserRepository;
import com.breakupstories.util.RequestIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Background worker for handling story uploads
 * 
 * Workflow:
 * 1. Fetches stories with UPLOAD_PENDING status
 * 2. Uploads audio file to cloud storage
 * 3. Updates audio URL and duration in data store
 * 4. Changes status to PROCESSING_PENDING
 * 5. Creates StoryDataStore
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StoryUploadWorker {
    
    private final StoryDataStoreRepository storyDataStoreRepository;
    private final UploadService uploadService;
    private final StoryStatusService storyStatusService;
    
    /**
     * Process uploads every 1 minute
     */
    @Scheduled(fixedRate = 60000) // 1 minute
    public void processUploads() {
        String requestId = RequestIdGenerator.generateRequestId();
        log.info("Starting story upload worker (Request ID: {})", requestId);
        
        try {
            // Fetch stories with UPLOAD_PENDING status, ordered by creation time (oldest first)
            List<StoryDataStore> uploadPendingStories = storyDataStoreRepository.findByProcessingStatusOrderByCreatedAtAscLimit(StoryDataStore.ProcessingStatus.UPLOAD_PENDING, 5);
            
            if (uploadPendingStories.isEmpty()) {
                log.info("No stories pending upload (Request ID: {})", requestId);
                return;
            }
            
            log.info("Found {} stories pending upload (Request ID: {})", uploadPendingStories.size(), requestId);
            
            // Process each story
            for (StoryDataStore dataStore : uploadPendingStories) {
                try {
                    processUploadPendingStory(dataStore, requestId);
                } catch (Exception e) {
                    log.error("Error uploading story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage(), e);
                    markStoryAsFailed(dataStore, "Upload failed: " + e.getMessage());
                }
            }
            
            log.info("Story upload worker completed (Request ID: {})", requestId);
            
        } catch (Exception e) {
            log.error("Error in story upload worker (Request ID: {}): {}", requestId, e.getMessage(), e);
        }
    }
    
    /**
     * Process a single upload pending story
     */
    private void processUploadPendingStory(StoryDataStore dataStore, String requestId) {
        log.info("Processing upload for story: {} (Request ID: {})", dataStore.getId(), requestId);
        
        try {
            // Update status to UPLOADING using StoryStatusService
            storyStatusService.updateStatusInBothCollections(dataStore.getStoryId(), Story.StoryStatus.UPLOADING);
            
            // Get audio file from metadata
            MultipartFile audioFile = getAudioFileFromMetadata(dataStore);
            if (audioFile == null || audioFile.isEmpty()) {
                throw new RuntimeException("Audio file not found in metadata for story: " + dataStore.getId());
            }
            
            // Validate audio file
            if (audioFile.getSize() == 0) {
                throw new RuntimeException("Audio file is empty for story: " + dataStore.getId());
            }
            
            String contentType = audioFile.getContentType();
            if (contentType == null || !contentType.startsWith("audio/")) {
                log.warn("Audio file has unexpected content type: {} for story: {}", contentType, dataStore.getId());
                // Continue anyway as Cloudinary might handle it
            }
            
            // Validate file format
            if (!isValidAudioFormat(audioFile)) {
                throw new RuntimeException("Unsupported audio format for story: " + dataStore.getId() + ". Please use MP3, WAV, M4A, or other common audio formats.");
            }
            
            log.info("Uploading audio file for story: {} - filename: {}, size: {} bytes, content-type: {} (Request ID: {})", 
                    dataStore.getId(), audioFile.getOriginalFilename(), audioFile.getSize(), audioFile.getContentType(), requestId);
            
            // Upload audio file
            String audioUrl = uploadService.uploadSingleFile(audioFile);
            Long duration = getAudioDuration(audioFile);
            
            // Update data store with audio URL and duration
            dataStore.setAudioUrl(audioUrl);
            dataStore.setDuration(duration);

            //remove the byte array.
            dataStore.getUploadMetadata().remove("audioFileData");
            storyDataStoreRepository.save(dataStore); // Save the updated data store
            
            // Update status to PROCESSING_PENDING using StoryStatusService
            storyStatusService.updateStatusInBothCollections(dataStore.getStoryId(), Story.StoryStatus.PROCESSING_PENDING);
            
            log.info("StoryDataStore updated for story: {} with audio URL: {} and duration: {} ms (Request ID: {})",
                    dataStore.getId(), audioUrl, duration, requestId);
            
        } catch (Exception e) {
            log.error("Error uploading story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage(), e);
            markStoryAsFailed(dataStore, "Upload failed: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Get audio file from metadata
     */
    private MultipartFile getAudioFileFromMetadata(StoryDataStore dataStore) {
        try {
            Map<String, String> uploadMetadata = dataStore.getUploadMetadata();
            if (uploadMetadata == null) {
                log.error("Upload metadata is null for story: {}", dataStore.getId());
                return null;
            }
            
            // Extract file information from metadata
            String audioFileData = uploadMetadata.get("audioFileData");
            String audioFileName = uploadMetadata.get("audioFileName");
            String audioContentType = uploadMetadata.get("audioContentType");
            String audioFileSize = uploadMetadata.get("audioFileSize");
            
            if (audioFileData == null || audioFileData.isEmpty()) {
                log.error("Audio file data not found in metadata for story: {}", dataStore.getId());
                return null;
            }
            
            // Decode base64 data
            byte[] fileBytes = java.util.Base64.getDecoder().decode(audioFileData);
            long fileSize = audioFileSize != null ? Long.parseLong(audioFileSize) : fileBytes.length;
            
            log.info("Reconstructed audio file for story: {} - filename: {}, size: {} bytes, content-type: {}", 
                    dataStore.getId(), audioFileName, fileSize, audioContentType);
            
            // Create MultipartFile from the decoded data
            return new MultipartFile() {
                @Override
                public String getName() {
                    return "audio";
                }
                
                @Override
                public String getOriginalFilename() {
                    return audioFileName != null ? audioFileName : "story_audio.wav";
                }
                
                @Override
                public String getContentType() {
                    return audioContentType != null ? audioContentType : "audio/wav";
                }
                
                @Override
                public boolean isEmpty() {
                    return fileBytes.length == 0;
                }
                
                @Override
                public long getSize() {
                    return fileSize;
                }
                
                @Override
                public byte[] getBytes() throws IOException {
                    return fileBytes;
                }
                
                @Override
                public InputStream getInputStream() throws IOException {
                    return new ByteArrayInputStream(fileBytes);
                }
                
                @Override
                public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
                        fos.write(fileBytes);
                    }
                }
            };
            
        } catch (Exception e) {
            log.error("Error getting audio file from metadata for story: {}", dataStore.getId(), e);
            return null;
        }
    }
    
    /**
     * Get audio duration (mock implementation)
     */
    private Long getAudioDuration(MultipartFile audioFile) {
        try {
            // In a real implementation, you'd analyze the audio file to get its duration
            // For now, return a mock duration
            return 30000L; // 30 seconds
        } catch (Exception e) {
            log.warn("Error getting audio duration, using default: {}", e.getMessage());
            return 30000L; // Default 30 seconds
        }
    }
    
    /**
     * Validate if the audio file format is supported
     */
    private boolean isValidAudioFormat(MultipartFile audioFile) {
        String contentType = audioFile.getContentType();
        String filename = audioFile.getOriginalFilename();
        
        if (contentType != null) {
            // Check content type
            if (contentType.startsWith("audio/")) {
                return true;
            }
        }
        
        if (filename != null) {
            // Check file extension
            String extension = filename.toLowerCase();
            return extension.endsWith(".mp3") || 
                   extension.endsWith(".wav") || 
                   extension.endsWith(".m4a") || 
                   extension.endsWith(".aac") || 
                   extension.endsWith(".ogg") || 
                   extension.endsWith(".flac");
        }
        
        return false;
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
} 