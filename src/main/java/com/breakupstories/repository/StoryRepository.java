package com.breakupstories.repository;

import com.breakupstories.model.Story;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoryRepository extends MongoRepository<Story, String> {
    
    Page<Story> findByUserId(String userId, Pageable pageable);
    
    Page<Story> findByStatus(Story.StoryStatus status, Pageable pageable);
    
    List<Story> findByTagsContaining(String tag);
    
    Page<Story> findByTagsContaining(String tag, Pageable pageable);
} 