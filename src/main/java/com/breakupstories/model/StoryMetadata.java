package com.breakupstories.model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class StoryMetadata {
    private List<String> names ;
    private List<String> locations;
    private List<String> pincodes;
    private String state;
    private String district;
    private String language;
    private String deviceInfo;
}
