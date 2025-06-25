package com.breakupstories.controller;

import com.breakupstories.dto.AuthResponse;
import com.breakupstories.dto.UserRequest;
import com.breakupstories.dto.UserResponse;
import com.breakupstories.service.JwtService;
import com.breakupstories.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and authorization APIs")
public class AuthController {
    
    private final UserService userService;
    private final JwtService jwtService;
    
    @PostMapping("/login")
    @Operation(summary = "Login with OAuth token", description = "Login using OAuth token from frontend and get JWT")
    public ResponseEntity<AuthResponse> login(@RequestParam String email, 
                                            @RequestParam String name, 
                                            @RequestParam(required = false) String profileImageUrl) {
        try {
            // Create or update user from OAuth data
            UserResponse user = userService.createUserFromOAuth(email, name, profileImageUrl);
            
            // Generate JWT token
            UserDetails userDetails = userService.loadUserByUsername(email);
            String token = jwtService.generateToken(userDetails);
            
            AuthResponse response = AuthResponse.of(token, user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get the current authenticated user's information")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        String email = authentication.getName();
        UserResponse user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }
    
    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Refresh the JWT token")
    public ResponseEntity<AuthResponse> refreshToken(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String newToken = jwtService.generateToken(userDetails);
        UserResponse user = userService.getUserByEmail(userDetails.getUsername());
        
        AuthResponse response = AuthResponse.of(newToken, user);
        return ResponseEntity.ok(response);
    }
} 