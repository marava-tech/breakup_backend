package com.breakupstories.service;

import com.breakupstories.dto.DefaultConfigRequest;
import com.breakupstories.dto.DefaultConfigResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.dto.QuoteResponse;
import com.breakupstories.model.DefaultConfig;
import com.breakupstories.repository.DefaultConfigRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefaultConfigService {
    private final DefaultConfigRepository defaultConfigRepository;
    private static final Logger log = LoggerFactory.getLogger(DefaultConfigService.class);

    public DefaultConfigResponse create(DefaultConfigRequest request) {
        if (defaultConfigRepository.existsByKey(request.getKey())) {
            throw new RuntimeException("Config with key already exists: " + request.getKey());
        }
        DefaultConfig config = DefaultConfig.builder()
                .key(request.getKey())
                .value(request.getValue())
                .description(request.getDescription())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();
        return DefaultConfigResponse.fromEntity(defaultConfigRepository.save(config));
    }

    public DefaultConfigResponse update(String id, DefaultConfigRequest request) {
        DefaultConfig config = defaultConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Config not found: " + id));
        config.setKey(request.getKey());
        config.setValue(request.getValue());
        config.setDescription(request.getDescription());
        config.setActive(request.getActive() != null ? request.getActive() : config.isActive());
        return DefaultConfigResponse.fromEntity(defaultConfigRepository.save(config));
    }

    public void delete(String id) {
        if (!defaultConfigRepository.existsById(id)) {
            throw new RuntimeException("Config not found: " + id);
        }
        defaultConfigRepository.deleteById(id);
    }

    public DefaultConfigResponse getById(String id) {
        return DefaultConfigResponse.fromEntity(
                defaultConfigRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Config not found: " + id))
        );
    }

    public DefaultConfigResponse getByKey(String key) {
        return DefaultConfigResponse.fromEntity(
                defaultConfigRepository.findByKey(key)
                        .orElseThrow(() -> new RuntimeException("Config not found for key: " + key))
        );
    }

    public PagedResponse<DefaultConfigResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<DefaultConfig> configPage = defaultConfigRepository.findAll(pageable);
        List<DefaultConfigResponse> configs = configPage.getContent().stream()
                .map(DefaultConfigResponse::fromEntity)
                .collect(Collectors.toList());
        return PagedResponse.of(configs, page, size, configPage.getTotalElements());
    }

    /**
     * Get list of languages from the default configuration
     *
     * @return List of language strings
     */
    public List<String> getLanguages() {
        try {
            DefaultConfig languagesConfig = defaultConfigRepository.findByKey("languages")
                    .orElseThrow(() -> new RuntimeException("Languages configuration not found"));

            if (languagesConfig.getValue() == null || languagesConfig.getValue().trim().isEmpty()) {
                return List.of();
            }

            // Split the value by comma and trim whitespace
            return List.of(languagesConfig.getValue().split(","))
                    .stream()
                    .map(String::trim)
                    .filter(lang -> !lang.isEmpty())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // Return empty list if there's any error
            return List.of();
        }
    }

    /**
     * Get all quote audios from default config
     *
     * @return List of quote audio configurations
     */
    public List<DefaultConfig> getQuoteAudios() {
        return defaultConfigRepository.findAll().stream()
                .filter(config -> config.getKey().startsWith("quote_audio_") && config.isActive())
                .collect(Collectors.toList());
    }

    /**
     * Get all quote images from default config
     *
     * @return List of quote image configurations
     */
    public List<DefaultConfig> getQuoteImages() {
        return defaultConfigRepository.findAll().stream()
                .filter(config -> config.getKey().startsWith("quote_image_") && config.isActive())
                .collect(Collectors.toList());
    }

    /**
     * Get all quote texts from default config
     *
     * @return List of quote text configurations
     */
    public List<DefaultConfig> getQuoteTexts() {
        return defaultConfigRepository.findAll().stream()
                .filter(config -> config.getKey().startsWith("quote_text_") && config.isActive())
                .collect(Collectors.toList());
    }

    /**
     * Get random quote combinations
     *
     * @param limit Maximum number of combinations to return (default 10)
     * @return List of QuoteResponse with random combinations
     */
    public List<QuoteResponse> getRandomQuotes(int limit) {
        List<DefaultConfig> quoteAudios = getQuoteAudios();
        List<DefaultConfig> quoteImages = getQuoteImages();
        List<DefaultConfig> quoteTexts = getQuoteTexts();

        if (quoteAudios.isEmpty() || quoteImages.isEmpty() || quoteTexts.isEmpty()) {
            return new ArrayList<>();
        }

        // Shuffle the lists to get random combinations
        Collections.shuffle(quoteAudios);
        Collections.shuffle(quoteImages);
        Collections.shuffle(quoteTexts);

        List<QuoteResponse> quotes = new ArrayList<>();
        int maxPossible = Math.min(limit, Math.min(quoteAudios.size(), Math.min(quoteImages.size(), quoteTexts.size())));

        for (int i = 0; i < maxPossible; i++) {
            DefaultConfig audio = quoteAudios.get(i);
            DefaultConfig image = quoteImages.get(i);
            DefaultConfig text = quoteTexts.get(i);

            // Extract numbers from keys
            int audioNumber = extractNumberFromKey(audio.getKey());
            int imageNumber = extractNumberFromKey(image.getKey());
            int textNumber = extractNumberFromKey(text.getKey());

            QuoteResponse quote = QuoteResponse.builder()
                    .quoteAudio(audio.getValue())
                    .quoteImage(image.getValue())
                    .quoteText(text.getValue())
                    .audioNumber(audioNumber)
                    .imageNumber(imageNumber)
                    .textNumber(textNumber)
                    .build();

            quotes.add(quote);
        }

        return quotes;
    }

    /**
     * Extract number from key like "quote_audio_1" -> 1
     *
     * @param key The key to extract number from
     * @return The extracted number
     */
    private int extractNumberFromKey(String key) {
        try {
            String[] parts = key.split("_");
            return Integer.parseInt(parts[parts.length - 1]);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get default thumbnail URL from configuration
     *
     * @return Default thumbnail URL
     */
    public String getDefaultThumbnailUrl() {
        try {
            DefaultConfig config = defaultConfigRepository.findByKey("default_thumbnail_url")
                    .orElseThrow(() -> new RuntimeException("Default thumbnail URL configuration not found"));

            return config.getValue();
        } catch (Exception e) {
            // Return a fallback URL if configuration is not found
            log.warn("Default thumbnail URL not found in configuration, using fallback URL", e);
            return "";
        }
    }

    /**
     * Get default story images from configuration
     *
     * @return List of default story image URLs
     */
    public List<String> getDefaultStoryImages() {
        try {
            List<DefaultConfig> storyImageConfigs = defaultConfigRepository.findAll().stream()
                    .filter(config -> config.getKey().startsWith("default_story_image_") && config.isActive())
                    .collect(Collectors.toList());

            if (storyImageConfigs.isEmpty()) {
                log.warn("No default story images found in configuration");
                return List.of();
            }

            // Extract URLs from configurations
            List<String> storyImages = storyImageConfigs.stream()
                    .map(DefaultConfig::getValue)
                    .filter(url -> url != null && !url.trim().isEmpty())
                    .collect(Collectors.toList());

            log.info("Found {} default story images", storyImages.size());
            return storyImages;

        } catch (Exception e) {
            log.error("Failed to get default story images", e);
            return List.of();
        }
    }

    /**
     * Get first story reward coins from configuration
     *
     * @return First story reward coins
     */
    public int getFirstStoryRewardCoins() {
        try {
            DefaultConfig config = defaultConfigRepository.findByKey("first_story_5min_reward_coins")
                    .orElseThrow(() -> new RuntimeException("First story reward coins configuration not found"));

            return Integer.parseInt(config.getValue());
        } catch (Exception e) {
            // Return default value if configuration is not found
            log.warn("First story reward coins not found in configuration, using default value", e);
            return 90;
        }
    }

    /**
     * Get minimum duration in minutes for first story reward from configuration
     *
     * @return Minimum duration in minutes
     */
    public int getFirstStoryMinDurationMinutes() {
        try {
            DefaultConfig config = defaultConfigRepository.findByKey("first_story_min_duration_minutes")
                    .orElseThrow(() -> new RuntimeException("First story min duration configuration not found"));

            return Integer.parseInt(config.getValue());
        } catch (Exception e) {
            // Return default value if configuration is not found
            log.warn("First story min duration not found in configuration, using default value", e);
            return 5;
        }
    }

    /**
     * Test method to verify default story images are loaded correctly
     * This method can be called to check if the default story images are available
     */
    public void testDefaultStoryImages() {
        List<String> storyImages = getDefaultStoryImages();
        log.info("Default story images test - Found {} images: {}", storyImages.size(), storyImages);

        if (storyImages.isEmpty()) {
            log.warn("No default story images found - please check defaultThumbnails.json configuration");
        } else {
            log.info("Default story images are properly configured");
        }
    }
} 