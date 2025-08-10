package com.breakupstories.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;


import java.util.TimeZone;

/**
 * Configuration class to set up Indian timezone for the entire application
 */
@Configuration
@Slf4j
public class TimezoneConfig {

    /**
     * Set the default timezone to Indian Standard Time (IST)
     * This ensures all date/time operations use IST regardless of server location
     */
    @Bean
    public CommandLineRunner timezoneInitializer() {
        return args -> {
            // Set the default timezone to Indian Standard Time
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
            
            log.info("Timezone configured to Indian Standard Time (IST) - Asia/Kolkata");
            log.info("Current system timezone: {}", TimeZone.getDefault().getID());
            log.info("Current system timezone offset: {}", TimeZone.getDefault().getRawOffset() / (1000 * 60 * 60) + " hours");
        };
    }
} 