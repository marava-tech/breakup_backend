package com.breakupstories.service;

import com.breakupstories.dto.DefaultConfigRequest;
import com.breakupstories.dto.DefaultConfigResponse;
import com.breakupstories.dto.PagedResponse;
import com.breakupstories.model.DefaultConfig;

import com.breakupstories.repository.DefaultConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefaultConfigService {
    private final DefaultConfigRepository defaultConfigRepository;

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
} 