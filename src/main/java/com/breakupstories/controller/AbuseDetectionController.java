package com.breakupstories.controller;

import com.breakupstories.dto.AbuseDetectionRequest;
import com.breakupstories.dto.AbuseDetectionResponse;
import com.breakupstories.dto.ErrorResponse;
import com.breakupstories.exception.AIServiceException;
import com.breakupstories.service.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for abuse detection functionality
 */
@RestController
@RequestMapping("/api/abuse-detection")
@RequiredArgsConstructor
@Slf4j
public class AbuseDetectionController {
    
    private final AIService aiService;
    
    /**
     * Detect abusive content in comments
     * POST /api/abuse-detection/detect
     */
    @PostMapping("/detect")
    public ResponseEntity<?> detectAbuse(@RequestBody AbuseDetectionRequest request) {
        log.info("Abuse detection request - Language: {}, Comment length: {}", 
                request.getLanguage(), request.getComment().length());
        
        try {
            AbuseDetectionResponse response = aiService.detectAbuse(
                    request.getComment(), 
                    request.getLanguage()
            );
            
            if (response.getSuccess()) {
                log.info("Abuse detection successful - Is Abusive: {}, Confidence: {}, Category: {}", 
                        response.getIs_abusive(), response.getConfidence(), response.getCategory());
                
                return ResponseEntity.ok(response);
            } else {
                log.error("Abuse detection failed - Error: {}", response.getError());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (AIServiceException e) {
            log.error("AI Service error in abuse detection: {} - Service: {}, Error Code: {}", 
                    e.getMessage(), e.getServiceName(), e.getErrorCode());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error(e.getErrorCode())
                            .message(e.getMessage())
                            .service(e.getServiceName())
                            .timestamp(System.currentTimeMillis())
                            .build());
        } catch (Exception e) {
            log.error("Unexpected error in abuse detection: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.builder()
                            .error("INTERNAL_SERVER_ERROR")
                            .message("An unexpected error occurred during abuse detection")
                            .service("Abuse Detection")
                            .timestamp(System.currentTimeMillis())
                            .build());
        }
    }
    
    /**
     * Detect abusive content (GET endpoint for testing)
     * GET /api/abuse-detection/detect?comment=...&language=...
     */
    @GetMapping("/detect")
    public ResponseEntity<?> detectAbuseGet(
            @RequestParam String comment,
            @RequestParam String language) {
        
        log.info("Abuse detection GET request - Language: {}, Comment length: {}", 
                language, comment.length());
        
        try {
            AbuseDetectionRequest request = AbuseDetectionRequest.builder()
                    .comment(comment)
                    .language(language)
                    .build();
            
            AbuseDetectionResponse response = aiService.detectAbuse(
                    request.getComment(), 
                    request.getLanguage()
            );
            
            if (response.getSuccess()) {
                log.info("Abuse detection successful - Is Abusive: {}, Confidence: {}, Category: {}", 
                        response.getIs_abusive(), response.getConfidence(), response.getCategory());
                
                return ResponseEntity.ok(response);
            } else {
                log.error("Abuse detection failed - Error: {}", response.getError());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (AIServiceException e) {
            log.error("AI Service error in abuse detection: {} - Service: {}, Error Code: {}", 
                    e.getMessage(), e.getServiceName(), e.getErrorCode());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error(e.getErrorCode())
                            .message(e.getMessage())
                            .service(e.getServiceName())
                            .timestamp(System.currentTimeMillis())
                            .build());
        } catch (Exception e) {
            log.error("Unexpected error in abuse detection: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.builder()
                            .error("INTERNAL_SERVER_ERROR")
                            .message("An unexpected error occurred during abuse detection")
                            .service("Abuse Detection")
                            .timestamp(System.currentTimeMillis())
                            .build());
        }
    }
} 