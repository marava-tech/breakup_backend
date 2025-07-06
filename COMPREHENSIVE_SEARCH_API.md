# Comprehensive Story Search API

## Overview

The Comprehensive Story Search API provides a unified search interface for stories with multiple filter options. The API returns actual `StoryResponse` objects with proper pagination, making it easy to display stories directly in the frontend.

## Key Features

- **Unified Search**: Single endpoint for all search operations
- **Multiple Filters**: Support for text, title, tags, emotions, location, user, language, and date range searches
- **Direct Story Objects**: Returns actual `StoryResponse` objects instead of search index objects
- **Pagination**: Built-in pagination support
- **Search Priority**: Intelligent search strategy based on provided filters
- **Performance Optimized**: Uses search index for fast queries

## API Endpoints

### POST `/api/v1/search/comprehensive`

Search stories using a JSON request body.

**Request Body:**
```json
{
  "searchText": "breakup story",
  "title": "my story",
  "tags": ["sad", "heartbreak"],
  "emotionType": "SADNESS",
  "minEmotionScore": 0.7,
  "location": "Mumbai",
  "state": "Maharashtra",
  "district": "Mumbai",
  "pincode": "400001",
  "name": "John",
  "userId": "user123",
  "language": "en",
  "status": "ACTIVE",
  "createdAfter": "2024-01-01T00:00:00",
  "createdBefore": "2024-12-31T23:59:59",
  "page": 0,
  "size": 10
}
```

### GET `/api/v1/search/comprehensive`

Search stories using query parameters.

**Query Parameters:**
- `searchText` (optional): Full-text search
- `title` (optional): Search by title
- `tags` (optional): List of tags to search
- `emotionType` (optional): Emotion type (e.g., "SADNESS", "ANGER", "JOY")
- `minEmotionScore` (optional): Minimum emotion score (0.0 to 1.0)
- `location` (optional): Location name
- `state` (optional): State name
- `district` (optional): District name
- `pincode` (optional): Pincode
- `name` (optional): Person name in story
- `userId` (optional): User ID who created the story
- `language` (optional): Language code (e.g., "en", "hi")
- `status` (optional): Story status (default: "ACTIVE")
- `createdAfter` (optional): ISO datetime string
- `createdBefore` (optional): ISO datetime string
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 10)

**Example:**
```
GET /api/v1/search/comprehensive?searchText=breakup&tags=sad,heartbreak&page=0&size=10
```

## Response Format

```json
{
  "stories": [
    {
      "id": "story123",
      "title": "My Breakup Story",
      "content": "This is my story...",
      "userId": "user123",
      "status": "ACTIVE",
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00",
      "audioUrl": "https://example.com/audio.mp3",
      "duration": 120,
      "viewCount": 50,
      "likeCount": 10,
      "commentCount": 5
    }
  ],
  "pagination": {
    "currentPage": 0,
    "totalPages": 5,
    "totalElements": 50,
    "pageSize": 10
  },
  "searchInfo": {
    "searchQuery": "text:breakup story",
    "appliedFilters": [
      "text_search: breakup story",
      "status: ACTIVE"
    ],
    "searchTimeMs": 150,
    "totalStories": 1000,
    "activeStories": 800
  }
}
```

## Search Priority

The API applies search filters in the following priority order:

1. **Text Search** (`searchText`) - Full-text search across all searchable content
2. **Title Search** (`title`) - Search by story title
3. **Tag Search** (`tags`) - Search by story tags
4. **Emotion Search** (`emotionType`) - Search by emotion type and score
5. **Location Search** (`location`) - Search by location
6. **Geographic Search** (`state`, `district`, `pincode`) - Search by geographic location
7. **Name Search** (`name`) - Search by person name in story
8. **User Search** (`userId`) - Search by story creator
9. **Language Search** (`language`) - Search by story language
10. **Date Range Search** (`createdAfter`, `createdBefore`) - Search by creation date
11. **Default** - All active stories

## Usage Examples

### 1. Text Search
```bash
curl -X POST http://localhost:8080/api/v1/search/comprehensive \
  -H "Content-Type: application/json" \
  -d '{
    "searchText": "breakup heartbreak",
    "page": 0,
    "size": 10
  }'
```

### 2. Tag Search
```bash
curl -X GET "http://localhost:8080/api/v1/search/comprehensive?tags=sad,heartbreak&page=0&size=10"
```

### 3. Emotion Search
```bash
curl -X POST http://localhost:8080/api/v1/search/comprehensive \
  -H "Content-Type: application/json" \
  -d '{
    "emotionType": "SADNESS",
    "minEmotionScore": 0.8,
    "page": 0,
    "size": 10
  }'
```

### 4. Location Search
```bash
curl -X GET "http://localhost:8080/api/v1/search/comprehensive?location=Mumbai&state=Maharashtra&page=0&size=10"
```

### 5. User Stories
```bash
curl -X GET "http://localhost:8080/api/v1/search/comprehensive?userId=user123&page=0&size=10"
```

### 6. Language Filter
```bash
curl -X GET "http://localhost:8080/api/v1/search/comprehensive?language=en&page=0&size=10"
```

### 7. Date Range Search
```bash
curl -X POST http://localhost:8080/api/v1/search/comprehensive \
  -H "Content-Type: application/json" \
  -d '{
    "createdAfter": "2024-01-01T00:00:00",
    "createdBefore": "2024-12-31T23:59:59",
    "page": 0,
    "size": 10
  }'
```

## Performance Considerations

1. **Search Index**: Uses optimized search index for fast queries
2. **Pagination**: Always use pagination to limit result size
3. **Filter Combination**: Combine multiple filters for more specific results
4. **Caching**: Consider caching frequently accessed search results

## Error Handling

The API returns appropriate HTTP status codes:
- `200 OK`: Successful search
- `400 Bad Request`: Invalid request parameters
- `500 Internal Server Error`: Server error

## Migration Notes

- **Removed Specific Endpoints**: Individual search endpoints (`/text`, `/tags`, `/emotion`, etc.) have been removed
- **Direct Story Objects**: Response now contains actual `StoryResponse` objects instead of `StorySearchIndex` objects
- **Unified Interface**: All search operations now go through the comprehensive search endpoint

## Architecture

```
StorySearchController
    ↓
StorySearchIndexService
    ↓
StorySearchIndexRepository (for search)
    ↓
StoryRepository (for fetching actual stories)
```

The search process:
1. Search the `StorySearchIndex` collection using optimized queries
2. Extract story IDs from search results
3. Fetch actual `Story` objects using the IDs
4. Convert `Story` objects to `StoryResponse` objects with user information
5. Apply pagination to the story response list
6. Return paginated story responses with search metadata 