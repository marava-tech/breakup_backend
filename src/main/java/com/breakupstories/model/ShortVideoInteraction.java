package com.breakupstories.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "short_video_interactions")
@CompoundIndexes({
        @CompoundIndex(name = "idx_user_video_type", def = "{'userId': 1, 'videoId': 1, 'type': 1}", unique = true)
})
public class ShortVideoInteraction {
    @Id
    private String id;

    @Indexed
    private String videoId;

    @Indexed
    private String userId;

    private InteractionType type;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum InteractionType {
        VIEW, SHARE
    }
}
