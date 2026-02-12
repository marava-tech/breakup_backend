# App Integration Guide — Backend Performance Updates

This guide describes frontend changes needed (if any) after the backend performance improvements in `claude-improvements.md`. All app-used APIs from `app_used_apis.md` have been reviewed.

---

## Summary

| API | Change Required? | Notes |
|-----|------------------|--------|
| Health Ping | Optional | Response simplified |
| Get Stories | Optional | New cursor pagination support |
| Get Stories by Type | Optional | New cursor pagination support |
| All other APIs | None | No contract changes |

---

## 1. Health Ping — `/api/health/ping`

**Previous response:**
```json
{
  "status": "OK",
  "message": "Service is running",
  "timestamp": "2025-02-12T...",
  "service": "Breakup Stories Backend",
  "version": "v1.0"
}
```

**Current response:**
```json
{
  "status": "OK"
}
```

**Frontend impact:**  
If the app only checks for `status === "OK"` or a 200 response, no change is needed. Connectivity checks work as before.

**If you use other fields:**
- `message`, `timestamp`, `service`, `version` are no longer returned.
- Update code to avoid accessing these fields, or use `/api/health/status` for more detailed info.

---

## 2. PagedResponse — New Optional Field

Story and comment list endpoints now include an optional `nextCursor` field:

```json
{
  "content": [...],
  "page": 0,
  "size": 10,
  "totalElements": 150,
  "totalPages": 15,
  "last": false,
  "nextCursor": "2025-02-12T10:30:00"
}
```

- **Without cursor pagination:** `nextCursor` is `null`. Existing behavior is unchanged.
- **With cursor pagination:** `nextCursor` is set when more pages exist. Use it for the next request.

**Action:** No change required. Existing fields are unchanged; `nextCursor` can be ignored if you keep using `page`/`size`.

---

## 3. Cursor-Based Pagination (Optional Enhancement)

### Endpoints

- `GET /api/stories?cursor=...&size=10`
- `GET /api/stories/type?searchType=LATEST&cursor=...&size=10`
- `GET /api/stories/type?searchType=GENERAL&cursor=...&size=10`

### When to use

Cursor pagination is more efficient for deep pagination (e.g. scrolling many pages). For the first few pages, `page`/`size` is fine.

### How to use

1. **First request:** Use `page=0` and `size=10` as before.
2. **Next request:** If `nextCursor` is not `null`, call the same endpoint with `cursor=<nextCursor>` and `size=10` instead of `page=1`.
3. **Stop:** When `nextCursor` is `null` or `last` is `true`, there are no more pages.

### Example (pseudo-code)

```javascript
// First page
const res1 = await api.get('/api/stories', { params: { page: 0, size: 10 } });

// Next page (if more data exists)
if (res1.data.nextCursor) {
  const res2 = await api.get('/api/stories', { params: { cursor: res1.data.nextCursor, size: 10 } });
}
```

### Cursor response semantics

When using cursor:
- `totalElements` and `totalPages` may not reflect full totals (they are optimized for cursor flow).
- `last` and `nextCursor` are the reliable indicators for “more pages”.

---

## 4. APIs With No Changes

| # | API | Endpoint | Status |
|---|-----|----------|--------|
| 2–6 | Auth (OTP, verify) | `/api/auth/*` | No changes |
| 8 | Get My Stories | `/api/stories/my-stories` | No changes |
| 9–10 | Create Story | `/api/stories`, `/api/stories/written` | No changes |
| 11 | Get Story by ID | `/api/stories/{id}` | No changes |
| 14–15 | Like / Unlike | `/api/stories/{id}/like` | No changes |
| 17–19 | Bookmarks | `/api/bookmarks/*` | No changes |
| 20–22 | Comments | `/api/comments/*` | No changes |
| 23–38 | Notifications, Config, Profile, Rewards, etc. | Various | No changes |

---

## 5. Checklist for App Update

- [ ] **Health Ping:** Ensure connectivity checks use `status === "OK"` or HTTP 200 only. Remove any use of `message`, `timestamp`, `service`, `version` if present.
- [ ] **PagedResponse:** Optional — add handling for `nextCursor` if you want cursor-based pagination.
- [ ] **Stories:** Optional — use `cursor` for feeds with heavy scrolling (e.g. infinite scroll beyond page 2–3).

---

## 6. Migration Strategy

- **Minimal:** No frontend changes. Existing usage remains valid.
- **Recommended:** Update health ping parsing if it relies on removed fields.
- **Optional:** Add cursor-based pagination for story feeds to improve performance on deep scrolls.
