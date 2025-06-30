package com.breakupstories.config;

import com.breakupstories.util.RequestContext;
import com.breakupstories.util.RequestIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interceptor to automatically generate and set request IDs for all incoming requests
 */
@Component
@Slf4j
public class RequestIdInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Check if request ID is already provided in headers
        String requestId = request.getHeader("X-Request-ID");
        
        if (requestId == null || requestId.trim().isEmpty()) {
            // Generate new request ID if not provided
            requestId = RequestIdGenerator.generateTimestampedRequestId();
        }
        
        // Set request ID in context
        RequestContext.setRequestId(requestId);
        RequestContext.setStartTime(System.currentTimeMillis());
        
        // Set up MDC logging with request ID
        LoggingConfig.setupRequestLogging(requestId);
        
        // Add request ID to response headers
        response.setHeader("X-Request-ID", requestId);
        
        // Log request start with request ID
        log.info("{} {} - Request started", request.getMethod(), request.getRequestURI());
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String requestId = RequestContext.getRequestId();
        Long startTime = RequestContext.getStartTime();
        
        if (requestId != null && startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            
            if (ex != null) {
                log.error("{} {} - Request failed after {}ms - Error: {}", 
                    request.getMethod(), request.getRequestURI(), duration, ex.getMessage());
            } else {
                log.info("{} {} - Request completed in {}ms with status {}", 
                    request.getMethod(), request.getRequestURI(), duration, response.getStatus());
            }
        }
        
        // Clear MDC context and request context
        LoggingConfig.clearRequestLogging();
        RequestContext.clear();
    }
} 