package com.breakupstories.service;

import com.breakupstories.model.Story;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Batches view count increments in Redis and flushes to MongoDB every 60
 * seconds.
 *
 * <p>
 * Write path (called on every story open, async):
 * <ol>
 * <li>Self-view? → drop immediately.
 * <li>SETNX view_dedup:{storyId}:u:{userId} EX 86400 — if already set → drop
 * (duplicate
 * within 24 h window).
 * <li>INCR viewcount:{storyId} — atomic, microsecond op, no DB write.
 * </ol>
 *
 * <p>
 * Flush path (every 60 s, @Scheduled):
 * <ol>
 * <li>SCAN viewcount:* — non-blocking cursor, safe for large keyspaces.
 * <li>GETDEL each key → batch-increment Story.viewCount in MongoDB (1 write per
 * story per
 * flush window regardless of traffic volume).
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ViewCountBatchService {

    private static final String VIEW_COUNT_PREFIX = "viewcount:";
    private static final String VIEW_DEDUP_PREFIX = "view_dedup:";

    @org.springframework.beans.factory.annotation.Value("${app.story.view-dedup-ttl-seconds:1800}")
    private long viewDedupTtlSeconds;

    private final StringRedisTemplate redisTemplate;
    private final MongoTemplate mongoTemplate;

    /**
     * Records a unique view for {@code storyId}.
     *
     * <ul>
     * <li>Self-views are always excluded.
     * <li>Authenticated users: deduplicated by userId (24 h window).
     * <li>Anonymous users: deduplicated by IP address (24 h window).
     * <li>Truly anonymous (no userId, no IP): counted without dedup to avoid
     * under-counting.
     * </ul>
     *
     * @return {@code true} if the view was counted, {@code false} if skipped
     */
    public boolean recordView(String storyId, String viewerId, String ipAddress, boolean isOwnStory) {
        if (isOwnStory) {
            log.debug("View skipped — self-view for story {}", storyId);
            return false;
        }

        String dedupKey = buildDedupKey(storyId, viewerId, ipAddress);

        if (dedupKey == null) {
            // No identity signal — count it to avoid silently under-counting
            incrementInRedis(storyId);
            log.debug("View counted (no dedup identity) for story {}", storyId);
            return true;
        }

        Boolean isNew = redisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", Duration.ofSeconds(viewDedupTtlSeconds));

        if (Boolean.TRUE.equals(isNew)) {
            incrementInRedis(storyId);
            log.debug("View counted for story {} (key={})", storyId, dedupKey);
            return true;
        }

        log.debug("View skipped — duplicate within 24 h window for story {} (key={})", storyId, dedupKey);
        return false;
    }

    // -------------------------------------------------------------------------
    // Scheduled flush
    // -------------------------------------------------------------------------

    /**
     * Flushes batched Redis view counts to MongoDB every 60 seconds.
     * Uses SCAN (not KEYS) so it never blocks the Redis event loop.
     */
    @Scheduled(fixedRate = 60_000)
    public void flushViewCountsToMongo() {
        Map<String, Long> batch = collectAndClearViewCounts();
        if (batch.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Long> entry : batch.entrySet()) {
            String storyId = entry.getKey();
            long count = entry.getValue();
            if (count <= 0)
                continue;

            try {
                Query query = Query.query(Criteria.where("id").is(storyId));
                Update update = new Update().inc("viewCount", count);
                mongoTemplate.updateFirst(query, update, Story.class);
                log.debug("Flushed {} view(s) for story {}", count, storyId);

                RewardService rewardService = com.breakupstories.util.ApplicationContextProvider
                        .getBean(RewardService.class);
                rewardService.checkViewsMilestoneReward(storyId);
            } catch (Exception e) {
                log.warn("Failed to flush view count for story {}: {}", storyId, e.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String buildDedupKey(String storyId, String viewerId, String ipAddress) {
        if (viewerId != null && !viewerId.isBlank()) {
            return VIEW_DEDUP_PREFIX + storyId + ":u:" + viewerId;
        }
        if (ipAddress != null && !ipAddress.isBlank()) {
            return VIEW_DEDUP_PREFIX + storyId + ":ip:" + ipAddress;
        }
        return null;
    }

    private void incrementInRedis(String storyId) {
        try {
            redisTemplate.opsForValue().increment(VIEW_COUNT_PREFIX + storyId);
        } catch (Exception e) {
            log.warn("Redis INCR failed for story {}: {}", storyId, e.getMessage());
        }
    }

    /**
     * Scans all {@code viewcount:*} keys, atomically fetches+deletes each one,
     * and returns a map of storyId → pendingCount.
     * Uses cursor-based SCAN to avoid blocking Redis.
     */
    private Map<String, Long> collectAndClearViewCounts() {
        Map<String, Long> result = new HashMap<>();
        try {
            ScanOptions opts = ScanOptions.scanOptions()
                    .match(VIEW_COUNT_PREFIX + "*")
                    .count(200)
                    .build();

            redisTemplate.execute((RedisConnection connection) -> {
                try (Cursor<byte[]> cursor = connection.scan(opts)) {
                    while (cursor.hasNext()) {
                        String key = new String(cursor.next(), StandardCharsets.UTF_8);
                        String val = redisTemplate.opsForValue().getAndDelete(key);
                        if (val != null) {
                            try {
                                String storyId = key.substring(VIEW_COUNT_PREFIX.length());
                                result.put(storyId, Long.parseLong(val));
                            } catch (NumberFormatException e) {
                                log.warn("Corrupt view count value for key {}: {}", key, val);
                            }
                        }
                    }
                }
                return null;
            });
        } catch (Exception e) {
            log.warn("View count flush scan failed: {}", e.getMessage());
        }
        return result;
    }
}
