package com.breakupstories.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for story analysis results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryAnalysis {
    @JsonProperty("emotions_with_scores")
    private Map<String, Double> emotions_with_scores;
    @JsonDeserialize(using = NamesListDeserializer.class)
    private List<String> tags;
    @JsonDeserialize(using = NamesListDeserializer.class)
    private List<String> locations;
    @JsonDeserialize(using = NamesListDeserializer.class)
    private List<String> names;
    @JsonProperty("story_type")
    private String story_type;
    @JsonProperty("is_valid_story")
    private Boolean is_valid_story;
    @JsonDeserialize(using = NamesListDeserializer.class)
    private List<String> themes;
    @JsonProperty("plot_summary")
    private String plot_summary;
    @JsonProperty("cultural_elements")
    @JsonDeserialize(using = NamesListDeserializer.class)
    private List<String> cultural_elements;
} 