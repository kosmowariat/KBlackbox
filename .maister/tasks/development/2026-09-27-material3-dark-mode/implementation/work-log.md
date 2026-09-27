# Work Log

## 2026-09-27T09:09:57Z - Implementation Started

**Total Steps**: 46 (+6 parent items)
**Task Groups**: 1 Theme Foundation → 2 Theme-Mode Preference & Startup → 3 Surface Toolbar & Main Screen → 4 List Screens & GMS → 5 Cleanup & Docs → 6 Final Build & Manual QA (linear, sequential waves of 1)
**Toolchain**: JDK 21 (D:/Android/jdk-21), Android SDK (D:/Android/Sdk); baseline `:app:assembleDebug` green.
**Testing**: no new automated tests (user decision K5); per-group verification = `./gradlew :app:assembleDebug` + owned manual QA rows.

## Standards Reading Log

### Loaded Per Group
(Entries added as groups execute)
**Revision (user decision)**: primary palette regenerated at chroma 48 — day primary #395BA9, night primary #B1C5FF / primary_container #1D438F; all contrast ≥5.02:1; build green. Spec Palette section to note chroma 48 (Group 5 docs).

### Group 2: Theme-Mode Preference and Startup
**From Implementation Plan**: frontend/android-resources.md, frontend/android-architecture.md, global/kotlin-style.md, global/error-handling.md, global/minimal-implementation.md, global/commenting.md, frontend/accessibility.md
**From INDEX.md**: global/coding-style.md
**Discovered During Execution**: none

## 2026-09-27T09:20:27Z - Group 2 Complete

**Steps**: 2.0–2.7 completed
**Build**: :app:assembleDebug BUILD SUCCESSFUL
**Manual QA**: rows 10, 11, 14, 16 + adb run-as default-prefs check pending user QA
**Files Modified**: BlackBoxLoader.kt (mThemeMode delegate, themeMode/invalidThemeMode/themeNightMode), App.kt (applyThemeMode gated by isMainProcess; DynamicColors precondition 'top.niunaijun.blackboxa.'), SettingFragment.kt (initThemeMode), xml/setting.xml (Appearance category + ListPreference theme_mode persistent=false), strings en/zh-rCN/zh-rTW
**Notes**: isMainProcess valid in onCreate (mProcessType set in BlackBoxCore.doAttachBaseContext). If doAttachBaseContext throws early, theme init is skipped (falls back to system default, no crash). New calls sit inside App.onCreate's existing try.

### Group 3: Surface Toolbar and Main Screen
**From Implementation Plan**: frontend/android-resources.md, frontend/android-views.md, frontend/accessibility.md, global/kotlin-style.md, global/minimal-implementation.md, global/commenting.md
**From INDEX.md**: global/coding-style.md
**Discovered During Execution**: none

## 2026-09-27T09:23:03Z - Group 3 Complete

**Steps**: 3.0–3.10 completed
**Build**: :app:assembleDebug BUILD SUCCESSFUL; greps clean (no hex/@color/primary/white/primary_text in touched files); ToolBarColorStyle removed
**Manual QA**: rows 2, 3, 4, 5, 6, 15 pending user QA
**Files Modified**: values/themes.xml (ThemeOverlay.BlackBox.Toolbar), view_toolbar.xml (Widget.Material3.Toolbar.Surface), ic_search.xml, ic_add.xml, activity_main.xml (dots ?attr/colorPrimary, FAB contentDescription, dimens), item_app.xml (badge tertiaryContainer, LabelMedium), AppsAdapter.kt (MaterialColors placeholder), MainActivity.kt (subtitle TextView lookup), drawable-night/ic_empty.xml (new)
**Notes**: CornerLabelView/dotsindicator attrs are format=color read via obtainStyledAttributes → ?attr/ works; no fallback used. Badge text = onTertiaryContainer (spec) vs mockup --tertiary.

### Group 4: List Screens and GMS Manager
**From Implementation Plan**: frontend/android-resources.md, frontend/accessibility.md, global/kotlin-style.md, global/minimal-implementation.md, global/commenting.md
**From INDEX.md**: frontend/components.md, frontend/css.md
**Discovered During Execution**: none

## 2026-09-27T09:25:44Z - Group 4 Complete

**Steps**: 4.0–4.7 completed
**Build**: :app:assembleDebug BUILD SUCCESSFUL; greps clean in 4 layouts
**Manual QA**: rows 8, 9, 12, 15 pending user QA
**Files Modified**: activity_list.xml (SimpleSearchView tokens, alpha 1.0, inline 28dp surfaceContainerHigh card), item_package.xml, item_fake.xml (re-indented), item_gms.xml (MaterialSwitch), GmsManagerActivity.kt (MaterialSwitch types)
**Notes**: SimpleSearchView reads attrs via obtainStyledAttributes → ?attr/ works. Cursor color set by library reflection may be no-op on newer Android (check QA row 8). GMS row summary line from mockup not added (not in spec).

### Group 5: Cleanup and Documentation
**From Implementation Plan**: docs/release-notes.md, global/conventions.md, global/minimal-implementation.md, frontend/android-resources.md
**From INDEX.md**: (structure-preservation note)
**Discovered During Execution**: none

## 2026-09-27T09:27:34Z - Group 5 Complete

**Steps**: 5.0–5.5 completed
**Build**: :app:assembleDebug BUILD SUCCESSFUL; all 5.2 success-criteria greps pass (no forceDarkAllowed; no hex/obsolete colors in touched files; 34 md_theme_* names identical day/night; Bcore, libs.versions.toml, app/build.gradle unchanged)
**Files Modified**: values/colors.xml (white removed), RELEASE_NOTES.md (2026-09-27 entry), roadmap.md, tech-stack.md, architecture.md, INDEX.md, implementation/spec.md (chroma-48 note)

### Group 6: Final Build and Manual QA
**From Implementation Plan**: global/minimal-implementation.md
**Discovered During Execution**: none

## 2026-09-27T09:38:50Z - Group 6 Partially Complete

**Steps**: 6.1 full `./gradlew assembleDebug` BUILD SUCCESSFUL (all modules incl. Bcore native; APKs: BlackBox_4.0.0_{arm64-v8a,armeabi-v7a,universal}-debug.apk ~10 MB); 6.2 verification/manual-qa-checklist.md written (16 rows × Light/Dark/Dynamic, empty).
**Deferred**: 6.3–6.5 — require the user to run QA on a device (ARM only; no x86 APK).

## 2026-09-27T09:38:50Z - Implementation Complete (pending manual QA)

**Total Steps**: 43 completed, 3 deferred to user QA (6.3–6.5)
**Standards**: logged per group above
**Test Suite**: no automated tests exist (team decision K5); full build green

## 2026-09-27T17:35:11Z - Manual QA confirmed by user

User reported device QA done ("QA narazie done") on Realme RMX3363 (Android 13). During QA the user found "adding apps shows Empty list" — a pre-existing bug fixed separately (quick-bugfix, commit 3954010 on fix/installed-apps-list). Steps 6.3–6.5 marked user-confirmed.
