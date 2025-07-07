package com.breakupstories.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import java.util.Map;

/**
 * Test class to verify timestamp serialization format
 */
public class TimestampFormatTest {
    
    public static void main(String[] args) throws Exception {
        // Create ObjectMapper with the same configuration as JacksonConfig
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Configure JavaTimeModule with custom serializers
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));
        
        objectMapper.registerModule(javaTimeModule);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        
        // Test data
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        LocalTime time = LocalTime.now();
        
        // Test serialization
        String json = objectMapper.writeValueAsString(Map.of(
            "createdAt", now,
            "date", today,
            "time", time
        ));
        
        System.out.println("Serialized JSON:");
        System.out.println(json);
        
        // Verify it's not an array format
        if (json.contains("[") && json.contains("]")) {
            System.out.println("❌ ERROR: Timestamps are being serialized as arrays!");
        } else {
            System.out.println("✅ SUCCESS: Timestamps are being serialized as strings!");
        }
    }
} 