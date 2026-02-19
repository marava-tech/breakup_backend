package com.breakupstories.config;

import com.breakupstories.model.DefaultConfig;
import com.breakupstories.repository.DefaultConfigRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializationConfig implements CommandLineRunner {
    private final DefaultConfigRepository defaultConfigRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void run(String... args) throws Exception {
        initializeDefaultConfigsFromResources();
    }

    private void initializeDefaultConfigsFromResources() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:startup/*.json");
            log.info("Found {} JSON files in startup directory", resources.length);

            for (Resource resource : resources) {
                log.info("Processing startup file: {}", resource.getFilename());
                List<DefaultConfig> configs = parseJsonResource(resource);
                log.info("Parsed {} configs from {}", configs.size(), resource.getFilename());

                for (DefaultConfig config : configs) {
                    java.util.Optional<DefaultConfig> existingOpt = defaultConfigRepository.findByKey(config.getKey());
                    if (existingOpt.isEmpty()) {
                        defaultConfigRepository.save(config);
                        log.info("Inserted default config from {}: {}", resource.getFilename(), config.getKey());
                    } else {
                        DefaultConfig existing = existingOpt.get();
                        if (!existing.getValue().equals(config.getValue())) {
                            existing.setValue(config.getValue());
                            existing.setDescription(config.getDescription());
                            existing.setActive(config.isActive());
                            defaultConfigRepository.save(existing);
                            log.info("Updated default config from {}: {}", resource.getFilename(), config.getKey());
                        } else {
                            log.debug("Config already exists and is up to date: {}", config.getKey());
                        }
                    }
                }
            }
            log.info("Default config initialization completed successfully");
        } catch (Exception e) {
            log.error("Error initializing default configs from resources", e);
        }
    }

    private List<DefaultConfig> parseJsonResource(Resource resource) {
        try (InputStream is = resource.getInputStream()) {
            // Try to parse as array
            try {
                return objectMapper.readValue(is, new TypeReference<List<DefaultConfig>>() {
                });
            } catch (Exception e) {
                // If not an array, try as single object
                try (InputStream is2 = resource.getInputStream()) {
                    DefaultConfig config = objectMapper.readValue(is2, DefaultConfig.class);
                    return Collections.singletonList(config);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse {} as DefaultConfig", resource.getFilename(), e);
            return new ArrayList<>();
        }
    }
}