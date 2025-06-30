# Notification System Update

## Overview

The notification system has been updated to provide a more structured and user-friendly approach. Instead of returning individual counts, the system now returns a list of notification items with text messages and deeplink types. The system now also includes story matching notifications.

## New Structure

### DeeplinkType Enum
```java
public enum DeeplinkType {
    GENERAL,    // General navigation (e.g., my stories, profile)
    STORY       // Story-specific navigation (e.g., specific story, story matches)
}
```

### NotificationItem
Each notification item contains:
- **text**: Human-readable message describing the notification
- **deeplinkType**: Type of navigation (GENERAL or STORY)
- **id**: Identifier for the specific navigation target

### NotificationResponse
The response now includes:
- **userId**: The user ID
- **notifications**: List of NotificationItem objects
- **lastNotificationView**: Timestamp of last notification view
- **notify**: Boolean indicating if there are any notifications (true if notifications list is non-empty)

## API Response Format

### Example Response with Story Matches
```json
{
  "userId": "user123",
  "notifications": [
    {
      "text": "You got 5 views, 3 likes and 2 comments",
      "deeplinkType": "GENERAL",
      "id": "my-stories"
    },
    {
      "text": "Your story matched 85% with another story. Want to view?",
      "deeplinkType": "STORY",
      "id": "story456"
    },
    {
      "text": "Your story matched 92% with another story. Want to view?",
      "deeplinkType": "STORY",
      "id": "story789"
    }
  ],
  "lastNotificationView": 1703123456789,
  "notify": true
}
```

### Empty Response (No Notifications)
```json
{
  "userId": "user123",
  "notifications": [],
  "lastNotificationView": 1703123456789,
  "notify": false
}
```

## Text Message Generation

The system generates human-readable messages based on the counts:

### Engagement Notifications
- **Single Type**: "You got 5 views"
- **Multiple Types**: "You got 5 views, 3 likes and 2 comments"

### Story Match Notifications
- **Format**: "Your story matched X% with another story. Want to view?"
- **Examples**:
  - "Your story matched 85% with another story. Want to view?"
  - "Your story matched 92% with another story. Want to view?"

### Grammar Rules
- Singular/plural forms are handled correctly
- Multiple items are joined with "and" for 2 items, commas and "and" for 3+ items
- Zero counts are ignored (not included in the message)

## Deeplink Structure

### DeeplinkType.GENERAL
- **id**: "my-stories" - Navigate to user's stories
- **id**: "profile" - Navigate to user profile
- **id**: "settings" - Navigate to settings

### DeeplinkType.STORY
- **id**: "{storyId}" - Navigate to specific story
- **id**: "{storyId}" - Navigate to story match (for story match notifications)

### Frontend Navigation Logic
The frontend should handle navigation based on deeplinkType and id:

```javascript
const handleNotificationClick = (notification) => {
  switch (notification.deeplinkType) {
    case 'GENERAL':
      switch (notification.id) {
        case 'my-stories':
          navigate('/stories/my-stories');
          break;
        case 'profile':
          navigate('/profile');
          break;
        case 'settings':
          navigate('/settings');
          break;
        default:
          console.log('Unknown general navigation:', notification.id);
      }
      break;
      
    case 'STORY':
      navigate(`/stories/${notification.id}`);
      break;
      
    default:
      console.log('Unknown deeplink type:', notification.deeplinkType);
  }
};
```

## Story Matching Implementation

### Audit Data Structure
Story matches are stored in the audit collection with:
- **entityType**: `STORY`
- **actionType**: `MATCH`
- **metadata**: Contains `percentage` field with match percentage

### Example Audit Record
```json
{
  "userId": "user123",
  "entityType": "STORY",
  "actionType": "MATCH",
  "entityId": "story456",
  "metadata": {
    "percentage": "85"
  },
  "createdAt": 1703123456789
}
```

### Database Query
The system queries for story matches using:
```java
// Find story matches for user since last notification view
List<Audit> matches = auditRepository.findStoryMatchesByUserIdAndCreatedAtAfter(userId, since);
```

## API Endpoint

### Get Notifications
```http
GET /api/notifications/getnotifications
Authorization: Bearer <token>
```

### Response Headers
- `Content-Type: application/json`

## Frontend Integration

### React/JavaScript Example
```javascript
const getNotifications = async () => {
  const response = await fetch('/api/notifications/getnotifications', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  const data = await response.json();
  
  if (data.notify) {
    // Show notification badge/indicator
    showNotificationBadge();
    
    // Display notifications
    data.notifications.forEach(notification => {
      showNotification(notification.text, notification.deeplinkType, notification.id);
    });
  }
};

const showNotification = (text, deeplinkType, id) => {
  // Display notification to user
  // When clicked, navigate using deeplinkType and id
  const handleClick = () => {
    handleNotificationClick({ deeplinkType, id });
  };
};

const handleNotificationClick = (notification) => {
  switch (notification.deeplinkType) {
    case 'GENERAL':
      switch (notification.id) {
        case 'my-stories':
          navigate('/stories/my-stories');
          break;
        case 'profile':
          navigate('/profile');
          break;
        case 'settings':
          navigate('/settings');
          break;
        default:
          console.log('Unknown general navigation:', notification.id);
      }
      break;
      
    case 'STORY':
      navigate(`/stories/${notification.id}`);
      break;
      
    default:
      console.log('Unknown deeplink type:', notification.deeplinkType);
  }
};
```

### iOS Navigation Handling
```swift
func handleNotificationClick(deeplinkType: String, id: String) {
    switch deeplinkType {
    case "GENERAL":
        switch id {
        case "my-stories":
            navigateToMyStories()
        case "profile":
            navigateToProfile()
        case "settings":
            navigateToSettings()
        default:
            print("Unknown general navigation: \(id)")
        }
        
    case "STORY":
        navigateToStory(id)
        
    default:
        print("Unknown deeplink type: \(deeplinkType)")
    }
}
```

### Android Navigation Handling
```kotlin
private fun handleNotificationClick(deeplinkType: String, id: String) {
    when (deeplinkType) {
        "GENERAL" -> {
            when (id) {
                "my-stories" -> navigateToMyStories()
                "profile" -> navigateToProfile()
                "settings" -> navigateToSettings()
                else -> Log.d("Navigation", "Unknown general navigation: $id")
            }
        }
        "STORY" -> navigateToStory(id)
        else -> Log.d("Navigation", "Unknown deeplink type: $deeplinkType")
    }
}
```

## Benefits of New Structure

1. **Better UX**: Human-readable messages instead of raw numbers
2. **Flexible Navigation**: Frontend has full control over navigation logic
3. **Extensible**: Easy to add new deeplink types and navigation targets
4. **Consistent**: Standardized format for all notifications
5. **Mobile-Friendly**: Works seamlessly with mobile apps
6. **Story Matching**: Users get notified about similar stories with match percentages
7. **Type Safety**: Enum-based deeplink types prevent invalid navigation

## Future Enhancements

1. **Additional Deeplink Types**: Add more types like COMMENT, USER, etc.
2. **Rich Notifications**: Include images, user avatars, etc.
3. **Notification Categories**: Group notifications by type
4. **Push Notifications**: Extend to push notification system
5. **Notification History**: Store and retrieve notification history
6. **Read/Unread Status**: Track which notifications have been read
7. **Match Details**: Include more details about matched stories
8. **Match Thresholds**: Only show matches above certain percentage thresholds
9. **Custom Navigation**: Allow custom navigation based on notification context
10. **Analytics**: Track notification engagement and navigation patterns

## Migration Notes

### Breaking Changes
- `totalNewLikes`, `totalNewViews`, `totalNewComments` fields removed
- New `notifications` array field added
- `notify` field now based on whether notifications list is empty
- `deeplink` field replaced with `deeplinkType` and `id` fields

### Backward Compatibility
- API endpoint remains the same
- Authentication requirements unchanged
- Audit logging continues to work as before

### Frontend Updates Required
- Update response parsing to handle new structure
- Implement deeplinkType and id-based navigation
- Update UI to display notification items instead of counts
- Handle story match notifications with percentage display
- Replace direct deeplink handling with deeplinkType/id logic 