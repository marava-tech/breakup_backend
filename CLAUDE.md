# Breakup Stories Backend

## What this is
Audio story platform API. Java 17 + Spring Boot 3.
MongoDB 7 (primary DB) + Redis 7 (cache only).
CI/CD: GitHub Actions → ghcr.io/marava-tech/breakup-backend:latest → auto-deploy on merge to main.

## Product context
Users submit breakup/relationship stories. AI (LLM) rewrites/enhances them. TTS converts to audio. Users listen to short-video-style content. OTP auth via email (Gmail SMTP). Stories reviewed by admin before publishing.

## Stack
- Java 17, Spring Boot 3.x
- MongoDB 7 (primary DB — videos, interactions, users, comments)
- Redis 7 (cache only)
- Cloudinary (image and audio uploads, Cloud: dhssmiyoc)
- Gmail SMTP (email OTP)
- LLM integration (story rewriting via StoryRewriteService)
- TTS service (text-to-speech audio generation)
- Transcription service (audio → text)
- Port: 9200 → domain: breakup-backend.marava.tech

## Project structure
```
src/main/java/com/breakup/
├── controller/     ← REST controllers
├── service/        ← business logic (LLM, TTS, transcription, recommendations)
├── repository/     ← Spring Data MongoDB repos
├── model/          ← DB entities (ShortVideo, ShortVideoInteraction, User, etc.)
├── dto/            ← request/response DTOs
├── exception/      ← custom exceptions
└── config/         ← Spring config
```

## Agents — load before acting
- New feature idea → read .claude/agents/spec.md first
- Writing/fixing code → read .claude/agents/backend.md first
- Testing → read .claude/agents/qa.md first
- Git/PR → read .claude/agents/git.md first
- Server/Docker → read .claude/agents/devops.md first

## Workflow
1. Spec agent writes specs/{feature}.md
2. Backend agent implements from spec
3. QA agent writes and runs tests
4. Git agent: branch → commit → push → PR
5. Merge PR → GitHub Actions auto-deploys. Never deploy manually.

## Hard rules
- Never push directly to main
- Never deploy manually
- Every feature needs a spec file before code is written
- All API routes under /api/v1/
- Error format: `{ "error": "message", "code": "ERROR_CODE" }`
- No default values in `@Value` for secrets — fail fast if env var missing
- Add individual `@Indexed` on fields queried alone (not just compound indexes)
- Batch DB calls — never call DB inside a loop (N+1 problem)
- Use atomic MongoDB operations for concurrent mutations (like/unlike)

## Related docs
- learnings.md — 24 documented bugs fixed + lessons learned
- memory/context.md — current focus
- specs/ — feature specifications
