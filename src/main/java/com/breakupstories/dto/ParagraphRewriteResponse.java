package com.breakupstories.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for paragraph rewrite service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParagraphRewriteResponse {
    private List<ParagraphContent> contents;
    private String language;
} 