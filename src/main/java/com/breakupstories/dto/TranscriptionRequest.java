package com.breakupstories.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for transcription requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptionRequest {
    private String audioUrl;
    private String language;
} 