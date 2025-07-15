# Story Creation Configuration API

## Overview

The Story Creation Configuration API provides a centralized way to retrieve all story creation configuration settings with parsed values. This API fetches all configuration keys with the prefix "app_config_" and automatically parses numeric and boolean values for easy consumption by frontend applications.

## API Endpoint

### GET /api/configs/story-creation-config

**Description**: Get all story creation configuration settings with parsed values and user eligibility information

**Authentication**: Optional (provides user-specific eligibility if authenticated)

**Parameters**: None

## Response Format

### Success Response (200 OK)

```json
{
  "configs": {
    "app_config_max_story_duration_minutes": 10,
    "app_config_max_written_story_length_characters": 1000,
    "app_config_user_story_creation_limit_per_day": 3,
    "app_config_audio_upload_enabled": true,
    "app_config_written_story_enabled": true,
    "app_config_auto_transcription_enabled": true,
    "app_config_max_audio_file_size_mb": 50,
    "app_config_min_story_duration_seconds": 30,
    "app_config_max_story_title_length": 100,
    "app_config_require_location": false,
    "user_story_creation_enabled": true,
    "user_daily_story_limit": 3,
    "user_current_story_count": 1,
    "user_remaining_stories": 2,
    "user_next_eligibility_time": null
  },
  "totalConfigs": 15,
  "message": "Story creation configuration retrieved successfully"
}
```

### Error Response (500 Internal Server Error)

```json
{
  "configs": {},
  "totalConfigs": 0,
  "message": "Failed to retrieve story creation configuration"
}
```

### Response for Unauthenticated Users

```json
{
  "configs": {
    "app_config_max_story_duration_minutes": 10,
    "app_config_max_written_story_length_characters": 1000,
    "app_config_user_story_creation_limit_per_day": 3,
    "app_config_audio_upload_enabled": true,
    "app_config_written_story_enabled": true,
    "app_config_auto_transcription_enabled": true,
    "app_config_max_audio_file_size_mb": 50,
    "app_config_min_story_duration_seconds": 30,
    "app_config_max_story_title_length": 100,
    "app_config_require_location": false
  },
  "totalConfigs": 10,
  "message": "Story creation configuration retrieved successfully"
}
```

## Implementation Details

### DTO Structure

**StoryCreationConfigResponse:**
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryCreationConfigResponse {
    private Map<String, Object> configs;
    private int totalConfigs;
    private String message;
}
```

### Service Method

```java
/**
 * Get story creation configuration settings with eligibility information
 * Fetches all configuration keys with prefix "app_config_" related to story creation
 * and parses numeric values appropriately. Also includes user eligibility information.
 *
 * @param userId The user ID to check eligibility for (optional, can be null)
 * @return StoryCreationConfigResponse containing parsed story creation configuration settings and eligibility
 */
public StoryCreationConfigResponse getStoryCreationConfig(String userId) {
    try {
        // Get all app_config_ prefixed configurations
        List<DefaultConfig> appConfigs = defaultConfigRepository.findByKeyStartingWithAndActiveTrue("app_config_");
        
        Map<String, Object> configMap = new HashMap<>();
        
        for (DefaultConfig config : appConfigs) {
            String key = config.getKey();
            String value = config.getValue();
            
            // Parse specific configuration values
            Object parsedValue = parseConfigValue(key, value);
            configMap.put(key, parsedValue);
        }
        
        // Add eligibility information if userId is provided
        if (userId != null && !userId.trim().isEmpty()) {
            Map<String, Object> eligibilityInfo = getEligibilityInfo(userId);
            configMap.putAll(eligibilityInfo);
        }
        
        log.info("Found {} story creation configuration settings for user: {}", configMap.size(), userId);
        
        return StoryCreationConfigResponse.builder()
                .configs(configMap)
                .totalConfigs(configMap.size())
                .message("Story creation configuration retrieved successfully")
                .build();
                
    } catch (Exception e) {
        log.error("Failed to get story creation configuration for user: {}", userId, e);
        return StoryCreationConfigResponse.builder()
                .configs(Map.of())
                .totalConfigs(0)
                .message("Failed to retrieve story creation configuration")
                .build();
    }
}

/**
 * Get eligibility information for a user
 * 
 * @param userId The user ID to check eligibility for
 * @return Map containing eligibility information
 */
private Map<String, Object> getEligibilityInfo(String userId) {
    Map<String, Object> eligibilityInfo = new HashMap<>();
    
    try {
        // Get daily limit from configuration
        DefaultConfig dailyLimitConfig = defaultConfigRepository.findByKey("app_config_user_story_creation_limit_per_day")
                .orElse(null);
        
        int dailyLimit = 1; // Default limit if config not found
        if (dailyLimitConfig != null && dailyLimitConfig.isActive()) {
            try {
                dailyLimit = Integer.parseInt(dailyLimitConfig.getValue());
            } catch (NumberFormatException e) {
                log.warn("Invalid daily limit value in config: {}. Using default value: 1", dailyLimitConfig.getValue());
            }
        } else {
            log.warn("Daily limit configuration not found or inactive. Using default value: 1");
        }
        
        // Calculate 24 hours ago
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        
        // Count user stories in last 24 hours (excluding FAILED and REJECTED)
        long storyCount = storyRepository.countByUserIdAndStatusNotInAndCreatedAtAfter(userId, twentyFourHoursAgo);
        
        // Check eligibility
        boolean isEligible = storyCount < dailyLimit;
        int remainingStories = Math.max(0, dailyLimit - (int) storyCount);
        
        // Calculate next eligibility time if user has reached the limit
        LocalDateTime nextEligibilityTime = null;
        if (!isEligible && storyCount > 0) {
            // Find the latest non-failed/non-rejected story to calculate when 24 hours will pass
            List<Story> latestStories = storyRepository.findLatestNonFailedStoryByUserId(userId);
            if (!latestStories.isEmpty()) {
                Story latestStory = latestStories.get(0);
                nextEligibilityTime = latestStory.getCreatedAt().plusHours(24);
            }
        }
        
        // Add eligibility information to config map
        eligibilityInfo.put("user_story_creation_enabled", isEligible);
        eligibilityInfo.put("user_daily_story_limit", dailyLimit);
        eligibilityInfo.put("user_current_story_count", (int) storyCount);
        eligibilityInfo.put("user_remaining_stories", remainingStories);
        eligibilityInfo.put("user_next_eligibility_time", nextEligibilityTime);
        
        log.info("Eligibility info for user {}: enabled={}, limit={}, count={}, remaining={}, nextEligibility={}", 
                userId, isEligible, dailyLimit, storyCount, remainingStories, nextEligibilityTime);
                
    } catch (Exception e) {
        log.error("Error getting eligibility info for user {}: {}", userId, e.getMessage(), e);
        // Add default eligibility info on error
        eligibilityInfo.put("user_story_creation_enabled", false);
        eligibilityInfo.put("user_daily_story_limit", 1);
        eligibilityInfo.put("user_current_story_count", 0);
        eligibilityInfo.put("user_remaining_stories", 0);
        eligibilityInfo.put("user_next_eligibility_time", null);
    }
    
    return eligibilityInfo;
}

/**
 * Parse configuration value based on key type
 * 
 * @param key The configuration key
 * @param value The raw configuration value
 * @return Parsed value (Integer, Boolean, or String)
 */
private Object parseConfigValue(String key, String value) {
    if (value == null || value.trim().isEmpty()) {
        return value;
    }
    
    try {
        // Parse numeric values
        if (key.contains("_minutes") || key.contains("_seconds") || 
            key.contains("_characters") || key.contains("_limit") || 
            key.contains("_count") || key.contains("_size")) {
            return Integer.parseInt(value.trim());
        }
        
        // Parse boolean values
        if (key.contains("_enabled") || key.contains("_disabled") || 
            key.contains("_allowed") || key.contains("_required")) {
            return Boolean.parseBoolean(value.trim());
        }
        
        // Return as string for other values
        return value.trim();
        
    } catch (NumberFormatException e) {
        log.warn("Failed to parse numeric value for key '{}': {}. Using as string.", key, value);
        return value.trim();
    }
}
```

### Controller Endpoint

```java
@GetMapping("/story-creation-config")
@Operation(summary = "Get story creation configuration", 
           description = "Get all story creation configuration settings with parsed values and user eligibility information")
public ResponseEntity<StoryCreationConfigResponse> getStoryCreationConfig(
        Authentication authentication) {
    try {
        String userId = null;
        
        // Get user ID if authenticated
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            userId = userService.getUserEntityByEmail(email).getId();
        }
        
        StoryCreationConfigResponse response = defaultConfigService.getStoryCreationConfig(userId);
        return ResponseEntity.ok(response);
    } catch (Exception e) {
        log.error("Error retrieving story creation configuration: {}", e.getMessage(), e);
        StoryCreationConfigResponse errorResponse = StoryCreationConfigResponse.builder()
                .configs(Map.of())
                .totalConfigs(0)
                .message("Error retrieving story creation configuration: " + e.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
```

## Value Parsing Rules

### Numeric Values (Integer)
The API automatically parses the following key patterns as integers:
- `*_minutes` - Duration in minutes
- `*_seconds` - Duration in seconds  
- `*_characters` - Character limits
- `*_limit` - Various limits
- `*_count` - Count values
- `*_size` - Size values

### Boolean Values
The API automatically parses the following key patterns as booleans:
- `*_enabled` - Feature enabled flags
- `*_disabled` - Feature disabled flags
- `*_allowed` - Permission flags
- `*_required` - Requirement flags

### String Values
All other values are returned as strings.

## Configuration Examples

### Common Story Creation Configurations

```bash
# Duration and length limits
curl -X POST "http://localhost:8080/api/configs" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "app_config_max_story_duration_minutes",
    "value": "10",
    "description": "Maximum story duration in minutes",
    "active": true
  }'

curl -X POST "http://localhost:8080/api/configs" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "app_config_max_written_story_length_characters",
    "value": "1000",
    "description": "Maximum written story length in characters",
    "active": true
  }'

# Feature flags
curl -X POST "http://localhost:8080/api/configs" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "app_config_audio_upload_enabled",
    "value": "true",
    "description": "Enable audio upload feature",
    "active": true
  }'

curl -X POST "http://localhost:8080/api/configs" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "app_config_written_story_enabled",
    "value": "true",
    "description": "Enable written story feature",
    "active": true
  }'

# File size limits
curl -X POST "http://localhost:8080/api/configs" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "app_config_max_audio_file_size_mb",
    "value": "50",
    "description": "Maximum audio file size in MB",
    "active": true
  }'

# Duration limits
curl -X POST "http://localhost:8080/api/configs" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "app_config_min_story_duration_seconds",
    "value": "30",
    "description": "Minimum story duration in seconds",
    "active": true
  }'

# Text limits
curl -X POST "http://localhost:8080/api/configs" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "app_config_max_story_title_length",
    "value": "100",
    "description": "Maximum story title length",
    "active": true
  }'

# Requirements
curl -X POST "http://localhost:8080/api/configs" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "app_config_require_location",
    "value": "false",
    "description": "Require location for story creation",
    "active": true
  }'
```

## Usage Examples

### Frontend Integration

```javascript
// Fetch story creation configuration
async function getStoryCreationConfig() {
    try {
        const response = await fetch('/api/configs/story-creation-config');
        const data = await response.json();
        
        if (response.ok) {
            // Apply configuration to UI
            applyStoryCreationConfig(data.configs);
        } else {
            console.error('Failed to fetch story creation config:', data.message);
        }
    } catch (error) {
        console.error('Error fetching story creation config:', error);
    }
}

// Apply configuration to UI
function applyStoryCreationConfig(configs) {
    // Set audio upload limits
    const maxDuration = configs['app_config_max_story_duration_minutes'] || 10;
    const maxFileSize = configs['app_config_max_audio_file_size_mb'] || 50;
    const minDuration = configs['app_config_min_story_duration_seconds'] || 30;
    
    // Set written story limits
    const maxCharacters = configs['app_config_max_written_story_length_characters'] || 1000;
    const maxTitleLength = configs['app_config_max_story_title_length'] || 100;
    
    // Enable/disable features
    const audioUploadEnabled = configs['app_config_audio_upload_enabled'] || false;
    const writtenStoryEnabled = configs['app_config_written_story_enabled'] || false;
    const requireLocation = configs['app_config_require_location'] || false;
    
    // Get user eligibility information
    const userStoryCreationEnabled = configs['user_story_creation_enabled'] || false;
    const userDailyLimit = configs['user_daily_story_limit'] || 1;
    const userCurrentCount = configs['user_current_story_count'] || 0;
    const userRemainingStories = configs['user_remaining_stories'] || 0;
    const userNextEligibilityTime = configs['user_next_eligibility_time'];
    
    // Update UI elements
    updateAudioUploadLimits(maxDuration, maxFileSize, minDuration);
    updateWrittenStoryLimits(maxCharacters, maxTitleLength);
    updateFeatureFlags(audioUploadEnabled, writtenStoryEnabled, requireLocation);
    updateUserEligibility(userStoryCreationEnabled, userDailyLimit, userCurrentCount, userRemainingStories, userNextEligibilityTime);
}

// Update user eligibility UI
function updateUserEligibility(enabled, dailyLimit, currentCount, remainingStories, nextEligibilityTime) {
    const createStoryButton = document.getElementById('create-story-btn');
    const eligibilityMessage = document.getElementById('eligibility-message');
    const nextEligibilityElement = document.getElementById('next-eligibility-time');
    
    if (enabled) {
        createStoryButton.disabled = false;
        eligibilityMessage.textContent = `You can create ${remainingStories} more stories today (${currentCount}/${dailyLimit})`;
        eligibilityMessage.className = 'text-success';
        nextEligibilityElement.style.display = 'none';
    } else {
        createStoryButton.disabled = true;
        eligibilityMessage.textContent = `Daily story creation limit reached (${currentCount}/${dailyLimit})`;
        eligibilityMessage.className = 'text-danger';
        
        // Show next eligibility time if available
        if (nextEligibilityTime) {
            const nextTime = new Date(nextEligibilityTime);
            const now = new Date();
            const timeDiff = nextTime - now;
            const hours = Math.floor(timeDiff / (1000 * 60 * 60));
            const minutes = Math.floor((timeDiff % (1000 * 60 * 60)) / (1000 * 60));
            
            nextEligibilityElement.style.display = 'block';
            nextEligibilityElement.textContent = `You can create stories again in ${hours}h ${minutes}m`;
        } else {
            nextEligibilityElement.style.display = 'none';
        }
    }
}

// Update audio upload limits
function updateAudioUploadLimits(maxDuration, maxFileSize, minDuration) {
    const durationInput = document.getElementById('story-duration');
    const fileInput = document.getElementById('audio-file');
    const durationHelp = document.getElementById('duration-help');
    
    durationInput.max = maxDuration * 60; // Convert to seconds
    durationInput.min = minDuration;
    fileInput.accept = `audio/*,.mp3,.wav,.m4a`;
    fileInput.setAttribute('data-max-size', maxFileSize * 1024 * 1024); // Convert to bytes
    
    durationHelp.textContent = `Duration: ${minDuration}s - ${maxDuration * 60}s, File size: max ${maxFileSize}MB`;
}

// Update written story limits
function updateWrittenStoryLimits(maxCharacters, maxTitleLength) {
    const storyTextArea = document.getElementById('story-text');
    const titleInput = document.getElementById('story-title');
    
    storyTextArea.maxLength = maxCharacters;
    titleInput.maxLength = maxTitleLength;
    
    // Update character counters
    updateCharacterCounter(storyTextArea, maxCharacters);
    updateCharacterCounter(titleInput, maxTitleLength);
}

// Update feature flags
function updateFeatureFlags(audioUploadEnabled, writtenStoryEnabled, requireLocation) {
    const audioUploadSection = document.getElementById('audio-upload-section');
    const writtenStorySection = document.getElementById('written-story-section');
    const locationInput = document.getElementById('location-input');
    
    audioUploadSection.style.display = audioUploadEnabled ? 'block' : 'none';
    writtenStorySection.style.display = writtenStoryEnabled ? 'block' : 'none';
    locationInput.required = requireLocation;
}
```

### API Testing

```bash
# Test the API
curl -X GET "http://localhost:8080/api/configs/story-creation-config"
```

## Configuration Categories

### Duration and Length Limits
- `app_config_max_story_duration_minutes`: Maximum story duration in minutes
- `app_config_min_story_duration_seconds`: Minimum story duration in seconds
- `app_config_max_written_story_length_characters`: Maximum written story length
- `app_config_max_story_title_length`: Maximum story title length

### File Upload Limits
- `app_config_max_audio_file_size_mb`: Maximum audio file size in MB
- `app_config_max_image_file_size_mb`: Maximum image file size in MB

### Feature Flags
- `app_config_audio_upload_enabled`: Enable audio upload feature
- `app_config_written_story_enabled`: Enable written story feature
- `app_config_auto_transcription_enabled`: Enable auto transcription
- `app_config_require_location`: Require location for story creation

### Rate Limiting
- `app_config_user_story_creation_limit_per_day`: Daily story creation limit
- `app_config_max_stories_per_hour`: Hourly story creation limit

## Benefits

1. **Centralized Configuration**: All story creation settings in one place
2. **Automatic Parsing**: Numeric and boolean values are automatically parsed
3. **Type Safety**: Frontend receives properly typed values
4. **Dynamic Updates**: Change limits without code deployment
5. **Feature Flags**: Enable/disable features dynamically
6. **Easy Integration**: Simple JSON response for frontend consumption
7. **Extensible**: Easy to add new configuration keys

## Security Considerations

1. **Public Access**: This endpoint is publicly accessible for frontend configuration
2. **Read-Only**: This endpoint only retrieves configuration values
3. **Active Only**: Only active configurations are returned
4. **Error Handling**: Proper error handling with fallback responses 