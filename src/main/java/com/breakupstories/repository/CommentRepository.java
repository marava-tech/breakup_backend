package com.breakupstories.repository;

import com.breakupstories.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {
    
    Page<Comment> findByStoryId(String storyId, Pageable pageable);
    
    List<Comment> findByStoryIdAndParentIdIsNull(String storyId);
    
    List<Comment> findByParentId(String parentId);
    
    long countByStoryId(String storyId);
    
    Page<Comment> findByUserId(String userId, Pageable pageable);
} 