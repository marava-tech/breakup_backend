package com.breakupstories.controller;

import com.breakupstories.dto.DefaultConfigRequest;
import com.breakupstories.dto.DefaultConfigResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.service.DefaultConfigService;
import com.breakupstories.service.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/configs")
@RequiredArgsConstructor
@Tag(name = "Default Configs", description = "Application default configuration management APIs")
@Slf4j
public class DefaultConfigController {
    private final DefaultConfigService defaultConfigService;
    private final UploadService uploadService;

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Create a new config entry")
    public ResponseEntity<DefaultConfigResponse> create(@Valid @RequestBody DefaultConfigRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(defaultConfigService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Update a config entry")
    public ResponseEntity<DefaultConfigResponse> update(@PathVariable String id, @Valid @RequestBody DefaultConfigRequest request) {
        return ResponseEntity.ok(defaultConfigService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Delete a config entry")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        defaultConfigService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Get config by ID")
    public ResponseEntity<DefaultConfigResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(defaultConfigService.getById(id));
    }

    @GetMapping("/key/{key}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Get config by key")
    public ResponseEntity<DefaultConfigResponse> getByKey(@PathVariable String key) {
        return ResponseEntity.ok(defaultConfigService.getByKey(key));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Get all configs (paginated)")
    public ResponseEntity<PagedResponse<DefaultConfigResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(defaultConfigService.getAll(page, size));
    }

    @GetMapping("/languages")
    @Operation(summary = "Get list of available languages")
    public ResponseEntity<List<String>> getLanguages() {
        return ResponseEntity.ok(defaultConfigService.getLanguages());
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('ADMIN')")
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
            var uploadResponse = uploadService.uploadFile(file);
            
            if (uploadResponse.getData() == null || uploadResponse.getData().isEmpty()) {
                log.error("File upload failed - no URL returned for file: {}", file.getOriginalFilename());
                return ResponseEntity.badRequest().build();
            }
            
            // Get the first uploaded URL
            String fileUrl = uploadResponse.getData().get(0);
            log.info("File uploaded successfully. URL: {}", fileUrl);
            
            // Create or update the config
            DefaultConfigRequest configRequest = DefaultConfigRequest.builder()
                    .key(key.trim())
                    .value(fileUrl)
                    .description(description != null ? description.trim() : "File uploaded for key: " + key)
                    .active(active)
                    .build();
            
            // Check if config already exists for this key
            try {
                var existingConfig = defaultConfigService.getByKey(key);
                // Update existing config
                log.info("Updated existing config for key: {} with file URL: {}", key, fileUrl);
                return ResponseEntity.ok(defaultConfigService.update(existingConfig.getId(), configRequest));
            } catch (Exception e) {
                // Create new config
                log.info("Created new config for key: {} with file URL: {}", key, fileUrl);
                return ResponseEntity.status(HttpStatus.CREATED).body(defaultConfigService.create(configRequest));
            }
            
        } catch (Exception e) {
            // Log the error for debugging
            log.error("Error in uploadFileAndSaveConfig: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
} 