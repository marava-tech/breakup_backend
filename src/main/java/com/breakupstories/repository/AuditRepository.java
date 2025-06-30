package com.breakupstories.repository;

import com.breakupstories.model.Audit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditRepository extends MongoRepository<Audit, String> {
    
    Page<Audit> findByUserId(String userId, Pageable pageable);
    
    Page<Audit> findByEntityType(Audit.EntityType entityType, Pageable pageable);
    
    Page<Audit> findByEntityId(String entityId, Pageable pageable);
    
    Page<Audit> findByActionType(Audit.ActionType actionType, Pageable pageable);
    
    Page<Audit> findByUserIdAndEntityType(String userId, Audit.EntityType entityType, Pageable pageable);
    
    Optional<Audit> findTopByUserIdAndEntityTypeAndActionTypeOrderByCreatedAtDesc(
            String userId, Audit.EntityType entityType, Audit.ActionType actionType);
    
    long countByUserIdAndEntityTypeAndActionType(String userId, Audit.EntityType entityType, Audit.ActionType actionType);
    
    long countByUserIdAndEntityTypeAndActionTypeAndCreatedAtAfter(
            String userId, Audit.EntityType entityType, Audit.ActionType actionType, Long since);
    
    long countByEntityIdAndEntityTypeAndActionType(String entityId, Audit.EntityType entityType, Audit.ActionType actionType);
    
    long countByEntityIdAndEntityTypeAndActionTypeAndCreatedAtAfter(
            String entityId, Audit.EntityType entityType, Audit.ActionType actionType, Long since);
    
    Optional<Audit> findTopByEntityIdOrderByCreatedAtDesc(String entityId);
    
    long countByEntityIdInAndEntityTypeAndActionType(
            List<String> entityIds, Audit.EntityType entityType, Audit.ActionType actionType);
    
    long countByEntityIdInAndEntityTypeAndActionTypeAndCreatedAtAfter(
            List<String> entityIds, Audit.EntityType entityType, Audit.ActionType actionType, Long since);
    
    // New methods for like/unlike logic
    
    /**
     * Find all likes for a specific entity that don't have corresponding unlikes
     * This query finds likes where there's no unlike action by the same user on the same entity after the like
     */
    @Query(value = "{'entityId': ?0, 'entityType': ?1, 'actionType': 'LIKE'}", 
           fields = "{'userId': 1, 'createdAt': 1}")
    List<Audit> findLikesByEntityIdAndEntityType(String entityId, Audit.EntityType entityType);
    
    /**
     * Find all likes for multiple entities that don't have corresponding unlikes
     */
    @Query(value = "{'entityId': {$in: ?0}, 'entityType': ?1, 'actionType': 'LIKE'}", 
           fields = "{'userId': 1, 'entityId': 1, 'createdAt': 1}")
    List<Audit> findLikesByEntityIdsAndEntityType(List<String> entityIds, Audit.EntityType entityType);
    
    /**
     * Find all likes for a specific entity since a given date that don't have corresponding unlikes
     */
    @Query(value = "{'entityId': ?0, 'entityType': ?1, 'actionType': 'LIKE', 'createdAt': {$gte: ?2}}", 
           fields = "{'userId': 1, 'createdAt': 1}")
    List<Audit> findLikesByEntityIdAndEntityTypeAndCreatedAtAfter(
            String entityId, Audit.EntityType entityType, Long since);
    
    /**
     * Find all likes for multiple entities since a given date that don't have corresponding unlikes
     */
    @Query(value = "{'entityId': {$in: ?0}, 'entityType': ?1, 'actionType': 'LIKE', 'createdAt': {$gte: ?2}}", 
           fields = "{'userId': 1, 'entityId': 1, 'createdAt': 1}")
    List<Audit> findLikesByEntityIdsAndEntityTypeAndCreatedAtAfter(
            List<String> entityIds, Audit.EntityType entityType, Long since);
    
    /**
     * Find all unlikes for a specific entity
     */
    @Query(value = "{'entityId': ?0, 'entityType': ?1, 'actionType': 'UNLIKE'}", 
           fields = "{'userId': 1, 'createdAt': 1}")
    List<Audit> findUnlikesByEntityIdAndEntityType(String entityId, Audit.EntityType entityType);
    
    /**
     * Find all unlikes for multiple entities
     */
    @Query(value = "{'entityId': {$in: ?0}, 'entityType': ?1, 'actionType': 'UNLIKE'}", 
           fields = "{'userId': 1, 'entityId': 1, 'createdAt': 1}")
    List<Audit> findUnlikesByEntityIdsAndEntityType(List<String> entityIds, Audit.EntityType entityType);
    
    /**
     * Find story matches for a user since a given date
     */
    @Query(value = "{'userId': ?0, 'entityType': 'STORY', 'actionType': 'MATCH', 'createdAt': {$gte: ?1}}")
    List<Audit> findStoryMatchesByUserIdAndCreatedAtAfter(String userId, Long since);
    
    /**
     * Find audits by userId, entityType, and actionType with pagination
     */
    Page<Audit> findByUserIdAndEntityTypeAndActionType(
            String userId, Audit.EntityType entityType, Audit.ActionType actionType, Pageable pageable);
    
    /**
     * Find all audits for multiple entities with specific entity type and action type
     */
    @Query(value = "{'entityId': {$in: ?0}, 'entityType': ?1, 'actionType': ?2}")
    List<Audit> findByEntityIdInAndEntityTypeAndActionType(
            List<String> entityIds, Audit.EntityType entityType, Audit.ActionType actionType);
    
    /**
     * Find all audits for multiple entities with specific entity type and action type since a given date
     */
    @Query(value = "{'entityId': {$in: ?0}, 'entityType': ?1, 'actionType': ?2, 'createdAt': {$gte: ?3}}")
    List<Audit> findByEntityIdInAndEntityTypeAndActionTypeAndCreatedAtAfter(
            List<String> entityIds, Audit.EntityType entityType, Audit.ActionType actionType, Long since);
    
    /**
     * Find all audits for a specific entity with specific entity type and action type
     */
    @Query(value = "{'entityId': ?0, 'entityType': ?1, 'actionType': ?2}")
    List<Audit> findByEntityIdAndEntityTypeAndActionType(
            String entityId, Audit.EntityType entityType, Audit.ActionType actionType);
    
    /**
     * Find all audits for a specific entity with specific entity type and action type since a given date
     */
    @Query(value = "{'entityId': ?0, 'entityType': ?1, 'actionType': ?2, 'createdAt': {$gte: ?3}}")
    List<Audit> findByEntityIdAndEntityTypeAndActionTypeAndCreatedAtAfter(
            String entityId, Audit.EntityType entityType, Audit.ActionType actionType, Long since);
    
    /**
     * Find audits by userId, entityId, entityType, actionType, and createdAt after a timestamp
     */
    @Query(value = "{'userId': ?0, 'entityId': ?1, 'entityType': ?2, 'actionType': ?3, 'createdAt': {$gte: ?4}}")
    List<Audit> findByUserIdAndEntityIdAndEntityTypeAndActionTypeAndCreatedAtAfter(
            String userId, String entityId, Audit.EntityType entityType, Audit.ActionType actionType, Long since);
    
    /**
     * Find audits by ipAddress, entityId, entityType, actionType, and createdAt after a timestamp
     */
    @Query(value = "{'ipAddress': ?0, 'entityId': ?1, 'entityType': ?2, 'actionType': ?3, 'createdAt': {$gte: ?4}}")
    List<Audit> findByIpAddressAndEntityIdAndEntityTypeAndActionTypeAndCreatedAtAfter(
            String ipAddress, String entityId, Audit.EntityType entityType, Audit.ActionType actionType, Long since);
} 