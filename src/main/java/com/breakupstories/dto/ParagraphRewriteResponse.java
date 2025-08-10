package com.breakupstories.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParagraphRewriteResponse {
    
    private List<Paragraph> paragraphs;
    private String status;
    private String error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Paragraph {
        private String originalText;
        private String rewrittenText;
        private Integer paragraphNumber;
        private String style;
    }
} 