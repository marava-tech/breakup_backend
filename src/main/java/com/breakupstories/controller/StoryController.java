package com.breakupstories.controller;

import com.breakupstories.dto.LikeResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.dto.StoryResponse;
import com.breakupstories.service.AuditService;
import com.breakupstories.service.ClientInfoService;
import com.breakupstories.service.StoryService;
import com.breakupstories.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;

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
    
    @PostMapping
    @Operation(summary = "Create a new story", description = "Upload a new story with content, tags, and metadata")
    public ResponseEntity<StoryResponse> createStory(
            Authentication authentication,
            MultipartHttpServletRequest request) {

        //request contains a audio file.
        
        String email = authentication.getName();
        String userId = userService.getUserEntityByEmail(email).getId();
        
        StoryResponse response = storyService.createStory(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    @Operation(summary = "Get all stories", description = "Retrieve paginated list of active stories")
    public ResponseEntity<PagedResponse<StoryResponse>> getStories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        
        PagedResponse<StoryResponse> response;
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            String userId = userService.getUserEntityByEmail(email).getId();
            response = storyService.getStories(userId, page, size);
        } else {
            response = storyService.getStories(page, size);
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/trending")
    @Operation(summary = "Get trending stories", description = "Retrieve paginated list of active stories sorted by view count")
    public ResponseEntity<PagedResponse<StoryResponse>> getTrendingStories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        PagedResponse<StoryResponse> response;
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            String userId = userService.getUserEntityByEmail(email).getId();
            response = storyService.getTrendingStories(userId, page, size);
        } else {
            response = storyService.getTrendingStories(page, size);
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
        
        // Increment view count for the story
        storyService.incrementViewCount(storyId);
        log.info("View count incremented for story: {}", storyId);
        
        // Audit story view
        if (userId != null) {
            ClientInfoService.ClientInfo clientInfo = clientInfoService.extractClientInfo();
            auditService.logStoryView(userId, storyId, clientInfo.getUserAgent(), 
                                    clientInfo.getIpAddress(), clientInfo.getSessionId());
            log.info("Audited story view for user {} on story {}", userId, storyId);
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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String email = authentication.getName();
        String userId = userService.getUserEntityByEmail(email).getId();
        
        PagedResponse<StoryResponse> response = storyService.getLikedStories(userId, page, size);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{storyId}/like-count")
    @Operation(summary = "Get like count", description = "Get the number of likes for a story")
    public ResponseEntity<Long> getLikeCount(@PathVariable String storyId) {
        long likeCount = storyService.getLikeCount(storyId);
        return ResponseEntity.ok(likeCount);
    }
    
    @GetMapping("/language/{language}")
    @Operation(summary = "Get stories by language", description = "Retrieve paginated list of stories in a specific language")
    public ResponseEntity<PagedResponse<StoryResponse>> getStoriesByLanguage(
            @PathVariable String language,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        
        PagedResponse<StoryResponse> response;
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            String userId = userService.getUserEntityByEmail(email).getId();
            response = storyService.getStoriesByLanguage(language, userId, page, size);
        } else {
            response = storyService.getStoriesByLanguage(language, page, size);
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/preferred-language")
    @Operation(summary = "Get stories by user's preferred language", description = "Retrieve paginated list of stories in the user's preferred language")
    public ResponseEntity<PagedResponse<StoryResponse>> getStoriesByPreferredLanguage(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String email = authentication.getName();
        String userId = userService.getUserEntityByEmail(email).getId();
        
        PagedResponse<StoryResponse> response = storyService.getStoriesByUserPreferredLanguage(userId, page, size);
        return ResponseEntity.ok(response);
    }
} 