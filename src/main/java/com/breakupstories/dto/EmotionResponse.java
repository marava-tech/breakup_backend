package com.breakupstories.dto;

import com.breakupstories.model.Emotion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmotionResponse {
    private String id;
    private Emotion.EmotionType type;
    private Double score;

    public static EmotionResponse fromEmotion(Emotion emotion) {
        return EmotionResponse.builder()
                .id(emotion.getId())
                .type(emotion.getType())
                .score(emotion.getScore())
                .build();
    }
} 