package com.breakupstories.controller;

import com.breakupstories.dto.*;
import com.breakupstories.enums.GENDER;
import com.breakupstories.enums.Role;
import com.breakupstories.model.*;
import com.breakupstories.repository.*;
import com.breakupstories.service.*;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Management", description = "Administrative endpoints for managing stories, users, and comments")
public class AdminController {
    
    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final StoryService storyService;
    private final UserService userService;
    private final AuditService auditService;
    private final WithdrawalRepository withdrawalRepository;
    private final RewardService rewardService;
    
    // ==================== STORY MANAGEMENT ====================
    
    @GetMapping("/stories")
    public ResponseEntity<PagedResponse<StoryResponse>> getStories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String storyId,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        
        try {
            Pageable pageable = createPageable(page, size, sortBy, sortOrder);
            Page<Story> storyPage;
            
            // Parse storyId if provided
            String singleStoryId = null;
            if (storyId != null && !storyId.trim().isEmpty()) {
                singleStoryId = storyId.trim();
            }
            
            // Parse status if provided
            Story.StoryStatus statusFilter = null;
            if (status != null && !status.trim().isEmpty()) {
                try {
                    statusFilter = Story.StoryStatus.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid status parameter: {}", status);
                    return ResponseEntity.badRequest().build();
                }
            }
            
            // Apply filters based on provided parameters
            if (singleStoryId != null) {
                // If storyId filter is provided, use it exclusively
                storyPage = storyRepository.findByIdWithPagination(singleStoryId, pageable);
            } else {
                // Apply other filters
                if (statusFilter != null && language != null && title != null && userId != null) {
                    storyPage = storyRepository.findByStatusAndLanguageAndTitleContainingAndUserId(statusFilter, language, title, userId, pageable);
                } else if (statusFilter != null && language != null && title != null) {
                    storyPage = storyRepository.findByStatusAndLanguageAndTitleContaining(statusFilter, language, title, pageable);
                } else if (statusFilter != null && language != null && userId != null) {
                    storyPage = storyRepository.findByStatusAndLanguageAndUserId(statusFilter, language, userId, pageable);
                } else if (statusFilter != null && title != null && userId != null) {
                    storyPage = storyRepository.findByStatusAndTitleContainingAndUserId(statusFilter, title, userId, pageable);
                } else if (language != null && title != null && userId != null) {
                    storyPage = storyRepository.findByLanguageAndTitleContainingAndUserId(language, title, userId, pageable);
                } else if (statusFilter != null && language != null) {
                    storyPage = storyRepository.findByStatusAndLanguage(statusFilter, language, pageable);
                } else if (statusFilter != null && title != null) {
                    storyPage = storyRepository.findByStatusAndTitleContaining(statusFilter, title, pageable);
                } else if (statusFilter != null && userId != null) {
                    storyPage = storyRepository.findByStatusAndUserId(statusFilter, userId, pageable);
                } else if (language != null && title != null) {
                    storyPage = storyRepository.findByLanguageAndTitleContaining(language, title, pageable);
                } else if (language != null && userId != null) {
                    storyPage = storyRepository.findByLanguageAndUserId(language, userId, pageable);
                } else if (title != null && userId != null) {
                    storyPage = storyRepository.findByTitleContainingAndUserId(title, userId, pageable);
                } else if (statusFilter != null) {
                    storyPage = storyRepository.findByStatus(statusFilter, pageable);
                } else if (language != null) {
                    storyPage = storyRepository.findByLanguage(language, pageable);
                } else if (title != null) {
                    storyPage = storyRepository.findByTitleContaining(title, pageable);
                } else if (userId != null) {
                    storyPage = storyRepository.findByUserId(userId, pageable);
                } else {
                    // No filters applied, return all stories
                    storyPage = storyRepository.findAll(pageable);
                }
            }
            
            List<StoryResponse> stories = storyPage.getContent().stream()
                    .map(story -> {
                        User user = userService.getUserEntityById(story.getUserId());
                        long likeCount = storyService.getLikeCount(story.getId());
                        long commentCount = storyService.getCommentCount(story.getId());
                        return StoryResponse.fromStory(story, user, false, false, likeCount, commentCount);
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(PagedResponse.of(stories, page, size, storyPage.getTotalElements()));
            
        } catch (Exception e) {
            log.error("Error fetching stories for admin: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @DeleteMapping("/stories/{storyId}")
    public ResponseEntity<Map<String, Object>> deleteStory(@PathVariable String storyId) {
        try {
            Story story = storyRepository.findById(storyId)
                    .orElseThrow(() -> new RuntimeException("Story not found"));
            
            storyRepository.deleteById(storyId);
            
            // Log audit
            auditService.logAudit("admin", Audit.EntityType.STORY, Audit.ActionType.DELETE, storyId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Story deleted successfully");
            response.put("storyId", storyId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error deleting story: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PutMapping("/stories/{storyId}")
    public ResponseEntity<Map<String, Object>> updateStory(
            @PathVariable String storyId,
            @RequestBody StoryUpdateRequest updates) {
        
        try {
            Story story = storyRepository.findById(storyId)
                    .orElseThrow(() -> new RuntimeException("Story not found"));
            
            // Update allowed fields
            if (updates.getTitle() != null) {
                story.setTitle(updates.getTitle());
            }
            if (updates.getStatus() != null) {
                story.setStatus(Story.StoryStatus.valueOf(updates.getStatus()));
            }
            if (updates.getLanguage() != null) {
                story.setLanguage(updates.getLanguage());
            }
            if (updates.getTags() != null) {
                story.setTags(java.util.Arrays.asList(updates.getTags()));
            }
            
            storyRepository.save(story);
            
            // Log audit
            auditService.logAudit("admin", Audit.EntityType.STORY, Audit.ActionType.UPDATE, storyId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Story updated successfully");
            response.put("storyId", storyId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error updating story: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PutMapping("/stories/{storyId}/status")
    @Operation(summary = "Update story status", description = "Update the status of a specific story")
    public ResponseEntity<Map<String, Object>> updateStoryStatus(
            @PathVariable String storyId,
            @RequestBody Map<String, String> request) {
        
        try {
            String newStatus = request.get("status");
            String rejectionReason = request.get("rejectionReason");
            
            if (newStatus == null || newStatus.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "Status is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            Story story = storyRepository.findById(storyId)
                    .orElseThrow(() -> new RuntimeException("Story not found"));
            
            // Validate status
            Story.StoryStatus status;
            try {
                status = Story.StoryStatus.valueOf(newStatus.toUpperCase());
            } catch (IllegalArgumentException e) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "Invalid status: " + newStatus);
                return ResponseEntity.badRequest().body(response);
            }
            
            // Update status
            story.setStatus(status);
            
            // Handle rejection reasons
            if (status == Story.StoryStatus.REJECTED && rejectionReason != null && !rejectionReason.trim().isEmpty()) {
                if (story.getRejectionReasons() == null) {
                    story.setRejectionReasons(new java.util.ArrayList<>());
                }
                story.getRejectionReasons().add(rejectionReason.trim());
            } else if (status != Story.StoryStatus.REJECTED) {
                // Clear rejection reasons if status is not REJECTED
                story.setRejectionReasons(null);
            }
            
            storyRepository.save(story);
            
            // Log audit
            auditService.logAudit("admin", Audit.EntityType.STORY, Audit.ActionType.UPDATE, storyId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Story status updated successfully");
            response.put("storyId", storyId);
            response.put("newStatus", status.name());
            response.put("previousStatus", story.getStatus().name());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error updating story status: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/stories/statistics")
    public ResponseEntity<Map<String, Object>> getStoryStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Total stories
            long totalStories = storyRepository.count();
            stats.put("totalStories", totalStories);
            
            // Stories by status
            stats.put("activeStories", storyRepository.countByStatus(Story.StoryStatus.ACTIVE));
            stats.put("processingStories", storyRepository.countByStatus(Story.StoryStatus.PROCESSING));
            stats.put("rejectedStories", storyRepository.countByStatus(Story.StoryStatus.REJECTED));
            stats.put("failedStories", storyRepository.countByStatus(Story.StoryStatus.FAILED));
            
            // Stories by language
            List<Map<String, Object>> languageStats = storyRepository.findAll().stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            story -> story.getLanguage() != null ? story.getLanguage() : "Unknown",
                            java.util.stream.Collectors.counting()))
                    .entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> langStat = new HashMap<>();
                        langStat.put("language", entry.getKey());
                        langStat.put("count", entry.getValue());
                        return langStat;
                    })
                    .collect(java.util.stream.Collectors.toList());
            stats.put("languageStats", languageStats);
            
            // Recent activity (last 7 days)
            LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
            long recentStories = storyRepository.countByCreatedAtAfter(weekAgo);
            stats.put("recentStories", recentStories);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error fetching story statistics: {}", e.getMessage(), e);
            Map<String, Object> errorStats = new HashMap<>();
            errorStats.put("error", "Unable to calculate story statistics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorStats);
        }
    }
    
    // ==================== USER MANAGEMENT ====================
    
    @GetMapping("/users")
    @Operation(summary = "Get users with filters", description = "Retrieve paginated list of users with optional filters including device ID and referredBy")
    public ResponseEntity<PagedResponse<UserResponse>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String referredBy,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        try {
            Pageable pageable = createPageable(page, size, sortBy, sortOrder);
            
            // Parse role if provided
            Role roleFilter = null;
            if (role != null && !role.trim().isEmpty()) {
                try {
                    roleFilter = Role.valueOf(role.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid role parameter: {}", role);
                    return ResponseEntity.badRequest().build();
                }
            }
            
            // Parse gender if provided
            GENDER genderFilter = null;
            if (gender != null && !gender.trim().isEmpty()) {
                try {
                    genderFilter = GENDER.valueOf(gender.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid gender parameter: {}", gender);
                    return ResponseEntity.badRequest().build();
                }
            }
            
            // Use the custom filtering method that supports device ID and referredBy
            Page<User> userPage = filterUsers(username, email, roleFilter, genderFilter, userId, deviceId, referredBy, pageable);
            
            List<UserResponse> users = userPage.getContent().stream()
                    .map(user -> UserResponse.fromUserWithReferrerName(user, userRepository))
                    .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(PagedResponse.of(users, page, size, userPage.getTotalElements()));
            
        } catch (Exception e) {
            log.error("Error fetching users for admin: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/users/statistics")
    public ResponseEntity<Map<String, Object>> getUserStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Total users
            long totalUsers = userRepository.count();
            stats.put("totalUsers", totalUsers);
            
            // Users by role
            stats.put("adminUsers", userRepository.countByRole(Role.ADMIN));
            stats.put("userUsers", userRepository.countByRole(Role.USER));
            
            // Users by gender
            stats.put("maleUsers", userRepository.countByGender(GENDER.MALE));
            stats.put("femaleUsers", userRepository.countByGender(GENDER.FEMALE));
            stats.put("otherUsers", userRepository.countByGender(GENDER.OTHER));
            
            // Coin statistics
            List<User> allUsers = userRepository.findAll();
            double avgCoins = allUsers.stream()
                    .mapToInt(user -> rewardService.getValidTotalCoins(user.getId()))
                    .average()
                    .orElse(0.0);
            stats.put("averageCoins", avgCoins);
            
            int maxCoins = allUsers.stream()
                    .mapToInt(user -> rewardService.getValidTotalCoins(user.getId()))
                    .max()
                    .orElse(0);
            stats.put("maxCoins", maxCoins);
            
            // Recent users (last 7 days)
            LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
            long recentUsers = userRepository.countByCreatedAtAfter(weekAgo);
            stats.put("recentUsers", recentUsers);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error fetching user statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/users/device/{deviceId}")
    @Operation(summary = "Get users by device ID", description = "Get all users associated with a specific device ID")
    public ResponseEntity<Map<String, Object>> getUsersByDeviceId(@PathVariable String deviceId) {
        try {
            List<User> users = userRepository.findAllByDeviceId(deviceId);
            List<UserResponse> userResponses = users.stream()
                    .map(UserResponse::fromUser)
                    .collect(java.util.stream.Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("deviceId", deviceId);
            response.put("userCount", users.size());
            response.put("users", userResponses);
            
            log.info("Retrieved {} users for device ID: {}", users.size(), deviceId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching users by device ID {}: {}", deviceId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Custom filtering method that supports all filter combinations including device ID and referredBy
     */
    private Page<User> filterUsers(String username, String email, Role roleFilter, GENDER genderFilter, 
                                   String userId, String deviceId, String referredBy, Pageable pageable) {
        
        // If only deviceId is provided, use the repository method
        if (deviceId != null && username == null && email == null && roleFilter == null && 
            genderFilter == null && userId == null && referredBy == null) {
            List<User> deviceUsers = userRepository.findAllByDeviceId(deviceId);
            return createPageFromList(deviceUsers, pageable);
        }
        
        // If only referredBy is provided, use the repository method
        if (referredBy != null && username == null && email == null && roleFilter == null && 
            genderFilter == null && userId == null && deviceId == null) {
            List<User> referredUsers = userRepository.findByReferredBy(referredBy);
            return createPageFromList(referredUsers, pageable);
        }
        
        // For complex filtering with device ID, we need to use a custom approach
        // Since adding all combinations to repository would be excessive, use programmatic filtering
        
        Page<User> basePage;
        
        // Start with the most specific filters first
        if (userId != null) {
            // If userId is provided, get that specific user
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return Page.empty(pageable);
            }
            User user = userOpt.get();
            
            // Check if the user matches all other filters
            if (matchesFilters(user, username, email, roleFilter, genderFilter, deviceId, referredBy)) {
                return createPageFromList(List.of(user), pageable);
            } else {
                return Page.empty(pageable);
            }
        }
        
        // If no userId but deviceId is specified, start with device users
        if (deviceId != null) {
            List<User> deviceUsers = userRepository.findAllByDeviceId(deviceId);
            List<User> filteredUsers = deviceUsers.stream()
                    .filter(user -> matchesFilters(user, username, email, roleFilter, genderFilter, null, referredBy))
                    .collect(java.util.stream.Collectors.toList());
            return createPageFromList(filteredUsers, pageable);
        }
        
        // If no userId but referredBy is specified, start with referred users
        if (referredBy != null) {
            List<User> referredUsers = userRepository.findByReferredBy(referredBy);
            List<User> filteredUsers = referredUsers.stream()
                    .filter(user -> matchesFilters(user, username, email, roleFilter, genderFilter, deviceId, null))
                    .collect(java.util.stream.Collectors.toList());
            return createPageFromList(filteredUsers, pageable);
        }
        
        // Fall back to existing repository methods for other combinations
        if (username != null && roleFilter != null && genderFilter != null) {
            basePage = userRepository.findByNameContainingIgnoreCaseAndRoleAndGender(username, roleFilter, genderFilter, pageable);
        } else if (email != null && roleFilter != null && genderFilter != null) {
            basePage = userRepository.findByEmailContainingIgnoreCaseAndRoleAndGender(email, roleFilter, genderFilter, pageable);
        } else if (username != null && roleFilter != null) {
            basePage = userRepository.findByNameContainingIgnoreCaseAndRole(username, roleFilter, pageable);
        } else if (email != null && roleFilter != null) {
            basePage = userRepository.findByEmailContainingIgnoreCaseAndRole(email, roleFilter, pageable);
        } else if (username != null && genderFilter != null) {
            basePage = userRepository.findByNameContainingIgnoreCaseAndGender(username, genderFilter, pageable);
        } else if (email != null && genderFilter != null) {
            basePage = userRepository.findByEmailContainingIgnoreCaseAndGender(email, genderFilter, pageable);
        } else if (roleFilter != null && genderFilter != null) {
            basePage = userRepository.findByRoleAndGender(roleFilter, genderFilter, pageable);
        } else if (username != null) {
            basePage = userRepository.findByNameContainingIgnoreCase(username, pageable);
        } else if (email != null) {
            basePage = userRepository.findByEmailContainingIgnoreCase(email, pageable);
        } else if (roleFilter != null) {
            basePage = userRepository.findByRole(roleFilter, pageable);
        } else if (genderFilter != null) {
            basePage = userRepository.findByGender(genderFilter, pageable);
        } else {
            basePage = userRepository.findAll(pageable);
        }
        
        return basePage;
    }
    
    /**
     * Check if a user matches the given filters
     */
    private boolean matchesFilters(User user, String username, String email, Role roleFilter, 
                                   GENDER genderFilter, String deviceId, String referredBy) {
        if (username != null && (user.getName() == null || 
                !user.getName().toLowerCase().contains(username.toLowerCase()))) {
            return false;
        }
        
        if (email != null && (user.getEmail() == null || 
                !user.getEmail().toLowerCase().contains(email.toLowerCase()))) {
            return false;
        }
        
        if (roleFilter != null && !roleFilter.equals(user.getRole())) {
            return false;
        }
        
        if (genderFilter != null && !genderFilter.equals(user.getGender())) {
            return false;
        }
        
        if (deviceId != null && !deviceId.equals(user.getDeviceId())) {
            return false;
        }
        
        if (referredBy != null && !referredBy.equals(user.getReferredBy())) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Create a Page object from a list of users
     */
    private Page<User> createPageFromList(List<User> users, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), users.size());
        
        if (start >= users.size()) {
            return new org.springframework.data.domain.PageImpl<>(
                    java.util.Collections.emptyList(), pageable, users.size());
        }
        
        List<User> pageContent = users.subList(start, end);
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, users.size());
    }
    
    // ==================== COMMENT MANAGEMENT ====================
    
    @GetMapping("/comments")
    public ResponseEntity<PagedResponse<CommentResponse>> getComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String storyId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) Boolean isAbusive,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        
        try {
            Pageable pageable = createPageable(page, size, sortBy, sortOrder);
            Page<Comment> commentPage;
            
            // Apply filters based on provided parameters
            if (storyId != null && userId != null && isAbusive != null && category != null && active != null) {
                commentPage = commentRepository.findByStoryIdAndUserIdAndIsAbusiveAndCategoryAndActive(storyId, userId, isAbusive, category, active, pageable);
            } else if (storyId != null && userId != null && isAbusive != null && category != null) {
                commentPage = commentRepository.findByStoryIdAndUserIdAndIsAbusiveAndCategory(storyId, userId, isAbusive, category, pageable);
            } else if (storyId != null && userId != null && isAbusive != null && active != null) {
                commentPage = commentRepository.findByStoryIdAndUserIdAndIsAbusiveAndActive(storyId, userId, isAbusive, active, pageable);
            } else if (storyId != null && userId != null && category != null && active != null) {
                commentPage = commentRepository.findByStoryIdAndUserIdAndCategoryAndActive(storyId, userId, category, active, pageable);
            } else if (storyId != null && isAbusive != null && category != null && active != null) {
                commentPage = commentRepository.findByStoryIdAndIsAbusiveAndCategoryAndActive(storyId, isAbusive, category, active, pageable);
            } else if (userId != null && isAbusive != null && category != null && active != null) {
                commentPage = commentRepository.findByUserIdAndIsAbusiveAndCategoryAndActive(userId, isAbusive, category, active, pageable);
            } else if (storyId != null && userId != null && isAbusive != null) {
                commentPage = commentRepository.findByStoryIdAndUserIdAndIsAbusive(storyId, userId, isAbusive, pageable);
            } else if (storyId != null && userId != null && category != null) {
                commentPage = commentRepository.findByStoryIdAndUserIdAndCategory(storyId, userId, category, pageable);
            } else if (storyId != null && userId != null && active != null) {
                commentPage = commentRepository.findByStoryIdAndUserIdAndActive(storyId, userId, active, pageable);
            } else if (storyId != null && isAbusive != null && category != null) {
                commentPage = commentRepository.findByStoryIdAndIsAbusiveAndCategory(storyId, isAbusive, category, pageable);
            } else if (storyId != null && isAbusive != null && active != null) {
                commentPage = commentRepository.findByStoryIdAndIsAbusiveAndActive(storyId, isAbusive, active, pageable);
            } else if (storyId != null && category != null && active != null) {
                commentPage = commentRepository.findByStoryIdAndCategoryAndActive(storyId, category, active, pageable);
            } else if (userId != null && isAbusive != null && category != null) {
                commentPage = commentRepository.findByUserIdAndIsAbusiveAndCategory(userId, isAbusive, category, pageable);
            } else if (userId != null && isAbusive != null && active != null) {
                commentPage = commentRepository.findByUserIdAndIsAbusiveAndActive(userId, isAbusive, active, pageable);
            } else if (userId != null && category != null && active != null) {
                commentPage = commentRepository.findByUserIdAndCategoryAndActive(userId, category, active, pageable);
            } else if (isAbusive != null && category != null && active != null) {
                commentPage = commentRepository.findByIsAbusiveAndCategoryAndActive(isAbusive, category, active, pageable);
            } else if (storyId != null && userId != null) {
                commentPage = commentRepository.findByStoryIdAndUserId(storyId, userId, pageable);
            } else if (storyId != null && isAbusive != null) {
                commentPage = commentRepository.findByStoryIdAndIsAbusive(storyId, isAbusive, pageable);
            } else if (storyId != null && category != null) {
                commentPage = commentRepository.findByStoryIdAndCategory(storyId, category, pageable);
            } else if (storyId != null && active != null) {
                commentPage = commentRepository.findByStoryIdAndActive(storyId, active, pageable);
            } else if (userId != null && isAbusive != null) {
                commentPage = commentRepository.findByUserIdAndIsAbusive(userId, isAbusive, pageable);
            } else if (userId != null && category != null) {
                commentPage = commentRepository.findByUserIdAndCategory(userId, category, pageable);
            } else if (userId != null && active != null) {
                commentPage = commentRepository.findByUserIdAndActive(userId, active, pageable);
            } else if (isAbusive != null && category != null) {
                commentPage = commentRepository.findByIsAbusiveAndCategory(isAbusive, category, pageable);
            } else if (isAbusive != null && active != null) {
                commentPage = commentRepository.findByIsAbusiveAndActive(isAbusive, active, pageable);
            } else if (category != null && active != null) {
                commentPage = commentRepository.findByCategoryAndActive(category, active, pageable);
            } else if (storyId != null) {
                commentPage = commentRepository.findByStoryId(storyId, pageable);
            } else if (userId != null) {
                commentPage = commentRepository.findByUserId(userId, pageable);
            } else if (isAbusive != null) {
                commentPage = commentRepository.findByIsAbusive(isAbusive, pageable);
            } else if (category != null) {
                commentPage = commentRepository.findByCategory(category, pageable);
            } else if (active != null) {
                commentPage = commentRepository.findByActive(active, pageable);
            } else {
                // No filters applied, return all comments
                commentPage = commentRepository.findAll(pageable);
            }
            
            List<CommentResponse> comments = commentPage.getContent().stream()
                    .map(comment -> {
                        User user = userService.getUserEntityById(comment.getUserId());
                        return CommentResponse.fromComment(comment, user);
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(PagedResponse.of(comments, page, size, commentPage.getTotalElements()));
            
        } catch (Exception e) {
            log.error("Error fetching comments for admin: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Map<String, Object>> deleteComment(@PathVariable String commentId) {
        try {
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new RuntimeException("Comment not found"));
            
            commentRepository.deleteById(commentId);
            
            // Log audit
            auditService.logAudit("admin", Audit.EntityType.COMMENT, Audit.ActionType.DELETE, commentId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Comment deleted successfully");
            response.put("commentId", commentId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error deleting comment: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/comments/statistics")
    public ResponseEntity<Map<String, Object>> getCommentStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Total comments
            long totalComments = commentRepository.count();
            stats.put("totalComments", totalComments);
            
            // Active vs inactive comments
            long activeComments = commentRepository.countByActiveTrue();
            long inactiveComments = totalComments - activeComments;
            stats.put("activeComments", activeComments);
            stats.put("inactiveComments", inactiveComments);
            
            // Abusive comments
            long abusiveComments = commentRepository.countByIsAbusiveTrue();
            stats.put("abusiveComments", abusiveComments);
            
            // Comments by category
            List<Map<String, Object>> categoryStats = commentRepository.findAll().stream()
                    .filter(comment -> comment.getCategory() != null)
                    .collect(java.util.stream.Collectors.groupingBy(
                            Comment::getCategory,
                            java.util.stream.Collectors.counting()))
                    .entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> catStat = new HashMap<>();
                        catStat.put("category", entry.getKey());
                        catStat.put("count", entry.getValue());
                        return catStat;
                    })
                    .collect(java.util.stream.Collectors.toList());
            stats.put("categoryStats", categoryStats);
            
            // Recent comments (last 7 days)
            LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
            long recentComments = commentRepository.countByCreatedAtAfter(weekAgo);
            stats.put("recentComments", recentComments);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error fetching comment statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/withdrawals/statistics")
    public ResponseEntity<Map<String, Object>> getWithdrawalStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Total withdrawals
            long totalWithdrawals = withdrawalRepository.count();
            stats.put("totalWithdrawals", totalWithdrawals);
            
            // Withdrawals by status
            stats.put("pendingWithdrawals", withdrawalRepository.countByStatus(com.breakupstories.model.Withdrawal.WithdrawalStatus.PENDING));
            stats.put("processingWithdrawals", withdrawalRepository.countByStatus(com.breakupstories.model.Withdrawal.WithdrawalStatus.PROCESSING));
            stats.put("processedWithdrawals", withdrawalRepository.countByStatus(com.breakupstories.model.Withdrawal.WithdrawalStatus.PROCESSED));
            stats.put("rejectedWithdrawals", withdrawalRepository.countByStatus(com.breakupstories.model.Withdrawal.WithdrawalStatus.REJECTED));
            
            // Recent withdrawals (last 7 days)
            LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
            long recentWithdrawals = withdrawalRepository.countByCreatedAtAfter(weekAgo);
            stats.put("recentWithdrawals", recentWithdrawals);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error fetching withdrawal statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    

    
    // ==================== DASHBOARD STATISTICS ====================
    
    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        
        try {
            LocalDateTime fromDateTime = null;
            LocalDateTime toDateTime = null;
            
            // Parse date parameters if provided
            if (fromDate != null && !fromDate.trim().isEmpty()) {
                fromDateTime = parseDateTime(fromDate);
            }
            if (toDate != null && !toDate.trim().isEmpty()) {
                toDateTime = parseDateTime(toDate);
            }
            
            // If no dates provided, default to last 30 days
            if (fromDateTime == null && toDateTime == null) {
                toDateTime = LocalDateTime.now();
                fromDateTime = toDateTime.minusDays(30);
            } else if (fromDateTime == null) {
                fromDateTime = toDateTime.minusDays(30);
            } else if (toDateTime == null) {
                toDateTime = fromDateTime.plusDays(30);
            }
            
            Map<String, Object> dashboardStats = new HashMap<>();
            
            // User Registration Stats
            Map<String, Object> userStats = getUserRegistrationStats(fromDateTime, toDateTime);
            dashboardStats.put("userStats", userStats);
            
            // Story Creation Stats
            Map<String, Object> storyStats = getStoryCreationStats(fromDateTime, toDateTime);
            dashboardStats.put("storyStats", storyStats);
            
            // Comment Stats
            Map<String, Object> commentStats = getCommentStats(fromDateTime, toDateTime);
            dashboardStats.put("commentStats", commentStats);
            
            // Like Stats
            Map<String, Object> likeStats = getLikeStats(fromDateTime, toDateTime);
            dashboardStats.put("likeStats", likeStats);
            
            // View Stats
            Map<String, Object> viewStats = getViewStats(fromDateTime, toDateTime);
            dashboardStats.put("viewStats", viewStats);
            
            // Engagement Metrics
            Map<String, Object> engagementStats = getEngagementStats(fromDateTime, toDateTime);
            dashboardStats.put("engagementStats", engagementStats);
            
            // Platform Health Metrics
            Map<String, Object> platformHealth = getPlatformHealthStats(fromDateTime, toDateTime);
            dashboardStats.put("platformHealth", platformHealth);
            
            // Withdrawal Statistics
            Map<String, Object> withdrawalStats = getWithdrawalStats(fromDateTime, toDateTime);
            dashboardStats.put("withdrawalStats", withdrawalStats);
            
            // Date range info
            Map<String, Object> dateRange = new HashMap<>();
            dateRange.put("fromDate", fromDateTime.toString());
            dateRange.put("toDate", toDateTime.toString());
            dateRange.put("durationDays", java.time.Duration.between(fromDateTime, toDateTime).toDays());
            dashboardStats.put("dateRange", dateRange);
            
            return ResponseEntity.ok(dashboardStats);
            
        } catch (Exception e) {
            log.error("Error fetching dashboard statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    private Map<String, Object> getUserRegistrationStats(LocalDateTime fromDate, LocalDateTime toDate) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Total users in date range
            long totalUsersInRange = userRepository.countByCreatedAtBetween(fromDate, toDate);
            stats.put("totalRegistrations", totalUsersInRange);
            
            // Total users overall
            long totalUsersOverall = userRepository.count();
            stats.put("totalUsersOverall", totalUsersOverall);
            
            // Users by role in date range
            long adminRegistrations = userRepository.countByRoleAndCreatedAtBetween(Role.ADMIN, fromDate, toDate);
            long userRegistrations = userRepository.countByRoleAndCreatedAtBetween(Role.USER, fromDate, toDate);
            stats.put("adminRegistrations", adminRegistrations);
            stats.put("userRegistrations", userRegistrations);
            
            // Users by gender in date range
            long maleRegistrations = userRepository.countByGenderAndCreatedAtBetween(GENDER.MALE, fromDate, toDate);
            long femaleRegistrations = userRepository.countByGenderAndCreatedAtBetween(GENDER.FEMALE, fromDate, toDate);
            long otherRegistrations = userRepository.countByGenderAndCreatedAtBetween(GENDER.OTHER, fromDate, toDate);
            stats.put("maleRegistrations", maleRegistrations);
            stats.put("femaleRegistrations", femaleRegistrations);
            stats.put("otherRegistrations", otherRegistrations);
            
            // Average daily registrations
            long durationDays = java.time.Duration.between(fromDate, toDate).toDays();
            double avgDailyRegistrations = durationDays > 0 ? (double) totalUsersInRange / durationDays : 0;
            stats.put("avgDailyRegistrations", Math.round(avgDailyRegistrations * 100.0) / 100.0);
            
            // Growth rate (comparing with previous period)
            LocalDateTime previousFromDate = fromDate.minus(java.time.Duration.between(fromDate, toDate));
            long previousPeriodRegistrations = userRepository.countByCreatedAtBetween(previousFromDate, fromDate);
            double growthRate = previousPeriodRegistrations > 0 ? 
                ((double) (totalUsersInRange - previousPeriodRegistrations) / previousPeriodRegistrations) * 100 : 0;
            stats.put("growthRate", Math.round(growthRate * 100.0) / 100.0);
            
        } catch (Exception e) {
            log.warn("Error calculating user registration stats: {}", e.getMessage());
            stats.put("error", "Unable to calculate user registration statistics");
        }
        
        return stats;
    }
    
    private Map<String, Object> getStoryCreationStats(LocalDateTime fromDate, LocalDateTime toDate) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Total stories in date range
            long totalStoriesInRange = storyRepository.countByCreatedAtBetween(fromDate, toDate);
            stats.put("totalStoriesCreated", totalStoriesInRange);
            
            // Total stories overall
            long totalStoriesOverall = storyRepository.count();
            stats.put("totalStoriesOverall", totalStoriesOverall);
            
            // Stories by status in date range
            long activeStories = storyRepository.countByStatusAndCreatedAtBetween(Story.StoryStatus.ACTIVE, fromDate, toDate);
            long processingStories = storyRepository.countByStatusAndCreatedAtBetween(Story.StoryStatus.PROCESSING, fromDate, toDate);
            long rejectedStories = storyRepository.countByStatusAndCreatedAtBetween(Story.StoryStatus.REJECTED, fromDate, toDate);
            long failedStories = storyRepository.countByStatusAndCreatedAtBetween(Story.StoryStatus.FAILED, fromDate, toDate);
            
            stats.put("activeStories", activeStories);
            stats.put("processingStories", processingStories);
            stats.put("rejectedStories", rejectedStories);
            stats.put("failedStories", failedStories);
            
            // Stories by language in date range
            List<Map<String, Object>> languageStats = storyRepository.findByCreatedAtBetween(fromDate, toDate).stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            story -> story.getLanguage() != null ? story.getLanguage() : "Unknown",
                            java.util.stream.Collectors.counting()))
                    .entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> langStat = new HashMap<>();
                        langStat.put("language", entry.getKey());
                        langStat.put("count", entry.getValue());
                        return langStat;
                    })
                    .collect(java.util.stream.Collectors.toList());
            stats.put("languageStats", languageStats);
            
            // Average daily story creation
            long durationDays = java.time.Duration.between(fromDate, toDate).toDays();
            if (durationDays <= 0) {
                durationDays = 1; // Avoid division by zero
            }
            double avgDailyStories = (double) totalStoriesInRange / durationDays;
            stats.put("avgDailyStories", Math.round(avgDailyStories * 100.0) / 100.0);
            
            // Success rate
            double successRate = totalStoriesInRange > 0 ? 
                ((double) activeStories / totalStoriesInRange) * 100 : 0;
            stats.put("successRate", Math.round(successRate * 100.0) / 100.0);
            
        } catch (Exception e) {
            log.error("Error calculating story creation stats: {}", e.getMessage(), e);
            stats.put("error", "Unable to calculate story creation statistics: " + e.getMessage());
        }
        
        return stats;
    }
    
    private Map<String, Object> getCommentStats(LocalDateTime fromDate, LocalDateTime toDate) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Total comments in date range
            long totalCommentsInRange = commentRepository.countByCreatedAtBetween(fromDate, toDate);
            stats.put("totalComments", totalCommentsInRange);
            
            // Total comments overall
            long totalCommentsOverall = commentRepository.count();
            stats.put("totalCommentsOverall", totalCommentsOverall);
            
            // Active vs inactive comments
            long activeComments = commentRepository.countByActiveTrueAndCreatedAtBetween(fromDate, toDate);
            long inactiveComments = totalCommentsInRange - activeComments;
            stats.put("activeComments", activeComments);
            stats.put("inactiveComments", inactiveComments);
            
            // Abusive comments
            long abusiveComments = commentRepository.countByIsAbusiveTrueAndCreatedAtBetween(fromDate, toDate);
            stats.put("abusiveComments", abusiveComments);
            
            // Comments by category
            List<Map<String, Object>> categoryStats = commentRepository.findByCreatedAtBetween(fromDate, toDate).stream()
                    .filter(comment -> comment.getCategory() != null)
                    .collect(java.util.stream.Collectors.groupingBy(
                            Comment::getCategory,
                            java.util.stream.Collectors.counting()))
                    .entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> catStat = new HashMap<>();
                        catStat.put("category", entry.getKey());
                        catStat.put("count", entry.getValue());
                        return catStat;
                    })
                    .collect(java.util.stream.Collectors.toList());
            stats.put("categoryStats", categoryStats);
            
            // Average daily comments
            long durationDays = java.time.Duration.between(fromDate, toDate).toDays();
            double avgDailyComments = durationDays > 0 ? (double) totalCommentsInRange / durationDays : 0;
            stats.put("avgDailyComments", Math.round(avgDailyComments * 100.0) / 100.0);
            
            // Engagement rate (comments per story)
            long totalStoriesInRange = storyRepository.countByCreatedAtBetween(fromDate, toDate);
            double commentsPerStory = totalStoriesInRange > 0 ? (double) totalCommentsInRange / totalStoriesInRange : 0;
            stats.put("commentsPerStory", Math.round(commentsPerStory * 100.0) / 100.0);
            
        } catch (Exception e) {
            log.warn("Error calculating comment stats: {}", e.getMessage());
            stats.put("error", "Unable to calculate comment statistics");
        }
        
        return stats;
    }
    
    private Map<String, Object> getLikeStats(LocalDateTime fromDate, LocalDateTime toDate) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Total likes in date range
            long totalLikesInRange = likeRepository.countByCreatedAtBetween(fromDate, toDate);
            stats.put("totalLikes", totalLikesInRange);
            
            // Total likes overall
            long totalLikesOverall = likeRepository.count();
            stats.put("totalLikesOverall", totalLikesOverall);
            
            // Average daily likes
            long durationDays = java.time.Duration.between(fromDate, toDate).toDays();
            double avgDailyLikes = durationDays > 0 ? (double) totalLikesInRange / durationDays : 0;
            stats.put("avgDailyLikes", Math.round(avgDailyLikes * 100.0) / 100.0);
            
            // Likes per story
            long totalStoriesInRange = storyRepository.countByCreatedAtBetween(fromDate, toDate);
            double likesPerStory = totalStoriesInRange > 0 ? (double) totalLikesInRange / totalStoriesInRange : 0;
            stats.put("likesPerStory", Math.round(likesPerStory * 100.0) / 100.0);
            
            // Engagement rate (likes per user)
            long totalUsersInRange = userRepository.countByCreatedAtBetween(fromDate, toDate);
            double likesPerUser = totalUsersInRange > 0 ? (double) totalLikesInRange / totalUsersInRange : 0;
            stats.put("likesPerUser", Math.round(likesPerUser * 100.0) / 100.0);
            
        } catch (Exception e) {
            log.warn("Error calculating like stats: {}", e.getMessage());
            stats.put("error", "Unable to calculate like statistics");
        }
        
        return stats;
    }
    
    private Map<String, Object> getViewStats(LocalDateTime fromDate, LocalDateTime toDate) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Total views in date range (assuming view count is stored in story metadata)
            long totalViewsInRange = storyRepository.findByCreatedAtBetween(fromDate, toDate).stream()
                    .mapToLong(story -> story.getViewCount() != null ? story.getViewCount() : 0)
                    .sum();
            stats.put("totalViews", totalViewsInRange);
            
            // Average views per story
            long totalStoriesInRange = storyRepository.countByCreatedAtBetween(fromDate, toDate);
            double avgViewsPerStory = totalStoriesInRange > 0 ? (double) totalViewsInRange / totalStoriesInRange : 0;
            stats.put("avgViewsPerStory", Math.round(avgViewsPerStory * 100.0) / 100.0);
            
            // Average daily views
            long durationDays = java.time.Duration.between(fromDate, toDate).toDays();
            double avgDailyViews = durationDays > 0 ? (double) totalViewsInRange / durationDays : 0;
            stats.put("avgDailyViews", Math.round(avgDailyViews * 100.0) / 100.0);
            
            // Most viewed stories in date range
            List<Map<String, Object>> topViewedStories = storyRepository.findByCreatedAtBetween(fromDate, toDate).stream()
                    .filter(story -> story.getViewCount() != null && story.getViewCount() > 0)
                    .sorted((s1, s2) -> Long.compare(s2.getViewCount(), s1.getViewCount()))
                    .limit(5)
                    .map(story -> {
                        Map<String, Object> storyStat = new HashMap<>();
                        storyStat.put("storyId", story.getId());
                        storyStat.put("title", story.getTitle());
                        storyStat.put("viewCount", story.getViewCount());
                        return storyStat;
                    })
                    .collect(java.util.stream.Collectors.toList());
            stats.put("topViewedStories", topViewedStories);
            
        } catch (Exception e) {
            log.warn("Error calculating view stats: {}", e.getMessage());
            stats.put("error", "Unable to calculate view statistics");
        }
        
        return stats;
    }
    
    private Map<String, Object> getEngagementStats(LocalDateTime fromDate, LocalDateTime toDate) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Total engagement (likes + comments + views)
            long totalLikes = likeRepository.countByCreatedAtBetween(fromDate, toDate);
            long totalComments = commentRepository.countByCreatedAtBetween(fromDate, toDate);
            long totalViews = storyRepository.findByCreatedAtBetween(fromDate, toDate).stream()
                    .mapToLong(story -> story.getViewCount() != null ? story.getViewCount() : 0)
                    .sum();
            
            long totalEngagement = totalLikes + totalComments + totalViews;
            stats.put("totalEngagement", totalEngagement);
            stats.put("totalLikes", totalLikes);
            stats.put("totalComments", totalComments);
            stats.put("totalViews", totalViews);
            
            // Engagement per user
            long totalUsersInRange = userRepository.countByCreatedAtBetween(fromDate, toDate);
            double engagementPerUser = totalUsersInRange > 0 ? (double) totalEngagement / totalUsersInRange : 0;
            stats.put("engagementPerUser", Math.round(engagementPerUser * 100.0) / 100.0);
            
            // Engagement per story
            long totalStoriesInRange = storyRepository.countByCreatedAtBetween(fromDate, toDate);
            double engagementPerStory = totalStoriesInRange > 0 ? (double) totalEngagement / totalStoriesInRange : 0;
            stats.put("engagementPerStory", Math.round(engagementPerStory * 100.0) / 100.0);
            
            // Duration info for reference
            long durationDays = java.time.Duration.between(fromDate, toDate).toDays();
            stats.put("durationDays", durationDays);
            
        } catch (Exception e) {
            log.warn("Error calculating engagement stats: {}", e.getMessage());
            stats.put("error", "Unable to calculate engagement statistics");
        }
        
        return stats;
    }
    
    private Map<String, Object> getPlatformHealthStats(LocalDateTime fromDate, LocalDateTime toDate) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // User retention (users who registered and are still active)
            long totalUsersInRange = userRepository.countByCreatedAtBetween(fromDate, toDate);
            // For now, we'll use a simple metric - users who have stories or comments
            long activeUsersInRange = userRepository.findByCreatedAtBetween(fromDate, toDate).stream()
                    .filter(user -> {
                        // Consider user active if they have stories or comments
                        long userStories = storyRepository.countByUserId(user.getId());
                        long userComments = commentRepository.countByUserId(user.getId());
                        return userStories > 0 || userComments > 0;
                    })
                    .count();
            double retentionRate = totalUsersInRange > 0 ? (double) activeUsersInRange / totalUsersInRange * 100 : 0;
            stats.put("retentionRate", Math.round(retentionRate * 100.0) / 100.0);
            stats.put("activeUsers", activeUsersInRange);
            
            // Content quality metrics
            long totalStoriesInRange = storyRepository.countByCreatedAtBetween(fromDate, toDate);
            long highQualityStories = storyRepository.findByCreatedAtBetween(fromDate, toDate).stream()
                    .filter(story -> story.getViewCount() != null && story.getViewCount() > 10)
                    .count();
            double qualityRate = totalStoriesInRange > 0 ? (double) highQualityStories / totalStoriesInRange * 100 : 0;
            stats.put("contentQualityRate", Math.round(qualityRate * 100.0) / 100.0);
            
            // System performance metrics
            long totalCommentsInRange = commentRepository.countByCreatedAtBetween(fromDate, toDate);
            long abusiveCommentsInRange = commentRepository.countByIsAbusiveTrueAndCreatedAtBetween(fromDate, toDate);
            double abuseRate = totalCommentsInRange > 0 ? (double) abusiveCommentsInRange / totalCommentsInRange * 100 : 0;
            stats.put("abuseRate", Math.round(abuseRate * 100.0) / 100.0);
            
            // Average response time (placeholder - would need actual metrics)
            stats.put("avgResponseTime", "2.3s");
            
            // Error rate (placeholder - would need actual error tracking)
            stats.put("errorRate", "0.1%");
            
        } catch (Exception e) {
            log.warn("Error calculating platform health stats: {}", e.getMessage());
            stats.put("error", "Unable to calculate platform health statistics");
        }
        
        return stats;
    }
    
    private Map<String, Object> getWithdrawalStats(LocalDateTime fromDate, LocalDateTime toDate) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Total withdrawals in date range
            long totalWithdrawalsInRange = withdrawalRepository.countByCreatedAtBetween(fromDate, toDate);
            stats.put("totalWithdrawalsInRange", totalWithdrawalsInRange);
            
            // Total withdrawals overall
            long totalWithdrawalsOverall = withdrawalRepository.count();
            stats.put("totalWithdrawalsOverall", totalWithdrawalsOverall);
            
            // Withdrawals by status in date range
            long pendingWithdrawals = withdrawalRepository.countByStatusAndCreatedAtBetween(Withdrawal.WithdrawalStatus.PENDING, fromDate, toDate);
            long processingWithdrawals = withdrawalRepository.countByStatusAndCreatedAtBetween(Withdrawal.WithdrawalStatus.PROCESSING, fromDate, toDate);
            long processedWithdrawals = withdrawalRepository.countByStatusAndCreatedAtBetween(Withdrawal.WithdrawalStatus.PROCESSED, fromDate, toDate);
            long rejectedWithdrawals = withdrawalRepository.countByStatusAndCreatedAtBetween(Withdrawal.WithdrawalStatus.REJECTED, fromDate, toDate);
            
            stats.put("pendingWithdrawals", pendingWithdrawals);
            stats.put("processingWithdrawals", processingWithdrawals);
            stats.put("processedWithdrawals", processedWithdrawals);
            stats.put("rejectedWithdrawals", rejectedWithdrawals);
            
            // Total withdrawals by status overall
            stats.put("totalPendingWithdrawals", withdrawalRepository.countByStatus(Withdrawal.WithdrawalStatus.PENDING));
            stats.put("totalProcessingWithdrawals", withdrawalRepository.countByStatus(Withdrawal.WithdrawalStatus.PROCESSING));
            stats.put("totalProcessedWithdrawals", withdrawalRepository.countByStatus(Withdrawal.WithdrawalStatus.PROCESSED));
            stats.put("totalRejectedWithdrawals", withdrawalRepository.countByStatus(Withdrawal.WithdrawalStatus.REJECTED));
            
            // Amount calculations for date range
            List<Withdrawal> withdrawalsInRange = withdrawalRepository.findByCreatedAtBetween(fromDate, toDate);
            BigDecimal totalAmountInRange = withdrawalsInRange.stream()
                    .map(Withdrawal::getMoneyInRs)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalCoinsInRange = withdrawalsInRange.stream()
                    .map(Withdrawal::getCoins)
                    .filter(Objects::nonNull)
                    .map(BigDecimal::new)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            stats.put("totalAmountInRange", totalAmountInRange);
            stats.put("totalCoinsInRange", totalCoinsInRange);
            
            // Processed withdrawals with amount
            List<Withdrawal> processedWithdrawalsList = withdrawalRepository.findByStatusAndCreatedAtBetween(Withdrawal.WithdrawalStatus.PROCESSED, fromDate, toDate);
            BigDecimal processedAmount = processedWithdrawalsList.stream()
                    .map(Withdrawal::getMoneyInRs)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal processedCoins = processedWithdrawalsList.stream()
                    .map(Withdrawal::getCoins)
                    .filter(Objects::nonNull)
                    .map(BigDecimal::new)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            stats.put("processedWithdrawalsCount", processedWithdrawals);
            stats.put("processedAmount", processedAmount);
            stats.put("processedCoins", processedCoins);
            
            // Pending withdrawals with amount
            List<Withdrawal> pendingWithdrawalsList = withdrawalRepository.findByStatusAndCreatedAtBetween(Withdrawal.WithdrawalStatus.PENDING, fromDate, toDate);
            BigDecimal pendingAmount = pendingWithdrawalsList.stream()
                    .map(Withdrawal::getMoneyInRs)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal pendingCoins = pendingWithdrawalsList.stream()
                    .map(Withdrawal::getCoins)
                    .filter(Objects::nonNull)
                    .map(BigDecimal::new)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            stats.put("pendingWithdrawalsCount", pendingWithdrawals);
            stats.put("pendingAmount", pendingAmount);
            stats.put("pendingCoins", pendingCoins);
            
            // Rejected withdrawals with amount
            List<Withdrawal> rejectedWithdrawalsList = withdrawalRepository.findByStatusAndCreatedAtBetween(Withdrawal.WithdrawalStatus.REJECTED, fromDate, toDate);
            BigDecimal rejectedAmount = rejectedWithdrawalsList.stream()
                    .map(Withdrawal::getMoneyInRs)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal rejectedCoins = rejectedWithdrawalsList.stream()
                    .map(Withdrawal::getCoins)
                    .filter(Objects::nonNull)
                    .map(BigDecimal::new)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            stats.put("rejectedWithdrawalsCount", rejectedWithdrawals);
            stats.put("rejectedAmount", rejectedAmount);
            stats.put("rejectedCoins", rejectedCoins);
            
            // Processing withdrawals with amount
            List<Withdrawal> processingWithdrawalsList = withdrawalRepository.findByStatusAndCreatedAtBetween(Withdrawal.WithdrawalStatus.PROCESSING, fromDate, toDate);
            BigDecimal processingAmount = processingWithdrawalsList.stream()
                    .map(Withdrawal::getMoneyInRs)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal processingCoins = processingWithdrawalsList.stream()
                    .map(Withdrawal::getCoins)
                    .filter(Objects::nonNull)
                    .map(BigDecimal::new)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            stats.put("processingWithdrawalsCount", processingWithdrawals);
            stats.put("processingAmount", processingAmount);
            stats.put("processingCoins", processingCoins);
            
            // Success rate calculations
            double successRate = totalWithdrawalsInRange > 0 ? 
                ((double) processedWithdrawals / totalWithdrawalsInRange) * 100 : 0;
            stats.put("successRate", Math.round(successRate * 100.0) / 100.0);
            
            // Rejection rate
            double rejectionRate = totalWithdrawalsInRange > 0 ? 
                ((double) rejectedWithdrawals / totalWithdrawalsInRange) * 100 : 0;
            stats.put("rejectionRate", Math.round(rejectionRate * 100.0) / 100.0);
            
            // Processing rate
            double processingRate = totalWithdrawalsInRange > 0 ? 
                ((double) processingWithdrawals / totalWithdrawalsInRange) * 100 : 0;
            stats.put("processingRate", Math.round(processingRate * 100.0) / 100.0);
            
            // Pending rate
            double pendingRate = totalWithdrawalsInRange > 0 ? 
                ((double) pendingWithdrawals / totalWithdrawalsInRange) * 100 : 0;
            stats.put("pendingRate", Math.round(pendingRate * 100.0) / 100.0);
            
            // Average withdrawal amount in date range
            double avgWithdrawalAmount = totalWithdrawalsInRange > 0 ? 
                totalAmountInRange.doubleValue() / totalWithdrawalsInRange : 0;
            stats.put("avgWithdrawalAmount", Math.round(avgWithdrawalAmount * 100.0) / 100.0);
            
            // Average processed withdrawal amount
            double avgProcessedAmount = processedWithdrawals > 0 ? 
                processedAmount.doubleValue() / processedWithdrawals : 0;
            stats.put("avgProcessedAmount", Math.round(avgProcessedAmount * 100.0) / 100.0);
            
            // Average pending withdrawal amount
            double avgPendingAmount = pendingWithdrawals > 0 ? 
                pendingAmount.doubleValue() / pendingWithdrawals : 0;
            stats.put("avgPendingAmount", Math.round(avgPendingAmount * 100.0) / 100.0);
            
            // Average daily withdrawals
            long durationDays = java.time.Duration.between(fromDate, toDate).toDays();
            double avgDailyWithdrawals = durationDays > 0 ? (double) totalWithdrawalsInRange / durationDays : 0;
            stats.put("avgDailyWithdrawals", Math.round(avgDailyWithdrawals * 100.0) / 100.0);
            
            // Average daily processed withdrawals
            double avgDailyProcessed = durationDays > 0 ? (double) processedWithdrawals / durationDays : 0;
            stats.put("avgDailyProcessed", Math.round(avgDailyProcessed * 100.0) / 100.0);
            
            // Growth rate (comparing with previous period)
            LocalDateTime previousFromDate = fromDate.minus(java.time.Duration.between(fromDate, toDate));
            long previousPeriodWithdrawals = withdrawalRepository.countByCreatedAtBetween(previousFromDate, fromDate);
            double growthRate = previousPeriodWithdrawals > 0 ? 
                ((double) (totalWithdrawalsInRange - previousPeriodWithdrawals) / previousPeriodWithdrawals) * 100 : 0;
            stats.put("growthRate", Math.round(growthRate * 100.0) / 100.0);
            
            // Amount growth rate
            List<Withdrawal> previousPeriodWithdrawalsList = withdrawalRepository.findByCreatedAtBetween(previousFromDate, fromDate);
            BigDecimal previousPeriodAmount = previousPeriodWithdrawalsList.stream()
                    .map(Withdrawal::getMoneyInRs)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            double amountGrowthRate = previousPeriodAmount.compareTo(BigDecimal.ZERO) > 0 ? 
                ((totalAmountInRange.doubleValue() - previousPeriodAmount.doubleValue()) / previousPeriodAmount.doubleValue()) * 100 : 0;
            stats.put("amountGrowthRate", Math.round(amountGrowthRate * 100.0) / 100.0);
            
            // Top withdrawal amounts in date range
            List<Map<String, Object>> topWithdrawals = withdrawalsInRange.stream()
                    .filter(w -> w.getMoneyInRs() != null)
                    .sorted((w1, w2) -> w2.getMoneyInRs().compareTo(w1.getMoneyInRs()))
                    .limit(5)
                    .map(w -> {
                        Map<String, Object> withdrawalInfo = new HashMap<>();
                        withdrawalInfo.put("withdrawalId", w.getId());
                        withdrawalInfo.put("userId", w.getUserId());
                        withdrawalInfo.put("amount", w.getMoneyInRs());
                        withdrawalInfo.put("coins", w.getCoins());
                        withdrawalInfo.put("status", w.getStatus());
                        withdrawalInfo.put("createdAt", w.getCreatedAt());
                        return withdrawalInfo;
                    })
                    .collect(java.util.stream.Collectors.toList());
            stats.put("topWithdrawals", topWithdrawals);
            
            // Status distribution summary
            Map<String, Object> statusDistribution = new HashMap<>();
            statusDistribution.put("processed", Map.of("count", processedWithdrawals, "amount", processedAmount, "percentage", successRate));
            statusDistribution.put("pending", Map.of("count", pendingWithdrawals, "amount", pendingAmount, "percentage", pendingRate));
            statusDistribution.put("processing", Map.of("count", processingWithdrawals, "amount", processingAmount, "percentage", processingRate));
            statusDistribution.put("rejected", Map.of("count", rejectedWithdrawals, "amount", rejectedAmount, "percentage", rejectionRate));
            stats.put("statusDistribution", statusDistribution);
            
        } catch (Exception e) {
            log.warn("Error calculating withdrawal stats: {}", e.getMessage());
            stats.put("error", "Unable to calculate withdrawal statistics");
        }
        
        return stats;
    }
    
    /**
     * Parse date string that can be either LocalDateTime or ISO 8601 with timezone
     * @param dateString The date string to parse
     * @return LocalDateTime object
     */
    private LocalDateTime parseDateTime(String dateString) {
        try {
            // First try to parse as LocalDateTime (without timezone)
            return LocalDateTime.parse(dateString);
        } catch (java.time.format.DateTimeParseException e) {
            try {
                // If that fails, try to parse as ISO 8601 with timezone and convert to LocalDateTime
                java.time.ZonedDateTime zonedDateTime = java.time.ZonedDateTime.parse(dateString);
                return zonedDateTime.toLocalDateTime();
            } catch (java.time.format.DateTimeParseException e2) {
                // If both fail, try to parse as Instant and convert to LocalDateTime
                java.time.Instant instant = java.time.Instant.parse(dateString);
                return LocalDateTime.ofInstant(instant, java.time.ZoneOffset.UTC);
            }
        }
    }
    
    // ==================== HELPER METHODS ====================
    
    private Pageable createPageable(int page, int size, String sortBy, String sortOrder) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortOrder.toUpperCase()), sortBy);
        return PageRequest.of(page, size, sort);
    }
} 