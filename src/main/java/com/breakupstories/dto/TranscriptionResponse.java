package com.breakupstories.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for transcription responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptionResponse {
    
    private String transcript;
    private String language;
    private Double confidence;
} 