# APIs Review

> Reference doc for all API endpoints: which are actively used and which are worth keeping even though they aren't directly called by the current app/dashboard.

---

## Used APIs

### App (Android / Web App)

| # | Method | Endpoint | Purpose |
|---|--------|----------|---------|
| 1 | `GET` | `/api/health/ping` | Connectivity check on app launch |
| 2 | `POST` | `/api/auth/send-otp-registration` | Signup OTP flow |
| 3 | `POST` | `/api/auth/send-otp-login` | Login OTP flow |
| 4 | `POST` | `/api/auth/verify-otp-registration` | Complete signup |
| 5 | `POST` | `/api/auth/verify-otp-login` | Complete login |
| 6 | `GET` | `/api/public/check-email-ban` | Pre-login ban check |
| 7 | `GET` | `/api/stories` | Main feed |
| 8 | `GET` | `/api/stories/my-stories` | User's own stories |
| 9 | `POST` | `/api/stories` | Upload voice story |
| 10 | `POST` | `/api/stories/written` | Submit text story |
| 11 | `GET` | `/api/stories/{storyId}` | Story detail page |
| 12 | `GET` | `/api/stories/type` | Filtered feed (SIMILAR, LANGUAGE, NEAR_ME) |
| 13 | `GET` | `/api/stories/search` | Search stories |
| 14 | `POST` | `/api/stories/{storyId}/like` | Like a story |
| 15 | `DELETE` | `/api/stories/{storyId}/like` | Unlike a story |
| 16 | `POST` | `/api/bookmarks/story/{storyId}` | Bookmark a story |
| 17 | `DELETE` | `/api/bookmarks/story/{storyId}` | Remove bookmark |
| 18 | `GET` | `/api/bookmarks/stories` | View bookmarked stories |
| 19 | `GET` | `/api/comments/story/{storyId}` | Load story comments |
| 20 | `POST` | `/api/comments` | Add a comment |
| 21 | `DELETE` | `/api/comments/{commentId}` | Delete own comment |
| 22 | `GET` | `/api/notifications` | Notification list |
| 23 | `GET` | `/api/users/profile` | Current user profile |
| 24 | `POST` | `/api/users/profile-image` | Update profile photo |
| 25 | `GET` | `/api/configs/languages` | Supported languages |
| 26 | `GET` | `/api/configs/app-configs` | App config / update check |
| 27 | `GET` | `/api/configs/user-configs` | User-level config |
| 28 | `GET` | `/api/configs/device-configs` | Device ban status |
| 29 | `POST` | `/api/feedbacks` | Submit feedback |
| 30 | `GET` | `/api/feedbacks/my-feedbacks` | User's feedback history |
| 31 | `GET` | `/api/rewards/coins` | Coin balance |
| 32 | `GET` | `/api/rewards/referral-stats` | Referral code + stats |
| 33 | `GET` | `/api/withdrawals/my-withdrawals` | Withdrawal history |
| 34 | `GET` | `/api/withdrawals?status=PROCESSED` | Processed withdrawals (inspiration list) |
| 35 | `POST` | `/api/withdrawals` | Create withdrawal request |

### Dashboard (Admin Web)

| # | Method | Endpoint | Purpose |
|---|--------|----------|---------|
| 1 | `POST` | `/api/auth/send-otp-login` | Admin login OTP |
| 2 | `POST` | `/api/auth/verify-otp-login` | Complete admin login |
| 3 | `GET` | `/api/auth/me` | Get logged-in admin profile |
| 4 | `POST` | `/api/auth/refresh` | Refresh JWT |
| 5 | `GET` | `/api/health/status` | System health (DB, CPU, memory, uptime) |
| 6 | `GET` | `/api/admin/dashboard/stats` | Dashboard stats overview |
| 7 | `GET` | `/api/audits` | Recent activity / audit logs |
| 8 | `GET` | `/api/audits/user/{userId}` | Audit history for a user |
| 9 | `GET` | `/api/admin/users` | Paginated user list |
| 10 | `GET` | `/api/rewards/coins/{userId}` | Coin balance for a user |
| 11 | `POST` | `/api/rewards/coin-history` | Manually credit coins |
| 12 | `PUT` | `/api/rewards/coin-history/invalidate` | Invalidate a coin entry |
| 13 | `GET` | `/api/admin/stories` | Paginated story list |
| 14 | `PUT` | `/api/admin/stories/{storyId}` | Edit story details |
| 15 | `DELETE` | `/api/admin/stories/{storyId}` | Delete story |
| 16 | `PUT` | `/api/admin/stories/{storyId}/status` | Approve / reject story |
| 17 | `GET` | `/api/feedbacks` | All feedbacks with filters |
| 18 | `POST` | `/api/feedbacks/{feedbackId}/admin-response` | Admin response to feedback |
| 19 | `PUT` | `/api/feedbacks/{feedbackId}/status` | Update feedback status |
| 20 | `GET` | `/api/withdrawals` | All withdrawals |
| 21 | `PUT` | `/api/withdrawals/{withdrawalId}/status` | Process / reject withdrawal |
| 22 | `GET` | `/api/admin/banned-devices/search` | Search banned devices |
| 23 | `POST` | `/api/admin/banned-devices/ban` | Ban a device |
| 24 | `DELETE` | `/api/admin/banned-devices/unban/{deviceId}` | Unban a device |
| 25 | `PUT` | `/api/admin/banned-devices/{deviceId}` | Update ban details |
| 26 | `GET` | `/api/configs/search` | Search config entries |
| 27 | `POST` | `/api/configs` | Create config entry |
| 28 | `POST` | `/api/configs/upload` | Bulk upload config |
| 29 | `PUT` | `/api/configs/{id}` | Update config entry |
| 30 | `DELETE` | `/api/configs/{id}` | Delete config entry |

---

## Keep — Not Directly Used But Necessary

These endpoints are not currently called by any client but should be **kept** because they serve a clear purpose, are likely needed for upcoming features, or are required for correctness and data integrity.

### Auth
| Method | Endpoint | Why Keep |
|--------|----------|----------|
| *(none)* | — | — |

### Stories
| Method | Endpoint | Why Keep |
|--------|----------|----------|
| `GET` | `/api/stories/liked` | Natural next step for a "Liked Stories" profile tab |
| `GET` | `/api/stories/{storyId}/like-count` | Useful if like count is ever displayed standalone (e.g., like button refresh) |
| `GET` | `/api/stories/withdrawal-eligibility` | Business logic — needed before showing withdrawal CTA; flagged as defined-but-unused in app docs, likely a planned feature |

### Comments
| Method | Endpoint | Why Keep |
|--------|----------|----------|
| `GET` | `/api/comments/{commentId}/replies` | Needed if threaded replies are added to the app |
| `PUT` | `/api/comments/{commentId}` | Comment editing — obvious future feature |
| `POST` | `/api/comments/{commentId}/flag` | User-reported comment moderation |
| `POST` | `/api/comments/{commentId}/unflag` | Counterpart to flag — keep together |
| `GET` | `/api/comments/story/{storyId}/count` | Lightweight count for story cards without loading all comments |

### Bookmarks
| Method | Endpoint | Why Keep |
|--------|----------|----------|
| `GET` | `/api/bookmarks/check/{storyId}` | Real-time bookmark status check when navigating to a story |

### Users
| Method | Endpoint | Why Keep |
|--------|----------|----------|
| `GET` | `/api/users/{userId}` | Viewing another user's public profile |
| `PUT` | `/api/users/preferred-language` | Language preference setting in app — clearly a planned feature |

### Rewards
| Method | Endpoint | Why Keep |
|--------|----------|----------|
| `GET` | `/api/rewards/referral-stats/{userId}` | Admin might need to view any user's referral stats (not just own) |
| `GET` | `/api/rewards/configurations` | Admin visibility into reward rule configuration |

### Withdrawals
| Method | Endpoint | Why Keep |
|--------|----------|----------|
| `GET` | `/api/withdrawals/options` | Needed before the withdrawal form is shown — currently unused suggests a UI gap |
| `GET` | `/api/withdrawals/{withdrawalId}` | Withdrawal receipt / detail view |

### Admin — Analytics
| Method | Endpoint | Why Keep |
|--------|----------|----------|
| `GET` | `/api/admin/stories/statistics` | Dashboard analytics — clearly useful, just not wired up yet |
| `GET` | `/api/admin/users/statistics` | Same — analytics not yet wired |
| `GET` | `/api/admin/comments` | Comment moderation panel (planned) |
| `DELETE` | `/api/admin/comments/{commentId}` | Admin comment removal — essential for moderation |
| `GET` | `/api/admin/feedback/statistics` | Feedback analytics |
| `GET` | `/api/admin/withdrawals/statistics` | Withdrawal analytics |
| `GET` | `/api/audits/analytics/story-views` | Story performance analytics |
| `GET` | `/api/audits/analytics/user-activity` | User engagement analytics |

### Configs
| Method | Endpoint | Why Keep |
|--------|----------|----------|
| `GET` | `/api/configs/story-creation-config` | Story creation config — needed if the app reads creation rules from backend |

---

## Cleanup Required — Dashboard Bug

The following endpoints are called by the dashboard but **do not exist** in the backend. They need to be fixed.

| Dashboard Calls | Should Map To |
|-----------------|---------------|
| `GET /api/feedback` | `GET /api/admin/feedback` or `GET /api/feedbacks` |
| `PATCH /api/feedback/{id}/status` | `PUT /api/admin/feedback/{feedbackId}/status` or `PUT /api/feedbacks/{feedbackId}/status` |

> Decide on one canonical route for admin feedback management and align both backend and dashboard.
