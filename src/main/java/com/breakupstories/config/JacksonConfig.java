package com.breakupstories.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * Jackson configuration for proper date/time serialization with Indian timezone
 */
@Configuration
public class JacksonConfig {

    /**
     * Configure ObjectMapper with Indian timezone for date/time serialization
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Register JavaTimeModule for proper LocalDateTime handling
        objectMapper.registerModule(new JavaTimeModule());
        
        // Set timezone to Indian Standard Time
        objectMapper.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        
        return objectMapper;
    }
} 