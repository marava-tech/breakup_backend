package com.breakupstories.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    
    private String userId;
    private long totalNewLikes;
    private long totalNewViews;
    private long totalNewComments;
    private Long lastNotificationView;
    private Boolean notify;

} 