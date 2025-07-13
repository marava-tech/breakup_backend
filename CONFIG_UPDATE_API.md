# Config Update API Documentation

## Overview

This document describes the enhanced config management API that includes the complete config object in responses and provides a dedicated update API for modifying config values, descriptions, and active status.

## Features

### 1. Complete Config Object in Responses

All config-related APIs now return the complete config object including:
- `id`: Unique identifier
- `key`: Config key name
- `value`: Config value
- `description`: Config description
- `active`: Whether the config is active

### 2. Config Update API

A new PATCH endpoint allows updating specific fields of a config without changing the key.

## API Endpoints

### Update Config (PUT)

**Endpoint:** `PUT /api/configs/update/{id}`

**Description:** Update config value, description, and active status by ID

**Authorization:** Requires `ROLE_ADMIN`

**Request Body:**
```json
{
  "value": "new_value",
  "description": "Updated description",
  "active": true
}
```

**Response:**
```json
{
  "id": "config_id",
  "key": "config_key",
  "value": "new_value",
  "description": "Updated description",
  "active": true
}
```

**Features:**
- Only updates fields that are provided in the request
- Preserves existing values for fields not included in the request
- Validates that the config exists before updating
- Returns the complete updated config object

## Usage Examples

### 1. Update Only Value

```bash
curl -X PUT "http://localhost:8080/api/configs/update/64f1a2b3c4d5e6f7g8h9i0j1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "value": "new_config_value"
  }'
```

### 2. Update Value and Description

```bash
curl -X PUT "http://localhost:8080/api/configs/update/64f1a2b3c4d5e6f7g8h9i0j1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "value": "updated_value",
    "description": "Updated description for this config"
  }'
```

### 3. Update Active Status

```bash
curl -X PUT "http://localhost:8080/api/configs/update/64f1a2b3c4d5e6f7g8h9i0j1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "active": false
  }'
```

### 4. Update All Fields

```bash
curl -X PUT "http://localhost:8080/api/configs/update/64f1a2b3c4d5e6f7g8h9i0j1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "value": "completely_new_value",
    "description": "New description for the config",
    "active": true
  }'
```

## Error Handling

### 1. Config Not Found
```json
{
  "error": "Config not found: 64f1a2b3c4d5e6f7g8h9i0j1"
}
```

### 2. Invalid Request
```json
{
  "error": "Invalid request format"
}
```

### 3. Unauthorized Access
```json
{
  "error": "Access denied"
}
```

## Data Flow

1. **Request Validation:** The API validates the request body format
2. **Config Lookup:** Finds the config by ID in the database
3. **Field Update:** Updates only the fields provided in the request
4. **Persistence:** Saves the updated config to the database
5. **Response:** Returns the complete updated config object

## Benefits

1. **Flexible Updates:** Only update the fields you need to change
2. **Complete Responses:** Always get the full config object in responses
3. **Safe Operations:** Key field cannot be modified through this endpoint
4. **Admin Control:** Only administrators can modify configs
5. **Audit Trail:** Changes are tracked in the database

## Integration with Existing APIs

The complete config object is now returned by all existing config APIs:
- `GET /api/configs/{id}` - Get config by ID
- `GET /api/configs/key/{key}` - Get config by key
- `GET /api/configs` - Get all configs (paginated)
- `GET /api/configs/search` - Search configs with pagination
- `POST /api/configs` - Create new config
- `PUT /api/configs/{id}` - Full config update

## Technical Implementation

### DTOs

**DefaultConfigUpdateRequest:**
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefaultConfigUpdateRequest {
    private String value;
    private String description;
    private Boolean active;
}
```

**DefaultConfigResponse:**
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefaultConfigResponse {
    private String id;
    private String key;
    private String value;
    private String description;
    private boolean active;
}
```

### Service Method

```java
public DefaultConfigResponse updateConfig(String id, DefaultConfigUpdateRequest request) {
    DefaultConfig config = defaultConfigRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Config not found: " + id));
    
    // Update only the fields provided in the request
    if (request.getValue() != null) {
        config.setValue(request.getValue());
    }
    if (request.getDescription() != null) {
        config.setDescription(request.getDescription());
    }
    if (request.getActive() != null) {
        config.setActive(request.getActive());
    }
    
    return DefaultConfigResponse.fromEntity(defaultConfigRepository.save(config));
}
```

### Controller Endpoint

```java
@PutMapping("/update/{id}")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Operation(summary = "Update config value, description, and active status")
public ResponseEntity<DefaultConfigResponse> updateConfig(
        @PathVariable String id, 
        @Valid @RequestBody DefaultConfigUpdateRequest request) {
    return ResponseEntity.ok(defaultConfigService.updateConfig(id, request));
}
```

## Security Considerations

1. **Admin Only:** Update operations require `ROLE_ADMIN` authority
2. **Input Validation:** Request body is validated using `@Valid`
3. **Safe Updates:** Key field cannot be modified through this endpoint
4. **Error Handling:** Proper error responses for invalid requests

## Testing

### Test Cases

1. **Valid Update:** Update value, description, and active status
2. **Partial Update:** Update only specific fields
3. **Invalid ID:** Try to update non-existent config
4. **Unauthorized:** Try to update without admin role
5. **Invalid Request:** Send malformed request body

### Sample Test Commands

```bash
# Test valid update
curl -X PUT "http://localhost:8080/api/configs/update/64f1a2b3c4d5e6f7g8h9i0j1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
  -d '{"value": "test_value", "description": "Test description", "active": true}'

# Test partial update
curl -X PUT "http://localhost:8080/api/configs/update/64f1a2b3c4d5e6f7g8h9i0j1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
  -d '{"value": "new_value"}'

# Test invalid ID
curl -X PUT "http://localhost:8080/api/configs/update/invalid_id" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
  -d '{"value": "test"}'
```

## Migration Notes

- Existing config APIs continue to work as before
- All responses now include the complete config object
- New update API provides more granular control over config modifications
- No breaking changes to existing functionality 