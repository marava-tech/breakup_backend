# Withdrawal Eligibility API

## Overview

The Withdrawal Eligibility API allows you to check if a user has uploaded an active story with creation type "UPLOADED" and is eligible for coin withdrawal. This API is designed to prevent users from withdrawing coins without contributing content to the platform.

## API Endpoint

### Check Withdrawal Eligibility
```http
GET /api/stories/withdrawal-eligibility
```

**Authentication:** Required (Bearer token)

**Description:** Check if the authenticated user has uploaded an active story and is eligible for coin withdrawal.

## Request

### Headers
```
Authorization: Bearer <your-jwt-token>
Content-Type: application/json
```

### Example Request
```bash
curl -X GET "http://localhost:8080/api/stories/withdrawal-eligibility" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Response

### Success Response (200 OK)

**User has uploaded an active story:**
```json
{
  "userId": "user123",
  "hasUploadedActiveStory": true,
  "message": "User has uploaded an active story and is eligible for withdrawal"
}
```

**User has not uploaded an active story:**
```json
{
  "userId": "user123",
  "hasUploadedActiveStory": false,
  "message": "User has not uploaded an active story and is not eligible for withdrawal"
}
```

### Error Response (401 Unauthorized)
```json
{
  "error": "login required"
}
```

## Business Logic

### Eligibility Criteria
A user is eligible for withdrawal if they have:
1. **Active Story**: A story with status `ACTIVE`
2. **UPLOADED Creation Type**: The story must have creation type `UPLOADED` (not `WRITTEN`)
3. **Ownership**: The story must belong to the authenticated user

### Story Status Types
- `ACTIVE`: Story is live and available to users
- `INACTIVE`: Story is not available
- `FAILED`: Story processing failed
- `REJECTED`: Story was rejected
- `PROCESSING`: Story is being processed
- `UPLOAD_PENDING`: Story upload is pending

### Creation Types
- `UPLOADED`: Story created by uploading an audio file
- `WRITTEN`: Story created by typing text

## Implementation Details

### Database Query
The API uses a MongoDB query to check for active uploaded stories:
```java
boolean hasUploadedActiveStory = storyRepository.existsByUserIdAndStatusAndCreationType(
    userId, 
    Story.StoryStatus.ACTIVE, 
    Story.CreationType.UPLOADED
);
```

### Repository Method
```java
// Check if user has an active story with UPLOADED creation type
boolean existsByUserIdAndStatusAndCreationType(String userId, Story.StoryStatus status, Story.CreationType creationType);
```

### Service Method
```java
public WithdrawalEligibilityResponse checkWithdrawalEligibility(String userId) {
    boolean hasUploadedActiveStory = storyRepository.existsByUserIdAndStatusAndCreationType(
        userId, 
        Story.StoryStatus.ACTIVE, 
        Story.CreationType.UPLOADED
    );
    
    String message = hasUploadedActiveStory 
        ? "User has uploaded an active story and is eligible for withdrawal"
        : "User has not uploaded an active story and is not eligible for withdrawal";
    
    return WithdrawalEligibilityResponse.builder()
            .userId(userId)
            .hasUploadedActiveStory(hasUploadedActiveStory)
            .message(message)
            .build();
}
```

## Use Cases

### 1. Withdrawal Flow
```javascript
// Check eligibility before allowing withdrawal
const response = await fetch('/api/stories/withdrawal-eligibility', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});

const { hasUploadedActiveStory } = await response.json();

if (hasUploadedActiveStory) {
  // Allow withdrawal
  showWithdrawalForm();
} else {
  // Show message that user needs to upload a story first
  showUploadStoryPrompt();
}
```

### 2. Dashboard Display
```javascript
// Show eligibility status on user dashboard
const eligibility = await checkWithdrawalEligibility();

if (eligibility.hasUploadedActiveStory) {
  document.getElementById('withdrawal-status').textContent = '✅ Eligible for withdrawal';
} else {
  document.getElementById('withdrawal-status').textContent = '❌ Upload a story to withdraw';
}
```

### 3. Mobile App Integration
```kotlin
// Android app integration
val response = apiService.checkWithdrawalEligibility()
if (response.hasUploadedActiveStory) {
    // Enable withdrawal button
    withdrawalButton.isEnabled = true
} else {
    // Disable withdrawal button and show message
    withdrawalButton.isEnabled = false
    showMessage("Upload a story to withdraw coins")
}
```

## Security Considerations

### 1. Authentication Required
- All requests must include a valid JWT token
- User can only check their own eligibility
- No admin override for this endpoint

### 2. Data Privacy
- Only returns boolean status and user ID
- No sensitive story information is exposed
- No story content or metadata is returned

### 3. Rate Limiting
- Consider implementing rate limiting to prevent abuse
- Cache results for a short period to reduce database load

## Testing

### Test Cases

1. **User with active uploaded story**
   - Expected: `hasUploadedActiveStory: true`

2. **User with only written stories**
   - Expected: `hasUploadedActiveStory: false`

3. **User with inactive uploaded stories**
   - Expected: `hasUploadedActiveStory: false`

4. **User with no stories**
   - Expected: `hasUploadedActiveStory: false`

5. **Unauthenticated request**
   - Expected: 401 Unauthorized

### Test Commands
```bash
# Test with valid token
curl -X GET "http://localhost:8080/api/stories/withdrawal-eligibility" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Test without token
curl -X GET "http://localhost:8080/api/stories/withdrawal-eligibility"
```

## Integration with Withdrawal System

### Withdrawal Controller Integration
```java
@PostMapping("/withdraw")
public ResponseEntity<WithdrawalResponse> withdrawCoins(
        @RequestBody WithdrawalRequest request,
        Authentication authentication) {
    
    // Check eligibility first
    String userId = getUserIdFromAuthentication(authentication);
    WithdrawalEligibilityResponse eligibility = storyService.checkWithdrawalEligibility(userId);
    
    if (!eligibility.isHasUploadedActiveStory()) {
        throw new WithdrawalNotAllowedException("User must upload an active story before withdrawing");
    }
    
    // Proceed with withdrawal
    return withdrawalService.processWithdrawal(request, userId);
}
```

## Benefits

### 1. **Content Quality Control**
- Ensures users contribute content before withdrawing
- Prevents users from gaming the system
- Encourages active participation

### 2. **Platform Growth**
- Motivates users to upload stories
- Increases platform content
- Builds user engagement

### 3. **Fair System**
- Only rewards users who contribute
- Prevents abuse of the reward system
- Maintains platform integrity

## Future Enhancements

### Potential Improvements
1. **Multiple Story Requirement**: Require multiple uploaded stories
2. **Story Quality Metrics**: Consider story engagement (likes, views)
3. **Time-based Requirements**: Require stories to be active for a minimum time
4. **Content Moderation**: Ensure stories meet community guidelines
5. **Analytics**: Track withdrawal eligibility patterns

### Additional Endpoints
1. **Bulk Eligibility Check**: Check multiple users at once
2. **Eligibility History**: Track when users became eligible
3. **Detailed Requirements**: Show what's needed to become eligible 