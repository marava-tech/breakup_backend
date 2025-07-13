# Config Search API

## Overview

The Config Search API provides functionality to search and filter default configuration entries by key. This allows administrators to find specific configurations quickly using partial key matching with case-insensitive search.

## API Endpoint

### GET /api/configs/search

**Description**: Search configs by key containing search term

**Authentication**: Required (Admin role)

**Parameters**:
- `searchTerm` (optional): The search term to look for in config keys. If not provided, returns all configs
- `activeOnly` (optional): Whether to return only active configs (default: false)
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 10)

## Implementation Details

### Repository Methods

Added to `DefaultConfigRepository`:

```java
// Search methods
List<DefaultConfig> findByKeyContainingIgnoreCase(String key);
List<DefaultConfig> findByKeyContainingIgnoreCaseAndActiveTrue(String key);
```

### Service Methods

Added to `DefaultConfigService`:

```java
/**
 * Search configs by key containing the search term (case-insensitive)
 *
 * @param searchTerm The search term to look for in config keys
 * @param activeOnly Whether to return only active configs (default: false)
 * @return List of matching config responses
 */
public List<DefaultConfigResponse> searchByKey(String searchTerm, boolean activeOnly) {
    try {
        List<DefaultConfig> configs;
        
        if (activeOnly) {
            configs = defaultConfigRepository.findByKeyContainingIgnoreCaseAndActiveTrue(searchTerm);
        } else {
            configs = defaultConfigRepository.findByKeyContainingIgnoreCase(searchTerm);
        }
        
        return configs.stream()
                .map(DefaultConfigResponse::fromEntity)
                .collect(Collectors.toList());
                
    } catch (Exception e) {
        log.error("Error searching configs by key: {}", searchTerm, e);
        return List.of();
    }
}

/**
 * Search configs by key containing the search term with pagination (case-insensitive)
 *
 * @param searchTerm The search term to look for in config keys (optional)
 * @param activeOnly Whether to return only active configs (default: false)
 * @param page Page number (default: 0)
 * @param size Page size (default: 10)
 * @return Paged response of matching config responses
 */
public PagedResponse<DefaultConfigResponse> searchByKeyWithPagination(String searchTerm, boolean activeOnly, int page, int size) {
    try {
        Page<DefaultConfig> configPage;
        Pageable pageable = PageRequest.of(page, size);
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            // If no search term, return all configs with pagination
            configPage = defaultConfigRepository.findAll(pageable);
        } else if (activeOnly) {
            configPage = defaultConfigRepository.findByKeyContainingIgnoreCaseAndActiveTrue(searchTerm, pageable);
        } else {
            configPage = defaultConfigRepository.findByKeyContainingIgnoreCase(searchTerm, pageable);
        }
        
        List<DefaultConfigResponse> configs = configPage.getContent().stream()
                .map(DefaultConfigResponse::fromEntity)
                .collect(Collectors.toList());
        
        return PagedResponse.of(configs, page, size, configPage.getTotalElements());
                
    } catch (Exception e) {
        log.error("Error searching configs by key with pagination: {}", searchTerm, e);
        return PagedResponse.of(List.of(), page, size, 0);
    }
}
```

### Controller Endpoint

```java
@GetMapping("/search")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Operation(summary = "Search configs by key containing search term with pagination", 
           description = "Search for configs where the key contains the provided search term (case-insensitive) with pagination support")
public ResponseEntity<Map<String, Object>> searchByKey(
        @RequestParam(required = false) String searchTerm,
        @RequestParam(defaultValue = "false") boolean activeOnly,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
    
    try {
        PagedResponse<DefaultConfigResponse> results = defaultConfigService.searchByKeyWithPagination(searchTerm, activeOnly, page, size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", results.getContent());
        response.put("pagination", Map.of(
            "page", results.getPage(),
            "size", results.getSize(),
            "totalElements", results.getTotalElements(),
            "totalPages", results.getTotalPages(),
            "last", results.isLast()
        ));
        response.put("searchTerm", searchTerm);
        response.put("activeOnly", activeOnly);
        
        return ResponseEntity.ok(response);
        
    } catch (Exception e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", e.getMessage());
        errorResponse.put("message", "Failed to search configs");
        errorResponse.put("searchTerm", searchTerm);
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
}
```

## Usage Examples

### Basic Search

```bash
curl -X GET "http://localhost:8080/api/configs/search?searchTerm=quote" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

### Search with Pagination

```bash
curl -X GET "http://localhost:8080/api/configs/search?searchTerm=quote&page=0&size=5" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

### Search Only Active Configs

```bash
curl -X GET "http://localhost:8080/api/configs/search?searchTerm=quote&activeOnly=true" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

### Get All Configs (No Search Term)

```bash
curl -X GET "http://localhost:8080/api/configs/search?page=0&size=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

### Search for Language Configs

```bash
curl -X GET "http://localhost:8080/api/configs/search?searchTerm=language" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

### Search for Default Configs

```bash
curl -X GET "http://localhost:8080/api/configs/search?searchTerm=default" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

## Response Examples

### Successful Search Response with Pagination

```json
{
  "success": true,
  "data": [
    {
      "id": "config123",
      "key": "quote_audio_1",
      "value": "https://example.com/audio1.mp3",
      "description": "Quote audio file 1",
      "active": true,
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:35:00"
    },
    {
      "id": "config124",
      "key": "quote_image_1",
      "value": "https://example.com/image1.jpg",
      "description": "Quote image file 1",
      "active": true,
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:35:00"
    },
    {
      "id": "config125",
      "key": "quote_text_1",
      "value": "Life is what happens when you're busy making other plans.",
      "description": "Quote text 1",
      "active": true,
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:35:00"
    }
  ],
  "pagination": {
    "page": 0,
    "size": 10,
    "totalElements": 15,
    "totalPages": 2,
    "last": false
  },
  "searchTerm": "quote",
  "activeOnly": false
}
```

### No Results Found

```json
{
  "success": true,
  "data": [],
  "pagination": {
    "page": 0,
    "size": 10,
    "totalElements": 0,
    "totalPages": 0,
    "last": true
  },
  "searchTerm": "nonexistent",
  "activeOnly": false
}
```

### Error Response

```json
{
  "success": false,
  "error": "Invalid search term",
  "message": "Failed to search configs",
  "searchTerm": ""
}
```

## Search Features

### Case-Insensitive Search

The search is case-insensitive, so searching for:
- `"quote"` will match `"quote_audio_1"`, `"Quote_Image_1"`, `"QUOTE_TEXT_1"`
- `"QUOTE"` will match the same results as `"quote"`
- `"Quote"` will match the same results as `"quote"`

### Partial Matching

The search uses "contains" logic, so:
- `"quote"` will match `"quote_audio_1"`, `"quote_image_1"`, `"quote_text_1"`
- `"audio"` will match `"quote_audio_1"`, `"default_audio_url"`
- `"default"` will match `"default_thumbnail_url"`, `"default_story_image_1"`

### Active Only Filter

When `activeOnly=true`:
- Only returns configs where `active: true`
- Useful for finding only currently active configurations
- Excludes inactive/deleted configurations

## Common Search Patterns

### Quote Configurations

```bash
# Search for all quote-related configs
GET /api/configs/search?searchTerm=quote

# Search for quote audio configs only
GET /api/configs/search?searchTerm=quote_audio

# Search for quote image configs only
GET /api/configs/search?searchTerm=quote_image

# Search for quote text configs only
GET /api/configs/search?searchTerm=quote_text
```

### Default Configurations

```bash
# Search for all default configs
GET /api/configs/search?searchTerm=default

# Search for default story images
GET /api/configs/search?searchTerm=default_story_image

# Search for default thumbnail
GET /api/configs/search?searchTerm=default_thumbnail
```

### Language Configurations

```bash
# Search for language configs
GET /api/configs/search?searchTerm=language

# Search for specific language
GET /api/configs/search?searchTerm=languages
```

### Payment Configurations

```bash
# Search for payment-related configs
GET /api/configs/search?searchTerm=payment

# Search for coin conversion configs
GET /api/configs/search?searchTerm=coin

# Search for rupee conversion configs
GET /api/configs/search?searchTerm=rupee
```

## Security Considerations

- **Authentication Required**: Only authenticated users can access the API
- **Admin Role Required**: Only users with `ROLE_ADMIN` authority can search configs
- **No Sensitive Data Exposure**: The API only returns configuration data, not sensitive user information

## Performance Considerations

- **Database Indexing**: Consider adding indexes on the `key` field for better search performance
- **Case-Insensitive Search**: Uses MongoDB's case-insensitive search capabilities
- **Result Limiting**: No pagination implemented - returns all matching results
- **Error Handling**: Graceful error handling with fallback to empty results

## Error Handling

### Missing Search Term

```bash
curl -X GET "http://localhost:8080/api/configs/search" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

Response:
```json
{
  "success": false,
  "error": "Required request parameter 'searchTerm' for method parameter type String is not present",
  "message": "Failed to search configs",
  "searchTerm": null
}
```

### Unauthorized Access

```bash
curl -X GET "http://localhost:8080/api/configs/search?searchTerm=test"
```

Response:
```json
{
  "success": false,
  "error": "Access Denied",
  "message": "Authentication required"
}
```

## Testing

### Unit Tests

```java
@Test
public void testSearchByKey() {
    // Given: Search term
    String searchTerm = "quote";
    
    // When: Searching configs
    List<DefaultConfigResponse> results = defaultConfigService.searchByKey(searchTerm, false);
    
    // Then: Should return matching configs
    assertThat(results).isNotEmpty();
    results.forEach(config -> 
        assertThat(config.getKey().toLowerCase()).contains(searchTerm.toLowerCase())
    );
}

@Test
public void testSearchByKeyActiveOnly() {
    // Given: Search term and active only
    String searchTerm = "quote";
    boolean activeOnly = true;
    
    // When: Searching active configs
    List<DefaultConfigResponse> results = defaultConfigService.searchByKey(searchTerm, activeOnly);
    
    // Then: Should return only active matching configs
    assertThat(results).isNotEmpty();
    results.forEach(config -> {
        assertThat(config.getKey().toLowerCase()).contains(searchTerm.toLowerCase());
        assertThat(config.isActive()).isTrue();
    });
}
```

### Integration Tests

```java
@Test
public void testConfigSearchAPI() {
    // Given: Authenticated admin user
    String token = getAdminAuthToken();
    
    // When: Searching for quote configs
    ResponseEntity<Map<String, Object>> response = 
        restTemplate.getForEntity("/api/configs/search?searchTerm=quote", Map.class);
    
    // Then: Should return successful response
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().get("success")).isEqualTo(true);
    
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
    assertThat(data).isNotEmpty();
}
```

## Future Enhancements

1. **Pagination**: Add pagination support for large result sets
2. **Advanced Filtering**: Add filters for description, value, creation date
3. **Sorting**: Add sorting options by key, creation date, etc.
4. **Full-Text Search**: Implement full-text search across key, value, and description
5. **Search History**: Track and suggest popular search terms
6. **Export Results**: Add functionality to export search results 