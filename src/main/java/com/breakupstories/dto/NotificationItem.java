package com.breakupstories.dto;

import com.breakupstories.enums.DeeplinkType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationItem {
    
    private String text;
    private DeeplinkType deeplinkType;
    private String id;
} 