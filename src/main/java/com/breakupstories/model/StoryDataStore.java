package com.breakupstories.model;

import com.breakupstories.dto.TranscriptionResponse;
import com.breakupstories.dto.StoryRewriteResponse;
import com.breakupstories.dto.ParagraphRewriteResponse;
import com.breakupstories.dto.StoryAnalysisResponse;
import com.breakupstories.dto.VisualPromptResponse;
import com.breakupstories.dto.ImagesResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Model to store AI processing responses and search index data for stories
 * This collection stores all the responses from various AI APIs and searchable data for each story
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "story_data_store")
public class StoryDataStore {
    
    @Id
    private String id;
    
    private String storyId;

    // Upload metadata - moved from Story
    private Map<String, String> uploadMetadata;
    
    // Search index fields
    private String title;
    private String userId;
    private String language;
    private StoryMetadata metadata;
    private String searchText;

    // Audio information
    private String audioUrl;
    private Long duration; // Duration in milliseconds
    
    // AI processing responses
    private TranscriptionResponse transcriptionResponse;
    private StoryRewriteResponse storyRewriteResponse;
    private ParagraphRewriteResponse paragraphRewriteResponse;
    private StoryAnalysisResponse storyAnalysisResponse;
    private VisualPromptResponse visualPromptResponse;
    
    // Images response
    private ImagesResponse imagesResponse;
    
    // Processing status for AI steps
    private ProcessingStatus processingStatus;
    
    // Error tracking
    private List<String> errors;
    private String errorMessage;
    private String transcriptionError;
    private String rewriteError;
    private String paragraphError;
    private String analysisError;
    private String visualPromptError;
    private Map<String, String> stepErrors; // Detailed errors for each step
    
    // Processing metadata
    private LocalDateTime transcriptionCompletedAt;
    private LocalDateTime rewriteCompletedAt;
    private LocalDateTime paragraphCompletedAt;
    private LocalDateTime analysisCompletedAt;
    private LocalDateTime visualPromptCompletedAt;
    private LocalDateTime processingStartedAt;
    private LocalDateTime processingCompletedAt;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;

    private boolean isConversionPending;
    
    public enum ProcessingStatus {
        UPLOAD_PENDING,    // Initial state - waiting for upload
        UPLOADING,         // Currently uploading audio
        PROCESSING_PENDING, // Upload complete, waiting for AI processing
        PROCESSING,        // Currently being processed by AI
        PROCESSED,         // AI processing completed
        CONVERTING,        // Converting to final Story
        COMPLETED,         // All processing steps completed
        FAILED,            // Processing failed
        REJECTED          // Story was rejecte
    }
    
    /**
     * Generate search text from all searchable fields
     */
    public String generateSearchText() {
        StringBuilder searchText = new StringBuilder();
        
        if (title != null) {
            searchText.append(title).append(" ");
        }
        
        if (metadata != null) {
            if (metadata.getLocations() != null) {
                searchText.append(String.join(" ", metadata.getLocations())).append(" ");
            }
            if (metadata.getNames() != null) {
                searchText.append(String.join(" ", metadata.getNames())).append(" ");
            }
            if (metadata.getState() != null) {
                searchText.append(metadata.getState()).append(" ");
            }
            if (metadata.getDistrict() != null) {
                searchText.append(metadata.getDistrict()).append(" ");
            }
            if (metadata.getPincodes() != null) {
                searchText.append(String.join(" ", metadata.getPincodes())).append(" ");
            }
        }
        
        return searchText.toString().trim();
    }
} 