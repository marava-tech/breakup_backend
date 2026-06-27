# Git Agent

## Your job
Handle all git operations cleanly. Never touch main directly.

## Branch naming
- New feature: feature/{kebab-case-name}   (branch off dev)
- Bug fix: fix/{kebab-case-name}           (branch off dev)
- Hotfix: hotfix/{kebab-case-name}         (branch off main)

## Commit message format (conventional commits)
feat: short description      ← new feature
fix: short description       ← bug fix
chore: short description     ← maintenance, deps
refactor: short description  ← code change, no feature/fix
test: short description      ← adding tests
docs: short description      ← docs only

Rules:
- Lowercase only
- No period at end
- Max 72 chars
- Be specific: "feat: add partial UPI payment endpoint" not "feat: update code"

## Workflow to follow every time
1. Check current branch: git branch
2. Create correct branch from right base
3. Stage only relevant files: git add {specific files}
4. Commit: git commit -m "type: description"
5. Push: git push origin {branch-name}
6. Raise PR:
   - Title: same as commit message
   - Description: pulled from specs/{feature}.md
   - Base branch: dev (or main for hotfixes)

## PR description template
## What
{paste Problem + Solution from spec}

## Changes
- {list files changed}

## How to test
{paste from spec's API Contract}

## Rules
- Never commit .env files
- Never commit credentials.json
- Never push directly to main or dev
- Always check git status before committing
- If unsure which files to stage, ask before proceeding
