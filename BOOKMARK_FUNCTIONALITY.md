# Bookmark Functionality Documentation

## Overview

The Breakup Stories API now includes a comprehensive bookmark system that allows users to save and organize their favorite stories. Users can bookmark any number of stories and easily access them later.

## 🎯 Features

### Core Functionality
- **Create Bookmarks**: Users can bookmark any story
- **Remove Bookmarks**: Users can remove bookmarks by story ID or bookmark ID
- **View Bookmarks**: Users can view all their bookmarks with pagination
- **Bookmarked Stories**: Users can view full story details of bookmarked stories
- **Bookmark Status**: Check if a story is bookmarked by the current user
- **Bookmark Count**: Get total number of bookmarks for a user
- **Audit Trail**: All bookmark actions are audited for analytics

### User Experience
- **Unlimited Bookmarks**: Users can bookmark any number of stories
- **Quick Access**: Easy access to bookmarked stories
- **Story Details**: Full story information in bookmark lists
- **Real-time Status**: Bookmark status included in story responses

## 📊 Data Model

### Bookmark Entity
```java
public class Bookmark {
    private String id;
    private String storyId;
    private String userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### Story Response Enhancement
```java
public class StoryResponse {
    // ... existing fields ...
    private final boolean isBookmarkedByMe = false;
}
```

## 🔧 Implementation

### Services

#### BookmarkService
The main service for bookmark operations:

```java
@Service
public class BookmarkService {
    public BookmarkResponse createBookmark(String userId, BookmarkRequest request);
    public PagedResponse<BookmarkResponse> getBookmarksByUser(String userId, int page, int size);
    public PagedResponse<StoryResponse> getBookmarkedStoriesWithDetails(String userId, int page, int size);
    public boolean isBookmarked(String userId, String storyId);
    public long getBookmarkCountByUser(String userId);
    public void deleteBookmark(String bookmarkId, String userId);
    public void deleteBookmarkByUserAndStory(String userId, String storyId);
}
```

#### StoryService Enhancement
Updated to include bookmark status in story responses:

```java
public StoryResponse getStoryById(String storyId, String currentUserId) {
    // ... existing logic ...
    boolean bookmarkedByMe = bookmarkService.isBookmarked(currentUserId, storyId);
    return StoryResponse.fromStory(story, likedByMe, bookmarkedByMe, likeCount, commentCount);
}
```

### Repository
```java
@Repository
public interface BookmarkRepository extends MongoRepository<Bookmark, String> {
    Page<Bookmark> findByUserId(String userId, Pageable pageable);
    Optional<Bookmark> findByStoryIdAndUserId(String storyId, String userId);
    boolean existsByUserIdAndStoryId(String userId, String storyId);
    void deleteByUserIdAndStoryId(String userId, String storyId);
    long countByUserId(String userId);
    long countByStoryId(String storyId);
}
```

## 📡 API Endpoints

### Bookmark Management

#### Create Bookmark
```http
POST /api/bookmarks
Authorization: Bearer <user-token>
Content-Type: application/json

{
  "storyId": "story123"
}
```

**Response:**
```json
{
  "id": "bookmark456",
  "userId": "user789",
  "storyId": "story123",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

#### Get User Bookmarks
```http
GET /api/bookmarks?page=0&size=10
Authorization: Bearer <user-token>
```

**Response:**
```json
{
  "content": [
    {
      "id": "bookmark456",
      "userId": "user789",
      "storyId": "story123",
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

#### Get Bookmarked Stories (with full details)
```http
GET /api/bookmarks/stories?page=0&size=10
Authorization: Bearer <user-token>
```

**Response:**
```json
{
  "content": [
    {
      "id": "story123",
      "userId": "user456",
      "title": "My Breakup Story",
      "audioUrl": "https://example.com/audio.mp3",
      "shareLink": "https://example.com/share/story123",
      "audioLanguage": "ENGLISH",
      "viewCount": 150,
      "likeCount": 25,
      "commentCount": 8,
      "status": "PUBLISHED",
      "contents": [...],
      "tags": ["breakup", "healing"],
      "emotions": [...],
      "keywords": [...],
      "isLikedByMe": false,
      "isBookmarkedByMe": true,
      "createdAt": "2024-01-10T15:30:00",
      "updatedAt": "2024-01-10T15:30:00"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

#### Check Bookmark Status
```http
GET /api/bookmarks/check/{storyId}
Authorization: Bearer <user-token>
```

**Response:**
```json
true
```

#### Get Bookmark Count
```http
GET /api/bookmarks/count
Authorization: Bearer <user-token>
```

**Response:**
```json
15
```

#### Delete Bookmark by ID
```http
DELETE /api/bookmarks/{bookmarkId}
Authorization: Bearer <user-token>
```

**Response:**
```http
204 No Content
```

#### Remove Bookmark by Story
```http
DELETE /api/bookmarks/story/{storyId}
Authorization: Bearer <user-token>
```

**Response:**
```http
204 No Content
```

#### Get Bookmark by ID
```http
GET /api/bookmarks/{bookmarkId}
Authorization: Bearer <user-token>
```

**Response:**
```json
{
  "id": "bookmark456",
  "userId": "user789",
  "storyId": "story123",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

### Enhanced Story Endpoints

#### Get Story with Bookmark Status
```http
GET /api/stories/{storyId}
Authorization: Bearer <user-token>
```

**Response:**
```json
{
  "id": "story123",
  "userId": "user456",
  "title": "My Breakup Story",
  "audioUrl": "https://example.com/audio.mp3",
  "shareLink": "https://example.com/share/story123",
  "audioLanguage": "ENGLISH",
  "viewCount": 150,
  "likeCount": 25,
  "commentCount": 8,
  "status": "PUBLISHED",
  "contents": [...],
  "tags": ["breakup", "healing"],
  "emotions": [...],
  "keywords": [...],
  "isLikedByMe": false,
  "isBookmarkedByMe": true,
  "createdAt": "2024-01-10T15:30:00",
  "updatedAt": "2024-01-10T15:30:00"
}
```

## 🔍 Database Queries

### MongoDB Queries

#### Get all bookmarks for a user
```javascript
db.bookmarks.find({ userId: "user789" })
```

#### Get bookmark count for a user
```javascript
db.bookmarks.countDocuments({ userId: "user789" })
```

#### Check if story is bookmarked by user
```javascript
db.bookmarks.findOne({ 
  userId: "user789", 
  storyId: "story123" 
})
```

#### Get most bookmarked stories
```javascript
db.bookmarks.aggregate([
  { $group: { _id: "$storyId", bookmarkCount: { $sum: 1 } } },
  { $sort: { bookmarkCount: -1 } },
  { $limit: 10 }
])
```

#### Get user's bookmarks with story details
```javascript
db.bookmarks.aggregate([
  { $match: { userId: "user789" } },
  { $lookup: { 
    from: "stories", 
    localField: "storyId", 
    foreignField: "_id", 
    as: "story" 
  }},
  { $unwind: "$story" },
  { $sort: { createdAt: -1 } }
])
```

## 🛡️ Security & Validation

### Access Control
- **Authentication Required**: All bookmark endpoints require user authentication
- **Ownership Validation**: Users can only access their own bookmarks
- **Story Validation**: Bookmarks can only be created for existing stories

### Business Rules
- **Unique Bookmarks**: Users cannot bookmark the same story twice
- **Story Existence**: Bookmarks can only be created for valid stories
- **Ownership**: Users can only delete their own bookmarks

### Error Handling
```json
{
  "error": "Bookmark already exists for this story",
  "status": 400
}
```

```json
{
  "error": "Story not found with ID: story123",
  "status": 404
}
```

```json
{
  "error": "You can only delete your own bookmarks",
  "status": 403
}
```

## 📊 Analytics & Auditing

### Audit Events
All bookmark actions are automatically audited:

- **Bookmark Creation**: `logBookmarkCreate()`
- **Bookmark Deletion**: `logBookmarkDelete()`

### Audit Data Captured
- User ID
- Story ID
- Action type (CREATE/DELETE)
- Client information (IP, User Agent, Session ID)
- Timestamp

### Analytics Queries
```javascript
// Get bookmark analytics for a user
db.audits.aggregate([
  { $match: { 
    userId: "user789", 
    entityType: "BOOKMARK" 
  }},
  { $group: { 
    _id: "$actionType", 
    count: { $sum: 1 } 
  }}
])
```

## 🚀 Frontend Integration

### JavaScript Examples

#### Create Bookmark
```javascript
async function createBookmark(storyId) {
  const response = await fetch('/api/bookmarks', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({ storyId })
  });
  
  if (response.ok) {
    const bookmark = await response.json();
    console.log('Bookmark created:', bookmark);
  }
}
```

#### Get Bookmarked Stories
```javascript
async function getBookmarkedStories(page = 0, size = 10) {
  const response = await fetch(`/api/bookmarks/stories?page=${page}&size=${size}`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  if (response.ok) {
    const stories = await response.json();
    return stories.content;
  }
}
```

#### Check Bookmark Status
```javascript
async function isBookmarked(storyId) {
  const response = await fetch(`/api/bookmarks/check/${storyId}`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  if (response.ok) {
    return await response.json();
  }
  return false;
}
```

#### Remove Bookmark
```javascript
async function removeBookmark(storyId) {
  const response = await fetch(`/api/bookmarks/story/${storyId}`, {
    method: 'DELETE',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  if (response.ok) {
    console.log('Bookmark removed successfully');
  }
}
```

### React Component Example
```jsx
import React, { useState, useEffect } from 'react';

const BookmarkButton = ({ storyId, initialBookmarked = false }) => {
  const [isBookmarked, setIsBookmarked] = useState(initialBookmarked);
  const [loading, setLoading] = useState(false);

  const toggleBookmark = async () => {
    setLoading(true);
    try {
      if (isBookmarked) {
        await removeBookmark(storyId);
        setIsBookmarked(false);
      } else {
        await createBookmark(storyId);
        setIsBookmarked(true);
      }
    } catch (error) {
      console.error('Error toggling bookmark:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <button 
      onClick={toggleBookmark} 
      disabled={loading}
      className={`bookmark-btn ${isBookmarked ? 'bookmarked' : ''}`}
    >
      {isBookmarked ? '★' : '☆'} Bookmark
    </button>
  );
};
```

## 📋 Performance Considerations

### Database Indexing
```javascript
// Create indexes for optimal performance
db.bookmarks.createIndex({ userId: 1, createdAt: -1 });
db.bookmarks.createIndex({ storyId: 1, userId: 1 });
db.bookmarks.createIndex({ userId: 1, storyId: 1 });
```

### Caching Strategy
- **Bookmark Status**: Cache bookmark status for frequently accessed stories
- **Bookmark Lists**: Cache paginated bookmark lists with TTL
- **Story Details**: Cache story details for bookmarked stories

### Pagination
- Default page size: 10 items
- Maximum page size: 100 items
- Efficient cursor-based pagination for large datasets

## 🔄 Future Enhancements

### Planned Features
1. **Bookmark Folders**: Organize bookmarks into categories
2. **Bookmark Notes**: Add personal notes to bookmarks
3. **Bookmark Sharing**: Share bookmark collections with other users
4. **Bookmark Export**: Export bookmarks to external formats
5. **Bookmark Sync**: Cross-device bookmark synchronization

### Performance Optimizations
1. **Bulk Operations**: Batch bookmark operations for efficiency
2. **Real-time Updates**: WebSocket updates for bookmark status changes
3. **Advanced Filtering**: Filter bookmarks by story attributes
4. **Search Integration**: Search within bookmarked stories

---

This bookmark system provides a comprehensive solution for users to save and organize their favorite stories while maintaining performance, security, and scalability. 