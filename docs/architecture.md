# Architecture — Breakup Stories Backend

## System overview

Flutter app + React admin → Spring Boot REST API → MongoDB (primary) + Redis (cache)

## Content pipeline
1. User submits story text via Flutter app
2. Admin reviews and approves in dashboard
3. LLM (StoryRewriteService) rewrites/enhances the story
4. TTS (TTSService) converts to audio
5. Audio stored in Cloudinary
6. Video record published to feed
7. Users discover via recommendation feed (ShortVideoRecommendationService)

## Authentication
- User app: Email OTP via Gmail SMTP → backend JWT
- Admin dashboard: separate admin auth

## Key services
- `ShortVideoService` — CRUD for video content
- `ShortVideoInteractionService` — likes, comments, shares (atomic operations)
- `ShortVideoRecommendationService` — feed algorithm, language-based filtering
- `StoryRewriteService` — LLM integration for story enhancement
- `TTSService` — text-to-speech audio generation
- `TranscriptionService` — audio → text (with GCS integration)
- `StoryProcessingService` — orchestrates the full content pipeline
