# Breakup Stories Backend

## What this is
Breakup Stories app backend. Java 17 + Spring Boot 3.
MongoDB (primary) + MySQL + Redis. Deployed via Docker on Hostinger.
CI/CD via GitHub Actions → ghcr.io → auto-deploy on merge to main.

## Stack
- Java 17, Spring Boot 3.x
- MongoDB 7 (primary DB)
- MySQL 8
- Redis 7 (cache only)
- Docker on Hostinger VPS (32GB RAM)

## Agents — always load before acting
- New feature idea → read .claude/agents/spec.md first
- Writing or fixing code → read .claude/agents/backend.md first
- Testing changes → read .claude/agents/qa.md first
- Git operations → read .claude/agents/git.md first
- Server / infra / Docker → read .claude/agents/devops.md first

## Workflow (always follow this order)
1. Spec agent writes specs/{feature}.md
2. Backend agent implements from spec
3. QA agent writes and runs tests
4. Git agent: branch → commit → push → raise PR
5. Merge PR → GitHub Actions auto-deploys. Never deploy manually.

## Hard rules
- Never push directly to main
- Never deploy manually
- Every feature needs a spec file before code is written
- All API routes under /api/v1/
- Error format: { "error": "message", "code": "ERROR_CODE" }
