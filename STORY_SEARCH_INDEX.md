# Story Search Index Implementation

This document describes the implementation of a separate search index collection for optimizing story search performance.

## Overview

The story search index is a separate MongoDB collection (`story_search_index`) that contains searchable content with references to the original story IDs. This separation provides several benefits:

- **Performance**: Faster search queries on indexed fields
- **Scalability**: Reduced load on the main stories collection
- **Flexibility**: Easy to add new search fields without affecting the main story structure
- **Optimization**: Indexed fields for efficient querying

## Architecture

### Collections

1. **`stories`** - Main story collection (simplified)
   - Core story data (title, content, audio, etc.)
   - No longer contains searchable fields

2. **`story_search_index`** - Search index collection
   - Searchable content (tags, emotions, metadata)
   - References to story IDs
   - Indexed fields for fast queries

### Data Flow

```
Story Creation → Story Index Creation → AI Processing → Index Update
     ↓              ↓                      ↓              ↓
  Main Story    Search Index         AI Results    Updated Index
  Collection    Collection           (tags, etc.)   Collection
```

## Models

### Story Model (Updated)

```java
@Document(collection = "stories")
public class Story {
    @Id
    private String id;
    private String userId;
    private String title;
    private String audioUrl;
    private String thumbnailUrl;
    private Long viewCount;
    private Long duration;
    private StoryStatus status;
    private List<Content> contents;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> rejectionReasons;
    // Removed: tags, emotions, metadata
}
```

### StorySearchIndex Model

```java
@Document(collection = "story_search_index")
public class StorySearchIndex {
    @Id
    private String id;
    @Indexed
    private String storyId;           // Reference to original story
    @Indexed
    private String title;             // Searchable title
    @Indexed
    private List<String> tags;        // Searchable tags
    private List<Emotion> emotions;   // Emotional analysis
    private StoryMetadata metadata;   // Location, names, etc.
    @Indexed
    private Story.StoryStatus status; // Filter by status
    @Indexed
    private String userId;            // Filter by user
    @Indexed
    private String language;          // Filter by language
    @Indexed
    private LocalDateTime createdAt;  // Time-based queries
    private LocalDateTime updatedAt;
    @Indexed
    private String searchText;        // Full-text search
}
```

## Services

### StorySearchIndexService

Main service for managing search index operations:

- **createOrUpdateSearchIndex()** - Create/update search index for a story
- **updateSearchIndexWithAIResults()** - Update with AI processing results
- **searchByText()** - Full-text search
- **searchByTags()** - Tag-based search
- **searchByEmotion()** - Emotion-based search
- **searchByLocation()** - Location-based search
- **complexSearch()** - Multi-criteria search

### StorySearchIndexMigrationService

Migration service for existing data:

- **migrateAllStories()** - Migrate all existing stories
- **migrateStoriesByUser()** - Migrate stories by user
- **migrateStoriesByStatus()** - Migrate stories by status
- **cleanupOrphanedIndices()** - Remove orphaned indices
- **getMigrationStats()** - Get migration statistics

## API Endpoints

### Search Endpoints

```
GET /api/v1/search/text?query={text}&page={page}&size={size}
GET /api/v1/search/title?title={title}&page={page}&size={size}
GET /api/v1/search/tags?tags={tag1,tag2,tag3}
GET /api/v1/search/emotion?emotionType={emotion}
GET /api/v1/search/location?location={location}
GET /api/v1/search/state?state={state}
GET /api/v1/search/name?name={name}
GET /api/v1/search/language?language={language}
GET /api/v1/search/complex?status={status}&searchTerm={term}&tags={tags}
GET /api/v1/search/user/{userId}
GET /api/v1/search/language/{language}/active
GET /api/v1/search/emotion/score?minScore={score}
GET /api/v1/search/date/after?date={date}
GET /api/v1/search/active
GET /api/v1/search/stats
```

## Search Capabilities

### 1. Full-Text Search
```java
// Search in title, tags, names, locations
Page<StorySearchIndex> results = searchIndexService.searchByText("breakup love", pageable);
```

### 2. Tag-Based Search
```java
// Find stories with specific tags
List<StorySearchIndex> results = searchIndexService.searchByTags(Arrays.asList("breakup", "love"));
```

### 3. Emotion-Based Search
```java
// Find stories with specific emotions
List<StorySearchIndex> results = searchIndexService.searchByEmotionType("SAD");
```

### 4. Location-Based Search
```java
// Find stories by location
List<StorySearchIndex> results = searchIndexService.searchByLocation("Mumbai");
```

### 5. Language-Based Search
```java
// Find stories by language
List<StorySearchIndex> results = searchIndexService.searchByLanguage("HINDI");
```

### 6. Complex Search
```java
// Multi-criteria search
Page<StorySearchIndex> results = searchIndexService.complexSearch(
    Story.StoryStatus.ACTIVE, "breakup", Arrays.asList("love", "memories"), pageable
);
```

## Indexes

The search index collection has the following MongoDB indexes:

```javascript
// Story ID index
db.story_search_index.createIndex({"storyId": 1})

// Title index
db.story_search_index.createIndex({"title": 1})

// Tags index
db.story_search_index.createIndex({"tags": 1})

// Status index
db.story_search_index.createIndex({"status": 1})

// User ID index
db.story_search_index.createIndex({"userId": 1})

// Language index
db.story_search_index.createIndex({"language": 1})

// Created date index
db.story_search_index.createIndex({"createdAt": 1})

// Search text index
db.story_search_index.createIndex({"searchText": 1})

// Compound indexes for common queries
db.story_search_index.createIndex({"status": 1, "language": 1})
db.story_search_index.createIndex({"status": 1, "userId": 1})
```

## Migration

### Automatic Migration

The system automatically creates search indices when:
1. New stories are created
2. Stories are processed by AI
3. Story status changes

### Manual Migration

For existing data, use the migration service:

```java
// Migrate all stories
migrationService.migrateAllStories();

// Migrate stories by user
migrationService.migrateStoriesByUser("userId");

// Migrate stories by status
migrationService.migrateStoriesByStatus(Story.StoryStatus.ACTIVE);

// Clean up orphaned indices
migrationService.cleanupOrphanedIndices();
```

## Performance Benefits

### Before (Single Collection)
- Large documents with all data
- Slower queries on non-indexed fields
- Higher memory usage
- Complex queries on nested fields

### After (Separate Collections)
- Smaller, focused documents
- Fast queries on indexed fields
- Lower memory usage per query
- Simple queries on flat structure

## Usage Examples

### 1. Search Stories by Text
```bash
curl "http://localhost:8080/api/v1/search/text?query=breakup&page=0&size=10"
```

### 2. Search Stories by Tags
```bash
curl "http://localhost:8080/api/v1/search/tags?tags=breakup,love,memories"
```

### 3. Search Stories by Emotion
```bash
curl "http://localhost:8080/api/v1/search/emotion?emotionType=SAD"
```

### 4. Search Stories by Location
```bash
curl "http://localhost:8080/api/v1/search/location?location=Mumbai"
```

### 5. Complex Search
```bash
curl "http://localhost:8080/api/v1/search/complex?status=ACTIVE&searchTerm=breakup&tags=love,memories&page=0&size=10"
```

## Monitoring

### Health Checks

```java
// Check search index health
long totalStories = storyRepository.count();
long totalIndices = searchIndexRepository.count();
double syncPercentage = (double) totalIndices / totalStories * 100;
```

### Statistics

```java
// Get search statistics
SearchStats stats = searchController.getSearchStats();
long activeStories = stats.getActiveStories();
```

## Best Practices

### 1. Index Management
- Monitor index usage
- Remove unused indexes
- Create compound indexes for common queries

### 2. Data Consistency
- Keep search index in sync with main collection
- Handle orphaned indices
- Validate data integrity

### 3. Performance Optimization
- Use pagination for large result sets
- Implement caching for frequent queries
- Monitor query performance

### 4. Error Handling
- Handle missing search indices gracefully
- Provide fallback to main collection
- Log search errors for debugging

## Future Enhancements

### 1. Advanced Search
- Fuzzy search capabilities
- Search result ranking
- Search suggestions

### 2. Analytics
- Search analytics
- Popular search terms
- Search performance metrics

### 3. Caching
- Redis caching for search results
- Search result caching
- Query result caching

### 4. Real-time Updates
- Real-time search index updates
- Event-driven index updates
- WebSocket notifications

## Troubleshooting

### Common Issues

1. **Missing Search Indices**
   - Run migration service
   - Check for orphaned indices
   - Verify data consistency

2. **Slow Search Queries**
   - Check index usage
   - Optimize query patterns
   - Add missing indexes

3. **Data Inconsistency**
   - Run cleanup operations
   - Validate data integrity
   - Re-sync indices

### Debug Commands

```bash
# Check search index collection
db.story_search_index.find().limit(5)

# Check indexes
db.story_search_index.getIndexes()

# Check collection stats
db.story_search_index.stats()

# Find orphaned indices
db.story_search_index.find({
  "storyId": {
    $nin: db.stories.distinct("_id")
  }
})
```

This implementation provides a robust, scalable solution for story search functionality with excellent performance characteristics. 