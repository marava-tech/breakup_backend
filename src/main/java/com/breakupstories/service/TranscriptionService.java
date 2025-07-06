package com.breakupstories.service;

import com.breakupstories.dto.StoryRewriteRequest;
import com.breakupstories.dto.StoryRewriteResponse;
import com.breakupstories.dto.TranscriptionRequest;
import com.breakupstories.dto.TranscriptionResponse;
import com.breakupstories.dto.ParagraphRewriteRequest;
import com.breakupstories.dto.ParagraphRewriteResponse;
import com.breakupstories.dto.StoryAnalysisRequest;
import com.breakupstories.dto.StoryAnalysisResponse;
import com.breakupstories.dto.AbuseDetectionRequest;
import com.breakupstories.dto.AbuseDetectionResponse;
import com.breakupstories.dto.LocationInfoRequest;
import com.breakupstories.dto.LocationInfoResponse;
import com.breakupstories.exception.AIServiceException;
import com.breakupstories.model.Story;
import com.breakupstories.model.User;
import com.breakupstories.repository.StoryRepository;
import com.breakupstories.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

/**
 * Service for handling transcription requests
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TranscriptionService {
    
    private final RestTemplate restTemplate;
    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    
    @Value("${ai.service.base-url:http://localhost:8000}")
    private String aiServiceBaseUrl;
    
    /**
     * Transcribe audio from URL
     * @param audioUrl The audio URL to transcribe
     * @param language The language code (e.g., "te", "en", "hi")
     * @return TranscriptionResponse with transcript, language, and confidence
     */
    public TranscriptionResponse transcribeAudioFromUrl(String audioUrl, String language) {
        log.info("Transcribing audio from URL: {} with language: {}", audioUrl, language);
        
        try {
            // Create request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            // Create form data
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("audio_url", audioUrl);
            body.add("language", language);
            
            // Create HTTP entity
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            // Make request to AI service
            String transcriptionUrl = aiServiceBaseUrl + "/transcription/transcribe-url";
            ResponseEntity<TranscriptionResponse> response = restTemplate.exchange(
                    transcriptionUrl,
                    HttpMethod.POST,
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
                throw new RuntimeException("Transcription failed with status: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error transcribing audio: {}", e.getMessage(), e);
            throw new AIServiceException("Transcription", "TRANSCRIPTION_ERROR", 
                "Failed to transcribe audio: " + e.getMessage(), e);
        }
    }
    
    /**
     * Transcribe audio for a story
     * @param storyId The story ID
     * @return TranscriptionResponse
     */
    public TranscriptionResponse transcribeStoryAudio(String storyId) {
        log.info("Transcribing audio for story: {}", storyId);
        
        // Get story
        Optional<Story> storyOpt = storyRepository.findById(storyId);
        if (storyOpt.isEmpty()) {
            throw new AIServiceException("Transcription", "STORY_NOT_FOUND", 
                "Story not found: " + storyId);
        }
        
        Story story = storyOpt.get();
        String audioUrl = story.getAudioUrl();
        
        if (audioUrl == null || audioUrl.trim().isEmpty()) {
            throw new AIServiceException("Transcription", "NO_AUDIO_URL", 
                "No audio URL found for story: " + storyId);
        }
        
        // Get user language
        String language = getUserLanguage(story.getUserId());
        
        // Transcribe audio (no automatic saving to collections)
        return transcribeAudioFromUrl(audioUrl, language);
    }
    
    /**
     * Get user language preference
     * @param userId The user ID
     * @return Language code (default: "en")
     */
    private String getUserLanguage(String userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                String userLanguage = user.getPreferredStoryLanguage();
                if (userLanguage != null && !userLanguage.trim().isEmpty()) {
                    return userLanguage.toLowerCase();
                }
            }
        } catch (Exception e) {
            log.warn("Error getting user language for userId: {}, using default", userId, e);
        }
        
        // Default language
        return "en";
    }
    
    /**
     * Rewrite story from transcript using external AI service
     * @param transcript The original transcript
     * @param language The language code
     * @return Rewritten story
     */
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
    
    /**
     * Rewrite story into paragraphs using external AI service
     * @param transcript The original transcript
     * @param language The language code
     * @return Paragraph rewrite response with contents
     */
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
    
    /**
     * Analyze story for emotions, tags, locations, themes, and cultural elements
     * @param story The story text to analyze
     * @param language The language code
     * @return Story analysis response with comprehensive analysis
     */
    public StoryAnalysisResponse analyzeStory(String story, String language) {
        log.info("Analyzing story - Language: {}, Story length: {}", language, story.length());
        
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
    
    /**
     * Detect abusive content in comments
     * @param comment The comment text to analyze
     * @param language The language code
     * @return Abuse detection response with confidence and explanation
     */
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
    
    /**
     * Get location information from coordinates
     * @param latitude The latitude coordinate
     * @param longitude The longitude coordinate
     * @return Location info response with address details
     */
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
                            locationResponse.getDistrict(), 
                            locationResponse.getState(),
                            locationResponse.getPincode());
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
} 