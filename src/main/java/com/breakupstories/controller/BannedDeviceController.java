package com.breakupstories.controller;

import com.breakupstories.dto.BanDeviceRequest;
import com.breakupstories.dto.BanUpdateRequest;
import com.breakupstories.dto.BannedDeviceResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.service.BannedDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/banned-devices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Banned Device Management", description = "APIs for managing banned devices")
@PreAuthorize("hasRole('ADMIN')")
public class BannedDeviceController {
    
    private final BannedDeviceService bannedDeviceService;
    
    /**
     * Ban a device with a specific reason
     */
    @PostMapping("/ban")
    @Operation(summary = "Ban a device", description = "Ban a device with a reason and fetch all associated user emails")
    public ResponseEntity<BannedDeviceResponse> banDevice(@Valid @RequestBody BanDeviceRequest request) {
        try {
            BannedDeviceResponse response = bannedDeviceService.banDevice(request.getDeviceId(), request.getReason());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error banning device {}: {}", request.getDeviceId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    
    /**
     * Get banned device details
     */
    @GetMapping("/{deviceId}")
    @Operation(summary = "Get banned device details", description = "Get details of a banned device including associated emails")
    public ResponseEntity<BannedDeviceResponse> getBannedDevice(@PathVariable String deviceId) {
        try {
            BannedDeviceResponse response = bannedDeviceService.getBannedDevice(deviceId);
            if (response != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting banned device {}: {}", deviceId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get all banned devices with pagination
     */
    @GetMapping
    @Operation(summary = "Get all banned devices", description = "Retrieve paginated list of all banned devices")
    public ResponseEntity<PagedResponse<BannedDeviceResponse>> getAllBannedDevices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        try {
            PagedResponse<BannedDeviceResponse> response = bannedDeviceService.getAllBannedDevices(page, size, sortBy, sortOrder);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting all banned devices: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Comprehensive search banned devices by device ID, reason, or email
     */
    @GetMapping("/search")
    @Operation(summary = "Search banned devices", 
               description = "Search banned devices by device ID, reason, or email (case-insensitive)")
    public ResponseEntity<PagedResponse<BannedDeviceResponse>> searchBannedDevices(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String reason,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        try {
            PagedResponse<BannedDeviceResponse> response;
            
            // If searchTerm is provided, do comprehensive search
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                response = bannedDeviceService.searchBannedDevices(searchTerm, page, size, sortBy, sortOrder);
            }
            // If specific field searches are provided
            else if (deviceId != null && !deviceId.trim().isEmpty()) {
                response = bannedDeviceService.searchBannedDevicesByDeviceId(deviceId, page, size, sortBy, sortOrder);
            }
            else if (email != null && !email.trim().isEmpty()) {
                response = bannedDeviceService.searchBannedDevicesByEmail(email, page, size, sortBy, sortOrder);
            }
            else if (reason != null && !reason.trim().isEmpty()) {
                response = bannedDeviceService.searchBannedDevicesByReason(reason, page, size, sortBy, sortOrder);
            }
            // If no search parameters provided, return all
            else {
                response = bannedDeviceService.getAllBannedDevices(page, size, sortBy, sortOrder);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error searching banned devices: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Search banned devices by device ID only
     */
    @GetMapping("/search/device")
    @Operation(summary = "Search banned devices by device ID", 
               description = "Search banned devices by device ID (case-insensitive)")
    public ResponseEntity<PagedResponse<BannedDeviceResponse>> searchBannedDevicesByDeviceId(
            @RequestParam String deviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        try {
            PagedResponse<BannedDeviceResponse> response = bannedDeviceService.searchBannedDevicesByDeviceId(
                    deviceId, page, size, sortBy, sortOrder);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error searching banned devices by device ID '{}': {}", deviceId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Search banned devices by email only
     */
    @GetMapping("/search/email")
    @Operation(summary = "Search banned devices by email", 
               description = "Search banned devices by email (case-insensitive)")
    public ResponseEntity<PagedResponse<BannedDeviceResponse>> searchBannedDevicesByEmail(
            @RequestParam String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        try {
            PagedResponse<BannedDeviceResponse> response = bannedDeviceService.searchBannedDevicesByEmail(
                    email, page, size, sortBy, sortOrder);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error searching banned devices by email '{}': {}", email, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Search banned devices by reason only
     */
    @GetMapping("/search/reason")
    @Operation(summary = "Search banned devices by reason", 
               description = "Search banned devices by reason (case-insensitive)")
    public ResponseEntity<PagedResponse<BannedDeviceResponse>> searchBannedDevicesByReason(
            @RequestParam String reason,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        try {
            PagedResponse<BannedDeviceResponse> response = bannedDeviceService.searchBannedDevicesByReason(
                    reason, page, size, sortBy, sortOrder);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error searching banned devices by reason '{}': {}", reason, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Update a banned device's reason and manage emails
     */
    @PutMapping("/{deviceId}")
    @Operation(summary = "Update banned device", description = "Update the ban reason and manage emails for a banned device")
    public ResponseEntity<BannedDeviceResponse> updateBannedDevice(
            @PathVariable String deviceId,
            @Valid @RequestBody BanUpdateRequest request) {
        try {
            BannedDeviceResponse response = bannedDeviceService.updateBannedDevice(
                    deviceId, 
                    request.getReason(),
                    request.getEmailsToAdd(),
                    request.getEmailsToRemove()
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Device is not banned")) {
                return ResponseEntity.notFound().build();
            }
            log.error("Error updating banned device {}: {}", deviceId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Error updating banned device {}: {}", deviceId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Unban a device
     */
    @DeleteMapping("/unban/{deviceId}")
    @Operation(summary = "Unban a device", description = "Remove a device from the banned devices list")
    public ResponseEntity<Void> unbanDevice(@PathVariable String deviceId) {
        try {
            boolean wasUnbanned = bannedDeviceService.unbanDevice(deviceId);
            if (wasUnbanned) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error unbanning device {}: {}", deviceId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
