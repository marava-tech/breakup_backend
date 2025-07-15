package com.breakupstories.controller;

import com.breakupstories.dto.AppConfigResponse;
import com.breakupstories.dto.DefaultConfigRequest;
import com.breakupstories.dto.DefaultConfigResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.dto.StoryCreationConfigResponse;
import com.breakupstories.dto.StoryCreationEligibilityResponse;
import com.breakupstories.service.DefaultConfigService;
import com.breakupstories.service.UploadService;
import com.breakupstories.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/configs")
@RequiredArgsConstructor
@Tag(name = "Default Configs", description = "Application default configuration management APIs")
@Slf4j
public class DefaultConfigController {
    private final DefaultConfigService defaultConfigService;
    private final UploadService uploadService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Create a new config entry")
    public ResponseEntity<DefaultConfigResponse> create(@Valid @RequestBody DefaultConfigRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(defaultConfigService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Update a config entry")
    public ResponseEntity<DefaultConfigResponse> update(@PathVariable String id, @Valid @RequestBody DefaultConfigRequest request) {
        return ResponseEntity.ok(defaultConfigService.update(id, request));
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Delete a config entry")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        defaultConfigService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get config by ID")
    public ResponseEntity<DefaultConfigResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(defaultConfigService.getById(id));
    }

    
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Search configs by key containing search term with pagination", 
               description = "Search for configs where the key contains the provided search term (case-insensitive) with pagination support")
    public ResponseEntity<Map<String, Object>> searchByKey(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            PagedResponse<DefaultConfigResponse> results = defaultConfigService.searchByKeyWithPagination(searchTerm, activeOnly, page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", results.getContent());
            response.put("pagination", Map.of(
                "page", results.getPage(),
                "size", results.getSize(),
                "totalElements", results.getTotalElements(),
                "totalPages", results.getTotalPages(),
                "last", results.isLast()
            ));
            response.put("searchTerm", searchTerm);
            response.put("activeOnly", activeOnly);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("message", "Failed to search configs");
            errorResponse.put("searchTerm", searchTerm);
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/languages")
    @Operation(summary = "Get list of available languages")
    public ResponseEntity<List<String>> getLanguages() {
        return ResponseEntity.ok(defaultConfigService.getLanguages());
    }

    @GetMapping("/by-prefix")
    @Operation(summary = "Get configuration settings by prefix", 
               description = "Retrieve all configuration settings with a specific prefix for UI control and restrictions")
    public ResponseEntity<AppConfigResponse> getConfigurationsByPrefix(
            @RequestParam String configPrefix) {
        try {
            AppConfigResponse response = defaultConfigService.getConfigurationsByPrefix(configPrefix);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving configurations with prefix '{}': {}", configPrefix, e.getMessage(), e);
            AppConfigResponse errorResponse = AppConfigResponse.builder()
                    .configs(Map.of())
                    .totalConfigs(0)
                    .message("Failed to retrieve configurations for prefix '" + configPrefix + "': " + e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/story-creation-config")
    @Operation(summary = "Get story creation configuration", 
               description = "Get all story creation configuration settings with parsed values and user eligibility information")
    public ResponseEntity<StoryCreationConfigResponse> getStoryCreationConfig(
            Authentication authentication) {
        try {
            String userId = null;
            
            // Get user ID if authenticated
            if (authentication != null && authentication.isAuthenticated()) {
                String email = authentication.getName();
                userId = userService.getUserEntityByEmail(email).getId();
            }
            
            StoryCreationConfigResponse response = defaultConfigService.getStoryCreationConfig(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving story creation configuration: {}", e.getMessage(), e);
            StoryCreationConfigResponse errorResponse = StoryCreationConfigResponse.builder()
                    .configs(Map.of())
                    .totalConfigs(0)
                    .message("Error retrieving story creation configuration: " + e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }



    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Upload a file and save the URL as a value in the default config")
    public ResponseEntity<DefaultConfigResponse> uploadFileAndSaveConfig(
            @RequestParam String key, 
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "true") boolean active) {
        
        try {
            // Validate input
            if (key == null || key.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            // Upload the file
            log.info("Uploading file: {} ({} bytes) for config key: {}", 
                file.getOriginalFilename(), file.getSize(), key);
            var uploadResponse = uploadService.uploadSingleFile(file);
            
            if (ObjectUtils.isEmpty(uploadResponse)) {
                log.error("File upload failed - no URL returned for file: {}", file.getOriginalFilename());
                return ResponseEntity.badRequest().build();
            }
            
            // Get the first uploaded URL
            log.info("File uploaded successfully. URL: {}", uploadResponse);
            
            // Create or update the config
            DefaultConfigRequest configRequest = DefaultConfigRequest.builder()
                    .key(key.trim())
                    .value(uploadResponse)
                    .description(description != null ? description.trim() : "File uploaded for key: " + key)
                    .active(active)
                    .build();
            
            // Check if config already exists for this key
            try {
                var existingConfig = defaultConfigService.getByKey(key);
                // Update existing config
                log.info("Updated existing config for key: {} with file URL: {}", key, uploadResponse);
                return ResponseEntity.ok(defaultConfigService.update(existingConfig.getId(), configRequest));
            } catch (Exception e) {
                // Create new config
                log.info("Created new config for key: {} with file URL: {}", key, uploadResponse);
                return ResponseEntity.status(HttpStatus.CREATED).body(defaultConfigService.create(configRequest));
            }
            
        } catch (Exception e) {
            // Log the error for debugging
            log.error("Error in uploadFileAndSaveConfig: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/verify-startup-data")
    public ResponseEntity<Map<String, Object>> verifyStartupData() {
        try {
            Map<String, Object> response = new HashMap<>();
            
            // Check default story images
            List<String> storyImages = defaultConfigService.getDefaultStoryImages();
            response.put("defaultStoryImages", storyImages);
            response.put("storyImageCount", storyImages.size());
            
            // Check default thumbnail
            String defaultThumbnail = defaultConfigService.getDefaultThumbnailUrl();
            response.put("defaultThumbnail", defaultThumbnail);
            
            // Check languages
            List<String> languages = defaultConfigService.getLanguages();
            response.put("languages", languages);
            response.put("languageCount", languages.size());
            
            response.put("success", true);
            response.put("message", "Startup data verification completed");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "Failed to verify startup data");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
} 