# Location-Based Story Metadata

## Overview

The location-based story metadata system enhances story creation by automatically extracting and storing location information when latitude and longitude coordinates are provided in the request headers. This feature enables location-aware story processing and improves content discovery based on geographic context.

## Features

- ✅ **Header-Based Location Input**: Extract coordinates from `X-Latitude` and `X-Longitude` headers
- ✅ **Automatic Geocoding**: Convert coordinates to district, state, and pincode
- ✅ **Metadata Integration**: Store location data in story metadata
- ✅ **Fallback Handling**: Graceful handling when coordinates are not provided
- ✅ **Mock Geocoding Service**: Simulated location extraction for development
- ✅ **Backward Compatibility**: Existing functionality works without location data

## Implementation

### 1. **Header Extraction**

The system extracts location coordinates from HTTP headers:

```java
// In StoryController.createStory()
String latitude = request.getHeader("X-Latitude");
String longitude = request.getHeader("X-Longitude");
```

### 2. **Location Processing**

Coordinates are processed through the MockAIService:

```java
// Extract location details from coordinates
String locationDetails = mockAIService.extractLocationFromCoordinates(latitude, longitude);
```

### 3. **Metadata Storage**

Location information is stored in StoryMetadata:

```java
StoryMetadata metadata = StoryMetadata.builder()
    .names(names)
    .locations(locations)
    .pincodes(pincodes)
    .state(currentState != null ? currentState : mockGetState(locations))
    .district(currentDistrict != null ? currentDistrict : mockGetState(locations))
    .language(language)
    .deviceInfo(deviceInfo)
    .build();
```

## API Usage

### Request Headers

Include location coordinates in the request headers:

```http
POST /api/stories
Content-Type: multipart/form-data
Authorization: Bearer YOUR_TOKEN
X-Latitude: 19.0760
X-Longitude: 72.8777
```

### Example Request

```bash
curl -X POST "http://localhost:8080/api/stories" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "X-Latitude: 19.0760" \
  -H "X-Longitude: 72.8777" \
  -F "audio=@story.mp3"
```

### Response

The story creation response includes location metadata:

```json
{
  "requestId": "req_1703123456789_a1b2c3d4",
  "data": {
    "id": "story123",
    "title": "Uploading...",
    "status": "PROCESSING",
    "metadata": {
      "names": ["John", "Sarah"],
      "locations": ["Mumbai, Maharashtra"],
      "pincodes": ["400001"],
      "state": "Maharashtra",
      "district": "Mumbai",
      "language": "English",
      "deviceInfo": "Android 12.0"
    }
  },
  "message": "Story creation initiated successfully",
  "timestamp": 1703123456789
}
```

## Mock Geocoding Service

### Supported Locations

The mock service supports various Indian and international locations:

#### Indian Cities
- **Mumbai**: `19.0760, 72.8777` → "Mumbai, Maharashtra, 400001"
- **Bangalore**: `12.9716, 77.5946` → "Bangalore, Karnataka, 560001"
- **New Delhi**: `28.6139, 77.2090` → "New Delhi, Delhi, 110001"
- **Kolkata**: `22.5726, 88.3639` → "Kolkata, West Bengal, 700001"
- **Chennai**: `13.0827, 80.2707` → "Chennai, Tamil Nadu, 600001"
- **Hyderabad**: `17.3850, 78.4867` → "Hyderabad, Telangana, 500001"
- **Ahmedabad**: `23.0225, 72.5714` → "Ahmedabad, Gujarat, 380001"
- **Lucknow**: `26.8467, 80.9462` → "Lucknow, Uttar Pradesh, 226001"
- **Varanasi**: `25.3176, 82.9739` → "Varanasi, Uttar Pradesh, 221001"
- **Bhubaneswar**: `20.2961, 85.8245` → "Bhubaneswar, Odisha, 751001"

#### International Cities
- **New York**: `40.7128, -73.9352` → "New York, New York, 10001"
- **London**: `51.5074, -0.1278` → "London, England, SW1A1AA"
- **Paris**: `48.8566, 2.3522` → "Paris, France, 75001"
- **Tokyo**: `35.6762, 139.6503` → "Tokyo, Japan, 100-0001"
- **Sydney**: `-33.8688, 151.2093` → "Sydney, New South Wales, 2000"

### Coordinate Ranges

The service uses coordinate ranges to determine location:

```java
// India coordinates
if (latitude >= 8.0 && latitude <= 37.0 && longitude >= 68.0 && longitude <= 97.0) {
    // Indian location logic
}

// International coordinates
else {
    // International location logic
}
```

## Error Handling

### Invalid Coordinates

```java
// Invalid format
if (latitude == null || longitude == null || latitude.trim().isEmpty() || longitude.trim().isEmpty()) {
    log.warn("Invalid coordinates provided: lat={}, lng={}", latitude, longitude);
    return null;
}

// Number format exception
try {
    double lat = Double.parseDouble(latitude);
    double lng = Double.parseDouble(longitude);
} catch (NumberFormatException e) {
    log.error("Invalid coordinate format: lat={}, lng={}", latitude, longitude, e);
    return null;
}
```

### Fallback Behavior

When coordinates are not provided or invalid:

1. **No Location Headers**: Story creation proceeds normally without location data
2. **Invalid Coordinates**: Location extraction is skipped, story processing continues
3. **Geocoding Failure**: Default location values are used

## Integration Points

### 1. **Story Creation Flow**

```java
// 1. Extract coordinates from headers
String latitude = request.getHeader("X-Latitude");
String longitude = request.getHeader("X-Longitude");

// 2. Pass to story service
StoryResponse response = storyService.createStory(userId, request, latitude, longitude);

// 3. AI processing includes location extraction
mockAIService.processStoryWithAIAsync(storyId, latitude, longitude);
```

### 2. **AI Processing Pipeline**

```java
// Location extraction during AI processing
if (latitude != null && longitude != null && !latitude.trim().isEmpty() && !longitude.trim().isEmpty()) {
    String currentLocation = extractLocationFromCoordinates(latitude, longitude);
    if (currentLocation != null) {
        // Parse and add to metadata
        String[] locationParts = currentLocation.split(", ");
        currentDistrict = locationParts[0];
        currentState = locationParts[1];
        currentPincode = locationParts[2];
    }
}
```

### 3. **Metadata Storage**

```java
// Store in StoryMetadata
StoryMetadata metadata = StoryMetadata.builder()
    .state(currentState != null ? currentState : mockGetState(locations))
    .district(currentDistrict != null ? currentDistrict : mockGetState(locations))
    .build();
```

## Testing

### Test Cases

1. **Valid Coordinates**
```bash
curl -X POST "http://localhost:8080/api/stories" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "X-Latitude: 19.0760" \
  -H "X-Longitude: 72.8777" \
  -F "audio=@story.mp3"
# Expected: Location metadata includes "Mumbai, Maharashtra, 400001"
```

2. **Invalid Coordinates**
```bash
curl -X POST "http://localhost:8080/api/stories" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "X-Latitude: invalid" \
  -H "X-Longitude: invalid" \
  -F "audio=@story.mp3"
# Expected: Story creation succeeds without location data
```

3. **Missing Coordinates**
```bash
curl -X POST "http://localhost:8080/api/stories" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "audio=@story.mp3"
# Expected: Story creation succeeds without location data
```

### Log Messages

The system logs location processing:

```
INFO  - Location coordinates received [RequestID: req_123] - lat: 19.0760, lng: 72.8777
INFO  - Extracting location details for coordinates: lat=19.076, lng=72.8777
INFO  - Location details extracted: Mumbai, Maharashtra, 400001
INFO  - Current location added for story story123: Mumbai, Maharashtra, 400001
INFO  - AI processing started for story: story123 with location data [RequestID: req_123]
```

## Future Enhancements

### 1. **Real Geocoding Service**

Replace mock service with actual geocoding:

```java
// Google Maps Geocoding API
@Value("${google.maps.api.key}")
private String googleMapsApiKey;

private String realGeocodeCoordinates(double latitude, double longitude) {
    // Call Google Maps Geocoding API
    String url = String.format(
        "https://maps.googleapis.com/maps/api/geocode/json?latlng=%f,%f&key=%s",
        latitude, longitude, googleMapsApiKey
    );
    // Implementation details...
}
```

### 2. **Reverse Geocoding**

Enhanced location details:

```java
public class LocationDetails {
    private String district;
    private String state;
    private String pincode;
    private String country;
    private String formattedAddress;
    private Double latitude;
    private Double longitude;
}
```

### 3. **Location-Based Features**

- **Nearby Stories**: Find stories from nearby locations
- **Location Filtering**: Filter stories by district/state
- **Geographic Analytics**: Location-based engagement metrics
- **Regional Content**: Location-specific content recommendations

### 4. **Configuration**

Make location extraction configurable:

```yaml
# application.yml
location:
  extraction:
    enabled: true
    geocoding-service: mock # mock, google, openstreetmap
    cache-enabled: true
    cache-ttl: 3600 # seconds
```

## Benefits

### 1. **Content Discovery**
- Location-based story recommendations
- Regional content filtering
- Geographic content analytics

### 2. **User Experience**
- Personalized content based on location
- Local story discovery
- Regional engagement features

### 3. **Analytics**
- Geographic engagement patterns
- Location-based user behavior
- Regional content performance

### 4. **Content Moderation**
- Location-aware content filtering
- Regional compliance requirements
- Geographic content policies

## Security Considerations

### 1. **Privacy**
- Coordinate data handling
- Location data retention
- User consent for location tracking

### 2. **Data Protection**
- Secure coordinate transmission
- Location data encryption
- GDPR compliance for location data

### 3. **API Security**
- Coordinate validation
- Rate limiting for geocoding
- API key management for real services

## Monitoring

### 1. **Metrics to Track**
- Location extraction success rate
- Geocoding API performance
- Location data accuracy
- Coordinate validation failures

### 2. **Alerts**
- High geocoding failure rates
- Invalid coordinate patterns
- Location service downtime
- Unusual location patterns

### 3. **Logging**
- Coordinate extraction logs
- Geocoding service calls
- Location metadata updates
- Error handling events 