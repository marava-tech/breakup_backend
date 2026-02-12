package com.breakupstories.service;

import com.breakupstories.model.Story;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Batches view count increments in Redis and flushes to MongoDB every 60 seconds.
 * Reduces N view events to 1 DB write per story per batch.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ViewCountBatchService {

    private static final String VIEW_COUNT_PREFIX = "viewcount:";

    private final StringRedisTemplate redisTemplate;
    private final MongoTemplate mongoTemplate;

    /**
     * Increment view count in Redis (microsecond op). Safe to call from async path.
     */
    public void incrementInRedis(String storyId) {
        try {
            redisTemplate.opsForValue().increment(VIEW_COUNT_PREFIX + storyId);
        } catch (Exception e) {
            log.warn("Redis view count increment failed for story {}: {}", storyId, e.getMessage());
        }
    }

    @Scheduled(fixedRate = 60_000) // every 60 seconds
    public void flushViewCountsToMongo() {
        try {
            Set<String> keys = redisTemplate.keys(VIEW_COUNT_PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                return;
            }
            for (String key : keys) {
                String storyId = key.replace(VIEW_COUNT_PREFIX, "");
                String val = redisTemplate.opsForValue().getAndDelete(key);
                if (val != null) {
                    try {
                        long count = Long.parseLong(val);
                        if (count > 0) {
                            Query query = Query.query(Criteria.where("id").is(storyId));
                            Update update = new Update().inc("viewCount", count);
                            mongoTemplate.updateFirst(query, update, Story.class);
                            log.debug("Flushed {} views for story {}", count, storyId);
                            // Check milestone reward (async from flush)
                            RewardService rewardService = com.breakupstories.util.ApplicationContextProvider
                                    .getBean(RewardService.class);
                            rewardService.checkViewsMilestoneReward(storyId);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to flush view count for story {}: {}", storyId, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("View count flush failed: {}", e.getMessage());
        }
    }
}
