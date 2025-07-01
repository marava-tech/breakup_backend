package com.breakupstories.service;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.List;

public interface UploadService {

    /**
     * Upload files from the multipart request
     *
     * @param request the multipart HTTP request containing files
     * @return list of uploaded file URLs
     */
    List<String> upload(MultipartHttpServletRequest request);

    /**
     * Upload a single file synchronously
     * @param file The file to upload
     * @return The uploaded file URL
     */
    String uploadSingleFile(MultipartFile file);
} 