package com.breakupstories.dto;

import com.breakupstories.util.RequestContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response wrapper that includes request ID for tracking
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestIdResponse<T> {
    
    private String requestId;
    private T data;
    private String message;
    private long timestamp;
    
    /**
     * Create a response with current request ID
     * @param data The response data
     * @param message Optional message
     * @return RequestIdResponse with current request ID
     */
    public static <T> RequestIdResponse<T> of(T data, String message) {
        return RequestIdResponse.<T>builder()
                .requestId(RequestContext.getRequestId())
                .data(data)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * Create a response with current request ID and no message
     * @param data The response data
     * @return RequestIdResponse with current request ID
     */
    public static <T> RequestIdResponse<T> of(T data) {
        return of(data, null);
    }
    
    /**
     * Create an error response with current request ID
     * @param message Error message
     * @return RequestIdResponse with error message
     */
    public static <T> RequestIdResponse<T> error(String message) {
        return RequestIdResponse.<T>builder()
                .requestId(RequestContext.getRequestId())
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }
} 