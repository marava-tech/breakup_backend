package com.breakupstories.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Utility class for timestamp conversions between LocalDateTime and epoch milliseconds
 * All operations use Indian Standard Time (IST) - Asia/Kolkata
 */
public class TimestampUtil {
    
    // Indian Standard Time Zone ID
    private static final ZoneId INDIAN_TIMEZONE = ZoneId.of("Asia/Kolkata");
    
    /**
     * Convert LocalDateTime to epoch milliseconds using Indian timezone
     * @param localDateTime the LocalDateTime to convert
     * @return epoch timestamp in milliseconds, or null if input is null
     */
    public static Long toEpochMillis(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(INDIAN_TIMEZONE).toInstant().toEpochMilli();
    }
    
    /**
     * Convert epoch milliseconds to LocalDateTime using Indian timezone
     * @param epochMillis the epoch timestamp in milliseconds
     * @return LocalDateTime in Indian timezone, or null if input is null
     */
    public static LocalDateTime fromEpochMillis(Long epochMillis) {
        if (epochMillis == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), INDIAN_TIMEZONE);
    }
    
    /**
     * Get current epoch timestamp in milliseconds
     * @return current epoch timestamp in milliseconds
     */
    public static Long currentEpochMillis() {
        return System.currentTimeMillis();
    }
    
    /**
     * Get current LocalDateTime in Indian timezone
     * @return current LocalDateTime in IST
     */
    public static LocalDateTime currentLocalDateTime() {
        return LocalDateTime.now(INDIAN_TIMEZONE);
    }
    
    /**
     * Get current ZonedDateTime in Indian timezone
     * @return current ZonedDateTime in IST
     */
    public static ZonedDateTime currentZonedDateTime() {
        return ZonedDateTime.now(INDIAN_TIMEZONE);
    }
    
    /**
     * Convert LocalDateTime to ZonedDateTime in Indian timezone
     * @param localDateTime the LocalDateTime to convert
     * @return ZonedDateTime in IST, or null if input is null
     */
    public static ZonedDateTime toZonedDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(INDIAN_TIMEZONE);
    }
    
    /**
     * Get the Indian timezone ZoneId
     * @return ZoneId for Asia/Kolkata
     */
    public static ZoneId getIndianTimezone() {
        return INDIAN_TIMEZONE;
    }
} 