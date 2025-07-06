package com.breakupstories.exception;

/**
 * Custom exception for AI service errors
 */
public class AIServiceException extends RuntimeException {
    
    private final String serviceName;
    private final String errorCode;
    
    public AIServiceException(String serviceName, String message) {
        super(message);
        this.serviceName = serviceName;
        this.errorCode = "AI_SERVICE_ERROR";
    }
    
    public AIServiceException(String serviceName, String message, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
        this.errorCode = "AI_SERVICE_ERROR";
    }
    
    public AIServiceException(String serviceName, String errorCode, String message) {
        super(message);
        this.serviceName = serviceName;
        this.errorCode = errorCode;
    }
    
    public AIServiceException(String serviceName, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
        this.errorCode = errorCode;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
} 