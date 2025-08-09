package com.breakupstories.service;

import com.breakupstories.dto.AppConfigResponse;
import com.breakupstories.dto.DefaultConfigRequest;
import com.breakupstories.dto.DefaultConfigResponse;
import com.breakupstories.dto.DeviceConfigResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.dto.QuoteResponse;
import com.breakupstories.dto.StoryCreationConfigResponse;
import com.breakupstories.dto.UserConfigResponse;
import com.breakupstories.model.DefaultConfig;
import com.breakupstories.model.Withdrawal;
import com.breakupstories.repository.DefaultConfigRepository;
import com.breakupstories.repository.StoryRepository;
import com.breakupstories.repository.UserRepository;
import com.breakupstories.repository.WithdrawalRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
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
    private final WithdrawalRepository withdrawalRepository;
    private final UserRepository userRepository;
    private final BannedDeviceService bannedDeviceService;
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
     * Get default audio URL from configuration
     *
     * @return Default audio URL
     */
    public String getDefaultAudioUrl() {
        try {
            DefaultConfig config = defaultConfigRepository.findByKey("default_audio_url")
                    .orElseThrow(() -> new RuntimeException("Default audio URL configuration not found"));

            return config.getValue();
        } catch (Exception e) {
            // Return a fallback URL if configuration is not found
            log.warn("Default audio URL not found in configuration, using fallback URL", e);
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
            // Get total story limit from configuration
            DefaultConfig totalLimitConfig = defaultConfigRepository.findByKey("user_story_limit")
                    .orElse(null);
            
            int totalLimit = 2; // Default limit if config not found
            if (totalLimitConfig != null && totalLimitConfig.isActive()) {
                try {
                    totalLimit = Integer.parseInt(totalLimitConfig.getValue());
                } catch (NumberFormatException e) {
                    log.warn("Invalid total story limit value in config: {}. Using default value: 2", totalLimitConfig.getValue());
                }
            } else {
                log.warn("Total story limit configuration not found or inactive. Using default value: 2");
            }
            
            // Count all user stories (excluding FAILED and REJECTED)
            long totalStoryCount = storyRepository.countByUserIdAndStatusNotIn(userId);
            
            // Check eligibility
            boolean isEligible = totalStoryCount < totalLimit;
            int remainingStories = Math.max(0, totalLimit - (int) totalStoryCount);
            
            // Add eligibility information to config map
            eligibilityInfo.put("user_story_creation_enabled", isEligible);
            eligibilityInfo.put("user_total_story_limit", totalLimit);
            eligibilityInfo.put("user_current_story_count", (int) totalStoryCount);
            eligibilityInfo.put("user_remaining_stories", remainingStories);
            
            log.info("Eligibility info for user {}: enabled={}, totalLimit={}, totalCount={}, remaining={}", 
                    userId, isEligible, totalLimit, totalStoryCount, remainingStories);
                    
        } catch (Exception e) {
            log.error("Error getting eligibility info for user {}: {}", userId, e.getMessage(), e);
            // Add default eligibility info on error
            eligibilityInfo.put("user_story_creation_enabled", false);
            eligibilityInfo.put("user_total_story_limit", 2);
            eligibilityInfo.put("user_current_story_count", 0);
            eligibilityInfo.put("user_remaining_stories", 0);
        }
        
        return eligibilityInfo;
    }
    
    /**
     * Get withdrawal eligibility information for a user
     * 
     * @param userId The user ID to check withdrawal eligibility for
     * @return Map containing withdrawal eligibility information
     */
    private Map<String, Object> getWithdrawalEligibilityInfo(String userId) {
        Map<String, Object> withdrawalInfo = new HashMap<>();
        
        try {
            // Get coin to rupee conversion rate
            BigDecimal coinToRupeeRate = BigDecimal.valueOf(2); // Default rate
            try {
                String rateString = defaultConfigRepository.findByKey("1_rupee_equals_in_coins")
                        .map(DefaultConfig::getValue)
                        .orElse("2");
                coinToRupeeRate = new BigDecimal(rateString);
            } catch (Exception e) {
                log.warn("Could not get coin conversion rate, using default: 2");
            }
            
            // Check if user has uploaded active story (basic withdrawal requirement)
            boolean hasUploadedActiveStory = storyRepository.existsByUserIdAndStatusAndCreationType(
                userId, 
                com.breakupstories.model.Story.StoryStatus.ACTIVE, 
                com.breakupstories.model.Story.CreationType.UPLOADED
            );
            
            // Predefined withdrawal amounts
            BigDecimal[] amounts = {
                BigDecimal.valueOf(30.00),
                BigDecimal.valueOf(90.00),
                BigDecimal.valueOf(190.00),
                BigDecimal.valueOf(500.00)
            };
            
            // Check eligibility for each amount
            List<Map<String, Object>> withdrawalOptions = new ArrayList<>();
            for (BigDecimal amount : amounts) {
                Integer coins = amount.multiply(coinToRupeeRate).intValue();
                
                // User is eligible if:
                // 1. No existing withdrawal for this amount, OR
                // 2. Existing withdrawal is REJECTED (user can try again)
                boolean hasNonRejectedWithdrawal = withdrawalRepository.existsByUserIdAndMoneyInRsAndStatusNot(
                    userId, 
                    amount.setScale(2), 
                    Withdrawal.WithdrawalStatus.REJECTED
                );
                
                boolean isEligible = hasUploadedActiveStory && !hasNonRejectedWithdrawal;
                
                Map<String, Object> option = new HashMap<>();
                option.put("amount", amount.doubleValue());
                option.put("coins", coins);
                option.put("eligible", isEligible);
                
                withdrawalOptions.add(option);
            }
            
            withdrawalInfo.put("withdrawal_base_eligibility", hasUploadedActiveStory);
            withdrawalInfo.put("withdrawal_options", withdrawalOptions);
            
            log.info("Withdrawal eligibility info for user {}: baseEligible={}, optionsCount={}", 
                    userId, hasUploadedActiveStory, withdrawalOptions.size());
                    
        } catch (Exception e) {
            log.error("Error getting withdrawal eligibility info for user {}: {}", userId, e.getMessage(), e);
            // Add default withdrawal eligibility info on error
            withdrawalInfo.put("withdrawal_base_eligibility", false);
            withdrawalInfo.put("withdrawal_options", List.of());
        }
        
        return withdrawalInfo;
    }
    
    /**
     * Check if a user is eligible to create a story based on total story limit
     * 
     * @param userId The user ID to check eligibility for
     * @return true if user can create a story, false otherwise
     * @throws RuntimeException if user has reached the story limit
     */
    public boolean checkStoryCreationEligibility(String userId) {
        try {
            // Get total story limit from configuration
            DefaultConfig totalLimitConfig = defaultConfigRepository.findByKey("user_story_limit")
                    .orElse(null);
            
            int totalLimit = 2; // Default limit if config not found
            if (totalLimitConfig != null && totalLimitConfig.isActive()) {
                try {
                    totalLimit = Integer.parseInt(totalLimitConfig.getValue());
                } catch (NumberFormatException e) {
                    log.warn("Invalid total story limit value in config: {}. Using default value: 2", totalLimitConfig.getValue());
                }
            }
            
            // Count all user stories (excluding FAILED and REJECTED)
            long totalStoryCount = storyRepository.countByUserIdAndStatusNotIn(userId);
            
            // Check eligibility
            boolean isEligible = totalStoryCount < totalLimit;
            
            if (!isEligible) {
                throw new RuntimeException(String.format("Story creation limit reached. You have created %d out of %d allowed stories.", 
                        totalStoryCount, totalLimit));
            }
            
            log.info("Story creation eligibility check for user {}: eligible={}, count={}/{}", 
                    userId, isEligible, totalStoryCount, totalLimit);
            
            return true;
            
        } catch (RuntimeException e) {
            throw e; // Re-throw limit exceeded exception
        } catch (Exception e) {
            log.error("Error checking story creation eligibility for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to check story creation eligibility: " + e.getMessage(), e);
        }
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
    
    /**
     * Get all app-level configurations
     * Returns configurations that start with "app_config_" and other app-level configs
     * 
     * @return AppConfigResponse containing app-level configuration settings
     */
    public AppConfigResponse getAppConfigs() {
        try {
            // Get all app_config_ prefixed configurations
            List<DefaultConfig> appConfigs = defaultConfigRepository.findByKeyStartingWithAndActiveTrue("app_config_");
            
            // Add other app-level configs (reward configs, referral configs, etc.)
            List<String> otherAppConfigKeys = List.of(
                "first_story_5min_reward_coins",
                "first_story_min_duration_minutes", 
                "default_1000_views_points",
                "default_100_likes_points",
                "5_feedbacks_points",
                "default_story_active_points",
                "max_referrals_per_user",
                "default_referral_welcome_points",
                "default_referral_reward_points",
                "1_rupee_equals_in_coins",
                "user_story_limit",
                "pause_withdrawls",
                "pause_withdrawls_reason"
            );
            
            List<DefaultConfig> otherConfigs = defaultConfigRepository.findByKeyInAndActiveTrue(otherAppConfigKeys);
            
            Map<String, String> configMap = new HashMap<>();
            
            // Add app_config_ prefixed configurations
            for (DefaultConfig config : appConfigs) {
                configMap.put(config.getKey(), config.getValue());
            }
            
            // Add other app-level configurations
            for (DefaultConfig config : otherConfigs) {
                configMap.put(config.getKey(), config.getValue());
            }
            
            log.info("Found {} app-level configuration settings", configMap.size());
            
            return AppConfigResponse.builder()
                    .configs(configMap)
                    .totalConfigs(configMap.size())
                    .message("App configuration retrieved successfully")
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to get app configuration: {}", e.getMessage(), e);
            return AppConfigResponse.builder()
                    .configs(Map.of())
                    .totalConfigs(0)
                    .message("Failed to retrieve app configuration")
                    .build();
        }
    }
    
    /**
     * Get user-specific configurations and eligibility information
     * Returns user-specific data like story creation eligibility, withdrawal eligibility, etc.
     * 
     * @param userId The user ID to get configurations for
     * @return UserConfigResponse containing user-specific configuration and eligibility data
     */
    public UserConfigResponse getUserConfigs(String userId) {
        try {
            Map<String, Object> configMap = new HashMap<>();
            
            if (userId != null && !userId.trim().isEmpty()) {
                // Add story creation eligibility information
                Map<String, Object> storyEligibilityInfo = getEligibilityInfo(userId);
                configMap.putAll(storyEligibilityInfo);
                
                // Add withdrawal eligibility information
                Map<String, Object> withdrawalEligibilityInfo = getWithdrawalEligibilityInfo(userId);
                configMap.putAll(withdrawalEligibilityInfo);
                
                // Add other user-specific configs here as needed
                // For example: user preferences, etc.
            }
            
            log.info("Found {} user-specific configuration settings for user: {}", configMap.size(), userId);
            
            return UserConfigResponse.builder()
                    .configs(configMap)
                    .totalConfigs(configMap.size())
                    .message("User configuration retrieved successfully")
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to get user configuration for user {}: {}", userId, e.getMessage(), e);
            return UserConfigResponse.builder()
                    .configs(Map.of())
                    .totalConfigs(0)
                    .message("Failed to retrieve user configuration")
                    .build();
        }
    }
    
    /**
     * Get device-specific configurations including ban status and referral eligibility
     * Returns only device_config_* configurations (no app-level configs)
     * @param deviceId The device ID to get configuration for
     * @return DeviceConfigResponse with device-specific information
     */
    public DeviceConfigResponse getDeviceConfigs(String deviceId) {
        try {
            log.info("Getting device configuration for device: {}", deviceId);
            
            // Get only device-specific configurations
            Map<String, Object> configMap = new HashMap<>();
            
            // Add device-specific configs if any exist
            List<DefaultConfig> deviceConfigs = defaultConfigRepository.findByKeyStartingWithAndActiveTrue("device_config_");
            for (DefaultConfig config : deviceConfigs) {
                String keyWithoutPrefix = config.getKey().replace("device_config_", "");
                configMap.put(keyWithoutPrefix, config.getValue());
            }
            
            // Check if device is banned
            boolean isBanned = false;
            String banReason = null;
            java.time.LocalDateTime bannedAt = null;
            java.util.List<String> bannedEmails = null;
            
            if (deviceId != null && !deviceId.trim().isEmpty()) {
                com.breakupstories.dto.BannedDeviceResponse bannedDeviceResponse = bannedDeviceService.getBannedDevice(deviceId);
                if (bannedDeviceResponse != null) {
                    isBanned = true;
                    banReason = bannedDeviceResponse.getReason();
                    bannedAt = bannedDeviceResponse.getCreatedAt();
                    bannedEmails = bannedDeviceResponse.getEmails();
                }
            }
            
            // Check referral eligibility using the same logic as RewardService
            boolean isEligibleForReferral = false;
            if (deviceId != null && !deviceId.trim().isEmpty()) {
                // Device is eligible if it hasn't been used for referral yet
                // This uses the same logic as RewardService.hasDeviceUsedReferral()
                isEligibleForReferral = !userRepository.existsByDeviceId(deviceId);
            }
            
            log.info("Device {} config: banned={}, eligible_for_referral={}", 
                    deviceId, isBanned, isEligibleForReferral);
            
            return DeviceConfigResponse.success(
                    configMap, 
                    deviceId, 
                    isBanned, 
                    banReason, 
                    bannedAt, 
                    bannedEmails,
                    isEligibleForReferral
            );
            
        } catch (Exception e) {
            log.error("Failed to get device configuration for device {}: {}", deviceId, e.getMessage(), e);
            return DeviceConfigResponse.error("Failed to retrieve device configuration", deviceId);
        }
    }
} 