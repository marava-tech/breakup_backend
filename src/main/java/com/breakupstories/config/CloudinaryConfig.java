package com.breakupstories.config;

import com.cloudinary.Cloudinary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;


    @Configuration
    public class CloudinaryConfig {

        @Bean
        Cloudinary getCloudinary() {

            Map<String, String> map = new HashMap<>();

            map.put("cloud_name", "dohsebpd1");
            map.put("api_key", "421394291995232");
            map.put("api_secret", "ChXQSdHuz9jeycoA0XsW2BFwkcs");
            map.put("secure", "true");

            // Performance optimizations
            map.put("timeout", "60000"); // 60 seconds timeout
            map.put("connection_timeout", "60000"); // 30 seconds connection timeout

            return new Cloudinary(map);
        }


}
