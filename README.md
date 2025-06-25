# Breakup Stories Backend API

A clean and modular Spring Boot 3.2 application for managing breakup stories with MongoDB persistence, featuring JWT authentication and comprehensive user management.

## 🚀 Tech Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Data MongoDB**
- **Spring Security**
- **JWT Authentication**
- **Lombok**
- **Spring Web**
- **Spring Validation**
- **Spring Boot DevTools**
- **Swagger/OpenAPI 3**

## 📂 Project Structure

```
src/main/java/com/breakupstories/
├── BreakupStoriesApplication.java
├── config/
│   ├── MongoConfig.java
│   ├── OpenApiConfig.java
│   ├── SecurityConfig.java
│   ├── WebConfig.java
│   └── JwtAuthenticationFilter.java
├── controller/
│   ├── AuthController.java
│   ├── StoryController.java
│   ├── UserController.java
│   ├── FeedbackController.java
│   └── AuditController.java
├── dto/
│   ├── AuthResponse.java
│   ├── CreateStoryRequest.java
│   ├── PagedResponse.java
│   ├── StoryResponse.java
│   ├── UserRequest.java
│   ├── UserResponse.java
│   ├── FeedbackRequest.java
│   ├── FeedbackResponse.java
│   ├── AuditRequest.java
│   └── AuditResponse.java
├── model/
│   ├── Audit.java
│   ├── Bookmark.java
│   ├── Comment.java
│   ├── Content.java
│   ├── Emotion.java
│   ├── Feedback.java
│   ├── Keyword.java
│   ├── Like.java
│   ├── Story.java
│   └── User.java
├── repository/
│   ├── AuditRepository.java
│   ├── BookmarkRepository.java
│   ├── CommentRepository.java
│   ├── FeedbackRepository.java
│   ├── LikeRepository.java
│   ├── StoryRepository.java
│   └── UserRepository.java
└── service/
    ├── JwtService.java
    ├── StoryService.java
    ├── UserService.java
    ├── FeedbackService.java
    └── AuditService.java
```

## 🏗️ Features

### Core Entities
- **User**: Authentication and profile management
- **Story**: Main content with audio, text, images, and metadata
- **Content**: Flexible content types (TEXT, IMAGE, VIDEO)
- **Emotion**: Emotional analysis with scores
- **Keyword**: Tagged keywords for categorization
- **Like**: User story interactions
- **Bookmark**: User story bookmarks
- **Comment**: Nested comment system with replies
- **Feedback**: User feedback with different tones
- **Audit**: Change tracking and logging

### Authentication & Security
- **JWT Tokens**: Stateless authentication with JWT
- **Spring Security**: Comprehensive security configuration
- **Role-based Access**: Admin and user role management
- **Frontend OAuth Integration**: Ready for frontend OAuth2 implementation

### API Endpoints

#### Authentication
- `POST /api/auth/login` - Login with OAuth data from frontend
- `GET /api/auth/me` - Get current user information
- `POST /api/auth/refresh` - Refresh JWT token

#### Users
- `POST /api/users` - Create a new user
- `GET /api/users` - Get paginated users (Admin only)
- `GET /api/users/{id}` - Get user by ID
- `GET /api/users/email/{email}` - Get user by email
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user (Admin only)

#### Stories
- `POST /api/stories` - Create a new story (Authenticated)
- `GET /api/stories` - Get paginated stories
- `GET /api/stories/{id}` - Get story by ID

#### Feedbacks
- `POST /api/feedbacks` - Create a new feedback (Authenticated)
- `GET /api/feedbacks` - Get paginated feedbacks
- `GET /api/feedbacks/story/{storyId}` - Get feedbacks by story
- `GET /api/feedbacks/user/{userId}` - Get feedbacks by user
- `GET /api/feedbacks/{feedbackId}` - Get feedback by ID
- `PUT /api/feedbacks/{feedbackId}` - Update feedback (Owner only)
- `DELETE /api/feedbacks/{feedbackId}` - Delete feedback (Owner only)

#### Audits (Admin Only)
- `POST /api/audits` - Create a new audit entry
- `GET /api/audits` - Get paginated audit entries
- `GET /api/audits/user/{userId}` - Get audits by user
- `GET /api/audits/entity-type/{entityType}` - Get audits by entity type
- `GET /api/audits/entity/{entityId}` - Get audits by entity ID
- `GET /api/audits/{auditId}` - Get audit by ID
- `DELETE /api/audits/{auditId}` - Delete audit entry

## 🛠️ Setup Instructions

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- MongoDB 4.4+ (or Docker)

### Environment Variables
Create a `.env` file or set environment variables:
```bash
JWT_SECRET=your-super-secret-jwt-key
```

### Running with Docker MongoDB
```bash
# Start MongoDB
docker run -d --name mongodb -p 27017:27017 mongo:latest

# Build and run the application
mvn clean install
mvn spring-boot:run
```

### Running Locally
1. **Install MongoDB** locally or use Docker
2. **Clone the repository**
3. **Build the project**:
   ```bash
   mvn clean install
   ```
4. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```

## 📖 API Documentation

Once the application is running, you can access:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs

## 🔐 Authentication Flow

### Frontend OAuth2 Integration
1. Frontend implements Google OAuth2 flow
2. After successful OAuth2, frontend calls `/api/auth/login` with user data
3. Backend creates/updates user and returns JWT token
4. Frontend stores JWT token and uses it for authenticated requests

### JWT Token Usage
Include the JWT token in the Authorization header:
```
Authorization: Bearer <your-jwt-token>
```

## 🧪 Testing

Run the tests with:
```bash
mvn test
```

## 📝 Example Usage

### Login with OAuth Data
```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "email=user@example.com&name=John%20Doe&profileImageUrl=https://example.com/avatar.jpg"
```

### Create a Story (with JWT)
```bash
curl -X POST http://localhost:8080/api/stories \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -d '{
    "title": "My Breakup Story",
    "audioUrl": "https://example.com/audio.mp3",
    "contents": [
      {
        "type": "TEXT",
        "data": "This is my story...",
        "orderIndex": 1
      }
    ],
    "tags": ["breakup", "healing"],
    "emotions": [
      {
        "type": "SAD",
        "score": 0.8
      }
    ]
  }'
```

### Get Current User
```bash
curl -H "Authorization: Bearer <your-jwt-token>" \
  http://localhost:8080/api/auth/me
```

### Create User
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "profileImageUrl": "https://example.com/avatar.jpg"
  }'
```

### Create Feedback
```bash
curl -X POST http://localhost:8080/api/feedbacks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -d '{
    "storyId": "story-id-here",
    "tone": "POSITIVE",
    "contents": [
      {
        "type": "TEXT",
        "data": "This story really helped me heal",
        "orderIndex": 1
      }
    ]
  }'
```

### Get Feedbacks by Story
```bash
curl -H "Authorization: Bearer <your-jwt-token>" \
  "http://localhost:8080/api/feedbacks/story/story-id-here?page=0&size=10"
```

### Create Audit Entry (Admin Only)
```bash
curl -X POST http://localhost:8080/api/audits \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-jwt-token>" \
  -d '{
    "userId": "user-id-here",
    "entityType": "STORY",
    "actionType": "CREATE",
    "entityId": "story-id-here"
  }'
```

### Get Audits by Entity Type (Admin Only)
```bash
curl -H "Authorization: Bearer <admin-jwt-token>" \
  "http://localhost:8080/api/audits/entity-type/STORY?page=0&size=10"
```

## 🔧 Configuration

The application uses `application.yml` for configuration. Key settings:

- **MongoDB**: Configured for localhost:27017
- **Server**: Runs on port 8080
- **CORS**: Enabled for all origins
- **Swagger**: Available at `/swagger-ui.html`
- **JWT**: Token generation and validation

## 🏗️ Architecture

The project follows clean architecture principles:

- **Controllers**: Handle HTTP requests and responses
- **Services**: Contain business logic
- **Repositories**: Data access layer
- **DTOs**: Data transfer objects for API contracts
- **Models**: MongoDB document entities
- **Config**: Global configuration and beans
- **Security**: JWT authentication ready for frontend OAuth2

## 📄 License

This project is licensed under the MIT License.