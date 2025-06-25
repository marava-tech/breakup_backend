package com.breakupstories.dto;

import com.breakupstories.model.Content;
import com.breakupstories.model.Emotion;
import com.breakupstories.model.Keyword;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStoryRequest {
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String audioUrl;
    private String shareLink;
    
    @NotEmpty(message = "At least one content item is required")
    private List<Content> contents;
    
    private List<String> tags;
    private List<Emotion> emotions;
    private List<Keyword> keywords;
} 