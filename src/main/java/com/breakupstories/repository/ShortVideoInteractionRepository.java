package com.breakupstories.repository;

import com.breakupstories.model.ShortVideoInteraction;
import com.breakupstories.model.ShortVideoInteraction.InteractionType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShortVideoInteractionRepository extends MongoRepository<ShortVideoInteraction, String> {

    Optional<ShortVideoInteraction> findByUserIdAndVideoIdAndType(String userId, String videoId, InteractionType type);

    List<ShortVideoInteraction> findByUserIdAndTypeAndCreatedAtAfter(String userId, InteractionType type,
            LocalDateTime after);

    // Issue #17: projection — returns only videoId field to avoid loading full documents
    @Query(value = "{'userId': ?0, 'type': ?1}", fields = "{'videoId': 1, '_id': 0}")
    List<ShortVideoInteraction> findVideoIdsByUserIdAndType(String userId, InteractionType type);
}
