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
public class VisualPromptResponse {
    
    private List<VisualPrompt> visualPrompts;
    private String status;
    private String error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisualPrompt {
        private Integer paragraphNumber;
        private String paragraphText;
        private String visualDescription;
        private String style;
    }
} 