# Transcription Integration

## Overview

This document describes the integration of transcription functionality into the Breakup Stories backend API. The system now supports real-time audio transcription using an external AI service.

## Features

- **Real-time Transcription**: Transcribe audio files from URLs using external AI service
- **Story Rewrite**: Rewrite and enhance stories from transcripts using AI
- **Language Detection**: Automatic language detection and support for multiple languages
- **Confidence Scoring**: Get confidence scores for transcription accuracy
- **Search Integration**: Transcription data can be stored in search index when needed (not automatic)
- **User Language Preference**: Use user's preferred language for transcription

## Architecture

```
Story Upload → Audio Processing → Transcription Service → Story Rewrite Service → AI Service → Search Index Update
```

## API Endpoints

### 1. Transcribe Audio from URL
```
POST /api/v1/transcription/transcribe-url
```

**Request Body:**
```json
{
  "audioUrl": "https://res.cloudinary.com/dohsebpd1/video/upload/v1751199673/breakup/recording_2.mp3",
  "language": "te"
}
```

**Response:**
```json
{
  "transcript": "కాలం మారుతుంది ఒకప్పుడు అబ్బాయిలు అమ్మాయిలు మోసం చేశారని ఇప్పుడు అమ్మాయిలే అబ్బాయిలు మోసం చేస్తున్నారు ఇలాంటి కాలంలో కూడా నా దగ్గర ఉంది అదే నా ప్రేమకథ",
  "language": "te",
  "confidence": 0.5880911201238632
}
```

### 2. Transcribe Story Audio
```
POST /api/v1/transcription/transcribe-story/{storyId}
```

**Response:**
```json
{
  "transcript": "Transcribed text from the story audio",
  "language": "en",
  "confidence": 0.85
}
```

### 3. Rewrite Story from Transcript
```
POST /api/v1/story-rewrite/rewrite
```

**Request Body:**
```json
{
  "transcript": "కాలం మారుతుంది ఒకప్పుడు అబ్బాయిలు అమ్మాయిలు మోసం చేశారని ఇప్పుడు అమ్మాయిలే అబ్బాయిలు మోసం చేస్తున్నారు ఇలాంటి కాలంలో కూడా నా దగ్గర ఉంది అదే నా ప్రేమకథ",
  "language": "te"
}
```

**Response:**
```json
{
  "originalTranscript": "కాలం మారుతుంది ఒకప్పుడు అబ్బాయిలు అమ్మాయిలు మోసం చేశారని ఇప్పుడు అమ్మాయిలే అబ్బాయిలు మోసం చేస్తున్నారు ఇలాంటి కాలంలో కూడా నా దగ్గర ఉంది అదే నా ప్రేమకథ",
  "rewrittenStory": "కాలం మారింది గురూ! ఒకప్పుడు మగాళ్లు ఆడోళ్లని ఏడిపించేవాళ్లు, ఇప్పుడు ఆడోళ్లే మగాళ్లకి చుక్కలు చూపిస్తున్నారు. ఐనా ఈ కలికాలంలో కూడా నా దగ్గర ఏదన్నా మిగిలి ఉందంటే అది నా ప్రేమ కథే! ఛ! ఎంత సతాయిస్తుందో!",
  "language": "te"
}
```

## Configuration

### External AI Service
The system integrates with an external AI service that provides transcription and story rewrite capabilities. The AI service is now independent and runs separately from this application.

**Transcription Endpoint:** `POST {base-url}/transcription/transcribe-url`

**Request Format:**
- Content-Type: `multipart/form-data`
- Fields:
  - `audio_url`: The audio URL to transcribe
  - `language`: Language code (e.g., "te", "en", "hi")

**Response Format:**
```json
{
  "transcript": "Transcribed text",
  "language": "language_code",
  "confidence": 0.85
}
```

**Story Rewrite Endpoint:** `POST {base-url}/story-rewrite/rewrite`

**Request Format:**
- Content-Type: `application/json`
- Body:
  - `transcript`: The original transcript
  - `language`: Language code (e.g., "te", "en", "hi")

**Response Format:**
```json
{
  "originalTranscript": "Original transcript text",
  "rewrittenStory": "Enhanced and rewritten story",
  "language": "language_code"
}
```

## Integration Points

### 1. Story Processing Pipeline
When a story is uploaded and processed:

1. **Audio Upload**: Story audio is uploaded to Cloudinary
2. **Story Creation**: Story is created with `PROCESSING` status
3. **Async Processing**: AI processing is triggered asynchronously
4. **Transcription**: Audio is transcribed using external AI service
5. **Search Index Update**: Transcription data is stored in search index
6. **Story Update**: Story is updated with processed content

### 2. Search Index Integration (Optional)
Transcription data can be stored in the `StorySearchIndex` collection when needed:

```json
{
  "storyId": "story123",
  "transcript": "Transcribed text content",
  "transcriptionLanguage": "te",
  "transcriptionConfidence": 0.85,
  "searchText": "Title transcript tags names locations",
  "errors": [
    "Transcription failed: Network timeout",
    "Story rewrite failed: Invalid language code"
  ]
}
```

**Note:** Transcription data is not automatically saved to collections. This will be handled later when needed.

### 3. Language Handling
- **User Language**: Retrieved from user's `preferredStoryLanguage` field
- **Fallback**: Defaults to "en" if user language is not available
- **Detection**: AI service can detect language if not specified

## Usage Examples

### 1. Transcribe Audio from URL
```bash
curl -X POST http://localhost:8081/api/v1/transcription/transcribe-url \
  -H "Content-Type: application/json" \
  -d '{
    "audioUrl": "https://res.cloudinary.com/dohsebpd1/video/upload/v1751199673/breakup/recording_2.mp3",
    "language": "te"
  }'
```

### 2. Transcribe Story Audio
```bash
curl -X POST http://localhost:8080/api/v1/transcription/transcribe-story/story123
```

### 3. Rewrite Story from Transcript
```bash
curl -X POST http://localhost:8080/api/v1/story-rewrite/rewrite \
  -H "Content-Type: application/json" \
  -d '{
    "transcript": "కాలం మారుతుంది ఒకప్పుడు అబ్బాయిలు అమ్మాయిలు మోసం చేశారని ఇప్పుడు అమ్మాయిలే అబ్బాయిలు మోసం చేస్తున్నారు ఇలాంటి కాలంలో కూడా నా దగ్గర ఉంది అదే నా ప్రేమకథ",
    "language": "te"
  }'
```

### 4. Rewrite Story into Paragraphs
```bash
curl -X POST http://localhost:8080/api/paragraph-rewrite/rewrite \
  -H "Content-Type: application/json" \
  -d '{
    "transcript": "కాలం ఎంత మారిపోయిందో కదా! ఒకప్పుడు అబ్బాయిలు అమ్మాయిలని మోసం చేశారని వినేవాళ్ళం. ఇప్పుడు సీన్ రివర్స్! అమ్మాయిలే అబ్బాయిలని ముంచేస్తున్నారు. ఇలాంటి కల్లా కపటం లేని ప్రేమలు కరువైపోయిన రోజుల్లో, నా దగ్గర మాత్రం ఇంకా నిలిచివుంది... అదే నా స్వచ్ఛమైన ప్రేమకథ. వింటారా మరి?",
    "language": "te"
  }'
```

**Response:**
```json
{
  "contents": [
    {
      "type": "TEXT",
      "data": "అబ్బో, కాలం గిర్రున తిరిగిపోయింది కదూ! ఒకప్పుడు అబ్బాయిలు అమ్మాయిల మనసులతో ఆడుకునేవారని తెగ బాధపడేవాళ్ళం. ఇప్పుడేమో సీన్ రివర్స్ అయిపోయింది! అమ్మాయిలే అబ్బాయిలకి టోపీ పెడుతున్నారు. ఛా! ఇలాంటి కల్లా కపటం లేని ప్రేమలు కరువైపోయిన ఈ రోజుల్లో కూడా, నా గుండెల్లో మాత్రం ఇంకా ఒక స్వచ్ఛమైన ప్రేమ నిలిచి ఉంది...",
      "orderIndex": 0
    },
    {
      "type": "TEXT",
      "data": "అదో వెర్రి వెర్రి ప్రేమకథ! వింటారా అయితే, నా గోల భరించగలరా?",
      "orderIndex": 2
    }
  ],
  "language": "te"
}
```

### 5. Analyze Story
```bash
curl -X POST http://localhost:8080/api/story-analysis/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "story": "కాలం ఎంత మారిపోయిందో కదా! ఒకప్పుడు వరంగల్ లో అబ్బాయిలు అమ్మాయిలని మోసం చేశారని వినేవాళ్ళం. ఇప్పుడు సీన్ రివర్స్! అమ్మాయిలే అబ్బాయిలని ముంచేస్తున్నారు. ఇలాంటి కల్లా కపటం లేని ప్రేమలు కరువైపోయిన రోజుల్లో, నా దగ్గర మాత్రం ఇంకా నిలిచివుంది... అదే నా స్వచ్ఛమైన ప్రేమకథ. వింటారా మరి?",
    "language": "te"
  }'
```

**Response:**
```json
{
  "success": true,
  "analysis": {
    "emotions_with_scores": {
      "love": 0.7,
      "sadness": 0.4,
      "anger": 0.2,
      "joy": 0.3,
      "fear": 0.1,
      "surprise": 0.3,
      "disgust": 0.1,
      "trust": 0.5
    },
    "tags": [
      "love",
      "betrayal",
      "heartbreak",
      "cheat",
      "changing_times"
    ],
    "locations": [
      "Warangal"
    ],
    "names": [],
    "story_type": "love_story",
    "is_valid_story": true,
    "themes": [
      "love",
      "betrayal",
      "changing social norms",
      "nostalgia"
    ],
    "plot_summary": "The narrator reflects on how times have changed, with women now being the ones who deceive in relationships, contrasting with the past. The narrator then introduces their own pure love story, implying it's a contrast to the current trend.",
    "cultural_elements": [
      "Telugu societal views on relationships",
      "Gender roles and expectations in Telugu culture",
      "Changing dynamics in love and relationships"
    ]
  },
  "error": null
}
```

### 6. Detect Abuse in Comments
```bash
curl -X POST http://localhost:8080/api/abuse-detection/detect \
  -H "Content-Type: application/json" \
  -d '{
    "comment": "nuv chaala baagunnav",
    "language": "te"
  }'
```

**Response:**
```json
{
  "success": true,
  "is_abusive": false,
  "confidence": 0.9,
  "category": "none",
  "explanation": "The Telugu phrase \"nuv chaala baagunnav\" translates to \"you look very beautiful\" or \"you are very beautiful.\" This is a compliment and does not contain any abusive or harmful content targeting women.",
  "error": null
}
```

### 7. Get Location Information
```bash
curl -X POST http://localhost:8080/api/location-info/get-location \
  -H "Content-Type: application/json" \
  -d '{
    "latitude": 12.9716,
    "longitude": 77.5946
  }'
```

**Response:**
```json
{
  "success": true,
  "pincode": "560001",
  "district": "Bangalore Urban",
  "state": "Karnataka",
  "country": "India",
  "full_address": "Near MG Road, Bangalore, Karnataka, India",
  "error": null
}
```

### 3. Story Processing (Service Only)
When a story is uploaded, transcription service is available but not automatically saved:

```java
// In MockAIService.processStoryWithAIAsync()
TranscriptionResponse response = transcriptionService.transcribeAudioFromUrl(audioUrl, userLanguage);
String transcription = response.getTranscript();
String language = response.getLanguage();
Double confidence = response.getConfidence();

// Note: Transcription details are not automatically saved to search index
// This will be handled later when needed

// Rewrite story (optional - can be called later)
String rewrittenStory = aiService.rewriteStory(transcription, language);

// Rewrite story into paragraphs (optional - can be called later)
ParagraphRewriteResponse paragraphResponse = aiService.rewriteStoryIntoParagraphs(transcription, language);
List<ParagraphContent> contents = paragraphResponse.getContents();

// Analyze story for emotions, tags, locations, themes, and cultural elements (optional - can be called later)
StoryAnalysisResponse analysisResponse = aiService.analyzeStory(transcription, language);
StoryAnalysis analysis = analysisResponse.getAnalysis();

// Detect abuse in comments (optional - can be called later)
AbuseDetectionResponse abuseResponse = aiService.detectAbuse(comment, language);
Boolean isAbusive = abuseResponse.getIs_abusive();
Double confidence = abuseResponse.getConfidence();

// Get location information from coordinates (optional - can be called later)
LocationInfoResponse locationResponse = aiService.getLocationInfo(latitude, longitude);
String district = locationResponse.getDistrict();
String state = locationResponse.getState();
String pincode = locationResponse.getPincode();
```

## Error Handling

### 1. Network Errors
- Retry mechanism with exponential backoff
- Fallback to mock transcription if external service is unavailable
- Logging of all errors for debugging

### 2. Invalid Audio URLs
- Validation of audio URL format
- Error response with descriptive messages
- Graceful degradation

### 3. Language Issues
- Default language fallback
- User language preference handling
- Language detection by AI service

### 4. Error Storage
- Errors are stored in the `StorySearchIndex` collection
- Multiple errors can be accumulated for a single story
- Errors can be cleared when processing is retried
- Error tracking helps with debugging and monitoring

### 5. Story Analysis Features
- **Emotions Analysis**: Detects 8 different emotions with confidence scores
- **Tag Generation**: Automatically generates relevant tags for the story
- **Location Extraction**: Identifies locations mentioned in the story
- **Name Extraction**: Extracts person names from the story
- **Theme Detection**: Identifies underlying themes and motifs
- **Cultural Analysis**: Analyzes cultural elements and context
- **Plot Summary**: Generates a concise summary of the story
- **Story Validation**: Determines if the story is valid and appropriate

### 6. Abuse Detection Features
- **Content Analysis**: Detects abusive, hateful, or inappropriate content
- **Confidence Scoring**: Provides confidence levels for detection accuracy
- **Category Classification**: Categorizes abuse types (hate_speech, harassment, etc.)
- **Explanation**: Provides detailed explanations for detection results
- **Multi-language Support**: Works with various languages including Telugu
- **Real-time Detection**: Can be used for real-time comment moderation

### 7. Location Information Features
- **Coordinate Processing**: Converts latitude/longitude to address information
- **Address Details**: Provides district, state, country, and pincode
- **Full Address**: Returns complete formatted address
- **Geocoding**: Reverse geocoding from coordinates to human-readable addresses
- **Error Handling**: Graceful handling of invalid coordinates
- **Mock Fallback**: Provides realistic mock data when external service is unavailable

#### Error Management Methods:
```java
// Add single error
storySearchIndexService.addErrorToSearchIndex(storyId, "Transcription failed: Network timeout");

// Add multiple errors
storySearchIndexService.updateSearchIndexWithErrors(storyId, Arrays.asList(
    "Transcription failed: Network timeout",
    "Story rewrite failed: Invalid language code"
));

// Clear all errors
storySearchIndexService.clearErrorsFromSearchIndex(storyId);
```

## Performance Considerations

### 1. Async Processing
- Transcription happens asynchronously to avoid blocking
- Story status is updated to reflect processing state
- Users can continue using the app while processing

### 2. Caching
- Consider caching transcription results for repeated requests
- Store transcription data in search index for quick access

### 3. Rate Limiting
- Implement rate limiting for external AI service calls
- Queue processing for high-volume scenarios

## Monitoring and Logging

### 1. Logging
- All transcription requests are logged with details
- Error logs include stack traces for debugging
- Performance metrics are tracked

### 2. Metrics
- Transcription success rate
- Average processing time
- Language distribution
- Confidence score distribution

## Future Enhancements

### 1. Real-time Transcription
- WebSocket support for real-time transcription
- Progress updates during processing

### 2. Multiple Language Support
- Support for more languages
- Automatic language detection
- Language-specific processing

### 3. Quality Improvements
- Confidence score thresholds
- Manual review for low-confidence transcriptions
- Transcription correction interface

## Troubleshooting

### Common Issues

1. **External Service Unavailable**
   - Check AI service status
   - Verify network connectivity
   - Check configuration settings

2. **Language Detection Issues**
   - Verify user language preferences
   - Check language code format
   - Test with known language samples

3. **Performance Issues**
   - Monitor processing times
   - Check resource usage
   - Optimize async processing

### Debug Commands

```bash
# Check AI service health
curl http://localhost:8000/health

# Test transcription endpoint
curl -X POST http://localhost:8000/transcription/transcribe-url \
  --form 'audio_url="test_url"' \
  --form 'language="en"'

# Check application logs
docker-compose logs breakup-stories-api
```

## Security Considerations

1. **URL Validation**: Validate audio URLs before processing
2. **Rate Limiting**: Implement rate limiting for transcription requests
3. **Error Handling**: Don't expose internal errors to clients
4. **Authentication**: Ensure transcription endpoints are properly secured

This transcription integration provides a robust foundation for audio processing in the Breakup Stories application, with proper error handling, performance optimization, and future extensibility. 