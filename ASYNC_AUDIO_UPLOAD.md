# Async Audio Upload System

## Overview

The Async Audio Upload System enhances the story creation process by making audio file uploads asynchronous, improving user experience and system performance. This system allows users to start story creation immediately while audio uploads and AI processing happen in the background.

## Features

- ✅ **Async Audio Upload**: Non-blocking audio file uploads
- ✅ **Immediate Response**: Users get story ID immediately
- ✅ **Background Processing**: AI processing continues after upload
- ✅ **Error Handling**: Comprehensive error handling and status updates
- ✅ **Status Tracking**: Real-time status updates from UPLOADING to PROCESSING to ACTIVE
- ✅ **Fallback Handling**: Automatic status updates on failures

## Architecture

### Components

1. **AsyncUploadService**: Handles async file uploads to external service
2. **AsyncStoryCreationService**: Coordinates the complete async story creation process
3. **StoryService**: Updated to use async creation flow
4. **StoryRepository**: Enhanced with methods to find pending stories

### Flow Diagram

```
User Request → Story Creation API
     ↓
Create Story (PROCESSING status, null audioUrl)
     ↓
Return Story Response Immediately
     ↓
Start Async Audio Upload
     ↓
Update Story with Audio URL
     ↓
Start Async AI Processing
     ↓
Update Story Status to ACTIVE/REJECTED
```

## API Endpoints

### Create Story (Async)
```
POST /api/stories
```

**Request:**
- `audio`: MultipartFile (required) - Audio file to upload
- `Authorization`: Bearer token (required)

**Response:**
```json
{
  "id": "story123",
  "title": "Uploading...",
  "audioUrl": null,
  "status": "PROCESSING",
  "userId": "user123",
  "viewCount": 0,
  "contents": [],
  "tags": [],
  "emotions": [],
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

**Status Codes:**
- `201 Created` - Story creation initiated successfully
- `400 Bad Request` - Missing or invalid audio file
- `401 Unauthorized` - Invalid or missing authentication
- `500 Internal Server Error` - Server error during creation

## Implementation Details

### AsyncUploadService

```java
@Service
public interface AsyncUploadService {
    CompletableFuture<String> uploadFileAsync(MultipartFile file);
    CompletableFuture<List<String>> uploadFilesAsync(List<MultipartFile> files);
}
```

**Features:**
- Uses `@Async` annotation for non-blocking execution
- Returns `CompletableFuture` for async result handling
- Comprehensive error handling and logging
- Same upload service integration as sync version

### AsyncStoryCreationService

```java
@Service
public class AsyncStoryCreationService {
    public CompletableFuture<Story> createStoryAsync(String userId, MultipartFile audioFile);
}
```

**Process:**
1. Upload audio file asynchronously
2. Find pending story by userId and null audioUrl
3. Update story with audio URL
4. Start AI processing
5. Handle errors and update status accordingly

### Story Status Flow

1. **PROCESSING** (Initial): Story created, waiting for audio upload
2. **PROCESSING** (After Upload): Audio uploaded, AI processing started
3. **ACTIVE**: AI processing completed successfully
4. **REJECTED**: Upload or AI processing failed

## Error Handling

### Upload Failures
- Story status updated to REJECTED
- Error logged with details
- User can retry with new request

### AI Processing Failures
- Story status updated to REJECTED
- Rejection reasons populated
- Audio URL remains available for retry

### Network Issues
- Automatic retry mechanisms
- Timeout handling
- Graceful degradation

## Configuration

### Async Processing
```yaml
# application.yml
spring:
  task:
    execution:
      pool:
        core-size: 5
        max-size: 10
        queue-capacity: 25
```

### Upload Service
```yaml
upload:
  service:
    url: http://localhost:9090
    endpoint: /api/v1/upload
```

## Monitoring and Logging

### Key Log Messages
- `Starting async story creation for user: {}`
- `Audio upload completed for user {}: {}`
- `Story updated with audio URL: {} for story: {}`
- `AI processing started for story: {}`
- `Async story creation completed for story: {}`
- `Async story creation failed for story {}: {}`

### Metrics to Monitor
- Upload success/failure rates
- Upload duration
- AI processing duration
- Story creation completion rates
- Error rates by failure type

## Benefits

### User Experience
- **Immediate Feedback**: Users get story ID instantly
- **Non-blocking**: No waiting for upload completion
- **Progress Indication**: Status updates show progress
- **Retry Capability**: Failed uploads can be retried

### System Performance
- **Reduced Response Time**: API responds immediately
- **Better Resource Utilization**: Async processing uses thread pool
- **Scalability**: Can handle more concurrent uploads
- **Resilience**: Failures don't block other operations

### Developer Experience
- **Clear Separation**: Upload logic separated from story creation
- **Error Isolation**: Upload failures don't affect story creation
- **Easy Testing**: Async components can be tested independently
- **Monitoring**: Comprehensive logging for debugging

## Usage Examples

### Frontend Integration

```javascript
// Create story with async upload
const formData = new FormData();
formData.append('audio', audioFile);

const response = await fetch('/api/stories', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`
  },
  body: formData
});

const story = await response.json();
console.log('Story created:', story.id);

// Poll for status updates
const checkStatus = async (storyId) => {
  const statusResponse = await fetch(`/api/stories/${storyId}`);
  const storyStatus = await statusResponse.json();
  
  if (storyStatus.status === 'ACTIVE') {
    console.log('Story is ready!');
  } else if (storyStatus.status === 'REJECTED') {
    console.log('Story creation failed');
  } else {
    // Continue polling
    setTimeout(() => checkStatus(storyId), 2000);
  }
};

checkStatus(story.id);
```

### Error Handling

```javascript
try {
  const response = await fetch('/api/stories', {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` },
    body: formData
  });
  
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
  }
  
  const story = await response.json();
  // Handle successful creation
  
} catch (error) {
  console.error('Story creation failed:', error);
  // Handle error (show user message, retry, etc.)
}
```

## Migration from Sync to Async

### Breaking Changes
- None - API interface remains the same
- Response format unchanged
- Authentication requirements unchanged

### Backward Compatibility
- Existing sync upload service still available
- Can be gradually migrated
- No database schema changes required

### Testing Strategy
1. **Unit Tests**: Test async services independently
2. **Integration Tests**: Test complete async flow
3. **Load Tests**: Verify performance improvements
4. **Error Tests**: Verify error handling scenarios

## Future Enhancements

### Planned Features
- **Progress Callbacks**: WebSocket notifications for upload progress
- **Batch Uploads**: Support for multiple audio files
- **Upload Resumption**: Resume interrupted uploads
- **Compression**: Automatic audio compression before upload
- **CDN Integration**: Direct upload to CDN for better performance

### Performance Optimizations
- **Chunked Uploads**: Large file upload optimization
- **Parallel Processing**: Multiple uploads simultaneously
- **Caching**: Audio URL caching for faster access
- **Compression**: Audio file compression before upload 