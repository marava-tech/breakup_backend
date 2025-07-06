package com.breakupstories.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for individual paragraph content
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParagraphContent {
    private String type;
    private String data;
    private Integer orderIndex;
} 