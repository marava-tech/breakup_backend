# Configuration by Prefix API

## Overview

The Configuration by Prefix API provides a flexible way to retrieve application configuration settings that control UI behavior and restrictions. This API fetches all configuration keys with a specified prefix from the default configuration system.

## API Endpoint

### GET /api/configs/by-prefix

**Description**: Retrieve all configuration settings with a specific prefix

**Authentication**: Not required (public endpoint)

**Parameters**: 
- `configPrefix` (required): The prefix to search for in configuration keys

## Response Format

### Success Response (200 OK)

```json
{
  "configs": {
    "app_config_feature_flag_1": "true",
    "app_config_max_upload_size": "10",
    "app_config_maintenance_mode": "false",
    "app_config_ui_theme": "dark",
    "app_config_rate_limit": "100"
  },
  "totalConfigs": 5,
  "message": "Configurations retrieved successfully for prefix: app_config_"
}
```

### Error Response (400 Bad Request)

```json
{
  "configs": {},
  "totalConfigs": 0,
  "message": "Config prefix is required"
}
```

### Error Response (500 Internal Server Error)

```json
{
  "configs": {},
  "totalConfigs": 0,
  "message": "Failed to retrieve configurations for prefix: app_config_"
}
```

## Implementation Details

### Repository Method

Added to `DefaultConfigRepository`:

```java
// App configuration methods
List<DefaultConfig> findByKeyStartingWithAndActiveTrue(String keyPrefix);
```

This method performs a direct database query to fetch configurations where:
- Key starts with the specified prefix ("app_config_")
- Active status is true
- Returns only the matching configurations, not all configurations

### DTO Structure

**AppConfigResponse:**
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppConfigResponse {
    private Map<String, String> configs;
    private int totalConfigs;
    private String message;
}
```

### Service Method

```java
/**
 * Get all configuration settings with a specific prefix
 * This method uses a direct database query to fetch all configuration keys that start with the specified prefix 
 * and returns them as key-value pairs
 *
 * @param configPrefix The prefix to search for in configuration keys
 * @return AppConfigResponse containing all configuration settings with the specified prefix
 */
public AppConfigResponse getConfigurationsByPrefix(String configPrefix) {
    try {
        // Validate prefix parameter
        if (configPrefix == null || configPrefix.trim().isEmpty()) {
            log.warn("Config prefix is null or empty, returning empty response");
            return AppConfigResponse.builder()
                    .configs(Map.of())
                    .totalConfigs(0)
                    .message("Config prefix is required")
                    .build();
        }

        // Direct database query to fetch configurations with the specified prefix
        List<DefaultConfig> configs = defaultConfigRepository.findByKeyStartingWithAndActiveTrue(configPrefix.trim());

        Map<String, String> configMap = configs.stream()
                .collect(Collectors.toMap(
                        DefaultConfig::getKey,
                        DefaultConfig::getValue,
                        (existing, replacement) -> existing // Keep existing value if duplicate key
                ));

        log.info("Found {} configuration settings with prefix '{}' using direct DB query", configMap.size(), configPrefix);
        
        return AppConfigResponse.builder()
                .configs(configMap)
                .totalConfigs(configMap.size())
                .message("Configurations retrieved successfully for prefix: " + configPrefix)
                .build();

    } catch (Exception e) {
        log.error("Failed to get configurations with prefix: {}", configPrefix, e);
        return AppConfigResponse.builder()
                .configs(Map.of())
                .totalConfigs(0)
                .message("Failed to retrieve configurations for prefix: " + configPrefix)
                .build();
    }
}
```

### Controller Endpoint

```java
@GetMapping("/by-prefix")
@Operation(summary = "Get configuration settings by prefix", 
           description = "Retrieve all configuration settings with a specific prefix for UI control and restrictions")
public ResponseEntity<AppConfigResponse> getConfigurationsByPrefix(
        @RequestParam String configPrefix) {
    try {
        AppConfigResponse response = defaultConfigService.getConfigurationsByPrefix(configPrefix);
        return ResponseEntity.ok(response);
    } catch (Exception e) {
        log.error("Error retrieving configurations with prefix '{}': {}", configPrefix, e.getMessage(), e);
        AppConfigResponse errorResponse = AppConfigResponse.builder()
                .configs(Map.of())
                .totalConfigs(0)
                .message("Failed to retrieve configurations for prefix '" + configPrefix + "': " + e.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
```

## Usage Examples

### Frontend Integration

```javascript
// Fetch configurations by prefix
async function getConfigurationsByPrefix(prefix) {
    try {
        const response = await fetch(`/api/configs/by-prefix?configPrefix=${encodeURIComponent(prefix)}`);
        const data = await response.json();
        
        if (response.ok) {
            // Apply configurations to UI
            applyConfigurations(data.configs);
        } else {
            console.error('Failed to fetch configurations:', data.message);
        }
    } catch (error) {
        console.error('Error fetching configurations:', error);
    }
}

// Apply configurations to UI
function applyConfigurations(configs) {
    // Example: Check maintenance mode
    if (configs['app_config_maintenance_mode'] === 'true') {
        showMaintenanceMode();
    }
    
    // Example: Set upload size limit
    const maxUploadSize = parseInt(configs['app_config_max_upload_size'] || '10');
    setUploadSizeLimit(maxUploadSize);
    
    // Example: Apply UI theme
    const theme = configs['app_config_ui_theme'] || 'light';
    applyTheme(theme);
}

// Usage examples
getConfigurationsByPrefix('app_config_');  // Get app configurations
getConfigurationsByPrefix('ui_');          // Get UI configurations
getConfigurationsByPrefix('payment_');     // Get payment configurations
```

### Configuration Management

To add new configurations, use the existing config management APIs:

```bash
# Add a new app configuration
curl -X POST "http://localhost:8080/api/configs" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "app_config_feature_flag_1",
    "value": "true",
    "description": "Enable new feature flag 1",
    "active": true
  }'

# Add UI configuration
curl -X POST "http://localhost:8080/api/configs" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "ui_theme_dark",
    "value": "true",
    "description": "Enable dark theme",
    "active": true
  }'

# Add payment configuration
curl -X POST "http://localhost:8080/api/configs" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "payment_enabled",
    "value": "true",
    "description": "Enable payment features",
    "active": true
  }'
```

## Configuration Examples

Common configuration prefixes and keys you might want to use:

### App Configurations (app_config_)
- `app_config_maintenance_mode`: Enable/disable maintenance mode
- `app_config_max_upload_size`: Maximum file upload size in MB
- `app_config_rate_limit`: API rate limit per minute
- `app_config_feature_flag_1`: Enable/disable specific features
- `app_config_payment_enabled`: Enable/disable payment features
- `app_config_analytics_enabled`: Enable/disable analytics tracking
- `app_config_debug_mode`: Enable/disable debug mode

### UI Configurations (ui_)
- `ui_theme_dark`: Enable/disable dark theme
- `ui_show_ads`: Enable/disable advertisements
- `ui_show_notifications`: Enable/disable notifications
- `ui_show_tutorial`: Enable/disable tutorial mode

### Payment Configurations (payment_)
- `payment_enabled`: Enable/disable payment features
- `payment_gateway`: Payment gateway selection
- `payment_currency`: Default currency
- `payment_min_amount`: Minimum payment amount

## Security Considerations

1. **Public Access**: This endpoint is publicly accessible to allow frontend applications to retrieve configuration without authentication
2. **Read-Only**: This endpoint only retrieves configuration values, no modification is possible
3. **Active Only**: Only active configurations are returned
4. **Error Handling**: Proper error handling with fallback responses

## Testing

### Test Cases

1. **Valid Request**: Retrieve app configurations successfully
2. **No Configurations**: Return empty map when no app_config keys exist
3. **Database Error**: Handle database connection issues gracefully
4. **Mixed Configurations**: Ensure only app_config_ prefixed keys are returned

### Test Configuration Setup

```bash
# Add test configurations
curl -X POST "http://localhost:8080/api/configs" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "app_config_test_feature",
    "value": "true",
    "description": "Test feature flag",
    "active": true
  }'

# Test the API with different prefixes
curl -X GET "http://localhost:8080/api/configs/by-prefix?configPrefix=app_config_"
curl -X GET "http://localhost:8080/api/configs/by-prefix?configPrefix=ui_"
curl -X GET "http://localhost:8080/api/configs/by-prefix?configPrefix=payment_"
```

## Benefits

1. **Centralized Configuration**: All app settings in one place
2. **Dynamic Updates**: Change app behavior without code deployment
3. **UI Control**: Easy integration with frontend applications
4. **Feature Flags**: Enable/disable features dynamically
5. **Maintenance Mode**: Quick maintenance mode activation
6. **Rate Limiting**: Dynamic rate limit configuration
7. **Theme Control**: Dynamic UI theme switching 