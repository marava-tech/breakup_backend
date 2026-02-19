package com.breakupstories.repository;

import com.breakupstories.model.CommentSupport;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentSupportRepository extends MongoRepository<CommentSupport, String> {
    long countByCommentId(String commentId);

    boolean existsByUserIdAndCommentId(String userId, String commentId);

    Optional<CommentSupport> findByUserIdAndCommentId(String userId, String commentId);

    void deleteByUserIdAndCommentId(String userId, String commentId);
}
