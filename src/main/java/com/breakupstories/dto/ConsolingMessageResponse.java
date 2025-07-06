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
public class ConsolingMessageResponse {
    
    @JsonProperty("success")
    private Boolean success;
    
    @JsonProperty("consoling_message")
    private String consolingMessage;
    
    @JsonProperty("language")
    private String language;
    
    @JsonProperty("console_by")
    private String consoleBy;
    
    @JsonProperty("error")
    private String error;
} 