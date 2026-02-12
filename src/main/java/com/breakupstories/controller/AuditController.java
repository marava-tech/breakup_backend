package com.breakupstories.controller;

import com.breakupstories.dto.AuditResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audits")
@RequiredArgsConstructor
@Tag(name = "Audits", description = "Audit trail management APIs")
public class AuditController {
    
    private final AuditService auditService;
    
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get all audits", description = "Retrieve paginated list of all audit entries")
    public ResponseEntity<PagedResponse<AuditResponse>> getAudits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam( defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        
        PagedResponse<AuditResponse> response = auditService.getAudits(page, size, sortBy, sortOrder);
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
    
} 