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

@RestController
@RequestMapping("/api/audits")
@RequiredArgsConstructor
@Tag(name = "Audits", description = "Audit trail management APIs")
public class AuditController {
    
    private final AuditService auditService;
    
    @PostMapping
    @Operation(summary = "Create a new audit entry", description = "Create a new audit entry for tracking changes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditResponse> createAudit(@Valid @RequestBody AuditRequest request) {
        AuditResponse response = auditService.createAudit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    @Operation(summary = "Get all audits", description = "Retrieve paginated list of all audit entries")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<AuditResponse>> getAudits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        PagedResponse<AuditResponse> response = auditService.getAudits(page, size);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get audits by user", description = "Retrieve paginated audit entries for a specific user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<AuditResponse>> getAuditsByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        PagedResponse<AuditResponse> response = auditService.getAuditsByUser(userId, page, size);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/entity-type/{entityType}")
    @Operation(summary = "Get audits by entity type", description = "Retrieve paginated audit entries by entity type")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<AuditResponse>> getAuditsByEntityType(
            @PathVariable Audit.EntityType entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        PagedResponse<AuditResponse> response = auditService.getAuditsByEntityType(entityType, page, size);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/entity/{entityId}")
    @Operation(summary = "Get audits by entity ID", description = "Retrieve paginated audit entries for a specific entity")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<AuditResponse>> getAuditsByEntityId(
            @PathVariable String entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        PagedResponse<AuditResponse> response = auditService.getAuditsByEntityId(entityId, page, size);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{auditId}")
    @Operation(summary = "Get audit by ID", description = "Retrieve a specific audit entry by its ID")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditResponse> getAuditById(@PathVariable String auditId) {
        AuditResponse response = auditService.getAuditById(auditId);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{auditId}")
    @Operation(summary = "Delete audit", description = "Delete an audit entry by its ID")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAudit(@PathVariable String auditId) {
        auditService.deleteAudit(auditId);
        return ResponseEntity.noContent().build();
    }
} 