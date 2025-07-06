package com.breakupstories.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for paragraph rewrite service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParagraphRewriteRequest {
    private String transcript;
    private String language;
} 