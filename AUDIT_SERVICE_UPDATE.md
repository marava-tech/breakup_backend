# Audit Service Update: Story-Based Statistics

## Overview

The audit service has been updated to fetch likes, views, and comments count for stories owned by the current logged-in user, instead of counting interactions performed by the user.

## Changes Made

### 1. AuditService Updates

#### New Methods Added:
- `getUserStoryIds(String userId)` - Gets all story IDs owned by a user
- `getLikesCountForUserStories(String userId, LocalDateTime since)` - Counts likes on user's stories
- `getViewsCountForUserStories(String userId, LocalDateTime since)` - Counts views on user's stories  
- `getCommentsCountForUserStories(String userId, LocalDateTime since)` - Counts comments on user's stories
- `getStoryStatisticsForUser(String userId, LocalDateTime since)` - Gets combined statistics
- `getStoryWiseStatisticsForUser(String userId, LocalDateTime since)` - Gets per-story statistics

#### Helper Methods:
- `getLikesCountForStory(String storyId, LocalDateTime since)` - Counts likes for a specific story
- `getViewsCountForStory(String storyId, LocalDateTime since)` - Counts views for a specific story
- `getCommentsCountForStory(String storyId, LocalDateTime since)` - Counts comments for a specific story

### 2. AuditRepository Updates

#### New Methods Added:
- `countByEntityIdInAndEntityTypeAndActionType(List<String> entityIds, EntityType, ActionType)` - Counts audits for multiple entity IDs
- `countByEntityIdInAndEntityTypeAndActionTypeAndCreatedAtAfter(List<String> entityIds, EntityType, ActionType, LocalDateTime)` - Counts audits with time filter

### 3. NotificationService Updates

#### Method Changes:
- Updated `getNotifications()` to use new story-based counting methods
- Fixed entity types in story counting methods (LIKE → STORY, VIEW → STORY)

## Implementation Details

### Before (User-Based Counting):
```java
// Counted interactions performed BY the user
long likes = auditService.getNewLikesCount(userId, since); // User's likes on others' stories
long views = auditService.getNewViewsCount(userId, since); // User's views on others' stories
long comments = auditService.getNewCommentsCount(userId, since); // User's comments on others' stories
```

### After (Story-Based Counting):
```java
// Counts interactions ON the user's stories
long likes = auditService.getLikesCountForUserStories(userId, since); // Likes on user's stories
long views = auditService.getViewsCountForUserStories(userId, since); // Views on user's stories
long comments = auditService.getCommentsCountForUserStories(userId, since); // Comments on user's stories
```

## Usage Examples

### 1. Get Total Statistics for User's Stories
```java
Map<String, Long> stats = auditService.getStoryStatisticsForUser(userId, since);
long totalLikes = stats.get("likes");
long totalViews = stats.get("views");
long totalComments = stats.get("comments");
```

### 2. Get Per-Story Statistics
```java
Map<String, Map<String, Long>> storyStats = auditService.getStoryWiseStatisticsForUser(userId, since);
for (Map.Entry<String, Map<String, Long>> entry : storyStats.entrySet()) {
    String storyId = entry.getKey();
    Map<String, Long> storyData = entry.getValue();
    long storyLikes = storyData.get("likes");
    long storyViews = storyData.get("views");
    long storyComments = storyData.get("comments");
}
```

### 3. Get Individual Counts
```java
long likes = auditService.getLikesCountForUserStories(userId, since);
long views = auditService.getViewsCountForUserStories(userId, since);
long comments = auditService.getCommentsCountForUserStories(userId, since);
```

## Database Queries

### New Repository Methods:
```java
// Count likes on user's stories
countByEntityIdInAndEntityTypeAndActionType(
    userStoryIds, Audit.EntityType.STORY, Audit.ActionType.LIKE
)

// Count views on user's stories with time filter
countByEntityIdInAndEntityTypeAndActionTypeAndCreatedAtAfter(
    userStoryIds, Audit.EntityType.STORY, Audit.ActionType.VIEW, since
)
```

### MongoDB Query Example:
```javascript
// Count likes on user's stories
db.audits.countDocuments({
    entityId: { $in: ["story1", "story2", "story3"] },
    entityType: "STORY",
    actionType: "LIKE"
})

// Count views with time filter
db.audits.countDocuments({
    entityId: { $in: ["story1", "story2", "story3"] },
    entityType: "STORY", 
    actionType: "VIEW",
    createdAt: { $gt: ISODate("2024-01-01") }
})
```

## Benefits

1. **Accurate Statistics**: Now counts interactions ON the user's stories instead of BY the user
2. **Better Notifications**: Users get notified about activity on their own stories
3. **Performance**: Efficient queries using `$in` operator for multiple story IDs
4. **Flexibility**: Supports both total counts and per-story breakdowns
5. **Time Filtering**: Maintains support for time-based filtering

## Migration Notes

- Old methods (`getNewLikesCount`, `getNewViewsCount`, `getNewCommentsCount`) are deprecated but still functional
- New methods provide the same interface but with different semantics
- NotificationService automatically uses the new methods
- No database migration required - only query logic changes

## Testing

### Test Scenarios:
1. User with no stories should return 0 for all counts
2. User with stories but no interactions should return 0
3. User with stories and interactions should return correct counts
4. Time filtering should work correctly
5. Per-story statistics should match total statistics

### Example Test Data:
```javascript
// User creates stories
db.stories.insertMany([
    { _id: "story1", userId: "user1", title: "Story 1" },
    { _id: "story2", userId: "user1", title: "Story 2" }
])

// Others interact with user's stories
db.audits.insertMany([
    { entityId: "story1", entityType: "STORY", actionType: "LIKE", userId: "user2" },
    { entityId: "story1", entityType: "STORY", actionType: "VIEW", userId: "user3" },
    { entityId: "story2", entityType: "STORY", actionType: "LIKE", userId: "user4" },
    { entityId: "story2", entityType: "COMMENT", actionType: "CREATE", userId: "user5" }
])

// Expected results for user1:
// - Total likes: 2 (on story1 and story2)
// - Total views: 1 (on story1)
// - Total comments: 1 (on story2)
``` 