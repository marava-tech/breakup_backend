package com.breakupstories.enums;

public enum CoinHistoryEntityType {
    STORY,              // relatedEntityId refers to a story ID
    USER,               // relatedEntityId refers to a user ID (for referrals)
    WITHDRAWAL,         // relatedEntityId refers to a withdrawal ID
    FEEDBACK,           // relatedEntityId refers to a feedback ID
    CAMPAIGN,           // relatedEntityId refers to a campaign/bonus ID
    SYSTEM,             // relatedEntityId refers to system operation ID
    MANUAL              // relatedEntityId refers to manual operation ID
}
