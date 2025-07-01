package com.breakupstories.service;

import com.breakupstories.exception.FileUploadException;
import com.breakupstories.service.UploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.cloudinary.Transformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Arrays;
import java.util.UUID;

@Service
@Slf4j
public class UploadServiceImpl implements UploadService {

    @Autowired
    private Cloudinary cloudinary;

    // Optimized thread pool for parallel file uploads
    private final ExecutorService executorService = new ThreadPoolExecutor(
            5,  // core pool size
            20, // maximum pool size
            60L, // keep alive time
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100), // work queue
            new ThreadPoolExecutor.CallerRunsPolicy() // rejection policy
    );

    @PreDestroy
    public void cleanup() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public List<String> upload(MultipartHttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            List<MultipartFile> files = request.getFiles("files");
            log.info("{} files found in the request ", files.size());
            if (files.isEmpty()) {
                log.info("No files found in the request");
                throw new FileUploadException("No files provided");
            }

            // Filter out empty files
            List<MultipartFile> validFiles = files.stream()
                    .filter(file -> !file.isEmpty())
                    .toList();

            if (validFiles.isEmpty()) {
                log.info("No valid files found in the request");
                throw new FileUploadException("No valid files provided");
            }

            log.info("Processing {} valid files in parallel", validFiles.size());

            // Create parallel upload tasks with timeout
            List<CompletableFuture<String>> uploadFutures = validFiles.stream()
                    .map(file -> CompletableFuture.supplyAsync(() -> uploadSingleFile(file), executorService)
                            .orTimeout(120, TimeUnit.SECONDS) // 2 minute timeout per file
                            .exceptionally(throwable -> {
                                log.error("Failed to upload file: {}", file.getOriginalFilename(), throwable);
                                return null;
                            }))
                    .toList();

            // Wait for all uploads to complete with timeout
            CompletableFuture<Void> allUploads = CompletableFuture.allOf(
                    uploadFutures.toArray(new CompletableFuture[0])
            );

            try {
                allUploads.get(300, TimeUnit.SECONDS); // 5 minute total timeout
            } catch (Exception e) {
                log.error("Timeout or error waiting for uploads to complete", e);
                throw new FileUploadException("Upload timeout: " + e.getMessage());
            }

            // Get all results, filtering out failed uploads
            List<String> fileUrls = uploadFutures.stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception e) {
                            log.error("Error getting upload result", e);
                            return null;
                        }
                    })
                    .filter(url -> url != null)
                    .toList();

            long endTime = System.currentTimeMillis();
            log.info("Successfully uploaded {} files in parallel. Total time: {}ms",
                    fileUrls.size(), (endTime - startTime));

            return fileUrls;

        } catch (Exception e) {
            log.error("Error uploading files to Cloudinary", e);
            throw new FileUploadException("Error uploading files to Cloudinary: " + e.getMessage());
        }
    }

    /**
     * Upload a single file to Cloudinary with optimized settings
     */
    public String uploadSingleFile(MultipartFile file) {
        long fileStartTime = System.currentTimeMillis();
        try {
            String contentType = file.getContentType();
            String originalFilename = file.getOriginalFilename();
            log.debug("Starting upload for file: {} with content type: {}", originalFilename, contentType);

            Map<String, Object> uploadOptions = new HashMap<>();

            // Set folder to "breakup" for all uploads
            uploadOptions.put("folder", "breakup");

            // Configure upload options based on file type with optimized settings
            if (contentType != null) {
                if (contentType.startsWith("image/")) {
                    uploadOptions.put("resource_type", "image");
                    uploadOptions.put("quality", "auto:low"); // Aggressive compression for smaller file sizes

                    // Simplified transformation without any "auto" values that might cause issues
                    Transformation transformation = new Transformation()
                            .quality("auto:low");

                    uploadOptions.put("transformation", transformation);


                    // Simplified eager transformations
                    uploadOptions.put("eager", Arrays.asList(
                            new Transformation()
                                    .quality("auto:low")
                    ));

                    uploadOptions.put("eager_async", true); // Process optimizations asynchronously

                    // Additional image optimizations
                    uploadOptions.put("overwrite", true);
                    uploadOptions.put("invalidate", true); // Invalidate CDN cache for fresh content

                    // Progressive JPEG for better perceived performance
                    if (contentType.equals("image/jpeg") || contentType.equals("image/jpg")) {
                        uploadOptions.put("flags", "progressive");
                    }

                    // Remove potentially problematic "auto" format settings
                    // uploadOptions.put("fetch_format", "auto"); // Commented out
                    // uploadOptions.put("format", "auto"); // Commented out

                } else if (contentType.startsWith("video/")) {
                    uploadOptions.put("resource_type", "video");
                    uploadOptions.put("chunk_size", 10000000); // 10MB chunks for better performance
                    uploadOptions.put("quality", "auto:low"); // Aggressive compression

                    // Simplified video transformation
                    Transformation videoTransformation = new Transformation()
                            .quality("auto:low");

                    uploadOptions.put("transformation", videoTransformation);
                    uploadOptions.put("eager", Arrays.asList(videoTransformation));
                    uploadOptions.put("eager_async", true);

                } else if (contentType.startsWith("audio/")) {
                    uploadOptions.put("resource_type", "video"); // Cloudinary uses 'video' for audio
                    uploadOptions.put("format", "mp3");
                    uploadOptions.put("quality", "auto:low"); // Aggressive compression

                    // Simplified audio transformation
                    Transformation audioTransformation = new Transformation()
                            .quality("auto:low");

                    uploadOptions.put("transformation", audioTransformation);

                } else {
                    uploadOptions.put("resource_type", "raw");
                }
            }

            // Add public_id for better organization (will be prefixed with folder path)
            if (originalFilename != null) {
                // Split filename into base and extension
                String baseName = originalFilename;
                String extension = "";
                int lastDotIndex = originalFilename.lastIndexOf('.');
                if (lastDotIndex > 0 && lastDotIndex < originalFilename.length() - 1) {
                    baseName = originalFilename.substring(0, lastDotIndex);
                    extension = originalFilename.substring(lastDotIndex); // includes the dot
                }
                // Clean up the base name by replacing invalid characters
                baseName = baseName.replaceAll("[^a-zA-Z0-9.-]", "_");
                // Generate UUID
                String uuid = UUID.randomUUID().toString()+extension;
                // Construct new filename
                String publicId = baseName + "-" + uuid ;
                uploadOptions.put("public_id", publicId);
            }

            // Add async upload for better performance
            uploadOptions.put("async", false); // Keep sync for immediate URL return

            // Enhanced logging for debugging
            log.info("Cloudinary upload options for {}: {}", originalFilename, uploadOptions);
            log.info("Transformation object: {}", uploadOptions.get("transformation"));
            log.info("Eager transformations: {}", uploadOptions.get("eager"));

            try {
                Map uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadOptions);
                String fileUrl = (String) uploadResult.get("url");

                long fileEndTime = System.currentTimeMillis();
                log.info("Successfully uploaded: {} -> {} (took {}ms)",
                        originalFilename, fileUrl, (fileEndTime - fileStartTime));

                return fileUrl;

            } catch (Exception e) {
                log.error("Cloudinary upload failed for {} with options: {}", originalFilename, uploadOptions, e);
                throw e;
            }

        } catch (IOException e) {
            log.error("Error uploading file: {}", file.getOriginalFilename(), e);
            throw new FileUploadException("Error uploading file: " + file.getOriginalFilename(), e);
        }
    }
}