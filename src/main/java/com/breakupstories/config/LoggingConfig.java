package com.breakupstories.config;

import com.breakupstories.util.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Configuration for enhanced logging with request ID tracking
 */
@Configuration
@Slf4j
public class LoggingConfig {
    
    @PostConstruct
    public void init() {
        log.info("Request ID tracking logging configuration initialized");
    }
    
    /**
     * Set up MDC with request ID for logging
     * @param requestId The request ID to include in logs
     */
    public static void setupRequestLogging(String requestId) {
        if (requestId != null) {
            MDC.put("requestId", requestId);
        }
    }
    
    /**
     * Clear MDC context
     */
    public static void clearRequestLogging() {
        MDC.clear();
    }
    
    /**
     * Get current request ID from context
     * @return Current request ID or "NO_REQUEST_ID" if not set
     */
    public static String getCurrentRequestId() {
        String requestId = RequestContext.getRequestId();
        return requestId != null ? requestId : "NO_REQUEST_ID";
    }
} 