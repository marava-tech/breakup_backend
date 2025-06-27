package com.breakupstories.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Utility class for timestamp conversions between LocalDateTime and epoch milliseconds
 */
public class TimestampUtil {
    
    /**
     * Convert LocalDateTime to epoch milliseconds
     * @param localDateTime the LocalDateTime to convert
     * @return epoch timestamp in milliseconds, or null if input is null
     */
    public static Long toEpochMillis(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
    
    /**
     * Convert epoch milliseconds to LocalDateTime
     * @param epochMillis the epoch timestamp in milliseconds
     * @return LocalDateTime, or null if input is null
     */
    public static LocalDateTime fromEpochMillis(Long epochMillis) {
        if (epochMillis == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }
    
    /**
     * Get current epoch timestamp in milliseconds
     * @return current epoch timestamp in milliseconds
     */
    public static Long currentEpochMillis() {
        return System.currentTimeMillis();
    }
    
    /**
     * Get current LocalDateTime
     * @return current LocalDateTime
     */
    public static LocalDateTime currentLocalDateTime() {
        return LocalDateTime.now();
    }
} 