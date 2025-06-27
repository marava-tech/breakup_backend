# Like/Unlike Counting Logic Update

## Overview
Updated the like counting logic to only count "effective likes" - likes that don't have corresponding unlikes. This ensures that if a user likes a story and then unlikes it, that like is not counted in notifications.

## Changes Made

### 1. AuditRepository Updates
Added new methods to support like/unlike logic:

- `findLikesByEntityIdAndEntityType()` - Find all likes for a specific entity
- `findLikesByEntityIdsAndEntityType()` - Find all likes for multiple entities
- `findLikesByEntityIdAndEntityTypeAndCreatedAtAfter()` - Find likes since a date for a specific entity
- `findLikesByEntityIdsAndEntityTypeAndCreatedAtAfter()` - Find likes since a date for multiple entities
- `findUnlikesByEntityIdAndEntityType()` - Find all unlikes for a specific entity
- `findUnlikesByEntityIdsAndEntityType()` - Find all unlikes for multiple entities

### 2. AuditService Updates
Updated like counting methods to implement effective like counting:

#### New Methods Added:
- `getEffectiveLikesCountForStories()` - Count effective likes for multiple stories
- `getEffectiveLikesCountForStoriesSince()` - Count effective likes since a date for multiple stories
- `getEffectiveLikesCountForStory()` - Count effective likes for a single story
- `getEffectiveLikesCountForStorySince()` - Count effective likes since a date for a single story
- `calculateEffectiveLikes()` - Core logic to filter out likes with corresponding unlikes

#### Updated Methods:
- `getLikesCountForUserStories()` - Now uses effective like counting
- `getLikesCountForStory()` - Now uses effective like counting

### 3. NotificationService Updates
Updated `getStoryLikeCount()` method to use the new effective like counting logic from AuditService.

## How the Logic Works

### Effective Like Calculation
1. **Fetch all likes** for the given stories/entities
2. **Fetch all unlikes** for the same stories/entities
3. **Create a lookup map** of user -> entity -> unlike timestamp
4. **Filter likes** by checking if:
   - User has no unlikes for this entity (like is valid)
   - User has an unlike but it happened before the like (like is valid)
   - User has an unlike that happened after the like (like is invalid - exclude it)

### Example Scenarios

#### Scenario 1: User likes, then unlikes
- User A likes Story X at 10:00 AM
- User A unlikes Story X at 2:00 PM
- **Result**: Like is NOT counted (excluded because unlike happened after like)

#### Scenario 2: User unlikes, then likes again
- User A unlikes Story X at 10:00 AM
- User A likes Story X at 2:00 PM
- **Result**: Like IS counted (unlike happened before like)

#### Scenario 3: User only likes (no unlike)
- User A likes Story X at 10:00 AM
- **Result**: Like IS counted (no unlike exists)

## Benefits

1. **Accurate Counts**: Only shows actual current likes, not historical likes that were later removed
2. **Better User Experience**: Users see meaningful notification counts
3. **Data Integrity**: Reflects the true state of user interactions
4. **Performance**: Efficient filtering using in-memory processing

## Impact on APIs

### Affected Endpoints:
- `/getnotifications` - Now shows accurate like counts
- Any other endpoints using `getLikesCountForUserStories()` or `getLikesCountForStory()`

### Response Changes:
- Like counts in notifications will be lower (more accurate)
- Only reflects current active likes, not historical likes that were unliked

## Testing Considerations

1. **Test like/unlike scenarios** to ensure counts are accurate
2. **Test multiple users** liking/unliking the same story
3. **Test time-based filtering** with the `since` parameter
4. **Verify performance** with large datasets

## Migration Notes

- **No database migration required** - uses existing audit data
- **Backward compatible** - existing APIs continue to work
- **Gradual improvement** - like counts will become more accurate over time as users interact with the unlike feature 