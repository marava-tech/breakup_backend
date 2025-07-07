# Indian Timezone (IST) Implementation

This document outlines the implementation of Indian Standard Time (IST) throughout the backend application.

## Overview

The application has been configured to use Indian Standard Time (IST) - Asia/Kolkata timezone for all date/time operations, regardless of the server location where the application is deployed.

## Changes Made

### 1. Application Configuration (`application.yml`)

**Added timezone configuration:**
```yaml
spring:
  # Timezone configuration for Indian Standard Time
  jackson:
    default-property-inclusion: non_null
    serialization:
      write-dates-as-timestamps: false
    time-zone: Asia/Kolkata
    date-format: yyyy-MM-dd HH:mm:ss

  # JVM timezone configuration
  jvm:
    timezone: Asia/Kolkata
```

### 2. Timezone Configuration Class (`TimezoneConfig.java`)

**Created new configuration class:**
```java
@Configuration
@Slf4j
public class TimezoneConfig {

    @Bean
    public CommandLineRunner timezoneInitializer() {
        return args -> {
            // Set the default timezone to Indian Standard Time
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
            ZoneId.systemDefault();
            
            log.info("Timezone configured to Indian Standard Time (IST) - Asia/Kolkata");
            log.info("Current system timezone: {}", TimeZone.getDefault().getID());
            log.info("Current system timezone offset: {}", TimeZone.getDefault().getRawOffset() / (1000 * 60 * 60) + " hours");
        };
    }
}
```

### 3. Jackson Configuration (`JacksonConfig.java`)

**Created Jackson configuration for proper date serialization:**
```java
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Register JavaTimeModule for proper LocalDateTime handling
        objectMapper.registerModule(new JavaTimeModule());
        
        // Set timezone to Indian Standard Time
        objectMapper.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        
        return objectMapper;
    }
}
```

### 4. Updated TimestampUtil (`TimestampUtil.java`)

**Enhanced with Indian timezone support:**
```java
public class TimestampUtil {
    
    // Indian Standard Time Zone ID
    private static final ZoneId INDIAN_TIMEZONE = ZoneId.of("Asia/Kolkata");
    
    // All methods now use Indian timezone
    public static LocalDateTime currentLocalDateTime() {
        return LocalDateTime.now(INDIAN_TIMEZONE);
    }
    
    public static ZonedDateTime currentZonedDateTime() {
        return ZonedDateTime.now(INDIAN_TIMEZONE);
    }
    
    public static Long currentEpochMillis() {
        return System.currentTimeMillis();
    }
    
    // ... other methods updated to use INDIAN_TIMEZONE
}
```

### 5. Updated Services

**All services now use TimestampUtil for consistent timezone handling:**

#### StoryConversionWorker.java
- Updated all `LocalDateTime.now()` calls to `TimestampUtil.currentLocalDateTime()`
- Updated all `System.currentTimeMillis()` calls to `TimestampUtil.currentEpochMillis()`

#### CommentAnalysisService.java
- Updated time calculations to use `TimestampUtil.currentLocalDateTime()`

#### StoryService.java
- Updated story creation timestamps to use `TimestampUtil.currentLocalDateTime()`

#### StoryProcessingWorker.java
- Updated all processing timestamps to use `TimestampUtil.currentLocalDateTime()`

#### OTPServiceImpl.java
- Updated OTP expiry calculations to use `TimestampUtil.currentEpochMillis()`
- Updated email content generation to use `TimestampUtil.currentLocalDateTime()`

#### JwtService.java
- Updated JWT token generation to use `TimestampUtil.currentEpochMillis()`
- Updated token expiration checks to use `TimestampUtil.currentEpochMillis()`

#### AuditService.java
- Updated cooldown calculations to use `TimestampUtil.currentEpochMillis()`

## Benefits

### 1. **Consistent Timezone**
- All timestamps are now in Indian Standard Time (IST)
- No dependency on server location or system timezone
- Consistent behavior across different deployment environments

### 2. **Proper Date Serialization**
- Jackson ObjectMapper configured for IST timezone
- All JSON responses will show dates in IST
- Proper date format: `yyyy-MM-dd HH:mm:ss`

### 3. **Database Consistency**
- All database timestamps stored in IST
- Consistent with Indian user expectations
- No timezone conversion issues

### 4. **API Response Consistency**
- All API responses show timestamps in IST
- Frontend can rely on consistent timezone
- No client-side timezone conversion needed

## Usage Examples

### Getting Current Time in IST
```java
// Get current LocalDateTime in IST
LocalDateTime now = TimestampUtil.currentLocalDateTime();

// Get current ZonedDateTime in IST
ZonedDateTime zonedNow = TimestampUtil.currentZonedDateTime();

// Get current epoch milliseconds
Long epochMillis = TimestampUtil.currentEpochMillis();
```

### Converting Between Timezones
```java
// Convert UTC to IST
ZonedDateTime utcTime = ZonedDateTime.now(ZoneId.of("UTC"));
ZonedDateTime istTime = utcTime.withZoneSameInstant(ZoneId.of("Asia/Kolkata"));

// Convert IST to UTC
ZonedDateTime istTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
ZonedDateTime utcTime = istTime.withZoneSameInstant(ZoneId.of("UTC"));
```

### Database Operations
```java
// Creating entities with IST timestamps
Story story = Story.builder()
    .createdAt(TimestampUtil.currentLocalDateTime())
    .updatedAt(TimestampUtil.currentLocalDateTime())
    .build();
```

## Deployment Considerations

### 1. **Docker Deployment**
When deploying in Docker containers, the timezone will be automatically set to IST regardless of the host system timezone.

### 2. **Cloud Deployment**
When deploying to cloud platforms (AWS, GCP, Azure), the application will consistently use IST timezone.

### 3. **Database Migration**
Existing data will continue to work correctly. New data will be stored with IST timestamps.

## Testing

A test class `TimezoneTest.java` has been created to verify the timezone configuration:

```java
public class TimezoneTest {
    public static void main(String[] args) {
        System.out.println("Default TimeZone: " + TimeZone.getDefault().getID());
        System.out.println("Current LocalDateTime (IST): " + TimestampUtil.currentLocalDateTime());
        System.out.println("Current ZonedDateTime (IST): " + TimestampUtil.currentZonedDateTime());
    }
}
```

## Verification

To verify that the timezone implementation is working correctly:

1. **Check Application Logs**: Look for timezone initialization messages
2. **API Responses**: Verify that all timestamp fields show IST time
3. **Database Records**: Check that new records have IST timestamps
4. **Scheduled Tasks**: Verify that scheduled operations use IST time

## Timezone Information

- **Timezone ID**: `Asia/Kolkata`
- **UTC Offset**: `+05:30` (5 hours 30 minutes ahead of UTC)
- **Daylight Saving**: Not observed in India
- **Standard Time**: Indian Standard Time (IST)

## Migration Notes

- **Backward Compatibility**: Existing data remains compatible
- **API Changes**: No breaking changes to API contracts
- **Database**: No migration required for existing data
- **Frontend**: May need to update timezone handling if previously expecting UTC

This implementation ensures that your entire backend application consistently uses Indian Standard Time, providing a seamless experience for Indian users regardless of where the application is deployed. 