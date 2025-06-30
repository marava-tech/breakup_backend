# Default Config File Upload API

## Overview

The Default Config File Upload API allows administrators to upload files and automatically save the uploaded file URL as a value in the default configuration system. This is useful for managing static assets like images, audio files, or other media that need to be referenced by configuration keys.

## API Endpoint

### Upload File and Save to Config

**POST** `/api/configs/upload`

Uploads a file and saves the resulting URL as a value in the default configuration.

#### Request Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `key` | String | Yes | The configuration key to store the file URL under |
| `file` | MultipartFile | Yes | The file to upload |
| `description` | String | No | Optional description for the configuration entry |
| `active` | Boolean | No | Whether the config should be active (default: true) |

#### Request Example

```bash
curl -X POST "http://localhost:8080/api/configs/upload" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "key=profile_image_default" \
  -F "file=@/path/to/image.jpg" \
  -F "description=Default profile image for new users" \
  -F "active=true"
```

#### Response

**Success (201 Created) - New Config Created:**
```json
{
  "id": "64f8a1b2c3d4e5f6a7b8c9d0",
  "key": "profile_image_default",
  "value": "https://cdn.example.com/uploads/profile_image_default_123456.jpg",
  "description": "Default profile image for new users",
  "active": true
}
```

**Success (200 OK) - Existing Config Updated:**
```json
{
  "id": "64f8a1b2c3d4e5f6a7b8c9d0",
  "key": "profile_image_default",
  "value": "https://cdn.example.com/uploads/profile_image_default_789012.jpg",
  "description": "Updated default profile image for new users",
  "active": true
}
```

**Error (400 Bad Request):**
```json
{
  "error": "File upload failed or no URL returned"
}
```

**Error (401 Unauthorized):**
```json
{
  "error": "Access denied. Admin privileges required."
}
```

**Error (500 Internal Server Error):**
```json
{
  "error": "Internal server error during file upload or config save"
}
```

## Features

### 1. **Automatic File Upload**
- Files are uploaded to the configured upload service
- Supports all file types supported by the upload service
- Returns the uploaded file URL

### 2. **Smart Config Management**
- **Create New Config**: If the key doesn't exist, creates a new configuration entry
- **Update Existing Config**: If the key already exists, updates the existing configuration
- **Preserve Metadata**: Maintains description and active status

### 3. **Flexible Configuration**
- Optional description for better organization
- Configurable active status
- Automatic description generation if not provided

### 4. **Security**
- Admin-only access (requires `ADMIN` authority)
- JWT token authentication required
- File upload validation

## Use Cases

### 1. **Default Profile Images**
```bash
curl -X POST "http://localhost:8080/api/configs/upload" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "key=default_profile_image" \
  -F "file=@default_avatar.png" \
  -F "description=Default avatar for new users"
```

### 2. **Audio Files for Recording**
```bash
curl -X POST "http://localhost:8080/api/configs/upload" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "key=english_male_start_rec" \
  -F "file=@start_recording_en_male.mp3" \
  -F "description=English male recording start audio"
```

### 3. **Quote Images**
```bash
curl -X POST "http://localhost:8080/api/configs/upload" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "key=quote_image_1" \
  -F "file=@quote_background_1.jpg" \
  -F "description=Background image for quote 1"
```

### 4. **Thumbnail Images**
```bash
curl -X POST "http://localhost:8080/api/configs/upload" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "key=default_thumbnail_url" \
  -F "file=@default_thumbnail.jpg" \
  -F "description=Default thumbnail for stories"
```

## Integration with Existing Services

### UploadService Integration
- Uses the existing `UploadService.uploadFile()` method
- Leverages the configured upload service (e.g., CDN, cloud storage)
- Maintains consistency with other file upload operations

### DefaultConfigService Integration
- Uses existing `create()` and `update()` methods
- Maintains data consistency and validation
- Preserves existing configuration management features

## Error Handling

### File Upload Errors
- Invalid file format
- File size limits
- Upload service unavailability
- Network connectivity issues

### Configuration Errors
- Invalid key format
- Database connection issues
- Validation failures

### Security Errors
- Unauthorized access
- Invalid JWT token
- Insufficient privileges

## Monitoring and Logging

### Success Metrics
- File upload success rate
- Configuration save success rate
- Response times

### Error Tracking
- File upload failures
- Configuration save failures
- Authentication failures

### Logs to Monitor
- File upload attempts and results
- Configuration creation/update operations
- Error details for debugging

## Best Practices

### 1. **File Naming**
- Use descriptive keys that indicate the file's purpose
- Follow consistent naming conventions
- Include file type or category in the key

### 2. **File Management**
- Regularly review and clean up unused files
- Monitor file storage usage
- Implement file size limits

### 3. **Configuration Management**
- Use descriptive descriptions for better organization
- Set appropriate active/inactive status
- Document configuration keys and their purposes

### 4. **Security**
- Validate file types and sizes
- Implement proper access controls
- Monitor for suspicious upload patterns

## Future Enhancements

### 1. **Batch Upload**
- Support for uploading multiple files at once
- Bulk configuration creation/update

### 2. **File Validation**
- File type validation
- File size limits
- Image dimension validation

### 3. **Versioning**
- File version management
- Rollback capabilities
- Change history tracking

### 4. **CDN Integration**
- Direct CDN upload
- Automatic CDN URL generation
- Cache invalidation

### 5. **File Processing**
- Image optimization
- Audio compression
- Thumbnail generation 