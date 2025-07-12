# Language Filtering Feature

## Overview

The Language Filtering feature automatically applies language-based filtering to story APIs based on the logged-in user's preferred language. This ensures users see content in their preferred language by default.

## Features

- ✅ **Automatic Language Filtering**: Stories are filtered by user's preferred language by default
- ✅ **Manual Override**: Users can specify a different language via query parameter
- ✅ **Fallback Support**: If no language is specified, shows all stories
- ✅ **Unauthenticated Support**: Unauthenticated users can specify language manually
- ✅ **Type-Specific Language Filtering**: Each story type maintains its specific logic while adding language filtering

## Affected APIs

### 1. General Stories API
```
GET /api/stories
```

**Language Filtering:**
- **Authenticated Users**: Uses user's preferred language by default
- **Unauthenticated Users**: No automatic filtering, can specify language manually

**Query Parameters:**
- `language` (optional): Override language filter
- `page` (default: 0): Page number
- `size` (default: 10): Page size

### 2. Stories by Type API
```
GET /api/stories/type?searchType=TRENDING
```

**Supported Types with Language Filtering:**
- `GENERAL` - General stories with language filter
- `TRENDING` - Trending stories with language filter (maintains trending score calculation)
- `FOR_YOU` - For you stories with language filter (maintains view count sorting)
- `SIMILAR` - Similar stories with language filter (maintains view count sorting)
- `LATEST` - Latest stories with language filter (maintains creation date sorting)

**Types without Language Filtering:**
- `NEAR_ME` - Location-based filtering (no language filter)
- `LANGUAGE` - Explicit language filtering (uses provided language parameter)

## Implementation Details

### Language Priority Logic

1. **Explicit Language Parameter**: If `language` query parameter is provided, use it
2. **User Preferred Language**: If no explicit language, use user's `preferredStoryLanguage`
3. **No Filtering**: If neither is available, show all stories

### Code Changes

#### StoryController Updates

1. **General Stories Endpoint:**
   ```java
   @GetMapping
   public ResponseEntity<PagedResponse<StoryResponse>> getStories(
           @RequestParam(defaultValue = "0") int page,
           @RequestParam(defaultValue = "10") int size,
           @RequestParam(required = false) String language,
           Authentication authentication)
   ```

2. **Stories by Type Endpoint:**
   ```java
   @GetMapping("/type")
   public ResponseEntity<PagedResponse<StoryResponse>> getStoriesByType(
           @RequestParam StorySearchType searchType,
           // ... other parameters
   )
   ```

### Language Filtering Logic

```java
// Get user's preferred language
User user = userService.getUserEntityByEmail(email);
String userPreferredLanguage = user.getPreferredStoryLanguage();

// Use provided language or user's preferred language
String filterLanguage = (language != null && !language.trim().isEmpty()) 
    ? language 
    : userPreferredLanguage;

// Apply language filtering if available
if (filterLanguage != null && !filterLanguage.trim().isEmpty()) {
    response = storyService.getStoriesByLanguage(filterLanguage, userId, page, size);
} else {
    response = storyService.getStories(userId, page, size);
}
```

### Type-Specific Language Filtering Methods

The implementation now includes language-filtered versions of each story type that maintain their specific logic:

1. **Trending Stories**: `getTrendingStoriesByLanguage()` - Maintains trending score calculation
2. **Latest Stories**: `getLatestStoriesByLanguage()` - Maintains creation date sorting
3. **For You Stories**: `getForYouStoriesByLanguage()` - Maintains view count sorting
4. **Similar Stories**: `getSimilarStoriesByLanguage()` - Maintains view count sorting

## Usage Examples

### 1. Get Stories with User's Preferred Language
```bash
# Authenticated user - automatically uses preferred language
curl -X GET "http://localhost:8080/api/stories" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 2. Override Language Filter
```bash
# Override with specific language
curl -X GET "http://localhost:8080/api/stories?language=te" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 3. Get Trending Stories with Language Filter
```bash
# Trending stories in user's preferred language
curl -X GET "http://localhost:8080/api/stories/type?searchType=TRENDING" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Trending stories in specific language
curl -X GET "http://localhost:8080/api/stories/type?searchType=TRENDING&language=en" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 4. Get Latest Stories with Language Filter
```bash
# Latest stories in user's preferred language
curl -X GET "http://localhost:8080/api/stories/type?searchType=LATEST" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Latest stories in specific language
curl -X GET "http://localhost:8080/api/stories/type?searchType=LATEST&language=hi" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 5. Unauthenticated User with Language
```bash
# Unauthenticated user specifying language
curl -X GET "http://localhost:8080/api/stories?language=hi"
```

## Response Format

The response format remains the same, but stories are filtered by language:

```json
{
  "content": [
    {
      "id": "story123",
      "title": "My Breakup Story",
      "language": "en",
      "userId": "user123",
      "viewCount": 150,
      "contents": [...],
      "tags": [...],
      "emotions": [...],
      "createdAt": "2024-01-15T10:30:00Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 25,
  "totalPages": 3
}
```

## Database Queries

The language filtering uses the following repository methods:

```java
// StoryRepository
Page<Story> findByLanguageAndStatus(String language, Story.StoryStatus status, Pageable pageable);
Page<Story> findByLanguageAndStatusOrderByCreatedAtDesc(String language, Story.StoryStatus status, Pageable pageable);
Page<Story> findByLanguageAndStatusOrderByViewCountDesc(String language, Story.StoryStatus status, Pageable pageable);
```

## Configuration

### User Language Preference

Users can set their preferred language in their profile:

```json
{
  "preferredStoryLanguage": "en"
}
```

### Supported Languages

The system supports multiple languages:
- `en` - English
- `te` - Telugu
- `hi` - Hindi
- `ta` - Tamil
- `kn` - Kannada
- `ml` - Malayalam
- And more...

## Error Handling

- **Invalid Language**: If invalid language is provided, falls back to user's preferred language
- **No Preferred Language**: If user has no preferred language, shows all stories
- **Unauthenticated Users**: No automatic filtering, manual language parameter required

## Performance Considerations

- **Indexing**: Ensure `language` field is indexed in the database
- **Caching**: Consider caching language-filtered results
- **Pagination**: Language filtering works with existing pagination

## Future Enhancements

1. **Multi-language Support**: Allow users to specify multiple preferred languages
2. **Language Detection**: Automatically detect story language from content
3. **Language Preferences**: Allow users to set preferences for different story types
4. **Regional Language Support**: Add support for regional language variants

## Technical Implementation

### New Service Methods Added

1. `getTrendingStoriesByLanguage()` - Language-filtered trending stories with score calculation
2. `getLatestStoriesByLanguage()` - Language-filtered latest stories with date sorting
3. `getForYouStoriesByLanguage()` - Language-filtered for you stories with view count sorting
4. `getSimilarStoriesByLanguage()` - Language-filtered similar stories with view count sorting

### New Repository Methods Added

1. `findByLanguageAndStatusOrderByCreatedAtDesc()` - For latest stories
2. `findByLanguageAndStatusOrderByViewCountDesc()` - For for you and similar stories

### Benefits

- **Consistent User Experience**: Users see content in their preferred language across all story types
- **Maintained Logic**: Each story type preserves its specific sorting and calculation logic
- **Performance**: Efficient database queries with proper indexing
- **Flexibility**: Manual override capability for specific language requests 