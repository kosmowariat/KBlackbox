# Requirements

## TL;DR
Remove every Telegram integration and reference from KBlackbox: replace the CI workflow with a build-only build.yml that uploads GitHub Actions artifacts, remove the "Telegram Group" overflow-menu item (menu, handler, tg_group in 3 locales), and reword 7 .maister/docs files. Bcore guest-app compat code and historical task artifacts stay.

## Key Decisions
- CI keeps building on push to main + workflow_dispatch; Telegram send/pin steps are replaced by actions/upload-artifact (4 groups, 30 days, if-no-files-found: error).
- Remove historical upstream Telegram mentions from docs too.
- No RELEASE_NOTES.md entry.
- Only the "Telegram Group" menu item goes; Settings, Fake location and the upstream repo link stay.

## Open Questions / Risks
- User must delete TELEGRAM_BOT_TOKEN / TELEGRAM_CHAT_ID repository secrets manually.
- build.yml can only be verified on GitHub after push.

## Initial description
"zdropuj integracje telegramowe i odnośniki do niego" — drop the Telegram integrations and the references to it.

## Q&A
- Phase 1: CI → keep build only, rename build.yml, upload-artifact instead of Telegram. Historical doc mentions → remove too.
- Phase 2: RELEASE_NOTES entry → skip. Artifact policy → defaults (30-day retention, if-no-files-found: error, include debug-signed release APKs).
- Phase 5: confirmed assumptions — (1) user journey: only the "Telegram Group" item disappears from the main-screen overflow menu; Settings, Fake location, upstream repo link (ALEX5402/NewBlackbox) remain; (2) reuse: build.yml reproduces the current build steps exactly (checkout@v4, setup-java@v4 temurin 21 + gradle cache, setup-android@v3, sdkmanager licenses, chmod gradlew, same gradle command), nothing new added; (3) visual assets: none, no new UI.

## Functional requirements
1. Delete .github/workflows/build_and_telegram.yml; add .github/workflows/build.yml (name "Build", job "build") with the same triggers and build steps, no TELEGRAM_* env, no aar_paths/additional_artifacts find steps, and upload-artifact@v4 steps:
   - app-debug-apks: app/build/outputs/apk/debug/*.apk
   - app-release-apks: app/build/outputs/apk/release/*.apk
   - bcore-aars: Bcore/build/outputs/aar/*.aar
   - libs: black-reflection/build/libs/*.jar, compiler/build/libs/*.jar
   - retention-days: 30, if-no-files-found: error
2. Remove main_tg item from app/src/main/res/menu/menu_main.xml, the R.id.main_tg branch from MainActivity.kt, and tg_group from values, values-zh-rCN, values-zh-rTW strings.xml.
3. Reword/remove Telegram mentions in .maister/docs: INDEX.md, standards/build/ci.md (drop Secrets section, new workflow name + artifacts), standards/global/conventions.md (keep meaningful-commit-subject rule, drop Telegram reason), project/tech-stack.md, architecture.md, roadmap.md (:6 and :26), vision.md.

## Scope boundaries
- In: CI workflow, app menu link + strings, .maister/docs.
- Out: Bcore SocialMediaAppCrashPrevention.java / SimpleCrashFix.java (org.telegram.messenger compat), .maister/tasks/** history, RELEASE_NOTES.md, README.md/Docs.md (no hits), GitHub secrets (manual).

## Technical considerations
- Remove the menu item, Kotlin branch and all 3 strings in one change (dangling refs break the build).
- Intent/Uri imports in MainActivity.kt stay (still used).
- Verify with ./gradlew :app:assembleDebug locally; build.yml verified on GitHub after push.
