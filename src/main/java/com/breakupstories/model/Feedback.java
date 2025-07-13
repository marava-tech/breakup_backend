package com.breakupstories.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "feedbacks")
public class Feedback {
    
    @Id
    private String id;
    
    private String storyId; // Optional - for story-specific feedback
    private String userId;
    private FeedbackType type; // New field for feedback type
    private String subject; // For general feedback
    private String description; // For general feedback
    private FeedbackStatus status; // New field for tracking feedback status
    private String adminResponse; // Admin's response to the feedback
    private String fileUrl;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum FeedbackType {
        STORY_FEEDBACK, // Story-specific feedback (existing functionality)
        BUG_REPORT,     // Report a bug or technical issue
        FEATURE_REQUEST, // Request new features
        SUGGESTION,     // General suggestions
        COMPLAINT,      // Complaints about app or content
        GENERAL         // General feedback
    }
    
    public enum FeedbackStatus {
        PENDING,    // New feedback, not yet reviewed
        IN_REVIEW,  // Being reviewed by admin
        RESOLVED,   // Issue resolved or suggestion implemented
        VALUED,
        CLOSED,     // Feedback closed without action
        REJECTED    // Feedback rejected
    }
} 