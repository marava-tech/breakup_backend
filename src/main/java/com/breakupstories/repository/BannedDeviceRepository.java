package com.breakupstories.repository;

import com.breakupstories.model.BannedDevice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BannedDeviceRepository extends MongoRepository<BannedDevice, String> {
    
    /**
     * Find banned device by device ID
     */
    Optional<BannedDevice> findByDeviceId(String deviceId);
    
    /**
     * Check if device ID is banned
     */
    boolean existsByDeviceId(String deviceId);
    
    /**
     * Find all banned devices with pagination
     */
    @NonNull
    Page<BannedDevice> findAll(@NonNull Pageable pageable);
    
    /**
     * Find banned devices by reason containing (case-insensitive)
     */
    Page<BannedDevice> findByReasonContainingIgnoreCase(String reason, Pageable pageable);
    
    /**
     * Find banned devices by device ID containing (case-insensitive)
     */
    Page<BannedDevice> findByDeviceIdContainingIgnoreCase(String deviceId, Pageable pageable);
    
    /**
     * Find banned devices by email in the emails list
     */
    Page<BannedDevice> findByEmailsContaining(String email, Pageable pageable);
    
    /**
     * Find banned devices by device ID or reason containing (case-insensitive)
     */
    @org.springframework.data.mongodb.repository.Query(
        "{ $or: [ " +
        "  { 'deviceId': { $regex: ?0, $options: 'i' } }, " +
        "  { 'reason': { $regex: ?0, $options: 'i' } } " +
        "] }"
    )
    Page<BannedDevice> findByDeviceIdOrReasonContainingIgnoreCase(String searchTerm, Pageable pageable);
    
    /**
     * Find banned devices by device ID, reason, or emails containing the search term
     */
    @org.springframework.data.mongodb.repository.Query(
        "{ $or: [ " +
        "  { 'deviceId': { $regex: ?0, $options: 'i' } }, " +
        "  { 'reason': { $regex: ?0, $options: 'i' } }, " +
        "  { 'emails': { $regex: ?0, $options: 'i' } } " +
        "] }"
    )
    Page<BannedDevice> findByDeviceIdOrReasonOrEmailsContainingIgnoreCase(String searchTerm, Pageable pageable);
}
