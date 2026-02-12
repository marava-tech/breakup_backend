# API Performance Improvements

> Stack: Spring Boot 3.2 · Java 17 · MongoDB · No existing cache layer
> Constraint: No DB schema changes. New indexes allowed. Redis allowed.
> Basis: `app_used_apis.md` usage counts — Stories (23 calls), Auth (11), Config (9), Consoling AI (5), Bookmarks (5), Rewards/Withdrawals (6), Health (3).

**Status legend:** ✅ Done | ⬜ Pending

---

## Critical Issues Found (Before Any Fix)

1. **Zero caching** — every request hits MongoDB, including configs that never change
2. **Trending stories = full collection scan on every request** — `getTrendingStories()` calls `findByStatus(ACTIVE)` (loads ALL active stories into memory) then sorts by computed score in-memory
3. **Story by ID writes on every read** — `GET /api/stories/{id}` (called 5×/session) does a synchronous `viewCount` increment + synchronous audit log write before returning
4. **Story feed N+1 queries** — for each story in the page, like count and comment count are fetched individually from the DB
5. **Config APIs hit DB on every app launch** — `app-configs`, `user-configs`, `device-configs` are called 4×, 2×, 1× per session; these configs almost never change
6. **Missing compound indexes** for the most common query patterns
7. **Comment enrichment is recursive** — `buildCommentWithReplies()` issues a separate DB query per comment for replies

---

## Tier 1 — Critical (Highest ROI, implement first)

### 1. Add Redis and Enable Spring Cache ✅

**Why:** Single biggest win. Eliminates repeated DB hits for configs and story data that barely change.

**Add to `pom.xml`:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

**Add to `application.yml`:**
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 10
          max-idle: 5
          min-idle: 2
```

**Create `CacheConfig.java`:**
```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        Map<String, RedisCacheConfiguration> configs = new HashMap<>();

        // Static configs — very long TTL, evict manually on update
        configs.put("app-config",     ttl(Duration.ofMinutes(60)));
        configs.put("user-config",    ttl(Duration.ofMinutes(60)));
        configs.put("device-config",  ttl(Duration.ofMinutes(60)));
        configs.put("languages",      ttl(Duration.ofHours(6)));

        // Stories — short TTL, stale is fine
        configs.put("story",          ttl(Duration.ofMinutes(10)));
        configs.put("stories-feed",   ttl(Duration.ofMinutes(3)));
        configs.put("stories-type",   ttl(Duration.ofMinutes(3)));
        configs.put("stories-mine",   ttl(Duration.ofMinutes(5)));
        configs.put("story-search",   ttl(Duration.ofMinutes(2)));

        // Social
        configs.put("comments",       ttl(Duration.ofMinutes(3)));
        configs.put("bookmarks",      ttl(Duration.ofMinutes(5)));

        // User / Rewards
        configs.put("user-profile",   ttl(Duration.ofMinutes(5)));
        configs.put("coin-balance",   ttl(Duration.ofMinutes(2)));
        configs.put("referral-stats", ttl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(factory)
            .withInitialCacheConfigurations(configs)
            .build();
    }

    private RedisCacheConfiguration ttl(Duration duration) {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(duration)
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
}
```

---

### 2. Cache Config Endpoints (Eliminates ~9 DB calls per app session) ✅

`DefaultConfigService.java` — add `@Cacheable` / `@CacheEvict`:

```java
@Cacheable(value = "app-config", key = "'global'")
public AppConfigResponse getAppConfigs() { ... }

@Cacheable(value = "user-config", key = "'global'")
public AppConfigResponse getUserConfigs() { ... }

@Cacheable(value = "device-config", key = "'global'")
public AppConfigResponse getDeviceConfigs() { ... }

@Cacheable(value = "languages", key = "'all'")
public List<String> getLanguages() { ... }

// On any config write, evict all config caches
@CacheEvict(value = {"app-config","user-config","device-config","languages"}, allEntries = true)
public DefaultConfigResponse createConfig(...) { ... }

@CacheEvict(value = {"app-config","user-config","device-config","languages"}, allEntries = true)
public DefaultConfigResponse updateConfig(...) { ... }
```

**Impact:** 9 DB queries eliminated per app session, across all users.

---

### 3. Cache Story by ID (Eliminates 5 DB reads per session for the hottest endpoint) ✅

`StoryService.java`:

```java
@Cacheable(value = "story", key = "#storyId")
public StoryResponse getStoryById(String storyId) { ... }

@Cacheable(value = "story", key = "#storyId + ':' + #userId")
public StoryResponse getStoryById(String storyId, String userId) { ... }

// Evict on any mutation
@CacheEvict(value = "story", key = "#storyId")
public void likeStory(String storyId, ...) { ... }

@CacheEvict(value = "story", key = "#storyId")
public StoryResponse updateStory(String storyId, ...) { ... }

@CacheEvict(value = "story", key = "#storyId")
public void deleteStory(String storyId) { ... }
```

**Impact:** Most-called endpoint (5×/session) reduced from MongoDB read to Redis lookup in <1ms.

---

### 4. Fix Trending Stories — Full Collection Scan on Every Request ✅

**Current:** `getTrendingStories()` does `findByStatus(ACTIVE)` → loads ALL stories into memory → sorts by score. This is O(n) per request.

**Fix — Scheduled Pre-computation:**

```java
// Add field trendingScore to Story (or compute in a separate scheduled cache)
@Scheduled(fixedRate = 900000) // every 15 min
@CacheEvict(value = "stories-type", allEntries = true)
public void refreshTrendingCache() {
    // Compute scores once, store top-N in Redis
    List<Story> allActive = storyRepository.findByStatus(ACTIVE);
    List<String> sortedIds = allActive.stream()
        .sorted(Comparator.comparingDouble(s ->
            -(s.getLikeCount() * 1.0 + s.getViewCount() * 0.4 + s.getCommentCount() * 0.6)))
        .limit(200)
        .map(Story::getId)
        .collect(toList());
    redisTemplate.opsForValue().set("trending:ids", sortedIds, Duration.ofMinutes(20));
}

// getTrendingStories now reads from Redis, not MongoDB
public PagedResponse<StoryResponse> getTrendingStories(String userId, int page, int size) {
    List<String> ids = (List<String>) redisTemplate.opsForValue().get("trending:ids");
    if (ids == null) { refreshTrendingCache(); /* fallback */ }
    // Slice by page, fetch only those story IDs from DB (or cache)
}
```

**Impact:** Eliminates full collection scan. Trending result served in <5ms vs potentially 500ms+ at scale.

---

### 5. Make View Count Increment Async (Removes Synchronous Write from Read Path) ✅

**Current:** `GET /api/stories/{id}` → return story → **synchronously** `storyService.incrementViewCount(storyId)` — a MongoDB write blocking the response.

**Fix:**

```java
// AsyncConfig.java — add a story operations executor
@Bean(name = "storyOpsExecutor")
public Executor storyOpsExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(5000);
    executor.setThreadNamePrefix("StoryOps-");
    executor.initialize();
    return executor;
}

// StoryService.java
@Async("storyOpsExecutor")
public CompletableFuture<Void> incrementViewCountAsync(String storyId) {
    storyRepository.incrementViewCount(storyId);
    return CompletableFuture.completedFuture(null);
}

// StoryController.java — fire and forget
storyService.incrementViewCountAsync(storyId); // non-blocking
```

**Impact:** Removes a synchronous MongoDB write from the critical path of the most-called endpoint. Response time drops by the write latency (~5-20ms).

---

### 6. Make Audit Logging Async ✅

**Current:** Every story view triggers `auditService.logStoryView(...)` — a synchronous MongoDB insert before returning the response.

**Fix:**

```java
@Async("storyOpsExecutor")
public CompletableFuture<Void> logStoryViewAsync(...) {
    auditRepository.save(audit);
    return CompletableFuture.completedFuture(null);
}
```

**Impact:** Removes 1 more synchronous DB write from the story detail read path.

---

## Tier 2 — High Impact

### 7. Cache Story Feed and Type Feeds ✅

```java
// StoryService.java
@Cacheable(value = "stories-feed", key = "#language + ':' + #page + ':' + #size")
public PagedResponse<StoryResponse> getStoriesByLanguage(String language, ...) { ... }

@Cacheable(value = "stories-feed", key = "'all:' + #page + ':' + #size")
public PagedResponse<StoryResponse> getStories(String userId, int page, int size) { ... }

@Cacheable(value = "stories-type", key = "#searchType + ':' + #language + ':' + #page")
public PagedResponse<StoryResponse> getForYouStoriesByLanguage(String language, ...) { ... }

@Cacheable(value = "stories-mine", key = "#userId + ':' + #page")
public PagedResponse<StoryResponse> getMyStories(String userId, int page, int size) { ... }
```

**Important:** Cache key must NOT include `userId` for global feeds — all users share the same feed cache. Per-user personalization is acceptable via the language filter key.

**Cache eviction:** Evict `stories-feed` and `stories-type` when a new story is published or status changes (in admin/story update).

---

### 8. Cache Coin Balance Per User ✅

`RewardService.java`:
```java
@Cacheable(value = "coin-balance", key = "#userId")
public CoinBalanceResponse getCoinBalance(String userId) { ... }

@CacheEvict(value = "coin-balance", key = "#userId")
public void addCoins(String userId, ...) { ... }

@CacheEvict(value = "coin-balance", key = "#userId")
public void invalidateCoinHistory(String entryId, ...) { ... }
```

---

### 9. Cache Comments Per Story ✅

`CommentService.java`:
```java
@Cacheable(value = "comments", key = "#storyId + ':' + #page")
public PagedResponse<CommentResponse> getCommentsByStory(String storyId, int page, int size) { ... }

@CacheEvict(value = "comments", key = "#storyId + ':*'")  // evict all pages
public CommentResponse addComment(String storyId, ...) { ... }

@CacheEvict(value = "comments", key = "#storyId + ':*'")
public void deleteComment(String commentId, String storyId, ...) { ... }
```

---

### 10. Add Missing Compound MongoDB Indexes ✅

The `mongo-init.js` has single-field indexes but is missing compounds for the actual query patterns. Add these:

```javascript
// Most important: covers getStoriesByLanguage query
db.stories.createIndex(
  { "metadata.language": 1, "status": 1, "createdAt": -1 },
  { name: "idx_stories_lang_status_date" }
);

// Covers getForYouStories (status + viewCount ordered)
db.stories.createIndex(
  { "status": 1, "viewCount": -1 },
  { name: "idx_stories_status_views" }
);

// Covers getLatestStories (status + createdAt ordered)
db.stories.createIndex(
  { "status": 1, "createdAt": -1 },
  { name: "idx_stories_status_date" }
);

// Covers getMyStories
db.stories.createIndex(
  { "userId": 1, "status": 1, "createdAt": -1 },
  { name: "idx_stories_user_status_date" }
);

// Covers paginated comments for a story (most common comment query)
db.comments.createIndex(
  { "storyId": 1, "parentId": 1, "active": 1, "createdAt": -1 },
  { name: "idx_comments_story_parent_active_date" }
);

// Covers coin balance query — findValidCoinHistoryByUserId
db.coinHistory.createIndex(
  { "userId": 1, "isInvalidated": 1 },
  { name: "idx_coinhistory_user_valid" }
);

// Covers withdrawal queries
db.withdrawals.createIndex(
  { "userId": 1, "createdAt": -1 },
  { name: "idx_withdrawals_user_date" }
);
db.withdrawals.createIndex(
  { "status": 1, "createdAt": -1 },
  { name: "idx_withdrawals_status_date" }
);

// Covers notification fetch
db.notifications.createIndex(
  { "userId": 1, "createdAt": -1 },
  { name: "idx_notifications_user_date" }
);

// Covers referral stats — findByReferredBy
db.users.createIndex(
  { "referredBy": 1 },
  { name: "idx_users_referredby" }
);
```

---

### 11. Enable GZIP Compression for Responses ✅

Story content and feed payloads are large JSON. GZIP typically reduces payload by 60–80%.

**`application.yml`:**
```yaml
server:
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/plain
    min-response-size: 1024
```

**Impact:** Reduces mobile data usage and network transfer time. No code changes needed.

---

### 12. Cache User Profile ✅

`UserService.java`:
```java
@Cacheable(value = "user-profile", key = "#userId")
public UserProfileResponse getUserProfile(String userId) { ... }

@Cacheable(value = "user-profile", key = "'email:' + #email")
public User getUserEntityByEmail(String email) { ... }  // Called on EVERY authenticated request

@CacheEvict(value = "user-profile", allEntries = true)
public void updateProfileImage(String userId, ...) { ... }
```

**Note:** `getUserEntityByEmail()` is called on every authenticated API request (JWT authentication resolves email first). Caching this alone eliminates one DB lookup per request across the entire API.

---

## Tier 3 — Medium Impact

### 13. Health Ping Must Not Touch the Database ✅

`GET /api/health/ping` is called 3× per app launch (splash, main, error screens). Verify it does not execute any DB query — it should return `{status: "OK"}` instantly from memory only.

```java
@GetMapping("/ping")
public ResponseEntity<Map<String, String>> ping() {
    return ResponseEntity.ok(Map.of("status", "OK"));
    // NO DB call, NO service call
}
```

---

### 14. Disable auto-index-creation in Production ✅

`auto-index-creation: true` causes Spring to check/create indexes on every application startup — slow and risky in production.

```yaml
spring:
  data:
    mongodb:
      auto-index-creation: false  # manage indexes via mongo-init.js / migration scripts
```

---

### 15. Redis-based View Count Batching (Avoid Per-View DB Write) ✅

Even with async incrementing, a DB write per story view is expensive at scale.

**Pattern:**
```java
// On each view: increment in Redis (in-memory, microsecond op)
redisTemplate.opsForValue().increment("viewcount:" + storyId);

// Scheduled flush every 60 seconds: batch write to MongoDB
@Scheduled(fixedRate = 60000)
public void flushViewCounts() {
    Set<String> keys = redisTemplate.keys("viewcount:*");
    keys.forEach(key -> {
        String storyId = key.replace("viewcount:", "");
        Long count = redisTemplate.opsForValue().getAndDelete(key);
        if (count != null && count > 0) {
            storyRepository.incrementViewCount(storyId, count.intValue());
        }
    });
}
```

**Impact:** N view events → 1 DB write per 60s batch instead of N writes.

---

### 16. Cache User Email → User Entity (eliminates 1 DB hit per every authenticated API request) ✅

Every protected endpoint resolves `authentication.getName()` (email) → `userService.getUserEntityByEmail(email)`. This is a DB query on **every single API call**.

```java
@Cacheable(value = "user-profile", key = "'email:' + #email")
public User getUserEntityByEmail(String email) {
    return userRepository.findByEmail(email)
        .orElseThrow(...);
}
```

TTL: 10 minutes. Evict on profile update/deletion.

**Impact:** Removes 1 MongoDB query from the overhead of every authenticated request — Auth, Stories, Bookmarks, Comments, Rewards, Withdrawals, etc.

---

### 17. Story Feed — User Lookup Before Query (Unnecessary DB Hit) ✅

`GET /api/stories` does: `getUserEntityByEmail(email)` → reads `preferredStoryLanguage` from the User → then queries stories.

If User entity is cached (point 16), this becomes a Redis hit instead of DB hit. No code change needed beyond caching the user.

---

### 18. Replace Offset Pagination with Cursor-based for Story Feed ✅

Current offset pagination (`PageRequest.of(page, size)`) requires MongoDB to skip `page * size` documents — gets slower as page number increases.

**Fix for story feed:** Use `createdAt` cursor instead of page offset:
```java
// Instead of: findByStatusOrderByCreatedAtDesc(ACTIVE, PageRequest.of(page, size))
// Use: findByStatusAndCreatedAtBeforeOrderByCreatedAtDesc(ACTIVE, cursor, limit)
```

The app can pass the `createdAt` of the last item as a cursor for the next page.

---

## Tier 4 — Architecture

### 19. Async Notification Creation After Like/Comment ✅

When a user likes or comments on a story, a notification is likely created for the story owner. This should always be async (fire and forget).

```java
@Async("storyOpsExecutor")
public CompletableFuture<Void> createNotificationAsync(Notification notification) {
    notificationRepository.save(notification);
    return CompletableFuture.completedFuture(null);
}
```

---

### 20. Upgrade to Java 21 — Virtual Threads ✅

Spring Boot 3.2 supports Project Loom virtual threads. For I/O-bound workloads (MongoDB, HTTP calls to the AI consoling service), this dramatically increases throughput with zero code changes.

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

One-line config change. Enables virtual threads for the entire Tomcat thread pool. Particularly beneficial for the `GET /api/story/{id}/consoling-message` endpoint which calls an external HTTP service.

---

### 21. MongoDB Read Preference — Secondary for Heavy Read Queries ✅

If MongoDB is running as a replica set (common in production), route read-heavy, slightly-stale-tolerant queries to secondary nodes:

```java
// MongoConfig.java
@Bean
public MongoClient mongoClient() {
    MongoClientSettings settings = MongoClientSettings.builder()
        .readPreference(ReadPreference.secondaryPreferred())  // reads go to secondary
        .build();
}
```

Offloads read traffic from the primary, which handles all writes.

---

### 22. Set Redis TTL Fallback — Graceful Degradation ✅

Always code for Redis unavailability. If Redis is down, fall through to the DB:

```java
@Bean
public CacheErrorHandler errorHandler() {
    return new SimpleCacheErrorHandler() {
        @Override
        public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
            log.warn("Cache get failed for key {}: {}", key, e.getMessage());
            // Spring will proceed to execute the method (DB fallback)
        }
    };
}
```

---

## Implementation Priority Order

| # | Change | Effort | Impact | Type | Status |
|---|--------|--------|--------|------|--------|
| 1 | Add Redis + CacheConfig | Medium | Critical | Infrastructure | ✅ |
| 2 | Cache config endpoints | Low | Critical | Code | ✅ |
| 3 | Cache story by ID | Low | Critical | Code | ✅ |
| 4 | Fix trending stories (precompute) | Medium | Critical | Code | ✅ |
| 5 | Async view count + audit log | Low | High | Code | ✅ |
| 6 | Cache user email→entity | Low | High | Code | ✅ |
| 7 | Add compound MongoDB indexes | Low | High | Config | ✅ |
| 8 | Cache story feeds | Low | High | Code | ✅ |
| 9 | Cache coin balance | Low | High | Code | ✅ |
| 10 | Cache comments | Low | Medium | Code | ✅ |
| 11 | Enable GZIP compression | Trivial | High | Config | ✅ |
| 12 | Disable auto-index-creation | Trivial | Medium | Config | ✅ |
| 13 | Health ping — no DB | Trivial | Low | Code | ✅ |
| 14 | Redis view count batching | Medium | Medium | Code | ✅ |
| 15 | Virtual threads (Java 21) | Medium | High | Upgrade | ✅ |
| 16 | Cursor-based pagination | Medium | Medium | Code | ✅ |
| 17 | Async notifications | Low | Medium | Code | ✅ |
| 18 | MongoDB read preference secondary | Low | Medium | Config | ✅ |
| 22 | Redis TTL fallback (CacheErrorHandler) | Trivial | Medium | Config | ✅ |

---

## Expected Outcome After Tier 1 + Tier 2

| Metric | Before | After |
|--------|--------|-------|
| DB queries per app session | ~35 | ~8 (fresh) / ~3 (warm cache) |
| Story detail response time | ~50–100ms | ~1–5ms (cache hit) |
| Config API response time | ~30–60ms | <1ms (cache hit) |
| Trending stories response time | ~200–1000ms (full scan) | <5ms (precomputed) |
| Story feed response time | ~80–150ms | ~2–8ms (cache hit) |
| Coin balance response time | ~30–50ms | <1ms (cache hit) |
