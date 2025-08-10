package com.breakupstories.controller;


import com.breakupstories.dto.BookmarkResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.dto.StoryResponse;
import com.breakupstories.service.AuditService;
import com.breakupstories.service.BookmarkService;
import com.breakupstories.service.ClientInfoService;
import com.breakupstories.service.StoryService;
import com.breakupstories.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Bookmarks", description = "Bookmark management APIs")
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final StoryService storyService;
    private final UserService userService;
    private final AuditService auditService;
    private final ClientInfoService clientInfoService;
    
    @PostMapping("/story/{storyId}")
    @Operation(summary = "Create a bookmark", description = "Bookmark a story for the authenticated user")
    public ResponseEntity<BookmarkResponse> createBookmark(
            @Valid @PathVariable String storyId,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String email = authentication.getName();
        String userId = userService.getUserEntityByEmail(email).getId();
        
        log.info("User {} creating bookmark for story {}", userId, storyId);
        
        // Verify story exists
        storyService.getStoryById(storyId);
        
        BookmarkResponse response = bookmarkService.createBookmark(userId, storyId);
        
        // Audit bookmark creation
        ClientInfoService.ClientInfo clientInfo = clientInfoService.extractClientInfo();
        auditService.logBookmarkCreate(userId, storyId, clientInfo.getUserAgent(),
                                     clientInfo.getIpAddress(), clientInfo.getSessionId());
        log.info("Audited bookmark creation for user {} on story {}", userId, storyId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    @Operation(summary = "Get user bookmarks", description = "Get paginated list of bookmarks for the authenticated user")
    public ResponseEntity<PagedResponse<BookmarkResponse>> getUserBookmarks(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String email = authentication.getName();
        String userId = userService.getUserEntityByEmail(email).getId();
        
        PagedResponse<BookmarkResponse> response = bookmarkService.getBookmarksByUser(userId, page, size);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/stories")
    @Operation(summary = "Get bookmarked stories", description = "Get paginated list of bookmarked stories with full story details")
    public ResponseEntity<PagedResponse<StoryResponse>> getBookmarkedStories(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String email = authentication.getName();
        String userId = userService.getUserEntityByEmail(email).getId();
        
        PagedResponse<StoryResponse> response = bookmarkService.getBookmarkedStoriesWithDetails(userId, page, size);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/check/{storyId}")
    @Operation(summary = "Check bookmark status", description = "Check if a story is bookmarked by the authenticated user")
    public ResponseEntity<Boolean> isBookmarked(
            @PathVariable String storyId,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String email = authentication.getName();
        String userId = userService.getUserEntityByEmail(email).getId();
        
        boolean isBookmarked = bookmarkService.isBookmarked(userId, storyId);
        return ResponseEntity.ok(isBookmarked);
    }
    
    @GetMapping("/count")
    @Operation(summary = "Get bookmark count", description = "Get the total number of bookmarks for the authenticated user")
    public ResponseEntity<Long> getBookmarkCount(Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String email = authentication.getName();
        String userId = userService.getUserEntityByEmail(email).getId();
        
        long count = bookmarkService.getBookmarkCountByUser(userId);
        return ResponseEntity.ok(count);
    }
    
    @DeleteMapping("/{bookmarkId}")
    @Operation(summary = "Delete bookmark by ID", description = "Delete a specific bookmark by its ID")
    public ResponseEntity<Void> deleteBookmark(
            @PathVariable String bookmarkId,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String email = authentication.getName();
        String userId = userService.getUserEntityByEmail(email).getId();
        
        log.info("User {} deleting bookmark {}", userId, bookmarkId);
        
        // Get bookmark details for auditing
        BookmarkResponse bookmark = bookmarkService.getBookmarkById(bookmarkId);
        
        bookmarkService.deleteBookmark(bookmarkId, userId);
        
        // Audit bookmark deletion
        ClientInfoService.ClientInfo clientInfo = clientInfoService.extractClientInfo();
        auditService.logBookmarkDelete(userId, bookmark.getStoryId(), clientInfo.getUserAgent(), 
                                     clientInfo.getIpAddress(), clientInfo.getSessionId());
        log.info("Audited bookmark deletion for user {} on story {}", userId, bookmark.getStoryId());
        
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/story/{storyId}")
    @Operation(summary = "Remove bookmark by story", description = "Remove bookmark for a specific story")
    public ResponseEntity<Void> removeBookmarkByStory(
            @PathVariable String storyId,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String email = authentication.getName();
        String userId = userService.getUserEntityByEmail(email).getId();
        
        log.info("User {} removing bookmark for story {}", userId, storyId);
        
        bookmarkService.deleteBookmarkByUserAndStory(userId, storyId);
        
        // Audit bookmark deletion
        ClientInfoService.ClientInfo clientInfo = clientInfoService.extractClientInfo();
        auditService.logBookmarkDelete(userId, storyId, clientInfo.getUserAgent(), 
                                     clientInfo.getIpAddress(), clientInfo.getSessionId());
        log.info("Audited bookmark deletion for user {} on story {}", userId, storyId);
        
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{bookmarkId}")
    @Operation(summary = "Get bookmark by ID", description = "Get a specific bookmark by its ID")
    public ResponseEntity<BookmarkResponse> getBookmarkById(
            @PathVariable String bookmarkId,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String email = authentication.getName();
        String userId = userService.getUserEntityByEmail(email).getId();
        
        BookmarkResponse bookmark = bookmarkService.getBookmarkById(bookmarkId);
        
        // Check if user owns this bookmark
        if (!bookmark.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(bookmark);
    }
} 