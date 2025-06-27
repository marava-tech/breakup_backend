package com.breakupstories.service;

import com.breakupstories.dto.StoryResponse;
import com.breakupstories.model.Content;
import com.breakupstories.model.Emotion;
import com.breakupstories.model.Story;
import com.breakupstories.model.StoryMetadata;
import com.breakupstories.repository.StoryRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@AllArgsConstructor
public class MockAIService {

    private final StoryRepository storyRepository;


    @Async
    public void  processStoryWithAIAsync(String storyId) {
        log.info("Starting async AI processing for story: {}", storyId);
        try {
            // Simulate processing time (10 seconds)
            Thread.sleep(10000);

            // Step 1: Mock Transcription
            String transcription = mockTranscription();
            log.info("Transcription completed for story: {}", storyId);

            // Step 1.5: Mock Audio Language Detection
            String language = mockDetectAudioLanguage(transcription);
            log.info("Audio language detected for story {}: {}", storyId, language);

            // Step 2: Mock Detailed Story Creation
            String detailedStory = mockCreateDetailedStory(transcription);
            log.info("Detailed story created for story: {}", storyId);

            // Step 3: Mock Title Generation
            String title = mockCreateTitle(detailedStory);
            log.info("Title generated for story: {}", storyId);

            // Step 4: Mock Animated Images (skip for now as mentioned in comments)
            List<String> animatedImages = mockGenerateAnimatedImages(detailedStory);
            log.info("Animated images generated for story: {}", storyId);

            // Step 5: Mock Content Creation with Images
            List<Content> contents = mockCreateContents(detailedStory, animatedImages);
            log.info("Contents created for story: {}", storyId);

            // Step 6: Mock Emotions Analysis
            List<Emotion> emotions = mockGetEmotions(detailedStory);
            log.info("Emotions analyzed for story: {}", storyId);

            // Step 7: Mock Tags Generation
            List<String> tags = mockGetTags(detailedStory);
            log.info("Tags generated for story: {}", storyId);

            // Step 8: Get story metadata details by calling AI services
            // 8.1 - get names involved in the story from AI service
            List<String> names = mockExtractNames(detailedStory);
            log.info("Names extracted for story {}: {}", storyId, names);

            // 8.2 - get locations involved in the story (add current location if lat, long present in header)
            List<String> locations = mockExtractLocations(detailedStory);
            log.info("Locations extracted for story {}: {}", storyId, locations);

            // 8.3 - fetch pincodes of the location (send location names)
            List<String> pincodes = mockFetchPincodes(locations);
            log.info("Pincodes fetched for story {}: {}", storyId, pincodes);

            // 8.4 - get language which the transcription is in from AI service / if not found add language from user document
            // Language is already detected in Step 1.5

            // 8.5 - store device info as well from headers
            String deviceInfo = mockGetDeviceInfo();
            log.info("Device info extracted for story {}: {}", storyId, deviceInfo);

            // Create metadata object
            StoryMetadata metadata = StoryMetadata.builder()
                    .names(names)
                    .locations(locations)
                    .pincodes(pincodes)
                    .state(mockGetState(locations))
                    .district(mockGetState(locations))
                    .language(language)
                    .deviceInfo(deviceInfo)
                    .build();

            // Step 9: Mock Shareable Link Generation
            String shareLink = mockCreateShareableLink(storyId);
            log.info("Shareable link created for story: {}", storyId);

            // Update the story with all processed data including metadata
            updateStoryWithAIResults(storyId, title, contents, tags, emotions, shareLink, metadata);

            log.info("AI processing completed successfully for story: {}", storyId);

        } catch (Exception e) {
            log.error("Error in AI processing for story {}: {}", storyId, e.getMessage(), e);
            // Update story status to REJECTED (failed processing)
            updateStoryStatusWithRejection(storyId, Story.StoryStatus.REJECTED, Arrays.asList("AI processing failed: " + e.getMessage()));
        }

    }

    /**
     * Async AI processing workflow
     * This simulates the AI service calls that will be integrated later
     */

    /**
     * Mock AI processing workflow (synchronous version - kept for backward compatibility)
     * This simulates the AI service calls that will be integrated later
     */
    private void processStoryWithAI(String storyId) {
        log.info("Starting AI processing for story: {}", storyId);

        try {
            // Simulate processing time (10 seconds)
            Thread.sleep(10000);

            // Step 1: Mock Transcription
            String transcription = mockTranscription();
            log.info("Transcription completed for story: {}", storyId);

            // Step 1.5: Mock Audio Language Detection
            String language = mockDetectAudioLanguage(transcription);
            log.info("Audio language detected for story {}: {}", storyId, language);

            // Step 2: Mock Detailed Story Creation
            String detailedStory = mockCreateDetailedStory(transcription);
            log.info("Detailed story created for story: {}", storyId);

            // Step 3: Mock Title Generation
            String title = mockCreateTitle(detailedStory);
            log.info("Title generated for story: {}", storyId);

            // Step 4: Mock Animated Images (skip for now as mentioned in comments)
            List<String> animatedImages = mockGenerateAnimatedImages(detailedStory);
            log.info("Animated images generated for story: {}", storyId);

            // Step 5: Mock Content Creation with Images
            List<Content> contents = mockCreateContents(detailedStory, animatedImages);
            log.info("Contents created for story: {}", storyId);

            // Step 6: Mock Emotions Analysis
            List<Emotion> emotions = mockGetEmotions(detailedStory);
            log.info("Emotions analyzed for story: {}", storyId);

            // Step 7: Mock Tags Generation
            List<String> tags = mockGetTags(detailedStory);
            log.info("Tags generated for story: {}", storyId);

            // Step 8: Get story metadata details by calling AI services
            // 8.1 - get names involved in the story from AI service
            List<String> names = mockExtractNames(detailedStory);
            log.info("Names extracted for story {}: {}", storyId, names);

            // 8.2 - get locations involved in the story (add current location if lat, long present in header)
            List<String> locations = mockExtractLocations(detailedStory);
            log.info("Locations extracted for story {}: {}", storyId, locations);

            // 8.3 - fetch pincodes of the location (send location names)
            List<String> pincodes = mockFetchPincodes(locations);
            log.info("Pincodes fetched for story {}: {}", storyId, pincodes);

            // 8.4 - get language which the transcription is in from AI service / if not found add language from user document
            // Language is already detected in Step 1.5

            // 8.5 - store device info as well from headers
            String deviceInfo = mockGetDeviceInfo();
            log.info("Device info extracted for story {}: {}", storyId, deviceInfo);

            // Create metadata object
            StoryMetadata metadata = StoryMetadata.builder()
                    .names(names)
                    .locations(locations)
                    .pincodes(pincodes)
                    .state(mockGetState(locations))
                    .district(mockGetState(locations))
                    .language(language)
                    .deviceInfo(deviceInfo)
                    .build();

            // Step 9: Mock Shareable Link Generation
            String shareLink = mockCreateShareableLink(storyId);
            log.info("Shareable link created for story: {}", storyId);

            // Update the story with all processed data including metadata
            updateStoryWithAIResults(storyId, title, contents, tags, emotions, shareLink, metadata);

            log.info("AI processing completed successfully for story: {}", storyId);

        } catch (Exception e) {
            log.error("Error in AI processing for story {}: {}", storyId, e.getMessage(), e);
            // Update story status to REJECTED (failed processing)
            updateStoryStatusWithRejection(storyId, Story.StoryStatus.REJECTED, Arrays.asList("AI processing failed: " + e.getMessage()));
            throw new RuntimeException("AI processing failed: " + e.getMessage(), e);
        }
    }

    // Mock AI Service Methods

    /**
     * Mock audio language detection
     * In real implementation, this would call an AI service to detect the language
     * @param transcription The transcribed audio text
     * @return Detected language as string
     */
    private String mockDetectAudioLanguage(String transcription) {
        // Mock language detection based on some keywords or patterns
        String text = transcription.toLowerCase();

        // Simple keyword-based detection (in real implementation, use proper NLP/AI)
        if (text.contains("నేను") || text.contains("మీరు") || text.contains("అతను") || text.contains("ఆమె")) {
            return "TELUGU";
        } else if (text.contains("मैं") || text.contains("आप") || text.contains("वह") || text.contains("यह")) {
            return "HINDI";
        } else if (text.contains("நான்") || text.contains("நீங்கள்") || text.contains("அவன்") || text.contains("அவள்")) {
            return "TAMIL";
        } else if (text.contains("ನಾನು") || text.contains("ನೀವು") || text.contains("ಅವನು") || text.contains("ಅವಳು")) {
            return "KANNADA";
        } else if (text.contains("ഞാൻ") || text.contains("നിങ്ങൾ") || text.contains("അവൻ") || text.contains("അവൾ")) {
            return "MALAYALAM";
        } else if (text.contains("আমি") || text.contains("আপনি") || text.contains("সে") || text.contains("এটা")) {
            return "BENGALI";
        } else if (text.contains("मी") || text.contains("तुम्ही") || text.contains("तो") || text.contains("हे")) {
            return "MARATHI";
        } else if (text.contains("હું") || text.contains("તમે") || text.contains("તે") || text.contains("આ")) {
            return "GUJARATI";
        } else if (text.contains("ਮੈਂ") || text.contains("ਤੁਸੀਂ") || text.contains("ਉਹ") || text.contains("ਇਹ")) {
            return "PUNJABI";
        } else if (text.contains("میں") || text.contains("آپ") || text.contains("وہ") || text.contains("یہ")) {
            return "URDU";
        } else {
            // Default to English if no specific language patterns detected
            return "ENGLISH";
        }
    }

    private String mockTranscription() {
        return "I remember the day we first met. It was raining, and she was standing under that old oak tree, " +
                "looking so beautiful with her hair slightly wet. We talked for hours, and I knew right then " +
                "that she was the one. But life has a way of testing us, and sometimes love isn't enough to " +
                "overcome the challenges we face. We grew apart, and now I'm left with memories and a heart " +
                "that still beats for someone who's no longer mine.";
    }

    private String mockCreateDetailedStory(String transcription) {
        return "In the quiet corners of my mind, I still hear the echo of her laughter. " +
                "It was a Tuesday afternoon when the rain decided to play matchmaker, bringing two souls " +
                "together under the shelter of an ancient oak tree. She was reading a book, completely " +
                "unaware that her life was about to change forever.\n\n" +
                "Our eyes met, and in that moment, time stood still. The world around us faded into " +
                "nothingness as we discovered the magic of connection. We talked about everything and nothing, " +
                "sharing dreams, fears, and the kind of intimate thoughts that only lovers share.\n\n" +
                "But love, as beautiful as it is, can be fragile. Life threw us curveballs, and we found " +
                "ourselves drifting apart like ships in the night. The distance grew, not just physical, " +
                "but emotional too. We became strangers who once knew each other's hearts.\n\n" +
                "Now, as I sit here writing this story, I realize that some loves are meant to be memories " +
                "rather than forever. And that's okay. Because even though she's no longer mine, the love " +
                "we shared will always be a part of who I am.";
    }

    private String mockCreateTitle(String detailedStory) {
        String[] titles = {
                "Love Under the Oak Tree",
                "When Rain Brought Us Together",
                "Memories of a Tuesday Afternoon",
                "The Story of Us",
                "Fragile Hearts, Beautiful Memories",
                "Ships in the Night",
                "A Love That Became Memory",
                "The Echo of Her Laughter"
        };
        return titles[(int) (Math.random() * titles.length)];
    }

    private List<String> mockGenerateAnimatedImages(String detailedStory) {
        // Mock animated image URLs (skip for now as mentioned in comments)
        return Arrays.asList(
                "https://res.cloudinary.com/dohsebpd1/image/upload/v1750951801/animated_rain_scene.gif",
                "https://res.cloudinary.com/dohsebpd1/image/upload/v1750951801/animated_oak_tree.gif",
                "https://res.cloudinary.com/dohsebpd1/image/upload/v1750951801/animated_hearts.gif",
                "https://res.cloudinary.com/dohsebpd1/image/upload/v1750951801/animated_memories.gif"
        );
    }

    private List<Content> mockCreateContents(String detailedStory, List<String> animatedImages) {
        List<Content> contents = new ArrayList<>();

        // Split the story into paragraphs
        String[] paragraphs = detailedStory.split("\n\n");

        int imageIndex = 0;
        for (int i = 0; i < paragraphs.length; i++) {
            // Add text content
            Content textContent = Content.builder()
                    .type(Content.ContentType.TEXT)
                    .data(paragraphs[i].trim())
                    .orderIndex(i * 2)
                    .build();
            contents.add(textContent);

            // Add image content after every other paragraph (if images available)
            if (imageIndex < animatedImages.size() && i > 0) {
                Content imageContent = Content.builder()
                        .type(Content.ContentType.IMAGE)
                        .data(animatedImages.get(imageIndex))
                        .orderIndex(i * 2 + 1)
                        .build();
                contents.add(imageContent);
                imageIndex++;
            }
        }

        return contents;
    }

    private List<Emotion> mockGetEmotions(String detailedStory) {
        return Arrays.asList(
                Emotion.builder()
                        .type(Emotion.EmotionType.SAD)
                        .score(0.85)
                        .build(),
                Emotion.builder()
                        .type(Emotion.EmotionType.CALM)
                        .score(0.72)
                        .build(),
                Emotion.builder()
                        .type(Emotion.EmotionType.HAPPY)
                        .score(0.68)
                        .build(),
                Emotion.builder()
                        .type(Emotion.EmotionType.EXCITED)
                        .score(0.45)
                        .build(),
                Emotion.builder()
                        .type(Emotion.EmotionType.SURPRISED)
                        .score(0.38)
                        .build()
        );
    }

    private List<String> mockGetTags(String detailedStory) {
        return Arrays.asList(
                "breakup",
                "love",
                "memories",
                "rain",
                "oak tree",
                "nostalgia",
                "healing",
                "moving on"
        );
    }


    private String mockCreateShareableLink(String storyId) {
        return "https://breakupstories.app/story/" + storyId;
    }

    private String mockCreateThumbnailUrl(String storyId) {
        return "https://res.cloudinary.com/dohsebpd1/image/upload/v1750951801/thumbnail_" + storyId + ".jpg";
    }

    /**
     * Mock extraction of names from story text
     * In real implementation, this would call an AI service to extract named entities
     * @param storyText The story text to analyze
     * @return List of extracted names
     */
    private List<String> mockExtractNames(String storyText) {
        // Mock name extraction - in real implementation, use NLP/AI to extract named entities
        List<String> names = new ArrayList<>();

        // Simple pattern matching for common names (this is just a mock)
        String[] commonNames = {"Sarah", "John", "Emma", "Michael", "Priya", "Raj", "Anjali", "Amit", "Sneha", "Vikram"};
        String text = storyText.toLowerCase();

        for (String name : commonNames) {
            if (text.contains(name.toLowerCase())) {
                names.add(name);
            }
        }

        // If no names found, add some generic ones
        if (names.isEmpty()) {
            names.add("Anonymous");
        }

        return names;
    }

    /**
     * Mock extraction of locations from story text
     * In real implementation, this would call an AI service to extract location entities
     * @param storyText The story text to analyze
     * @return List of extracted locations
     */
    private List<String> mockExtractLocations(String storyText) {
        // Mock location extraction - in real implementation, use NLP/AI to extract location entities
        List<String> locations = new ArrayList<>();

        // Simple pattern matching for common locations (this is just a mock)
        String[] commonLocations = {"Mumbai", "Delhi", "Bangalore", "Chennai", "Hyderabad", "Kolkata", "Pune", "Ahmedabad", "Jaipur", "Lucknow"};
        String text = storyText.toLowerCase();

        for (String location : commonLocations) {
            if (text.contains(location.toLowerCase())) {
                locations.add(location);
            }
        }

        // If no locations found, add some generic ones
        if (locations.isEmpty()) {
            locations.add("Unknown Location");
        }

        return locations;
    }

    /**
     * Mock fetching of pincodes for locations
     * In real implementation, this would call a geocoding service
     * @param locations List of locations to get pincodes for
     * @return List of pincodes
     */
    private List<String> mockFetchPincodes(List<String> locations) {
        // Mock pincode fetching - in real implementation, call geocoding API
        List<String> pincodes = new ArrayList<>();

        // Mock pincodes for common cities
        Map<String, String> locationPincodes = Map.of(
                "Mumbai", "400001",
                "Delhi", "110001",
                "Bangalore", "560001",
                "Chennai", "600001",
                "Hyderabad", "500001",
                "Kolkata", "700001",
                "Pune", "411001",
                "Ahmedabad", "380001",
                "Jaipur", "302001",
                "Lucknow", "226001"
        );

        for (String location : locations) {
            String pincode = locationPincodes.get(location);
            if (pincode != null) {
                pincodes.add(pincode);
            } else {
                pincodes.add("000000"); // Default pincode for unknown locations
            }
        }

        return pincodes;
    }

    /**
     * Mock extraction of device info
     * In real implementation, this would extract from request headers
     * @return Device information string
     */
    private String mockGetDeviceInfo() {
        // Mock device info - in real implementation, extract from User-Agent header
        String[] devices = {
                "iPhone 14 Pro - iOS 16.0",
                "Samsung Galaxy S23 - Android 13",
                "OnePlus 11 - Android 13",
                "Google Pixel 7 - Android 13",
                "iPhone 13 - iOS 15.0"
        };

        return devices[(int) (Math.random() * devices.length)];
    }

    /**
     * Mock extraction of state from locations
     * In real implementation, this would call a geocoding service
     * @param locations List of locations
     * @return State name
     */
    private String mockGetState(List<String> locations) {
        // Mock state extraction - in real implementation, call geocoding API
        Map<String, String> locationStates = Map.of(
                "Mumbai", "Maharashtra",
                "Delhi", "Delhi",
                "Bangalore", "Karnataka",
                "Chennai", "Tamil Nadu",
                "Hyderabad", "Telangana",
                "Kolkata", "West Bengal",
                "Pune", "Maharashtra",
                "Ahmedabad", "Gujarat",
                "Jaipur", "Rajasthan",
                "Lucknow", "Uttar Pradesh"
        );

        for (String location : locations) {
            String state = locationStates.get(location);
            if (state != null) {
                return state;
            }
        }

        return "Unknown State";
    }

    private void updateStoryWithAIResults(String storyId, String title, List<Content> contents,
                                          List<String> tags, List<Emotion> emotions, String shareLink, StoryMetadata metadata) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found: " + storyId));

        story.setTitle(title);
        story.setContents(contents);
        story.setTags(tags);
        story.setEmotions(emotions);
        story.setShareLink(shareLink);
        story.setStatus(Story.StoryStatus.ACTIVE);

        // Mock and set thumbnailUrl
        story.setThumbnailUrl(mockCreateThumbnailUrl(storyId));

        // Update metadata
        StoryMetadata existingMetadata = story.getMetadata();
        if (existingMetadata == null) {
            existingMetadata = StoryMetadata.builder().build();
        }
        existingMetadata.setNames(metadata.getNames());
        existingMetadata.setLocations(metadata.getLocations());
        existingMetadata.setPincodes(metadata.getPincodes());
        existingMetadata.setState(metadata.getState());
        existingMetadata.setLanguage(metadata.getLanguage());
        existingMetadata.setDeviceInfo(metadata.getDeviceInfo());
        story.setMetadata(existingMetadata);

        storyRepository.save(story);
        log.info("Story updated with AI results: {}", storyId);
    }

    private void updateStoryStatusWithRejection(String storyId, Story.StoryStatus status, List<String> rejectionReasons) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found: " + storyId));

        story.setStatus(status);
        story.setRejectionReasons(rejectionReasons);
        storyRepository.save(story);
        log.info("Story status updated to {} with rejection reasons: {}", status, storyId);
    }




}
