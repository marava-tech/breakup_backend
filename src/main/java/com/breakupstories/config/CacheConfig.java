package com.breakupstories.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        // Static global configs — long TTL, evict only on admin update
        cacheConfigs.put("app-config",    defaultConfig.entryTtl(Duration.ofMinutes(60)));
        cacheConfigs.put("user-config",   defaultConfig.entryTtl(Duration.ofMinutes(60)));
        cacheConfigs.put("device-config", defaultConfig.entryTtl(Duration.ofMinutes(60)));
        cacheConfigs.put("languages",     defaultConfig.entryTtl(Duration.ofHours(6)));

        // Story data
        cacheConfigs.put("story",         defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("stories-feed",   defaultConfig.entryTtl(Duration.ofMinutes(3)));
        cacheConfigs.put("stories-type",  defaultConfig.entryTtl(Duration.ofMinutes(3)));
        cacheConfigs.put("stories-mine",  defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // Comments per story page
        cacheConfigs.put("comments",      defaultConfig.entryTtl(Duration.ofMinutes(3)));

        // User entity (email → User, userId → User) — used on every authenticated request
        cacheConfigs.put("user-entity",   defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // Coin balance per user
        cacheConfigs.put("coin-balance",  defaultConfig.entryTtl(Duration.ofMinutes(2)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("Cache GET failed [cache={}, key={}]: {} — falling back to DB",
                        cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("Cache PUT failed [cache={}, key={}]: {} — continuing without cache",
                        cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                log.warn("Cache EVICT failed [cache={}, key={}]: {}",
                        cache.getName(), key, e.getMessage());
            }
        };
    }
}
