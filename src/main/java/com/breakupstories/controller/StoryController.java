package com.breakupstories.controller;

import com.breakupstories.dto.CreateStoryRequest;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.dto.StoryResponse;
import com.breakupstories.service.StoryService;
import com.breakupstories.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
@Tag(name = "Stories", description = "Story management APIs")
public class StoryController {
    
    private final StoryService storyService;
    private final UserService userService;
    
    @PostMapping
    @Operation(summary = "Create a new story", description = "Upload a new story with content, tags, and metadata")
    public ResponseEntity<StoryResponse> createStory(
            Authentication authentication,
            @Valid @RequestBody CreateStoryRequest request) {
        
        String email = authentication.getName();
        String userId = userService.getUserEntityByEmail(email).getId();
        
        StoryResponse response = storyService.createStory(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    @Operation(summary = "Get all stories", description = "Retrieve paginated list of active stories")
    public ResponseEntity<PagedResponse<StoryResponse>> getStories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        PagedResponse<StoryResponse> response = storyService.getStories(page, size);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{storyId}")
    @Operation(summary = "Get story by ID", description = "Retrieve a specific story by its ID")
    public ResponseEntity<StoryResponse> getStoryById(@PathVariable String storyId) {
        StoryResponse response = storyService.getStoryById(storyId);
        return ResponseEntity.ok(response);
    }
} 