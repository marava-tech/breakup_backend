package com.breakupstories.util;

import java.util.UUID;

/**
 * Utility class for generating unique request IDs
 */
public class RequestIdGenerator {
    
    /**
     * Generate a unique request ID using UUID
     * @return Unique request ID string
     */
    public static String generateRequestId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    /**
     * Generate a unique request ID with custom prefix
     * @param prefix Custom prefix for the request ID
     * @return Unique request ID string with prefix
     */
    public static String generateRequestId(String prefix) {
        return prefix + "_" + generateRequestId();
    }
    
    /**
     * Generate a timestamped request ID
     * @return Unique request ID with timestamp
     */
    public static String generateTimestampedRequestId() {
        long timestamp = System.currentTimeMillis();
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return String.format("req_%d_%s", timestamp, uuid);
    }
} 