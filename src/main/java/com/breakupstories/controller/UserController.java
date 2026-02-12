package com.breakupstories.controller;

import com.breakupstories.dto.UserResponse;
import com.breakupstories.dto.UserProfileResponse;
import com.breakupstories.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
    
    @GetMapping("/profile")
    @Operation(summary = "Get user profile", description = "Retrieve user profile with statistics")
    public ResponseEntity<UserProfileResponse> getUserProfile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthenticated access attempt to get user profile");
            throw new BadCredentialsException("User not authenticated");
        }
        
        String userEmail = authentication.getName();
        log.info("Retrieving profile for authenticated user: {}", userEmail);
        
        UserProfileResponse response = userService.getUserProfile(userEmail);
        return ResponseEntity.ok(response);
    }
    
} 