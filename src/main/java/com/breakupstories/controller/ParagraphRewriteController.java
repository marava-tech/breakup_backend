package com.breakupstories.controller;

import com.breakupstories.dto.ErrorResponse;
import com.breakupstories.dto.ParagraphRewriteRequest;
import com.breakupstories.dto.ParagraphRewriteResponse;
import com.breakupstories.exception.AIServiceException;
import com.breakupstories.service.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for paragraph rewrite functionality
 */
@RestController
@RequestMapping("/api/paragraph-rewrite")
@RequiredArgsConstructor
@Slf4j
public class ParagraphRewriteController {
    
    private final AIService aiService;
    
    /**
     * Rewrite story into paragraphs
     * POST /api/paragraph-rewrite/rewrite
     */
    @PostMapping("/rewrite")
    public ResponseEntity<?> rewriteStoryIntoParagraphs(@RequestBody ParagraphRewriteRequest request) {
        log.info("Paragraph rewrite request - Language: {}, Transcript length: {}", 
                request.getLanguage(), request.getTranscript().length());
        
        try {
            ParagraphRewriteResponse response = aiService.rewriteStoryIntoParagraphs(
                    request.getTranscript(), 
                    request.getLanguage()
            );
            
            log.info("Paragraph rewrite successful - Language: {}, Contents: {}", 
                    response.getLanguage(), response.getContents().size());
            
            return ResponseEntity.ok(response);
            
        } catch (AIServiceException e) {
            log.error("AI Service error in paragraph rewrite: {} - Service: {}, Error Code: {}", 
                    e.getMessage(), e.getServiceName(), e.getErrorCode());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error(e.getErrorCode())
                            .message(e.getMessage())
                            .service(e.getServiceName())
                            .timestamp(System.currentTimeMillis())
                            .build());
        } catch (Exception e) {
            log.error("Unexpected error in paragraph rewrite: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.builder()
                            .error("INTERNAL_SERVER_ERROR")
                            .message("An unexpected error occurred during paragraph rewrite")
                            .service("Paragraph Rewrite")
                            .timestamp(System.currentTimeMillis())
                            .build());
        }
    }
    
    /**
     * Rewrite story into paragraphs (GET endpoint for testing)
     * GET /api/paragraph-rewrite/rewrite?transcript=...&language=...
     */
    @GetMapping("/rewrite")
    public ResponseEntity<?> rewriteStoryIntoParagraphsGet(
            @RequestParam String transcript,
            @RequestParam String language) {
        
        log.info("Paragraph rewrite GET request - Language: {}, Transcript length: {}", 
                language, transcript.length());
        
        try {
            ParagraphRewriteRequest request = ParagraphRewriteRequest.builder()
                    .transcript(transcript)
                    .language(language)
                    .build();
            
            ParagraphRewriteResponse response = aiService.rewriteStoryIntoParagraphs(
                    request.getTranscript(), 
                    request.getLanguage()
            );
            
            log.info("Paragraph rewrite successful - Language: {}, Contents: {}", 
                    response.getLanguage(), response.getContents().size());
            
            return ResponseEntity.ok(response);
            
        } catch (AIServiceException e) {
            log.error("AI Service error in paragraph rewrite: {} - Service: {}, Error Code: {}", 
                    e.getMessage(), e.getServiceName(), e.getErrorCode());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error(e.getErrorCode())
                            .message(e.getMessage())
                            .service(e.getServiceName())
                            .timestamp(System.currentTimeMillis())
                            .build());
        } catch (Exception e) {
            log.error("Unexpected error in paragraph rewrite: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.builder()
                            .error("INTERNAL_SERVER_ERROR")
                            .message("An unexpected error occurred during paragraph rewrite")
                            .service("Paragraph Rewrite")
                            .timestamp(System.currentTimeMillis())
                            .build());
        }
    }
} 