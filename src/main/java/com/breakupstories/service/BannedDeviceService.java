package com.breakupstories.service;

import com.breakupstories.dto.BannedDeviceResponse;
import com.breakupstories.dto.EmailBanCheckResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.exception.AccountBannedException;
import com.breakupstories.model.BannedDevice;
import com.breakupstories.model.User;
import com.breakupstories.repository.BannedDeviceRepository;
import com.breakupstories.repository.UserRepository;
import com.breakupstories.util.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BannedDeviceService {
    
    private final BannedDeviceRepository bannedDeviceRepository;
    private final UserRepository userRepository;
    
    /**
     * Ban a device with a specific reason and fetch all associated user emails
     * @param deviceId The device ID to ban
     * @param reason The reason for banning
     * @return BannedDeviceResponse with all associated emails
     */
    public BannedDeviceResponse banDevice(String deviceId, String reason) {
        String requestId = RequestContext.getRequestId();
        log.info("Banning device {} with reason: {} [RequestID: {}]", deviceId, reason, requestId);
        
        try {
            // Check if device is already banned
            Optional<BannedDevice> existingBan = bannedDeviceRepository.findByDeviceId(deviceId);
            if (existingBan.isPresent()) {
                log.warn("Device {} is already banned. Updating reason. [RequestID: {}]", deviceId, requestId);
                BannedDevice bannedDevice = existingBan.get();
                bannedDevice.setReason(reason);
                bannedDevice.setUpdatedAt(LocalDateTime.now());
                
                // Refresh the emails list in case new users were added to this device
                List<String> emails = fetchEmailsByDeviceId(deviceId);
                bannedDevice.setEmails(emails);
                
                BannedDevice updated = bannedDeviceRepository.save(bannedDevice);
                log.info("Updated banned device {} with {} emails [RequestID: {}]", 
                        deviceId, emails.size(), requestId);
                return BannedDeviceResponse.fromBannedDevice(updated);
            }
            
            // Fetch all users with this device ID
            List<String> emails = fetchEmailsByDeviceId(deviceId);
            log.info("Found {} users with device ID {} [RequestID: {}]", emails.size(), deviceId, requestId);
            
            // Create banned device record
            BannedDevice bannedDevice = BannedDevice.builder()
                    .deviceId(deviceId)
                    .reason(reason)
                    .emails(emails)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            BannedDevice saved = bannedDeviceRepository.save(bannedDevice);
            log.info("Successfully banned device {} with {} associated emails [RequestID: {}]", 
                    deviceId, emails.size(), requestId);
            
            return BannedDeviceResponse.fromBannedDevice(saved);
            
        } catch (Exception e) {
            log.error("Error banning device {} [RequestID: {}]: {}", deviceId, requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to ban device: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check if a device is banned
     * @param deviceId The device ID to check
     * @return true if device is banned, false otherwise
     */
    public boolean isDeviceBanned(String deviceId) {
        return bannedDeviceRepository.existsByDeviceId(deviceId);
    }
    
    /**
     * Get banned device details by device ID
     * @param deviceId The device ID
     * @return BannedDeviceResponse if found, null otherwise
     */
    public BannedDeviceResponse getBannedDevice(String deviceId) {
        Optional<BannedDevice> bannedDevice = bannedDeviceRepository.findByDeviceId(deviceId);
        return bannedDevice.map(BannedDeviceResponse::fromBannedDevice).orElse(null);
    }
    
    /**
     * Get all banned devices with pagination
     * @param page Page number
     * @param size Page size
     * @param sortBy Sort field (default: createdAt)
     * @param sortOrder Sort order (asc/desc, default: desc)
     * @return PagedResponse of banned devices
     */
    public PagedResponse<BannedDeviceResponse> getAllBannedDevices(int page, int size, String sortBy, String sortOrder) {
        String requestId = RequestContext.getRequestId();
        log.info("Getting all banned devices - page: {}, size: {} [RequestID: {}]", page, size, requestId);
        
        try {
            Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            
            Page<BannedDevice> bannedDevicePage = bannedDeviceRepository.findAll(pageable);
            
            List<BannedDeviceResponse> responses = bannedDevicePage.getContent().stream()
                    .map(BannedDeviceResponse::fromBannedDevice)
                    .collect(Collectors.toList());
            
            log.info("Returning {} banned devices [RequestID: {}]", responses.size(), requestId);
            return PagedResponse.of(responses, page, size, bannedDevicePage.getTotalElements());
            
        } catch (Exception e) {
            log.error("Error getting banned devices [RequestID: {}]: {}", requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to get banned devices: " + e.getMessage(), e);
        }
    }
    
    /**
     * Search banned devices by reason
     * @param reason Reason to search for (case-insensitive)
     * @param page Page number
     * @param size Page size
     * @param sortBy Sort field
     * @param sortOrder Sort order
     * @return PagedResponse of banned devices matching the reason
     */
    public PagedResponse<BannedDeviceResponse> searchBannedDevicesByReason(String reason, int page, int size, 
                                                                           String sortBy, String sortOrder) {
        String requestId = RequestContext.getRequestId();
        log.info("Searching banned devices by reason: {} [RequestID: {}]", reason, requestId);
        
        try {
            Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            
            Page<BannedDevice> bannedDevicePage = bannedDeviceRepository.findByReasonContainingIgnoreCase(reason, pageable);
            
            List<BannedDeviceResponse> responses = bannedDevicePage.getContent().stream()
                    .map(BannedDeviceResponse::fromBannedDevice)
                    .collect(Collectors.toList());
            
            log.info("Found {} banned devices matching reason '{}' [RequestID: {}]", responses.size(), reason, requestId);
            return PagedResponse.of(responses, page, size, bannedDevicePage.getTotalElements());
            
        } catch (Exception e) {
            log.error("Error searching banned devices by reason '{}' [RequestID: {}]: {}", reason, requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to search banned devices: " + e.getMessage(), e);
        }
    }
    
    /**
     * Search banned devices by device ID
     * @param deviceId Device ID to search for (case-insensitive)
     * @param page Page number
     * @param size Page size
     * @param sortBy Sort field
     * @param sortOrder Sort order
     * @return PagedResponse of banned devices matching the device ID
     */
    public PagedResponse<BannedDeviceResponse> searchBannedDevicesByDeviceId(String deviceId, int page, int size, 
                                                                             String sortBy, String sortOrder) {
        String requestId = RequestContext.getRequestId();
        log.info("Searching banned devices by device ID: {} [RequestID: {}]", deviceId, requestId);
        
        try {
            Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            
            Page<BannedDevice> bannedDevicePage = bannedDeviceRepository.findByDeviceIdContainingIgnoreCase(deviceId, pageable);
            
            List<BannedDeviceResponse> responses = bannedDevicePage.getContent().stream()
                    .map(BannedDeviceResponse::fromBannedDevice)
                    .collect(Collectors.toList());
            
            log.info("Found {} banned devices matching device ID '{}' [RequestID: {}]", responses.size(), deviceId, requestId);
            return PagedResponse.of(responses, page, size, bannedDevicePage.getTotalElements());
            
        } catch (Exception e) {
            log.error("Error searching banned devices by device ID '{}' [RequestID: {}]: {}", deviceId, requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to search banned devices: " + e.getMessage(), e);
        }
    }
    
    /**
     * Search banned devices by email
     * @param email Email to search for (case-insensitive)
     * @param page Page number
     * @param size Page size
     * @param sortBy Sort field
     * @param sortOrder Sort order
     * @return PagedResponse of banned devices containing the email
     */
    public PagedResponse<BannedDeviceResponse> searchBannedDevicesByEmail(String email, int page, int size, 
                                                                          String sortBy, String sortOrder) {
        String requestId = RequestContext.getRequestId();
        log.info("Searching banned devices by email: {} [RequestID: {}]", email, requestId);
        
        try {
            Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            
            Page<BannedDevice> bannedDevicePage = bannedDeviceRepository.findByEmailsContaining(email, pageable);
            
            List<BannedDeviceResponse> responses = bannedDevicePage.getContent().stream()
                    .map(BannedDeviceResponse::fromBannedDevice)
                    .collect(Collectors.toList());
            
            log.info("Found {} banned devices containing email '{}' [RequestID: {}]", responses.size(), email, requestId);
            return PagedResponse.of(responses, page, size, bannedDevicePage.getTotalElements());
            
        } catch (Exception e) {
            log.error("Error searching banned devices by email '{}' [RequestID: {}]: {}", email, requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to search banned devices: " + e.getMessage(), e);
        }
    }
    
    /**
     * Comprehensive search across device ID, reason, and emails
     * @param searchTerm Term to search for across all fields (case-insensitive)
     * @param page Page number
     * @param size Page size
     * @param sortBy Sort field
     * @param sortOrder Sort order
     * @return PagedResponse of banned devices matching the search term
     */
    public PagedResponse<BannedDeviceResponse> searchBannedDevices(String searchTerm, int page, int size, 
                                                                   String sortBy, String sortOrder) {
        String requestId = RequestContext.getRequestId();
        log.info("Comprehensive search for banned devices with term: {} [RequestID: {}]", searchTerm, requestId);
        
        try {
            Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            
            Page<BannedDevice> bannedDevicePage = bannedDeviceRepository
                    .findByDeviceIdOrReasonOrEmailsContainingIgnoreCase(searchTerm, pageable);
            
            List<BannedDeviceResponse> responses = bannedDevicePage.getContent().stream()
                    .map(BannedDeviceResponse::fromBannedDevice)
                    .collect(Collectors.toList());
            
            log.info("Found {} banned devices matching search term '{}' [RequestID: {}]", responses.size(), searchTerm, requestId);
            return PagedResponse.of(responses, page, size, bannedDevicePage.getTotalElements());
            
        } catch (Exception e) {
            log.error("Error searching banned devices with term '{}' [RequestID: {}]: {}", searchTerm, requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to search banned devices: " + e.getMessage(), e);
        }
    }
    
    /**
     * Unban a device (remove from banned devices collection)
     * @param deviceId The device ID to unban
     * @return true if device was unbanned, false if device was not banned
     */
    public boolean unbanDevice(String deviceId) {
        String requestId = RequestContext.getRequestId();
        log.info("Unbanning device {} [RequestID: {}]", deviceId, requestId);
        
        try {
            Optional<BannedDevice> bannedDevice = bannedDeviceRepository.findByDeviceId(deviceId);
            if (bannedDevice.isPresent()) {
                bannedDeviceRepository.delete(bannedDevice.get());
                log.info("Successfully unbanned device {} [RequestID: {}]", deviceId, requestId);
                return true;
            } else {
                log.warn("Device {} was not banned [RequestID: {}]", deviceId, requestId);
                return false;
            }
        } catch (Exception e) {
            log.error("Error unbanning device {} [RequestID: {}]: {}", deviceId, requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to unban device: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check if an email is in any banned device's email list and throw exception if banned
     * @param email The email to check
     * @throws AccountBannedException if the email is found in any banned device
     */
    public void checkEmailBanned(String email) throws AccountBannedException {
        String requestId = RequestContext.getRequestId();
        log.info("Checking if email {} is banned [RequestID: {}]", email, requestId);
        
        try {
            List<BannedDevice> allBannedDevices = bannedDeviceRepository.findAll();
            
            for (BannedDevice bannedDevice : allBannedDevices) {
                if (bannedDevice.getEmails() != null && bannedDevice.getEmails().contains(email)) {
                    log.warn("Email {} found in banned device {} [RequestID: {}]", 
                            email, bannedDevice.getDeviceId(), requestId);
                    throw new AccountBannedException(email, bannedDevice.getDeviceId(), bannedDevice.getReason());
                }
            }
            
            log.info("Email {} is not banned [RequestID: {}]", email, requestId);
            
        } catch (AccountBannedException e) {
            // Re-throw the AccountBannedException
            throw e;
        } catch (Exception e) {
            log.error("Error checking if email {} is banned [RequestID: {}]: {}", email, requestId, e.getMessage(), e);
            // Don't throw exception for technical errors - allow login to proceed
            // This ensures that database issues don't prevent legitimate users from logging in
        }
    }
    
    /**
     * Check if an email is in any banned device's email list (returns boolean instead of throwing exception)
     * @param email The email to check
     * @return true if email is banned, false otherwise
     */
    public boolean isEmailBanned(String email) {
        String requestId = RequestContext.getRequestId();
        log.info("Checking banned status for email {} [RequestID: {}]", email, requestId);
        
        try {
            List<BannedDevice> allBannedDevices = bannedDeviceRepository.findAll();
            
            for (BannedDevice bannedDevice : allBannedDevices) {
                if (bannedDevice.getEmails() != null && bannedDevice.getEmails().contains(email)) {
                    log.info("Email {} is banned (device: {}) [RequestID: {}]", 
                            email, bannedDevice.getDeviceId(), requestId);
                    return true;
                }
            }
            
            log.info("Email {} is not banned [RequestID: {}]", email, requestId);
            return false;
            
        } catch (Exception e) {
            log.error("Error checking banned status for email {} [RequestID: {}]: {}", email, requestId, e.getMessage(), e);
            // Return false on error to allow login to proceed
            return false;
        }
    }
    
    /**
     * Check email ban status and return detailed information
     * @param email The email to check
     * @return EmailBanCheckResponse with ban details if banned
     */
    public EmailBanCheckResponse checkEmailBanStatus(String email) {
        String requestId = RequestContext.getRequestId();
        log.info("Checking email ban status for {} [RequestID: {}]", email, requestId);
        
        try {
            List<BannedDevice> allBannedDevices = bannedDeviceRepository.findAll();
            
            for (BannedDevice bannedDevice : allBannedDevices) {
                if (bannedDevice.getEmails() != null && bannedDevice.getEmails().contains(email)) {
                    log.info("Email {} is banned (device: {}) [RequestID: {}]", 
                            email, bannedDevice.getDeviceId(), requestId);
                    
                    return EmailBanCheckResponse.banned(
                            email,
                            bannedDevice.getDeviceId(),
                            bannedDevice.getReason(),
                            bannedDevice.getCreatedAt(),
                            bannedDevice.getEmails()
                    );
                }
            }
            
            log.info("Email {} is not banned [RequestID: {}]", email, requestId);
            return EmailBanCheckResponse.notBanned(email);
            
        } catch (Exception e) {
            log.error("Error checking email ban status for {} [RequestID: {}]: {}", email, requestId, e.getMessage(), e);
            return EmailBanCheckResponse.error(email, "Error checking ban status: " + e.getMessage());
        }
    }
    
    /**
     * Update a banned device's reason and manage emails
     * @param deviceId The device ID to update
     * @param reason The new reason for banning
     * @param emailsToAdd List of emails to add (optional)
     * @param emailsToRemove List of emails to remove (optional)
     * @return Updated BannedDeviceResponse
     */
    public BannedDeviceResponse updateBannedDevice(String deviceId, String reason, 
                                                   List<String> emailsToAdd, List<String> emailsToRemove) {
        String requestId = RequestContext.getRequestId();
        log.info("Updating banned device {} with reason: {}, emails to add: {}, emails to remove: {} [RequestID: {}]", 
                deviceId, reason, emailsToAdd != null ? emailsToAdd.size() : 0, 
                emailsToRemove != null ? emailsToRemove.size() : 0, requestId);
        
        try {
            // Find the banned device
            Optional<BannedDevice> optionalBannedDevice = bannedDeviceRepository.findByDeviceId(deviceId);
            if (optionalBannedDevice.isEmpty()) {
                log.error("Device {} is not banned, cannot update [RequestID: {}]", deviceId, requestId);
                throw new RuntimeException("Device is not banned");
            }
            
            BannedDevice bannedDevice = optionalBannedDevice.get();
            
            // Update the reason
            bannedDevice.setReason(reason);
            
            // Get current emails list (create new list to avoid modifying the original)
            List<String> currentEmails = bannedDevice.getEmails() != null ? 
                    new java.util.ArrayList<>(bannedDevice.getEmails()) : new java.util.ArrayList<>();
            
            // Add new emails if provided
            if (emailsToAdd != null && !emailsToAdd.isEmpty()) {
                for (String email : emailsToAdd) {
                    if (email != null && !email.trim().isEmpty() && !currentEmails.contains(email)) {
                        currentEmails.add(email.trim());
                        log.info("Added email {} to banned device {} [RequestID: {}]", email, deviceId, requestId);
                    }
                }
            }
            
            // Remove emails if provided
            if (emailsToRemove != null && !emailsToRemove.isEmpty()) {
                for (String email : emailsToRemove) {
                    if (email != null && !email.trim().isEmpty()) {
                        boolean removed = currentEmails.remove(email.trim());
                        if (removed) {
                            log.info("Removed email {} from banned device {} [RequestID: {}]", email, deviceId, requestId);
                        } else {
                            log.warn("Email {} was not found in banned device {} [RequestID: {}]", email, deviceId, requestId);
                        }
                    }
                }
            }
            
            // Update the emails list
            bannedDevice.setEmails(currentEmails);
            bannedDevice.setUpdatedAt(LocalDateTime.now());
            
            // Save the updated banned device
            BannedDevice updated = bannedDeviceRepository.save(bannedDevice);
            log.info("Successfully updated banned device {} with {} emails [RequestID: {}]", 
                    deviceId, currentEmails.size(), requestId);
            
            return BannedDeviceResponse.fromBannedDevice(updated);
            
        } catch (Exception e) {
            log.error("Error updating banned device {} [RequestID: {}]: {}", deviceId, requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to update banned device: " + e.getMessage(), e);
        }
    }
    
    /**
     * Helper method to fetch all user emails associated with a device ID
     * @param deviceId The device ID
     * @return List of email addresses
     */
    private List<String> fetchEmailsByDeviceId(String deviceId) {
        List<User> users = userRepository.findAllByDeviceId(deviceId);
        return users.stream()
                .map(User::getEmail)
                .distinct() // Remove duplicates if any
                .collect(Collectors.toList());
    }
}
