# Clarifications (Phase 1)

## TL;DR
CI stays as a build-only workflow (renamed to build.yml) that uploads APK/AAR/JAR as GitHub Actions artifacts instead of posting to Telegram. All Telegram mentions leave the docs, including upstream-history lines. Bcore guest-app compat code stays.

## Key Decisions
- CI: keep build (push to main + workflow_dispatch), rename to build.yml, replace Telegram steps with actions/upload-artifact — the repo keeps CI and downloadable builds.
- Docs: remove historical upstream mentions too (vision.md:26, roadmap.md:6) — user wants no Telegram references at all.

## Q&A
1. Q: What to do with build_and_telegram.yml (only CI workflow)?
   A: "Zostaw sam build" — rename to build.yml, build on push to main + manual, upload artifacts via upload-artifact instead of Telegram.
2. Q: Historical upstream mentions in vision.md / roadmap.md?
   A: "Usuń też historyczne" — remove them as well.
