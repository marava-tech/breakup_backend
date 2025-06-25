package com.breakupstories.dto;

import com.breakupstories.model.Keyword;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeywordResponse {
    private String id;
    private String keyword;

    public static KeywordResponse fromKeyword(Keyword keyword) {
        return KeywordResponse.builder()
                .id(keyword.getId())
                .keyword(keyword.getKeyword())
                .build();
    }
} 