package com.breakupstories.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Feedback update request with flexible fields")
public class FeedbackUpdateRequest {
    
    @Schema(description = "Feedback subject", example = "Updated Subject")
    private String subject;
    
    @Schema(description = "Feedback description", example = "Updated description")
    private String description;
    
    @Schema(description = "Feedback type", example = "BUG_REPORT")
    private String type;
    
    @Schema(description = "Feedback status", example = "IN_REVIEW")
    private String status;
    
    @Schema(description = "Admin response", example = "Thank you for your feedback")
    private String adminResponse;
    
    @Schema(description = "File URL", example = "https://example.com/file.pdf")
    private String fileUrl;
} 