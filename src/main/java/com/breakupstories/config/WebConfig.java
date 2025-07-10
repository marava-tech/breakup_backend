package com.breakupstories.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    
    private final RequestIdInterceptor requestIdInterceptor;
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                    "http://localhost:3000",           // Local development
                    "https://localhost:3000",          // Local development with HTTPS
                    "http://breakup-ai-server:8080",   // Server domain
                    "https://breakup-ai-server:8080",  // Server domain with HTTPS
                    "https://breakupadmin.marava.tech", // Production admin domain
                    "http://breakupadmin.marava.tech"   // Production admin domain (fallback)
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestIdInterceptor)
                .addPathPatterns("/**") // Apply to all paths
                .excludePathPatterns("/health", "/actuator/**"); // Exclude health checks
    }
} 