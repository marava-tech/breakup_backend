package com.breakupstories.controller;

import com.breakupstories.dto.*;
import com.breakupstories.enums.GENDER;
import com.breakupstories.enums.Role;
import com.breakupstories.model.*;
import com.breakupstories.repository.*;
import com.breakupstories.service.*;
import org.springframework.security.core.userdetails.UserDetails;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Management", description = "Administrative endpoints for managing stories, users, comments, and feedbacks")
public class AdminController {
    
    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final FeedbackRepository feedbackRepository;
    private final LikeRepository likeRepository;
    private final StoryService storyService;
    private final UserService userService;
    private final FeedbackService feedbackService;
    private final AuditService auditService;
    
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
                            Story::getLanguage,
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ==================== USER MANAGEMENT ====================
    
    @GetMapping("/users")
    public ResponseEntity<PagedResponse<UserResponse>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) Integer minCoins,
            @RequestParam(required = false) Integer maxCoins,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        try {
            Pageable pageable = createPageable(page, size, sortBy, sortOrder);
            Page<User> userPage;
            
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
            
            // Apply filters based on provided parameters
            if (userId != null) {
                // If userId is provided, use it as the primary filter
                if (username != null && email != null && roleFilter != null && genderFilter != null && minCoins != null && maxCoins != null) {
                    userPage = userRepository.findByNameContainingIgnoreCaseAndRoleAndGenderAndIdAndCoinBalanceBetween(username, roleFilter, genderFilter, userId, minCoins, maxCoins, pageable);
                } else if (email != null && roleFilter != null && genderFilter != null && minCoins != null && maxCoins != null) {
                    userPage = userRepository.findByEmailContainingIgnoreCaseAndRoleAndGenderAndIdAndCoinBalanceBetween(email, roleFilter, genderFilter, userId, minCoins, maxCoins, pageable);
                } else if (username != null && roleFilter != null && genderFilter != null) {
                    userPage = userRepository.findByNameContainingIgnoreCaseAndRoleAndGenderAndId(username, roleFilter, genderFilter, userId, pageable);
                } else if (email != null && roleFilter != null && genderFilter != null) {
                    userPage = userRepository.findByEmailContainingIgnoreCaseAndRoleAndGenderAndId(email, roleFilter, genderFilter, userId, pageable);
                } else if (username != null && roleFilter != null && minCoins != null && maxCoins != null) {
                    userPage = userRepository.findByNameContainingIgnoreCaseAndRoleAndIdAndCoinBalanceBetween(username, roleFilter, userId, minCoins, maxCoins, pageable);
                } else if (email != null && roleFilter != null && minCoins != null && maxCoins != null) {
                    userPage = userRepository.findByEmailContainingIgnoreCaseAndRoleAndIdAndCoinBalanceBetween(email, roleFilter, userId, minCoins, maxCoins, pageable);
                } else if (username != null && genderFilter != null && minCoins != null && maxCoins != null) {
                    userPage = userRepository.findByNameContainingIgnoreCaseAndGenderAndIdAndCoinBalanceBetween(username, genderFilter, userId, minCoins, maxCoins, pageable);
                } else if (email != null && genderFilter != null && minCoins != null && maxCoins != null) {
                    userPage = userRepository.findByEmailContainingIgnoreCaseAndGenderAndIdAndCoinBalanceBetween(email, genderFilter, userId, minCoins, maxCoins, pageable);
                } else if (roleFilter != null && genderFilter != null && minCoins != null && maxCoins != null) {
                    userPage = userRepository.findByRoleAndGenderAndIdAndCoinBalanceBetween(roleFilter, genderFilter, userId, minCoins, maxCoins, pageable);
                } else if (username != null && roleFilter != null) {
                    userPage = userRepository.findByNameContainingIgnoreCaseAndRoleAndId(username, roleFilter, userId, pageable);
                } else if (email != null && roleFilter != null) {
                    userPage = userRepository.findByEmailContainingIgnoreCaseAndRoleAndId(email, roleFilter, userId, pageable);
                } else if (username != null && genderFilter != null) {
                    userPage = userRepository.findByNameContainingIgnoreCaseAndGenderAndId(username, genderFilter, userId, pageable);
                } else if (email != null && genderFilter != null) {
                    userPage = userRepository.findByEmailContainingIgnoreCaseAndGenderAndId(email, genderFilter, userId, pageable);
                } else if (username != null && minCoins != null && maxCoins != null) {
                    userPage = userRepository.findByNameContainingIgnoreCaseAndIdAndCoinBalanceBetween(username, userId, minCoins, maxCoins, pageable);
                } else if (email != null && minCoins != null && maxCoins != null) {
                    userPage = userRepository.findByEmailContainingIgnoreCaseAndIdAndCoinBalanceBetween(email, userId, minCoins, maxCoins, pageable);
                } else if (roleFilter != null && genderFilter != null) {
                    userPage = userRepository.findByRoleAndGenderAndId(roleFilter, genderFilter, userId, pageable);
                } else if (roleFilter != null && minCoins != null && maxCoins != null) {
                    userPage = userRepository.findByRoleAndIdAndCoinBalanceBetween(roleFilter, userId, minCoins, maxCoins, pageable);
                } else if (genderFilter != null && minCoins != null && maxCoins != null) {
                    userPage = userRepository.findByGenderAndIdAndCoinBalanceBetween(genderFilter, userId, minCoins, maxCoins, pageable);
                } else if (username != null) {
                    userPage = userRepository.findByNameContainingIgnoreCaseAndId(username, userId, pageable);
                } else if (email != null) {
                    userPage = userRepository.findByEmailContainingIgnoreCaseAndId(email, userId, pageable);
                } else if (roleFilter != null) {
                    userPage = userRepository.findByRoleAndId(roleFilter, userId, pageable);
                } else if (genderFilter != null) {
                    userPage = userRepository.findByGenderAndId(genderFilter, userId, pageable);
                } else if (minCoins != null && maxCoins != null) {
                    userPage = userRepository.findByIdAndCoinBalanceBetween(userId, minCoins, maxCoins, pageable);
                } else {
                    // Only userId filter
                    userPage = userRepository.findById(userId, pageable);
                }
            } else {
                // No userId filter, use existing logic
                if (username != null && email != null && roleFilter != null && genderFilter != null && minCoins != null && maxCoins != null) {
                    userPage = userRepository.findByNameContainingIgnoreCaseAndRoleAndGenderAndCoinBalanceBetween(username, roleFilter, genderFilter, minCoins, maxCoins, pageable);
                } else if (email != null && roleFilter != null && genderFilter != null && minCoins != null && maxCoins != null) {
                    userPage = userRepository.findByEmailContainingIgnoreCaseAndRoleAndGenderAndCoinBalanceBetween(email, roleFilter, genderFilter, minCoins, maxCoins, pageable);
                } else if (username != null && roleFilter != null && genderFilter != null) {
                    userPage = userRepository.findByNameContainingIgnoreCaseAndRoleAndGender(username, roleFilter, genderFilter, pageable);
                } else if (email != null && roleFilter != null && genderFilter != null) {
                    userPage = userRepository.findByEmailContainingIgnoreCaseAndRoleAndGender(email, roleFilter, genderFilter, pageable);
                } else if (username != null && roleFilter != null && minCoins != null && maxCoins != null) {
                    userPage = userRepository.findByNameContainingIgnoreCaseAndRoleAndCoinBalanceBetween(username, roleFilter, minCoins, maxCoins, pageable);
                } else if (email != null && roleFilter != null && minCoins != null && maxCoins != null) {
                    userPage = userRepository.findByEmailContainingIgnoreCaseAndRoleAndCoinBalanceBetween(email, roleFilter, minCoins, maxCoins, pageable);
                } else if (username != null && genderFilter != null && minCoins != null && maxCoins != null) {
                    userPage = userRepository.findByNameContainingIgnoreCaseAndGenderAndCoinBalanceBetween(username, genderFilter, minCoins, maxCoins, pageable);
                } else if (email != null && genderFilter != null && minCoins != null && maxCoins != null) {
                    userPage = userRepository.findByEmailContainingIgnoreCaseAndGenderAndCoinBalanceBetween(email, genderFilter, minCoins, maxCoins, pageable);
                } else if (roleFilter != null && genderFilter != null && minCoins != null && maxCoins != null) {
                    userPage = userRepository.findByRoleAndGenderAndCoinBalanceBetween(roleFilter, genderFilter, minCoins, maxCoins, pageable);
                } else if (username != null && roleFilter != null) {
                    userPage = userRepository.findByNameContainingIgnoreCaseAndRole(username, roleFilter, pageable);
                } else if (email != null && roleFilter != null) {
                    userPage = userRepository.findByEmailContainingIgnoreCaseAndRole(email, roleFilter, pageable);
                } else if (username != null && genderFilter != null) {
                    userPage = userRepository.findByNameContainingIgnoreCaseAndGender(username, genderFilter, pageable);
                } else if (email != null && genderFilter != null) {
                    userPage = userRepository.findByEmailContainingIgnoreCaseAndGender(email, genderFilter, pageable);
                } else if (username != null && minCoins != null && maxCoins != null) {
                    userPage = userRepository.findByNameContainingIgnoreCaseAndCoinBalanceBetween(username, minCoins, maxCoins, pageable);
                } else if (email != null && minCoins != null && maxCoins != null) {
                    userPage = userRepository.findByEmailContainingIgnoreCaseAndCoinBalanceBetween(email, minCoins, maxCoins, pageable);
                } else if (roleFilter != null && genderFilter != null) {
                    userPage = userRepository.findByRoleAndGender(roleFilter, genderFilter, pageable);
                } else if (roleFilter != null && minCoins != null && maxCoins != null) {
                    userPage = userRepository.findByRoleAndCoinBalanceBetween(roleFilter, minCoins, maxCoins, pageable);
                } else if (genderFilter != null && minCoins != null && maxCoins != null) {
                    userPage = userRepository.findByGenderAndCoinBalanceBetween(genderFilter, minCoins, maxCoins, pageable);
                } else if (username != null) {
                    userPage = userRepository.findByNameContainingIgnoreCase(username, pageable);
                } else if (email != null) {
                    userPage = userRepository.findByEmailContainingIgnoreCase(email, pageable);
                } else if (roleFilter != null) {
                    userPage = userRepository.findByRole(roleFilter, pageable);
                } else if (genderFilter != null) {
                    userPage = userRepository.findByGender(genderFilter, pageable);
                } else if (minCoins != null && maxCoins != null) {
                    userPage = userRepository.findByCoinBalanceBetween(minCoins, maxCoins, pageable);
                } else if (minCoins != null) {
                    userPage = userRepository.findByCoinBalanceGreaterThanEqual(minCoins, pageable);
                } else if (maxCoins != null) {
                    userPage = userRepository.findByCoinBalanceLessThanEqual(maxCoins, pageable);
                } else {
                    // No filters applied, return all users
                    userPage = userRepository.findAll(pageable);
                }
            }
            
            List<UserResponse> users = userPage.getContent().stream()
                    .map(UserResponse::fromUser)
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
                    .mapToInt(User::getCoinBalance)
                    .average()
                    .orElse(0.0);
            stats.put("averageCoins", avgCoins);
            
            int maxCoins = allUsers.stream()
                    .mapToInt(User::getCoinBalance)
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
    
    // ==================== FEEDBACK MANAGEMENT ====================
    
    @GetMapping("/feedbacks")
    public ResponseEntity<PagedResponse<FeedbackResponse>> getFeedbacks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String storyId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        
        try {
            Pageable pageable = createPageable(page, size, sortBy, sortOrder);
            Page<Feedback> feedbackPage = feedbackRepository.findAll(pageable);
            
            List<FeedbackResponse> feedbacks = feedbackPage.getContent().stream()
                    .map(feedbackService::enrichFeedbackResponse)
                    .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(PagedResponse.of(feedbacks, page, size, feedbackPage.getTotalElements()));
            
        } catch (Exception e) {
            log.error("Error fetching feedbacks for admin: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PutMapping("/feedbacks/{feedbackId}")
    public ResponseEntity<Map<String, Object>> updateFeedback(
            @PathVariable String feedbackId,
            @RequestBody FeedbackUpdateRequest updates) {
        
        try {
            Feedback feedback = feedbackRepository.findById(feedbackId)
                    .orElseThrow(() -> new RuntimeException("Feedback not found"));
            
            // Update allowed fields
            if (updates.getStatus() != null) {
                feedback.setStatus(Feedback.FeedbackStatus.valueOf(updates.getStatus()));
            }
            if (updates.getAdminResponse() != null) {
                feedback.setAdminResponse(updates.getAdminResponse());
            }
            
            feedbackRepository.save(feedback);
            
            // Log audit
            auditService.logAudit("admin", Audit.EntityType.FEEDBACK, Audit.ActionType.UPDATE, feedbackId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Feedback updated successfully");
            response.put("feedbackId", feedbackId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error updating feedback: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/feedbacks/statistics")
    public ResponseEntity<Map<String, Object>> getFeedbackStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Total feedbacks
            long totalFeedbacks = feedbackRepository.count();
            stats.put("totalFeedbacks", totalFeedbacks);
            
            // Feedbacks by status
            stats.put("pendingFeedbacks", feedbackRepository.countByStatus(Feedback.FeedbackStatus.PENDING));
            stats.put("inProgressFeedbacks", feedbackRepository.countByStatus(Feedback.FeedbackStatus.IN_REVIEW));
            stats.put("resolvedFeedbacks", feedbackRepository.countByStatus(Feedback.FeedbackStatus.RESOLVED));
            stats.put("rejectedFeedbacks", feedbackRepository.countByStatus(Feedback.FeedbackStatus.REJECTED));
            
            // Feedbacks by type
            List<Map<String, Object>> typeStats = feedbackRepository.findAll().stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            Feedback::getType,
                            java.util.stream.Collectors.counting()))
                    .entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> typeStat = new HashMap<>();
                        typeStat.put("type", entry.getKey());
                        typeStat.put("count", entry.getValue());
                        return typeStat;
                    })
                    .collect(java.util.stream.Collectors.toList());
            stats.put("typeStats", typeStats);
            
            // Recent feedbacks (last 7 days)
            LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
            long recentFeedbacks = feedbackRepository.countByCreatedAtAfter(weekAgo);
            stats.put("recentFeedbacks", recentFeedbacks);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error fetching feedback statistics: {}", e.getMessage(), e);
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
                            Story::getLanguage,
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
            double avgDailyStories = durationDays > 0 ? (double) totalStoriesInRange / durationDays : 0;
            stats.put("avgDailyStories", Math.round(avgDailyStories * 100.0) / 100.0);
            
            // Success rate
            double successRate = totalStoriesInRange > 0 ? 
                ((double) activeStories / totalStoriesInRange) * 100 : 0;
            stats.put("successRate", Math.round(successRate * 100.0) / 100.0);
            
        } catch (Exception e) {
            log.warn("Error calculating story creation stats: {}", e.getMessage());
            stats.put("error", "Unable to calculate story creation statistics");
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