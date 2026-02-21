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
@Document(collection = "short_video_comments")
@CompoundIndexes({
        @CompoundIndex(name = "idx_video_active_created", def = "{'videoId': 1, 'active': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "idx_user_active_created", def = "{'userId': 1, 'active': 1, 'createdAt': -1}")
})
public class ShortVideoComment {
    @Id
    private String id;

    @Indexed
    private String videoId;

    @Indexed
    private String userId;

    @Indexed
    private String parentId; // nullable for replies
    private String text;

    @Indexed
    @Builder.Default
    private boolean active = true; // default to true

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
