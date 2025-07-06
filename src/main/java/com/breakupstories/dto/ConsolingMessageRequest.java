package com.breakupstories.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsolingMessageRequest {
    
    @JsonProperty("story")
    private String story;
    
    @JsonProperty("language")
    private String language;
    
    @JsonProperty("gender")
    private String gender;
    
    @JsonProperty("age")
    private Integer age;
    
    @JsonProperty("console_by")
    private String consoleBy;
} 