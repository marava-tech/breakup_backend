package com.breakupstories.controller;

import com.breakupstories.dto.LocationInfoRequest;
import com.breakupstories.dto.LocationInfoResponse;
import com.breakupstories.dto.ErrorResponse;
import com.breakupstories.service.AIService;
import com.breakupstories.exception.AIServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for location info functionality
 */
@RestController
@RequestMapping("/api/location-info")
@RequiredArgsConstructor
@Slf4j
public class LocationInfoController {
    
    private final AIService aiService;
    
    /**
     * Get location information from coordinates
     * POST /api/location-info/get-location
     */
    @PostMapping("/get-location")
    public ResponseEntity<?> getLocationInfo(@RequestBody LocationInfoRequest request) {
        log.info("Location info request - Latitude: {}, Longitude: {}", 
                request.getLatitude(), request.getLongitude());
        
        try {
            LocationInfoResponse response = aiService.getLocationInfo(
                    request.getLatitude(), 
                    request.getLongitude()
            );
            
            if (response.getSuccess()) {
                log.info("Location info successful - District: {}, State: {}, Pincode: {}", 
                        response.getDistrict(), response.getState(), response.getPincode());
                
                return ResponseEntity.ok(response);
            } else {
                log.error("Location info failed - Error: {}", response.getError());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (AIServiceException e) {
            log.error("AI Service error in location info: {} - Service: {}, Error Code: {}", 
                    e.getMessage(), e.getServiceName(), e.getErrorCode());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error(e.getErrorCode())
                            .message(e.getMessage())
                            .service(e.getServiceName())
                            .timestamp(System.currentTimeMillis())
                            .build());
        } catch (Exception e) {
            log.error("Unexpected error in location info: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.builder()
                            .error("INTERNAL_SERVER_ERROR")
                            .message("An unexpected error occurred during location info retrieval")
                            .service("Location Info")
                            .timestamp(System.currentTimeMillis())
                            .build());
        }
    }
    
    /**
     * Get location information (GET endpoint for testing)
     * GET /api/location-info/get-location?latitude=...&longitude=...
     */
    @GetMapping("/get-location")
    public ResponseEntity<?> getLocationInfoGet(
            @RequestParam Double latitude,
            @RequestParam Double longitude) {
        
        log.info("Location info GET request - Latitude: {}, Longitude: {}", latitude, longitude);
        
        try {
            LocationInfoRequest request = LocationInfoRequest.builder()
                    .latitude(latitude)
                    .longitude(longitude)
                    .build();
            
            LocationInfoResponse response = aiService.getLocationInfo(
                    request.getLatitude(), 
                    request.getLongitude()
            );
            
            if (response.getSuccess()) {
                log.info("Location info successful - District: {}, State: {}, Pincode: {}", 
                        response.getDistrict(), response.getState(), response.getPincode());
                
                return ResponseEntity.ok(response);
            } else {
                log.error("Location info failed - Error: {}", response.getError());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (AIServiceException e) {
            log.error("AI Service error in location info: {} - Service: {}, Error Code: {}", 
                    e.getMessage(), e.getServiceName(), e.getErrorCode());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error(e.getErrorCode())
                            .message(e.getMessage())
                            .service(e.getServiceName())
                            .timestamp(System.currentTimeMillis())
                            .build());
        } catch (Exception e) {
            log.error("Unexpected error in location info: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.builder()
                            .error("INTERNAL_SERVER_ERROR")
                            .message("An unexpected error occurred during location info retrieval")
                            .service("Location Info")
                            .timestamp(System.currentTimeMillis())
                            .build());
        }
    }
} 