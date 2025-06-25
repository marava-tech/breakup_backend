package com.breakupstories.repository;

import com.breakupstories.model.Audit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditRepository extends MongoRepository<Audit, String> {
    
    Page<Audit> findByUserId(String userId, Pageable pageable);
    
    Page<Audit> findByEntityType(Audit.EntityType entityType, Pageable pageable);
    
    Page<Audit> findByEntityId(String entityId, Pageable pageable);
} 