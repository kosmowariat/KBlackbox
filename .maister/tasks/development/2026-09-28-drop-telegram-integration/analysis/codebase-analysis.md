# Codebase Analysis Report

## TL;DR
Telegram shows up in three places: the only CI workflow (`.github/workflows/build_and_telegram.yml`, 167 lines), one in-app menu item (a `main_tg` menu entry, a `MainActivity` branch and a `tg_group` string in 3 locales), and about 12 doc lines across 7 `.maister/docs` files. It's a small, removal-only change with no tests involved. The main risk is losing CI entirely, so replace the workflow with a build-only one (`actions/upload-artifact`) instead of just deleting it. Bcore's `org.telegram.messenger` compat code is out of scope and must not be touched.

## Key Decisions
- **Replace the workflow instead of deleting it.** Rename it to `build.yml` ("Build"), keep the build steps, drop the `TELEGRAM_*` env and the 6 send/pin steps, and publish outputs with `actions/upload-artifact@v4`. It's the repo's only workflow, so deleting it would leave no CI and no downloadable artifacts.
- **Remove the in-app link in one change.** `menu_main.xml`, `MainActivity.kt` and the 3 `strings.xml` files must change together, or the build breaks on dangling `R.id.main_tg` / `@string/tg_group` references.
- **Treat docs by type.** Current-state docs (INDEX, ci.md, conventions.md, tech-stack.md, architecture.md, roadmap.md:26) get reworded. Historical upstream lines (roadmap.md:6, vision.md:26) describe what upstream did, so they can stay or get a light edit.
- **Keep the commit-subject rule in conventions.md** ("meaningful commit subjects") and drop only the Telegram-caption reason.
- **Search precisely.** Don't do a blanket search/replace for "telegram", because it would hit Bcore guest-app compat code.

## Open Questions / Risks
- Does the operator want a build-only workflow (recommended) or no CI at all? This should be confirmed in gap analysis.
- Should the replacement workflow keep `push: main` + `workflow_dispatch` triggers, and how long should artifacts be kept?
- After the change, the `TELEGRAM_BOT_TOKEN` / `TELEGRAM_CHAT_ID` repository secrets are orphaned. The user has to delete them by hand in GitHub settings; code can't do it.
- Docs must stay consistent: the new workflow filename has to be reflected in INDEX.md, ci.md and tech-stack.md, and the INDEX summaries must match the reworded standards.
- Nothing can verify this automatically (no tests, no lint). Checking relies on `./gradlew assembleDebug` plus a final grep.

---

## Summary

The Telegram integration is shallow and self-contained. One CI workflow uploads build artifacts to a Telegram chat, one overflow-menu item opens the upstream Telegram group, and project docs describe both. There are no Telegram dependencies in Gradle, no drawables and no references in settings. README, Docs.md and RELEASE_NOTES.md are clean. Everything that remains named "Telegram" in Bcore is guest-app crash-compat logic and must be kept.

---

## Files Identified

### Primary Files

**.github/workflows/build_and_telegram.yml** (167 lines)
- The only workflow. It's named "Build and Send APK to Telegram" and runs job `build-and-send` on push to `main` and on `workflow_dispatch`.
- Env `TELEGRAM_BOT_TOKEN` and `TELEGRAM_CHAT_ID` come from secrets (:13-14).
- Steps to keep: `actions/checkout@v4`, `actions/setup-java@v4` (temurin 21, gradle cache), `android-actions/setup-android@v3`, `sdkmanager --licenses`, `chmod +x gradlew`, and `./gradlew assembleDebug assembleRelease :Bcore:assembleDebug :Bcore:assembleRelease :black-reflection:assemble :compiler:assemble`.
- Steps to remove: the `aar_paths` / `additional_artifacts` steps (they only feed Telegram), and the six "Send … to Telegram and Pin" steps (:54, :73, :92, :112, :132, :150). These use curl with `sendDocument` + `pinChatMessage`, caption prefixes `[Bcore DEBUG AAR]`, `[Bcore RELEASE AAR]`, `[black-reflection]`, `[compiler]`, `[DEBUG]`, `[RELEASE]`, and `COMMIT_MSG=$(git log -1 --pretty=%s)`.

**app/src/main/java/top/niunaijun/blackboxa/view/main/MainActivity.kt** (369 lines)
- Lines 351-354: the `R.id.main_tg -> { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/newblackboxa"))) }` branch in `onOptionsItemSelected`.
- The `Intent` / `Uri` imports stay, because `main_git` and line ~176 still use them.

**app/src/main/res/menu/menu_main.xml** (29 lines)
- Lines 26-29: the last item, `main_tg` (`@string/tg_group`, `showAsAction="never"`).

**app/src/main/res/values/strings.xml**: line 12, `tg_group` = "Telegram Group"
**app/src/main/res/values-zh-rCN/strings.xml**: line 12, `tg_group` = "交流群"
**app/src/main/res/values-zh-rTW/strings.xml**: line 11, `tg_group` = "電報組"
- There are no other usages (verified by grep).

### Related Files (documentation)

**.maister/docs/INDEX.md**
- :35 "GitHub Actions to Telegram CI"; :63 "the CI uses them as Telegram captions"; :148-149 the CI & Distribution summary (`build_and_telegram.yml`, posts to Telegram, Telegram credentials from secrets).

**.maister/docs/standards/build/ci.md**
- :4 the workflow description (posts to Telegram with caption prefixes); :7-8 the `### Secrets` section (`TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID`), which becomes obsolete.

**.maister/docs/standards/global/conventions.md**
- :11 says the commit subject is used as the Telegram caption. Keep the rule and drop the reason.

**.maister/docs/project/tech-stack.md**
- :73, :75: the workflow name and "uploads to Telegram chat and pins".

**.maister/docs/project/architecture.md**
- :99 "Telegram Bot API: CI artifact distribution only" (drop this integration entry); :107 "posted to Telegram" (reword the deployment line).

**.maister/docs/project/roadmap.md**
- :26 "CI only builds and uploads to Telegram" (current tech debt, so reword it); :6 the upstream history line "Build and Telegram distribution" (historical).

**.maister/docs/project/vision.md**
- :26 upstream "CI distribution through Telegram" (historical).

### Out of Scope (keep unchanged)
- `Bcore/src/main/java/top/niunaijun/blackbox/utils/SocialMediaAppCrashPrevention.java:31` (`"org.telegram.messenger"`)
- `Bcore/src/main/java/top/niunaijun/blackbox/utils/SimpleCrashFix.java:257, :273` (guest-app crash heuristics)
- `.maister/tasks/**` (historical artifacts)
- `.github/FUNDING.yml` has no Telegram references.

---

## Current Functionality

### Key Components
- **CI workflow**: builds all app APKs (per ABI `armeabi-v7a`, `arm64-v8a` + universal, debug and release), the Bcore AARs, and the `black-reflection` / `compiler` JARs. It then posts each one to a Telegram chat, captioned with the last commit subject, and pins the message. There is no other way to get the artifacts, and no tests or lint run.
- **Main menu "Telegram Group" item**: an overflow menu entry that opens `https://t.me/newblackboxa`, which is the upstream community group and not this fork's. The neighbouring `main_git` item opens the upstream GitHub repo (`ALEX5402/NewBlackbox`), while the fork's origin is `kosmowariat/KBlackbox`. That's context only and outside this task.

### Data Flow
- CI: push to main → Gradle build → artifact paths found → curl to the Telegram Bot API (`sendDocument`, `pinChatMessage`).
- App: overflow menu tap → `onOptionsItemSelected` → `ACTION_VIEW` intent → external Telegram / browser.

---

## Dependencies

### Imports
- Workflow: GitHub secrets `TELEGRAM_BOT_TOKEN` / `TELEGRAM_CHAT_ID`, and the Telegram Bot API over curl.
- App: `android.content.Intent` and `android.net.Uri`, which are shared and must stay.

### Consumers
- `R.id.main_tg`: only `MainActivity.kt`.
- `R.string.tg_group`: only `menu_main.xml`.
- Workflow filename: referenced by INDEX.md, ci.md and tech-stack.md.

**Consumer Count**: 1-2 per symbol
**Impact Scope**: Low. Everything is leaf-level and nothing else in the code depends on it.

---

## Test Coverage

### Test Files
- None. The project has no automated tests (K5), and CI runs neither tests nor lint.

### Coverage Assessment
- **Test count**: 0
- **Gaps**: all verification is manual. Check that `./gradlew assembleDebug` succeeds (no dangling resource references), that the overflow menu no longer shows the item, that the replacement workflow parses and runs on GitHub, and that a final grep for `telegram|tg_group|main_tg|t\.me` finds hits only in the out-of-scope locations.

---

## Coding Patterns

### Naming Conventions
- **Menu ids**: `main_<action>` (`main_git`, `main_setting`, `main_tg`)
- **Strings**: snake_case, mirrored in `values-zh-rCN` / `values-zh-rTW`
- **Workflow file**: describes what it does (`build_and_telegram.yml`), so the replacement would be `build.yml`

### Architecture Patterns
- **Style**: Kotlin Activity with `when`-based `onOptionsItemSelected`, and the Groovy Gradle build
- **CI**: a single GitHub Actions job on ubuntu-latest with JDK 21

---

## Complexity Assessment

| Factor | Value | Level |
|--------|-------|-------|
| File Size | 167 / 369 / 29 lines (primary) | Low |
| Dependencies | 2 secrets, 2 shared imports | Low |
| Consumers | 1-2 per symbol | Low |
| Test Coverage | 0 tests (nothing to break, nothing to verify automatically) | Low (coverage) |
| File count | 6 code/config + 7 docs = 13 files | High by count, but trivial per file |

### Overall: Simple

The change is removal-only and mechanical. It spreads across many files, but each edit is small, and nothing in it changes engine behaviour.

---

## Key Findings

### Strengths
- The integration is isolated: no Gradle dependency, no drawable, no settings entry, no Kotlin helper.
- The build command in the workflow is independent of the Telegram steps, so it's easy to keep.

### Concerns
- Deleting the workflow outright would remove the fork's only CI and its only artifact distribution.
- A blanket rename would touch Bcore compat code that handles the real Telegram guest app.
- The doc index and the standards could drift apart if the edits aren't coordinated.

### Opportunities
- Switching to `actions/upload-artifact@v4` gives artifacts downloadable from the GitHub run page, with no secrets needed.
- `if-no-files-found: error` (or `warn`) on the upload steps would catch a broken build output path early.
- Removing the upstream community link fits the fork's independent identity. The `main_git` upstream link could be a separate follow-up.

---

## Impact Assessment

- **Primary changes**: `.github/workflows/build_and_telegram.yml` (replace with `build.yml`), `MainActivity.kt`, `menu_main.xml`, and the 3 `strings.xml` files
- **Related changes**: INDEX.md, ci.md, conventions.md, tech-stack.md, architecture.md, roadmap.md (+ optionally vision.md for the historical line)
- **Test updates**: none, only manual build and grep checks

### Risk Level: Low

The code edits are local and can be checked at compile time. The only functional risk is the CI replacement, since the workflow can only really be tested once it runs on GitHub. Deleting the orphaned secrets is a manual step for the user.

---

## Recommendations

**Modifying existing code (removal):**
1. **App**: in one commit, remove the `main_tg` menu item, the `when` branch in `MainActivity.kt:351-354` and the `tg_group` strings in all 3 locales. Keep the `Intent` / `Uri` imports. Build with `./gradlew assembleDebug`.
2. **CI**: `git mv` the workflow to `.github/workflows/build.yml` and set `name: Build`. Keep the triggers and build steps. Remove the job `env`, the `aar_paths` / `additional_artifacts` steps and the six send steps. Add `actions/upload-artifact@v4` steps, for example `app-debug-apks` (`app/build/outputs/apk/debug/*.apk`), `app-release-apks` (`app/build/outputs/apk/release/*.apk`), `bcore-aars` (`Bcore/build/outputs/aar/*.aar`) and `libs` (the black-reflection and compiler jars), each with `if-no-files-found`. Rename the job from `build-and-send` to `build`.
3. **Docs**: reword the current-state lines to describe the build-only workflow and artifact upload. Drop the ci.md `### Secrets` section, and the architecture.md Telegram Bot API entry. Keep the commit-subject rule in conventions.md without the Telegram reason. Update the INDEX.md summaries (:35, :63, :148-149) to match, including the new filename. Leave the historical upstream mentions (roadmap.md:6, vision.md:26) as they are, or mark them clearly as upstream history.
4. **Verification**: grep the repo for `telegram|tg_group|main_tg|t\.me|TELEGRAM_`, excluding `.maister/tasks/`. Only the Bcore compat hits (and any deliberate historical lines) should remain.
5. **Manual follow-up for the user**: delete the `TELEGRAM_BOT_TOKEN` / `TELEGRAM_CHAT_ID` repository secrets in GitHub settings.
6. **Release notes**: this is optional. If it's a notable change, add a "Removed Features" entry to RELEASE_NOTES.md.

---

## Next Steps

Invoke the gap-analyzer to confirm the CI choice (build-only replacement vs. full deletion), artifact retention and triggers, and how to handle the historical doc lines. Then move on to specification and a small implementation plan with 3 task groups: app, CI and docs.
