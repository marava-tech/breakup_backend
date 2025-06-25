package com.breakupstories.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "emotions")
public class Emotion {
    @Id
    private String id;
    private EmotionType type;
    private Double score;
    
    public enum EmotionType {
        SAD, HAPPY, ANGRY, EXCITED, NERVOUS, CALM, CONFUSED, SURPRISED
    }
} 