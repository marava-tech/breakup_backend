package com.breakupstories.service;

import com.breakupstories.model.Story;
import com.breakupstories.model.StoryDataStore;
import com.breakupstories.model.User;
import com.breakupstories.repository.StoryDataStoreRepository;
import com.breakupstories.repository.StoryRepository;
import com.breakupstories.repository.UserRepository;
import com.breakupstories.util.RequestIdGenerator;
import com.breakupstories.exception.AIServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.breakupstories.util.TimestampUtil;

/**
 * Background worker that processes stories with AI services
 * 
 * Workflow:
 * 1. Fetches stories with PROCESSING_PENDING status
 * 2. Calls AI services (transcription, rewrite, paragraph, analysis)
 * 3. Updates processing status and stores AI responses
 * 4. Updates StoryDataStore with AI results
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StoryProcessingWorker {
    
    private final StoryDataStoreRepository storyDataStoreRepository;
    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final RetryableAIService retryableAIService;
    private final StoryStatusService storyStatusService;
    private final AudioGenerationService audioGenerationService;
    
    /**
     * Process stories every 2 minutes
     */
    @Scheduled(fixedRate =  60000) // 2 minutes
    public void processStories() {
        String requestId = RequestIdGenerator.generateRequestId();
        log.info("Starting story processing worker (Request ID: {})", requestId);
        
        try {
            // Fetch stories with PROCESSING_PENDING status, ordered by creation time (oldest first)
            List<StoryDataStore> processingPendingStories = storyDataStoreRepository.findByProcessingStatusOrderByCreatedAtAscLimit(StoryDataStore.ProcessingStatus.PROCESSING_PENDING, 5);
            
            if (processingPendingStories.isEmpty()) {
                log.info("No stories pending processing (Request ID: {})", requestId);
                return;
            }
            
            log.info("Found {} stories pending processing (Request ID: {})", processingPendingStories.size(), requestId);
            
            // Process each story
            for (StoryDataStore dataStore : processingPendingStories) {
                try {
                    processStory(dataStore, requestId);
                } catch (Exception e) {
                    log.error("Error processing story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage(), e);
                    markStoryAsFailed(dataStore, "Processing failed: " + e.getMessage());
                }
            }
            
            log.info("Story processing worker completed (Request ID: {})", requestId);
            
        } catch (Exception e) {
            log.error("Error in story processing worker (Request ID: {}): {}", requestId, e.getMessage(), e);
        }
    }
    
    /**
     * Process a single story with AI services
     */
    private void processStory(StoryDataStore dataStore, String requestId) {
        log.info("Processing story: {} (Request ID: {})", dataStore.getId(), requestId);
        
        try {
            // Get the actual Story entity
            Story story = storyRepository.findById(dataStore.getStoryId())
                    .orElseThrow(() -> new RuntimeException("Story not found: " + dataStore.getStoryId()));
            
            // Get user for context
            User user = userRepository.findById(story.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found: " + story.getUserId()));
            
            // Update status to PROCESSING
            dataStore.setProcessingStartedAt(TimestampUtil.currentLocalDateTime());
            storyStatusService.updateStatusInBothCollections(dataStore.getStoryId(), Story.StoryStatus.PROCESSING);
            
            log.info("Processing story with AI services for story: {} (Request ID: {})", dataStore.getId(), requestId);
            
            // Process with AI services
            processStoryWithAI(story, dataStore, user, requestId);
            
            // Update status to PROCESSED
            dataStore.setProcessingCompletedAt(TimestampUtil.currentLocalDateTime());
            storyStatusService.updateStatusInBothCollections(dataStore.getStoryId(), Story.StoryStatus.PROCESSED);
            
            log.info("Successfully processed story: {} (Request ID: {})", dataStore.getId(), requestId);
            
        } catch (Exception e) {
            log.error("Error processing story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage(), e);
            markStoryAsFailed(dataStore, "Processing failed: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Process story with AI services
     */
    private void processStoryWithAI(Story story, StoryDataStore dataStore, User user, String requestId) {
        log.info("Processing story with AI services for story: {} (Request ID: {})", dataStore.getId(), requestId);
        
        try {
            
            // Check if this is a written story
            boolean isWrittenStory = story.getCreationType() == Story.CreationType.WRITTEN;
            
            if (isWrittenStory) {
                processWrittenStoryWithAI(story, dataStore, requestId);
            } else {
                processAudioStoryWithAI(dataStore, requestId);
            }
            
        } catch (Exception e) {
            log.error("AI processing failed for story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Process written story with AI services (different flow: rewrite first, then generate audio)
     */
    private void processWrittenStoryWithAI(Story story, StoryDataStore dataStore, String requestId) {
        log.info("Processing written story with AI services for story: {} (Request ID: {})", dataStore.getId(), requestId);

         String userLanguage;
         if(ObjectUtils.isNotEmpty(story.getLanguage())){
             userLanguage = story.getLanguage();
         }else{
             userLanguage = dataStore.getLanguage();
         }
        try {
            // Get story text from metadata
            String storyText = dataStore.getUploadMetadata().get("storyText");
            if (storyText == null || storyText.trim().isEmpty()) {
                throw new RuntimeException("Story text not found in metadata for written story: " + dataStore.getId());
            }
            
            // Step 1: Story Rewrite (for written stories, we rewrite the original text)
            try {
                log.info("Starting story rewrite for written story: {} (Request ID: {})", dataStore.getId(), requestId);
                
                String rewrittenStory = retryableAIService.rewriteStory(storyText, userLanguage);
                if(ObjectUtils.isEmpty(rewrittenStory)) throw new RuntimeException("unable to rewrite the story");
                dataStore.setStoryRewriteResponse(com.breakupstories.dto.StoryRewriteResponse.builder()
                        .originalTranscript(storyText)
                        .rewrittenStory(rewrittenStory)
                        .language(userLanguage)
                        .build());
                dataStore.setRewriteCompletedAt(TimestampUtil.currentLocalDateTime());
                storyDataStoreRepository.save(dataStore);
                log.info("Story rewrite completed for written story: {} (Request ID: {})", dataStore.getId(), requestId);
            } catch (AIServiceException e) {
                if ("RATE_LIMIT_EXCEEDED".equals(e.getErrorCode())) {
                    log.warn("AI service is busy (429 error) for written story {} (Request ID: {}). Skipping this story.", 
                            dataStore.getId(), requestId);
                    return; // Skip this story and continue with next one
                } else {
                    log.error("Story rewrite failed for written story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage());
                    dataStore.setRewriteError("Story rewrite failed: " + e.getMessage());
                    storyDataStoreRepository.save(dataStore);
                    throw e; // Stop processing if rewrite fails
                }
            } catch (Exception e) {
                log.error("Story rewrite failed for written story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage());
                dataStore.setRewriteError("Story rewrite failed: " + e.getMessage());
                storyDataStoreRepository.save(dataStore);
                throw e; // Stop processing if rewrite fails
            }
            
            // Step 2: Generate Audio from rewritten story
            try {
                log.info("Starting audio generation for written story: {} (Request ID: {})", dataStore.getId(), requestId);
                
                String rewrittenStory = dataStore.getStoryRewriteResponse().getRewrittenStory();
                String audioUrl = audioGenerationService.generateAudioFromText(rewrittenStory, userLanguage);
                Long duration = audioGenerationService.getEstimatedDuration(rewrittenStory, userLanguage);
                
                // Update story and data store with audio URL
                story.setAudioUrl(audioUrl);
                story.setDuration(duration);
                storyRepository.save(story);
                
                dataStore.setAudioUrl(audioUrl);
                dataStore.setDuration(duration);
                storyDataStoreRepository.save(dataStore);
                
                log.info("Audio generation completed for written story: {} (Request ID: {}) - URL: {}", 
                        dataStore.getId(), requestId, audioUrl);
            } catch (Exception e) {
                log.error("Audio generation failed for written story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage());
                dataStore.setErrorMessage("Audio generation failed: " + e.getMessage());
                storyDataStoreRepository.save(dataStore);
                throw e; // Stop processing if audio generation fails
            }
            
            // Step 3: Paragraph Rewrite
            try {
                log.info("Starting paragraph rewrite for written story: {} (Request ID: {})", dataStore.getId(), requestId);
                dataStore.setParagraphRewriteResponse(retryableAIService.rewriteStoryIntoParagraphs(storyText, userLanguage));
                dataStore.setParagraphCompletedAt(TimestampUtil.currentLocalDateTime());
                storyDataStoreRepository.save(dataStore);
                log.info("Paragraph rewrite completed for written story: {} (Request ID: {})", dataStore.getId(), requestId);
            } catch (AIServiceException e) {
                if ("RATE_LIMIT_EXCEEDED".equals(e.getErrorCode())) {
                    log.warn("AI service is busy (429 error) for written story {} (Request ID: {}). Skipping this story.", 
                            dataStore.getId(), requestId);
                    return; // Skip this story and continue with next one
                } else {
                    log.error("Paragraph rewrite failed for written story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage());
                    dataStore.setParagraphError("Paragraph rewrite failed: " + e.getMessage());
                    storyDataStoreRepository.save(dataStore);
                    throw e; // Stop processing if paragraph rewrite fails
                }
            } catch (Exception e) {
                log.error("Paragraph rewrite failed for written story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage());
                dataStore.setParagraphError("Paragraph rewrite failed: " + e.getMessage());
                storyDataStoreRepository.save(dataStore);
                throw e; // Stop processing if paragraph rewrite fails
            }
            
            // Step 4: Story Analysis
            try {
                log.info("Starting story analysis for written story: {} (Request ID: {})", dataStore.getId(), requestId);
                
                // Check if we have a valid rewritten story
                if (dataStore.getStoryRewriteResponse() == null || 
                    dataStore.getStoryRewriteResponse().getRewrittenStory() == null || 
                    dataStore.getStoryRewriteResponse().getRewrittenStory().trim().isEmpty()) {
                    throw new RuntimeException("No valid rewritten story available for analysis");
                }
                
                String rewrittenStory = dataStore.getStoryRewriteResponse().getRewrittenStory();
                dataStore.setStoryAnalysisResponse(retryableAIService.analyzeStory(rewrittenStory, userLanguage));
                dataStore.setAnalysisCompletedAt(TimestampUtil.currentLocalDateTime());
                storyDataStoreRepository.save(dataStore);
                log.info("Story analysis completed for written story: {} (Request ID: {})", dataStore.getId(), requestId);
            } catch (AIServiceException e) {
                if ("RATE_LIMIT_EXCEEDED".equals(e.getErrorCode())) {
                    log.warn("AI service is busy (429 error) for written story {} (Request ID: {}). Skipping this story.", 
                            dataStore.getId(), requestId);
                    return; // Skip this story and continue with next one
                } else {
                    log.error("Story analysis failed for written story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage());
                    dataStore.setAnalysisError("Story analysis failed: " + e.getMessage());
                    storyDataStoreRepository.save(dataStore);
                    throw e; // Stop processing if analysis fails
                }
            } catch (Exception e) {
                log.error("Story analysis failed for written story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage());
                dataStore.setAnalysisError("Story analysis failed: " + e.getMessage());
                storyDataStoreRepository.save(dataStore);
                throw e; // Stop processing if analysis fails
            }
            
            // Update metadata with AI results
            updateDataStoreMetadata(dataStore, requestId);
            
        } catch (Exception e) {
            log.error("Written story AI processing failed for story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Process audio story with AI services (original flow: transcription first, then rewrite)
     */
    private void processAudioStoryWithAI( StoryDataStore dataStore,  String requestId) {
        log.info("Processing audio story with AI services for story: {} (Request ID: {})", dataStore.getId(), requestId);


        var userLanguage = dataStore.getLanguage();

        // Step 1: Transcription

            try {
                log.info("Starting transcription for story: {} (Request ID: {})", dataStore.getId(), requestId);
                dataStore.setTranscriptionResponse(retryableAIService.transcribeAudio(dataStore.getAudioUrl(), userLanguage));
                dataStore.setTranscriptionCompletedAt(TimestampUtil.currentLocalDateTime());
                storyDataStoreRepository.save(dataStore);
                log.info("Transcription completed for story: {} (Request ID: {})", dataStore.getId(), requestId);
            } catch (AIServiceException e) {
                if ("RATE_LIMIT_EXCEEDED".equals(e.getErrorCode())) {
                    log.warn("AI service is busy (429 error) for story {} (Request ID: {}). Skipping this story.", 
                            dataStore.getId(), requestId);
                    // Don't update story status, just skip to next story
                    return; // Skip this story and continue with next one
                } else {
                    log.error("Transcription failed for story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage());
                    dataStore.setTranscriptionError("Transcription failed: " + e.getMessage());
                    storyDataStoreRepository.save(dataStore);
                    throw e; // Stop processing if transcription fails
                }
            } catch (Exception e) {
                log.error("Transcription failed for story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage());
                dataStore.setTranscriptionError("Transcription failed: " + e.getMessage());
                storyDataStoreRepository.save(dataStore);
                throw e; // Stop processing if transcription fails
            }
            
            // Step 2: Story Rewrite
            try {
                log.info("Starting story rewrite for story: {} (Request ID: {})", dataStore.getId(), requestId);
                
                // Debug: Check transcription response
                if (dataStore.getTranscriptionResponse() == null) {
                    throw new RuntimeException("Transcription response is null");
                }
                
                String transcript = dataStore.getTranscriptionResponse().getTranscript();
                log.info("Transcript for rewrite - Length: {}, Content: {}", 
                        transcript != null ? transcript.length() : 0, 
                        transcript != null ? transcript.substring(0, Math.min(100, transcript.length())) : "null");
                
                String rewrittenStory = retryableAIService.rewriteStory(transcript, userLanguage);
                if(ObjectUtils.isEmpty(rewrittenStory)) throw new RuntimeException("unable to rewrite the story");
                dataStore.setStoryRewriteResponse(com.breakupstories.dto.StoryRewriteResponse.builder()
                        .originalTranscript(dataStore.getTranscriptionResponse().getTranscript())
                        .rewrittenStory(rewrittenStory)
                        .language(userLanguage)
                        .build());
                dataStore.setRewriteCompletedAt(TimestampUtil.currentLocalDateTime());
                storyDataStoreRepository.save(dataStore);
                log.info("Story rewrite completed for story: {} (Request ID: {})", dataStore.getId(), requestId);
            } catch (AIServiceException e) {
                if ("RATE_LIMIT_EXCEEDED".equals(e.getErrorCode())) {
                    log.warn("AI service is busy (429 error) for story {} (Request ID: {}). Skipping this story.", 
                            dataStore.getId(), requestId);
                    return; // Skip this story and continue with next one
                } else {
                    log.error("Story rewrite failed for story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage());
                    dataStore.setRewriteError("Story rewrite failed: " + e.getMessage());
                    storyDataStoreRepository.save(dataStore);
                    throw e; // Stop processing if rewrite fails
                }
            } catch (Exception e) {
                log.error("Story rewrite failed for story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage());
                dataStore.setRewriteError("Story rewrite failed: " + e.getMessage());
                storyDataStoreRepository.save(dataStore);
                throw e; // Stop processing if rewrite fails
            }
            
            // Step 3: Paragraph Rewrite
            try {
                log.info("Starting paragraph rewrite for story: {} (Request ID: {})", dataStore.getId(), requestId);
                dataStore.setParagraphRewriteResponse(retryableAIService.rewriteStoryIntoParagraphs(dataStore.getTranscriptionResponse().getTranscript(), userLanguage));
                dataStore.setParagraphCompletedAt(TimestampUtil.currentLocalDateTime());
                storyDataStoreRepository.save(dataStore);
                log.info("Paragraph rewrite completed for story: {} (Request ID: {})", dataStore.getId(), requestId);
            } catch (AIServiceException e) {
                if ("RATE_LIMIT_EXCEEDED".equals(e.getErrorCode())) {
                    log.warn("AI service is busy (429 error) for story {} (Request ID: {}). Skipping this story.", 
                            dataStore.getId(), requestId);
                    return; // Skip this story and continue with next one
                } else {
                    log.error("Paragraph rewrite failed for story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage());
                    dataStore.setParagraphError("Paragraph rewrite failed: " + e.getMessage());
                    storyDataStoreRepository.save(dataStore);
                    throw e; // Stop processing if paragraph rewrite fails
                }
            } catch (Exception e) {
                log.error("Paragraph rewrite failed for story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage());
                dataStore.setParagraphError("Paragraph rewrite failed: " + e.getMessage());
                storyDataStoreRepository.save(dataStore);
                throw e; // Stop processing if paragraph rewrite fails
            }
            
            // Step 4: Story Analysis
            try {
                log.info("Starting story analysis for story: {} (Request ID: {})", dataStore.getId(), requestId);
                
                // Check if we have a valid rewritten story
                if (dataStore.getStoryRewriteResponse() == null || 
                    dataStore.getStoryRewriteResponse().getRewrittenStory() == null || 
                    dataStore.getStoryRewriteResponse().getRewrittenStory().trim().isEmpty()) {
                    throw new RuntimeException("No valid rewritten story available for analysis");
                }
                
                String rewrittenStory = dataStore.getStoryRewriteResponse().getRewrittenStory();
                dataStore.setStoryAnalysisResponse(retryableAIService.analyzeStory(rewrittenStory, userLanguage));
                dataStore.setAnalysisCompletedAt(TimestampUtil.currentLocalDateTime());
                storyDataStoreRepository.save(dataStore);
                log.info("Story analysis completed for story: {} (Request ID: {})", dataStore.getId(), requestId);
            } catch (AIServiceException e) {
                if ("RATE_LIMIT_EXCEEDED".equals(e.getErrorCode())) {
                    log.warn("AI service is busy (429 error) for story {} (Request ID: {}). Skipping this story.", 
                            dataStore.getId(), requestId);
                    return; // Skip this story and continue with next one
                } else {
                    log.error("Story analysis failed for story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage());
                    dataStore.setAnalysisError("Story analysis failed: " + e.getMessage());
                    storyDataStoreRepository.save(dataStore);
                    throw e; // Stop processing if analysis fails
                }
            } catch (Exception e) {
                log.error("Story analysis failed for story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage());
                dataStore.setAnalysisError("Story analysis failed: " + e.getMessage());
                storyDataStoreRepository.save(dataStore);
                throw e; // Stop processing if analysis fails
            }
            
            // Update metadata with AI results
            updateDataStoreMetadata(dataStore, requestId);
    }
    

    
    /**
     * Update data store metadata with AI processing results
     */
    private void updateDataStoreMetadata(StoryDataStore dataStore, String requestId) {
        log.info("Updating metadata for story: {} (Request ID: {})", dataStore.getId(), requestId);
        
        try {
            // Update title from rewrite response
            if (dataStore.getStoryRewriteResponse() != null && dataStore.getStoryRewriteResponse().getRewrittenStory() != null) {
                // Extract title from the first sentence or use a default
                String rewrittenStory = dataStore.getStoryRewriteResponse().getRewrittenStory();
                String title = rewrittenStory.split("\\.")[0].trim();
                dataStore.setTitle(title);
            }
            
            // Update search text
            dataStore.setSearchText(dataStore.generateSearchText());
            
            // Save updated data store
            storyDataStoreRepository.save(dataStore);
            
            log.info("Metadata updated for story: {} (Request ID: {})", dataStore.getId(), requestId);
            
        } catch (Exception e) {
            log.error("Error updating metadata for story {} (Request ID: {}): {}", dataStore.getId(), requestId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Language code to full name mapping
     */
    private static final Map<String, String> LANGUAGE_MAP = new HashMap<>();
    
    static {
        // South Indian Languages
        LANGUAGE_MAP.put("te", "telugu");
        LANGUAGE_MAP.put("ta", "tamil");
        LANGUAGE_MAP.put("ka", "kannada");
        LANGUAGE_MAP.put("ml", "malayalam");
        
        // Hindi and English
        LANGUAGE_MAP.put("hi", "hindi");
        LANGUAGE_MAP.put("en", "english");
        
        // Additional Indian Languages
        LANGUAGE_MAP.put("bn", "bengali");
        LANGUAGE_MAP.put("gu", "gujarati");
        LANGUAGE_MAP.put("pa", "punjabi");
        LANGUAGE_MAP.put("mr", "marathi");
        LANGUAGE_MAP.put("or", "odia");
        LANGUAGE_MAP.put("as", "assamese");
        LANGUAGE_MAP.put("kn", "kannada"); // Alternative code
        LANGUAGE_MAP.put("ml", "malayalam"); // Alternative code
    }
    
    /**
     * Get user language preference and return full language name
     */
    private String getUserLanguage(User user) {
        try {
            String userLanguage = user.getPreferredStoryLanguage();
            if (userLanguage != null && !userLanguage.trim().isEmpty()) {
                String languageCode = userLanguage.toLowerCase().trim();
                
                // If it's a 2-character code, try to get the full name
                if (languageCode.length() == 2) {
                    String fullLanguageName = LANGUAGE_MAP.get(languageCode);
                    if (fullLanguageName != null) {
                        return fullLanguageName;
                    }
                }
                
                // If it's already a full name or not found in map, return as is
                return languageCode;
            }
        } catch (Exception e) {
            log.warn("Error getting user language for userId: {}, using default", user.getId(), e);
        }
        
        // Default language
        return "english";
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