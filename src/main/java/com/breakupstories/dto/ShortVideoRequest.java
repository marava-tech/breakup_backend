package com.breakupstories.dto;

import com.breakupstories.enums.VideoLanguage;
import com.breakupstories.model.ShortVideo.VideoStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ShortVideoRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Video URL is required")
    private String videoUrl;

    private String thumbnailUrl;

    @NotNull(message = "Language is required")
    private VideoLanguage language;

    private List<String> tags;
    private VideoStatus status;
}
