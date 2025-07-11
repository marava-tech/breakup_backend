package com.breakupstories.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Slf4j
public class HealthController {

    private final long startTime = System.currentTimeMillis();

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
        response.put("version", "v1.0");
        
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
        response.put("version", "1.5");
        
        Map<String, String> components = new HashMap<>();
        components.put("database", "UP");
        components.put("api", "UP");
        
        response.put("components", components);
        
        // Add system metrics
        Map<String, Object> systemMetrics = new HashMap<>();
        systemMetrics.put("cpu", getCpuUsage());
        systemMetrics.put("memory", getMemoryUsage());
        systemMetrics.put("uptime", getUptime());
        
        response.put("system", systemMetrics);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get CPU usage percentage
     * @return CPU usage as a map with percentage and details
     */
    private Map<String, Object> getCpuUsage() {
        Map<String, Object> cpuInfo = new HashMap<>();
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            double cpuLoad = osBean.getSystemLoadAverage();
            
            // Convert to percentage (load average is typically 0-1 for single core, multiply by 100 for percentage)
            double cpuPercentage = Math.min(100.0, cpuLoad * 100);
            
            cpuInfo.put("usage_percentage", Math.round(cpuPercentage * 100.0) / 100.0);
            cpuInfo.put("load_average", cpuLoad);
            cpuInfo.put("available_processors", osBean.getAvailableProcessors());
        } catch (Exception e) {
            log.warn("Could not retrieve CPU information: {}", e.getMessage());
            cpuInfo.put("usage_percentage", -1);
            cpuInfo.put("error", "Unable to retrieve CPU information");
        }
        return cpuInfo;
    }

    /**
     * Get memory usage information
     * @return Memory usage as a map with details
     */
    private Map<String, Object> getMemoryUsage() {
        Map<String, Object> memoryInfo = new HashMap<>();
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            Runtime runtime = Runtime.getRuntime();
            
            // System memory info (JVM heap)
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            double memoryUsagePercentage = ((double) usedMemory / maxMemory) * 100;
            
            memoryInfo.put("total_mb", totalMemory / (1024 * 1024));
            memoryInfo.put("used_mb", usedMemory / (1024 * 1024));
            memoryInfo.put("free_mb", freeMemory / (1024 * 1024));
            memoryInfo.put("max_mb", maxMemory / (1024 * 1024));
            memoryInfo.put("usage_percentage", Math.round(memoryUsagePercentage * 100.0) / 100.0);
            
            // JVM memory info
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            double heapUsagePercentage = ((double) heapUsed / heapMax) * 100;
            
            Map<String, Object> jvmMemory = new HashMap<>();
            jvmMemory.put("heap_used_mb", heapUsed / (1024 * 1024));
            jvmMemory.put("heap_max_mb", heapMax / (1024 * 1024));
            jvmMemory.put("heap_usage_percentage", Math.round(heapUsagePercentage * 100.0) / 100.0);
            
            memoryInfo.put("jvm", jvmMemory);
            
        } catch (Exception e) {
            log.warn("Could not retrieve memory information: {}", e.getMessage());
            memoryInfo.put("error", "Unable to retrieve memory information");
        }
        return memoryInfo;
    }

    /**
     * Get application uptime
     * @return Uptime information as a map
     */
    private Map<String, Object> getUptime() {
        Map<String, Object> uptimeInfo = new HashMap<>();
        try {
            long currentTime = System.currentTimeMillis();
            long uptimeMillis = currentTime - startTime;
            
            Duration uptime = Duration.ofMillis(uptimeMillis);
            
            uptimeInfo.put("milliseconds", uptimeMillis);
            uptimeInfo.put("seconds", uptime.getSeconds());
            uptimeInfo.put("minutes", uptime.toMinutes());
            uptimeInfo.put("hours", uptime.toHours());
            uptimeInfo.put("days", uptime.toDays());
            uptimeInfo.put("formatted", String.format("%dd %dh %dm %ds", 
                uptime.toDays(), 
                uptime.toHours() % 24, 
                uptime.toMinutes() % 60, 
                uptime.getSeconds() % 60));
            
        } catch (Exception e) {
            log.warn("Could not calculate uptime: {}", e.getMessage());
            uptimeInfo.put("error", "Unable to calculate uptime");
        }
        return uptimeInfo;
    }
} 