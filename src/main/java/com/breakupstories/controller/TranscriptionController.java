package com.breakupstories.controller;

import com.breakupstories.dto.ErrorResponse;
import com.breakupstories.dto.TranscriptionRequest;
import com.breakupstories.dto.TranscriptionResponse;
import com.breakupstories.exception.AIServiceException;
import com.breakupstories.service.TranscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for transcription operations
 */
@RestController
@RequestMapping("/api/v1/transcription")
@RequiredArgsConstructor
@Slf4j
public class TranscriptionController {
    
    private final TranscriptionService transcriptionService;
    
    /**
     * Transcribe audio from URL
     */
    @PostMapping("/transcribe-url")
    public ResponseEntity<?> transcribeAudioFromUrl(@RequestBody TranscriptionRequest request) {
        log.info("Transcription request - Audio URL: {}, Language: {}", request.getAudioUrl(), request.getLanguage());
        
        try {
            TranscriptionResponse response = transcriptionService.transcribeAudioFromUrl(
                    request.getAudioUrl(), request.getLanguage());
            return ResponseEntity.ok(response);
        } catch (AIServiceException e) {
            log.error("AI Service error in transcription: {} - Service: {}, Error Code: {}", 
                    e.getMessage(), e.getServiceName(), e.getErrorCode());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error(e.getErrorCode())
                            .message(e.getMessage())
                            .service(e.getServiceName())
                            .timestamp(System.currentTimeMillis())
                            .build());
        } catch (Exception e) {
            log.error("Unexpected error in transcription: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.builder()
                            .error("INTERNAL_SERVER_ERROR")
                            .message("An unexpected error occurred during transcription")
                            .service("Transcription")
                            .build());
        }
    }
    
    /**
     * Transcribe audio for a story
     */
    @PostMapping("/transcribe-story/{storyId}")
    public ResponseEntity<?> transcribeStoryAudio(@PathVariable String storyId) {
        log.info("Transcription request for story: {}", storyId);
        
        try {
            TranscriptionResponse response = transcriptionService.transcribeStoryAudio(storyId);
            return ResponseEntity.ok(response);
        } catch (AIServiceException e) {
            log.error("AI Service error in story transcription: {} - Service: {}, Error Code: {}", 
                    e.getMessage(), e.getServiceName(), e.getErrorCode());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error(e.getErrorCode())
                            .message(e.getMessage())
                            .service(e.getServiceName())
                            .timestamp(System.currentTimeMillis())
                            .build());
        } catch (Exception e) {
            log.error("Unexpected error in story transcription: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.builder()
                            .error("INTERNAL_SERVER_ERROR")
                            .message("An unexpected error occurred during story transcription")
                            .service("Transcription")
                            .timestamp(System.currentTimeMillis())
                            .build());
        }
    }
} 