package com.breakupstories.service;

import com.breakupstories.dto.*;
import com.breakupstories.model.StoryDataStore;
import com.breakupstories.repository.StoryDataStoreRepository;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class StoryProcessingService {

    private static final Logger log = LoggerFactory.getLogger(StoryProcessingService.class);
    private final StoryDataStoreRepository storyDataStoreRepository;
    private final TranscriptionService transcriptionService;
    private final StoryRewriteService storyRewriteService;
    private final ThumbnailGenerationService thumbnailGenerationService;
    private final TTSService ttsService;
    private final UserService userService;
    private final PromptConfigurationService promptConfig;

    public StoryProcessingService(StoryDataStoreRepository storyDataStoreRepository,
            TranscriptionService transcriptionService,
            StoryRewriteService storyRewriteService,
            ThumbnailGenerationService thumbnailGenerationService, TTSService ttsService,
            UserService userService, PromptConfigurationService promptConfig) {
        this.storyDataStoreRepository = storyDataStoreRepository;
        this.transcriptionService = transcriptionService;
        this.storyRewriteService = storyRewriteService;
        this.thumbnailGenerationService = thumbnailGenerationService;
        this.ttsService = ttsService;
        this.userService = userService;
        this.promptConfig = promptConfig;
    }

    public List<StoryDataStore> getStoriesToProcess() {
        return storyDataStoreRepository.findByProcessingStatus(StoryDataStore.ProcessingStatus.PROCESSING_PENDING);
    }

    /**
     * Fetch up to 10 pending stories and update their status to PROCESSING in bulk
     */
    public List<StoryDataStore> fetchAndMarkProcessingStories() {
        List<StoryDataStore> pendingStories = storyDataStoreRepository
                .findByProcessingStatus(StoryDataStore.ProcessingStatus.PROCESSING_PENDING);
        List<StoryDataStore> storiesToProcess = pendingStories.stream().limit(10).toList();
        // Bulk update status to PROCESSING
        for (StoryDataStore story : storiesToProcess) {
            story.setProcessingStatus(StoryDataStore.ProcessingStatus.PROCESSING);
            story.setProcessingStartedAt(LocalDateTime.now());
        }
        storyDataStoreRepository.saveAll(storiesToProcess);
        return storiesToProcess;
    }

    /**
     * Process a single story
     */
    public void processStory(StoryDataStore story) {
        try {
            log.info("Processing single story: {}", story.getStoryId());

            // Check if story is already in a terminal state
            if (StoryDataStore.isTerminalStatus(story.getProcessingStatus())) {
                log.info("Story {} is already in terminal state: {}. Skipping processing.",
                        story.getStoryId(), story.getProcessingStatus());
                return;
            }

            // Set status to PROCESSING and save immediately (only if not already
            // PROCESSING)
            if (story.getProcessingStatus() != StoryDataStore.ProcessingStatus.PROCESSING) {
                story.setProcessingStatus(StoryDataStore.ProcessingStatus.PROCESSING);
                story.setProcessingStartedAt(LocalDateTime.now());
                storyDataStoreRepository.save(story);
                log.info("Story {} - Status updated to PROCESSING and saved to database", story.getStoryId());
            } else {
                log.info("Story {} - Already in PROCESSING status, continuing with workflow", story.getStoryId());
            }

            String creationType = getCreationType(story);
            log.info("Story creation type: {}", creationType);

            if ("UPLOADED".equals(creationType)) {
                processUploadedStoryInMemory(story);
                // Only set to COMPLETED if not already REJECTED
                if (story.getProcessingStatus() != StoryDataStore.ProcessingStatus.REJECTED) {
                    story.setProcessingStatus(StoryDataStore.ProcessingStatus.COMPLETED);
                }
            } else if ("WRITTEN".equals(creationType)) {
                processWrittenStoryInMemory(story);
                // Only set to COMPLETED if not already REJECTED
                if (story.getProcessingStatus() != StoryDataStore.ProcessingStatus.REJECTED) {
                    story.setProcessingStatus(StoryDataStore.ProcessingStatus.COMPLETED);
                }
            } else {
                throw new IllegalArgumentException("Unknown creation type: " + creationType);
            }

            story.setProcessingCompletedAt(LocalDateTime.now());
            storyDataStoreRepository.save(story);

            if (story.getProcessingStatus() == StoryDataStore.ProcessingStatus.REJECTED) {
                log.info("Story {} was rejected and processing stopped", story.getStoryId());
            } else {
                log.info("Successfully completed processing for story: {}", story.getStoryId());
            }

        } catch (Exception e) {
            log.error("Error processing story: {}", story.getStoryId(), e);
            handleProcessingError(story, e);
            storyDataStoreRepository.save(story);
        }
    }

    /**
     * Check if transcription step is already completed successfully
     */
    private boolean isTranscriptionCompleted(StoryDataStore story) {
        return story.getTranscriptionResponse() != null &&
                story.getTranscriptionResponse().getTranscription() != null &&
                !story.getTranscriptionResponse().getTranscription().trim().isEmpty() &&
                story.getTranscriptionError() == null;
    }

    /**
     * Check if story rewrite step is already completed successfully
     */
    private boolean isStoryRewriteCompleted(StoryDataStore story) {
        return story.getStoryRewriteResponse() != null &&
                story.getStoryRewriteResponse().getRewrittenText() != null &&
                !story.getStoryRewriteResponse().getRewrittenText().trim().isEmpty() &&
                story.getRewriteError() == null;
    }

    /**
     * Check if story analysis step is already completed successfully
     */
    private boolean isStoryAnalysisCompleted(StoryDataStore story) {
        return story.getStoryAnalysisResponse() != null &&
                story.getStoryAnalysisResponse().getAnalysis() != null &&
                story.getAnalysisError() == null;
    }

    /**
     * Check if paragraph rewrite step is already completed successfully
     */
    private boolean isParagraphRewriteCompleted(StoryDataStore story) {
        return story.getParagraphRewriteResponse() != null &&
                story.getParagraphRewriteResponse().getParagraphs() != null &&
                !story.getParagraphRewriteResponse().getParagraphs().isEmpty() &&
                story.getParagraphError() == null;
    }

    /**
     * Check if visual prompts step is already completed successfully
     */
    private boolean isVisualPromptsCompleted(StoryDataStore story) {
        return story.getVisualPromptResponse() != null &&
                story.getVisualPromptResponse().getVisualPrompts() != null &&
                !story.getVisualPromptResponse().getVisualPrompts().isEmpty() &&
                story.getVisualPromptError() == null;
    }

    /**
     * Check if images step is already completed successfully
     */
    private boolean isImagesCompleted(StoryDataStore story) {
        return story.getImagesResponse() != null &&
                story.getImagesResponse().getStoryImageUrls() != null &&
                !story.getImagesResponse().getStoryImageUrls().isEmpty();
    }

    /**
     * Check if thumbnail step is already completed successfully
     */
    private boolean isThumbnailCompleted(StoryDataStore story) {
        return story.getImagesResponse() != null &&
                story.getImagesResponse().getThumbnailImageUrl() != null &&
                !story.getImagesResponse().getThumbnailImageUrl().trim().isEmpty();
    }

    /**
     * Get creation type from story upload metadata
     */
    private String getCreationType(StoryDataStore story) {
        // Check uploadMetadata for creationType
        if (story.getUploadMetadata() != null && story.getUploadMetadata().containsKey("creationType")) {
            return story.getUploadMetadata().get("creationType");
        }

        // Fallback: assume UPLOADED if there's transcription, otherwise WRITTEN
        if (story.getTranscriptionResponse() != null &&
                story.getTranscriptionResponse().getTranscription() != null) {
            return promptConfig.getPrompt("story_creation_type_uploaded");
        }
        return promptConfig.getPrompt("story_creation_type_written");
    }

    /**
     * Handle processing errors
     */
    private void handleProcessingError(StoryDataStore story, Exception e) {
        // Preserve REJECTED status if it's already set (for invalid stories)
        if (story.getProcessingStatus() != StoryDataStore.ProcessingStatus.REJECTED) {
            story.setProcessingStatus(StoryDataStore.ProcessingStatus.FAILED);
        }
        story.setErrorMessage(e.getMessage());
        if (story.getErrors() == null) {
            story.setErrors(new ArrayList<>());
        }
        story.getErrors().add(e.getMessage());
    }

    /**
     * In-memory processing for UPLOADED stories (audio files)
     * Flow: Transcribe → Rewrite → Analyze → Validate → Paragraph → Visual → Images
     * → Thumbnail
     */
    private void processUploadedStoryInMemory(StoryDataStore story) throws Exception {
        log.info("Story {} - Starting UPLOADED story processing", story.getStoryId());

        // Step 1: Transcribe the audio (REQUIRED) - skip if already completed
        if (!isTranscriptionCompleted(story)) {
            log.info("Story {} - Step 1: Transcribing audio", story.getStoryId());
            transcriptionService.processTranscription(story);
            if (story.getTranscriptionResponse() == null) {
                log.error("Story {} - Step 1: Transcription failed - no transcription response generated",
                        story.getStoryId());
                story.setProcessingStatus(StoryDataStore.ProcessingStatus.FAILED);
                story.setErrorMessage("Transcription failed - no transcription response generated");
                storyDataStoreRepository.save(story);
                throw new RuntimeException("Transcription failed - no transcription response generated");
            }
        } else {
            log.info("Story {} - Step 1: Transcription already completed, skipping", story.getStoryId());
        }

        // Step 2: Rewrite the story (REQUIRED) - skip if already completed
        if (!isStoryRewriteCompleted(story)) {
            log.info("Story {} - Step 2: Rewriting story", story.getStoryId());
            storyRewriteService.processStoryRewrite(story);
            if (story.getStoryRewriteResponse() == null) {
                log.error("Story {} - Step 2: Story rewrite failed - no rewrite response generated",
                        story.getStoryId());
                story.setProcessingStatus(StoryDataStore.ProcessingStatus.FAILED);
                story.setErrorMessage("Story rewrite failed - no rewrite response generated");
                storyDataStoreRepository.save(story);
                throw new RuntimeException("Story rewrite failed - no rewrite response generated");
            }
        } else {
            log.info("Story {} - Step 2: Story rewrite already completed, skipping", story.getStoryId());
        }

        // Step 3: Analyze the story - skip if already completed
        if (!isStoryAnalysisCompleted(story)) {
            log.info("Story {} - Step 3: Analyzing story", story.getStoryId());
            storyRewriteService.processStoryAnalysis(story);
        } else {
            log.info("Story {} - Step 3: Story analysis already completed, skipping", story.getStoryId());
        }

        // Step 4: Validate story - stop and mark rejected if not valid
        if (!isStoryValid(story)) {
            String errorMessage = getInvalidStoryErrorMessage(story);
            log.error("Story {} - Step 4: Story validation failed - {}", story.getStoryId(), errorMessage);
            story.setProcessingStatus(StoryDataStore.ProcessingStatus.REJECTED);
            story.setErrorMessage(errorMessage);
            if (story.getErrors() == null) {
                story.setErrors(new ArrayList<>());
            }
            story.getErrors().add(errorMessage);
            storyDataStoreRepository.save(story);
            return; // Exit processing without throwing exception
        }

        // Step 5: Paragraph rewrite - skip if already completed
        if (!isParagraphRewriteCompleted(story)) {
            log.info("Story {} - Step 5: Rewriting paragraphs", story.getStoryId());
            storyRewriteService.processParagraphRewrite(story);
        } else {
            log.info("Story {} - Step 5: Paragraph rewrite already completed, skipping", story.getStoryId());
        }

        // Step 6: Visual prompts - skip if already completed
        if (!isVisualPromptsCompleted(story)) {
            log.info("Story {} - Step 6: Generating visual prompts", story.getStoryId());
            storyRewriteService.processVisualPrompts(story);
        } else {
            log.info("Story {} - Step 6: Visual prompts already completed, skipping", story.getStoryId());
        }

        // Step 7: Story image generation - skip if already completed
        if (!isImagesCompleted(story)) {
            log.info("Story {} - Step 7: Generating story images", story.getStoryId());
            generateStoryImages(story);
        } else {
            log.info("Story {} - Step 7: Story images already completed, skipping", story.getStoryId());
        }

        // Step 8: Thumbnail generation - skip if already completed
        if (!isThumbnailCompleted(story)) {
            log.info("Story {} - Step 8: Generating thumbnail", story.getStoryId());
            boolean thumbnailGenerated = thumbnailGenerationService.generateThumbnail(story);
            if (thumbnailGenerated) {
                storyDataStoreRepository.save(story);
                log.info("Story {} - Step 8: Thumbnail generated successfully", story.getStoryId());
            } else {
                log.warn("Story {} - Step 8: Thumbnail generation failed, continuing without thumbnail",
                        story.getStoryId());
            }
        } else {
            log.info("Story {} - Step 8: Thumbnail already completed, skipping", story.getStoryId());
        }

        log.info("Story {} - UPLOADED story processing completed successfully", story.getStoryId());
    }

    /**
     * In-memory processing for WRITTEN stories (text input)
     * Flow: Create Transcription from UploadMetadata → Rewrite → Analyze → Validate
     * → Audio → Paragraph → Visual → Images → Thumbnail
     */
    private void processWrittenStoryInMemory(StoryDataStore story) throws Exception {
        log.info("Story {} - Starting WRITTEN story processing", story.getStoryId());

        // Step 1: Create transcription response from upload metadata (REQUIRED) - skip
        // if already completed
        if (!isTranscriptionCompleted(story)) {
            log.info("Story {} - Step 1: Creating transcription response from upload metadata", story.getStoryId());

            if (story.getUploadMetadata() == null) {
                log.error("Story {} - Step 1: Upload metadata is missing", story.getStoryId());
                story.setProcessingStatus(StoryDataStore.ProcessingStatus.FAILED);
                story.setErrorMessage("Upload metadata is required for written stories but is missing");
                storyDataStoreRepository.save(story);
                throw new RuntimeException("Upload metadata is required for written stories but is missing");
            }

            String storyText = story.getUploadMetadata().get("storyText");
            String storyLanguage = story.getUploadMetadata().get("storyLanguage");

            if (storyText == null || storyText.trim().isEmpty()) {
                log.error("Story {} - Step 1: Story text is missing or empty in upload metadata", story.getStoryId());
                story.setProcessingStatus(StoryDataStore.ProcessingStatus.FAILED);
                story.setErrorMessage(
                        "Story text is required for written stories but is missing or empty in upload metadata");
                storyDataStoreRepository.save(story);
                throw new RuntimeException(
                        "Story text is required for written stories but is missing or empty in upload metadata");
            }

            // Create TranscriptionResponse from upload metadata
            TranscriptionResponse transcriptionResponse = TranscriptionResponse.builder()
                    .transcription(storyText.trim())
                    .language(storyLanguage != null ? storyLanguage : "en")
                    .confidence(1.0) // High confidence since it's user-provided text
                    .status("SUCCESS")
                    .build();

            story.setTranscriptionResponse(transcriptionResponse);
            story.setTranscriptionCompletedAt(java.time.LocalDateTime.now());
            storyDataStoreRepository.save(story);

            log.info(
                    "Story {} - Step 1: Successfully created transcription response from upload metadata ({} chars, language: {})",
                    story.getStoryId(), storyText.length(), storyLanguage != null ? storyLanguage : "en");
        } else {
            log.info("Story {} - Step 1: Transcription already completed, skipping", story.getStoryId());
        }

        // Step 2: Story rewriting (REQUIRED) - skip if already completed
        if (!isStoryRewriteCompleted(story)) {
            log.info("Story {} - Step 2: Rewriting story", story.getStoryId());
            storyRewriteService.processStoryRewrite(story);
            if (story.getStoryRewriteResponse() == null) {
                log.error("Story {} - Step 2: Story rewrite failed - no rewrite response generated",
                        story.getStoryId());
                story.setProcessingStatus(StoryDataStore.ProcessingStatus.FAILED);
                story.setErrorMessage("Story rewrite failed - no rewrite response generated");
                storyDataStoreRepository.save(story);
                throw new RuntimeException("Story rewrite failed - no rewrite response generated");
            }
        } else {
            log.info("Story {} - Step 2: Story rewrite already completed, skipping", story.getStoryId());
        }

        // Step 3: Story analysis - skip if already completed
        if (!isStoryAnalysisCompleted(story)) {
            log.info("Story {} - Step 3: Analyzing story", story.getStoryId());
            storyRewriteService.processStoryAnalysis(story);
        } else {
            log.info("Story {} - Step 3: Story analysis already completed, skipping", story.getStoryId());
        }

        // Step 4: Validate story - stop and mark rejected if not valid
        if (!isStoryValid(story)) {
            String errorMessage = getInvalidStoryErrorMessage(story);
            log.error("Story {} - Step 4: Story validation failed - {}", story.getStoryId(), errorMessage);
            story.setProcessingStatus(StoryDataStore.ProcessingStatus.REJECTED);
            story.setErrorMessage(errorMessage);
            if (story.getErrors() == null) {
                story.setErrors(new ArrayList<>());
            }
            story.getErrors().add(errorMessage);
            storyDataStoreRepository.save(story);
            return; // Exit processing without throwing exception
        }

        // Step 5: Audio generation (only after confirming it's a valid story) - skip if
        // already completed
        if (story.getAudioUrl() == null && story.getUploadMetadata() != null &&
                story.getUploadMetadata().get("ttsAudioData") == null) {
            try {
                log.info("Story {} - Step 5: Audio generation ", story.getStoryId());
                String gender = Optional.ofNullable(userService.getUserById(story.getUserId()).getGender())
                        .map(Enum::name)
                        .orElse("unknown");
                TTSResponse ttsResponse = ttsService.generateAudio(story.getStoryRewriteResponse().getRewrittenText(),
                        story.getLanguage(), gender);

                // Check if TTS generation was successful
                if (ttsResponse == null || "ERROR".equals(ttsResponse.getStatus()) ||
                        ttsResponse.getAudioData() == null || ttsResponse.getAudioData().trim().isEmpty()) {
                    String errorMsg = ttsResponse != null ? ttsResponse.getError() : "TTS response is null";
                    log.error("Story {} - Step 5: Audio generation failed - {}", story.getStoryId(), errorMsg);
                    story.setProcessingStatus(StoryDataStore.ProcessingStatus.FAILED);
                    story.setErrorMessage("Audio generation failed: " + errorMsg);
                    story.setStepErrors(new HashMap<>());
                    story.getStepErrors().put("audio_generation", errorMsg);
                    storyDataStoreRepository.save(story);
                    throw new RuntimeException("Audio generation failed: " + errorMsg);
                }

                story.getUploadMetadata().put("ttsAudioData", ttsResponse.getAudioData());
                log.info("Story {} - Step 5: Audio generation completed successfully", story.getStoryId());
            } catch (Exception e) {
                log.error("Story {} - Step 5: Audio generation failed - {}", story.getStoryId(), e.getMessage());
                story.setProcessingStatus(StoryDataStore.ProcessingStatus.FAILED);
                story.setErrorMessage("Audio generation failed: " + e.getMessage());
                story.setStepErrors(new HashMap<>());
                story.getStepErrors().put("audio_generation", e.getMessage());
                storyDataStoreRepository.save(story);
                throw new RuntimeException("Audio generation failed: " + e.getMessage());
            }
        } else {
            log.info("Story {} - Step 5: Audio generation already completed, skipping", story.getStoryId());
        }

        // Step 6: Paragraph rewrite - skip if already completed
        if (!isParagraphRewriteCompleted(story)) {
            log.info("Story {} - Step 6: Rewriting paragraphs", story.getStoryId());
            storyRewriteService.processParagraphRewrite(story);
        } else {
            log.info("Story {} - Step 6: Paragraph rewrite already completed, skipping", story.getStoryId());
        }

        // Step 7: Visual prompts - skip if already completed
        if (!isVisualPromptsCompleted(story)) {
            log.info("Story {} - Step 7: Generating visual prompts", story.getStoryId());
            storyRewriteService.processVisualPrompts(story);
        } else {
            log.info("Story {} - Step 7: Visual prompts already completed, skipping", story.getStoryId());
        }

        // Step 8: Story image generation - skip if already completed
        if (!isImagesCompleted(story)) {
            log.info("Story {} - Step 8: Generating story images", story.getStoryId());
            generateStoryImages(story);
        } else {
            log.info("Story {} - Step 8: Story images already completed, skipping", story.getStoryId());
        }

        // Step 9: Thumbnail generation - skip if already completed
        if (!isThumbnailCompleted(story)) {
            log.info("Story {} - Step 9: Generating thumbnail", story.getStoryId());
            boolean thumbnailGenerated = thumbnailGenerationService.generateThumbnail(story);
            if (thumbnailGenerated) {
                storyDataStoreRepository.save(story);
                log.info("Story {} - Step 9: Thumbnail generated successfully", story.getStoryId());
            } else {
                log.warn("Story {} - Step 9: Thumbnail generation failed, continuing without thumbnail",
                        story.getStoryId());
            }
        } else {
            log.info("Story {} - Step 9: Thumbnail already completed, skipping", story.getStoryId());
        }

        log.info("Story {} - WRITTEN story processing completed successfully", story.getStoryId());
    }

    // Helper to check if story is valid
    private boolean isStoryValid(StoryDataStore story) {
        if (story.getStoryAnalysisResponse() == null || story.getStoryAnalysisResponse().getAnalysis() == null) {
            return false;
        }
        Boolean isValid = story.getStoryAnalysisResponse().getAnalysis().isValidStory();
        return isValid != null && isValid;
    }

    // Helper to get detailed error message for invalid stories
    private String getInvalidStoryErrorMessage(StoryDataStore story) {
        if (story.getStoryAnalysisResponse() == null || story.getStoryAnalysisResponse().getAnalysis() == null) {
            return promptConfig.getPrompt("story_processing_error_no_analysis");
        }

        StoryAnalysisResponse.Analysis analysis = story.getStoryAnalysisResponse().getAnalysis();
        String storyType = analysis.getStoryType() != null ? analysis.getStoryType() : "unknown";
        String plotSummary = analysis.getPlotSummary() != null ? analysis.getPlotSummary() : "no summary available";

        Map<String, String> params = new HashMap<>();
        params.put("storyType", storyType);
        params.put("plotSummary", plotSummary);
        return promptConfig.formatPrompt("story_processing_error_invalid_story", params);
    }

    // Helper to generate story images using already-computed visual prompts
    private void generateStoryImages(StoryDataStore story) {
        try {
            log.info("Story {} - Generating images from visual prompts", story.getStoryId());

            if (story.getVisualPromptResponse() == null || story.getVisualPromptResponse().getVisualPrompts() == null) {
                log.warn("Story {} - No visual prompts available for image generation", story.getStoryId());
                return;
            }

            List<String> imageUrls = storyRewriteService.generateImagesFromVisualPrompts(
                    story.getVisualPromptResponse().getVisualPrompts());

            ImagesResponse imagesResponse = ImagesResponse.builder()
                    .storyImageUrls(imageUrls)
                    .status("SUCCESS")
                    .build();

            story.setImagesResponse(imagesResponse);
            storyDataStoreRepository.save(story);

            log.info("Story {} - Successfully generated {} images", story.getStoryId(), imageUrls.size());

        } catch (Exception e) {
            log.error("Story {} - Failed to generate story images: {}", story.getStoryId(), e.getMessage());
            // Don't fail the entire process for image generation errors
        }
    }

}