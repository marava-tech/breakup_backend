package com.breakupstories.service;

import com.breakupstories.model.Story;
import com.breakupstories.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Precomputes trending story IDs to avoid full collection scan on every request.
 * Refreshes every 15 minutes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrendingCacheService {

    private static final String TRENDING_IDS_KEY = "trending:ids";
    private static final int TOP_N = 200;
    private static final Duration TTL = Duration.ofMinutes(20);

    private final StoryRepository storyRepository;
    private final LikeService likeService;
    private final CommentService commentService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Scheduled(fixedRate = 900_000) // 15 minutes
    @CacheEvict(value = "stories-type", allEntries = true)
    public void refreshTrendingCache() {
        try {
            List<Story> allActive = storyRepository.findByStatus(Story.StoryStatus.ACTIVE);
            List<String> sortedIds = allActive.stream()
                    .sorted(Comparator.comparingDouble(s -> -trendingScore(s)))
                    .limit(TOP_N)
                    .map(Story::getId)
                    .collect(Collectors.toList());
            redisTemplate.opsForValue().set(TRENDING_IDS_KEY, sortedIds, TTL);
            log.info("Refreshed trending cache: {} story IDs", sortedIds.size());
        } catch (Exception e) {
            log.warn("Failed to refresh trending cache: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> getTrendingStoryIds() {
        try {
            Object cached = redisTemplate.opsForValue().get(TRENDING_IDS_KEY);
            if (cached instanceof List) {
                return (List<String>) cached;
            }
        } catch (Exception e) {
            log.warn("Cache read failed for trending IDs: {}", e.getMessage());
        }
        return null;
    }

    private double trendingScore(Story s) {
        long likeCount = likeService.getLikeCount(s.getId());
        long viewCount = s.getViewCount() != null ? s.getViewCount() : 0L;
        long commentCount = commentService.getCommentCount(s.getId());
        return likeCount * 1.0 + viewCount * 0.4 + commentCount * 0.6;
    }
}
