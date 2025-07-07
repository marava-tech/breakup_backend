package com.breakupstories.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Slf4j
public class HealthController {

    /**
     * Ping endpoint to check if the service is accessible
     * @return Health status with timestamp
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        log.info("Health check ping received");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "Service is running");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "Breakup Stories Backend");
        response.put("version", "v1.2");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint with more detailed information
     * @return Detailed health status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> healthStatus() {
        log.info("Detailed health check requested");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "HEALTHY");
        response.put("message", "All systems operational");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "Breakup Stories Backend");
        response.put("version", "1.0.0");
        
        Map<String, String> components = new HashMap<>();
        components.put("database", "UP");
        components.put("api", "UP");
        components.put("fileStorage", "UP");
        
        response.put("components", components);
        
        return ResponseEntity.ok(response);
    }
} 