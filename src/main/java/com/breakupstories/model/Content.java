package com.breakupstories.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Content {
    
    private ContentType type;
    private String data;
    private Integer orderIndex;
    
    public enum ContentType {
        TEXT, IMAGE, VIDEO
    }
} 