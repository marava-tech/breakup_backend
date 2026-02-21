package com.breakupstories.repository;

import com.breakupstories.model.ShortVideo;
import com.breakupstories.model.ShortVideo.VideoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShortVideoRepository extends MongoRepository<ShortVideo, String> {

    @Query("{ 'status' : ?0, '_id' : { $nin : ?1 } }")
    Page<ShortVideo> findByStatusAndIdNotIn(VideoStatus status, List<String> excludedIds, Pageable pageable);

    Page<ShortVideo> findByStatus(VideoStatus status, Pageable pageable);

    @Query("{ 'status' : ?0, 'language' : ?1, '_id' : { $nin : ?2 } }")
    Page<ShortVideo> findByStatusAndLanguageAndIdNotIn(VideoStatus status, String language, List<String> excludedIds,
            Pageable pageable);

    Page<ShortVideo> findByStatusAndLanguage(VideoStatus status, String language, Pageable pageable);

    // Issue #18: real counts for accurate pagination totals
    long countByStatus(VideoStatus status);

    long countByStatusAndLanguageIn(VideoStatus status, List<String> languages);
}
