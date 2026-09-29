# Implementation Plan: Drop Telegram Integration

## TL;DR
4 task groups, 21 steps. Groups 1 (CI workflow), 2 (app menu, handler and strings) and 3 (docs) touch disjoint files and can run in parallel. Group 4 (final sweep and verification) runs last. No unit tests are added (K5). Each group's "tests" are its own checks: a YAML parse and content greps for CI, `./gradlew :app:assembleDebug` for the app, and targeted greps for the docs. The R12 repo-wide grep is the final gate.

## Key Decisions
- **3 parallel groups plus a final verification group.** CI, app and docs share no files, so the executor can run them concurrently. Only the R12 sweep needs all three done.
- **All of R2–R4 go in one group (Group 2).** A dangling `R.id.main_tg` or `@string/tg_group` breaks the build, so the menu item, Kotlin branch and 3 strings are removed together and then built once.
- **The checks replace unit tests.** The project has no tests, and the spec says not to add any. Each group starts by writing down its checks as exact shell commands, then runs them at the end.
- **The YAML check uses `python -c "import yaml; yaml.safe_load(...)"`.** Python 3.13 with PyYAML is available on this machine.
- **Edit the docs in place.** Only the named lines and sentences are reworded. No sections are rewritten (spec: Reusable Components).

## Open Questions / Risks
- **`build.yml` can only be proven on GitHub.** Locally there is only the YAML parse and a content review. The first push or `workflow_dispatch` run must show 4 artifacts (manual follow-up).
- **Manual follow-up:** the user must delete the `TELEGRAM_BOT_TOKEN` and `TELEGRAM_CHAT_ID` repository secrets in GitHub.
- **Blanket-replace hazard:** `Bcore/.../SimpleCrashFix.java` and `SocialMediaAppCrashPrevention.java` contain `telegram` and must stay untouched. Never run a repo-wide search-and-replace.
- **Grep exclusion hazard:** R12 must not exclude directories named `build`, or `.maister/docs/standards/build/ci.md` is hidden. Exclude only `.git`, `.maister/tasks` and module build-output dirs by path.
- **Line endings:** all target files are currently LF, and `.gitattributes` has `* text=auto`. Keep them LF, and don't let editors write CRLF.

## Overview
Total Steps: 21
Task Groups: 4
Expected Tests: none automated. There are 2-6 scripted checks per group (about 16 in total), plus one `:app:assembleDebug` build.

## Implementation Steps

### Task Group 1: CI Workflow
**Dependencies:** None
**Files to Modify:** `.github/workflows/build_and_telegram.yml` (delete), `.github/workflows/build.yml` (create)
**Estimated Steps:** 4

- [x] 1.0 Replace the Telegram workflow with a build-only workflow (R1)
  - [x] 1.1 Define the CI checks (2-6, run in 1.4)
    - `python -c "import yaml,sys; d=yaml.safe_load(open('.github/workflows/build.yml')); print(d['name'], list(d['jobs']))"` parses the file and prints `Build ['build']`.
    - `grep -niE 'telegram|secrets\.' .github/workflows/build.yml` returns nothing.
    - `grep -c 'actions/upload-artifact@v4'` = 4, `grep -c 'retention-days: 30'` = 4, `grep -c 'if-no-files-found: error'` = 4.
    - `test ! -e .github/workflows/build_and_telegram.yml`
  - [x] 1.2 Create `.github/workflows/build.yml`
    - Copy the triggers and build steps from `build_and_telegram.yml` lines 1-37 unchanged, except as noted below:
      - `name: Build`
      - `on: push: branches: [main]` + `workflow_dispatch`
      - job `build`, `runs-on: ubuntu-latest`
    - Drop the `env` block and every `TELEGRAM_*` reference.
    - Steps, in order:
      1. `actions/checkout@v4`
      2. `actions/setup-java@v4` (distribution `temurin`, java-version `21`, `cache: gradle`)
      3. `android-actions/setup-android@v3`
      4. the sdkmanager license-accept command
      5. `chmod +x ./gradlew`
      6. `./gradlew assembleDebug assembleRelease :Bcore:assembleDebug :Bcore:assembleRelease :black-reflection:assemble :compiler:assemble`
    - Do not carry over the `aar_paths` / `additional_artifacts` find steps or the Telegram send/pin steps.
    - Reuse: build steps from `build_and_telegram.yml` lines 1-37.
  - [x] 1.3 Add the 4 `actions/upload-artifact@v4` steps. Each one sets `retention-days: 30` and `if-no-files-found: error`:
    - `app-debug-apks`: `app/build/outputs/apk/debug/*.apk`
    - `app-release-apks`: `app/build/outputs/apk/release/*.apk`
    - `bcore-aars`: `Bcore/build/outputs/aar/*.aar`
    - `libs`: a multi-line `path: |` with `black-reflection/build/libs/*.jar` and `compiler/build/libs/*.jar`
  - [x] 1.4 Delete `.github/workflows/build_and_telegram.yml` (`git rm`), then run only the checks from 1.1.

**Acceptance Criteria:**
- All checks from 1.1 pass.
- `build.yml` matches R1 exactly: name, triggers, the unchanged build steps, and the 4 artifact groups with their paths, retention and `if-no-files-found`.
- The old workflow file is gone.

### Task Group 2: App Overflow Menu
**Dependencies:** None
**Files to Modify:** `app/src/main/res/menu/menu_main.xml`, `app/src/main/java/top/niunaijun/blackboxa/view/main/MainActivity.kt`, `app/src/main/res/values/strings.xml`, `app/src/main/res/values-zh-rCN/strings.xml`, `app/src/main/res/values-zh-rTW/strings.xml`
**Estimated Steps:** 5

- [x] 2.0 Remove the "Telegram Group" menu entry (R2–R4). All removals happen in this one group.
  - [x] 2.1 Define the app checks (run in 2.5)
    - `grep -rnE 'main_tg|tg_group|t\.me/|newblackboxa' app/src` returns nothing.
    - `grep -c '<item' app/src/main/res/menu/menu_main.xml` = 4 (layout, setting, fake_location, main_git).
    - `cd /d/KBlackbox && ./gradlew :app:assembleDebug` succeeds.
  - [x] 2.2 Delete the `main_tg` `<item>` block from `menu_main.xml` (about lines 26-29). Keep the other 4 items and the file structure as they are.
  - [x] 2.3 Delete the `R.id.main_tg -> { ... }` branch (the `https://t.me/newblackboxa` intent) from `onOptionsItemSelected` in `MainActivity.kt` (about lines 351-354).
    - Keep the `Intent` and `Uri` imports. `main_git` still uses them.
  - [x] 2.4 Delete the `tg_group` `<string>` line from all 3 locales:
    - `values/strings.xml` (about :12)
    - `values-zh-rCN/strings.xml` (about :12)
    - `values-zh-rTW/strings.xml` (about :11)
  - [x] 2.5 Run only the checks from 2.1, including `./gradlew :app:assembleDebug`.

**Acceptance Criteria:**
- All checks from 2.1 pass, and the debug build succeeds.
- The overflow menu has exactly 4 entries: layout, setting, fake location and the upstream repo link.
- No other menu items, strings or imports change, and the files stay LF.

### Task Group 3: Project Docs & Standards
**Dependencies:** None
**Files to Modify:** `.maister/docs/standards/build/ci.md`, `.maister/docs/standards/global/conventions.md`, `.maister/docs/project/tech-stack.md`, `.maister/docs/project/architecture.md`, `.maister/docs/project/roadmap.md`, `.maister/docs/project/vision.md`, `.maister/docs/INDEX.md`
**Estimated Steps:** 8

- [x] 3.0 Reword the docs so they describe the build-only CI and have no Telegram mention left (R5–R11)
  - [x] 3.1 Define the docs checks (run in 3.8)
    - `grep -rniE 'telegram|t\.me/' .maister/docs` returns nothing. This is a path-scoped grep, so `standards/build/` is included.
    - `grep -n 'build.yml' .maister/docs/standards/build/ci.md .maister/docs/project/tech-stack.md .maister/docs/INDEX.md` has a hit in each file.
    - `grep -n 'No tests or lint run in CI' .maister/docs/standards/build/ci.md` still matches.
  - [x] 3.2 `standards/build/ci.md` (R5)
    - Rewrite the pipeline section: `build.yml` runs on push to `main` or manual dispatch and builds all APKs/AARs/JARs with JDK 21.
    - The outputs are uploaded as 4 GitHub Actions artifacts (`app-debug-apks`, `app-release-apks`, `bcore-aars`, `libs`) with 30-day retention and can be downloaded from the run page.
    - Keep "No tests or lint run in CI today."
    - Secrets: the workflow needs no secrets, and secrets are never committed. Delete the section only if nothing meaningful remains.
  - [x] 3.3 `standards/global/conventions.md:11` (R6): keep "write meaningful commit subjects" and drop the Telegram-caption reason.
  - [x] 3.4 `project/tech-stack.md` (R7)
    - :73: the workflow becomes `.github/workflows/build.yml`.
    - :75: the outputs are uploaded as GitHub Actions workflow artifacts.
  - [x] 3.5 `project/architecture.md` (R8)
    - Remove the "Telegram Bot API" integration line (about :99).
    - :107: "posted to Telegram" becomes "uploaded as GitHub Actions artifacts".
  - [x] 3.6 `project/roadmap.md` (R9) and `project/vision.md` (R10)
    - roadmap :6: the upstream update mentions only CI builds for all modules.
    - roadmap :26: the text becomes "CI only builds and uploads artifacts".
    - vision :26: "CI distribution through Telegram" becomes "CI builds", or drop the clause.
  - [x] 3.7 `.maister/docs/INDEX.md` (R11)
    - :35: "GitHub Actions to Telegram CI" becomes GitHub Actions build CI with artifact upload.
    - :63: drop the Telegram-caption reason.
    - :149: the CI & Distribution summary describes `build.yml` and upload-artifact and mentions no Telegram credentials.
    - The summaries must match the wording in ci.md and conventions.md.
  - [x] 3.8 Run only the checks from 3.1, then compare INDEX.md :35/:63/:149 against ci.md and conventions.md by eye.

**Acceptance Criteria:**
- All checks from 3.1 pass.
- Each edit is local (a sentence or line reworded), and no sections are rewritten.
- The INDEX.md summaries are consistent with the reworded standards.

### Task Group 4: Final Sweep & Verification
**Dependencies:** 1, 2, 3
**Files to Modify:** None

- [x] 4.0 Verify that the removal is complete across the repo (R12)
  - [x] 4.1 Review the checks from groups 1-3 and confirm each one passed. Re-run any that were not recorded.
  - [x] 4.2 Run the R12 sweep, excluding paths only and never by the bare directory name `build`:
    - `cd /d/KBlackbox && git grep --untracked -niE 'telegram|t\.me/|tg_group|main_tg|newblackboxa' -- . ':!.maister/tasks'`
    - `--untracked` includes the new, unstaged `build.yml`. Ignored files are still skipped, so `.git` and the gitignored module `*/build/` outputs are excluded, while `.maister/docs/standards/build/ci.md` is still searched.
    - Expected hits: only `Bcore/.../SimpleCrashFix.java` and `Bcore/.../SocialMediaAppCrashPrevention.java`.
  - [x] 4.3 Confirm that the Bcore compat files and `.maister/tasks/**` are unchanged (`git status --short` shows only the 14 planned paths).
  - [x] 4.4 Record the manual follow-ups for the user:
    - Run `build.yml` once through `workflow_dispatch` and confirm the 4 artifacts.
    - Delete the `TELEGRAM_BOT_TOKEN` / `TELEGRAM_CHAT_ID` repo secrets.
    - Suggested commit subject: `chore: drop Telegram integration, build-only CI with artifact upload`.

**Acceptance Criteria:**
- The R12 grep matches only the 2 Bcore compat files.
- The diff touches exactly the 14 planned paths: 2 in CI, 5 in the app, 7 in the docs.
- No test files are added, and no dependencies are added.

## Execution Order

1. Group 1: CI Workflow (4 steps). No dependencies, runs in parallel with 2 and 3.
2. Group 2: App Overflow Menu (5 steps). No dependencies, runs in parallel with 1 and 3.
3. Group 3: Project Docs & Standards (8 steps). No dependencies, runs in parallel with 1 and 2.
4. Group 4: Final Sweep & Verification (4 steps). Depends on 1, 2 and 3.

## Standards Compliance

Follow the standards in `.maister/docs/standards/`:
- `global/minimal-implementation.md`: nothing is added beyond the replacement workflow, and the Telegram-only find steps are removed.
- `global/conventions.md`: meaningful commit subjects, secrets never committed, LF line endings, no new dependencies (K2).
- `build/ci.md`: rewritten by this change to match the new pipeline.
- `build/gradle.md`: the build command is unchanged.
- `frontend/android-resources.md`: the string is removed from all 3 mirrored locales together.
- `docs/release-notes.md`: no RELEASE_NOTES entry, by user decision.

## Notes

- **Checks, not tests:** each group writes its checks first (N.1) and runs only those at the end. There is no full test suite, and none exists (K5).
- **Run incrementally:** only Group 2 runs a Gradle build (`:app:assembleDebug`).
- **Mark progress:** check off each step as it is completed.
- **Don't touch:** `Bcore/**`, `.maister/tasks/**`, RELEASE_NOTES.md, README.md, Docs.md.
