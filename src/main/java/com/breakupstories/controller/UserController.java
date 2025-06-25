package com.breakupstories.controller;

import com.breakupstories.dto.PagedResponse;
import com.breakupstories.dto.UserRequest;
import com.breakupstories.dto.UserResponse;
import com.breakupstories.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management APIs")
public class UserController {
    
    private final UserService userService;
    
    @PostMapping
    @Operation(summary = "Create a new user", description = "Create a new user with the provided details")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieve paginated list of all users")
    @PreAuthorize("hasRole('ADMIN')")
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
    
    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete user", description = "Delete a user by their ID")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
} 