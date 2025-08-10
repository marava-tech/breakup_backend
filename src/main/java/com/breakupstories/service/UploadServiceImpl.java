package com.breakupstories.service;

import com.breakupstories.exception.FileUploadException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import com.cloudinary.Cloudinary;

import com.cloudinary.Transformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.LinkedBlockingQueue;

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
                    uploadOptions.put("eager", Collections.singletonList(
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
                    uploadOptions.put("eager", Collections.singletonList(videoTransformation));
                    uploadOptions.put("eager_async", true);

                } else if (contentType.startsWith("audio/")) {
                    uploadOptions.put("resource_type", "video"); // Cloudinary uses 'video' for audio
                    uploadOptions.put("format", "mp3"); // Force MP3 format for consistency
                    uploadOptions.put("quality", "auto:low"); // Aggressive compression

                    // Simplified audio transformation
                    Transformation audioTransformation = new Transformation()
                            .quality("auto:low");

                    uploadOptions.put("transformation", audioTransformation);
                    
                    // Add better error handling for audio files
                    uploadOptions.put("invalidate", true);
                    uploadOptions.put("overwrite", true);

                } else if (contentType.equals("application/octet-stream") && isAudioFile(originalFilename)) {
                    // Handle audio files with generic content type
                    uploadOptions.put("resource_type", "video"); // Cloudinary uses 'video' for audio
                    uploadOptions.put("format", "mp3"); // Force MP3 format for consistency
                    uploadOptions.put("quality", "auto:low"); // Aggressive compression

                    // Simplified audio transformation
                    Transformation audioTransformation = new Transformation()
                            .quality("auto:low");

                    uploadOptions.put("transformation", audioTransformation);
                    
                    // Add better error handling for audio files
                    uploadOptions.put("invalidate", true);
                    uploadOptions.put("overwrite", true);
                    
                    log.info("Detected audio file with generic content type: {} -> treating as audio", originalFilename);

                } else {
                    uploadOptions.put("resource_type", "raw");
                }
            }

            // Add public_id for better organization (will be prefixed with folder path)
            if (originalFilename != null) {
                // Split filename into base and extension
                String baseName = originalFilename;
                String fileExtension = "";
                int lastDotIndex = originalFilename.lastIndexOf('.');
                if (lastDotIndex > 0 && lastDotIndex < originalFilename.length() - 1) {
                    baseName = originalFilename.substring(0, lastDotIndex);
                    fileExtension = originalFilename.substring(lastDotIndex); // Include the dot
                }
                // Clean up the base name by replacing invalid characters
                baseName = baseName.replaceAll("[^a-zA-Z0-9.-]", "_");
                // Generate UUID
                String uuid = UUID.randomUUID().toString();
                
                // Include extension in public_id for proper URL generation
                String publicId;
                if ((contentType != null && contentType.startsWith("audio/")) || 
                    (contentType != null && contentType.equals("application/octet-stream") && isAudioFile(originalFilename))) {
                    // For audio files, force .mp3 extension
                    publicId = baseName + "-" + uuid + ".mp3";
                } else if (contentType != null && contentType.startsWith("image/")) {
                    // For image files, include the original extension or determine from content type
                    if (fileExtension.isEmpty()) {
                        // If no extension in filename, determine from content type
                        fileExtension = getExtensionFromContentType(contentType);
                    }
                    publicId = baseName + "-" + uuid + fileExtension;
                } else {
                    // For other files, include extension if available
                    publicId = baseName + "-" + uuid + fileExtension;
                }
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
                
                // Provide more specific error messages
                String errorMessage = e.getMessage();
                if (errorMessage != null) {
                    if (errorMessage.contains("Unsupported video format")) {
                        log.error("Audio file format not supported by Cloudinary. File: {}, Content-Type: {}", 
                                originalFilename, contentType);
                        throw new FileUploadException("Audio file format not supported. Please use MP3, WAV, or other common audio formats.");
                    } else if (errorMessage.contains("Invalid file")) {
                        log.error("Invalid audio file. File: {}, Size: {} bytes", originalFilename, file.getSize());
                        throw new FileUploadException("Invalid audio file. Please check the file format and try again.");
                    } else if (errorMessage.contains("File too large")) {
                        log.error("Audio file too large. File: {}, Size: {} bytes", originalFilename, file.getSize());
                        throw new FileUploadException("Audio file too large. Please use a smaller file.");
                    }
                }
                
                throw new FileUploadException("Failed to upload audio file: " + errorMessage, e);
            }

        } catch (Exception e) {
            log.error("Error uploading file: {}", file.getOriginalFilename(), e);
            throw new FileUploadException("Error uploading file: " + file.getOriginalFilename(), e);
        }
    }
    
    /**
     * Check if a file has an audio extension
     */
    private boolean isAudioFile(String filename) {
        if (filename == null) {
            return false;
        }
        
        String extension = filename.toLowerCase();
        return extension.endsWith(".mp3") || 
               extension.endsWith(".wav") || 
               extension.endsWith(".m4a") || 
               extension.endsWith(".aac") || 
               extension.endsWith(".ogg") || 
               extension.endsWith(".flac") ||
               extension.endsWith(".wma") ||
               extension.endsWith(".aiff");
    }
    
    /**
     * Get file extension from content type
     */
    private String getExtensionFromContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        
        switch (contentType.toLowerCase()) {
            // Image types
            case "image/jpeg":
            case "image/jpg":
                return ".jpg";
            case "image/png":
                return ".png";
            case "image/gif":
                return ".gif";
            case "image/webp":
                return ".webp";
            case "image/bmp":
                return ".bmp";
            case "image/tiff":
                return ".tiff";
            case "image/svg+xml":
                return ".svg";
            
            // Audio types
            case "audio/mpeg":
            case "audio/mp3":
                return ".mp3";
            case "audio/wav":
                return ".wav";
            case "audio/ogg":
                return ".ogg";
            case "audio/aac":
                return ".aac";
            case "audio/m4a":
                return ".m4a";
            
            // Video types
            case "video/mp4":
                return ".mp4";
            case "video/webm":
                return ".webm";
            case "video/avi":
                return ".avi";
            
            // Document types
            case "application/pdf":
                return ".pdf";
            case "text/plain":
                return ".txt";
            
            default:
                return "";
        }
    }
}