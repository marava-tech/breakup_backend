package com.breakupstories.service;

import com.breakupstories.dto.UploadResponse;
import com.breakupstories.exception.FileUploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadServiceImpl implements UploadService {
    
    private final RestTemplate restTemplate;
    
    @Value("${upload.service.url:http://localhost:9090}")
    private String uploadServiceUrl;
    
    @Value("${upload.service.endpoint:/api/v1/upload}")
    private String uploadEndpoint;
    
    @Override
    public UploadResponse uploadFile(MultipartFile file) {
        log.info("Uploading single file: {} ({} bytes)", file.getOriginalFilename(), file.getSize());
        
        try {
            HttpHeaders headers = createHeaders();
            MultiValueMap<String, Object> body = createRequestBody(List.of(file));
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            String url = uploadServiceUrl + uploadEndpoint;
            log.debug("Making request to upload service: {}", url);
            
            ResponseEntity<UploadResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                UploadResponse.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("File uploaded successfully: {} -> {} URLs", 
                    file.getOriginalFilename(), 
                    response.getBody().getData().size());
                return response.getBody();
            } else {
                log.error("Upload service returned non-success status: {}", response.getStatusCode());
                throw new FileUploadException("Upload service returned status: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error uploading file {}: {}", file.getOriginalFilename(), e.getMessage(), e);
            throw new FileUploadException("Failed to upload file: " + file.getOriginalFilename(), e);
        }
    }
    
    @Override
    public UploadResponse uploadFiles(List<MultipartFile> files) {
        log.info("Uploading {} files", files.size());
        
        if (files == null || files.isEmpty()) {
            log.warn("No files provided for upload");
            throw new FileUploadException("No files provided for upload");
        }
        
        try {
            HttpHeaders headers = createHeaders();
            MultiValueMap<String, Object> body = createRequestBody(files);
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            String url = uploadServiceUrl + uploadEndpoint;
            log.debug("Making request to upload service: {}", url);
            
            ResponseEntity<UploadResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                UploadResponse.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Files uploaded successfully: {} files -> {} URLs", 
                    files.size(), 
                    response.getBody().getData().size());
                return response.getBody();
            } else {
                log.error("Upload service returned non-success status: {}", response.getStatusCode());
                throw new FileUploadException("Upload service returned status: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error uploading {} files: {}", files.size(), e.getMessage(), e);
            throw new FileUploadException("Failed to upload files", e);
        }
    }
    
    @Override
    @Async
    public CompletableFuture<String> uploadFileAsync(MultipartFile file) {
        log.info("Starting async upload for file: {} ({} bytes)", file.getOriginalFilename(), file.getSize());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpHeaders headers = createHeaders();
                MultiValueMap<String, Object> body = createRequestBody(List.of(file));
                
                HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
                
                String url = uploadServiceUrl + uploadEndpoint;
                log.debug("Making async request to upload service: {}", url);
                
                ResponseEntity<UploadResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    UploadResponse.class
                );
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    String uploadedUrl = response.getBody().getData().get(0);
                    log.info("Async file upload completed successfully: {} -> {}", 
                        file.getOriginalFilename(), uploadedUrl);
                    return uploadedUrl;
                } else {
                    log.error("Async upload service returned non-success status: {}", response.getStatusCode());
                    throw new FileUploadException("Upload service returned status: " + response.getStatusCode());
                }
                
            } catch (Exception e) {
                log.error("Error in async upload for file {}: {}", file.getOriginalFilename(), e.getMessage(), e);
                throw new FileUploadException("Failed to upload file: " + file.getOriginalFilename(), e);
            }
        });
    }
    
    @Override
    @Async
    public CompletableFuture<List<String>> uploadFilesAsync(List<MultipartFile> files) {
        log.info("Starting async upload for {} files", files.size());
        
        if (files == null || files.isEmpty()) {
            log.warn("No files provided for async upload");
            throw new FileUploadException("No files provided for upload");
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpHeaders headers = createHeaders();
                MultiValueMap<String, Object> body = createRequestBody(files);
                
                HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
                
                String url = uploadServiceUrl + uploadEndpoint;
                log.debug("Making async request to upload service: {}", url);
                
                ResponseEntity<UploadResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    UploadResponse.class
                );
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    List<String> uploadedUrls = response.getBody().getData();
                    log.info("Async files upload completed successfully: {} files -> {} URLs", 
                        files.size(), uploadedUrls.size());
                    return uploadedUrls;
                } else {
                    log.error("Async upload service returned non-success status: {}", response.getStatusCode());
                    throw new FileUploadException("Upload service returned status: " + response.getStatusCode());
                }
                
            } catch (Exception e) {
                log.error("Error in async upload for {} files: {}", files.size(), e.getMessage(), e);
                throw new FileUploadException("Failed to upload files", e);
            }
        });
    }
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        // Set X-App-Key as current date in yyyy-MM-dd format
        String currentDate = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        headers.set("X-App-Key", currentDate);
        
        log.debug("Created headers with X-App-Key: {}", currentDate);
        return headers;
    }
    
    private MultiValueMap<String, Object> createRequestBody(List<MultipartFile> files) throws IOException {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        
        for (MultipartFile file : files) {
            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            
            body.add("files", fileResource);
            log.debug("Added file to request body: {} ({} bytes)", 
                file.getOriginalFilename(), file.getSize());
        }
        
        return body;
    }
} 