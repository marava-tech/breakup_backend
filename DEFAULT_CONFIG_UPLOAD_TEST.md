# Default Config File Upload API - Test Guide

## Prerequisites

1. **Admin Access**: You need an admin JWT token
2. **Upload Service**: The upload service should be running and accessible
3. **Test Files**: Prepare some test files (images, audio, etc.)

## Getting Admin Token

First, you need to get an admin JWT token. Use the auth endpoint:

```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "admin_password"
  }'
```

Save the JWT token from the response.

## Test Scenarios

### 1. Upload Default Profile Image

```bash
curl -X POST "http://localhost:8080/api/configs/upload" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "key=default_profile_image" \
  -F "file=@test_images/default_avatar.png" \
  -F "description=Default avatar for new users" \
  -F "active=true"
```

**Expected Response (201 Created):**
```json
{
  "id": "64f8a1b2c3d4e5f6a7b8c9d0",
  "key": "default_profile_image",
  "value": "https://cdn.example.com/uploads/default_profile_image_123456.png",
  "description": "Default avatar for new users",
  "active": true
}
```

### 2. Upload Recording Start Audio

```bash
curl -X POST "http://localhost:8080/api/configs/upload" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "key=english_male_start_rec" \
  -F "file=@test_audio/start_recording_en_male.mp3" \
  -F "description=English male recording start audio" \
  -F "active=true"
```

### 3. Upload Quote Background Image

```bash
curl -X POST "http://localhost:8080/api/configs/upload" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "key=quote_image_1" \
  -F "file=@test_images/quote_background_1.jpg" \
  -F "description=Background image for quote 1" \
  -F "active=true"
```

### 4. Update Existing Config

Upload a new file for an existing key:

```bash
curl -X POST "http://localhost:8080/api/configs/upload" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "key=default_profile_image" \
  -F "file=@test_images/new_avatar.png" \
  -F "description=Updated default avatar for new users" \
  -F "active=true"
```

**Expected Response (200 OK):**
```json
{
  "id": "64f8a1b2c3d4e5f6a7b8c9d0",
  "key": "default_profile_image",
  "value": "https://cdn.example.com/uploads/new_avatar_789012.png",
  "description": "Updated default avatar for new users",
  "active": true
}
```

## Error Test Cases

### 1. Missing File

```bash
curl -X POST "http://localhost:8080/api/configs/upload" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "key=test_key"
```

**Expected Response (400 Bad Request)**

### 2. Missing Key

```bash
curl -X POST "http://localhost:8080/api/configs/upload" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@test_images/test.png"
```

**Expected Response (400 Bad Request)**

### 3. Unauthorized Access

```bash
curl -X POST "http://localhost:8080/api/configs/upload" \
  -F "key=test_key" \
  -F "file=@test_images/test.png"
```

**Expected Response (401 Unauthorized)**

### 4. Invalid Token

```bash
curl -X POST "http://localhost:8080/api/configs/upload" \
  -H "Authorization: Bearer INVALID_TOKEN" \
  -F "key=test_key" \
  -F "file=@test_images/test.png"
```

**Expected Response (401 Unauthorized)**

## Verification

After uploading files, you can verify the configuration was saved:

### 1. Get Config by Key

```bash
curl -X GET "http://localhost:8080/api/configs/key/default_profile_image" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 2. Get All Configs

```bash
curl -X GET "http://localhost:8080/api/configs?page=0&size=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Test Files Structure

Create a test directory structure:

```
test_files/
├── images/
│   ├── default_avatar.png
│   ├── quote_background_1.jpg
│   └── thumbnail_default.jpg
├── audio/
│   ├── start_recording_en_male.mp3
│   ├── start_recording_en_female.mp3
│   └── start_recording_hi_male.mp3
└── documents/
    └── terms_of_service.pdf
```

## Automated Testing Script

Create a test script `test_upload_api.sh`:

```bash
#!/bin/bash

# Configuration
BASE_URL="http://localhost:8080"
JWT_TOKEN="YOUR_JWT_TOKEN"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "Testing Default Config File Upload API..."

# Test 1: Upload default profile image
echo -e "\n${GREEN}Test 1: Upload default profile image${NC}"
response=$(curl -s -w "%{http_code}" -X POST "$BASE_URL/api/configs/upload" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "key=default_profile_image" \
  -F "file=@test_files/images/default_avatar.png" \
  -F "description=Default avatar for new users")

http_code="${response: -3}"
body="${response%???}"

if [ "$http_code" = "201" ] || [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✓ Success (HTTP $http_code)${NC}"
    echo "$body" | jq .
else
    echo -e "${RED}✗ Failed (HTTP $http_code)${NC}"
    echo "$body"
fi

# Test 2: Upload audio file
echo -e "\n${GREEN}Test 2: Upload audio file${NC}"
response=$(curl -s -w "%{http_code}" -X POST "$BASE_URL/api/configs/upload" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "key=english_male_start_rec" \
  -F "file=@test_files/audio/start_recording_en_male.mp3" \
  -F "description=English male recording start audio")

http_code="${response: -3}"
body="${response%???}"

if [ "$http_code" = "201" ] || [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✓ Success (HTTP $http_code)${NC}"
    echo "$body" | jq .
else
    echo -e "${RED}✗ Failed (HTTP $http_code)${NC}"
    echo "$body"
fi

# Test 3: Missing file (should fail)
echo -e "\n${GREEN}Test 3: Missing file (should fail)${NC}"
response=$(curl -s -w "%{http_code}" -X POST "$BASE_URL/api/configs/upload" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "key=test_key")

http_code="${response: -3}"
body="${response%???}"

if [ "$http_code" = "400" ]; then
    echo -e "${GREEN}✓ Correctly failed (HTTP $http_code)${NC}"
else
    echo -e "${RED}✗ Should have failed (HTTP $http_code)${NC}"
fi

echo -e "\n${GREEN}Testing completed!${NC}"
```

Make the script executable and run it:

```bash
chmod +x test_upload_api.sh
./test_upload_api.sh
```

## Monitoring

Check the application logs for upload operations:

```bash
tail -f logs/application.log | grep "uploadFileAndSaveConfig"
```

Look for log entries like:
- `Uploading file: default_avatar.png (12345 bytes) for config key: default_profile_image`
- `File uploaded successfully. URL: https://cdn.example.com/uploads/...`
- `Created new config for key: default_profile_image with file URL: ...`
- `Updated existing config for key: default_profile_image with file URL: ...` 