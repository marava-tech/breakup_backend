package com.breakupstories.controller;

import com.breakupstories.dto.ErrorResponse;
import com.breakupstories.dto.StoryRewriteRequest;
import com.breakupstories.dto.StoryRewriteResponse;
import com.breakupstories.exception.AIServiceException;
import com.breakupstories.service.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for story rewrite operations
 */
@RestController
@RequestMapping("/api/v1/story-rewrite")
@RequiredArgsConstructor
@Slf4j
public class StoryRewriteController {
    
    private final AIService aiService;
    
    /**
     * Rewrite story from transcript
     */
    @PostMapping("/rewrite")
    public ResponseEntity<?> rewriteStory(@RequestBody StoryRewriteRequest request) {
        log.info("Story rewrite request - Language: {}, Transcript length: {}", 
                request.getLanguage(), request.getTranscript().length());
        
        try {
            String rewrittenStory = aiService.rewriteStory(request.getTranscript(), request.getLanguage());
            
            StoryRewriteResponse response = StoryRewriteResponse.builder()
                    .originalTranscript(request.getTranscript())
                    .rewrittenStory(rewrittenStory)
                    .language(request.getLanguage())
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (AIServiceException e) {
            log.error("AI Service error in story rewrite: {} - Service: {}, Error Code: {}", 
                    e.getMessage(), e.getServiceName(), e.getErrorCode());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error(e.getErrorCode())
                            .message(e.getMessage())
                            .service(e.getServiceName())
                            .timestamp(System.currentTimeMillis())
                            .build());
        } catch (Exception e) {
            log.error("Unexpected error in story rewrite: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.builder()
                            .error("INTERNAL_SERVER_ERROR")
                            .message("An unexpected error occurred during story rewrite")
                            .service("Story Rewrite")
                            .timestamp(System.currentTimeMillis())
                            .build());
        }
    }
} 