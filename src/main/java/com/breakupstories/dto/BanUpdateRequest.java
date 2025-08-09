package com.breakupstories.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BanUpdateRequest {
    
    @NotBlank(message = "Reason is required")
    private String reason;
    
    private List<String> emailsToAdd;    // Emails to add to the banned device
    private List<String> emailsToRemove; // Emails to remove from the banned device
}
