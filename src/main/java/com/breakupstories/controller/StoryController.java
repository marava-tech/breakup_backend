package com.breakupstories.controller;

import com.breakupstories.dto.LikeResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.dto.RequestIdResponse;
import com.breakupstories.dto.StoryResponse;
import com.breakupstories.dto.WrittenStoryRequest;
import com.breakupstories.enums.StorySearchType;
import com.breakupstories.model.Story;
import com.breakupstories.model.User;
import com.breakupstories.repository.StoryRepository;
import com.breakupstories.service.AuditService;
import com.breakupstories.service.ClientInfoService;
import com.breakupstories.service.StoryService;
import com.breakupstories.service.UserService;

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
    @Operation(summary = "Get all stories", description = "Retrieve paginated list of active stories. Use cursor param for efficient deep pagination.")
    public ResponseEntity<PagedResponse<StoryResponse>> getStories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String cursor,
            Authentication authentication) {
        
        PagedResponse<StoryResponse> response;
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("login required");
        }
        String email = authentication.getName();
        User user = userService.getUserEntityByEmail(email);
        String userId = user.getId();
        String filterLanguage = (language != null && !language.trim().isEmpty()) ? language : user.getPreferredStoryLanguage();
        
        if (cursor != null && !cursor.isBlank()) {
            response = storyService.getLatestStoriesWithCursor(userId, cursor, size);
        } else if (filterLanguage != null && !filterLanguage.trim().isEmpty()) {
            response = storyService.getStoriesByLanguage(filterLanguage, userId, page, size);
        } else {
            response = storyService.getStories(userId, page, size);
        }
        return ResponseEntity.ok(response);
    }


    @GetMapping("/type")
    @Operation(summary = "Get stories by search type", description = "Retrieve paginated list of stories. Use cursor for efficient LATEST/GENERAL pagination.")
    public ResponseEntity<PagedResponse<StoryResponse>> getStoriesByType(
            @RequestParam StorySearchType searchType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String storyId,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String cursor,
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
                    if (cursor != null && !cursor.isBlank()) {
                        response = storyService.getLatestStoriesWithCursor(userId, cursor, size);
                    } else if (filterLanguage != null && !filterLanguage.trim().isEmpty()) {
                        response = storyService.getLatestStoriesByLanguage(filterLanguage, userId, page, size);
                    } else {
                        response = storyService.getLatestStories(userId, page, size);
                    }
                }
                case VOICE -> {
                    if (filterLanguage != null && !filterLanguage.trim().isEmpty()) {
                        response = storyService.getVoiceStoriesByLanguage(filterLanguage, userId, page, size);
                    } else {
                        response = storyService.getVoiceStories(userId, page, size);
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
                    if (cursor != null && !cursor.isBlank()) {
                        response = storyService.getLatestStoriesWithCursor(userId, cursor, size);
                    } else if (filterLanguage != null && !filterLanguage.trim().isEmpty()) {
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
                    if (cursor != null && !cursor.isBlank()) {
                        response = storyService.getLatestStoriesWithCursor(null, cursor, size);
                    } else if (language != null && !language.trim().isEmpty()) {
                        response = storyService.getLatestStoriesByLanguage(language, page, size);
                    } else {
                        response = storyService.getLatestStories(page, size);
                    }
                }
                case VOICE -> {
                    if (language != null && !language.trim().isEmpty()) {
                        response = storyService.getVoiceStoriesByLanguage(language, page, size);
                    } else {
                        response = storyService.getVoiceStories(page, size);
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
                    if (cursor != null && !cursor.isBlank()) {
                        response = storyService.getLatestStoriesWithCursor(null, cursor, size);
                    } else {
                        response = storyService.getStories(page, size);
                    }
                }
                default -> {
                    if (cursor != null && !cursor.isBlank()) {
                        response = storyService.getLatestStoriesWithCursor(null, cursor, size);
                    } else {
                        response = storyService.getStories(page, size);
                    }
                }
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
        
        // COMMENTED OUT: Check if user has viewed this story recently (1-minute cooldown)
        // boolean hasViewedRecently = false;
        // if (userId != null) {
        //     hasViewedRecently = auditService.hasViewedStoryRecently(userId, storyId);
        // } else if (ipAddress != null) {
        //     hasViewedRecently = auditService.hasViewedStoryRecentlyByIP(ipAddress, storyId);
        // }
        // 
        // if (hasViewedRecently) {
        //     log.info("View skipped due to 1-minute cooldown - story: {}, user: {}, ip: {}", storyId, userId, ipAddress);
        //     return ResponseEntity.ok(response);
        // }
        
        // COMMENTED OUT: Check if user is viewing their own story
        // boolean isOwnStory = false;
        // if (userId != null) {
        //     Story story = storyRepository.findById(storyId).orElse(null);
        //     if (story != null) {
        //         isOwnStory = userId.equals(story.getUserId());
        //     }
        // }
        
        // Async: increment view count and audit — non-blocking, removes DB writes from response path
        storyService.incrementViewCountAsync(storyId);
        log.debug("View count increment scheduled for story: {} (viewed by user: {}, ip: {})", storyId, userId, ipAddress);
        
        // COMMENTED OUT: Only increment view count if user is not viewing their own story and hasn't viewed recently
        // if (!isOwnStory) {
        //     storyService.incrementViewCount(storyId);
        //     log.info("View count incremented for story: {} (viewed by user: {}, ip: {})", storyId, userId, ipAddress);
        // } else {
        //     log.info("View count not incremented for story: {} (user viewing their own story: {})", storyId, userId);
        // }
        
        // Audit story view (async — non-blocking)
        boolean isOwnStory = false;
        if (userId != null) {
            Story story = storyRepository.findById(storyId).orElse(null);
            if (story != null) {
                isOwnStory = userId.equals(story.getUserId());
            }
            auditService.logStoryViewAsync(userId, storyId, clientInfo.getUserAgent(), 
                    clientInfo.getIpAddress(), clientInfo.getSessionId(), isOwnStory);
            log.debug("Story view audit scheduled for user {} on story {}", userId, storyId);
        } else if (ipAddress != null) {
            auditService.logStoryViewByIPAsync(ipAddress, storyId, clientInfo.getUserAgent(), 
                    clientInfo.getSessionId());
            log.debug("Story view audit scheduled for IP {} on story {}", ipAddress, storyId);
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
        
        // Audit story like (async)
        ClientInfoService.ClientInfo clientInfo = clientInfoService.extractClientInfo();
        auditService.logStoryLikeAsync(userId, storyId, clientInfo.getUserAgent(), 
                clientInfo.getIpAddress(), clientInfo.getSessionId());
        
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
        
        // Audit story unlike (async)
        ClientInfoService.ClientInfo clientInfo = clientInfoService.extractClientInfo();
        auditService.logStoryUnlikeAsync(userId, storyId, clientInfo.getUserAgent(), 
                clientInfo.getIpAddress(), clientInfo.getSessionId());
        
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/my-stories")
    @Operation(summary = "Get my stories", description = "Get all stories uploaded, written, or recorded by the authenticated user with status from Story entity only")
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
        
        PagedResponse<StoryResponse> response = storyService.getMyStories(userId, page, size);
        return ResponseEntity.ok(response);
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



} 