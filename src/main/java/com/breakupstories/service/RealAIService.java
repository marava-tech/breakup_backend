package com.breakupstories.service;


import com.breakupstories.dto.AbuseDetectionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.breakupstories.dto.ParagraphRewriteResponse;
import com.breakupstories.dto.StoryAnalysisResponse;
import com.breakupstories.dto.TranscriptionResponse;
import com.breakupstories.dto.ConsolingMessageResponse;
import com.breakupstories.exception.AIServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Real AI Service implementation with actual HTTP calls to external AI services
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Primary
public class RealAIService implements AIService {
    
    private final RestTemplate restTemplate;
    
    @Value("${ai.service.base-url:http://localhost:8000}")
    private String aiServiceBaseUrl;


    @Override
    public List<String> generateAnimatedImages(String detailedStory) {
        log.info("Generating animated images for story - Story length: {}", detailedStory.length());
        
        try {
            // TODO: Implement actual image generation API call
            // This would call an external AI service for image generation
            log.info("Image generation not implemented yet, returning mock URLs");
            return List.of("generated-image-url-1", "generated-image-url-2");
            
        } catch (Exception e) {
            log.error("Error generating images: {}", e.getMessage(), e);
            return List.of("fallback-image-url");
        }
    }



    @Override
    public String rewriteStory(String transcript, String language) {
        log.info("Rewriting story - Language: {}, Transcript length: {}", language, transcript != null ? transcript.length() : 0);
        
        // Validate input parameters
        if (transcript == null || transcript.trim().isEmpty()) {
            log.error("Transcript is null or empty for story rewrite");
            throw new AIServiceException("Story Rewrite", "INVALID_INPUT", 
                "Transcript cannot be null or empty for story rewrite");
        }
        
        if (language == null || language.trim().isEmpty()) {
            log.error("Language is null or empty for story rewrite");
            throw new AIServiceException("Story Rewrite", "INVALID_INPUT", 
                "Language cannot be null or empty for story rewrite");
        }
        
        try {
            // URL encode the transcript for query parameter
            String encodedTranscript = java.net.URLEncoder.encode(transcript, "UTF-8");
            
            // Build URL with query parameters
            String url = String.format("%s/story-rewrite/rewrite?transcript=%s&language=%s", 
                    aiServiceBaseUrl, encodedTranscript, language);
            
            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create HTTP entity with empty body
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            log.info("Making GET request to: {} with transcript length: {}", url, transcript.length());
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Story rewrite successful");
                // Parse the JSON response to extract rewritten_story
                ObjectMapper objectMapper = new ObjectMapper();
                var jsonNode = objectMapper.readTree(response.getBody());
                String rewrittenStory = jsonNode.get("rewritten_story").asText();
                log.info("Extracted rewritten story with length: {}", rewrittenStory.length());
                return rewrittenStory;
            } else {
                log.error("Story rewrite failed - Status: {}", response.getStatusCode());
                throw new AIServiceException("Story Rewrite", "REWRITE_HTTP_ERROR", 
                    "Story rewrite failed with status: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error rewriting story: {}", e.getMessage(), e);
            throw new AIServiceException("Story Rewrite", "REWRITE_ERROR", 
                "Failed to rewrite story: " + e.getMessage(), e);
        }
    }
    
    @Override
    public ParagraphRewriteResponse rewriteStoryIntoParagraphs(String transcript, String language) {
        log.info("Rewriting story into paragraphs - Language: {}, Transcript length: {}", language, transcript.length());
        
        try {
            // URL encode the transcript for query parameter
            String encodedTranscript = java.net.URLEncoder.encode(transcript, "UTF-8");
            
            // Build URL with query parameters
            String url = String.format("%s/story-rewrite/paragraphs?transcript=%s&language=%s", 
                    aiServiceBaseUrl, encodedTranscript, language);
            
            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create HTTP entity with empty body
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            log.info("Making GET request to: {} with transcript length: {}", url, transcript.length());
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Story paragraph rewrite successful");
                // Parse the JSON response into ParagraphRewriteResponse
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(response.getBody(), ParagraphRewriteResponse.class);
            } else {
                log.error("Story paragraph rewrite failed - Status: {}", response.getStatusCode());
                throw new AIServiceException("Paragraph Rewrite", "PARAGRAPH_REWRITE_HTTP_ERROR", 
                    "Story paragraph rewrite failed with status: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error rewriting story into paragraphs: {}", e.getMessage(), e);
            throw new AIServiceException("Paragraph Rewrite", "PARAGRAPH_REWRITE_ERROR", 
                "Failed to rewrite story into paragraphs: " + e.getMessage(), e);
        }
    }
    
    @Override
    public StoryAnalysisResponse analyzeStory(String story, String language) {
        log.info("Analyzing story - Language: {}, Story length: {}", language, story != null ? story.length() : 0);
        
        // Validate input parameters
        if (story == null || story.trim().isEmpty()) {
            log.error("Story is null or empty for analysis");
            throw new AIServiceException("Story Analysis", "INVALID_INPUT", 
                "Story cannot be null or empty for analysis");
        }
        
        try {
            // URL encode the story for query parameter
            String encodedStory = java.net.URLEncoder.encode(story, "UTF-8");
            
            // Build URL with query parameters
            String url = String.format("%s/story/analyze?story=%s&language=%s", 
                    aiServiceBaseUrl, encodedStory, language);
            
            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create HTTP entity with empty body
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            log.info("Making GET request to: {} with story length: {}", url, story.length());
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Story analysis successful");
                // Parse the JSON response into StoryAnalysisResponse
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(response.getBody(), StoryAnalysisResponse.class);
            } else {
                log.error("Story analysis failed - Status: {}", response.getStatusCode());
                throw new AIServiceException("Story Analysis", "ANALYSIS_HTTP_ERROR", 
                    "Story analysis failed with status: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error analyzing story: {}", e.getMessage(), e);
            throw new AIServiceException("Story Analysis", "ANALYSIS_ERROR", 
                "Failed to analyze story: " + e.getMessage(), e);
        }
    }
    
    @Override
    public AbuseDetectionResponse detectAbuse(String comment, String language) {
        log.info("Detecting abuse in comment - Language: {}, Comment length: {}", language, comment.length());
        
        try {
            // URL encode the comment for query parameter
            String encodedComment = java.net.URLEncoder.encode(comment, "UTF-8");
            
            // Build URL with query parameters
            String url = String.format("%s/abuse-detection/detect?comment=%s&language=%s", 
                    aiServiceBaseUrl, encodedComment, language);
            
            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create HTTP entity with empty body
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            log.info("Making GET request to: {} with comment length: {}", url, comment.length());
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Abuse detection successful");
                // Parse the JSON response into AbuseDetectionResponse
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(response.getBody(), AbuseDetectionResponse.class);
            } else {
                log.error("Abuse detection failed - Status: {}", response.getStatusCode());
                throw new AIServiceException("Abuse Detection", "ABUSE_DETECTION_HTTP_ERROR", 
                    "Abuse detection failed with status: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error detecting abuse: {}", e.getMessage(), e);
            throw new AIServiceException("Abuse Detection", "ABUSE_DETECTION_ERROR", 
                "Failed to detect abuse: " + e.getMessage(), e);
        }
    }
    

    
    @Override
    public TranscriptionResponse transcribeAudio(String audioUrl, String language) {
        log.info("Transcribing audio - Language: {}, Audio URL: {}", language, audioUrl);
        
        try {
            // Create request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create HTTP entity with query parameters
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            
            // Make request to AI service with query parameters
            String transcriptionUrl = String.format("%s/transcription/transcribe-url?audio_url=%s&language=%s", 
                    aiServiceBaseUrl, audioUrl, language);
            ResponseEntity<TranscriptionResponse> response = restTemplate.exchange(
                    transcriptionUrl,
                    HttpMethod.GET,
                    requestEntity,
                    TranscriptionResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                TranscriptionResponse transcriptionResponse = response.getBody();
                log.info("Transcription successful - Language: {}, Confidence: {}", 
                        transcriptionResponse.getLanguage(), transcriptionResponse.getConfidence());
                return transcriptionResponse;
            } else {
                log.error("Transcription failed - Status: {}", response.getStatusCode());
                throw new AIServiceException("Transcription", "TRANSCRIPTION_HTTP_ERROR", 
                    "Transcription failed with status: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error transcribing audio: {}", e.getMessage(), e);
            throw new AIServiceException("Transcription", "TRANSCRIPTION_ERROR", 
                "Failed to transcribe audio: " + e.getMessage(), e);
        }
    }
    
    @Override
    public ConsolingMessageResponse generateConsolingMessage(String story, String language, String gender, Integer age, String consoleBy) {
        log.info("Generating consoling message - Language: {}, Gender: {}, Age: {}, ConsoleBy: {}", 
                language, gender, age, consoleBy);
        
        try {
            // URL encode the story for query parameter
            String encodedStory = java.net.URLEncoder.encode(story, "UTF-8");
            
            // Build URL with query parameters
            String url = String.format("%s/consoling/generate-message?story=%s&language=%s&gender=%s&age=%d&consoleBy=%s", 
                    aiServiceBaseUrl, encodedStory, language, gender, age, consoleBy);
            
            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create HTTP entity with empty body
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            log.info("Making GET request to: {} with story length: {}", url, story.length());
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Consoling message generation successful");
                // Parse the JSON response into ConsolingMessageResponse
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(response.getBody(), ConsolingMessageResponse.class);
            } else {
                log.error("Consoling message generation failed - Status: {}", response.getStatusCode());
                throw new AIServiceException("Consoling Message", "CONSOLING_HTTP_ERROR", 
                    "Consoling message generation failed with status: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error generating consoling message: {}", e.getMessage(), e);
            throw new AIServiceException("Consoling Message", "CONSOLING_ERROR", 
                "Failed to generate consoling message: " + e.getMessage(), e);
        }
    }
} 