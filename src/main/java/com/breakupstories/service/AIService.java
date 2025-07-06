package com.breakupstories.service;

import com.breakupstories.dto.ParagraphRewriteResponse;
import com.breakupstories.dto.StoryAnalysisResponse;
import com.breakupstories.dto.AbuseDetectionResponse;
import com.breakupstories.dto.LocationInfoResponse;
import com.breakupstories.dto.TranscriptionResponse;
import com.breakupstories.dto.ConsolingMessageResponse;

import java.util.List;

/**
 * Interface for AI service implementations
 * This defines all AI functionalities that need to be implemented
 */
public interface AIService {

    /**
     * Generate animated images for a story
     * @param detailedStory The detailed story text
     * @return List of image URLs
     */
    List<String> generateAnimatedImages(String detailedStory);

    /**
     * Extract location from coordinates
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @return Location string (district, state, pincode)
     */
    String extractLocationFromCoordinates(String latitude, String longitude);

    /**
     * Rewrite story from transcript
     * @param transcript The original transcript
     * @param language The language code
     * @return Rewritten story
     */
    String rewriteStory(String transcript, String language);
    
    /**
     * Rewrite story into paragraphs
     * @param transcript The original transcript
     * @param language The language code
     * @return Paragraph rewrite response with contents
     */
    ParagraphRewriteResponse rewriteStoryIntoParagraphs(String transcript, String language);
    
    /**
     * Analyze story for emotions, tags, locations, themes, and cultural elements
     * @param story The story text to analyze
     * @param language The language code
     * @return Story analysis response with comprehensive analysis
     */
    StoryAnalysisResponse analyzeStory(String story, String language);
    
    /**
     * Detect abusive content in comments
     * @param comment The comment text to analyze
     * @param language The language code
     * @return Abuse detection response with confidence and explanation
     */
    AbuseDetectionResponse detectAbuse(String comment, String language);
    
    /**
     * Get location information from coordinates
     * @param latitude The latitude coordinate
     * @param longitude The longitude coordinate
     * @return Location info response with address details
     */
    LocationInfoResponse getLocationInfo(Double latitude, Double longitude);
    
    /**
     * Transcribe audio from URL
     * @param audioUrl The audio URL to transcribe
     * @param language The language code
     * @return TranscriptionResponse with transcript, language, and confidence
     */
    TranscriptionResponse transcribeAudio(String audioUrl, String language);
    
    /**
     * Generate consoling message for a story
     * @param story The story text
     * @param language The language code
     * @param gender The gender of the person
     * @param age The age of the person
     * @param consoleBy The type of consoler (e.g., "female_friend", "male_friend", etc.)
     * @return ConsolingMessageResponse with consoling message
     */
    ConsolingMessageResponse generateConsolingMessage(String story, String language, String gender, Integer age, String consoleBy);
} 