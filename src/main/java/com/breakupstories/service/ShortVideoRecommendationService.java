package com.breakupstories.service;

import com.breakupstories.dto.PagedResponse;
import com.breakupstories.dto.ShortVideoResponse;
import com.breakupstories.model.ShortVideo;
import com.breakupstories.model.ShortVideoInteraction;
import com.breakupstories.repository.ShortVideoInteractionRepository;
import com.breakupstories.repository.ShortVideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SampleOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import com.breakupstories.util.LanguageUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShortVideoRecommendationService {

    private final ShortVideoInteractionRepository interactionRepository;
    private final ShortVideoRepository shortVideoRepository;
    private final ShortVideoService shortVideoService;
    private final MongoTemplate mongoTemplate;

    public PagedResponse<ShortVideoResponse> getFeed(String userId, String language, int size) {
        List<String> watchedIds = Collections.emptyList();
        if (userId != null) {
            // Issue #17: projection query — fetches only videoId field, not full documents
            watchedIds = interactionRepository.findVideoIdsByUserIdAndType(userId, ShortVideoInteraction.InteractionType.VIEW)
                    .stream()
                    .map(ShortVideoInteraction::getVideoId)
                    .limit(2000)
                    .collect(Collectors.toList());
            log.info("User {} has watched {} videos", userId, watchedIds.size());
        }

        Criteria criteria = Criteria.where("status").is(ShortVideo.VideoStatus.ACTIVE);

        if (!watchedIds.isEmpty()) {
            criteria = criteria.and("_id").nin(watchedIds);
        }

        List<String> languageVariants = null;
        if (language != null && !language.trim().isEmpty()) {
            languageVariants = LanguageUtils.getLanguageVariants(language);
            log.info("Filtering short videos by language variants: {}", languageVariants);
            criteria = criteria.and("language").in(languageVariants);
        }

        log.info("Short video feed criteria: {}", criteria.getCriteriaObject());

        MatchOperation matchStage = Aggregation.match(criteria);
        SampleOperation sampleStage = Aggregation.sample(size);

        Aggregation aggregation = Aggregation.newAggregation(matchStage, sampleStage);

        AggregationResults<ShortVideo> results = mongoTemplate.aggregate(aggregation, ShortVideo.class,
                ShortVideo.class);

        List<ShortVideo> mappedResults = results.getMappedResults();

        // Fallback: if no unseen videos found and we were filtering watched ones, relax the criteria
        if (mappedResults.isEmpty() && !watchedIds.isEmpty()) {
            log.info("No unseen videos found for user {}, falling back to all active videos", userId);
            Criteria fallbackCriteria = Criteria.where("status").is(ShortVideo.VideoStatus.ACTIVE);
            if (languageVariants != null) {
                fallbackCriteria = fallbackCriteria.and("language").in(languageVariants);
            }
            aggregation = Aggregation.newAggregation(Aggregation.match(fallbackCriteria), sampleStage);
            results = mongoTemplate.aggregate(aggregation, ShortVideo.class, ShortVideo.class);
            mappedResults = results.getMappedResults();
        }

        log.info("Short video feed found {} results", mappedResults.size());

        List<ShortVideoResponse> responses = mappedResults.stream()
                .map(v -> shortVideoService.mapToResponse(v, userId))
                .collect(Collectors.toList());

        // Issue #18: use real total count instead of hardcoded 10000
        long total = languageVariants != null
                ? shortVideoRepository.countByStatusAndLanguageIn(ShortVideo.VideoStatus.ACTIVE, languageVariants)
                : shortVideoRepository.countByStatus(ShortVideo.VideoStatus.ACTIVE);

        return PagedResponse.of(responses, 0, size, total);
    }
}
