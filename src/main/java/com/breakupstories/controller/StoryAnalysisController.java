package com.breakupstories.controller;

import com.breakupstories.dto.ErrorResponse;
import com.breakupstories.dto.StoryAnalysisRequest;
import com.breakupstories.dto.StoryAnalysisResponse;
import com.breakupstories.exception.AIServiceException;
import com.breakupstories.service.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for story analysis functionality
 */
@RestController
@RequestMapping("/api/story-analysis")
@RequiredArgsConstructor
@Slf4j
public class StoryAnalysisController {
    
    private final AIService aiService;
    
    /**
     * Analyze story for emotions, tags, locations, themes, and cultural elements
     * POST /api/story-analysis/analyze
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeStory(@RequestBody StoryAnalysisRequest request) {
        log.info("Story analysis request - Language: {}, Story length: {}", 
                request.getLanguage(), request.getStory().length());
        
        try {
            StoryAnalysisResponse response = aiService.analyzeStory(
                    request.getStory(), 
                    request.getLanguage()
            );
            
            if (response.getSuccess()) {
                log.info("Story analysis successful - Story type: {}, Emotions: {}, Tags: {}", 
                        response.getAnalysis().getStory_type(),
                        response.getAnalysis().getEmotions_with_scores().size(),
                        response.getAnalysis().getTags().size());
                
                return ResponseEntity.ok(response);
            } else {
                log.error("Story analysis failed - Error: {}", response.getError());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (AIServiceException e) {
            log.error("AI Service error in story analysis: {} - Service: {}, Error Code: {}", 
                    e.getMessage(), e.getServiceName(), e.getErrorCode());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error(e.getErrorCode())
                            .message(e.getMessage())
                            .service(e.getServiceName())
                            .timestamp(System.currentTimeMillis())
                            .build());
        } catch (Exception e) {
            log.error("Unexpected error in story analysis: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.builder()
                            .error("INTERNAL_SERVER_ERROR")
                            .message("An unexpected error occurred during story analysis")
                            .service("Story Analysis")
                            .timestamp(System.currentTimeMillis())
                            .build());
        }
    }
    
    /**
     * Analyze story (GET endpoint for testing)
     * GET /api/story-analysis/analyze?story=...&language=...
     */
    @GetMapping("/analyze")
    public ResponseEntity<?> analyzeStoryGet(
            @RequestParam String story,
            @RequestParam String language) {
        
        log.info("Story analysis GET request - Language: {}, Story length: {}", 
                language, story.length());
        
        try {
            StoryAnalysisRequest request = StoryAnalysisRequest.builder()
                    .story(story)
                    .language(language)
                    .build();
            
            StoryAnalysisResponse response = aiService.analyzeStory(
                    request.getStory(), 
                    request.getLanguage()
            );
            
            if (response.getSuccess()) {
                log.info("Story analysis successful - Story type: {}, Emotions: {}, Tags: {}", 
                        response.getAnalysis().getStory_type(),
                        response.getAnalysis().getEmotions_with_scores().size(),
                        response.getAnalysis().getTags().size());
                
                return ResponseEntity.ok(response);
            } else {
                log.error("Story analysis failed - Error: {}", response.getError());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (AIServiceException e) {
            log.error("AI Service error in story analysis: {} - Service: {}, Error Code: {}", 
                    e.getMessage(), e.getServiceName(), e.getErrorCode());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error(e.getErrorCode())
                            .message(e.getMessage())
                            .service(e.getServiceName())
                            .timestamp(System.currentTimeMillis())
                            .build());
        } catch (Exception e) {
            log.error("Unexpected error in story analysis: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.builder()
                            .error("INTERNAL_SERVER_ERROR")
                            .message("An unexpected error occurred during story analysis")
                            .service("Story Analysis")
                            .timestamp(System.currentTimeMillis())
                            .build());
        }
    }
} 