# Docs findings: planned features, open items, stale claims

## TL;DR
- Docs name only four explicit feature ideas: backup/export, per-app spoofing profiles in the UI, permission onboarding, tablet/landscape layouts. All are undone. The other open roadmap items are engineering (Compose evaluation, tests, UI error handling, release signing), not features.
- All GUI roadmap items are ticked. The vision goal "better UX for core flows" is marked done for the main screen and settings only; fake location, GMS and install flows have no stated follow-up.
- Stale claims are the main hazard: roadmap and vision still list "Xposed-style module list" and "VPN toggle" as features, but both were removed (`.maister/docs/standards/engine/runtime.md:6-10`). README and Docs.md claim platforms and APIs that the standards call stale or aspirational.
- Earlier tasks deferred few ideas: Bcore LauncherActivity dark-mode white flash, osmdroid dark tiles, dynamic-color toggle, edge-to-edge and nav-bar coloring; manual QA of the M3 task was never executed.

## Key Decisions (constraints from docs)
- Do NOT reintroduce Xposed, host VPN mode or crash/log upload (`.maister/docs/standards/engine/runtime.md:6-10`). Telegram is removed (drop-telegram task).
- targetSdk stays 28; raising it is an engine decision (`.maister/docs/project/roadmap.md:33`). Engine code changes only when a UI feature needs them (`.maister/docs/project/vision.md:60`).
- No speculative stubs or abstractions (`.maister/docs/standards/global/minimal-implementation.md:12-13`); UI reaches the engine only via `data/` repositories (`.maister/docs/project/architecture.md` "UI Development Notes").
- Tablet variants use `-land` / `-sw600dp` qualifiers (`.maister/docs/standards/frontend/responsive.md:36`); `values-w600dp` already exists in `app/src/main/res`.

## Open Questions / Risks
- Whether README "Key Features" (Device Spoofing, Fake Location) work end to end cannot be confirmed from docs; code gatherers must verify.
- Docs.md API listings are flagged as partly non-existent (`.maister/docs/standards/docs/release-notes.md:12`, `.maister/docs/standards/engine/runtime.md:16`). Do not treat them as capability evidence.
- Manual QA of the M3 / dark-mode task (16 rows, Light/Dark/Dynamic) was never executed (`.maister/tasks/development/2026-09-27-material3-dark-mode/verification/manual-qa-checklist.md:8`); UI polish bugs may remain.
- The INDEX.md copy loaded in some contexts still mentions "VPN toggle"; the file on disk (`runtime.md`) says it was removed. Trust runtime.md and code.

---

## A. Candidate features (stated or implied) with status

| # | Candidate | Source | Status |
|---|-----------|--------|--------|
| 1 | App / space import-export and backup | `roadmap.md:32` "Feature ideas" | NOT DONE. Partial hook: `DataDirCopier` util (per-app data copy used by duplicate-user) |
| 2 | Per-app spoofing profiles in the UI | `roadmap.md:32` | NOT DONE. Engine spoofing exists (`vision.md:51`, `architecture.md` native layer "device spoofing") |
| 3 | Onboarding flow for required permissions (All Files Access; VPN no longer applies) | `roadmap.md:32` | NOT DONE. Storage dialog texts were only moved to strings (`roadmap.md:14`) |
| 4 | Tablet / landscape layouts | `roadmap.md:32`; `responsive.md:36` | NOT DONE (`values-w600dp` exists, content unverified) |
| 5 | Jetpack Compose for new screens | `roadmap.md:22` | OPEN checkbox (evaluation, not a user feature) |
| 6 | ViewModel unit tests + Espresso smoke tests | `roadmap.md:25` | OPEN (first JVM tests exist) |
| 7 | Consistent UI error surfacing (MainActivity try/catch) | `roadmap.md:28`; K1 in `INDEX.md:8` | OPEN (partly done in commits 4fba33e, 3fa9348) |
| 8 | Proper release signing | `roadmap.md:29` | OPEN; prerequisite for a public release |
| 9 | Polish translation ("optionally") | `roadmap.md:14` | Ticked, but only `values`, `values-zh-rCN`, `values-zh-rTW` exist: no Polish. strings.xml has 103 entries vs 101 in each zh file (2 untranslated) |
| 10 | Fix Bcore LauncherActivity white flash in dark mode | M3 `analysis/clarifications.md:10`; `implementation/spec.md:222-224`; QA checklist row 14 | DEFERRED ("separate task"), not started |
| 11 | Dark tiles for osmdroid fake-location map | M3 `implementation/spec.md` Out of Scope; requirements Q&A "Tiles stay light" | DEFERRED |
| 12 | Dynamic-color on/off toggle | M3 `implementation/spec.md` Out of Scope; `analysis/gap-analysis.md:168` option (B) | DEFERRED |
| 13 | Edge-to-edge, navigation-bar coloring, core-splashscreen | M3 `implementation/spec.md` Out of Scope | DEFERRED |
| 14 | Theming of rocker / floating joystick overlay (drawn in guest processes) | M3 `implementation/spec.md` Out of Scope | DEFERRED |
| 15 | Delete dead Xposed UI (activity_xp, item_xp, XpModuleInfo, xp strings) | `runtime.md:10`; M3 `analysis/scope-clarifications.md:10` | OPEN cleanup, not a feature |
| 16 | Move `JarManagerTest.java` out of Bcore `src/main` | `roadmap.md:25` | OPEN cleanup |
| 17 | Lint for Bcore (`checkReleaseBuilds false`) | `roadmap.md:27` | OPEN |
| 18 | `viewModel {}` initializers or Hilt/Koin if screen count grows | `roadmap.md:21` | OPEN, conditional |
| 19 | Track Android 16+ hidden-API changes affecting Bcore | `roadmap.md:33` | ONGOING risk |
| 20 | Docs.md "Future Features": enhanced anti-detection, performance, more Android versions, more service proxies | `Docs.md:504` | Generic upstream text, no concrete plan |
| 21 | Point the upstream repo link to this repo | drop-telegram `codebase-analysis.md:163`; commit 0f267db | DONE |
| 22 | Manual QA of M3 / dark mode, TalkBack check | QA checklist `:8` | NOT EXECUTED |
| 23 | Delete GitHub `TELEGRAM_*` secrets; first `build.yml` run | drop-telegram `implementation/implementation-plan.md:15`, `work-log.md:60` | Manual user follow-up |

## B. Implied by vision-level goals (no explicit roadmap item)
- "Improve the UX of the core flows: installing apps, app grid and multi-user pages, per-app actions, settings, fake location and GMS management" (`vision.md:58`). Only grid/main and settings carry done marks (`roadmap.md:13,15`). No follow-up is listed for fake location (saved places, routes), GMS management, or install flow.
- "Run several instances of the same app, or run untrusted apps, in isolated per-user spaces" (`vision.md:50`): implies per-space controls (permissions, network, spoofing per space). None are listed.
- "Spoof device identity (Android ID, device IDs, location)" (`vision.md:51`): implies a spoofing-profile UI (same as #2).
- "Offer a reusable engine (Bcore) for other host apps" (`vision.md:53`): implies accurate API docs (Docs.md is stale); not a user feature.
- "Polished, modern user-facing app" (`vision.md:63`): implies onboarding, release signing (#8) and proper README before going public.

## C. Claims in docs vs reality (stale claims)

| Claim | Where | Reality |
|-------|-------|---------|
| "Xposed-style module list/hosting" is a key feature | `roadmap.md:5`; `vision.md:15` | REMOVED (`RELEASE_NOTES.md:123-126`; `runtime.md:9-10`). Stale in roadmap and vision |
| "VPN toggle" / "VPN Network Mode" | `roadmap.md:5,6`; `vision.md:26`; `RELEASE_NOTES.md:62-69,91-99` | REMOVED (`runtime.md:6-7`); no "vpn" match in `app/src/main`. RELEASE_NOTES still documents it with no Removed note |
| Android 5.0 to 14.0+, ARMv7/ARM64/x86 | `README.md:7,26` | Stale: ARM only, tested Android 10-15 (`INDEX.md` K4) |
| JDK 17, SDK 34+, clone URL `your-repo/NewBlackbox` | `README.md:32-40` | Stale: JDK 21, compileSdk 35 (`standards/build/gradle.md`) |
| "No Root Required" vs "Root access (recommended)", Android 8.0+ (API 26) | `README.md:17` vs `Docs.md:33-34` | Contradictory and stale; minSdk 21, no root |
| Credits "Main Developer: ALEX502", title "NewBlackbox" | `README.md:96`; `RELEASE_NOTES.md:1` | Upstream naming, not APKEnclave |
| Docs.md APIs: `clearAppData(pkg,user,"cache")`, `setAppPermission`, `optimizeMemory`, `startVirtualProcess`, `UIDSpoofingHelper`, `GoogleAccountManagerProxy.addAccount`, PWA service-worker isolation | `Docs.md:107-140, 283-325, 430-460` | Partly non-existent per `release-notes.md:12` and `runtime.md:16`; verify against Bcore |
| "Join BlackBox user forums", "official source" | `Docs.md` Support section | No such channel for this fork |
| "Device Spoofing" and "Fake Location" as key features | `README.md:19-20` | Fake location has UI; device spoofing has no dedicated UI (see #2) |
| Only known issue: Oppo thermal log noise (harmless) | `RELEASE_NOTES.md:139-146` | Sole documented known issue |
| Roadmap ticks string externalization incl. optional Polish | `roadmap.md:14` | No Polish locale exists |
| tech-stack says CI "runs no tests and no lint" | `tech-stack.md:76` vs `roadmap.md:26` | Internal conflict; tech-stack is stale |

## D. "Not yet" / known-issue statements
- No ViewModel or Espresso tests (`roadmap.md:25`); CI now runs lint and unit tests (`roadmap.md:26`).
- Release builds are debug-signed (`roadmap.md:29`).
- About 18 legacy direct engine calls remain in AppsFragment, MainActivity, SettingFragment, FakeManagerActivity, ShortcutActivity (`standards/frontend/android-architecture.md:13`): refactor candidate.
- About 15 legacy hard-coded literals and 4 missing zh keys (`standards/frontend/android-resources.md:25`); possibly stale after the externalization commit 202c30b.
- Dark-mode white flash when a guest app launches (QA checklist row 14; M3 `clarifications.md:10`).
- Guest apps are not themed (`RELEASE_NOTES.md:15`).
- Engine-affecting settings need an app restart (`android-resources.md` Settings screen): UX friction, a "restart now" action would help.
- 27 legacy no-op hook stubs (`standards/engine/hooks.md:17`): engine debt.

## E. Release-history context
- Device-info logging header in logcat (`RELEASE_NOTES.md:78-86`). Logs stay on device and log upload was removed (`runtime.md:7`), so no in-app log viewer/share exists: a local "export diagnostics" feature is a gap candidate that must not reintroduce upload.
- Anti-detection native hooks (`RELEASE_NOTES.md:132-135`), Android 10 black-screen fix, Android 14/15 fixes (`roadmap.md:6`).
- GUI refresh 2026-09-27: M3, dark mode, theme preference (`RELEASE_NOTES.md:3-24`). RELEASE_NOTES has no entry for the 2026-09-30 main-screen, settings and StateFlow work.

## F. Observations for synthesis
1. Explicit documented features are few; the candidate list must come mainly from the code gap analysis and external research. Doc-backed top picks: backup/export (#1), spoofing-profile UI (#2), permission onboarding (#3), tablet layouts (#4).
2. Low-risk UI items already named in docs: dark osmdroid tiles (#11), dynamic-color toggle (#12), edge-to-edge (#13), Polish locale (#9), diagnostics export (section E).
3. Doc hygiene is a prerequisite for going public: rewrite README and Docs.md for APKEnclave, fix stale roadmap/vision lines (Xposed, VPN), add a release-notes entry for 2026-09-30.
