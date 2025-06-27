# Hybrid Timestamp Approach

## Overview
Implemented a hybrid timestamp approach where:
- **Audit-related functionality** uses epoch timestamps (milliseconds) for consistency and performance
- **User and Story models** use LocalDateTime for traditional date/time handling
- **Notification system** bridges between the two formats using TimestampUtil

## Current Implementation

### 1. Epoch Timestamps (Audit System)
**Used for**: Audit model, AuditResponse, NotificationResponse, ErrorResponse

#### Audit Model
- **File**: `src/main/java/com/breakupstories/model/Audit.java`
- **Timestamps**: `createdAt` and `updatedAt` use `Long` (epoch milliseconds)

#### AuditResponse DTO
- **File**: `src/main/java/com/breakupstories/dto/AuditResponse.java`
- **Timestamps**: `createdAt` and `updatedAt` use `Long` (epoch milliseconds)

#### NotificationResponse DTO
- **File**: `src/main/java/com/breakupstories/dto/NotificationResponse.java`
- **Timestamps**: 
  - `lastNotificationView` uses `Long` (epoch milliseconds)
  - `NotificationItem.lastActivity` uses `Long` (epoch milliseconds)

### 2. LocalDateTime (User & Story System)
**Used for**: User model, Story model, UserResponse, StoryResponse

#### User Model
- **File**: `src/main/java/com/breakupstories/model/User.java`
- **Timestamps**: `createdAt` and `updatedAt` use `LocalDateTime`

#### Story Model
- **File**: `src/main/java/com/breakupstories/model/Story.java`
- **Timestamps**: `createdAt` and `updatedAt` use `LocalDateTime`

#### UserResponse DTO
- **File**: `src/main/java/com/breakupstories/dto/UserResponse.java`
- **Timestamps**: `createdAt` and `updatedAt` use `LocalDateTime`

#### StoryResponse DTO
- **File**: `src/main/java/com/breakupstories/dto/StoryResponse.java`
- **Timestamps**: `createdAt` and `updatedAt` use `LocalDateTime`

### 3. Service Layer
**AuditService**: Uses epoch timestamps throughout
**NotificationService**: Bridges between LocalDateTime (User/Story) and epoch timestamps (Audit)

## Benefits of This Approach

### 1. **Audit Performance**
- Epoch timestamps provide better performance for audit queries
- Simpler comparison operations for like/unlike logic
- Optimized MongoDB queries

### 2. **User/Story Compatibility**
- LocalDateTime maintains traditional date/time handling
- Better readability for developers
- Standard Spring Boot conventions

### 3. **Flexibility**
- Each system uses the most appropriate timestamp format
- Easy to convert between formats when needed
- Maintains backward compatibility

## API Response Examples

### User/Story (LocalDateTime)
```json
{
  "id": "123",
  "name": "John Doe",
  "createdAt": "2025-06-28T02:15:30.123",
  "updatedAt": "2025-06-28T02:15:30.123"
}
```

### Audit/Notification (Epoch Milliseconds)
```json
{
  "id": "456",
  "userId": "123",
  "actionType": "LIKE",
  "createdAt": 1738005330123,
  "updatedAt": 1738005330123
}
```

## Conversion Utilities

### TimestampUtil Class
```java
// Convert LocalDateTime to epoch milliseconds
Long epochMillis = TimestampUtil.toEpochMillis(localDateTime);

// Convert epoch milliseconds to LocalDateTime
LocalDateTime localDateTime = TimestampUtil.fromEpochMillis(epochMillis);

// Get current epoch milliseconds
Long now = TimestampUtil.currentEpochMillis();
```

## Frontend Integration

### For User/Story Data (LocalDateTime)
```javascript
// Parse ISO string directly
const userDate = new Date("2025-06-28T02:15:30.123");
const formatted = userDate.toLocaleString();
```

### For Audit/Notification Data (Epoch)
```javascript
// Convert epoch milliseconds to Date
const auditDate = new Date(1738005330123);
const formatted = auditDate.toLocaleString();
```

## Migration Status

### ✅ Completed
- Audit model and DTOs (epoch timestamps)
- Notification system (epoch timestamps)
- Error responses (epoch timestamps)
- User model and DTOs (LocalDateTime)
- Story model and DTOs (LocalDateTime)

### 🔄 Remaining Models
The following models still use LocalDateTime (as intended):
- Comment
- Like
- Feedback
- Bookmark
- DefaultConfig
- Emotion
- Content
- StoryMetadata

## Best Practices

### 1. **When to Use Epoch Timestamps**
- Audit logging and tracking
- Performance-critical timestamp operations
- Cross-platform timestamp handling
- Notification systems

### 2. **When to Use LocalDateTime**
- User-facing data models
- Traditional date/time operations
- Spring Boot conventions
- Readable timestamp storage

### 3. **Conversion Guidelines**
- Use TimestampUtil for conversions
- Keep audit data in epoch format
- Keep user/story data in LocalDateTime format
- Convert only when necessary for cross-system operations

## Testing Considerations

1. **Test timestamp conversions** between formats
2. **Verify audit queries** work with epoch timestamps
3. **Ensure user/story operations** work with LocalDateTime
4. **Test notification system** bridging between formats
5. **Validate frontend handling** of both timestamp formats 