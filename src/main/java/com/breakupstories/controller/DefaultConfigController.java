package com.breakupstories.controller;

import com.breakupstories.dto.DefaultConfigRequest;
import com.breakupstories.dto.DefaultConfigResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.service.DefaultConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/configs")
@RequiredArgsConstructor
@Tag(name = "Default Configs", description = "Application default configuration management APIs")
public class DefaultConfigController {
    private final DefaultConfigService defaultConfigService;

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
} 