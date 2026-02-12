# APIs Cleanup — COMPLETED

> All unused endpoints listed below have been **removed** from the backend codebase.
> Build verified — compiles successfully after cleanup.

---

## Summary

| Category | Count | Status |
|----------|------:|--------|
| Endpoints removed | 50 | DONE |
| Entire controllers deleted | 3 | DONE |
| Phantom endpoints (client bug) | 2 | Needs dashboard fix |
| Test-only endpoints removed | 1 | DONE |

---

## What Was Removed

### Entire Controllers Deleted
- `UploadController.java` — sole endpoint `POST /api/upload/file` was unused
- `AudioController.java` — sole endpoint `GET /api/audio/quotes` was unused
- `StorySearchController.java` — all 4 endpoints unused (duplicate of StoryController search)

### AuthController
- `GET /api/auth/device-referral-status/{deviceId}` — removed

### StoryController (4 endpoints removed)
- `GET /api/stories/liked`
- `GET /api/stories/{storyId}/like-count`
- `GET /api/stories/test-story-images/{storyId}` (test-only)
- `GET /api/stories/withdrawal-eligibility`

### CommentController (11 endpoints removed)
- `GET /api/comments/story/{storyId}/all`
- `GET /api/comments/story/{storyId}/count`
- `GET /api/comments/{commentId}`
- `GET /api/comments/{commentId}/replies`
- `PUT /api/comments/{commentId}`
- `GET /api/comments/user/{userId}`
- `GET /api/comments/abusive`
- `GET /api/comments/abusive/category/{category}`
- `GET /api/comments/abusive/statistics`
- `POST /api/comments/{commentId}/flag`
- `POST /api/comments/{commentId}/unflag`

### BookmarkController (5 endpoints removed)
- `GET /api/bookmarks`
- `GET /api/bookmarks/check/{storyId}`
- `GET /api/bookmarks/count`
- `DELETE /api/bookmarks/{bookmarkId}`
- `GET /api/bookmarks/{bookmarkId}`

### UserController (5 endpoints removed)
- `GET /api/users`
- `GET /api/users/{userId}`
- `GET /api/users/email/{email}`
- `PUT /api/users/{userId}`
- `DELETE /api/users/{userId}`

### FeedbackController (7 endpoints removed)
- `GET /api/feedbacks/types/{type}`
- `GET /api/feedbacks/status/{status}`
- `GET /api/feedbacks/story/{storyId}`
- `GET /api/feedbacks/user/{userId}`
- `GET /api/feedbacks/{feedbackId}`
- `PUT /api/feedbacks/{feedbackId}`
- `DELETE /api/feedbacks/{feedbackId}`

### RewardController (2 endpoints removed)
- `GET /api/rewards/referral-stats/{userId}`
- `GET /api/rewards/configurations`

### WithdrawalController (1 endpoint removed)
- `PUT /api/withdrawals/{withdrawalId}/status-json`

### AdminController (11 endpoints removed)
- `GET /api/admin/stories/statistics`
- `GET /api/admin/users/statistics`
- `GET /api/admin/users/device/{deviceId}`
- `GET /api/admin/comments`
- `DELETE /api/admin/comments/{commentId}`
- `GET /api/admin/comments/statistics`
- `GET /api/admin/withdrawals/statistics`
- `GET /api/admin/feedback/statistics`
- `GET /api/admin/feedback`
- `PUT /api/admin/feedback/{feedbackId}/status`
- `DELETE /api/admin/feedback/{feedbackId}`

### BannedDeviceController (5 endpoints removed)
- `GET /api/admin/banned-devices/{deviceId}`
- `GET /api/admin/banned-devices`
- `GET /api/admin/banned-devices/search/device`
- `GET /api/admin/banned-devices/search/email`
- `GET /api/admin/banned-devices/search/reason`

### AuditController (9 endpoints removed)
- `POST /api/audits`
- `GET /api/audits/entity-type/{entityType}`
- `GET /api/audits/entity/{entityId}`
- `GET /api/audits/action-type/{actionType}`
- `GET /api/audits/user/{userId}/entity-type/{entityType}`
- `GET /api/audits/analytics/story-views`
- `GET /api/audits/analytics/user-activity`
- `GET /api/audits/{auditId}`
- `DELETE /api/audits/{auditId}`

### DefaultConfigController (4 endpoints removed)
- `GET /api/configs/{id}`
- `GET /api/configs/by-prefix`
- `GET /api/configs/story-creation-config`
- `GET /api/configs/verify-startup-data`

---

## Still Pending — Phantom Endpoints (Dashboard Bug)

These are called by the **Dashboard** but have **no matching controller** in the backend. They will always return 404.

| Method | Endpoint | Dashboard Section | What It Should Probably Map To |
|--------|----------|-------------------|--------------------------------|
| `GET` | `/api/feedback` | Feedback (App Feedback) | `/api/feedbacks` (with filters) |
| `PATCH` | `/api/feedback/{id}/status` | Feedback (App Feedback) | `/api/feedbacks/{feedbackId}/status` |

> **Action required:** Fix the dashboard to call the correct endpoints (`/api/feedbacks` and `/api/feedbacks/{feedbackId}/status`).

---

## Non-Existent Endpoints (Documentation Only — No Code Existed)

These appeared in `app_used_apis.md` but were never implemented. Can be removed from documentation.

| Endpoint | Method |
|----------|--------|
| `/api/audio-stream/stream/{id}` | GET |
| `/api/audio-stream/info/{id}` | GET |
| `/api/audio-stream/play/{id}` | POST |
| `/api/audio-stream/pause/{id}` | POST |
| `/api/audio-stream/stop/{id}` | POST |
