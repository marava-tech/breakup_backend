package com.breakupstories.service;

import com.breakupstories.dto.ParagraphRewriteResponse;
import com.breakupstories.dto.ParagraphContent;
import com.breakupstories.dto.StoryAnalysisResponse;
import com.breakupstories.dto.StoryAnalysis;
import com.breakupstories.dto.AbuseDetectionResponse;
import com.breakupstories.dto.TranscriptionResponse;
import com.breakupstories.dto.ConsolingMessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Mock AI Service implementation for all AI functionalities
 * Provides fallback mock implementations when real AI services are unavailable
 */
@Service
@Slf4j
public class MockAIService implements AIService {

    // List of negative/hateful words for basic filtering
    private static final List<String> NEGATIVE_WORDS = Arrays.asList(
        "hate", "stupid", "idiot", "dumb", "ugly", "fat", "kill", "die", "terrible", "awful",
        "horrible", "disgusting", "worthless", "useless", "pathetic", "loser", "failure",
        "crap", "shit", "fuck", "bitch", "asshole", "bastard", "damn", "hell"
    );


    @Override
    public List<String> generateAnimatedImages(String detailedStory) {
        log.info("Mock AI Service: Generating animated images for story - Story length: {}", detailedStory.length());
        
        // Return mock image URLs
        List<String> mockImages = Arrays.asList(
            "https://example.com/mock-image-1.jpg",
            "https://example.com/mock-image-2.jpg",
            "https://example.com/mock-image-3.jpg"
        );
        
        log.info("Mock image generation completed - Generated {} images", mockImages.size());
        return mockImages;
    }



    @Override
    public String rewriteStory(String transcript, String language) {
        log.info("Mock AI Service: Rewriting story - Language: {}, Transcript length: {}", language, transcript.length());
        
        // Create a mock rewritten story based on the transcript
        String mockRewrittenStory = "This is a mock rewritten story based on the original transcript. " +
                "The story has been enhanced with emotional depth and improved narrative flow. " +
                "It captures the essence of the original while making it more engaging and relatable. " +
                "The rewritten version maintains the core message while adding literary elements that " +
                "make it suitable for a breakup stories platform.";
        
        log.info("Mock story rewrite completed for language: {}", language);
        return mockRewrittenStory;
    }
    
    @Override
    public ParagraphRewriteResponse rewriteStoryIntoParagraphs(String transcript, String language) {
        log.info("Mock AI Service: Rewriting story into paragraphs - Language: {}, Transcript length: {}", language, transcript.length());
        
        // Create mock paragraph content
        List<ParagraphContent> mockContents = new ArrayList<>();
        
        ParagraphContent content1 = ParagraphContent.builder()
                .type("TEXT")
                .data("Mock rewritten paragraph 1 based on: " + transcript.substring(0, Math.min(50, transcript.length())) + "...")
                .orderIndex(0)
                .build();
        
        ParagraphContent content2 = ParagraphContent.builder()
                .type("TEXT")
                .data("Mock rewritten paragraph 2 with improved content and flow.")
                .orderIndex(1)
                .build();
        
        ParagraphContent content3 = ParagraphContent.builder()
                .type("TEXT")
                .data("Mock rewritten paragraph 3 with enhanced emotional depth.")
                .orderIndex(2)
                .build();
        
        mockContents.add(content1);
        mockContents.add(content2);
        mockContents.add(content3);
        
        ParagraphRewriteResponse mockResponse = ParagraphRewriteResponse.builder()
                .contents(mockContents)
                .language(language)
                .build();
        
        log.info("Mock paragraph rewrite completed for language: {}", language);
        return mockResponse;
    }
    
    @Override
    public StoryAnalysisResponse analyzeStory(String story, String language) {
        log.info("Mock AI Service: Analyzing story - Language: {}, Story length: {}", language, story.length());
        
        // Create mock analysis data
        Map<String, Double> mockEmotions = Map.of(
            "love", 0.6,
            "sadness", 0.3,
            "anger", 0.2,
            "joy", 0.4,
            "fear", 0.1,
            "surprise", 0.2,
            "disgust", 0.1,
            "trust", 0.5
        );
        
        List<String> mockTags = Arrays.asList("love", "story", "emotion", "relationship", "breakup");
        List<String> mockLocations = List.of("Unknown Location");
        List<String> mockNames = new ArrayList<>();
        List<String> mockThemes = Arrays.asList("love", "emotion", "relationship");
        List<String> mockCulturalElements = List.of("General cultural elements");
        
        // Create mock story analysis
        StoryAnalysis mockAnalysis = StoryAnalysis.builder()
                .story_type("personal")
                .emotions_with_scores(mockEmotions)
                .tags(mockTags)
                .locations(mockLocations)
                .names(mockNames)
                .themes(mockThemes)
                .cultural_elements(mockCulturalElements)
                .is_valid_story(true)
                .build();
        
        StoryAnalysisResponse mockResponse = StoryAnalysisResponse.builder()
                .success(true)
                .analysis(mockAnalysis)
                .error(null)
                .build();
        
        log.info("Mock story analysis completed for language: {}", language);
        return mockResponse;
    }
    
    @Override
    public AbuseDetectionResponse detectAbuse(String comment, String language) {
        log.info("Mock AI Service: Detecting abuse in comment - Language: {}, Comment length: {}", language, comment.length());
        
        // Simple mock logic to detect abusive content
        String lowerCaseComment = comment.toLowerCase();
        boolean isAbusive = false;
        String category = "none";
        String explanation = "Mock analysis: Comment appears to be non-abusive.";
        
        // Check for abusive words
        List<String> abusiveWords = Arrays.asList("hate", "stupid", "idiot", "dumb", "ugly", "kill", "die", "terrible");
        for (String word : abusiveWords) {
            if (lowerCaseComment.contains(word)) {
                isAbusive = true;
                category = "hate_speech";
                explanation = "Mock analysis: Comment contains potentially abusive language.";
                break;
            }
        }
        
        // Mock confidence based on content length
        double confidence = Math.min(0.9, 0.5 + (comment.length() * 0.01));
        
        AbuseDetectionResponse mockResponse = AbuseDetectionResponse.builder()
                .success(true)
                .is_abusive(isAbusive)
                .confidence(confidence)
                .category(category)
                .explanation(explanation)
                .error(null)
                .build();
        
        log.info("Mock abuse detection completed - Is Abusive: {}, Confidence: {}", isAbusive, confidence);
        return mockResponse;
    }
    

    
    @Override
    public TranscriptionResponse transcribeAudio(String audioUrl, String language) {
        log.info("Mock AI Service: Transcribing audio - URL: {}, Language: {}", audioUrl, language);
        
        // Create a mock transcription based on the audio URL
        String mockTranscript = "This is a mock transcription of the audio file. " +
                "In a real implementation, this would be the actual transcribed text " +
                "from the audio file using AI transcription services. " +
                "The transcription would be in the language: " + language + ". " +
                "This is a sample transcript for testing purposes.";
        
        TranscriptionResponse mockResponse = TranscriptionResponse.builder()
                .transcript(mockTranscript)
                .language(language)
                .confidence(0.85)
                .build();
        
        log.info("Mock transcription completed for language: {} with confidence: {}", language, mockResponse.getConfidence());
        return mockResponse;
    }
    
    @Override
    public ConsolingMessageResponse generateConsolingMessage(String story, String language, String gender, Integer age, String consoleBy) {
        log.info("Mock AI Service: Generating consoling message - Language: {}, Gender: {}, Age: {}, ConsoleBy: {}", 
                language, gender, age, consoleBy);
        
        // Create a mock consoling message based on the parameters
        String mockConsolingMessage = "This is a mock consoling message generated for your story. " +
                "I understand that you're going through a difficult time, and I want you to know that " +
                "you're not alone. Many people experience similar situations, and it's completely normal " +
                "to feel the way you do right now. " +
                "Remember that healing takes time, and it's okay to take things one day at a time. " +
                "Focus on taking care of yourself and doing things that make you happy. " +
                "You are stronger than you think, and you will get through this. " +
                "If you need someone to talk to, don't hesitate to reach out to friends, family, or professionals. " +
                "You deserve love and happiness, and better days are ahead.";
        
        ConsolingMessageResponse mockResponse = ConsolingMessageResponse.builder()
                .success(true)
                .consolingMessage(mockConsolingMessage)
                .language(language)
                .consoleBy(consoleBy)
                .error(null)
                .build();
        
        log.info("Mock consoling message generation completed for language: {} with consoleBy: {}", 
                language, consoleBy);
        return mockResponse;
    }
}
