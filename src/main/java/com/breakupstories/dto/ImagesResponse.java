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
public class ImagesResponse {
    private String thumbnailImageUrl;
    private List<String> storyImageUrls;
    private String status;
    private String error;
} 