package com.breakupstories.repository;

import com.breakupstories.model.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookmarkRepository extends MongoRepository<Bookmark, String> {
    
    Page<Bookmark> findByUserId(String userId, Pageable pageable);
    
    Page<Bookmark> findByStoryId(String storyId, Pageable pageable);
    
    Optional<Bookmark> findByStoryIdAndUserId(String storyId, String userId);
    
    boolean existsByStoryIdAndUserId(String storyId, String userId);
    
    boolean existsByUserIdAndStoryId(String userId, String storyId);
    
    void deleteByUserIdAndStoryId(String userId, String storyId);
} 