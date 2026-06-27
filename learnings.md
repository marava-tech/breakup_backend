# Learnings — Breakup Stories Backend

> All lessons extracted from the improvements.md bug fixes.

---

## Database & MongoDB

- **N+1 query in loops**: Calling `userService.getUserEntityById()` inside a `.map()` loop causes one DB call per item. Fix: batch-fetch all IDs first with `getUsersByIds(ids)`, build a lookup map, then map without DB calls. (`ShortVideoInteractionService.java`)
- **N+1 in feed queries**: `getFeed()` fetched 2000 full ShortVideoInteraction documents just to extract videoId strings. Fix: add a projection query `findVideoIdsByUserIdAndType()` with `@Field` to return only `videoId`.
- **Compound indexes don't cover single-field queries**: Only compound index on ShortVideoInteraction existed. Queries on `userId` alone or `videoId` alone did full collection scans. Add `@Indexed` on individual fields even when compound index exists.
- **Missing index on `parentId`**: Nested-comment (replies) queries did full scans on ShortVideoComment. Add `@Indexed` to `parentId` field.
- **Hardcoded total in pagination**: `total = 10000L` was hardcoded in PagedResponse. Use real count query or `-1` sentinel for "unknown total".

## Concurrency & Atomicity

- **Race condition in like/unlike**: Non-atomic check-then-save allows duplicate like records under concurrent requests. Fix: replace check + save with MongoDB upsert (`findAndModify` with `@Upsert`). (`ShortVideoInteractionService.java`)

## Null Safety

- **NPE on `.getGender().toString()`**: User object from `getUserById()` can be null. Always wrap: `Optional.ofNullable(user).map(User::getGender).map(Enum::name).orElse("unknown")`. (`StoryProcessingService.java`)
- **Unsafe `choices.get(0)` without bounds check**: Throws `IndexOutOfBoundsException` if LLM returns empty choices. Always guard: `if (choices == null || choices.isEmpty()) throw ...` (`StoryRewriteService.java`)

## Validation

- **Missing `@Valid` on request bodies**: `ShortVideoRequest` had no validation — null/empty fields were accepted. Add `@Valid` to controller params + `@NotBlank`/`@NotNull` on required DTO fields.
- **Orphan records without entity check**: `recordShare()` saved interaction without verifying the `videoId` exists. Add `videoRepository.existsById(videoId)` guard before saving.
- **No max page-size guard**: `size` request param with no upper limit loads entire collections. Clamp: `Math.min(size, 100)` in service layer or `@Max(100)` on controller param.

## Error Handling

- **Generic `RuntimeException` loses context**: Callers can't distinguish not-found from other errors. Throw `ResourceNotFoundException` (already exists) instead of raw `RuntimeException`.
- **`deleteComment()` swallows not-found**: `ifPresent()` silently succeeds on missing ID. Replace with `orElseThrow(() -> new ResourceNotFoundException("Comment not found"))`.
- **GCS cleanup swallows exceptions**: `cleanupGcs()` logged at WARN without the GCS URI. Log at ERROR and include full URI (`gs://bucket/object`). (`TranscriptionService.java`)
- **Temp file delete failure ignored**: `audioFile.delete()` return value was ignored. Log: `if (!audioFile.delete()) log.warn("Failed to delete temp file: {}", audioFile.getPath())`.

## Resource Management

- **`HttpURLConnection` never closed**: No `disconnect()` call — connections accumulate. Add `connection.disconnect()` in `finally` block. (`TranscriptionService.java`)
- **`audioChunks` list not cleared on TTS exception**: Memory held for all processed chunks if a later chunk fails. Add `audioChunks.clear()` in `catch` block before rethrowing.

## Configuration

- **Hardcoded Cloudinary credentials as `@Value` defaults**: `@Value("${prop:realSecretHere}")` leaks secrets in plaintext if env var missing. Remove all defaults from `@Value` for secrets. Add `@PostConstruct` check that throws on null/empty. (`CloudinaryConfig.java`)

## Performance

- **Regex recompiled per iteration**: `replaceAll("(?i)" + word, ...)` inside a loop recompiles the pattern each time. Pre-build a `Map<String, Pattern>` of compiled patterns once; reuse in loop. (`TTSService.java`)
- **O(n) MP3 header scan over full audio**: Byte-by-byte search through megabytes of audio data. Limit scan to first 256 bytes — MP3 frames always start at the beginning.

## Type Safety

- **Language stored as free-form String**: Allowed invalid codes like `"xyz"`. Replace with `enum VideoLanguage { te, ta, hi, kn, ml, en }` and update repositories.

## Logging & Observability

- **No logging for auth failures**: Unauthorized access in `requireUserId()` threw silently with no audit trail. Add `log.warn("Unauthorized access attempt - no userId in auth token")` before throwing.
- **Confidence threshold too low (0.2)**: Accepted very poor transcriptions. Raise to `0.5`; make it a configurable property.

## Auth Patterns

- **Inconsistent `@PreAuthorize`**: `DefaultConfigController` used `hasAuthority('ROLE_ADMIN')` while others used `hasRole('ADMIN')`. Standardize to `@PreAuthorize("hasRole('ADMIN')")` across all admin controllers.
