package com.breakupstories.repository;

import com.breakupstories.model.Emotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmotionRepository extends MongoRepository<Emotion, String> {
    Page<Emotion> findAll(Pageable pageable);
} 