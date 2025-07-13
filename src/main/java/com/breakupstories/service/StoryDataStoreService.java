package com.breakupstories.service;

import com.breakupstories.dto.StorySearchRequest;
import com.breakupstories.dto.StorySearchResponse;
import com.breakupstories.dto.StoryResponse;
import com.breakupstories.model.*;
import com.breakupstories.repository.StoryRepository;
import com.breakupstories.repository.StoryDataStoreRepository;
import com.breakupstories.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing story data store and search index operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StoryDataStoreService {
    
    private final StoryDataStoreRepository dataStoreRepository;
    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    @Lazy
    private final LikeService likeService;
    @Lazy
    private final CommentService commentService;
    @Lazy
    private final BookmarkService bookmarkService;

    /**
     * Update data store with errors
     */
    public StoryDataStore updateDataStoreWithErrors(String storyId, List<String> errors) {
        log.info("Updating data store with errors for story: {}", storyId);
        
        Optional<StoryDataStore> existingData = dataStoreRepository.findByStoryId(storyId);
        if (existingData.isEmpty()) {
            log.warn("Data store not found for story: {}", storyId);
            return null;
        }
        
        StoryDataStore dataStore = existingData.get();
        
        // Initialize errors list if null
        if (dataStore.getErrors() == null) {
            dataStore.setErrors(new ArrayList<>());
        }
        
        // Add new errors
        dataStore.getErrors().addAll(errors);
        
        StoryDataStore savedData = dataStoreRepository.save(dataStore);
        log.info("Data store updated with {} errors for story: {}", errors.size(), storyId);
        
        return savedData;
    }
    
    /**
     * Add single error to data store
     */
    public StoryDataStore addErrorToDataStore(String storyId, String error) {
        return updateDataStoreWithErrors(storyId, Collections.singletonList(error));
    }
    
    /**
     * Clear errors from data store
     */
    public StoryDataStore clearErrorsFromDataStore(String storyId) {
        log.info("Clearing errors from data store for story: {}", storyId);
        
        Optional<StoryDataStore> existingData = dataStoreRepository.findByStoryId(storyId);
        if (existingData.isEmpty()) {
            log.warn("Data store not found for story: {}", storyId);
            return null;
        }
        
        StoryDataStore dataStore = existingData.get();
        dataStore.setErrors(new ArrayList<>());
        
        StoryDataStore savedData = dataStoreRepository.save(dataStore);
        log.info("Errors cleared from data store for story: {}", storyId);
        
        return savedData;
    }
    
    /**
     * Delete data store for a story
     */
    public void deleteDataStore(String storyId) {
        log.info("Deleting data store for story: {}", storyId);
        dataStoreRepository.deleteByStoryId(storyId);
    }
    
    /**
     * Get story IDs from search results
     */
    public List<String> getStoryIdsFromSearchResults(List<StoryDataStore> searchResults) {
        return searchResults.stream()
                .map(StoryDataStore::getStoryId)
                .collect(Collectors.toList());
    }
    
    /**
     * Get stories by search results
     */
    public List<Story> getStoriesFromSearchResults(List<StoryDataStore> searchResults) {
        List<String> storyIds = getStoryIdsFromSearchResults(searchResults);
        return storyRepository.findAllById(storyIds);
    }
    
    /**
     * Get data store by story ID
     */
    public Optional<StoryDataStore> getDataStoreByStoryId(String storyId) {
        return dataStoreRepository.findByStoryId(storyId);
    }
    
    /**
     * Count active stories
     */
    public long countActiveStories() {
        return dataStoreRepository.countByProcessingStatus(StoryDataStore.ProcessingStatus.COMPLETED);
    }
    
    /**
     * Comprehensive search that returns Story objects with pagination
     */
    public StorySearchResponse comprehensiveSearch(StorySearchRequest request,Authentication authentication) {
        long startTime = System.currentTimeMillis();
        
        log.info("Starting comprehensive search with filters: {}", request);
        
        List<StoryDataStore> searchResults = new java.util.ArrayList<>();
        List<String> appliedFilters = new java.util.ArrayList<>();
        
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow(()->new RuntimeException("User not found"));  
        
        try {
            // Apply different search strategies based on request
            if (request.hasTextSearch()) {
                // Full-text search
                Page<StoryDataStore> textResults = dataStoreRepository.findBySearchTextContaining(
                        request.getSearchText(), request.toPageable());
                searchResults = textResults.getContent();
                appliedFilters.add("text_search: " + request.getSearchText());
                log.info("Text search found {} results", searchResults.size());
                
            } else if (request.hasTitleSearch()) {
                // Title search
                Page<StoryDataStore> titleResults = dataStoreRepository.findByTitleContaining(
                        request.getTitle(), request.toPageable());
                searchResults = titleResults.getContent();
                appliedFilters.add("title_search: " + request.getTitle());
                log.info("Title search found {} results", titleResults.getTotalElements());
                
            } else if (request.hasTagSearch()) {
                // Tag search - search in Story entities since tags are stored there
                List<Story> storiesWithTags = storyRepository.findByTagsIn(request.getTags());
                List<String> storyIds = storiesWithTags.stream()
                        .map(Story::getId)
                        .collect(Collectors.toList());
                
                // Get corresponding StoryDataStore entries
                searchResults = dataStoreRepository.findAllById(storyIds);
                appliedFilters.add("tags: " + String.join(", ", request.getTags()));
                log.info("Tag search found {} results", searchResults.size());
                
            } else if (request.hasEmotionSearch()) {
                // Emotion search
                if (request.getMinEmotionScore() != null) {
                    searchResults = dataStoreRepository.findByEmotionTypeAndMinScore(
                            request.getEmotionType(), request.getMinEmotionScore());
                    appliedFilters.add("emotion: " + request.getEmotionType() + " (min score: " + request.getMinEmotionScore() + ")");
                } else {
                    searchResults = dataStoreRepository.findByEmotionType(request.getEmotionType());
                    appliedFilters.add("emotion: " + request.getEmotionType());
                }
                log.info("Emotion search found {} results", searchResults.size());
                
            } else if (request.hasLocationSearch()) {
                // Location search
                searchResults = dataStoreRepository.findByLocation(request.getLocation());
                appliedFilters.add("location: " + request.getLocation());
                log.info("Location search found {} results", searchResults.size());
                
            } else if (request.getState() != null) {
                // State search
                searchResults = dataStoreRepository.findByState(request.getState());
                appliedFilters.add("state: " + request.getState());
                log.info("State search found {} results", searchResults.size());
                
            } else if (request.getDistrict() != null) {
                // District search
                searchResults = dataStoreRepository.findByDistrict(request.getDistrict());
                appliedFilters.add("district: " + request.getDistrict());
                log.info("District search found {} results", searchResults.size());
                
            } else if (request.getPincode() != null) {
                // Pincode search
                searchResults = dataStoreRepository.findByPincode(request.getPincode());
                appliedFilters.add("pincode: " + request.getPincode());
                log.info("Pincode search found {} results", searchResults.size());
                
            } else if (request.getName() != null) {
                // Name search
                searchResults = dataStoreRepository.findByName(request.getName());
                appliedFilters.add("name: " + request.getName());
                log.info("Name search found {} results", searchResults.size());
                
            } else if (request.getUserId() != null) {
                // User search
                searchResults = dataStoreRepository.findByUserIdAndProcessingStatus(request.getUserId(), convertToProcessingStatus(request.getStatus()));
                appliedFilters.add("user: " + request.getUserId());
                log.info("User search found {} results", searchResults.size());
                
            } else if (request.getLanguage() != null) {
                // Language search
                searchResults = dataStoreRepository.findByLanguageAndProcessingStatus(request.getLanguage(), convertToProcessingStatus(request.getStatus()));
                appliedFilters.add("language: " + request.getLanguage());
                log.info("Language search found {} results", searchResults.size());
                
            } else if (request.hasDateRangeSearch()) {
                // Date range search
                if (request.getCreatedAfter() != null && request.getCreatedBefore() != null) {
                    searchResults = dataStoreRepository.findByCreatedAtBetween(request.getCreatedAfter(), request.getCreatedBefore());
                    appliedFilters.add("date_range: " + request.getCreatedAfter() + " to " + request.getCreatedBefore());
                } else if (request.getCreatedAfter() != null) {
                    searchResults = dataStoreRepository.findByCreatedAtAfter(request.getCreatedAfter());
                    appliedFilters.add("created_after: " + request.getCreatedAfter());
                }
                log.info("Date range search found {} results", searchResults.size());
                
            } else {
                // Default: get all completed stories
                searchResults = dataStoreRepository.findByProcessingStatus(convertToProcessingStatus(request.getStatus()));
                appliedFilters.add("processingStatus: " + request.getStatus());
                log.info("Default search (completed stories) found {} results", searchResults.size());
            }
            
            // Apply additional filters if results exist
            if (!searchResults.isEmpty()) {
                searchResults = applyAdditionalFilters(searchResults, request, appliedFilters);
            }
            
        } catch (Exception e) {
            log.error("Error in comprehensive search: {}", e.getMessage(), e);
            searchResults = new java.util.ArrayList<>();
            appliedFilters.add("error: " + e.getMessage());
        }
        
        // Get story IDs and fetch actual Story objects
        List<String> storyIds = getStoryIdsFromSearchResults(searchResults);
        List<Story> stories = storyRepository.findAllById(storyIds);
        
        // Convert Story objects to StoryResponse objects
        List<StoryResponse> storyResponses = stories.stream()
                .map(story -> convertToStoryResponse(story, user.getId()))
                .collect(Collectors.toList());
        
        // Apply pagination to story responses
        List<StoryResponse> paginatedStoryResponses = applyPagination(storyResponses, request);
        
        long searchTime = System.currentTimeMillis() - startTime;
        
        // Calculate pagination info
        int totalPages = (int) Math.ceil((double) stories.size() / request.getSize());
        long totalElements = stories.size();
        
        // Get statistics
        long totalStories = dataStoreRepository.count();
        long activeStories = dataStoreRepository.countByProcessingStatus(StoryDataStore.ProcessingStatus.COMPLETED);
        
        log.info("Comprehensive search completed in {}ms. Found {} results", searchTime, paginatedStoryResponses.size());
        
        return StorySearchResponse.fromSearchResults(
                paginatedStoryResponses,
                request.getPage(),
                totalPages,
                totalElements,
                request.getSize(),
                buildSearchQuery(request),
                appliedFilters,
                searchTime,
                totalStories,
                activeStories
        );
    }
    
    /**
     * Apply additional filters to results
     */
    private List<StoryDataStore> applyAdditionalFilters(List<StoryDataStore> results, 
                                                        StorySearchRequest request, 
                                                        List<String> appliedFilters) {
        List<StoryDataStore> filteredResults = new java.util.ArrayList<>(results);
        
        // Filter by processing status if not already filtered
        if (request.getStatus() != null) {
            filteredResults = filteredResults.stream()
                    .filter(data -> convertToProcessingStatus(request.getStatus()).equals(data.getProcessingStatus()))
                    .collect(java.util.stream.Collectors.toList());
            appliedFilters.add("processingStatus: " + request.getStatus());
        }
        
        // Filter by user if not already filtered
        if (request.getUserId() != null) {
            filteredResults = filteredResults.stream()
                    .filter(data -> request.getUserId().equals(data.getUserId()))
                    .collect(java.util.stream.Collectors.toList());
            appliedFilters.add("user: " + request.getUserId());
        }
        
        // Filter by language if not already filtered
        if (request.getLanguage() != null) {
            filteredResults = filteredResults.stream()
                    .filter(data -> request.getLanguage().equals(data.getLanguage()))
                    .collect(java.util.stream.Collectors.toList());
            appliedFilters.add("language: " + request.getLanguage());
        }
        
        return filteredResults;
    }
    
    /**
     * Convert Story to StoryResponse
     */
    private StoryResponse convertToStoryResponse(Story story, String currentUserId) {
        // Get user for username
        User user = userRepository.findById(story.getUserId()).orElse(null);
        
        // Fetch like count, comment count, and user interaction status
        long likeCount = likeService.getLikeCount(story.getId());
        long commentCount = commentService.getCommentCount(story.getId());
        
        // Check if current user liked/bookmarked this story (only if currentUserId is provided)
        boolean likedByMe = currentUserId != null && likeService.isLiked(currentUserId, story.getId());
        boolean bookmarkedByMe = currentUserId != null && bookmarkService.isBookmarked(currentUserId, story.getId());

        return StoryResponse.fromStory(story, user, likedByMe, bookmarkedByMe, likeCount, commentCount);
    }
    
    /**
     * Apply pagination to StoryResponse objects
     */
    private List<StoryResponse> applyPagination(List<StoryResponse> results, StorySearchRequest request) {
        int startIndex = request.getPage() * request.getSize();
        int endIndex = Math.min(startIndex + request.getSize(), results.size());
        
        if (startIndex >= results.size()) {
            return new java.util.ArrayList<>();
        }
        
        return results.subList(startIndex, endIndex);
    }
    
    /**
     * Build search query string for logging
     */
    private String buildSearchQuery(StorySearchRequest request) {
        StringBuilder query = new StringBuilder();
        
        if (request.hasTextSearch()) {
            query.append("text:").append(request.getSearchText());
        } else if (request.hasTitleSearch()) {
            query.append("title:").append(request.getTitle());
        } else if (request.hasTagSearch()) {
            query.append("tags:").append(String.join(",", request.getTags()));
        } else if (request.hasEmotionSearch()) {
            query.append("emotion:").append(request.getEmotionType());
        } else if (request.hasLocationSearch()) {
            query.append("location:").append(request.getLocation());
        } else {
            query.append("default:completed_stories");
        }
        
        return query.toString();
    }
    
    /**
     * Convert Story.StoryStatus to StoryDataStore.ProcessingStatus
     */
    private StoryDataStore.ProcessingStatus convertToProcessingStatus(Story.StoryStatus storyStatus) {
        if (storyStatus == null) {
            return StoryDataStore.ProcessingStatus.COMPLETED; // Default to completed stories
        }
        
        return switch (storyStatus) {
            case UPLOAD_PENDING -> StoryDataStore.ProcessingStatus.UPLOAD_PENDING;
            case UPLOADING -> StoryDataStore.ProcessingStatus.UPLOADING;
            case PROCESSING_PENDING -> StoryDataStore.ProcessingStatus.PROCESSING_PENDING;
            case PROCESSING -> StoryDataStore.ProcessingStatus.PROCESSING;
            case PROCESSED -> StoryDataStore.ProcessingStatus.PROCESSED;
            case CONVERTING -> StoryDataStore.ProcessingStatus.CONVERTING;
            case ACTIVE -> StoryDataStore.ProcessingStatus.COMPLETED;
            case INACTIVE -> StoryDataStore.ProcessingStatus.COMPLETED;
            case FAILED -> StoryDataStore.ProcessingStatus.FAILED;
            case REJECTED -> StoryDataStore.ProcessingStatus.REJECTED;
        };
    }
} 