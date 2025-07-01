package com.breakupstories.controller;

import com.breakupstories.dto.UploadResponse;
import com.breakupstories.service.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Upload", description = "File upload APIs")
public class UploadController {
    
    private final UploadService uploadService;
    
    @PostMapping("/file")
    @Operation(summary = "Upload single file", description = "Upload a single file to the external service")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        log.info("Received single file upload request: {} ({} bytes)", 
            file.getOriginalFilename(), file.getSize());
        
       String fileUrl = uploadService.uploadSingleFile(file);
        
        log.info("Single file upload completed successfully:  -> {} URLs",
            file.getOriginalFilename());
        
        return ResponseEntity.ok(fileUrl);
    }

} 