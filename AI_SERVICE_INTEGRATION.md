# AI Service Integration Guide

This guide explains how to integrate your AI service into the breakup stories backend.

## Overview

The backend now has a flexible AI service architecture that allows you to switch between mock and real AI implementations. The system includes:

- **AIService Interface**: Defines all AI functionalities
- **MockAIService**: Current mock implementation for testing
- **RealAIService**: Template for your actual AI service integration
- **AIServiceFactory**: Switches between implementations based on configuration
- **AIServiceConfig**: Configuration properties for AI service settings

## Current AI Functionalities

### 1. Comment Analysis
- **Purpose**: Detect hateful/negative comments
- **Method**: `analyzeComment(String commentText)`
- **Current**: Basic keyword-based filtering
- **Real AI**: Sentiment analysis, toxicity detection

### 2. Story Processing
- **Audio Transcription**: Convert audio to text
- **Language Detection**: Identify audio language
- **Story Creation**: Generate detailed stories from transcriptions
- **Title Generation**: Create compelling titles
- **Content Generation**: Create text and image content blocks
- **Emotion Analysis**: Analyze emotional content
- **Tag Generation**: Generate relevant tags
- **Metadata Extraction**: Extract names, locations, pincodes

### 3. Image Generation
- **Animated Images**: Generate story-related images
- **Thumbnails**: Create story thumbnails

## Configuration

### Application Properties

Add to `application.yml`:

```yaml
ai:
  service:
    provider: mock  # Change to "real" to use actual AI service
    enabled: true
    base-url: https://api.openai.com  # Your AI service URL
    api-key: ${AI_SERVICE_API_KEY:your-api-key-here}
    timeout: 30000
    max-retries: 3
    retry-delay: 1000
    models:
      text-generation: gpt-3.5-turbo
      text-analysis: gpt-3.5-turbo
      transcription: whisper-1
      image-generation: dall-e-2
      language-detection: gpt-3.5-turbo
    rate-limit:
      requests-per-minute: 60
      requests-per-hour: 1000
      max-concurrent-requests: 10
```

### Environment Variables

Set these environment variables for your AI service:

```bash
export AI_SERVICE_API_KEY="your-actual-api-key"
export AI_SERVICE_PROVIDER="real"
```

## Integration Steps

### Step 1: Choose Your AI Service Provider

The system is designed to work with various AI service providers:

- **OpenAI**: GPT models, Whisper, DALL-E
- **Azure OpenAI**: Microsoft's OpenAI service
- **Google AI**: PaLM, Gemini models
- **Custom API**: Your own AI service

### Step 2: Update RealAIService Implementation

1. **Modify `RealAIService.java`**:
   - Replace the `callOpenAI()` method with your actual API calls
   - Update request/response handling for your AI service
   - Implement proper error handling and retry logic

2. **Example for OpenAI**:
```java
private String callOpenAI(String model, String prompt) {
    Map<String, Object> requestBody = Map.of(
        "model", model,
        "messages", List.of(Map.of("role", "user", "content", prompt)),
        "max_tokens", 1000,
        "temperature", 0.7
    );
    
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + aiServiceConfig.getApiKey());
    headers.set("Content-Type", "application/json");
    
    HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
    
    ResponseEntity<Map> response = restTemplate.exchange(
        aiServiceConfig.getBaseUrl() + "/v1/chat/completions",
        HttpMethod.POST, request, Map.class
    );
    
    // Parse response and extract generated text
    Map<String, Object> responseBody = response.getBody();
    // Extract the actual response text from your AI service response
    
    return extractedText;
}
```

### Step 3: Implement Async Story Processing

Update the `processStoryWithAIAsync()` method in `RealAIService`:

```java
@Override
public void processStoryWithAIAsync(String storyId, String latitude, String longitude) {
    log.info("Starting real AI processing for story: {} with location: lat={}, lng={}", 
            storyId, latitude, longitude);
    
    try {
        // Step 1: Get story from database
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found: " + storyId));
        
        // Step 2: Transcribe audio (if audio file exists)
        String transcription = transcribeAudio(story.getAudioData());
        
        // Step 3: Detect language
        String language = detectAudioLanguage(transcription);
        
        // Step 4: Create detailed story
        String detailedStory = createDetailedStory(transcription);
        
        // Step 5: Generate title
        String title = createTitle(detailedStory);
        
        // Step 6: Generate images
        List<String> animatedImages = generateAnimatedImages(detailedStory);
        
        // Step 7: Create content blocks
        List<Content> contents = createContents(detailedStory, animatedImages);
        
        // Step 8: Analyze emotions
        List<Emotion> emotions = getEmotions(detailedStory);
        
        // Step 9: Generate tags
        List<String> tags = getTags(detailedStory);
        
        // Step 10: Extract metadata
        List<String> names = extractNames(detailedStory);
        List<String> locations = extractLocations(detailedStory);
        List<String> pincodes = fetchPincodes(locations);
        
        // Step 11: Create metadata
        StoryMetadata metadata = StoryMetadata.builder()
                .names(names)
                .locations(locations)
                .pincodes(pincodes)
                .state(getState(locations))
                .district(getState(locations))
                .language(language)
                .deviceInfo(getDeviceInfo())
                .build();
        
        // Step 12: Update story with results
        updateStoryWithAIResults(storyId, title, contents, tags, emotions, metadata);
        
        log.info("Real AI processing completed successfully for story: {}", storyId);
        
    } catch (Exception e) {
        log.error("Error in real AI processing for story {}: {}", storyId, e.getMessage(), e);
        updateStoryStatusWithRejection(storyId, Story.StoryStatus.REJECTED, 
                Arrays.asList("AI processing failed: " + e.getMessage()));
    }
}
```

### Step 4: Switch to Real AI Service

1. **Update configuration**:
```yaml
ai:
  service:
    provider: real  # Change from "mock" to "real"
```

2. **Set environment variables**:
```bash
export AI_SERVICE_API_KEY="your-api-key"
```

3. **Restart the application**

## Testing

### Test with Mock Service

1. Ensure configuration has `provider: mock`
2. Start the application
3. Upload a story and check logs for mock processing

### Test with Real Service

1. Update configuration to `provider: real`
2. Set your API key
3. Start the application
4. Upload a story and monitor real AI processing

## Error Handling

The system includes comprehensive error handling:

- **API Timeouts**: Configurable timeout settings
- **Retry Logic**: Automatic retries with configurable delays
- **Fallback Responses**: Default responses when AI service fails
- **Logging**: Detailed logging for debugging

## Rate Limiting

Configure rate limiting to avoid API quota issues:

```yaml
ai:
  service:
    rate-limit:
      requests-per-minute: 60
      requests-per-hour: 1000
      max-concurrent-requests: 10
```

## Monitoring

### Health Checks

The system includes health checks for AI service:

- Check if AI service is enabled
- Verify API connectivity
- Monitor response times

### Logging

Enable detailed logging for AI service:

```yaml
logging:
  level:
    com.breakupstories.service: DEBUG
```

## Security Considerations

1. **API Key Management**: Use environment variables, not hardcoded keys
2. **Request Validation**: Validate all inputs before sending to AI service
3. **Response Sanitization**: Sanitize AI responses before storing
4. **Rate Limiting**: Implement proper rate limiting to prevent abuse

## Cost Optimization

1. **Model Selection**: Choose appropriate models for each task
2. **Caching**: Cache common AI responses
3. **Batch Processing**: Process multiple items together when possible
4. **Monitoring**: Monitor API usage and costs

## Troubleshooting

### Common Issues

1. **API Key Issues**: Check environment variables and API key validity
2. **Timeout Errors**: Increase timeout settings or optimize requests
3. **Rate Limit Errors**: Implement proper rate limiting
4. **Response Parsing**: Ensure correct parsing of AI service responses

### Debug Mode

Enable debug logging:

```yaml
logging:
  level:
    com.breakupstories.service: DEBUG
    com.breakupstories.config: DEBUG
```

## Next Steps

1. **Implement your specific AI service calls** in `RealAIService`
2. **Add proper error handling** for your AI service
3. **Implement rate limiting** if needed
4. **Add monitoring and alerting** for AI service health
5. **Test thoroughly** with real data
6. **Monitor costs** and optimize usage

## Support

For issues with AI service integration:

1. Check the application logs for detailed error messages
2. Verify your AI service configuration
3. Test with the mock service first
4. Ensure your AI service API is working correctly

The system is designed to be flexible and can work with any AI service provider. Just update the `RealAIService` implementation to match your specific AI service API. 