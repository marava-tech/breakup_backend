# Story View Cooldown Update

## Overview

The story viewing system has been enhanced with a 1-minute cooldown period to prevent spam viewing and ensure more accurate view counts. This feature prevents users from artificially inflating view counts by repeatedly viewing the same story within a short time period.

## Changes Made

### 1. **AuditService Updates**
- Added 1-minute cooldown check before processing views
- Updated view counting logic to exclude cooldown views
- Enhanced IP-based cooldown for unauthenticated users

### 2. **StoryController Updates**
```java
// Check if user has viewed this story recently (1-minute cooldown)
boolean hasViewedRecently = false;
if (userId != null) {
    hasViewedRecently = auditService.hasViewedStoryRecently(userId, storyId);
} else if (ipAddress != null) {
    hasViewedRecently = auditService.hasViewedStoryRecentlyByIP(ipAddress, storyId);
}

if (hasViewedRecently) {
    log.info("View skipped due to 1-minute cooldown - story: {}, user: {}, ip: {}", storyId, userId, ipAddress);
    return ResponseEntity.ok(response);
}
```

### 3. **View Counting Logic**
- Excludes self-views (users viewing their own stories)
- Excludes views within 1-minute cooldown period
- Handles both authenticated users (by user ID) and unauthenticated users (by IP address)

## Implementation Details

### Cooldown Logic

The cooldown system works as follows:

1. **Time Calculation**: 
   ```java
// Calculate timestamp for 1 minute ago
long oneMinuteAgo = System.currentTimeMillis() - (60 * 1000);
```

2. **Recent View Check**:
   - For authenticated users: Check by user ID
   - For unauthenticated users: Check by IP address

3. **View Exclusion**:
   - 1-minute period from the last view
   - Applies to both authenticated and unauthenticated users
   - 1-minute period from the last view

### Configuration

- **Duration**: 1 minute (60,000 milliseconds)
- **Scope**: Per user/IP per story
- **Reset**: After 1 minute, the same user/IP can view the story again

## API Behavior

### 1. **First View**
- User views a story for the first time
- View count increments
- Audit log created
- Cooldown period starts

### 2. **Repeated View Within 1 Minute**
- User views the same story again within 1 minute
- View count does not increment
- Audit log not created
- Response returned without view count change

### 3. **View After 1 Minute**
- User views the same story after 1 minute
- View count increments
- Audit log created
- New cooldown period starts

## Testing Scenarios

### Test Cases

1. **First View Test**
```bash
# First view should work
curl -X GET "http://localhost:8080/api/stories/{storyId}" \
  -H "Authorization: Bearer YOUR_TOKEN"
# Expected: View count increments
```

2. **Repeated View Test**
```bash
# Test repeated view within 1 minute (should be skipped)
curl -X GET "http://localhost:8080/api/stories/{storyId}" \
  -H "Authorization: Bearer YOUR_TOKEN"
# Expected: View count does not increment
```

3. **After Cooldown Test**
```bash
# Test view after 1 minute (should work)
# Wait 1 minute, then:
curl -X GET "http://localhost:8080/api/stories/{storyId}" \
  -H "Authorization: Bearer YOUR_TOKEN"
# Expected: View count increments
```

### Log Messages

The system logs the following messages:

- `View count incremented for story: {} (viewed by user: {}, ip: {})`
- `View count not incremented for story: {} (user viewing their own story: {})`
- `View skipped due to 1-minute cooldown - story: {} user: {} ip: {}`

## Benefits

1. **Accurate View Counts**: Prevents artificial inflation of view counts
2. **Spam Prevention**: Reduces abuse from repeated viewing
3. **Performance**: Reduces unnecessary database writes
4. **Fair Metrics**: Ensures more accurate engagement metrics

## Future Enhancements

The 1-minute cooldown is currently hardcoded but can be made configurable:

```yaml
# application.yml
story:
  view:
    cooldown:
      duration: 60 # seconds
      enabled: true
```

This would allow for easy adjustment of the cooldown period based on business requirements. 