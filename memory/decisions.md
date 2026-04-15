# Architecture Decisions — Breakup Stories Backend

## 2026-04-01 — MongoDB as primary DB
**Decision:** MongoDB for all video/interaction/user data.
**Why:** Document model fits the flexible short-video content structure with varying metadata per language/genre.
**Outcome:** Working well. Required careful index management (see learnings.md).

## 2026-04-01 — Atomic operations for like/unlike
**Decision:** Use MongoDB upsert (`findAndModify` with `@Upsert`) for like/unlike operations.
**Why:** Race condition with non-atomic check-then-save caused duplicate like records under concurrent requests.
**Outcome:** Fixed the race condition. Always use atomic operations for concurrent mutations.

## 2026-04-01 — Cloudinary for media storage
**Decision:** Cloudinary handles all image and audio file storage.
**Why:** Built-in transformations, CDN delivery, and simple SDK. Avoid managing MinIO for media files.
**Cloud name:** dhssmiyoc
