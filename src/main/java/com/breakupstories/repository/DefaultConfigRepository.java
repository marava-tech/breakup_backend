package com.breakupstories.repository;

import com.breakupstories.model.DefaultConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DefaultConfigRepository extends MongoRepository<DefaultConfig, String> {
    Optional<DefaultConfig> findByKey(String key);
    boolean existsByKey(String key);
    void deleteByKey(String key);
}