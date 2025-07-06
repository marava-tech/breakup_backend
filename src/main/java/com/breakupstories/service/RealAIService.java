package com.breakupstories.service;

import com.breakupstories.dto.AbuseDetectionRequest;
import com.breakupstories.dto.AbuseDetectionResponse;
import com.breakupstories.dto.LocationInfoRequest;
import com.breakupstories.dto.LocationInfoResponse;
import com.breakupstories.dto.ParagraphRewriteRequest;
import com.breakupstories.dto.ParagraphRewriteResponse;
import com.breakupstories.dto.StoryAnalysisRequest;
import com.breakupstories.dto.StoryAnalysisResponse;
import com.breakupstories.dto.StoryRewriteRequest;
import com.breakupstories.dto.StoryRewriteResponse;
import com.breakupstories.dto.TranscriptionResponse;
import com.breakupstories.dto.ConsolingMessageRequest;
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
    public String extractLocationFromCoordinates(String latitude, String longitude) {
        log.info("Extracting location from coordinates - Latitude: {}, Longitude: {}", latitude, longitude);
        
        try {
            LocationInfoResponse response = getLocationInfo(Double.parseDouble(latitude), Double.parseDouble(longitude));
            if (response.getSuccess()) {
                return String.format("%s, %s, %s", 
                    response.getDistrict(), response.getState(), response.getPincode());
            } else {
                return "Unknown Location";
            }
        } catch (Exception e) {
            log.error("Error extracting location: {}", e.getMessage(), e);
            return "Unknown Location";
        }
    }

    @Override
    public String rewriteStory(String transcript, String language) {
        log.info("Rewriting story - Language: {}, Transcript length: {}", language, transcript.length());
        
        try {
            // Create request body
            StoryRewriteRequest request = StoryRewriteRequest.builder()
                    .transcript(transcript)
                    .language(language)
                    .build();
            
            // Create request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create HTTP entity
            HttpEntity<StoryRewriteRequest> requestEntity = new HttpEntity<>(request, headers);
            
            // Make request to AI service
            String rewriteUrl = aiServiceBaseUrl + "/story-rewrite/rewrite";
            ResponseEntity<StoryRewriteResponse> response = restTemplate.exchange(
                    rewriteUrl,
                    HttpMethod.POST,
                    requestEntity,
                    StoryRewriteResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                StoryRewriteResponse rewriteResponse = response.getBody();
                log.info("Story rewrite successful - Language: {}", rewriteResponse.getLanguage());
                return rewriteResponse.getRewrittenStory();
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
            // Create request body
            ParagraphRewriteRequest request = ParagraphRewriteRequest.builder()
                    .transcript(transcript)
                    .language(language)
                    .build();
            
            // Create request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create HTTP entity
            HttpEntity<ParagraphRewriteRequest> requestEntity = new HttpEntity<>(request, headers);
            
            // Make request to AI service
            String rewriteUrl = aiServiceBaseUrl + "/story-rewrite/paragraphs";
            ResponseEntity<ParagraphRewriteResponse> response = restTemplate.exchange(
                    rewriteUrl,
                    HttpMethod.POST,
                    requestEntity,
                    ParagraphRewriteResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                ParagraphRewriteResponse rewriteResponse = response.getBody();
                log.info("Story paragraph rewrite successful - Language: {}, Contents: {}", 
                        rewriteResponse.getLanguage(), rewriteResponse.getContents().size());
                return rewriteResponse;
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
            // Create request body
            StoryAnalysisRequest request = StoryAnalysisRequest.builder()
                    .story(story)
                    .language(language)
                    .build();
            
            // Create request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create HTTP entity
            HttpEntity<StoryAnalysisRequest> requestEntity = new HttpEntity<>(request, headers);
            
            // Make request to AI service
            String analysisUrl = aiServiceBaseUrl + "/story/analyze";
            ResponseEntity<StoryAnalysisResponse> response = restTemplate.exchange(
                    analysisUrl,
                    HttpMethod.POST,
                    requestEntity,
                    StoryAnalysisResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                StoryAnalysisResponse analysisResponse = response.getBody();
                if (analysisResponse.getSuccess()) {
                    log.info("Story analysis successful - Language: {}, Emotions: {}, Tags: {}", 
                            analysisResponse.getAnalysis().getStory_type(), 
                            analysisResponse.getAnalysis().getEmotions_with_scores().size(),
                            analysisResponse.getAnalysis().getTags().size());
                    return analysisResponse;
                } else {
                    log.error("Story analysis failed - Error: {}", analysisResponse.getError());
                    throw new AIServiceException("Story Analysis", "ANALYSIS_FAILED", 
                        "Story analysis failed: " + analysisResponse.getError());
                }
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
            // Create request body
            AbuseDetectionRequest request = AbuseDetectionRequest.builder()
                    .comment(comment)
                    .language(language)
                    .build();
            
            // Create request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create HTTP entity
            HttpEntity<AbuseDetectionRequest> requestEntity = new HttpEntity<>(request, headers);
            
            // Make request to AI service
            String abuseDetectionUrl = aiServiceBaseUrl + "/abuse-detection/detect";
            ResponseEntity<AbuseDetectionResponse> response = restTemplate.exchange(
                    abuseDetectionUrl,
                    HttpMethod.POST,
                    requestEntity,
                    AbuseDetectionResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                AbuseDetectionResponse abuseResponse = response.getBody();
                if (abuseResponse.getSuccess()) {
                    log.info("Abuse detection successful - Is Abusive: {}, Confidence: {}, Category: {}", 
                            abuseResponse.getIs_abusive(), 
                            abuseResponse.getConfidence(),
                            abuseResponse.getCategory());
                    return abuseResponse;
                } else {
                    log.error("Abuse detection failed - Error: {}", abuseResponse.getError());
                    throw new AIServiceException("Abuse Detection", "ABUSE_DETECTION_FAILED", 
                        "Abuse detection failed: " + abuseResponse.getError());
                }
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
    public LocationInfoResponse getLocationInfo(Double latitude, Double longitude) {
        log.info("Getting location info - Latitude: {}, Longitude: {}", latitude, longitude);
        
        try {
            // Create request body
            LocationInfoRequest request = LocationInfoRequest.builder()
                    .latitude(latitude)
                    .longitude(longitude)
                    .build();
            
            // Create request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create HTTP entity
            HttpEntity<LocationInfoRequest> requestEntity = new HttpEntity<>(request, headers);
            
            // Make request to AI service
            String locationInfoUrl = aiServiceBaseUrl + "/location/get-location-info";
            ResponseEntity<LocationInfoResponse> response = restTemplate.exchange(
                    locationInfoUrl,
                    HttpMethod.POST,
                    requestEntity,
                    LocationInfoResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                LocationInfoResponse locationResponse = response.getBody();
                if (locationResponse.getSuccess()) {
                    log.info("Location info successful - District: {}, State: {}, Pincode: {}", 
                            locationResponse.getDistrict(), locationResponse.getState(), locationResponse.getPincode());
                    return locationResponse;
                } else {
                    log.error("Location info failed - Error: {}", locationResponse.getError());
                    throw new AIServiceException("Location Info", "LOCATION_INFO_FAILED", 
                        "Location info failed: " + locationResponse.getError());
                }
            } else {
                log.error("Location info failed - Status: {}", response.getStatusCode());
                throw new AIServiceException("Location Info", "LOCATION_INFO_HTTP_ERROR", 
                    "Location info failed with status: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error getting location info: {}", e.getMessage(), e);
            throw new AIServiceException("Location Info", "LOCATION_INFO_ERROR", 
                "Failed to get location info: " + e.getMessage(), e);
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
            String transcriptionUrl = String.format("%s/transcribe?audio_url=%s&language=%s", 
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
            // Create request body
            ConsolingMessageRequest request = ConsolingMessageRequest.builder()
                    .story(story)
                    .language(language)
                    .gender(gender)
                    .age(age)
                    .consoleBy(consoleBy)
                    .build();
            
            // Create request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create HTTP entity
            HttpEntity<ConsolingMessageRequest> requestEntity = new HttpEntity<>(request, headers);
            
            // Make request to AI service
            String consolingUrl = aiServiceBaseUrl + "/consoling/generate-message";
            ResponseEntity<ConsolingMessageResponse> response = restTemplate.exchange(
                    consolingUrl,
                    HttpMethod.POST,
                    requestEntity,
                    ConsolingMessageResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                ConsolingMessageResponse consolingResponse = response.getBody();
                log.info("Consoling message generation successful - Language: {}, ConsoleBy: {}", 
                        consolingResponse.getLanguage(), consolingResponse.getConsoleBy());
                return consolingResponse;
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