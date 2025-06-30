# View Count Exclusion Update

## Overview

The view counting system has been updated to exclude views when a user views their own story. This prevents users from artificially inflating their story view counts by repeatedly viewing their own content.

## Changes Made

### 1. StoryController Updates

#### Modified `getStoryById()` method:
- Added logic to check if the user is viewing their own story
- Only increments view count if user is viewing someone else's story
- Always logs the view for audit purposes, but marks it as a self-view

```java
// Check if user is viewing their own story
boolean isOwnStory = false;
if (userId != null) {
    Story story = storyRepository.findById(storyId).orElse(null);
    isOwnStory = userId.equals(story.getUserId());
}

// Only increment view count if user is not viewing their own story
if (!isOwnStory) {
    storyService.incrementViewCount(storyId);
    log.info("View count incremented for story: {} (viewed by user: {})", storyId, userId);
} else {
    log.info("View count not incremented for story: {} (user viewing their own story: {})", storyId, userId);
}
```

### 2. AuditService Updates

#### New `logStoryView()` method:
- Added overloaded method that includes `isOwnStory` parameter
- Stores self-view information in audit metadata

```java
public void logStoryView(String userId, String storyId, String userAgent, String ipAddress, String sessionId, boolean isOwnStory) {
    Map<String, Object> metadata = Map.of(
        "interaction_type", "story_view",
        "is_own_story", isOwnStory
    );
    logAudit(userId, Audit.EntityType.STORY, Audit.ActionType.VIEW, storyId, userAgent, ipAddress, sessionId, metadata);
}
```

#### Updated `getViewsCountForUserStories()` method:
- Now excludes self-views from the count
- Filters out views where the viewer is the same as the story owner
- Returns accurate view counts for notifications

```java
// Filter out self-views (where viewer is the same as story owner)
long selfViewCount = viewAudits.stream()
        .filter(audit -> {
            String viewerId = audit.getUserId();
            String storyId = audit.getEntityId();
            
            Story story = storyRepository.findById(storyId).orElse(null);
            if (story != null) {
                return viewerId.equals(story.getUserId());
            }
            return false;
        })
        .count();

// Return total views minus self-views
return viewAudits.size() - selfViewCount;
```

#### Updated `getViewsCountForStory()` method:
- Also excludes self-views for individual story view counts
- Ensures consistent behavior across all view counting methods

### 3. AuditRepository Updates

#### New methods added:
- `findByEntityIdInAndEntityTypeAndActionType()` - Find audits for multiple entities
- `findByEntityIdInAndEntityTypeAndActionTypeAndCreatedAtAfter()` - Find audits with time filter
- `findByEntityIdAndEntityTypeAndActionType()` - Find audits for single entity
- `findByEntityIdAndEntityTypeAndActionTypeAndCreatedAtAfter()` - Find audits for single entity with time filter

## Behavior Changes

### Before:
- All story views were counted, including self-views
- Users could artificially inflate their view counts
- Notifications included self-views in the count

### After:
- Self-views are excluded from view counts
- View counts only reflect genuine external views
- Notifications show accurate external view counts
- Self-views are still logged for audit purposes

## Impact on Features

### 1. Story View Counts
- Story objects now show accurate view counts (excluding self-views)
- Trending stories are ranked by genuine external views
- User statistics reflect real engagement

### 2. Notifications
- View notifications only show external views
- Users won't receive notifications for viewing their own stories
- More meaningful engagement metrics

### 3. Analytics
- More accurate analytics data
- Better insights into genuine story performance
- Cleaner data for business intelligence

## Implementation Details

### View Detection Logic
```java
// Check if user is viewing their own story
boolean isOwnStory = userId.equals(story.getUserId());

// Only count views from other users
if (!isOwnStory) {
    // Increment view count
    // Log as external view
} else {
    // Don't increment view count
    // Log as self-view for audit
}
```

### Audit Metadata
Self-views are marked in audit logs with metadata:
```json
{
  "interaction_type": "story_view",
  "is_own_story": true
}
```

### Database Queries
The system now uses more complex queries to filter out self-views:
```java
// Get all view audits
List<Audit> viewAudits = auditRepository.findByEntityIdInAndEntityTypeAndActionType(
    userStoryIds, Audit.EntityType.STORY, Audit.ActionType.VIEW);

// Filter out self-views
long selfViewCount = viewAudits.stream()
    .filter(audit -> audit.getUserId().equals(storyOwnerId))
    .count();

// Return external views only
return viewAudits.size() - selfViewCount;
```

## Testing

### Test Scenarios

1. **User views their own story**
   - View count should not increment
   - Audit log should be created with `is_own_story: true`
   - Notification should not include this view

2. **User views someone else's story**
   - View count should increment
   - Audit log should be created with `is_own_story: false`
   - Notification should include this view

3. **Unauthenticated user views story**
   - View count should increment (no user context)
   - Audit log should be created normally

### Test Commands

```bash
# Test viewing own story
curl -X GET "http://localhost:8080/api/stories/STORY_ID" \
  -H "Authorization: Bearer USER_JWT_TOKEN"

# Test viewing other user's story
curl -X GET "http://localhost:8080/api/stories/OTHER_STORY_ID" \
  -H "Authorization: Bearer USER_JWT_TOKEN"

# Check notifications
curl -X GET "http://localhost:8080/api/notifications" \
  -H "Authorization: Bearer USER_JWT_TOKEN"
```

## Monitoring

### Logs to Monitor
- `View count incremented for story: {} (viewed by user: {})`
- `View count not incremented for story: {} (user viewing their own story: {})`
- `Audited story view for user {} on story {} (own story: {})`

### Metrics to Track
- Self-view vs external view ratio
- View count accuracy improvements
- Notification relevance improvements

## Future Enhancements

### 1. **Time-based Self-view Detection**
- Consider self-views as valid after a certain time period
- Allow users to view their own stories for editing purposes

### 2. **IP-based Detection**
- Detect self-views based on IP address
- Handle cases where multiple users share the same IP

### 3. **Session-based Detection**
- Use session information to detect self-views
- More accurate detection for shared devices

### 4. **Configurable Rules**
- Make self-view exclusion configurable
- Allow different rules for different user types

## Rollback Plan

If issues arise, the system can be rolled back by:
1. Reverting the view count logic to include all views
2. Removing the self-view filtering from audit queries
3. Restoring the original notification counting logic

## Performance Considerations

### Database Impact
- Additional queries to check story ownership
- More complex view counting logic
- Slight performance impact due to filtering

### Optimization Opportunities
- Cache story ownership information
- Batch process view counts
- Use database indexes for efficient filtering 