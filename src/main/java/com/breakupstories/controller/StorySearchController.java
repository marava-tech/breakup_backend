package com.breakupstories.controller;

import com.breakupstories.dto.StorySearchRequest;
import com.breakupstories.dto.StorySearchResponse;
import com.breakupstories.service.StoryDataStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


import java.util.Map;

/**
 * Controller for story search operations using the search index
 */
@RestController
@RequestMapping("/api/stories/search")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Story Search", description = "Story search and filtering APIs")
public class StorySearchController {
    
    private final StoryDataStoreService storyDataStoreService;
    
    @PostMapping
    @Operation(summary = "Comprehensive story search", description = "Search stories using multiple criteria including text, tags, emotions, location, etc.")
    public ResponseEntity<StorySearchResponse> searchStories(@RequestBody StorySearchRequest request, Authentication authentication) {
        log.info("Received comprehensive search request: {}", request);
        StorySearchResponse response = storyDataStoreService.comprehensiveSearch(request,authentication);
        log.info("Search completed. Found {} stories", response.getStories().size());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/advanced")
    @Operation(summary = "Search stories with query parameters", description = "Search stories using query parameters for simple searches")
    public ResponseEntity<StorySearchResponse> searchStoriesWithParams(
            @RequestParam(required = false) String searchText,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String emotionType,
            @RequestParam(required = false) Double minEmotionScore,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String pincode,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String createdAfter,
            @RequestParam(required = false) String createdBefore,
            @RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        
        // Build request from parameters
        StorySearchRequest request = StorySearchRequest.builder()
                .searchText(searchText)
                .title(title)
                .tags(tags != null ? java.util.Arrays.asList(tags.split(",")) : null)
                .emotionType(emotionType)
                .minEmotionScore(minEmotionScore)
                .location(location)
                .state(state)
                .district(district)
                .pincode(pincode)
                .name(name)
                .userId(userId)
                .language(language)
                .createdAfter(createdAfter != null ? java.time.LocalDateTime.parse(createdAfter) : null)
                .createdBefore(createdBefore != null ? java.time.LocalDateTime.parse(createdBefore) : null)
                .status(com.breakupstories.model.Story.StoryStatus.valueOf(status))
                .page(page)
                .size(size)
                .build();
        
        log.info("Received parameter-based search request: {}", request);
        
        StorySearchResponse response = storyDataStoreService.comprehensiveSearch(request,authentication);
        
        log.info("Search completed. Found {} stories", response.getStories().size());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/count")
    @Operation(summary = "Get active stories count", description = "Get the total number of active stories")
    public ResponseEntity<Long> getActiveStoriesCount() {
        long activeStories = storyDataStoreService.countActiveStories();
        return ResponseEntity.ok(activeStories);
    }

    @GetMapping("/stats")
    public ResponseEntity<Object> getSearchStats() {
        try {
            long activeStories = storyDataStoreService.countActiveStories();
            
            return ResponseEntity.ok(Map.of(
                    "activeStories", activeStories,
                    "timestamp", java.time.LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            log.error("Error getting search stats: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get statistics: " + e.getMessage()));
        }
    }
} 