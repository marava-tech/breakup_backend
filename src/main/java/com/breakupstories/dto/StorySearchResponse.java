package com.breakupstories.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Comprehensive search response for stories
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorySearchResponse {
    
    // Search results
    private List<StoryResponse> stories;
    
    // Pagination info
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int pageSize;
    
    // Search metadata
    private String searchQuery;
    private List<String> appliedFilters;
    private long searchTimeMs;
    
    // Statistics
    private long totalStories;
    private long activeStories;
    
    /**
     * Create response from search results
     */
    public static StorySearchResponse fromSearchResults(
            List<StoryResponse> stories,
            int currentPage,
            int totalPages,
            long totalElements,
            int pageSize,
            String searchQuery,
            List<String> appliedFilters,
            long searchTimeMs,
            long totalStories,
            long activeStories) {
        
        return StorySearchResponse.builder()
                .stories(stories)
                .currentPage(currentPage)
                .totalPages(totalPages)
                .totalElements(totalElements)
                .pageSize(pageSize)
                .searchQuery(searchQuery)
                .appliedFilters(appliedFilters)
                .searchTimeMs(searchTimeMs)
                .totalStories(totalStories)
                .activeStories(activeStories)
                .build();
    }
} 