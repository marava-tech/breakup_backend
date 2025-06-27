package com.breakupstories.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private Long timestamp;
    private int status;
    private String error;
    private String message;
    private String description;
    private Map<String, String> fieldErrors;

    public static ErrorResponse of(String message, String error) {
        return ErrorResponse.builder()
                .message(message)
                .error(error)
                .timestamp(System.currentTimeMillis())
                .build();
    }
} 