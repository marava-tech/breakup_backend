package com.breakupstories.service;

import com.breakupstories.model.Story;
import com.breakupstories.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncStoryProcessingService {
    
    private final UploadService uploadService;
    private final MockAIService mockAIService;
    private final StoryRepository storyRepository;

    @Async
    public void processStoryAsync(Story story, byte[] audioFileContent, String originalFilename, String contentType, String latitude, String longitude, String requestId) {
        try {
            // Create a custom MultipartFile from the byte array
            MultipartFile audioFile = new MultipartFile() {
                @Override
                public String getName() {
                    return "audio";
                }

                @Override
                public String getOriginalFilename() {
                    return originalFilename;
                }

                @Override
                public String getContentType() {
                    return contentType;
                }

                @Override
                public boolean isEmpty() {
                    return audioFileContent.length == 0;
                }

                @Override
                public long getSize() {
                    return audioFileContent.length;
                }

                @Override
                public byte[] getBytes() throws IOException {
                    return audioFileContent;
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    return new ByteArrayInputStream(audioFileContent);
                }

                @Override
                public void transferTo(File dest) throws IOException, IllegalStateException {
                    try (FileOutputStream fos = new FileOutputStream(dest)) {
                        fos.write(audioFileContent);
                    }
                }
            };

            String audioUrl = uploadService.uploadSingleFile(audioFile);
            log.info("Audio upload completed for story {}: {} [RequestID: {}]", story.getId(), audioUrl, requestId);
            
            // Update story with audio URL
            story.setAudioUrl(audioUrl);
            story.setTitle("Processing..."); // Update title to indicate AI processing
            storyRepository.save(story);
            log.info("Story updated with audio URL: {} for story: {} [RequestID: {}]", audioUrl, story.getId(), requestId);
            
            // Step 2: Start async AI processing with location coordinates
            mockAIService.processStoryWithAIAsync(story.getId(), latitude, longitude);
            log.info("AI processing started for story: {} with location data [RequestID: {}]", story.getId(), requestId);
            
        } catch (Exception e) {
            log.error("Async processing failed for story {}: {} [RequestID: {}]", story.getId(), e.getMessage(), requestId, e);
            // Update story status to REJECTED if processing fails
            var rejectedReasons = story.getRejectionReasons();
            if(rejectedReasons==null){
                rejectedReasons = new ArrayList<>();
            }
            rejectedReasons.add(e.getMessage());
            story.setRejectionReasons(rejectedReasons);
            updateStoryStatus(story.getId(), Story.StoryStatus.REJECTED);
        }
    }

    private void updateStoryStatus(String storyId, Story.StoryStatus status) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found: " + storyId));
        
        story.setStatus(status);
        storyRepository.save(story);
        log.info("Story status updated to {}: {}", status, storyId);
    }
} 