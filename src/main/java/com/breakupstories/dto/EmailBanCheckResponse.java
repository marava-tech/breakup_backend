package com.breakupstories.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailBanCheckResponse {
    
    private String email;
    private boolean isBanned;
    private String message;
    
    // Ban details (only populated if banned)
    private String deviceId;
    private String banReason;
    private LocalDateTime bannedAt;
    private List<String> bannedEmails; // All emails associated with the banned device
    
    public static EmailBanCheckResponse notBanned(String email) {
        return EmailBanCheckResponse.builder()
                .email(email)
                .isBanned(false)
                .message("Account is not banned")
                .build();
    }
    
    public static EmailBanCheckResponse banned(String email, String deviceId, String banReason, 
                                              LocalDateTime bannedAt, List<String> bannedEmails) {
        return EmailBanCheckResponse.builder()
                .email(email)
                .isBanned(true)
                .deviceId(deviceId)
                .banReason(banReason)
                .bannedAt(bannedAt)
                .bannedEmails(bannedEmails)
                .message("Account is banned")
                .build();
    }
    
    public static EmailBanCheckResponse error(String email, String message) {
        return EmailBanCheckResponse.builder()
                .email(email)
                .isBanned(false)
                .message(message)
                .build();
    }
}
