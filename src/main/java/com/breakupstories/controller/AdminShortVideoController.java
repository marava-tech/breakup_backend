package com.breakupstories.controller;

import com.breakupstories.dto.PagedResponse;
import com.breakupstories.dto.ShortVideoRequest;
import com.breakupstories.dto.ShortVideoResponse;
import com.breakupstories.service.ShortVideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/short-videos")
@RequiredArgsConstructor
@Tag(name = "Admin Short Videos", description = "CMS capabilities for managing short videos")
public class AdminShortVideoController {

    private final ShortVideoService shortVideoService;

    @PostMapping
    @Operation(summary = "Create a short video")
    public ResponseEntity<ShortVideoResponse> createShortVideo(@Valid @RequestBody ShortVideoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shortVideoService.createShortVideo(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a short video")
    public ResponseEntity<ShortVideoResponse> updateShortVideo(
            @PathVariable String id,
            @Valid @RequestBody ShortVideoRequest request) {
        return ResponseEntity.ok(shortVideoService.updateShortVideo(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a short video")
    public ResponseEntity<Void> deleteShortVideo(@PathVariable String id) {
        shortVideoService.deleteShortVideo(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Get all short videos (admin)")
    public ResponseEntity<PagedResponse<ShortVideoResponse>> getAllShortVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(shortVideoService.getAllShortVideosForAdmin(page, Math.min(size, 100)));
    }
}
