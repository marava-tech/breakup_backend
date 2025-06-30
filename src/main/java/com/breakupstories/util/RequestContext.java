package com.breakupstories.util;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Request context holder for storing request-specific information
 */
@Component
public class RequestContext {
    
    private static final ThreadLocal<ConcurrentHashMap<String, Object>> contextHolder = 
        ThreadLocal.withInitial(ConcurrentHashMap::new);
    
    public static final String REQUEST_ID_KEY = "requestId";
    public static final String USER_ID_KEY = "userId";
    public static final String START_TIME_KEY = "startTime";
    
    /**
     * Set request ID in the current thread context
     * @param requestId The request ID to set
     */
    public static void setRequestId(String requestId) {
        getContext().put(REQUEST_ID_KEY, requestId);
    }
    
    /**
     * Get request ID from the current thread context
     * @return The request ID or null if not set
     */
    public static String getRequestId() {
        return (String) getContext().get(REQUEST_ID_KEY);
    }
    
    /**
     * Set user ID in the current thread context
     * @param userId The user ID to set
     */
    public static void setUserId(String userId) {
        getContext().put(USER_ID_KEY, userId);
    }
    
    /**
     * Get user ID from the current thread context
     * @return The user ID or null if not set
     */
    public static String getUserId() {
        return (String) getContext().get(USER_ID_KEY);
    }
    
    /**
     * Set request start time in the current thread context
     * @param startTime The start time to set
     */
    public static void setStartTime(Long startTime) {
        getContext().put(START_TIME_KEY, startTime);
    }
    
    /**
     * Get request start time from the current thread context
     * @return The start time or null if not set
     */
    public static Long getStartTime() {
        return (Long) getContext().get(START_TIME_KEY);
    }
    
    /**
     * Set a custom value in the current thread context
     * @param key The key to store the value under
     * @param value The value to store
     */
    public static void set(String key, Object value) {
        getContext().put(key, value);
    }
    
    /**
     * Get a custom value from the current thread context
     * @param key The key to retrieve the value for
     * @return The value or null if not found
     */
    public static Object get(String key) {
        return getContext().get(key);
    }
    
    /**
     * Clear the current thread context
     */
    public static void clear() {
        getContext().clear();
    }
    
    /**
     * Get the current thread context
     * @return The current thread context map
     */
    private static ConcurrentHashMap<String, Object> getContext() {
        return contextHolder.get();
    }
} 