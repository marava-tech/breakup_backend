package com.breakupstories.dto;

import com.breakupstories.model.Story;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Comprehensive search request for stories
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorySearchRequest {
    
    // Pagination
    private Integer page = 0;
    private Integer size = 10;
    
    // Text search
    private String searchText;
    private String title;
    
    // Tags and emotions
    private List<String> tags;
    private String emotionType;
    private Double minEmotionScore;
    
    // Location filters
    private String location;
    private String state;
    private String district;
    private String pincode;
    private String name;
    
    // User and language filters
    private String userId;
    private String language;
    
    // Status and date filters
    private Story.StoryStatus status = Story.StoryStatus.ACTIVE;
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
    
    // Sorting
    private String sortBy = "createdAt";
    private String sortDirection = "DESC"; // ASC or DESC
    
    /**
     * Convert to Pageable for pagination
     */
    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }
    
    /**
     * Check if any search criteria are provided
     */
    public boolean hasSearchCriteria() {
        return searchText != null || title != null || tags != null || 
               emotionType != null || location != null || state != null || 
               district != null || pincode != null || name != null || 
               userId != null || language != null || createdAfter != null || 
               createdBefore != null;
    }
    
    /**
     * Check if text search is requested
     */
    public boolean hasTextSearch() {
        return searchText != null && !searchText.trim().isEmpty();
    }
    
    /**
     * Check if title search is requested
     */
    public boolean hasTitleSearch() {
        return title != null && !title.trim().isEmpty();
    }
    
    /**
     * Check if tag search is requested
     */
    public boolean hasTagSearch() {
        return tags != null && !tags.isEmpty();
    }
    
    /**
     * Check if emotion search is requested
     */
    public boolean hasEmotionSearch() {
        return emotionType != null && !emotionType.trim().isEmpty();
    }
    
    /**
     * Check if location search is requested
     */
    public boolean hasLocationSearch() {
        return location != null && !location.trim().isEmpty();
    }
    
    /**
     * Check if date range search is requested
     */
    public boolean hasDateRangeSearch() {
        return createdAfter != null || createdBefore != null;
    }
} 