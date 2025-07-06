package com.breakupstories.controller;

import com.breakupstories.dto.ConsolingMessageRequest;
import com.breakupstories.dto.ConsolingMessageResponse;
import com.breakupstories.service.AIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for AI-related endpoints
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Services", description = "APIs for AI-powered features")
public class AIController {
    
    private final AIService aiService;
    
    /**
     * Generate consoling message for a story
     * @param request The consoling message request
     * @return ConsolingMessageResponse with the generated message
     */
    @PostMapping("/consoling/generate-message")
    @Operation(summary = "Generate consoling message", description = "Generate a consoling message for a story based on user parameters")
    public ResponseEntity<ConsolingMessageResponse> generateConsolingMessage(@RequestBody ConsolingMessageRequest request) {
        log.info("Generating consoling message for story - Language: {}, Gender: {}, Age: {}, ConsoleBy: {}", 
                request.getLanguage(), request.getGender(), request.getAge(), request.getConsoleBy());
        
        try {
            ConsolingMessageResponse response = aiService.generateConsolingMessage(
                    request.getStory(),
                    request.getLanguage(),
                    request.getGender(),
                    request.getAge(),
                    request.getConsoleBy()
            );
            
            log.info("Consoling message generated successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error generating consoling message: {}", e.getMessage(), e);
            
            ConsolingMessageResponse errorResponse = ConsolingMessageResponse.builder()
                    .success(false)
                    .consolingMessage(null)
                    .language(request.getLanguage())
                    .consoleBy(request.getConsoleBy())
                    .error("Failed to generate consoling message: " + e.getMessage())
                    .build();
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
} 