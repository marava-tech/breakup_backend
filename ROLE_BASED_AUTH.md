# Role-Based Authentication Implementation

This document explains the role-based authentication system implemented in the Breakup Stories application.

## Overview

The application now supports role-based authentication with two roles:
- **USER**: Default role for all regular users
- **ADMIN**: Administrative role with access to management endpoints

## Implementation Details

### 1. Role Enum
- Created `Role` enum in `src/main/java/com/breakupstories/enums/Role.java`
- Contains `USER` and `ADMIN` roles

### 2. User Model Updates
- Added `role` field to `User` model with default value `Role.USER`
- Role is stored in MongoDB and included in API responses

### 3. DTO Updates
- Updated `UserResponse`, `UserRequest`, and `RegistrationRequest` to include role field
- Role information is now part of all user-related API responses

### 4. Authentication & Authorization
- **JWT Tokens**: Now include role information in claims
- **Spring Security**: Uses proper role authorities (`ROLE_USER`, `ROLE_ADMIN`)
- **Method Security**: `@PreAuthorize` annotations for role-based access control

### 5. Security Configuration
- Admin endpoints require `ROLE_ADMIN` authority:
  - `/api/audits/**` - Audit management
  - `/api/configs/**` - Configuration management  
  - `/api/users/**` - User management
- Regular endpoints require authentication but no specific role

## API Endpoints

### Public Endpoints (No Authentication Required)
- `/api/auth/send-otp-registration`
- `/api/auth/send-otp-login`
- `/api/auth/verify-otp-registration`
- `/api/auth/verify-otp-login`
- `/api/stories` (GET)
- `/swagger-ui/**`
- `/api-docs/**`

### User Endpoints (Requires Authentication)
- `/api/auth/me`
- `/api/auth/refresh`
- `/api/stories/**` (except GET)
- `/api/comments/**`
- `/api/likes/**`
- `/api/bookmarks/**`
- `/api/notifications/**`

### Admin Endpoints (Requires ROLE_ADMIN)
- `/api/audits/**` - View audit logs
- `/api/configs/**` - Manage system configurations
- `/api/users/**` - Manage users (view all, delete, etc.)

## Usage Examples

### 1. User Registration (Default USER Role)
```bash
curl -X POST http://localhost:8080/api/auth/verify-otp-registration \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "otp": "123456",
    "gender": "MALE",
    "age": 25,
    "preferredStoryLanguage": "ENGLISH"
  }'
```

### 2. User Registration with Specific Role
```bash
curl -X POST http://localhost:8080/api/auth/verify-otp-registration \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Admin User",
    "email": "admin@example.com",
    "otp": "123456",
    "gender": "MALE",
    "age": 30,
    "preferredStoryLanguage": "ENGLISH",
    "role": "ADMIN"
  }'
```

### 3. Accessing Admin Endpoints
```bash
# Get all users (requires ADMIN role)
curl -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer <your-jwt-token>"

# View audit logs (requires ADMIN role)
curl -X GET http://localhost:8080/api/audits \
  -H "Authorization: Bearer <your-jwt-token>"
```

## Making Yourself an Admin

To make yourself an admin, you can directly update the database:

### Option 1: Using MongoDB Shell
```bash
# Connect to MongoDB
mongo breakup_stories

# Update your user to ADMIN role
db.users.updateOne(
    { email: "your-email@example.com" },
    { $set: { role: "ADMIN" } }
);

# Verify the update
db.users.findOne({ email: "your-email@example.com" });
```

### Option 2: Using the Provided Script
1. Edit `update-to-admin.js` and replace `'your-email@example.com'` with your actual email
2. Run the script:
```bash
mongo breakup_stories update-to-admin.js
```

### Option 3: Using MongoDB Compass or Other GUI
1. Connect to your MongoDB database
2. Navigate to the `users` collection
3. Find your user document by email
4. Update the `role` field from `"USER"` to `"ADMIN"`

## Testing Role-Based Access

### Test Bypass Authentication (Development Only)
```bash
# Test as regular user
curl -H "X-BS-Authorization: true" \
     -H "X-BS-UserId: user-id-here" \
     http://localhost:8080/api/users

# Test as admin
curl -H "X-BS-Authorization: true" \
     -H "X-BS-UserId: admin-user-id-here" \
     -H "X-BS-Admin: true" \
     http://localhost:8080/api/users
```

### JWT Token Authentication
```bash
# Login to get JWT token
curl -X POST http://localhost:8080/api/auth/verify-otp-login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "your-email@example.com",
    "otp": "123456"
  }'

# Use the token for authenticated requests
curl -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer <your-jwt-token>"
```

## Security Considerations

1. **Role Assignment**: By default, all new users get the `USER` role
2. **Admin Access**: Admin role should be manually assigned through database updates
3. **Token Security**: JWT tokens include role information and are validated on each request
4. **Method Security**: Both URL-level and method-level security are implemented
5. **Development Bypass**: Test bypass authentication should be disabled in production

## Database Schema

The `users` collection now includes a `role` field:
```json
{
  "_id": "user-id",
  "name": "John Doe",
  "email": "john@example.com",
  "gender": "MALE",
  "age": 25,
  "profileImageUrl": "https://...",
  "preferredStoryLanguage": "ENGLISH",
  "role": "USER",
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z"
}
```

## Migration Notes

- Existing users without a role field will be assigned `USER` role by default
- The `@Builder.Default` annotation ensures new users get `USER` role if not specified
- JWT tokens will automatically include the user's role in claims
- All existing functionality remains unchanged for regular users 