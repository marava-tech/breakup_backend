package com.breakupstories.service;

import com.breakupstories.dto.AppConfigResponse;
import com.breakupstories.dto.DefaultConfigRequest;
import com.breakupstories.dto.DefaultConfigResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.dto.QuoteResponse;
import com.breakupstories.dto.StoryCreationConfigResponse;
import com.breakupstories.dto.StoryCreationEligibilityResponse;
import com.breakupstories.model.DefaultConfig;
import com.breakupstories.model.Story;
import com.breakupstories.repository.DefaultConfigRepository;
import com.breakupstories.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefaultConfigService {
    private final DefaultConfigRepository defaultConfigRepository;
    private final StoryRepository storyRepository;
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
     * Search configs by key containing the search term with pagination (case-insensitive)
     *
     * @param searchTerm The search term to look for in config keys (optional)
     * @param activeOnly Whether to return only active configs (default: false)
     * @param page Page number (default: 0)
     * @param size Page size (default: 10)
     * @return Paged response of matching config responses
     */
    public PagedResponse<DefaultConfigResponse> searchByKeyWithPagination(String searchTerm, boolean activeOnly, int page, int size) {
        try {
            Page<DefaultConfig> configPage;
            Pageable pageable = PageRequest.of(page, size);
            
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                configPage = defaultConfigRepository.findByActive(activeOnly,pageable);
            } else {
                configPage = defaultConfigRepository.findByKeyContainingIgnoreCaseAndActive(searchTerm, activeOnly,pageable);
            }
            
            List<DefaultConfigResponse> configs = configPage.getContent().stream()
                    .map(DefaultConfigResponse::fromEntity)
                    .collect(Collectors.toList());
            
            return PagedResponse.of(configs, page, size, configPage.getTotalElements());
                    
        } catch (Exception e) {
            log.error("Error searching configs by key with pagination: {}", searchTerm, e);
            return PagedResponse.of(List.of(), page, size, 0);
        }
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

    /**
     * Get all configuration settings with a specific prefix
     * This method uses a direct database query to fetch all configuration keys that start with the specified prefix 
     * and returns them as key-value pairs
     *
     * @param configPrefix The prefix to search for in configuration keys
     * @return AppConfigResponse containing all configuration settings with the specified prefix
     */
    public AppConfigResponse getConfigurationsByPrefix(String configPrefix) {
        try {
            // Validate prefix parameter
            if (configPrefix == null || configPrefix.trim().isEmpty()) {
                log.warn("Config prefix is null or empty, returning empty response");
                return AppConfigResponse.builder()
                        .configs(Map.of())
                        .totalConfigs(0)
                        .message("Config prefix is required")
                        .build();
            }

            // Direct database query to fetch configurations with the specified prefix
            List<DefaultConfig> configs = defaultConfigRepository.findByKeyStartingWithAndActiveTrue(configPrefix.trim());

            Map<String, String> configMap = configs.stream()
                    .collect(Collectors.toMap(
                            DefaultConfig::getKey,
                            DefaultConfig::getValue,
                            (existing, replacement) -> existing // Keep existing value if duplicate key
                    ));

            log.info("Found {} configuration settings with prefix '{}' using direct DB query", configMap.size(), configPrefix);
            
            return AppConfigResponse.builder()
                    .configs(configMap)
                    .totalConfigs(configMap.size())
                    .message("Configurations retrieved successfully for prefix: " + configPrefix)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get configurations with prefix: {}", configPrefix, e);
            return AppConfigResponse.builder()
                    .configs(Map.of())
                    .totalConfigs(0)
                    .message("Failed to retrieve configurations for prefix: " + configPrefix)
                    .build();
        }
    }
    
    /**
     * Get story creation configuration settings with eligibility information
     * Fetches all configuration keys with prefix "app_config_" related to story creation
     * and parses numeric values appropriately. Also includes user eligibility information.
     *
     * @param userId The user ID to check eligibility for (optional, can be null)
     * @return StoryCreationConfigResponse containing parsed story creation configuration settings and eligibility
     */
    public StoryCreationConfigResponse getStoryCreationConfig(String userId) {
        try {
            // Get all app_config_ prefixed configurations
            List<DefaultConfig> appConfigs = defaultConfigRepository.findByKeyStartingWithAndActiveTrue("app_config_");
            
            Map<String, Object> configMap = new HashMap<>();
            
            for (DefaultConfig config : appConfigs) {
                String key = config.getKey();
                String value = config.getValue();
                
                // Parse specific configuration values
                Object parsedValue = parseConfigValue(key, value);
                configMap.put(key, parsedValue);
            }
            
            // Add eligibility information if userId is provided
            if (userId != null && !userId.trim().isEmpty()) {
                Map<String, Object> eligibilityInfo = getEligibilityInfo(userId);
                configMap.putAll(eligibilityInfo);
            }
            
            log.info("Found {} story creation configuration settings for user: {}", configMap.size(), userId);
            
            return StoryCreationConfigResponse.builder()
                    .configs(configMap)
                    .totalConfigs(configMap.size())
                    .message("Story creation configuration retrieved successfully")
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to get story creation configuration for user: {}", userId, e);
            return StoryCreationConfigResponse.builder()
                    .configs(Map.of())
                    .totalConfigs(0)
                    .message("Failed to retrieve story creation configuration")
                    .build();
        }
    }
    
    /**
     * Get eligibility information for a user
     * 
     * @param userId The user ID to check eligibility for
     * @return Map containing eligibility information
     */
    private Map<String, Object> getEligibilityInfo(String userId) {
        Map<String, Object> eligibilityInfo = new HashMap<>();
        
        try {
            // Get daily limit from configuration
            DefaultConfig dailyLimitConfig = defaultConfigRepository.findByKey("app_config_user_story_creation_limit_per_day")
                    .orElse(null);
            
            int dailyLimit = 1; // Default limit if config not found
            if (dailyLimitConfig != null && dailyLimitConfig.isActive()) {
                try {
                    dailyLimit = Integer.parseInt(dailyLimitConfig.getValue());
                } catch (NumberFormatException e) {
                    log.warn("Invalid daily limit value in config: {}. Using default value: 1", dailyLimitConfig.getValue());
                }
            } else {
                log.warn("Daily limit configuration not found or inactive. Using default value: 1");
            }
            
            // Calculate 24 hours ago
            LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
            
            // Count user stories in last 24 hours (excluding FAILED and REJECTED)
            long storyCount = storyRepository.countByUserIdAndStatusNotInAndCreatedAtAfter(userId, twentyFourHoursAgo);
            
            // Check eligibility
            boolean isEligible = storyCount < dailyLimit;
            int remainingStories = Math.max(0, dailyLimit - (int) storyCount);
            
            // Calculate next eligibility time if user has reached the limit
            LocalDateTime nextEligibilityTime = null;
            if (!isEligible && storyCount > 0) {
                // Find the latest non-failed/non-rejected story to calculate when 24 hours will pass
                List<Story> latestStories = storyRepository.findLatestNonFailedStoryByUserId(userId);
                if (!latestStories.isEmpty()) {
                    Story latestStory = latestStories.get(0);
                    nextEligibilityTime = latestStory.getCreatedAt().plusHours(24);
                }
            }
            
            // Add eligibility information to config map
            eligibilityInfo.put("user_story_creation_enabled", isEligible);
            eligibilityInfo.put("user_daily_story_limit", dailyLimit);
            eligibilityInfo.put("user_current_story_count", (int) storyCount);
            eligibilityInfo.put("user_remaining_stories", remainingStories);
            eligibilityInfo.put("user_next_eligibility_time", nextEligibilityTime);
            
            log.info("Eligibility info for user {}: enabled={}, limit={}, count={}, remaining={}, nextEligibility={}", 
                    userId, isEligible, dailyLimit, storyCount, remainingStories, nextEligibilityTime);
                    
        } catch (Exception e) {
            log.error("Error getting eligibility info for user {}: {}", userId, e.getMessage(), e);
            // Add default eligibility info on error
            eligibilityInfo.put("user_story_creation_enabled", false);
            eligibilityInfo.put("user_daily_story_limit", 1);
            eligibilityInfo.put("user_current_story_count", 0);
            eligibilityInfo.put("user_remaining_stories", 0);
            eligibilityInfo.put("user_next_eligibility_time", null);
        }
        
        return eligibilityInfo;
    }
    
    /**
     * Parse configuration value based on key type
     * 
     * @param key The configuration key
     * @param value The raw configuration value
     * @return Parsed value (Integer, Boolean, or String)
     */
    private Object parseConfigValue(String key, String value) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }
        
        try {
            // Parse numeric values
            if (key.contains("_minutes") || key.contains("_seconds") || 
                key.contains("_characters") || key.contains("_limit") || 
                key.contains("_count") || key.contains("_size")) {
                return Integer.parseInt(value.trim());
            }
            
            // Parse boolean values
            if (key.contains("_enabled") || key.contains("_disabled") || 
                key.contains("_allowed") || key.contains("_required")) {
                return Boolean.parseBoolean(value.trim());
            }
            
            // Return as string for other values
            return value.trim();
            
        } catch (NumberFormatException e) {
            log.warn("Failed to parse numeric value for key '{}': {}. Using as string.", key, value);
            return value.trim();
        }
    }
} 