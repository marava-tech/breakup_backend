package com.breakupstories.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for AI service integration
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.service")
public class AIServiceConfig {
    
    /**
     * AI service provider (e.g., "openai", "azure", "google", "custom")
     */
    private String provider = "real";
    
    /**
     * AI service API base URL
     */
    private String baseUrl;
    
    /**
     * AI service API key
     */
    private String apiKey;
    
    /**
     * AI service API timeout in milliseconds
     */
    private int timeout = 30000;
    
    /**
     * Maximum retry attempts for AI service calls
     */
    private int maxRetries = 3;
    
    /**
     * Retry delay in milliseconds
     */
    private int retryDelay = 1000;
    
    /**
     * Enable/disable AI service
     */
    private boolean enabled = true;
    
    /**
     * Model configuration for different AI tasks
     */
    private ModelConfig models = new ModelConfig();
    
    /**
     * Rate limiting configuration
     */
    private RateLimitConfig rateLimit = new RateLimitConfig();
    
    @Data
    public static class ModelConfig {
        /**
         * Model for text generation (stories, titles)
         */
        private String textGeneration = "gpt-3.5-turbo";
        
        /**
         * Model for text analysis (sentiment, emotions)
         */
        private String textAnalysis = "gpt-3.5-turbo";
        
        /**
         * Model for audio transcription
         */
        private String transcription = "whisper-1";
        
        /**
         * Model for image generation
         */
        private String imageGeneration = "dall-e-2";
        
        /**
         * Model for language detection
         */
        private String languageDetection = "gpt-3.5-turbo";
    }
    
    @Data
    public static class RateLimitConfig {
        /**
         * Maximum requests per minute
         */
        private int requestsPerMinute = 60;
        
        /**
         * Maximum requests per hour
         */
        private int requestsPerHour = 1000;
        
        /**
         * Maximum concurrent requests
         */
        private int maxConcurrentRequests = 10;
    }
} 