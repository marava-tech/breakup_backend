# Written Story Feature

## Overview

The Written Story feature allows users to create stories by typing text instead of recording audio. This feature provides an alternative way to share stories while maintaining the same AI processing pipeline.

## Features

- ✅ **Text Input**: Users can type their story text directly
- ✅ **Language Support**: Stories can be written in multiple languages
- ✅ **AI Processing**: Same AI pipeline as audio stories (rewrite, analysis, etc.)
- ✅ **Audio Generation**: Automatically generates audio from written text
- ✅ **Creation Type Tracking**: Distinguishes between RECORDED, UPLOADED, and WRITTEN stories

## API Endpoints

### Create Written Story
```
POST /api/stories/written
```

**Request Body:**
```json
{
  "storyText": "This is my story about a breakup that changed my life forever...",
  "language": "en"
}
```

**Response:**
```json
{
  "id": "story123",
  "title": "Processing....",
  "audioUrl": null,
  "status": "PROCESSING_PENDING",
  "userId": "user123",
  "viewCount": 0,
  "contents": [],
  "tags": [],
  "emotions": [],
  "creationType": "WRITTEN",
  "language": "en",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

## Story Creation Types

The system now supports three types of story creation:

1. **RECORDED**: Stories created by recording audio directly in the app
2. **UPLOADED**: Stories created by uploading audio files (current default)
3. **WRITTEN**: Stories created by typing text (new feature)

## Processing Flow

### Written Stories (Different Flow)
```
User Types Text → Story Creation → AI Rewrite → Audio Generation → AI Analysis → Final Story
```

### Audio Stories (Original Flow)
```
User Records/Uploads Audio → Story Creation → Audio Upload → Transcription → AI Rewrite → AI Analysis → Final Story
```

## Implementation Details

### 1. Story Model Updates
- Added `CreationType` enum with `RECORDED`, `UPLOADED`, `WRITTEN` values
- Added `creationType` field to Story model

### 2. New DTO
- `WrittenStoryRequest`: Contains story text and language

### 3. Audio Generation Service
- `AudioGenerationService`: Mock implementation for text-to-speech
- Currently returns placeholder URLs
- Can be replaced with real AI service integration

### 4. Processing Worker Updates
- `StoryProcessingWorker`: Handles different flows for written vs audio stories
- Written stories: Rewrite first, then generate audio
- Audio stories: Transcribe first, then rewrite

### 5. Story Conversion
- `StoryConversionWorker`: Preserves creation type when converting to final Story

## Configuration

### Audio Generation Service
Currently uses mock implementation. To integrate with real AI service:

```java
@Service
public class RealAudioGenerationService {
    public String generateAudioFromText(String text, String language) {
        // Call your AI service for text-to-speech
        // Return the generated audio URL
    }
}
```

## Usage Examples

### 1. Create Written Story
```bash
curl -X POST "http://localhost:8080/api/stories/written" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "storyText": "I never thought I would be writing this story, but here I am. It all started when...",
    "language": "en"
  }'
```

### 2. Check Story Status
```bash
curl -X GET "http://localhost:8080/api/stories/story123" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Error Handling

- **Missing Story Text**: Returns 400 Bad Request
- **Missing Language**: Returns 400 Bad Request
- **AI Service Errors**: Handled gracefully with retry logic
- **Audio Generation Failures**: Story marked as failed with error details

## Future Enhancements

1. **Real AI Integration**: Replace mock audio generation with actual text-to-speech service
2. **Voice Selection**: Allow users to choose different voices for audio generation
3. **Preview Audio**: Allow users to preview generated audio before publishing
4. **Draft Saving**: Save written stories as drafts before processing
5. **Rich Text Editor**: Enhanced text editor with formatting options

## Database Schema

### Story Collection
```json
{
  "_id": "story123",
  "userId": "user123",
  "title": "My Breakup Story",
  "audioUrl": "https://example.com/audio.mp3",
  "creationType": "WRITTEN",
  "language": "en",
  "status": "ACTIVE",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

### StoryDataStore Collection
```json
{
  "_id": "story123",
  "storyId": "story123",
  "userId": "user123",
  "language": "en",
  "processingStatus": "COMPLETED",
  "uploadMetadata": {
    "storyText": "Original story text...",
    "storyLanguage": "en",
    "creationType": "WRITTEN"
  },
  "audioUrl": "https://example.com/generated_audio.mp3",
  "duration": 120000
}
``` 