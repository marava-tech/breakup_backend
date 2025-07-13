package com.breakupstories.service;

import com.breakupstories.model.Story;
import com.breakupstories.model.StoryDataStore;
import com.breakupstories.repository.StoryDataStoreRepository;
import com.breakupstories.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service to handle story processing status queries
 * Provides methods to get processing status and data
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StoryProcessingService {
    
    private final StoryDataStoreRepository processingDataRepository;
    private final StoryRepository storyRepository;

    /**
     * Get processing data for a story
     * @param storyId The story ID
     * @return Optional containing processing data if found
     */
    public Optional<StoryDataStore> getProcessingData(String storyId) {
        log.info("Getting processing data for story: {}", storyId);
        return processingDataRepository.findByStoryId(storyId);
    }

    /**
     * Get detailed processing status for a story
     * @param storyId The story ID
     * @return ProcessingStatusDetails with comprehensive status information
     */
    public ProcessingStatusDetails getDetailedProcessingStatus(String storyId) {
        log.info("Getting detailed processing status for story: {}", storyId);
        
        Optional<StoryDataStore> processingData = getProcessingData(storyId);
        
        if (processingData.isEmpty()) {
            return ProcessingStatusDetails.builder()
                    .storyId(storyId)
                    .overallStatus("NOT_FOUND")
                    .transcriptionStatus("NOT_FOUND")
                    .rewriteStatus("NOT_FOUND")
                    .paragraphStatus("NOT_FOUND")
                    .analysisStatus("NOT_FOUND")
                    .build();
        }
        
        StoryDataStore data = processingData.get();
        
        return ProcessingStatusDetails.builder()
                .storyId(storyId)
                .overallStatus(getOverallStatus(data))
                .transcriptionStatus(getTranscriptionStatus(data))
                .rewriteStatus(getRewriteStatus(data))
                .paragraphStatus(getParagraphStatus(data))
                .analysisStatus(getAnalysisStatus(data))
                .errorMessage(data.getErrorMessage())
                .transcriptionError(data.getTranscriptionError())
                .rewriteError(data.getRewriteError())
                .paragraphError(data.getParagraphError())
                .analysisError(data.getAnalysisError())
                .processingStartedAt(data.getProcessingStartedAt())
                .processingCompletedAt(data.getProcessingCompletedAt())
                .transcriptionCompletedAt(data.getTranscriptionCompletedAt())
                .rewriteCompletedAt(data.getRewriteCompletedAt())
                .paragraphCompletedAt(data.getParagraphCompletedAt())
                .analysisCompletedAt(data.getAnalysisCompletedAt())
                .build();
    }
    
    private String getOverallStatus(StoryDataStore data) {
        if (data.getProcessingStatus() == null) {
            return "UNKNOWN";
        }
        
        return switch (data.getProcessingStatus()) {
            case UPLOAD_PENDING -> "UPLOAD_PENDING";
            case UPLOADING -> "UPLOADING";
            case PROCESSING_PENDING -> "PROCESSING_PENDING";
            case PROCESSING -> "PROCESSING";
            case PROCESSED -> "PROCESSED";
            case CONVERTING -> "CONVERTING";
            case COMPLETED -> "COMPLETED";
            case FAILED -> "FAILED";
            case REJECTED -> "REJECTED";
        };
    }
    
    private String getTranscriptionStatus(StoryDataStore data) {
        if (data.getTranscriptionError() != null) {
            return "FAILED";
        }
        if (data.getTranscriptionCompletedAt() != null) {
            return "COMPLETED";
        }
        if (data.getProcessingStatus() == StoryDataStore.ProcessingStatus.PROCESSING) {
            return "IN_PROGRESS";
        }
        return "PENDING";
    }
    
    private String getRewriteStatus(StoryDataStore data) {
        if (data.getRewriteError() != null) {
            return "FAILED";
        }
        if (data.getRewriteCompletedAt() != null) {
            return "COMPLETED";
        }
        if (data.getProcessingStatus() == StoryDataStore.ProcessingStatus.PROCESSING) {
            return "IN_PROGRESS";
        }
        return "PENDING";
    }
    
    private String getParagraphStatus(StoryDataStore data) {
        if (data.getParagraphError() != null) {
            return "FAILED";
        }
        if (data.getParagraphCompletedAt() != null) {
            return "COMPLETED";
        }
        if (data.getProcessingStatus() == StoryDataStore.ProcessingStatus.PROCESSING) {
            return "IN_PROGRESS";
        }
        return "PENDING";
    }
    
    private String getAnalysisStatus(StoryDataStore data) {
        if (data.getAnalysisError() != null) {
            return "FAILED";
        }
        if (data.getAnalysisCompletedAt() != null) {
            return "COMPLETED";
        }
        if (data.getProcessingStatus() == StoryDataStore.ProcessingStatus.PROCESSING) {
            return "IN_PROGRESS";
        }
        return "PENDING";
    }

    /**
     * Processing status details for a story
     */
    public static class ProcessingStatusDetails {
        private String storyId;
        private String overallStatus;
        private String transcriptionStatus;
        private String rewriteStatus;
        private String paragraphStatus;
        private String analysisStatus;
        private String errorMessage;
        private String transcriptionError;
        private String rewriteError;
        private String paragraphError;
        private String analysisError;
        private LocalDateTime processingStartedAt;
        private LocalDateTime processingCompletedAt;
        private LocalDateTime transcriptionCompletedAt;
        private LocalDateTime rewriteCompletedAt;
        private LocalDateTime paragraphCompletedAt;
        private LocalDateTime analysisCompletedAt;

        public static ProcessingStatusDetailsBuilder builder() {
            return new ProcessingStatusDetailsBuilder();
        }

        public static class ProcessingStatusDetailsBuilder {
            private final ProcessingStatusDetails details = new ProcessingStatusDetails();

            public ProcessingStatusDetailsBuilder storyId(String storyId) {
                details.storyId = storyId;
                return this;
            }

            public ProcessingStatusDetailsBuilder overallStatus(String overallStatus) {
                details.overallStatus = overallStatus;
                return this;
            }

            public ProcessingStatusDetailsBuilder transcriptionStatus(String transcriptionStatus) {
                details.transcriptionStatus = transcriptionStatus;
                return this;
            }

            public ProcessingStatusDetailsBuilder rewriteStatus(String rewriteStatus) {
                details.rewriteStatus = rewriteStatus;
                return this;
            }

            public ProcessingStatusDetailsBuilder paragraphStatus(String paragraphStatus) {
                details.paragraphStatus = paragraphStatus;
                return this;
            }

            public ProcessingStatusDetailsBuilder analysisStatus(String analysisStatus) {
                details.analysisStatus = analysisStatus;
                return this;
            }

            public ProcessingStatusDetailsBuilder errorMessage(String errorMessage) {
                details.errorMessage = errorMessage;
                return this;
            }

            public ProcessingStatusDetailsBuilder transcriptionError(String transcriptionError) {
                details.transcriptionError = transcriptionError;
                return this;
            }

            public ProcessingStatusDetailsBuilder rewriteError(String rewriteError) {
                details.rewriteError = rewriteError;
                return this;
            }

            public ProcessingStatusDetailsBuilder paragraphError(String paragraphError) {
                details.paragraphError = paragraphError;
                return this;
            }

            public ProcessingStatusDetailsBuilder analysisError(String analysisError) {
                details.analysisError = analysisError;
                return this;
            }

            public ProcessingStatusDetailsBuilder processingStartedAt(LocalDateTime processingStartedAt) {
                details.processingStartedAt = processingStartedAt;
                return this;
            }

            public ProcessingStatusDetailsBuilder processingCompletedAt(LocalDateTime processingCompletedAt) {
                details.processingCompletedAt = processingCompletedAt;
                return this;
            }

            public ProcessingStatusDetailsBuilder transcriptionCompletedAt(LocalDateTime transcriptionCompletedAt) {
                details.transcriptionCompletedAt = transcriptionCompletedAt;
                return this;
            }

            public ProcessingStatusDetailsBuilder rewriteCompletedAt(LocalDateTime rewriteCompletedAt) {
                details.rewriteCompletedAt = rewriteCompletedAt;
                return this;
            }

            public ProcessingStatusDetailsBuilder paragraphCompletedAt(LocalDateTime paragraphCompletedAt) {
                details.paragraphCompletedAt = paragraphCompletedAt;
                return this;
            }

            public ProcessingStatusDetailsBuilder analysisCompletedAt(LocalDateTime analysisCompletedAt) {
                details.analysisCompletedAt = analysisCompletedAt;
                return this;
            }

            public ProcessingStatusDetails build() {
                return details;
            }
        }

        // Getters
        public String getStoryId() { return storyId; }
        public String getOverallStatus() { return overallStatus; }
        public String getTranscriptionStatus() { return transcriptionStatus; }
        public String getRewriteStatus() { return rewriteStatus; }
        public String getParagraphStatus() { return paragraphStatus; }
        public String getAnalysisStatus() { return analysisStatus; }
        public String getErrorMessage() { return errorMessage; }
        public String getTranscriptionError() { return transcriptionError; }
        public String getRewriteError() { return rewriteError; }
        public String getParagraphError() { return paragraphError; }
        public String getAnalysisError() { return analysisError; }
        public LocalDateTime getProcessingStartedAt() { return processingStartedAt; }
        public LocalDateTime getProcessingCompletedAt() { return processingCompletedAt; }
        public LocalDateTime getTranscriptionCompletedAt() { return transcriptionCompletedAt; }
        public LocalDateTime getRewriteCompletedAt() { return rewriteCompletedAt; }
        public LocalDateTime getParagraphCompletedAt() { return paragraphCompletedAt; }
        public LocalDateTime getAnalysisCompletedAt() { return analysisCompletedAt; }
    }
} 