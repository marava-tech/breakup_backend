# Feedback Functionality Documentation

## Overview

The feedback system allows users to report issues, provide suggestions, and give general feedback about the application. Users can also attach files (images, documents, etc.) to their feedback for better context.

## Features

### 1. Multiple Feedback Types
- **STORY_FEEDBACK**: Story-specific feedback (existing functionality)
- **BUG_REPORT**: Report technical issues or bugs
- **FEATURE_REQUEST**: Request new features
- **SUGGESTION**: General suggestions for improvement
- **COMPLAINT**: Complaints about app or content
- **GENERAL**: General feedback

### 2. Feedback Status Tracking
- **PENDING**: New feedback, not yet reviewed
- **IN_REVIEW**: Being reviewed by admin
- **RESOLVED**: Issue resolved or suggestion implemented
- **CLOSED**: Feedback closed without action
- **REJECTED**: Feedback rejected

### 3. File Attachments
Users can attach files to their feedback using multipart form data with the key "file".

### 4. Admin Response System
Admins can respond to feedback and update its status.

## API Endpoints

### Create Feedback with File Upload
```http
POST /api/feedbacks
Authorization: Bearer <token>
Content-Type: multipart/form-data

Form Data:
- feedback: {"type": "BUG_REPORT", "subject": "App crashes on login", "description": "The app crashes every time I try to login"}
- file: [binary file data] (optional)
```

### Get All Feedbacks
```http
GET /api/feedbacks?page=0&size=10
```

### Get Feedbacks by Type
```http
GET /api/feedbacks/types/BUG_REPORT?page=0&size=10
```

### Get Feedbacks by Status
```http
GET /api/feedbacks/status/PENDING?page=0&size=10
```

### Get My Feedbacks
```http
GET /api/feedbacks/my-feedbacks?page=0&size=10
Authorization: Bearer <token>
```

### Update Feedback with File Upload
```http
PUT /api/feedbacks/{feedbackId}
Authorization: Bearer <token>
Content-Type: multipart/form-data

Form Data:
- feedback: {"type": "BUG_REPORT", "subject": "Updated subject", "description": "Updated description"}
- file: [binary file data] (optional)
```

### Update Feedback Status (Admin)
```http
PUT /api/feedbacks/{feedbackId}/status
Content-Type: application/json

{
  "status": "RESOLVED",
  "adminResponse": "This issue has been fixed in version 2.1.0"
}
```

## Request/Response Examples

### Story-Specific Feedback
```json
{
  "type": "STORY_FEEDBACK",
  "storyId": "story123",
  "subject": "Great story!",
  "description": "Loved the ending and the way it was narrated."
}
```

### Bug Report with File
```json
// Form data: feedback
{
  "type": "BUG_REPORT",
  "subject": "Audio not playing",
  "description": "When I try to play any story audio, it doesn't work"
}

// Form data: file
[Binary file data - screenshot, log file, etc.]
```

### Feature Request
```json
{
  "type": "FEATURE_REQUEST",
  "subject": "Dark mode support",
  "description": "Please add dark mode to the app for better user experience"
}
```

## Response Format
```json
{
  "id": "feedback123",
  "storyId": null,
  "userId": "user456",
  "username": "John Doe",
  "type": "BUG_REPORT",
  "subject": "App crashes on login",
  "description": "The app crashes every time I try to login",
  "fileUrl": "https://example.com/uploads/screenshot.jpg",
  "status": "PENDING",
  "adminResponse": null,
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

## Validation Rules

### Story-Specific Feedback
- `type` must be "STORY_FEEDBACK"
- `storyId` is required

### General Feedback
- `type` must be one of: BUG_REPORT, FEATURE_REQUEST, SUGGESTION, COMPLAINT, GENERAL
- `subject` is required
- `description` is required

### File Upload
- File is optional
- File should be uploaded with key "file"
- Supported file types depend on your upload service configuration

## Admin Features

### Status Management
Admins can update feedback status and provide responses:
- Move feedback through different statuses
- Add admin responses to provide updates
- Close or reject inappropriate feedback

### Filtering and Search
- Filter by feedback type
- Filter by status
- View feedbacks by user
- View feedbacks by story

## Security Considerations

1. **Authentication**: All endpoints require valid JWT token
2. **Authorization**: Users can only update/delete their own feedback
3. **Admin Access**: Status updates require admin privileges
4. **Input Validation**: All inputs are validated to prevent malicious content
5. **File Upload Security**: Files are validated and processed securely

## Integration with Existing Systems

The feedback system integrates with:
- **User Management**: Links feedback to user accounts
- **Story System**: Supports story-specific feedback
- **File Upload Service**: Supports file attachments
- **Notification System**: Can notify users of status updates (future enhancement)

## Frontend Integration Example

### JavaScript/React Example
```javascript
const createFeedback = async (feedbackData, file) => {
  const formData = new FormData();
  
  // Add feedback data as JSON string
  formData.append('feedback', JSON.stringify(feedbackData));
  
  // Add file if provided
  if (file) {
    formData.append('file', file);
  }
  
  const response = await fetch('/api/feedbacks', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`
    },
    body: formData
  });
  
  return response.json();
};

// Usage
const feedbackData = {
  type: 'BUG_REPORT',
  subject: 'App crashes on login',
  description: 'The app crashes every time I try to login'
};

const file = document.getElementById('fileInput').files[0];
const result = await createFeedback(feedbackData, file);
```

### cURL Example
```bash
curl -X POST http://localhost:8080/api/feedbacks \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "feedback={\"type\":\"BUG_REPORT\",\"subject\":\"App crashes\",\"description\":\"App crashes on login\"}" \
  -F "file=@screenshot.png"
```

## Future Enhancements

1. **Email Notifications**: Notify users when their feedback status changes
2. **Feedback Analytics**: Track common issues and feature requests
3. **Priority Levels**: Add priority levels for different feedback types
4. **Feedback Categories**: Add subcategories for better organization
5. **Auto-assignment**: Automatically assign feedback to appropriate team members
6. **File Type Validation**: Add specific file type restrictions
7. **File Size Limits**: Implement file size restrictions 