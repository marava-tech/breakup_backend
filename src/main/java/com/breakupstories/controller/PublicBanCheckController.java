package com.breakupstories.controller;

import com.breakupstories.dto.EmailBanCheckResponse;
import com.breakupstories.service.BannedDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Public Ban Check", description = "Public APIs for checking ban status")
public class PublicBanCheckController {
    
    private final BannedDeviceService bannedDeviceService;
    
    /**
     * Public endpoint to check if an email is banned (no authentication required)
     */
    @GetMapping("/check-email-ban")
    @Operation(summary = "Check if email is banned (Public API)", 
               description = "Check if an email address is banned and get ban details. No authentication required.")
    public ResponseEntity<EmailBanCheckResponse> checkEmailBanStatus(@RequestParam String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(EmailBanCheckResponse.error(email, "Email is required"));
            }
            
            EmailBanCheckResponse response = bannedDeviceService.checkEmailBanStatus(email.trim());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking email ban status for {}: {}", email, e.getMessage(), e);
            EmailBanCheckResponse errorResponse = EmailBanCheckResponse.error(
                    email, "Error checking email ban status: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
