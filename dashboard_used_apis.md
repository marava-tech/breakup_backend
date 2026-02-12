# Dashboard Used APIs

> **Total Endpoints:** 32 | **Base URL:** `/api`
> **HTTP Client:** Axios (centralized instance in `src/config/api.ts`)

---

## Authentication

| # | Method | Endpoint | Purpose | Used In (files) |
|---|--------|----------|---------|:----------------:|
| 1 | `POST` | `/auth/send-otp-login` | Send OTP to email for login | 2 |
| 2 | `POST` | `/auth/verify-otp-login` | Verify OTP and complete login | 2 |
| 3 | `GET` | `/auth/me` | Get current authenticated user profile | 2 |
| 4 | `POST` | `/auth/refresh` | Refresh authentication token | 1 |

## Dashboard

| # | Method | Endpoint | Purpose | Used In (files) |
|---|--------|----------|---------|:----------------:|
| 5 | `GET` | `/health/status` | Get system health status (DB, API, memory, CPU, uptime) | 2 |
| 6 | `GET` | `/admin/dashboard/stats` | Get dashboard statistics (users, stories, engagement, etc.) | 2 |
| 7 | `GET` | `/audits` | Get audit logs / recent activity | 2 |
| 8 | `GET` | `/audits/user/{userId}` | Get audit history for a specific user | 3 |

## Users

| # | Method | Endpoint | Purpose | Used In (files) |
|---|--------|----------|---------|:----------------:|
| 9 | `GET` | `/admin/users` | Get paginated list of users with filters | 2 |

## Rewards / Coins

| # | Method | Endpoint | Purpose | Used In (files) |
|---|--------|----------|---------|:----------------:|
| 10 | `POST` | `/rewards/coin-history` | Add rewards/coins to a user | 2 |
| 11 | `GET` | `/rewards/coins/{userId}` | Get coin history and balance for a user | 4 |
| 12 | `PUT` | `/rewards/coin-history/invalidate` | Invalidate a coin history entry (with optional refund) | 4 |

## Stories

| # | Method | Endpoint | Purpose | Used In (files) |
|---|--------|----------|---------|:----------------:|
| 13 | `GET` | `/admin/stories` | Get paginated list of stories with filters | 2 |
| 14 | `PUT` | `/admin/stories/{storyId}` | Update story details (title, status, language, tags, etc.) | 2 |
| 15 | `DELETE` | `/admin/stories/{storyId}` | Delete a story | 2 |
| 16 | `PUT` | `/admin/stories/{storyId}/status` | Update story status (with optional rejection reason) | 3 |

## Feedbacks

| # | Method | Endpoint | Purpose | Used In (files) |
|---|--------|----------|---------|:----------------:|
| 17 | `GET` | `/feedbacks` | Get paginated list of feedbacks with filters | 2 |
| 18 | `POST` | `/feedbacks/{feedbackId}/admin-response` | Submit admin response to a feedback | 2 |
| 19 | `PUT` | `/feedbacks/{feedbackId}/status` | Update feedback status | 2 |

## Feedback (App Feedback)

| # | Method | Endpoint | Purpose | Used In (files) |
|---|--------|----------|---------|:----------------:|
| 20 | `GET` | `/feedback` | Get app feedback list | 2 |
| 21 | `PATCH` | `/feedback/{id}/status` | Update feedback status (resolved/unresolved) | 2 |

## Withdrawals

| # | Method | Endpoint | Purpose | Used In (files) |
|---|--------|----------|---------|:----------------:|
| 22 | `GET` | `/withdrawals` | Get list of withdrawal requests | 2 |
| 23 | `PUT` | `/withdrawals/{withdrawalId}/status` | Update withdrawal status (with file upload & comments) | 2 |

## Banned Devices

| # | Method | Endpoint | Purpose | Used In (files) |
|---|--------|----------|---------|:----------------:|
| 24 | `GET` | `/admin/banned-devices/search` | Search for banned devices with filters | 2 |
| 25 | `POST` | `/admin/banned-devices/ban` | Ban a device | 2 |
| 26 | `DELETE` | `/admin/banned-devices/unban/{deviceId}` | Unban a device | 2 |
| 27 | `PUT` | `/admin/banned-devices/{deviceId}` | Update banned device details (reason, emails) | 2 |

## Configs

| # | Method | Endpoint | Purpose | Used In (files) |
|---|--------|----------|---------|:----------------:|
| 28 | `GET` | `/configs/search` | Search for configuration entries | 2 |
| 29 | `POST` | `/configs` | Create a new configuration entry | 2 |
| 30 | `POST` | `/configs/upload` | Upload a configuration file | 2 |
| 31 | `PUT` | `/configs/{id}` | Update a configuration entry | 2 |
| 32 | `DELETE` | `/configs/{id}` | Delete a configuration entry | 2 |

---

## Method Distribution

| Method | Count |
|--------|:-----:|
| GET | 15 |
| POST | 8 |
| PUT | 7 |
| DELETE | 2 |
| PATCH | 1 |

## Most Used Endpoints

| Endpoint | Files | Reason |
|----------|:-----:|--------|
| `/rewards/coins/{userId}` | 4 | Shared across users & withdrawals features |
| `/rewards/coin-history/invalidate` | 4 | Shared across users & withdrawals features |
| `/audits/user/{userId}` | 3 | Used in users feature + UserDetailModal |
| `/admin/stories/{storyId}/status` | 3 | Used in stories feature + StoryDetailModal |
