# Story Matching System

## Overview

The Story Matching System automatically finds the highest matching story for newly uploaded stories when their status changes to ACTIVE. This process runs asynchronously to avoid blocking the main story upload flow.

## Architecture

### Components

1. **StoryMatchingService** - Core service for finding story matches
2. **AsyncStoryMatchingService** - Handles async processing and audit logging
3. **MockAIService** - Triggers story matching after AI processing completes

### Flow

1. User uploads a story → Status: PROCESSING
2. AI processing completes → Status: ACTIVE
3. Async story matching is triggered
4. Mock service finds highest matching story (random for now)
5. Audit logs are created for both users involved in the match
6. Notification system can use these audit logs to show match notifications

## Implementation Details

### StoryMatchingService

```java
@Service
public class StoryMatchingService {
    
    public StoryMatchResult findHighestMatchingStory(String storyId) {
        // Mock implementation - returns random story ID and percentage
        // Will be replaced with actual AI service integration
    }
}
```

**StoryMatchResult:**
- `originalStoryId` - The story that was just activated
- `matchedStoryId` - The story with highest match percentage
- `percentage` - Match percentage (0-100)

### AsyncStoryMatchingService

```java
@Service
public class AsyncStoryMatchingService {
    
    @Async
    public void processStoryMatchingAsync(String storyId, String userId) {
        // 1. Find highest matching story
        // 2. Get matched story owner
        // 3. Ignore if same user
        // 4. Create audit logs for both users
    }
}
```

### Audit Logs

Two audit logs are created for each story match:

1. **For Original Story Owner:**
   - `entityType`: STORY
   - `actionType`: MATCH
   - `entityId`: original story ID
   - `metadata`: percentage, matched_story_id, original_story_id

2. **For Matched Story Owner:**
   - `entityType`: STORY
   - `actionType`: MATCH
   - `entityId`: matched story ID
   - `metadata`: percentage, matched_story_id, original_story_id

### Metadata Structure

```json
{
  "percentage": "85",
  "matched_story_id": "story_123",
  "original_story_id": "story_456"
}
```

## Integration Points

### MockAIService Integration

The story matching is triggered in `updateStoryWithAIResults()` method:

```java
private void updateStoryWithAIResults(String storyId, String title, List<Content> contents,
                                      List<String> tags, List<Emotion> emotions, String shareLink, StoryMetadata metadata) {
    // ... existing AI processing ...
    story.setStatus(Story.StoryStatus.ACTIVE);
    storyRepository.save(story);
    
    // Trigger async story matching
    asyncStoryMatchingService.processStoryMatchingAsync(storyId, story.getUserId());
}
```

### Notification System Integration

The notification system can fetch story match notifications by querying audit logs:

```java
// In NotificationService
List<Audit> storyMatches = auditRepository.findByUserIdAndEntityTypeAndActionType(
    userId, Audit.EntityType.STORY, Audit.ActionType.MATCH
);
```

## Configuration

### Async Processing

Async processing is enabled via `@EnableAsync` annotation in the main application class.

### Error Handling

- Story matching errors are logged but don't affect the main story upload flow
- If matched story is not found, the process is skipped
- If both users are the same, the match is ignored

## Future Enhancements

1. **AI Service Integration** - Replace mock service with actual AI-based story matching
2. **Match Quality Threshold** - Only create matches above a certain percentage
3. **Multiple Matches** - Return top N matching stories instead of just one
4. **Match Categories** - Different types of matches (emotional, situational, etc.)
5. **User Preferences** - Consider user preferences in matching algorithm

## API Usage

The story matching system is fully automated and doesn't require direct API calls. It triggers automatically when:

1. A story is uploaded and AI processing completes
2. Story status changes from PROCESSING to ACTIVE

## Monitoring

### Logs to Monitor

- `Starting async story matching process for storyId: {}`
- `Mock story match result - matchedStoryId: {}, percentage: {}%`
- `Creating audit log for original story owner: {}`
- `Creating audit log for matched story owner: {}`
- `Successfully completed story matching process for storyId: {}`

### Metrics to Track

- Number of story matches created per day
- Average match percentage
- Processing time for story matching
- Error rate in story matching process

## Testing

### Unit Tests

- Test story matching with same user (should be ignored)
- Test story matching with different users (should create audit logs)
- Test error handling when matched story not found

### Integration Tests

- End-to-end test from story upload to match creation
- Test async processing behavior
- Test audit log creation and structure 