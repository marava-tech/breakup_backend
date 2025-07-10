# Comment Analysis System

## Overview

The Comment Analysis System automatically detects and flags hateful/negative comments using AI analysis. It runs every 10 minutes to analyze recent comments and marks inappropriate content as inactive.

## Features

### 🔄 Automated Analysis
- **Scheduled Task**: Runs every 10 minutes
- **Time Window**: Analyzes comments from the last 10 minutes
- **No Missed Comments**: Ensures all recent comments are processed
- **Automatic Flagging**: Marks negative comments as `active = false`

### 🤖 AI Integration
- **Current Implementation**: Basic keyword-based analysis
- **Extensible**: Easy to integrate with external AI services (OpenAI, Google AI, etc.)
- **Fallback**: Local analysis if external service fails

### 📊 Analysis Criteria
- **Negative Words**: Hate speech, offensive language
- **Shouting**: Excessive use of capital letters (>70%)
- **Excessive Punctuation**: Multiple exclamation/question marks
- **Repeated Characters**: Patterns like "hellllloooooo"
- **Sentiment Analysis**: Positive vs negative word balance

## API Endpoints

### Admin Only Endpoints

#### 1. Manual Comment Analysis
```http
POST /api/comment-analysis/analyze/{commentId}
Authorization: Bearer <admin-token>
```

**Response:**
```json
{
  "commentId": "comment123",
  "isPositive": false,
  "status": "FLAGGED",
  "message": "Comment flagged as negative/hateful"
}
```

#### 2. Get Analysis Statistics
```http
GET /api/comment-analysis/stats
Authorization: Bearer <admin-token>
```

**Response:**
```json
{
  "totalComments": 25,
  "activeComments": 22,
  "inactiveComments": 3,
  "analysisTime": "2024-01-15T10:30:00"
}
```

#### 3. Trigger Manual Analysis
```http
POST /api/comment-analysis/trigger-analysis
Authorization: Bearer <admin-token>
```

**Response:**
```json
{
  "status": "SUCCESS",
  "message": "Comment analysis triggered successfully",
  "timestamp": 1705312200000
}
```

## Configuration

### Scheduling
- **Frequency**: Every 10 minutes (600,000 milliseconds)
- **Time Window**: Last 10 minutes of comments
- **Enable**: `@EnableScheduling` in main application

### AI Service Integration

#### Current Implementation (CommentAIService)
```java
@Service
public class CommentAIService implements AIService {
    // Basic keyword-based analysis
    // Replace with your external AI service
}
```

#### External AI Service Integration
```java
// Example with OpenAI
private boolean callExternalAIService(String commentText) {
    // Make HTTP request to OpenAI API
    // Parse response and return result
}
```

## Database Schema

### Comment Model Updates
```java
public class Comment {
    // ... existing fields ...
    private final boolean active = true; // New field for moderation
}
```

### Repository Methods
```java
// Find comments from last 10 minutes
List<Comment> findByCreatedAtAfter(LocalDateTime fromTime);

// Active comments only
List<Comment> findByStoryIdAndParentIdIsNullAndActiveTrue(String storyId);
Page<Comment> findByStoryIdAndParentIdIsNullAndActiveTrue(String storyId, Pageable pageable);
List<Comment> findByParentIdAndActiveTrue(String parentId);
```

## Implementation Details

### 1. Scheduled Task
```java
@Scheduled(fixedRate = 600000) // 10 minutes
public void analyzeRecentComments() {
    LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
    List<Comment> recentComments = commentRepository.findByCreatedAtAfter(tenMinutesAgo);
    
    for (Comment comment : recentComments) {
        boolean isPositive = aiService.analyzeComment(comment.getText());
        if (!isPositive) {
            comment.setActive(false);
            commentRepository.save(comment);
        }
    }
}
```

### 2. Comment Filtering
All comment retrieval methods now filter out inactive comments:
- `getCommentsByStory()` - Only active comments
- `getAllCommentsByStory()` - Only active comments
- `getCommentCount()` - Count only active comments
- `getCommentsByUser()` - Only active comments

### 3. Analysis Logic
```java
// Check multiple criteria
boolean isNegative = negativeWordCount > positiveWordCount || 
                    isShouting || 
                    hasExcessivePunctuation || 
                    hasRepeatedChars ||
                    sentimentScore < -1;
```

## Monitoring and Logging

### Log Levels
- **INFO**: Analysis start/completion, statistics
- **WARN**: Flagged negative comments
- **ERROR**: Analysis failures, service errors

### Example Logs
```
INFO  - Starting scheduled comment analysis...
INFO  - Found 5 comments to analyze from the last 10 minutes
WARN  - Flagged negative comment - ID: comment123, Text: 'This is terrible and I hate it'
INFO  - Comment analysis completed - Processed: 5, Flagged: 1
```

## Integration with External AI Services

### OpenAI Integration Example
```java
@Service
public class OpenAICommentService implements AIService {
    
    @Value("${openai.api.key}")
    private String apiKey;
    
    @Override
    public boolean analyzeComment(String commentText) {
        // Make HTTP request to OpenAI API
        // Parse response
        // Return true for positive, false for negative
    }
}
```

### Google AI Integration Example
```java
@Service
public class GoogleAICommentService implements AIService {
    
    @Override
    public boolean analyzeComment(String commentText) {
        // Use Google AI API for sentiment analysis
        // Return true for positive, false for negative
    }
}
```

## Security Considerations

### Admin Access
- All analysis endpoints require `ADMIN` authority
- Manual analysis triggers are restricted to admins
- Statistics are admin-only

### Data Privacy
- Comment text is logged for debugging (truncated to 50 chars)
- No personal information is exposed in logs
- Analysis results are stored securely

## Performance Optimization

### Batch Processing
- Process multiple comments in single batch
- Use async processing for large volumes
- Implement rate limiting for external AI calls

### Caching
- Cache analysis results for similar comments
- Implement result caching to reduce AI service calls
- Use Redis for distributed caching

## Troubleshooting

### Common Issues

#### 1. Scheduled Task Not Running
- Check if `@EnableScheduling` is enabled
- Verify application startup logs
- Check for exceptions in scheduled method

#### 2. Comments Not Being Analyzed
- Verify comment creation timestamps
- Check repository query results
- Ensure comments are within 10-minute window

#### 3. AI Service Integration Issues
- Check API keys and endpoints
- Verify network connectivity
- Implement proper error handling and fallbacks

### Debug Commands
```bash
# Check scheduled task logs
docker logs breakup-stories-api | grep "comment analysis"

# Trigger manual analysis
curl -X POST /api/comment-analysis/trigger-analysis \
  -H "Authorization: Bearer <admin-token>"

# Get analysis statistics
curl -X GET /api/comment-analysis/stats \
  -H "Authorization: Bearer <admin-token>"
```

## Future Enhancements

### 1. Advanced AI Models
- Integrate with GPT-4 for better analysis
- Use specialized content moderation APIs
- Implement multi-language support

### 2. User Feedback
- Allow users to report comments
- Implement appeal process for flagged comments
- Add user reputation system

### 3. Real-time Analysis
- Analyze comments immediately upon creation
- Implement webhook-based processing
- Add real-time notifications for admins

### 4. Analytics Dashboard
- Create admin dashboard for moderation
- Add trend analysis and reporting
- Implement automated alerts for unusual activity 