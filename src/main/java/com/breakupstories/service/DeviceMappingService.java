package com.breakupstories.service;

import com.breakupstories.model.User;
import com.breakupstories.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service dedicated to device mapping operations
 * Separated from UserService to avoid proxy conflicts with UserDetailsService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceMappingService {
    
    private final UserRepository userRepository;
    
    /**
     * Asynchronously map device ID to user only if the user doesn't already have a device ID
     * This method runs in a separate thread to avoid blocking the login response
     */
    @Async("deviceMappingExecutor")
    public void mapDeviceIdToUserAsync(String userEmail, String deviceId) {
        log.info("Asynchronously attempting to map device ID {} to user: {}", deviceId, userEmail);
        
        try {
            // Find the user first
            Optional<User> userOptional = userRepository.findByEmail(userEmail);
            if (userOptional.isEmpty()) {
                log.error("User not found with email: {} while mapping device ID: {}", userEmail, deviceId);
                return;
            }
            
            User user = userOptional.get();
            
            // Check if user already has a device ID - if yes, ignore the new device ID
            if (user.getDeviceId() != null && !user.getDeviceId().trim().isEmpty()) {
                log.info("User {} already has device ID: {}, ignoring new device ID: {}", 
                        userEmail, user.getDeviceId(), deviceId);
                return;
            }
            
            // Map the device ID to the user (multiple users can share the same device ID)
            user.setDeviceId(deviceId);
            userRepository.save(user);
            log.info("Successfully mapped device ID {} to user: {} (async)", deviceId, userEmail);
            
        } catch (Exception e) {
            log.error("Error occurred while asynchronously mapping device ID {} to user: {}", 
                    deviceId, userEmail, e);
        }
    }
}
