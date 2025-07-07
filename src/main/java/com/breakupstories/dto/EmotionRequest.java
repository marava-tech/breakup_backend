package com.breakupstories.dto;

import com.breakupstories.model.Emotion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmotionRequest {
    @NotNull(message = "Emotion type is required")
    private String type;
    @NotNull(message = "Score is required")
    private Double score;
} 