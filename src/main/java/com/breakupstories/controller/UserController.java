package com.breakupstories.controller;

import com.breakupstories.dto.PagedResponse;
import com.breakupstories.dto.UserRequest;
import com.breakupstories.dto.UserResponse;
import com.breakupstories.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "User management APIs")
public class UserController {
    
    private final UserService userService;
    
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get all users", description = "Retrieve paginated list of all users")
    public ResponseEntity<PagedResponse<UserResponse>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        PagedResponse<UserResponse> response = userService.getUsers(page, size);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", description = "Retrieve a specific user by their ID")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String userId) {
        UserResponse response = userService.getUserById(userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/email/{email}")
    @Operation(summary = "Get user by email", description = "Retrieve a specific user by their email")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        UserResponse response = userService.getUserByEmail(email);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{userId}")
    @Operation(summary = "Update user", description = "Update an existing user's information")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UserRequest request) {
        
        UserResponse response = userService.updateUser(userId, request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/profile-image")
    @Operation(summary = "Update profile image", description = "Update the current user's profile image")
    public ResponseEntity<UserResponse> updateProfileImage(
            Authentication authentication,
            @RequestParam("image") MultipartFile imageFile) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthenticated access attempt to update profile image");
            throw new BadCredentialsException("User not authenticated");
        }
        
        String userEmail = authentication.getName();
        log.info("Updating profile image for authenticated user: {}", userEmail);
        
        UserResponse response = userService.updateProfileImage(userEmail, imageFile);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Delete user", description = "Delete a user by their ID")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
    
    @PutMapping("/preferred-language")
    @Operation(summary = "Update preferred story language", description = "Update the current user's preferred story language")
    public ResponseEntity<UserResponse> updatePreferredStoryLanguage(
            Authentication authentication,
            @RequestParam String preferredStoryLanguage) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthenticated access attempt to update preferred story language");
            throw new BadCredentialsException("User not authenticated");
        }
        
        String userEmail = authentication.getName();
        log.info("Updating preferred story language for authenticated user: {} -> {}", 
            userEmail, preferredStoryLanguage);
        
        UserResponse response = userService.updatePreferredStoryLanguage(userEmail, preferredStoryLanguage);
        return ResponseEntity.ok(response);
    }
} 