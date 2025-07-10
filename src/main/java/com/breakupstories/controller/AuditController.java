package com.breakupstories.controller;

import com.breakupstories.dto.AuditRequest;
import com.breakupstories.dto.AuditResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.model.Audit;
import com.breakupstories.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/audits")
@RequiredArgsConstructor
@Tag(name = "Audits", description = "Audit trail management APIs")
public class AuditController {
    
    private final AuditService auditService;
    
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Create a new audit entry", description = "Create a new audit entry for tracking changes")
    public ResponseEntity<AuditResponse> createAudit(@Valid @RequestBody AuditRequest request) {
        AuditResponse response = auditService.createAudit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get all audits", description = "Retrieve paginated list of all audit entries")
    public ResponseEntity<PagedResponse<AuditResponse>> getAudits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        PagedResponse<AuditResponse> response = auditService.getAudits(page, size);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get audits by user", description = "Retrieve paginated audit entries for a specific user")
    public ResponseEntity<PagedResponse<AuditResponse>> getAuditsByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        
        PagedResponse<AuditResponse> response = auditService.getAuditsByUser(userId, page, size, sortBy, sortOrder);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/entity-type/{entityType}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get audits by entity type", description = "Retrieve paginated audit entries by entity type")
    public ResponseEntity<PagedResponse<AuditResponse>> getAuditsByEntityType(
            @PathVariable Audit.EntityType entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        PagedResponse<AuditResponse> response = auditService.getAuditsByEntityType(entityType, page, size);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/entity/{entityId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get audits by entity ID", description = "Retrieve paginated audit entries for a specific entity")
    public ResponseEntity<PagedResponse<AuditResponse>> getAuditsByEntityId(
            @PathVariable String entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        PagedResponse<AuditResponse> response = auditService.getAuditsByEntityId(entityId, page, size);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/action-type/{actionType}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get audits by action type", description = "Retrieve paginated audit entries by action type")
    public ResponseEntity<PagedResponse<AuditResponse>> getAuditsByActionType(
            @PathVariable Audit.ActionType actionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        PagedResponse<AuditResponse> response = auditService.getAuditsByActionType(actionType, page, size);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user/{userId}/entity-type/{entityType}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get audits by user and entity type", description = "Retrieve paginated audit entries for a specific user and entity type")
    public ResponseEntity<PagedResponse<AuditResponse>> getAuditsByUserAndEntityType(
            @PathVariable String userId,
            @PathVariable Audit.EntityType entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        PagedResponse<AuditResponse> response = auditService.getAuditsByUserAndEntityType(userId, entityType, page, size);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/analytics/story-views")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get story view analytics", description = "Get analytics for story views")
    public ResponseEntity<Map<String, Object>> getStoryViewAnalytics(
            @RequestParam(required = false) String storyId,
            @RequestParam(required = false) String userId) {
        
        // This would typically call a service method for analytics
        // For now, return a placeholder response
        Map<String, Object> analytics = Map.of(
            "total_views", 0,
            "unique_users", 0,
            "story_id", storyId,
            "user_id", userId
        );
        
        return ResponseEntity.ok(analytics);
    }
    

    
    @GetMapping("/analytics/user-activity")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get user activity analytics", description = "Get analytics for user activity")
    public ResponseEntity<Map<String, Object>> getUserActivityAnalytics(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String dateRange) {
        
        // This would typically call a service method for analytics
        // For now, return a placeholder response
        Map<String, Object> analytics = Map.of(
            "total_actions", 0,
            "story_views", 0,
            "likes", 0,
            "comments", 0,
            "user_id", userId,
            "date_range", dateRange
        );
        
        return ResponseEntity.ok(analytics);
    }
    
    @GetMapping("/{auditId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get audit by ID", description = "Retrieve a specific audit entry by its ID")
    public ResponseEntity<AuditResponse> getAuditById(@PathVariable String auditId) {
        AuditResponse response = auditService.getAuditById(auditId);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{auditId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Delete audit", description = "Delete an audit entry by its ID")
    public ResponseEntity<Void> deleteAudit(@PathVariable String auditId) {
        auditService.deleteAudit(auditId);
        return ResponseEntity.noContent().build();
    }
} 