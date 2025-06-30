# Request ID Tracking System

## Overview

The Request ID Tracking System provides comprehensive request tracing capabilities across the entire application. Each HTTP request is assigned a unique identifier that is tracked throughout the request lifecycle, enabling better debugging, monitoring, and troubleshooting.

## Features

- ✅ **Automatic Request ID Generation**: Unique IDs for every request
- ✅ **Request ID Propagation**: ID passed through all service layers
- ✅ **Enhanced Logging**: Request ID included in all log messages
- ✅ **Response Headers**: Request ID returned in response headers
- ✅ **Custom Request IDs**: Support for client-provided request IDs
- ✅ **Performance Tracking**: Request duration and timing information
- ✅ **Error Tracking**: Request ID included in error responses
- ✅ **MDC Integration**: Request ID available in logback MDC

## Architecture

### Components

1. **RequestIdGenerator**: Utility for generating unique request IDs
2. **RequestContext**: Thread-local storage for request-specific data
3. **RequestIdInterceptor**: HTTP interceptor for automatic request ID handling
4. **LoggingConfig**: MDC configuration for enhanced logging
5. **RequestIdResponse**: Response wrapper with request ID
6. **WebConfig**: Interceptor registration and configuration

### Flow Diagram

```
HTTP Request → RequestIdInterceptor
     ↓
Generate/Extract Request ID
     ↓
Set Request ID in ThreadLocal Context
     ↓
Set Request ID in MDC for Logging
     ↓
Add Request ID to Response Headers
     ↓
Process Request with Request ID Context
     ↓
Log Request Completion with Duration
     ↓
Clear Request Context
```

## Request ID Formats

### 1. Timestamped Request ID (Default)
```
req_1703123456789_a1b2c3d4
```
- Format: `req_{timestamp}_{8-char-uuid}`
- Example: `req_1703123456789_a1b2c3d4`

### 2. UUID Request ID
```
a1b2c3d4e5f67890
```
- Format: 16-character UUID without hyphens
- Example: `a1b2c3d4e5f67890`

### 3. Custom Prefix Request ID
```
story_a1b2c3d4e5f67890
```
- Format: `{prefix}_{16-char-uuid}`
- Example: `story_a1b2c3d4e5f67890`

## API Usage

### Request Headers

Clients can provide their own request ID:

```http
X-Request-ID: custom-request-123
```

If not provided, the system generates one automatically.

### Response Headers

All responses include the request ID:

```http
X-Request-ID: req_1703123456789_a1b2c3d4
```

### Response Format

For endpoints using `RequestIdResponse` wrapper:

```json
{
  "requestId": "req_1703123456789_a1b2c3d4",
  "data": {
    "id": "story123",
    "title": "Uploading...",
    "status": "PROCESSING"
  },
  "message": "Story creation initiated successfully",
  "timestamp": 1703123456789
}
```

## Implementation Details

### RequestIdGenerator

```java
public class RequestIdGenerator {
    public static String generateRequestId();
    public static String generateRequestId(String prefix);
    public static String generateTimestampedRequestId();
}
```

### RequestContext

```java
public class RequestContext {
    public static void setRequestId(String requestId);
    public static String getRequestId();
    public static void setUserId(String userId);
    public static String getUserId();
    public static void setStartTime(Long startTime);
    public static Long getStartTime();
    public static void clear();
}
```

### RequestIdInterceptor

```java
@Component
public class RequestIdInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler);
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex);
}
```

## Logging Configuration

### Log Pattern

```
%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{requestId:-NO_REQUEST_ID}] %-5level %logger{36} - %msg%n
```

### Example Log Output

```
2024-01-15 10:30:45.123 [http-nio-8080-exec-1] [req_1703123456789_a1b2c3d4] INFO  c.b.s.StoryService - Creating story for user: user123
2024-01-15 10:30:45.456 [http-nio-8080-exec-1] [req_1703123456789_a1b2c3d4] INFO  c.b.s.StoryService - Initial story created with ID: story123
2024-01-15 10:30:45.789 [http-nio-8080-exec-1] [req_1703123456789_a1b2c3d4] INFO  c.b.s.StoryService - Audio upload completed for story story123
```

## Usage Examples

### 1. Basic Request Tracking

```bash
# Make a request (system generates request ID)
curl -X POST "http://localhost:8080/api/stories" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "audio=@story.mp3"

# Response includes request ID
# X-Request-ID: req_1703123456789_a1b2c3d4
```

### 2. Custom Request ID

```bash
# Provide custom request ID
curl -X POST "http://localhost:8080/api/stories" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "X-Request-ID: my-custom-request-123" \
  -F "audio=@story.mp3"

# Response uses your request ID
# X-Request-ID: my-custom-request-123
```

### 3. Frontend Integration

```javascript
// Generate request ID for tracking
const requestId = `frontend_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;

// Make request with custom ID
const response = await fetch('/api/stories', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'X-Request-ID': requestId
  },
  body: formData
});

// Get request ID from response
const responseRequestId = response.headers.get('X-Request-ID');
console.log('Request tracked with ID:', responseRequestId);
```

### 4. Service Layer Usage

```java
@Service
public class MyService {
    
    public void processRequest() {
        String requestId = RequestContext.getRequestId();
        log.info("Processing request with ID: {}", requestId);
        
        // Your business logic here
        
        log.info("Request processing completed for ID: {}", requestId);
    }
}
```

## Monitoring and Debugging

### 1. Log Analysis

Search logs by request ID:

```bash
# Find all logs for a specific request
grep "req_1703123456789_a1b2c3d4" logs/breakup-stories.log

# Find all logs for a user
grep "user123" logs/breakup-stories.log | grep "req_"
```

### 2. Performance Monitoring

Request duration is automatically logged:

```
2024-01-15 10:30:45.123 [http-nio-8080-exec-1] [req_1703123456789_a1b2c3d4] INFO  - POST /api/stories - Request started
2024-01-15 10:30:46.456 [http-nio-8080-exec-1] [req_1703123456789_a1b2c3d4] INFO  - POST /api/stories - Request completed in 1333ms with status 201
```

### 3. Error Tracking

Errors include request ID for correlation:

```
2024-01-15 10:30:45.789 [http-nio-8080-exec-1] [req_1703123456789_a1b2c3d4] ERROR - POST /api/stories - Request failed after 666ms - Error: Audio file is required
```

## Configuration

### Application Properties

```yaml
# application.yml
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{requestId:-NO_REQUEST_ID}] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{requestId:-NO_REQUEST_ID}] %-5level %logger{36} - %msg%n"
  
  file:
    name: logs/breakup-stories.log
    
  logback:
    rollingpolicy:
      max-file-size: 100MB
      max-history: 30
```

### Interceptor Configuration

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(requestIdInterceptor)
            .addPathPatterns("/**") // Apply to all paths
            .excludePathPatterns("/health", "/actuator/**"); // Exclude health checks
}
```

## Benefits

### 1. Debugging
- **Request Correlation**: Track requests across multiple services
- **Error Isolation**: Identify specific requests that fail
- **Performance Analysis**: Measure request duration and bottlenecks

### 2. Monitoring
- **Request Flow**: Follow requests through the entire system
- **User Activity**: Track user-specific request patterns
- **System Health**: Monitor request success/failure rates

### 3. Support
- **Issue Resolution**: Quickly identify problematic requests
- **User Support**: Provide request IDs for customer support
- **Audit Trail**: Complete request history for compliance

### 4. Development
- **Testing**: Use request IDs to correlate test results
- **Development**: Debug issues in development environment
- **Staging**: Validate request flows before production

## Best Practices

### 1. Request ID Usage
- Always include request ID in log messages
- Use request ID for error correlation
- Pass request ID to external service calls

### 2. Logging
- Use structured logging with request ID
- Include request ID in error messages
- Log request start and completion

### 3. Performance
- Keep request ID generation lightweight
- Use efficient storage for request context
- Clear context after request completion

### 4. Security
- Don't expose sensitive data in request IDs
- Use secure random generation
- Validate client-provided request IDs

## Troubleshooting

### Common Issues

1. **Missing Request ID in Logs**
   - Check if interceptor is registered
   - Verify logback configuration
   - Ensure MDC is properly set up

2. **Request ID Not Propagated**
   - Check ThreadLocal usage
   - Verify async context propagation
   - Ensure context clearing

3. **Performance Impact**
   - Monitor request ID generation overhead
   - Check ThreadLocal memory usage
   - Optimize logging patterns

### Debug Commands

```bash
# Check if request ID is being generated
grep "Request started" logs/breakup-stories.log

# Find requests without request ID
grep "NO_REQUEST_ID" logs/breakup-stories.log

# Check request duration patterns
grep "Request completed" logs/breakup-stories.log | awk '{print $NF}' | sort -n
```

## Future Enhancements

### Planned Features
- **Distributed Tracing**: Integration with Jaeger/Zipkin
- **Request Correlation**: Cross-service request linking
- **Metrics Integration**: Request ID in Prometheus metrics
- **Dashboard**: Real-time request tracking dashboard
- **Alerting**: Request failure rate alerts

### Performance Optimizations
- **Request ID Caching**: Cache frequently used request IDs
- **Async Propagation**: Better async context handling
- **Compression**: Optimize request ID storage
- **Sampling**: Configurable request ID generation rate 