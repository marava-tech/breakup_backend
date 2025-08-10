package com.breakupstories.controller;


import com.breakupstories.dto.QuoteResponse;

import com.breakupstories.service.DefaultConfigService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audio")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Audio", description = "Audio-related APIs")
public class AudioController {
    
    private final DefaultConfigService defaultConfigService;


    @GetMapping("/quotes")
    @Operation(summary = "Get random quote combinations", description = "Get random combinations of quote text, audio, and image for creating videos")
    public ResponseEntity<List<QuoteResponse>> getQuotes(
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("Quote combinations request with limit: {}", limit);
        try {
            List<QuoteResponse> quotes = defaultConfigService.getRandomQuotes(limit);
            log.info("Successfully retrieved {} quote combinations", quotes.size());
            return ResponseEntity.ok(quotes);
        } catch (Exception e) {
            log.error("Failed to get quote combinations", e);
            return ResponseEntity.internalServerError().build();
        }
    }
} 