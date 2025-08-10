package com.breakupstories.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryAnalysisResponse {
    private boolean success;
    private String title;
    private Analysis analysis;
    private String error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Analysis {
        private Map<String, Double> emotionsWithScores;
        private List<String> tags;
        private List<String> locations;
        private List<NameInfo> names;
        private String storyType;
        private boolean isValidStory;
        private List<String> themes;
        private String plotSummary;
        private List<String> culturalElements;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NameInfo {
        private String name;
        private String role;
        private String gender;
    }
} 