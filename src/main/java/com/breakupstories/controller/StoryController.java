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
import com.breakupstories.service.ListeningProgressService;
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
import java.util.List;
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
    private final ListeningProgressService listeningProgressService;

    @PostMapping
    @Operation(summary = "Create a new story", description = "Upload a new story with content, tags, and metadata")
    public ResponseEntity<RequestIdResponse<StoryResponse>> createStory(
            Authentication authentication,
            MultipartHttpServletRequest request,
            @RequestParam(required = false) String creationType,
            @RequestParam(required = false) String category) {

        String requestId = RequestContext.getRequestId();
        log.info("Story creation request received [RequestID: {}]", requestId);

        // Extract device info from request
        ClientInfoService.ClientInfo clientInfo = clientInfoService.extractClientInfo();
        String deviceInfo = clientInfo.getUserAgent(); // Use User-Agent as device info

        Map<String, String> uploadMetadata = new HashMap<>();
        uploadMetadata.put("deviceInfo", deviceInfo);

        // Add category to upload metadata for processing
        if (category != null && !category.trim().isEmpty()) {
            uploadMetadata.put("category", category);
        }

        // request contains a audio file.

        String email = authentication.getName();
        User user = userService.getUserEntityByEmail(email);
        if (ObjectUtils.isEmpty(user))
            throw new RuntimeException("user not logged in");
        StoryResponse response = storyService.createStory(user, request, uploadMetadata, creationType);

        RequestIdResponse<StoryResponse> requestIdResponse = RequestIdResponse.of(response,
                "Story creation initiated successfully");
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

        // Extract device info from request context
        String deviceInfo = "Unknown";
        ClientInfoService.ClientInfo clientInfo = clientInfoService.extractClientInfo();
        if (clientInfo != null) {
            deviceInfo = clientInfo.getUserAgent();
        }

        Map<String, String> uploadMetadata = new HashMap<>();
        uploadMetadata.put("deviceInfo", deviceInfo);

        String email = authentication.getName();
        User user = userService.getUserEntityByEmail(email);
        if (ObjectUtils.isEmpty(user))
            throw new RuntimeException("user not logged in");

        StoryResponse response = storyService.createWrittenStory(user, request, uploadMetadata);

        RequestIdResponse<StoryResponse> requestIdResponse = RequestIdResponse.of(response,
                "Written story creation initiated successfully");
        log.info("Written story creation response sent [RequestID: {}]", requestId);

        return ResponseEntity.status(HttpStatus.CREATED).body(requestIdResponse);
    }

    @GetMapping
    @Operation(summary = "Get all stories", description = "Retrieve paginated list of active stories. Supports category and language filters.")
    public ResponseEntity<PagedResponse<StoryResponse>> getStories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String cursor,
            Authentication authentication) {

        PagedResponse<StoryResponse> response;
        String userId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            userId = userService.getUserEntityByEmail(authentication.getName()).getId();
        }

        if (cursor != null && !cursor.isBlank()) {
            response = storyService.getStoriesWithCursor(userId, language, category, cursor, size);
        } else {
            response = storyService.getStories(userId, page, size, language, category);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured stories")
    public ResponseEntity<PagedResponse<StoryResponse>> getFeaturedStories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String language,
            Authentication authentication) {
        String userId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            userId = userService.getUserEntityByEmail(authentication.getName()).getId();
        }
        return ResponseEntity.ok(storyService.getFeaturedStories(userId, language, page, size));
    }

    @GetMapping("/most-listened")
    @Operation(summary = "Get most listened stories")
    public ResponseEntity<PagedResponse<StoryResponse>> getMostListenedStories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String language,
            Authentication authentication) {
        String userId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            userId = userService.getUserEntityByEmail(authentication.getName()).getId();
        }
        return ResponseEntity.ok(storyService.getMostListenedStories(userId, language, page, size));
    }

    @GetMapping("/type")
    @Operation(summary = "Get stories by search type", description = "Retrieve paginated list of stories. Use cursor for efficient LATEST/GENERAL pagination.")
    public ResponseEntity<PagedResponse<StoryResponse>> getStoriesByType(
            @RequestParam StorySearchType searchType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String storyId,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String category,
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
                case TRENDING -> {
                    if (filterLanguage != null && !filterLanguage.trim().isEmpty()) {
                        response = storyService.getTrendingStoriesByLanguage(filterLanguage, userId, page, size);
                    } else {
                        response = storyService.getTrendingStories(userId, page, size);
                    }
                }
                case SIMILAR -> {
                    if (storyId == null || storyId.isBlank()) {
                        // no source story — fall back to trending
                        if (filterLanguage != null && !filterLanguage.trim().isEmpty()) {
                            response = storyService.getTrendingStoriesByLanguage(filterLanguage, userId, page, size);
                        } else {
                            response = storyService.getTrendingStories(userId, page, size);
                        }
                    } else if (filterLanguage != null && !filterLanguage.trim().isEmpty()) {
                        response = storyService.getSimilarStoriesByLanguage(storyId, filterLanguage, userId, page,
                                size);
                    } else {
                        response = storyService.getSimilarStories(storyId, userId, page, size);
                    }
                }
                case LATEST -> {
                    if (cursor != null && !cursor.isBlank()) {
                        response = storyService.getStoriesWithCursor(userId, filterLanguage, category, cursor, size);
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
                case CURATED -> {
                    response = storyService.getCuratedStories(userId, page, size);
                }
                case RECOMMENDATION -> {
                    if (filterLanguage != null && !filterLanguage.trim().isEmpty()) {
                        response = storyService.getForYouStoriesByLanguage(filterLanguage, userId, page, size);
                    } else {
                        response = storyService.getForYouStories(userId, page, size);
                    }
                }
                case GENERAL -> {
                    if (cursor != null && !cursor.isBlank()) {
                        response = storyService.getStoriesWithCursor(userId, filterLanguage, category, cursor, size);
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
                case CURATED -> {
                    response = storyService.getCuratedStories(null, page, size);
                }
                case RECOMMENDATION, FOR_YOU -> {
                    if (language != null && !language.trim().isEmpty()) {
                        response = storyService.getTrendingStoriesByLanguage(language, page, size);
                    } else {
                        response = storyService.getTrendingStories(page, size);
                    }
                }
                case TRENDING -> {
                    if (language != null && !language.trim().isEmpty()) {
                        response = storyService.getTrendingStoriesByLanguage(language, page, size);
                    } else {
                        response = storyService.getTrendingStories(page, size);
                    }
                }
                case LATEST -> {
                    if (cursor != null && !cursor.isBlank()) {
                        response = storyService.getStoriesWithCursor(null, language, category, cursor, size);
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
                        response = storyService.getStoriesWithCursor(null, language, category, cursor, size);
                    } else {
                        response = storyService.getStories(page, size);
                    }
                }
                default -> {
                    if (cursor != null && !cursor.isBlank()) {
                        response = storyService.getStoriesWithCursor(null, language, category, cursor, size);
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

        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            userId = userService.getUserEntityByEmail(email).getId();
            response = storyService.getStoryById(storyId, userId);
        } else {
            response = storyService.getStoryById(storyId);
        }

        ClientInfoService.ClientInfo clientInfo = clientInfoService.extractClientInfo();

        // Determine self-view once — used by both view counter and audit
        boolean isOwnStory = false;
        if (userId != null) {
            Story story = storyRepository.findById(storyId).orElse(null);
            if (story != null) {
                isOwnStory = userId.equals(story.getUserId());
            }
        }

        // Async: deduplicated view count (Redis INCR → MongoDB batch every 60 s)
        storyService.recordViewAsync(storyId, userId, clientInfo.getIpAddress(), isOwnStory);

        // Async: audit log for analytics
        if (userId != null) {
            auditService.logStoryViewAsync(userId, storyId, clientInfo.getUserAgent(),
                    clientInfo.getIpAddress(), clientInfo.getSessionId(), isOwnStory);
        } else if (clientInfo.getIpAddress() != null) {
            auditService.logStoryViewByIPAsync(clientInfo.getIpAddress(), storyId,
                    clientInfo.getUserAgent(), clientInfo.getSessionId());
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
            @RequestParam(required = false) String language,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        try {
            String requestId = RequestContext.getRequestId();
            log.info("Search request - searchContent: {}, language: {}, page: {}, size: {} [RequestID: {}]",
                    searchContent, language, page, size, requestId);

            PagedResponse<StoryResponse> response;
            if (authentication != null && authentication.isAuthenticated()) {
                String email = authentication.getName();
                String userId = userService.getUserEntityByEmail(email).getId();
                response = storyService.searchStoriesByContent(searchContent, language, userId, page, size);
            } else {
                response = storyService.searchStoriesByContent(searchContent, language, null, page, size);
            }

            log.info("Search completed successfully. Found {} results [RequestID: {}]",
                    response.getContent().size(), requestId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error searching stories: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/resume")
    @Operation(summary = "Get stories to resume", description = "Get stories started but not completed, sorted by most recently updated.")
    public ResponseEntity<PagedResponse<StoryResponse>> getResumeStories(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.ok(PagedResponse.of(java.util.Collections.emptyList(), page, size, 0));
        }

        String email = authentication.getName();
        String userId = userService.getUserEntityByEmail(email).getId();

        List<StoryResponse> stories = listeningProgressService.getResumeStories(userId, page, size);
        return ResponseEntity.ok(PagedResponse.of(stories, page, size, stories.size())); // Total size might be
                                                                                         // inaccurate if we filter, but
                                                                                         // acceptable for resume list
    }

    @GetMapping("/next")
    @Operation(summary = "Get next story for autoplay", description = "Get the next recommended story based on the current story ID.")
    public ResponseEntity<StoryResponse> getNextStory(
            @RequestParam String currentId,
            @RequestParam(required = false) String language,
            Authentication authentication) {

        String userId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            userId = userService.getUserEntityByEmail(authentication.getName()).getId();
        }

        StoryResponse nextStory = storyService.getNextStory(currentId, userId, language);
        return ResponseEntity.ok(nextStory);
    }

}