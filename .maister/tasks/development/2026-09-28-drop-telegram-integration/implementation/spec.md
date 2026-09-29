# Specification: Drop Telegram Integration

## TL;DR
Remove every Telegram integration and reference from KBlackbox, in three parts:
- **CI:** `build_and_telegram.yml` is replaced by a build-only `build.yml` that publishes 4 GitHub Actions artifact groups.
- **App:** the "Telegram Group" item leaves the main-screen overflow menu (menu item, Kotlin branch, and the `tg_group` string in all 3 locales).
- **Docs:** 7 `.maister/docs` files are reworded so no Telegram mention is left.

This is a removal-only change. The Bcore guest-app compat code, the `.maister/tasks/**` history and RELEASE_NOTES.md are not touched.

## Key Decisions
- **Replace the workflow instead of deleting it.** It is the repo's only CI, so the build steps are kept exactly as they are, and `actions/upload-artifact@v4` replaces the Telegram send/pin steps.
- **4 artifact groups, 30-day retention, `if-no-files-found: error`.** Each group can be downloaded on its own, and a broken build output fails loudly (the old workflow silently echoed "not found").
- **Delete the `aar_paths` and `additional_artifacts` find steps.** They only fed the Telegram steps, and the upload-artifact path globs replace them.
- **Remove the historical upstream mentions too** (roadmap.md:6, vision.md:26), because the user wants zero Telegram references.
- **No RELEASE_NOTES.md entry.** The user treats this as maintenance.
- **Keep the `Intent` and `Uri` imports in MainActivity.kt.** They are still used by `main_git` and elsewhere.

## Open Questions / Risks
- **Manual follow-up for the user:** delete the `TELEGRAM_BOT_TOKEN` and `TELEGRAM_CHAT_ID` repository secrets in GitHub (Settings → Secrets and variables → Actions). No code change can do this.
- **`build.yml` can only be checked on GitHub** (on push to `main` or by manual dispatch). Locally there is only YAML review plus `./gradlew :app:assembleDebug`.
- **Don't use a blanket search-and-replace.** `SimpleCrashFix.java:257,273` and `SocialMediaAppCrashPrevention.java:31` must stay untouched.
- **The final grep must not exclude directories named `build`.** Doing so hides `.maister/docs/standards/build/ci.md`. Exclude only by path (the `.git`, `.maister/tasks`, and module `*/build/` output dirs).

## Goal
KBlackbox, a solo independent fork, has no Telegram integration and no Telegram references left, except the Bcore compat code that keeps guest Telegram apps from crashing. CI keeps building every module and makes the outputs downloadable from GitHub.

## User Stories
- As an end user, I don't want to see an overflow-menu link to the upstream Telegram chat, which is unrelated to this fork.
- As the maintainer, I want CI to keep building every module and give me downloadable artifacts, without needing Telegram credentials.
- As a contributor, I want the project docs and standards to describe the real CI pipeline.

## Core Requirements
1. **R1: Replace the CI workflow.** Delete `.github/workflows/build_and_telegram.yml` and create `.github/workflows/build.yml` as follows.
   - Name it `Build`, with job `build` on `ubuntu-latest`. Triggers are `push` to `main` and `workflow_dispatch`.
   - There is no `env` block and there are no `TELEGRAM_*` references.
   - Steps are identical to the current ones: `actions/checkout@v4`; `actions/setup-java@v4` (temurin, 21, `cache: gradle`); `android-actions/setup-android@v3`; the sdkmanager license-accept command; `chmod +x ./gradlew`; and `./gradlew assembleDebug assembleRelease :Bcore:assembleDebug :Bcore:assembleRelease :black-reflection:assemble :compiler:assemble`.
   - These are followed by 4 `actions/upload-artifact@v4` steps. Each one sets `retention-days: 30` and `if-no-files-found: error`.

   | Artifact name | Path(s) |
   |---|---|
   | `app-debug-apks` | `app/build/outputs/apk/debug/*.apk` |
   | `app-release-apks` | `app/build/outputs/apk/release/*.apk` |
   | `bcore-aars` | `Bcore/build/outputs/aar/*.aar` |
   | `libs` | `black-reflection/build/libs/*.jar` and `compiler/build/libs/*.jar` (multi-line `path`) |

2. **R2: Remove the menu item.** Delete the `main_tg` `<item>` from `app/src/main/res/menu/menu_main.xml` (lines 26-29). The layout, setting, fake_location and main_git items stay.
3. **R3: Remove the handler.** Delete the `R.id.main_tg -> { ... }` branch (the `https://t.me/newblackboxa` intent) from `onOptionsItemSelected` in `app/src/main/java/top/niunaijun/blackboxa/view/main/MainActivity.kt` (lines 351-354).
4. **R4: Remove the string in all 3 locales.** Delete the `tg_group` entry from:
   - `app/src/main/res/values/strings.xml:12`
   - `values-zh-rCN/strings.xml:12`
   - `values-zh-rTW/strings.xml:11`
5. **R5: Update `standards/build/ci.md`.**
   - Pipeline section: describe `build.yml`, which builds all APKs/AARs/JARs and uploads them as GitHub Actions artifacts (the 4 groups, 30-day retention). It is downloadable from the run page. Keep "No tests or lint run in CI today."
   - Secrets section: the workflow needs no secrets, and secrets are never committed. Delete the section only if nothing meaningful is left.
6. **R6: Update `standards/global/conventions.md:11`.** Keep the "write meaningful commit subjects" rule and drop the Telegram-caption reason.
7. **R7: Update `project/tech-stack.md`.** At :73, change the workflow to `.github/workflows/build.yml`. At :75, say the outputs are uploaded as GitHub Actions workflow artifacts.
8. **R8: Update `project/architecture.md`.** Remove the :99 "Telegram Bot API" integration line. At :107, change "posted to Telegram" to "uploaded as GitHub Actions artifacts".
9. **R9: Update `project/roadmap.md`.** At :6, reword the upstream update to mention only CI builds for all modules. At :26, change the text to "CI only builds and uploads artifacts".
10. **R10: Update `project/vision.md:26`.** Change "CI distribution through Telegram" to "CI builds", or drop the clause.
11. **R11: Update `.maister/docs/INDEX.md`.**
    - :35: "GitHub Actions to Telegram CI" becomes GitHub Actions build CI with artifact upload.
    - :63: drop the Telegram-caption reason.
    - :149: the CI & Distribution summary describes `build.yml` with upload-artifact and mentions no Telegram credentials.

    The INDEX summaries must match the reworded standards.
12. **R12: Final sweep.** A case-insensitive grep for `telegram|t\.me/|tg_group|main_tg|newblackboxa` across the repo matches only the 2 Bcore compat files. The grep excludes `.git`, `.maister/tasks/**` and module build-output directories.

## Reusable Components

### Existing Code to Leverage
- `.github/workflows/build_and_telegram.yml` lines 1-37: triggers and build steps, copied unchanged into `build.yml`.
- Existing menu, handler and strings structure: only items are removed and nothing is restructured.
- The current wording in the 7 docs files: edit it in place, and don't rewrite whole sections.

### New Components Required
- `.github/workflows/build.yml`. It replaces the deleted workflow, which is the only CI. The 4 upload-artifact steps are the only new logic, and they replace the Telegram distribution the user asked to drop. `actions/upload-artifact` is the standard first-party action, so no Gradle or library dependency is added.

## Technical Approach
- Do it as one atomic change so `main` always has CI and the app always compiles. The menu item, Kotlin branch and 3 strings must go together, or dangling `R.id.main_tg` or `@string/tg_group` references break the build.
- The APK globs cover the `BlackBox_<version>_<baseName>.apk` naming from `app/build.gradle`. `black-reflection` and `compiler` are `java-library` modules, so their JARs land in `build/libs`.
- Suggested commit subject: `chore: drop Telegram integration, build-only CI with artifact upload`.
- After the merge, run the workflow once with `workflow_dispatch` to confirm the 4 artifacts appear, then delete the secrets (a manual step for the user).

## Implementation Guidance

### Testing Approach
- 2-8 focused checks per step group. No automated tests exist, and K5 says tests are encouraged but not required. Verification runs only the checks below and not a full suite.
  - **App group:** `./gradlew :app:assembleDebug` succeeds, and the overflow menu shows no "Telegram Group" item in any locale.
  - **CI group:**
    - `build.yml` is valid YAML.
    - It has no `TELEGRAM` or `secrets.` references.
    - The 4 artifact names and paths, retention and `if-no-files-found` match R1.
    - After the push, the run on GitHub is green and shows 4 artifacts.
  - **Docs group:** the R12 grep is clean, and the INDEX.md summaries are consistent with ci.md and conventions.md.

### Standards Compliance
- `standards/build/ci.md`: this change rewrites that standard to match the new pipeline. Secrets stay out of the repo.
- `standards/build/gradle.md`: the build command is unchanged, and no dependencies are added (K2 is not affected).
- `standards/global/conventions.md`: meaningful commit subjects and secrets never committed.
- `standards/global/minimal-implementation.md`: nothing is added beyond the replacement workflow. The Telegram-only find steps are removed.
- `standards/frontend/android-resources.md`: the string is removed from all 3 mirrored locales together.
- `standards/docs/release-notes.md`: no entry, by user decision.

## Out of Scope
- The Bcore `SocialMediaAppCrashPrevention.java` and `SimpleCrashFix.java` `org.telegram.messenger` compat logic.
- `.maister/tasks/**` historical artifacts.
- RELEASE_NOTES.md, and README.md and Docs.md (neither has any hits).
- Deleting the GitHub repository secrets (manual, done by the user).
- Adding lint or tests to CI (a separate roadmap item), and changing the upstream GitHub link.

## Success Criteria
- `build_and_telegram.yml` is gone, and `build.yml` matches R1 exactly.
- `./gradlew :app:assembleDebug` passes, and the overflow menu has 4 entries: layout, setting, fake location and the upstream repo link.
- The R12 grep returns only the 2 Bcore compat files.
- The first `build.yml` run on GitHub succeeds and publishes the 4 artifact groups.
