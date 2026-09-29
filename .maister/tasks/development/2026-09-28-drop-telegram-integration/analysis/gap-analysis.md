# Gap Analysis: Drop Telegram Integration

## TL;DR
Telegram appears in 3 places: the only CI workflow, one overflow-menu item, and about 12 doc lines. All of it is present today and all of it has to go. This is a removal-only (subtractive) change. The one additive piece is a build-only `build.yml` that uses `actions/upload-artifact`. The risk is low. The main things to watch are keeping CI working and not touching the Bcore `org.telegram.messenger` compat code. No critical decisions are open. The artifact details (retention, which files to upload, missing-file policy) can use the defaults proposed below.

## Key Decisions
- Characteristics: modifies_existing_code only. Nothing is broken today, there's no data entity, and removing one menu item is too small to count as ui_heavy.
- Change type: modificative. The in-app link and the Telegram distribution go away, and CI keeps building but distributes through GitHub Actions artifacts.
- The `Intent`/`Uri` imports in MainActivity.kt stay, because they're still used at :172-176 and :342-344. Only the `R.id.main_tg` branch (:351-354) is deleted.
- The `aar_paths`/`additional_artifacts` "find" steps go away. `upload-artifact` path globs replace them.
- The two scope questions (CI → build.yml with upload-artifact; also remove the historical upstream mentions) were already decided in clarifications.md, so they aren't raised again here.

## Open Questions / Risks
- **Manual follow-up for the user:** the `TELEGRAM_BOT_TOKEN` and `TELEGRAM_CHAT_ID` repository secrets remain in GitHub (Settings → Secrets and variables → Actions). The user must delete them by hand, since no code change can do it. The Telegram bot/chat itself can also be retired.
- The new workflow can only be verified on GitHub (by the push to main, or a manual dispatch). No local check is possible except YAML syntax review.
- Search precisely rather than with a blanket search/replace. `Bcore/.../SocialMediaAppCrashPrevention.java:31` and `SimpleCrashFix.java:257,:273` must stay untouched.
- Release APKs are debug-signed (app/build.gradle:33). Uploading them as artifacts is no worse than posting them to Telegram, but they are not "real" release builds (a known roadmap debt).

## Summary
- **Risk Level**: Low
- **Estimated Effort**: Low (about 12 files, mostly line deletions and rewording)
- **Detected Characteristics**: modifies_existing_code

## Task Characteristics
- Has reproducible defect: no
- Modifies existing code: yes
- Creates new entities: no (build.yml replaces an existing file; it's not a new capability)
- Involves data operations: no
- UI heavy: no (one overflow-menu item removed)

## Gaps Identified

### Behavioral Changes Needed
| # | Location | Current | Desired |
|---|----------|---------|---------|
| 1 | `.github/workflows/build_and_telegram.yml` (167 lines) | "Build and Send APK to Telegram", job `build-and-send`, `TELEGRAM_*` env, 2 find steps, 6 curl sendDocument+pinChatMessage steps | Deleted, and replaced by `.github/workflows/build.yml`: same triggers (push `main` + `workflow_dispatch`), same setup steps (checkout@v4, Temurin 21 + gradle cache, setup-android@v3, license accept, chmod, same gradle command), then `actions/upload-artifact@v4` steps. No secrets and no env block. |
| 2 | `app/src/main/res/menu/menu_main.xml:26-29` | `main_tg` item, title `@string/tg_group` | Item removed (the menu keeps layout, setting, fake_location, main_git) |
| 3 | `app/.../view/main/MainActivity.kt:351-354` | `R.id.main_tg ->` opens `https://t.me/newblackboxa` | Branch removed |
| 4 | `app/src/main/res/values/strings.xml:12`, `values-zh-rCN/strings.xml:12`, `values-zh-rTW/strings.xml:11` | `tg_group` string | Removed from all 3 locales (keeps the android-resources.md locale mirroring rule) |
| 5 | `.maister/docs/standards/build/ci.md` :4, :7-8 | Pipeline section describes Telegram posting; Secrets section lists TELEGRAM_* | Pipeline: `build.yml` builds everything and uploads it as GitHub Actions artifacts. The Secrets section is rewritten to say the workflow needs no secrets and that secrets are never committed. Delete the section entirely only if nothing is left. |
| 6 | `.maister/docs/standards/global/conventions.md:11` | Commit subject is used as the Telegram caption | Keep the "meaningful commit subjects" rule and drop only the Telegram reason |
| 7 | `.maister/docs/project/tech-stack.md:73,:75` | `build_and_telegram.yml`; uploads to a Telegram chat and pins | `build.yml`; uploads as GitHub Actions workflow artifacts |
| 8 | `.maister/docs/project/architecture.md:99,:107` | "Telegram Bot API: CI artifact distribution only"; APKs "posted to Telegram" | :99 line removed; :107 → "uploaded as GitHub Actions artifacts" |
| 9 | `.maister/docs/project/roadmap.md:6,:26` | "Build and Telegram distribution for all modules"; "CI only builds and uploads to Telegram" | :6 reworded, e.g. "CI builds for all modules" (per the decision, no Telegram mention); :26 → "CI only builds and uploads artifacts" |
| 10 | `.maister/docs/project/vision.md:26` | "...and CI distribution through Telegram" | Changed to "...and CI builds", or the clause dropped |
| 11 | `.maister/docs/INDEX.md:35,:63,:149` | "GitHub Actions to Telegram CI"; Telegram captions; `build_and_telegram.yml` summary incl. Telegram credentials | "GitHub Actions build CI"; drop the caption reason; the CI & Distribution summary describes `build.yml` + upload-artifact and has no secrets |
| 12 | (optional) `RELEASE_NOTES.md` | No entry | Per the release-notes standard, only "significant changes" need an entry. Removing a user-visible menu item and changing CI distribution could go under "Removed Features". See decision I-1. |

Verified clean (no changes needed): README.md, Docs.md, RELEASE_NOTES.md, Gradle files, drawables, settings screens. After the change, a grep over the repo for `telegram|t\.me/|tg_group|main_tg|newblackboxa` (excluding `.git`, `build`, `.maister/tasks`) should match only the 2 Bcore compat files.

### Missing Features / Incomplete Features
None. This is a removal task.

## User Journey Impact Assessment
| Dimension | Current | After | Assessment |
|-----------|---------|-------|------------|
| Reachability | Overflow menu → "Telegram Group" → external upstream chat | Item gone; overflow has 3 entries | Intended. It linked to the upstream community, not to this solo fork. |
| Discoverability | n/a | n/a | No change to the core flows |
| Flow Integration | No in-app flow depends on `main_tg` | Same | Neutral |
| Developer/CI journey | Builds arrive in a Telegram chat, pinned | Builds downloadable from the Actions run page (zip per artifact) | Neutral. Download needs repo access and expires after the retention period. |

## Proposed Defaults for build.yml (no user decision needed)
- **Workflow/job name**: `name: Build`, job `build`.
- **Artifacts** (actions/upload-artifact@v4, one step per group so each can be downloaded separately):
  - `app-debug-apks` → `app/build/outputs/apk/debug/*.apk`
  - `app-release-apks` → `app/build/outputs/apk/release/*.apk` (keep it: it matches today's output, and the debug-signing debt is tracked separately)
  - `bcore-aar` → `Bcore/build/outputs/aar/*.aar` (debug + release)
  - `libs-jar` → `black-reflection/build/libs/*.jar` and `compiler/build/libs/*.jar` (both are `java-library` modules, so the output is JARs)
- **if-no-files-found**: `error` for the APK and AAR groups (failing loudly beats the old silent `echo "not found"`), `warn` for the JARs is also acceptable. Default: `error` everywhere, since the gradle command builds all of them.
- **retention-days**: leave it unset and use the repo default (90 days), or set it to 30 to save storage. Default: `30`.
- The commit-subject captions are dropped. Artifact names don't need the commit message, because the run page already shows the commit.

## Issues Requiring Decisions

### Critical (Must Decide Before Proceeding)
None.

### Important (Should Decide)
1. **I-1 Release-notes entry**: whether to add a RELEASE_NOTES.md "Removed Features" entry (the Telegram menu link, CI moving from Telegram to GitHub artifacts).
   - Options: [A] add a short entry under the next or unreleased version; [B] skip it (maintenance/docs-only change)
   - Default: A
   - Rationale: a user-visible menu item disappears, and conventions.md asks for release notes on significant changes. It's cheap to add.
2. **I-2 Artifact retention/strictness**: accept the defaults above (30 days, `if-no-files-found: error`, release APKs included)?
   - Options: [A] use the defaults; [B] 90 days (repo default); [C] upload the debug APKs only
   - Default: A
   - Rationale: 30 days covers a solo fork's download window without piling up storage. `error` catches a broken build output early.

## Recommendations
- Replace the workflow in the same commit that removes it (git mv + rewrite) so `main` is never without CI.
- Use a conventional commit subject, e.g. `chore: drop Telegram integration, build-only CI with artifact upload`.
- After merge: trigger `workflow_dispatch` once to confirm that the artifacts appear, then delete the `TELEGRAM_BOT_TOKEN`/`TELEGRAM_CHAT_ID` secrets.
- Run `./gradlew :app:assembleDebug` locally to confirm that no dangling `R.id.main_tg`/`R.string.tg_group` references remain (there are 6 references today, all listed above).
- Suggest a standards note (optional): the "commit subject is the CI caption" rationale disappears, so conventions.md stays generic.

## Risk Assessment
- **Complexity Risk**: Low. Deletions plus one straightforward YAML file.
- **Integration Risk**: Low. The `upload-artifact@v4` glob paths must match the real outputs (the APK outputs are named `BlackBox_<version>_<baseName>.apk` per app/build.gradle:58, and a `*.apk` glob covers them).
- **Regression Risk**: Low. No code depends on `main_tg`/`tg_group`. The Bcore compat code is out of scope and must be left alone.
