# App API Usage Summary

APIs used in the Breakup app, with endpoint, purpose, and usage count.

---

## Base URLs

| Base URL | Purpose |
|----------|---------|
| `https://breakup-backend.marava.tech` | Main backend |
| `https://breakupaiv2.marava.tech` | Consoling / AI service |

---

## API Endpoints

| # | API Name | Endpoint | Method | Why | Times Used |
|---|----------|----------|--------|-----|------------|
| 1 | **Health Ping** | `/api/health/ping` | GET | Check backend availability (splash, main, error) | 3 |
| 2 | **Validate Token** | *(local)* | - | Check JWT validity before protected routes | 4 |
| 3 | **Send OTP (Registration)** | `/api/auth/send-otp-registration` | POST | Initiate signup OTP flow | 2 |
| 4 | **Send OTP (Login)** | `/api/auth/send-otp-login` | POST | Initiate login OTP flow | 2 |
| 5 | **Verify OTP (Registration)** | `/api/auth/verify-otp-registration` | POST | Complete signup | 1 |
| 6 | **Verify OTP (Login)** | `/api/auth/verify-otp-login` | POST | Complete login | 1 |
| 7 | **Get Stories** | `/api/stories` | GET | Feed / For You stories | 2 |
| 8 | **Get My Stories** | `/api/stories/my-stories` | GET | User’s created stories | 2 |
| 9 | **Create Story (Audio)** | `/api/stories` | POST | Upload voice recording | 2 |
| 10 | **Create Story (Written)** | `/api/stories/written` | POST | Submit text story | 1 |
| 11 | **Get Story by ID** | `/api/stories/{id}` | GET | Full story details | 5 |
| 12 | **Get Stories by Type** | `/api/stories/type` | GET | Filter by type (SIMILAR, LANGUAGE, NEAR_ME) | 2 |
| 13 | **Search Stories** | `/api/stories/search` | GET | Search with query | 1 |
| 14 | **Like Story** | `/api/stories/{id}/like` | POST | Like a story | 1 |
| 15 | **Unlike Story** | `/api/stories/{id}/like` | DELETE | Remove like | 1 |
| 16 | **Retry Story Processing** | `/api/story/{id}/process` (consoling) | POST | Retry failed AI processing | 1 |
| 17 | **Create Bookmark** | `/api/bookmarks/story/{id}` | POST | Bookmark story | 2 |
| 18 | **Remove Bookmark** | `/api/bookmarks/story/{id}` | DELETE | Remove bookmark | 2 |
| 19 | **Get Bookmarked Stories** | `/api/bookmarks/stories` | GET | Profile bookmarks | 1 |
| 20 | **Get Story Comments** | `/api/comments/story/{id}` | GET | Paginated comments | 1 |
| 21 | **Add Comment** | `/api/comments` | POST | Add comment | 2 |
| 22 | **Delete Comment** | `/api/comments/{id}` | DELETE | Delete own comment | 1 |
| 23 | **Get Notifications** | `/api/notifications` | GET | Notification list | 1 |
| 24 | **Get Languages** | `/api/configs/languages` | GET | Supported languages | 1 |
| 25 | **Get User Profile** | `/api/users/profile` | GET | Current user profile | 1 |
| 26 | **Upload Profile Image** | `/api/users/profile-image` | POST | Update profile photo | 1 |
| 27 | **Get App Configs** | `/api/configs/app-configs` | GET | App-level config, updates | 4 |
| 28 | **Get User Configs** | `/api/configs/user-configs` | GET | User-level config | 2 |
| 29 | **Get Device Configs** | `/api/configs/device-configs` | GET | Device status, ban check | 1 |
| 30 | **Check Email Banned** | `/api/public/check-email-ban` | GET | Ban check before login | 1 |
| 31 | **Get Consoling Message** | `/api/story/{id}/consoling-message` (consoling) | GET | AI-generated consoling message | 4 |
| 32 | **Submit Feedback** | `/api/feedbacks` | POST | Submit user feedback | 1 |
| 33 | **Get My Feedbacks** | `/api/feedbacks/my-feedbacks` | GET | User’s feedback history | 1 |
| 34 | **Get Coin Balance** | `/api/rewards/coins` | GET | User coins balance | 2 |
| 35 | **Get Referral Stats** | `/api/rewards/referral-stats` | GET | Referral stats | 1 |
| 36 | **Get My Withdrawals** | `/api/withdrawals/my-withdrawals` | GET | Withdrawal history | 1 |
| 37 | **Get Processed Withdrawals** | `/api/withdrawals?status=PROCESSED` | GET | Inspiration list | 1 |
| 38 | **Create Withdrawal** | `/api/withdrawals` | POST | Create withdrawal request | 1 |

---

## APIs Defined but Not Used

| API Name | Endpoint | Method | Purpose |
|----------|----------|--------|---------|
| Get Comment Count | `/api/comments/story/{id}/count` | GET | Comment count for story |
| Get Feedbacks | `/api/feedbacks` | GET | All feedbacks (admin) |
| Get Device Referral Status | `/api/auth/device-referral-status/{id}` | GET | Referral usage per device |
| Get Withdrawal Eligibility | `/api/stories/withdrawal-eligibility` | GET | Check withdrawal eligibility |
| Get Audio Stream URL | `/api/audio-stream/stream/{id}` | GET | Audio stream URL |
| Get Audio Info | `/api/audio-stream/info/{id}` | GET | Audio metadata |
| Log Audio Play | `/api/audio-stream/play/{id}` | POST | Log play event |
| Log Audio Pause | `/api/audio-stream/pause/{id}` | POST | Log pause event |
| Log Audio Stop | `/api/audio-stream/stop/{id}` | POST | Log stop event |

---

## Usage by Category

| Category | APIs | Total Calls |
|----------|------|-------------|
| Auth | 6 | 11 |
| Stories | 11 | 23 |
| Bookmarks | 3 | 5 |
| Comments | 3 | 4 |
| Config | 5 | 9 |
| Profile | 2 | 2 |
| Feedback | 2 | 2 |
| Rewards & Withdrawals | 5 | 6 |
| Consoling (AI) | 2 | 5 |
| Health | 1 | 3 |
