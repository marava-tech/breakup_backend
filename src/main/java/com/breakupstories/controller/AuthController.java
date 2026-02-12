package com.breakupstories.controller;

import com.breakupstories.dto.*;
import com.breakupstories.exception.InvalidOTPException;
import com.breakupstories.service.JwtService;
import com.breakupstories.service.OTPService;
import com.breakupstories.service.UserService;
import com.breakupstories.service.DeviceMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "OTP-based authentication and authorization APIs")
public class AuthController {
    
    private final UserService userService;
    private final JwtService jwtService;
    private final OTPService otpService;
    private final DeviceMappingService deviceMappingService;
    
    @PostMapping("/send-otp-registration")
    @Operation(summary = "Send OTP for registration", description = "Send OTP to email for new user registration")
    public ResponseEntity<OtpResponse> sendOtpForRegistration(@Valid @RequestBody OtpRequest request) {
        log.info("Sending OTP for registration to email: {}", request.getEmail());
        
        boolean success = userService.sendOtpForRegistration(request.getEmail());
        
        OtpResponse response = OtpResponse.builder()
                .message("OTP sent successfully to your email")
                .success(success)
                .build();
        
        log.info("OTP sent successfully for registration to email: {}", request.getEmail());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/send-otp-login")
    @Operation(summary = "Send OTP for login", description = "Send OTP to email for existing user login")
    public ResponseEntity<OtpResponse> sendOtpForLogin(@Valid @RequestBody OtpRequest request) {
        log.info("Sending OTP for login to email: {}", request.getEmail());
        
        boolean success = userService.sendOtpForLogin(request.getEmail());
        
        OtpResponse response = OtpResponse.builder()
                .message("OTP sent successfully to your email")
                .success(success)
                .build();
        
        log.info("OTP sent successfully for login to email: {}", request.getEmail());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/verify-otp-registration")
    @Operation(summary = "Verify OTP and register user", description = "Verify OTP and create new user account")
    public ResponseEntity<AuthResponse> verifyOtpAndRegister(@Valid @RequestBody RegistrationRequest request) {
        log.info("Verifying OTP for registration with email: {} and device ID: {}", request.getEmail(), request.getDeviceId());
        
        // Verify OTP first
        boolean otpValid = otpService.verifyOtp(request.getEmail(), request.getOtp());
        if (!otpValid) {
            log.error("OTP verification failed for registration with email: {}", request.getEmail());
            throw new InvalidOTPException("Invalid OTP provided for registration");
        }
        
        // Create user request from registration request
        UserRequest userRequest = UserRequest.builder()
                .name(request.getName())
                .email(request.getEmail())
                .gender(request.getGender())
                .age(request.getAge())
                .preferredStoryLanguage(request.getPreferredStoryLanguage())
                .role(request.getRole())
                .referralCode(request.getReferralCode())
                .deviceId(request.getDeviceId()) // Include device ID for referral tracking
                .build();
        
        // Create user after OTP verification
        UserResponse user = userService.createUserAfterOtpVerification(userRequest);
        
        // Generate JWT token
        UserDetails userDetails = userService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);
        
        AuthResponse response = AuthResponse.of(token, user);
        
        log.info("User registered successfully with email: {} and device ID: {}", request.getEmail(), request.getDeviceId());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/verify-otp-login")
    @Operation(summary = "Verify OTP and login", description = "Verify OTP and login existing user")
    public ResponseEntity<AuthResponse> verifyOtpAndLogin(@Valid @RequestBody OtpVerificationRequest request) {
        log.info("Verifying OTP for login with email: {} and device ID: {}", request.getEmail(), request.getDeviceId());
        
        // Verify OTP first
        boolean otpValid = otpService.verifyOtp(request.getEmail(), request.getOtp());
        if (!otpValid) {
            log.error("OTP verification failed for login with email: {}", request.getEmail());
            throw new InvalidOTPException("Invalid OTP provided for login");
        }
        
        // Handle device ID mapping if provided (asynchronously)
        if (request.getDeviceId() != null && !request.getDeviceId().trim().isEmpty()) {
            deviceMappingService.mapDeviceIdToUserAsync(request.getEmail(), request.getDeviceId());
        }
        
        // Get user and generate JWT token
        UserResponse user = userService.getUserByEmail(request.getEmail());
        UserDetails userDetails = userService.loadUserByUsername(request.getEmail());
        String token = jwtService.generateToken(userDetails);
        
        AuthResponse response = AuthResponse.of(token, user);
        
        log.info("User logged in successfully with email: {} and device ID: {}", request.getEmail(), request.getDeviceId());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get the current authenticated user's information")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthenticated access attempt to /me endpoint");
            throw new BadCredentialsException("User not authenticated");
        }
        
        String email = authentication.getName();
        log.info("Getting current user information for email: {}", email);
        
        UserResponse user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }
    
    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Refresh the JWT token")
    public ResponseEntity<AuthResponse> refreshToken(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthenticated access attempt to /refresh endpoint");
            throw new BadCredentialsException("User not authenticated");
        }
        
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String newToken = jwtService.generateToken(userDetails);
        UserResponse user = userService.getUserByEmail(userDetails.getUsername());
        
        AuthResponse response = AuthResponse.of(newToken, user);
        
        log.info("Token refreshed successfully for user: {}", userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
} 