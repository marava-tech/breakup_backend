package com.breakupstories.controller;

import com.breakupstories.dto.WithdrawalRequest;
import com.breakupstories.dto.WithdrawalResponse;
import com.breakupstories.model.Withdrawal;
import com.breakupstories.service.UserService;
import com.breakupstories.service.WithdrawalService;
import com.breakupstories.service.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.breakupstories.dto.WithdrawalOptionResponse;
import com.breakupstories.dto.WithdrawalOptionsResponse;

@RestController
@RequestMapping("/api/withdrawals")
@RequiredArgsConstructor
@Tag(name = "Withdrawals", description = "Withdrawal management APIs")
public class WithdrawalController {
    
    private final WithdrawalService withdrawalService;
    private final UserService userService;
    private final UploadService uploadService;
    
    @PostMapping
    @Operation(summary = "Create withdrawal request", description = "Create a new withdrawal request with coins and UPI ID")
    public ResponseEntity<Map<String, Object>> createWithdrawal(
            Authentication authentication,
            @Valid @RequestBody WithdrawalRequest request) {
        
        try {
            String email = authentication.getName();
            String userId = userService.getUserEntityByEmail(email).getId();
            
            WithdrawalResponse response = withdrawalService.createWithdrawal(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "data", response));
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("message", "Failed to create withdrawal request");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @PutMapping("/{withdrawalId}/status")
    @Operation(summary = "Update withdrawal status (Admin)", description = "Update withdrawal status to PROCESSING, PROCESSED, or REJECTED with optional proof image")
    public ResponseEntity<Map<String, Object>> updateWithdrawalStatus(
            @PathVariable String withdrawalId,
            MultipartHttpServletRequest request) {
        
        try {
            // Extract status from form data
            String statusParam = request.getParameter("status");
            if (statusParam == null || statusParam.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Status parameter is required");
                errorResponse.put("message", "Status is required for updating withdrawal");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            Withdrawal.WithdrawalStatus status = Withdrawal.WithdrawalStatus.valueOf(statusParam.toUpperCase());
            
            // Handle file upload
            String proofImageUrl = null;
            MultipartFile file = request.getFile("file");
            if (file != null && !file.isEmpty()) {
                proofImageUrl = uploadService.uploadSingleFile(file);
            }
            
            WithdrawalResponse response = withdrawalService.updateWithdrawalStatus(withdrawalId, status, proofImageUrl);
            return ResponseEntity.ok(Map.of("success", true, "data", response));
            
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Invalid status parameter: " + e.getMessage());
            errorResponse.put("message", "Invalid withdrawal status");
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("message", "Failed to update withdrawal status");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @GetMapping("/{withdrawalId}")
    @Operation(summary = "Get withdrawal by ID", description = "Retrieve a specific withdrawal by its ID")
    public ResponseEntity<Map<String, Object>> getWithdrawalById(@PathVariable String withdrawalId) {
        try {
            WithdrawalResponse response = withdrawalService.getWithdrawalById(withdrawalId);
            return ResponseEntity.ok(Map.of("success", true, "data", response));
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("message", "Withdrawal not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }
    
    @GetMapping("/options")
    @Operation(summary = "Get withdrawal options", description = "Get available withdrawal options with amounts, coins, eligibility, and processing time for the authenticated user")
    public ResponseEntity<Map<String, Object>> getWithdrawalOptions(Authentication authentication) {
        try {
            String email = authentication.getName();
            String userId = userService.getUserEntityByEmail(email).getId();
            
            WithdrawalOptionsResponse response = withdrawalService.getWithdrawalOptions(userId);
            return ResponseEntity.ok(Map.of("success", true, "data", response));
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("message", "Failed to retrieve withdrawal options");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @GetMapping("/my-withdrawals")
    @Operation(summary = "Get my withdrawals", description = "Retrieve paginated withdrawals for the authenticated user")
    public ResponseEntity<Map<String, Object>> getWithdrawals(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            String email = authentication.getName();
            String userId = userService.getUserEntityByEmail(email).getId();
            
            List<WithdrawalResponse> response = withdrawalService.getWithdrawalsByUser(userId, page, size);
            return ResponseEntity.ok(Map.of("success", true, "data", response));
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("message", "Failed to retrieve withdrawals");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    // Admin endpoints
    @GetMapping
    @Operation(summary = "Get all withdrawals (Admin)", description = "Retrieve paginated list of all withdrawals")
    public ResponseEntity<Map<String, Object>> getAllWithdrawals(
            @RequestParam(required = false) Withdrawal.WithdrawalStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            List<WithdrawalResponse> response;
            if(ObjectUtils.isNotEmpty(status)){
             response  = withdrawalService.getWithdrawalsByStatus(status, page, size);
            }else {
                response = withdrawalService.getAllWithdrawals(page, size);
            }
            return ResponseEntity.ok(Map.of("success", true, "data", response));
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("message", "Failed to retrieve all withdrawals");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

} 