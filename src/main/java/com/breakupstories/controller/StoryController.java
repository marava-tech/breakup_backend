package com.breakupstories.controller;

import com.breakupstories.dto.LikeResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.dto.RequestIdResponse;
import com.breakupstories.dto.StoryResponse;
import com.breakupstories.dto.ConsolingMessageResponse;
import com.breakupstories.dto.WrittenStoryRequest;
import com.breakupstories.enums.StorySearchType;
import com.breakupstories.model.Story;
import com.breakupstories.model.User;
import com.breakupstories.repository.StoryRepository;
import com.breakupstories.service.AuditService;
import com.breakupstories.service.ClientInfoService;
import com.breakupstories.service.StoryService;
import com.breakupstories.service.UserService;
import com.breakupstories.service.AIService;
import com.breakupstories.service.StoryDataStoreService;
import com.breakupstories.util.RequestContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.HashMap;
import java.util.Map;


import com.breakupstories.model.StoryDataStore;

@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Stories", description = "Story management APIs")
public class StoryController {
    
    private final StoryService storyService;
    private final UserService userService;
    private final AuditService auditService;
    private final ClientInfoService clientInfoService;
    private final StoryRepository storyRepository;
    private final AIService aiService;
    private final StoryDataStoreService storyDataStoreService;
    
    @PostMapping
    @Operation(summary = "Create a new story", description = "Upload a new story with content, tags, and metadata")
    public ResponseEntity<RequestIdResponse<StoryResponse>> createStory(
            Authentication authentication,
            MultipartHttpServletRequest request,
            @RequestParam(required = false) String creationType) {

        String requestId = RequestContext.getRequestId();
        log.info("Story creation request received [RequestID: {}]", requestId);
        
        // Extract location coordinates from headers
        String latitude = request.getHeader("X-Latitude");
        String longitude = request.getHeader("X-Longitude");
        
        // Extract device info from request
        ClientInfoService.ClientInfo clientInfo = clientInfoService.extractClientInfo();
        String deviceInfo = clientInfo.getUserAgent(); // Use User-Agent as device info

        Map<String,String> uploadMetadata = new HashMap<>();
        uploadMetadata.put("lat",latitude);
        uploadMetadata.put("long",longitude);
        uploadMetadata.put("deviceInfo",deviceInfo);
        if (latitude != null && longitude != null) {
            log.info("Location coordinates received [RequestID: {}] - lat: {}, lng: {}", requestId, latitude, longitude);
        } else {
            log.info("No location coordinates provided in headers [RequestID: {}]", requestId);
        }
        
        //request contains a audio file.
        
        String email = authentication.getName();
        User user = userService.getUserEntityByEmail(email);
        if(ObjectUtils.isEmpty(user)) throw new RuntimeException("user not logged in");
        StoryResponse response = storyService.createStory(user, request, uploadMetadata, creationType);
        
        RequestIdResponse<StoryResponse> requestIdResponse = RequestIdResponse.of(response, "Story creation initiated successfully");
        log.info("Story creation response sent [RequestID: {}]", requestId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(requestIdResponse);
    }
    
    @PostMapping("/written")
    @Operation(summary = "Create a written story", description = "Create a new story from written text")
    public ResponseEntity<RequestIdResponse<StoryResponse>> createWrittenStory(
            Authentication authentication,
            @RequestBody WrittenStoryRequest request) {

        String requestId = RequestContext.getRequestId();
        log.info("Written story creation request received [RequestID: {}]", requestId);
        
        // Extract location coordinates from headers (if available)
        String latitude = null;
        String longitude = null;
        String deviceInfo = "Unknown";
        
        // Extract device info from request context
        ClientInfoService.ClientInfo clientInfo = clientInfoService.extractClientInfo();
        if (clientInfo != null) {
            deviceInfo = clientInfo.getUserAgent();
        }

        Map<String,String> uploadMetadata = new HashMap<>();
        uploadMetadata.put("lat", latitude);
        uploadMetadata.put("long", longitude);
        uploadMetadata.put("deviceInfo", deviceInfo);
        
        String email = authentication.getName();
        User user = userService.getUserEntityByEmail(email);
        if(ObjectUtils.isEmpty(user)) throw new RuntimeException("user not logged in");
        
        StoryResponse response = storyService.createWrittenStory(user, request, uploadMetadata);
        
        RequestIdResponse<StoryResponse> requestIdResponse = RequestIdResponse.of(response, "Written story creation initiated successfully");
        log.info("Written story creation response sent [RequestID: {}]", requestId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(requestIdResponse);
    }
    
    @GetMapping
    @Operation(summary = "Get all stories", description = "Retrieve paginated list of active stories with automatic language filtering")
    public ResponseEntity<PagedResponse<StoryResponse>> getStories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String language,
            Authentication authentication) {
        
        PagedResponse<StoryResponse> response;
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("login required");
        }
            String email = authentication.getName();
            User user = userService.getUserEntityByEmail(email);
            String userId = user.getId();
            String userPreferredLanguage = user.getPreferredStoryLanguage();
            
            // Use provided language or user's preferred language
            String filterLanguage = (language != null && !language.trim().isEmpty()) ? language : userPreferredLanguage;
            
            if (filterLanguage != null && !filterLanguage.trim().isEmpty()) {
                response = storyService.getStoriesByLanguage(filterLanguage, userId, page, size);
            } else {
                response = storyService.getStories(userId, page, size);
            }
        
        return ResponseEntity.ok(response);
    }


    @GetMapping("/type")
    @Operation(summary = "Get stories by search type", description = "Retrieve paginated list of stories based on search type with automatic language filtering")
    public ResponseEntity<PagedResponse<StoryResponse>> getStoriesByType(
            @RequestParam StorySearchType searchType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String storyId,
            @RequestParam(required = false) String language,
            HttpServletRequest request,
            Authentication authentication) {
        
        PagedResponse<StoryResponse> response;
        
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            User user = userService.getUserEntityByEmail(email);
            String userId = user.getId();
            String userPreferredLanguage = user.getPreferredStoryLanguage();
            
            // Use provided language or user's preferred language
            String filterLanguage = (language != null && !language.trim().isEmpty()) ? language : userPreferredLanguage;
            
            switch (searchType) {
                case FOR_YOU -> {
                    if (filterLanguage != null && !filterLanguage.trim().isEmpty()) {
                        response = storyService.getForYouStoriesByLanguage(filterLanguage, userId, page, size);
                    } else {
                        response = storyService.getForYouStories(userId, page, size);
                    }
                }
                case NEAR_ME -> {
                    response = storyService.getNearbyStories(userId, request, page, size);
                }
                case TRENDING -> {
                    if (filterLanguage != null && !filterLanguage.trim().isEmpty()) {
                        response = storyService.getTrendingStoriesByLanguage(filterLanguage, userId, page, size);
                    } else {
                        response = storyService.getTrendingStories(userId, page, size);
                    }
                }
                case SIMILAR -> {
                    if (filterLanguage != null && !filterLanguage.trim().isEmpty()) {
                        response = storyService.getSimilarStoriesByLanguage(filterLanguage, userId, page, size);
                    } else {
                        response = storyService.getSimilarStories(userId, page, size);
                    }
                }
                case LATEST -> {
                    if (filterLanguage != null && !filterLanguage.trim().isEmpty()) {
                        response = storyService.getLatestStoriesByLanguage(filterLanguage, userId, page, size);
                    } else {
                        response = storyService.getLatestStories(userId, page, size);
                    }
                }
                case LANGUAGE -> {
                    if (language != null && !language.trim().isEmpty()) {
                        response = storyService.getStoriesByLanguage(language, userId, page, size);
                    } else {
                        response = storyService.getStories(userId, page, size);
                    }
                }
                case GENERAL -> {
                    if (filterLanguage != null && !filterLanguage.trim().isEmpty()) {
                        response = storyService.getStoriesByLanguage(filterLanguage, userId, page, size);
                    } else {
                        response = storyService.getStories(userId, page, size);
                    }
                }
                default -> {
                    if (filterLanguage != null && !filterLanguage.trim().isEmpty()) {
                        response = storyService.getStoriesByLanguage(filterLanguage, userId, page, size);
                    } else {
                        response = storyService.getStories(userId, page, size);
                    }
                }
            }
        } else {
            // For unauthenticated users - no language filtering by default
            switch (searchType) {
                case TRENDING -> {
                    if (language != null && !language.trim().isEmpty()) {
                        response = storyService.getTrendingStoriesByLanguage(language, page, size);
                    } else {
                        response = storyService.getTrendingStories(page, size);
                    }
                }
                case LATEST -> {
                    if (language != null && !language.trim().isEmpty()) {
                        response = storyService.getLatestStoriesByLanguage(language, page, size);
                    } else {
                        response = storyService.getLatestStories(page, size);
                    }
                }
                case LANGUAGE -> {
                    if (language != null && !language.trim().isEmpty()) {
                        response = storyService.getStoriesByLanguage(language, page, size);
                    } else {
                        response = storyService.getStories(page, size);
                    }
                }
                case GENERAL -> {
                    response = storyService.getStories(page, size);
                }
                default -> response = storyService.getStories(page, size);
            }
        }
        
        return ResponseEntity.ok(response);
    }



    @GetMapping("/{storyId}")
    @Operation(summary = "Get story by ID", description = "Retrieve a specific story by its ID with like status")
    public ResponseEntity<StoryResponse> getStoryById(
            @PathVariable String storyId,
            Authentication authentication) {
        
        StoryResponse response;
        String userId = null;
        String ipAddress = null;
        
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            userId = userService.getUserEntityByEmail(email).getId();
            response = storyService.getStoryById(storyId, userId);
        } else {
            response = storyService.getStoryById(storyId);
        }
        
        // Get client info for IP-based cooldown check
        ClientInfoService.ClientInfo clientInfo = clientInfoService.extractClientInfo();
        ipAddress = clientInfo.getIpAddress();
        
        // Check if user has viewed this story recently (1-minute cooldown)
        boolean hasViewedRecently = false;
        if (userId != null) {
            hasViewedRecently = auditService.hasViewedStoryRecently(userId, storyId);
        } else if (ipAddress != null) {
            hasViewedRecently = auditService.hasViewedStoryRecentlyByIP(ipAddress, storyId);
        }
        
        if (hasViewedRecently) {
            log.info("View skipped due to 1-minute cooldown - story: {}, user: {}, ip: {}", storyId, userId, ipAddress);
            return ResponseEntity.ok(response);
        }
        
        // Check if user is viewing their own story
        boolean isOwnStory = false;
        if (userId != null) {
            Story story = storyRepository.findById(storyId).orElse(null);
            if (story != null) {
                isOwnStory = userId.equals(story.getUserId());
            }
        }
        
        // Only increment view count if user is not viewing their own story and hasn't viewed recently
        if (!isOwnStory) {
            storyService.incrementViewCount(storyId);
            log.info("View count incremented for story: {} (viewed by user: {}, ip: {})", storyId, userId, ipAddress);
        } else {
            log.info("View count not incremented for story: {} (user viewing their own story: {})", storyId, userId);
        }
        
        // Audit story view (only if not viewed recently and not own story)
        if (userId != null) {
            auditService.logStoryView(userId, storyId, clientInfo.getUserAgent(), 
                                    clientInfo.getIpAddress(), clientInfo.getSessionId(), isOwnStory);
            log.info("Audited story view for user {} on story {} (own story: {})", userId, storyId, isOwnStory);
        } else if (ipAddress != null) {
            // For unauthenticated users, log with IP address
            auditService.logStoryViewByIP(ipAddress, storyId, clientInfo.getUserAgent(), 
                                        clientInfo.getSessionId());
            log.info("Audited story view for IP {} on story {}", ipAddress, storyId);
        }
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{storyId}/like")
    @Operation(summary = "Like a story", description = "Like a story by the authenticated user")
    public ResponseEntity<LikeResponse> likeStory(
            @PathVariable String storyId,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String email = authentication.getName();
        String userId = userService.getUserEntityByEmail(email).getId();
        
        log.info("User {} attempting to like story {}", userId, storyId);
        LikeResponse response = storyService.likeStory(userId, storyId);
        
        // Audit story like
        ClientInfoService.ClientInfo clientInfo = clientInfoService.extractClientInfo();
        auditService.logStoryLike(userId, storyId, clientInfo.getUserAgent(), 
                                clientInfo.getIpAddress(), clientInfo.getSessionId());
        log.info("Audited story like for user {} on story {}", userId, storyId);
        
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{storyId}/like")
    @Operation(summary = "Unlike a story", description = "Unlike a story by the authenticated user")
    public ResponseEntity<Void> unlikeStory(
            @PathVariable String storyId,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String email = authentication.getName();
        String userId = userService.getUserEntityByEmail(email).getId();
        
        log.info("User {} attempting to unlike story {}", userId, storyId);
        storyService.unlikeStory(userId, storyId);
        
        // Audit story unlike
        ClientInfoService.ClientInfo clientInfo = clientInfoService.extractClientInfo();
        auditService.logStoryUnlike(userId, storyId, clientInfo.getUserAgent(), 
                                  clientInfo.getIpAddress(), clientInfo.getSessionId());
        log.info("Audited story unlike for user {} on story {}", userId, storyId);
        
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/liked")
    @Operation(summary = "Get liked stories", description = "Get stories liked by the authenticated user")
    public ResponseEntity<PagedResponse<StoryResponse>> getLikedStories(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("login required");
        }
        
        String email = authentication.getName();
        User user = userService.getUserEntityByEmail(email);
        String userId = user.getId();
        
        PagedResponse<StoryResponse> response = storyService.getLikedStories(userId, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-stories")
    @Operation(summary = "Get my stories", description = "Get all stories uploaded, written, or recorded by the authenticated user without any filtering")
    public ResponseEntity<PagedResponse<StoryResponse>> getMyStories(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("login required");
        }
        
        String email = authentication.getName();
        User user = userService.getUserEntityByEmail(email);
        String userId = user.getId();
        
        PagedResponse<StoryResponse> response = storyService.getStories(userId, page, size);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{storyId}/like-count")
    @Operation(summary = "Get like count", description = "Get the number of likes for a story")
    public ResponseEntity<Long> getLikeCount(@PathVariable String storyId) {
        long likeCount = storyService.getLikeCount(storyId);
        return ResponseEntity.ok(likeCount);
    }

    @GetMapping("/search")
    @Operation(summary = "Search stories by content", description = "Search stories by title and tags using unified search content (case-insensitive)")
    public ResponseEntity<PagedResponse<StoryResponse>> searchStoriesByContent(
            @RequestParam String searchContent,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        
        try {
            String requestId = RequestContext.getRequestId();
            log.info("Search request - searchContent: {}, page: {}, size: {} [RequestID: {}]", 
                    searchContent, page, size, requestId);
            
            PagedResponse<StoryResponse> response;
            if (authentication != null && authentication.isAuthenticated()) {
                String email = authentication.getName();
                String userId = userService.getUserEntityByEmail(email).getId();
                response = storyService.searchStoriesByContent(searchContent, userId, page, size);
            } else {
                response = storyService.searchStoriesByContent(searchContent, null, page, size);
            }
            
            log.info("Search completed successfully. Found {} results [RequestID: {}]", 
                    response.getContent().size(), requestId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error searching stories: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{storyId}/consoling-message")
    @Operation(summary = "Generate consoling message for story", description = "Generate a consoling message for a story using story ID and console_by parameter")
    public ResponseEntity<ConsolingMessageResponse> generateConsolingMessageForStory(
            @PathVariable String storyId,
            @RequestParam String consoleBy,
            Authentication authentication) {
        
        log.info("Generating consoling message for story: {} with consoleBy: {}", storyId, consoleBy);
        
        try {
            // Get current user from authentication
            String email = authentication.getName();
            User currentUser = userService.getUserEntityByEmail(email);
            
            // Get story data store to extract transcription and language
            StoryDataStore dataStore = storyDataStoreService.getDataStoreByStoryId(storyId)
                    .orElseThrow(() -> new RuntimeException("Story data store not found for story ID: " + storyId));
            
            // Extract transcription from StoryDataStore
            String story = null;
            if (dataStore.getTranscriptionResponse() != null && dataStore.getTranscriptionResponse().getTranscript() != null) {
                story = dataStore.getTranscriptionResponse().getTranscript();
            } else if (dataStore.getStoryRewriteResponse() != null && dataStore.getStoryRewriteResponse().getRewrittenStory() != null) {
                story = dataStore.getStoryRewriteResponse().getRewrittenStory();
            } else {
                throw new RuntimeException("No story content available for story ID: " + storyId);
            }
            
            // Get language from StoryDataStore
            String language = dataStore.getLanguage();
            if (language == null || language.trim().isEmpty()) {
                language = "en"; // Default to English if not available
            }
            
            // Get user gender and age
            String gender = currentUser.getGender() != null ? currentUser.getGender().toString().toLowerCase() : "unknown";
            Integer age = currentUser.getAge();
            if (age == null) {
                age = 25; // Default age if not available
            }
            
            log.info("Extracted data for consoling message - Language: {}, Gender: {}, Age: {}, ConsoleBy: {}", 
                    language, gender, age, consoleBy);
            
            // Generate consoling message using AI service
            ConsolingMessageResponse response = aiService.generateConsolingMessage(story, language, gender, age, consoleBy);
            
            log.info("Consoling message generated successfully for story: {}", storyId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error generating consoling message for story {}: {}", storyId, e.getMessage(), e);
            
            ConsolingMessageResponse errorResponse = ConsolingMessageResponse.builder()
                    .success(false)
                    .consolingMessage(null)
                    .language("en")
                    .consoleBy(consoleBy)
                    .error("Failed to generate consoling message: " + e.getMessage())
                    .build();
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/test-story-images/{storyId}")
    public ResponseEntity<Map<String, Object>> testStoryImages(@PathVariable String storyId) {
        try {
            StoryResponse storyResponse = storyService.getStoryById(storyId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("storyId", storyId);
            response.put("storyImages", storyResponse.getStoryImages());
            response.put("imageCount", storyResponse.getStoryImages() != null ? storyResponse.getStoryImages().size() : 0);
            response.put("message", "Story images retrieved successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "Failed to retrieve story images");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
} 