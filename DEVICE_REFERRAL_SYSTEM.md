# Device-Based Referral System

## Overview

The Device-Based Referral System prevents referral abuse by tracking referrals at the device level instead of just the user level. This prevents users from creating multiple accounts on the same device to claim referral rewards.

## Problem Solved

Previously, users could:
1. Create an account with a referral code
2. Log out and create another account with the same referral code
3. Repeat this process to abuse the referral system

Now, each Android device can only use one referral code, regardless of how many accounts are created on that device.

## Implementation Details

### New Components

#### 1. DeviceReferral Model
```java
public class DeviceReferral {
    private String id;
    private String deviceId;           // Android device ID
    private String referralCode;       // Referral code used
    private String referrerUserId;     // User who provided the referral code
    private String referredUserId;     // User who used the referral code
    private boolean rewardClaimed;     // Whether the referral reward has been claimed
    private LocalDateTime rewardClaimedAt; // When the reward was claimed
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### 2. DeviceReferralRepository
- `existsByDeviceId(String deviceId)`: Check if device has used a referral
- `findByDeviceId(String deviceId)`: Get device referral details
- `countByReferrerUserId(String referrerUserId)`: Count referrals by referrer
- And other query methods for device referral tracking

#### 3. Updated User Model
```java
public class User {
    // ... existing fields
    private String deviceId; // Android device ID for referral tracking
}
```

#### 4. Updated UserRequest DTO
```java
public class UserRequest {
    // ... existing fields
    private String deviceId; // New field for device tracking
}
```

#### 5. Updated UserResponse DTO
```java
public class UserResponse {
    // ... existing fields
    private String deviceId; // Android device ID for referral tracking
}
```

#### 6. Updated UserRepository
- `findByDeviceId(String deviceId)`: Find user by device ID
- `existsByDeviceId(String deviceId)`: Check if device ID exists
- `findAllByDeviceId(String deviceId)`: Find all users by device ID

### API Endpoints

#### Check Device Referral Status (Public)
```http
GET /api/auth/device-referral-status/{deviceId}
```

**Response:**
```json
{
  "deviceId": "android_device_123",
  "hasUsedReferral": true
}
```

#### Get Users by Device ID (Admin)
```http
GET /api/admin/users/device/{deviceId}
```

**Response:**
```json
{
  "deviceId": "android_device_123",
  "userCount": 2,
  "users": [
    {
      "id": "user123",
      "name": "John Doe",
      "email": "john@example.com",
      "deviceId": "android_device_123",
      // ... other user fields
    },
    {
      "id": "user456",
      "name": "Jane Smith",
      "email": "jane@example.com",
      "deviceId": "android_device_123",
      // ... other user fields
    }
  ]
}
```

### Service Methods

#### RewardService Updates
- `processReferral(String newUserId, String referralCode, String deviceId)`: Device-based referral processing
- `hasDeviceUsedReferral(String deviceId)`: Check if device has used referral
- `getDeviceReferral(String deviceId)`: Get device referral details

#### UserService Updates
- Updated `createUserAfterOtpVerification()` to store deviceId in User entity
- Passes deviceId to referral processing

## How It Works

1. **Registration Process**:
   - Frontend collects Android device ID during registration
   - Device ID is sent along with referral code in registration request
   - System stores device ID in User entity
   - System checks if device has already used a referral code
   - If not, processes referral and creates device referral record
   - If yes, skips referral processing

2. **Device Tracking**:
   - Each device can only use one referral code
   - Device referral records are permanent
   - No way to reset or reuse device for referrals
   - Device ID is stored with each user for audit purposes

3. **Backward Compatibility**:
   - Existing user-based referral system remains intact
   - Old referrals without device ID are still valid
   - New system only applies to registrations with device ID

## Frontend Integration

### Android App
```kotlin
// Get device ID
val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

// Check device referral status before registration (public endpoint)
val response = apiService.checkDeviceReferralStatus(deviceId)
if (!response.hasUsedReferral) {
    // Allow referral code input
    showReferralCodeInput()
} else {
    // Hide referral code input
    hideReferralCodeInput()
}

// Send registration request with device ID
val registrationRequest = RegistrationRequest(
    name = name,
    email = email,
    otp = otp,
    gender = gender,
    age = age,
    preferredStoryLanguage = language,
    deviceId = deviceId,
    referralCode = referralCode
)
```

### Web App
```javascript
// For web apps, you might use a different identifier
const deviceId = generateDeviceId(); // Browser fingerprint or stored ID

// Check device referral status (public endpoint)
const response = await fetch(`/api/auth/device-referral-status/${deviceId}`);
const { hasUsedReferral } = await response.json();

if (!hasUsedReferral) {
    // Show referral code input
    document.getElementById('referralCodeInput').style.display = 'block';
} else {
    // Hide referral code input
    document.getElementById('referralCodeInput').style.display = 'none';
}

// Send registration request with device ID
const registrationRequest = {
    name: name,
    email: email,
    otp: otp,
    gender: gender,
    age: age,
    preferredStoryLanguage: language,
    deviceId: deviceId,
    referralCode: referralCode
};
```

### API Registration Flow
```bash
# 1. Send OTP for registration
curl -X POST "http://localhost:8080/api/auth/send-otp-registration" \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com"}'

# 2. Verify OTP and register with device ID
curl -X POST "http://localhost:8080/api/auth/verify-otp-registration" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "user@example.com",
    "otp": "123456",
    "gender": "MALE",
    "age": 25,
    "preferredStoryLanguage": "ENGLISH",
    "deviceId": "android_device_123",
    "referralCode": "ABC12345"
  }'
```

## Security Considerations

1. **Device ID Validation**: System validates device ID is not null/empty
2. **Permanent Tracking**: Device referral records cannot be deleted or modified
3. **Audit Trail**: All device referrals are logged with timestamps
4. **Maximum Limits**: Still respects maximum referrals per user configuration
5. **User-Device Association**: Device ID is stored with each user for audit purposes

## Database Schema

### users Collection Updates
```javascript
{
  "_id": ObjectId("..."),
  // ... existing fields
  "deviceId": "android_device_123",
  "referralCode": "ABC12345",
  "referredBy": "user789",
  "coinBalance": 125
}
```

### device_referrals Collection
```javascript
{
  "_id": ObjectId("..."),
  "deviceId": "android_device_123",
  "referralCode": "ABC12345",
  "referrerUserId": "user123",
  "referredUserId": "user456",
  "rewardClaimed": true,
  "rewardClaimedAt": ISODate("2024-01-15T10:30:00Z"),
  "createdAt": ISODate("2024-01-15T10:30:00Z"),
  "updatedAt": ISODate("2024-01-15T10:30:00Z")
}
```

## Benefits

1. **Prevents Abuse**: Users cannot create multiple accounts on same device
2. **Fair System**: Each device gets one referral opportunity
3. **Audit Trail**: Complete tracking of device referrals
4. **Backward Compatible**: Existing referrals remain valid
5. **Configurable**: Still respects maximum referrals per user
6. **User-Device Tracking**: Device ID stored with each user for admin purposes

## Migration Notes

- Existing users and referrals are unaffected
- New registrations require device ID for referral processing
- Device referral records are created automatically
- Device ID is stored with each new user
- No manual migration required

## Testing

### Test Cases
1. **New Device Registration**: Should allow referral code
2. **Same Device, New Account**: Should reject referral code
3. **Different Device**: Should allow referral code
4. **No Device ID**: Should skip referral processing
5. **Invalid Device ID**: Should skip referral processing
6. **Device ID Storage**: Should store device ID with user

### API Testing
```bash
# Check device status (public endpoint) - Example with specific device ID
curl -X GET "http://localhost:8080/api/auth/device-referral-status/7cfc5c71d3ce0181"

# Check device status (public endpoint) - Generic example
curl -X GET "http://localhost:8080/api/auth/device-referral-status/android_device_123"

# Send OTP for registration
curl -X POST "http://localhost:8080/api/auth/send-otp-registration" \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com"}'

# Register with device ID and referral code
curl -X POST "http://localhost:8080/api/auth/verify-otp-registration" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "email": "test@example.com",
    "otp": "123456",
    "gender": "MALE",
    "age": 25,
    "preferredStoryLanguage": "ENGLISH",
    "deviceId": "android_device_123",
    "referralCode": "ABC12345"
  }'

# Get users by device ID (admin)
curl -X GET "http://localhost:8080/api/admin/users/device/android_device_123" \
  -H "Authorization: Bearer <admin-token>"
```

## Admin Features

### Device User Tracking
- Admin can view all users associated with a specific device ID
- Helps identify potential abuse patterns
- Provides audit trail for device-user associations

### Device Referral Analytics
- Track which devices have used referrals
- Monitor referral patterns across devices
- Identify suspicious device activity 