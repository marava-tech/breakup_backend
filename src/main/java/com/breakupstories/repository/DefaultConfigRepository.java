package com.breakupstories.repository;

import com.breakupstories.model.DefaultConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DefaultConfigRepository extends MongoRepository<DefaultConfig, String> {
    Optional<DefaultConfig> findByKey(String key);
    boolean existsByKey(String key);
    void deleteByKey(String key);


    // Search methods
    List<DefaultConfig> findByKeyContainingIgnoreCase(String key);
    List<DefaultConfig> findByKeyContainingIgnoreCaseAndActiveTrue(String key);
    
    // Paginated search methods
    Page<DefaultConfig> findByActive(Boolean active,Pageable pageable);
    Page<DefaultConfig> findByKeyContainingIgnoreCaseAndActive(String key, Boolean active,Pageable pageable);
}