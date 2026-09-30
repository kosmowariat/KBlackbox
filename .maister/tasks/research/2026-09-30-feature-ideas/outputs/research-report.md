# Research Report: Feature ideas for APKEnclave

**Research type**: mixed (codebase gap analysis + competitor matrix + platform enablers + docs/roadmap + policy risk tagging)
**Date**: 2026-09-30
**Researcher**: research-synthesizer (inputs: 6 gatherer findings)

Finding-file tags used for citations: **[app]** = `codebase-app-gaps.md`, **[engine]** = `codebase-engine-gap-analysis.md`, **[docs]** = `docs-roadmap-and-claims.md`, **[comp]** = `external-competitors-feature-matrix.md`, **[plat]** = `external-platform-enablers.md`, **[policy]** = `external-policy-google-play.md`. Paths are relative to `D:\KBlackbox\app\src\main\java\top\niunaijun\blackboxa\` (abbrev. `J/`) for the app and `Bcore/src/main/java/...` for the engine unless stated.

## TL;DR
- The engine already offers far more than the app exposes: the app calls about 25 engine entry points [engine A]. Ten strong features can be built **UI-only, with no Bcore change**.
- Best externally validated feature is a **space/app biometric lock** (appears in 4 of 7 competitor families [comp]). It is cheap (`BiometricPrompt`, one new catalog dependency) but must also gate `ShortcutActivity`, which is currently exported and unvalidated [app F2].
- Recommended first batch (all UI-only, low risk, verifiable on the single Realme RMX3363 / Android 13): housekeeping + Polish locale, launcher shortcuts with `ShortcutActivity` hardening, permissions/health screen with restart button, running-apps indicator, then the biometric lock.
- Do not build from `Docs.md`: most of its API list does not exist in `Bcore` [engine D]. Do not add notification listener, accessibility, Xposed, host VPN or log upload.
- Play policy is largely moot for now: `targetSdk 28` blocks a Play release under the current target-API rule [plat 12, policy], and two existing features (disable FLAG_SECURE, hide root) are high-risk on Play. Tag features now, decide build split later.

## Key Decisions
- Start with UI-only features; Bcore work (notification mute, per-app permission deny-list, spoof profiles, block network) waits until a UI feature proves it needs it — matches `vision.md:60` [docs].
- Harden `ShortcutActivity` **before** shipping the lock, otherwise shortcuts bypass it [app F2, plat 5].
- Treat "sideload/GitHub build" as the default target; plan a Play build only if the project decides to publish and solves `targetSdk` first [policy, plat 12].
- Backup uses SAF (`CreateDocument`/`OpenDocument`), never All-Files-Access or Auto Backup [plat 7, policy].

## Open Questions / Risks
- Nothing was run on a device: "exists" means a code path exists, not that it works on Android 13 or other versions [engine, app].
- Lock is a UI gate, not encryption; launch paths that bypass host UI (notification taps, older pinned shortcuts, daemon-started processes) are unverified [plat 5, comp candidate 1].
- `isRunningApplication` depends on reflection over private `mTasks` and a task-affinity match; service-only apps may be misreported [engine Open Questions].
- Play target-API rule (36 for new apps/updates from 2026-08-31, extension to 2026-11-01) comes from a summarised fetch; verify on the page before relying on it [plat Open Questions].
- Competitor evidence is mostly search-result snippets (Medium confidence); complaint themes come from aggregator articles [comp].
- Brief says launcher icon variants exist, manifest has no `activity-alias` (see Section 8).

## Table of Contents
1. Ranked top-10 features
2. Recommended first batch and sequencing
3. Engine-vs-UI gap table
4. Competitor matrix
5. Platform enablers and targetSdk 28 constraints
6. Risk view: sideload vs Play, ideas to avoid
7. Housekeeping and stale claims
8. Contradictions and confidence
9. Methodology, conclusions and appendices

---

## 1. Ranked top-10 features

Order: value/effort, UI-only first (all ten need no Bcore change). Effort: S (about a day or two), M (several days), L (weeks). Risk tag = Google Play policy risk per [policy] / [plat]; "n/a" where no policy surface.

| # | Feature | What it is | Value | Effort | Bcore change? | Play risk | Evidence | Existing hook in code |
|---|---|---|---|---|---|---|---|---|
| 1 | Biometric lock (host, per space, optional per app) with auto-lock timer | `BiometricPrompt` gate on `MainActivity`/`UserAppsActivity`; also on `ShortcutActivity`. Lock timer after background | High (top externally validated feature) | S-M | N (UI gate; hard launch gate would need an engine hook) | low | [comp F1,F2,F7,F8]; [plat 5]; [app F5] (no `BiometricPrompt` anywhere); [engine C: lock S UI-only] | `ShortcutActivity.kt:10-23`, `WelcomeActivity.kt:13-33`, `UserAppsActivity.kt`; new dep in `gradle/libs.versions.toml` |
| 2 | Launcher shortcuts (dynamic/static), cleanup, and `ShortcutActivity` hardening | Long-press launcher entries (recent apps, Add app, Fake location), remove stale pinned shortcuts on uninstall, validate extras, add space badge to label | Medium (plus closes a security gap) | S | N | low | [app F2]; [plat 1]; [plat summary] | `ShortcutUtil.kt:22-53` (pinned), `ShortcutActivity.kt:10-23`, manifest `AndroidManifest.xml:59-62` (exported, no permission) |
| 3 | Polish locale, per-app language, string cleanup | `values-pl`, `locales_config.xml` + `android:localeConfig`, language row in Settings, fix "BlackBox" mentions and "Launched Failed" typos | Medium (developer is Polish; roadmap ticks "optional Polish" but it does not exist) | S (plus translation) | N | none | [app F11]; [plat 4]; [docs A#9] | `values-zh-rCN`/`zh-rTW` as templates; `res/xml/setting.xml`; AppCompat 1.7.0 already present |
| 4 | Permissions and health screen + "Restart engine" button | Checklist with live status (All Files Access, battery-optimisation exemption for daemon, pinned-shortcut permission, notifications on 13+); wire the dead storage callback; restart action instead of toast | Medium-high (roadmap: onboarding; removes "restart to apply" friction) | S-M | N | med (All Files Access is restricted on Play) | [app F4,F5]; [engine B4]; [docs A#3, D] | `MainActivity.kt:96-112`, `BlackBoxLoader.kt:156-163` (broadcast with no receiver), `BlackBoxCore.java:1030-1075`, `SettingFragment.kt:88` |
| 5 | Running indicator, "Stop all", simple task manager | "Running" badge on tiles, stop all in a space, per-space process list with kill | Medium-high (battery/notification reliability is top complaint) | S-M | N for badge and stop-all; small client wrapper for process list | low | [engine B1,B2]; [comp F10, candidate 6]; [policy: low] | `BlackBoxCore.java:1570` `isRunningApplication`; `BActivityManager.java:378,387`; `BProcessManagerService.java:230-263`; `AppsFragment.kt:252` (`stopPackage`) |
| 6 | Fake-location upgrade | Space picker (currently hard-coded user 0), favourites/history, coordinate input, per-app "disable", optional place search | Medium | S-M | N (presets/global/disable); cell APIs suspected buggy, avoid | med | [app F6]; [engine B3]; [comp F3 candidate 9] | `MainActivity.kt:279`, `FakeLocationRepository.kt:15-45`, `FollowMyLocationOverlay.kt:55,106-122`, `BLocationManager.java:32-166` |
| 7 | Backup/export and import of a space or app | Zip of app/space data to a SAF document, import into a new space; stop app first | High (roadmap item #1; Secure Folder lost backup, a gap) | M | N (host-side zip of `BEnvironment` dirs) | med (low if strictly SAF) | [docs A#1]; [engine C]; [plat 7]; [app F8] | `DataDirCopier.kt:8-22` (+ `DataDirCopierTest`), `AppsRepository.kt:355-426`, `BEnvironment.java:87-96,130,138` |
| 8 | App grid upgrade: details sheet, clone to another space, search/sort/favourites | Bottom sheet with version/size/install date/actions; "copy to space" menu item; search, sort (name/recent), favourites; last-used via lifecycle callback | Medium | S-M | N | low | [app F1,F9]; [engine B11] | `AppsFragment.kt:189-207,47`, `AppInfo.kt:5`, `AppsRepository.kt:396,413` (`installCopyForUser`, `copyAppData`), `ListActivity.kt` filter pattern |
| 9 | In-app About + diagnostics (local log export) | Version, engine ABI/SDK, share `logcat`/`Slog` zip via SAF/share sheet; no upload | Medium (release-readiness, support) | S | N | low | [app F5]; [engine B16]; [docs E] (upload removed, local export is a gap) | `SettingFragment.kt`, engine `utils/Slog`, `RELEASE_NOTES.md:78-86` device header |
| 10 | Freeze/auto-stop idle apps or spaces + unfreeze shortcut | Stop apps after idle timeout, "freeze" state with shortcut to start | Medium | S-M | N (reuses stop); hide/freeze state needs a small Bcore setter | low | [comp F4,F5,F6, candidate 2]; [engine B9] (`hidden` has no setter) | `AppsFragment.kt:252`, `ShortcutUtil.kt`, `BPackageUserState.java:8-12` |

### Next tier (not in top 10)

| Feature | Effort | Bcore? | Play risk | Why later | Evidence |
|---|---|---|---|---|---|
| Launcher icon / label disguise via `activity-alias` | S | N | low (disguise = higher) | Art exists but is not wired; stealth semantics | [plat 14], [app Open Questions] |
| Tablet/landscape two-pane + adaptive grid span + edge-to-edge | M | N | none | Roadmap item; medium value | [app F10], [plat 9,10], [docs #4,#13] |
| Share/"Open with" into a space, URL install, multi-select add | M | maybe | med (APK handler) | Competitors support "open in clone" (unconfirmed) | [plat 8], [app F7], [policy] |
| Per-space notification mute/clear | M | Y (`INotificationManagerProxy`) | unknown | Top complaint area, needs engine config lookup | [engine B5], [comp F7,F8,F10] |
| Per-app `hideRoot` / `disableFlagSecure` | S-M | Y (small) | high | Hooks read global flags | [engine B15], [policy] |
| QS tile (fake location toggle), home widget | M | maybe | low | Doc guidance discourages launch-only tiles; overlaps shortcuts | [plat 2,3] |
| Jobs viewer/cancel | S-M | N | low/med | Niche | [engine B6] |
| Per-app permission deny-list, "block network" | M-L | Y | med | Engine risk (apps crash on deny) | [engine C] |
| Per-app spoof profile UI (IMEI, Build.*) | L | Y | med-high | Needs per-(user,pkg) store and Build rewriting | [engine C], [docs #2] |
| Split-APK / XAPK import | L | Y | med-high | No split handling in engine | [engine C] |
| Clipboard isolation between spaces | M | Y | low | No clipboard hook | [engine C] |

---

## 2. Recommended first batch (UI-only, low risk, verifiable on one phone)

Every item can be verified on the Realme RMX3363 (Android 13) by installing one guest app, because none depends on another Android version. Items 1-4 are also the lowest-risk.

| Step | Work | Size | Verify on phone |
|---|---|---|---|
| 0 | Housekeeping pass (Section 7): delete dead strings/layouts/class, fix "BlackBox" text and typos, rename "Fake Location(Preview)" if ready | S | Build, lint, unit tests pass; screens unchanged |
| 1 | Polish locale + `localeConfig` + language row (#3) | S | Switch language in Settings, check recreated screens |
| 2 | `ShortcutActivity` hardening, then dynamic/static shortcuts + stale-shortcut cleanup (#2) | S | Long-press launcher icon; pin then uninstall app; call the exported activity from `adb shell am start` with bad extras |
| 3 | Permissions/health screen + restart button + wire storage callback (#4) | S-M | Revoke All Files Access, see status change; change an engine setting and press restart |
| 4 | Running indicator + stop-all (#5), process list only if the badge proves reliable | S-M | Launch guest app, background it, check badge; service-only guest apps are the known weak spot |
| 5 | Biometric lock + timer, gating `MainActivity`, `UserAppsActivity`, `ShortcutActivity` (#1) | S-M | Lock, background for timer, try launching from pinned shortcut, `adb` intent and recents |
| 6 | Fake-location upgrade (#6) and app grid upgrade (#8) | S-M each | Place favourite, apply to space 1 app; clone app to a second space |

Sequencing rationale: step 2 before step 5 because the lock has to close the shortcut bypass [plat 5]; step 3 before 4 because the restart button reduces test friction; steps 6+ (and #7 backup) follow once the first five are stable. Backup (#7) is the biggest roadmap-named win but needs stopped-app handling and progress UI, so schedule it as the first "M" after the batch. Android 10/11/12/14/15 behaviour is unverifiable with one phone [engine, app Open Questions].

---

## 3. Engine-vs-UI gap table (exists in Bcore, no UI)

Condensed from [engine B/C]. "Bcore?" = engine change needed to ship the UI.

| Engine capability | Defined | UI today | Idea | Bcore? | Risk |
|---|---|---|---|---|---|
| `isRunningApplication(pkg,userId)` | `BlackBoxCore.java:1570` | none | Running badge, stop-all | N (reflection on `mTasks`) | Low/Med |
| Process list/kill (`getRunningAppProcesses`, `killPackageAsUser`) | `BActivityManager.java:378,387`; `BProcessManagerService.java:230-263` | only `stopPackage` | Task manager | small wrapper | Med |
| Fake-location extras: global, cell, `disableFakeLocation`, `isFakeLocationEnable` | `BLocationManager.java:32-166` | only per-app set/get + pattern | Favourites, per-space default, disable toggle | N (cell API suspected buggy: `BLocationManagerService.java:96-98`) | Low/Med |
| Storage helpers `hasAllFilesAccess` etc. | `BlackBoxCore.java:1030-1075` | app reimplements | Permissions checklist | N | Low (MANAGE_EXTERNAL_STORAGE Play-sensitive) |
| Notification service | `BNotificationManager.java:19-100` | none | Mute/clear | Y for mute | Med |
| `BJobManager` | `BJobManager.java:24-50` | none | Jobs viewer | N | Low/Med |
| `BAccountManager` (30+ methods, `copyAccountToUser`) | `BAccountManager.java:19-327` | none | Accounts per space | N | Med/High (credentials) |
| `BPackageUserState{installed,stopped,hidden}` | `BPackageUserState.java:8-12` | none | Hide/freeze, install date | `hidden` has no setter | Low/Med |
| `AppLifecycleCallback` | `AppLifecycleCallback.java:15-23` | only `onStoragePermissionNeeded` | Launch history, last-used | N | Low |
| Engine logging `Slog` | `utils/Slog` | none | Log viewer/export | N | Low |
| `hideRoot`, `disableFlagSecure` | `BlackBoxCore.java:1240-1245` | global toggles | Per-app/per-space | Y (S) | Low/Med |
| Shortcut / AppWidget hooks | `IShortcutManagerProxy.java:65-101`; `IAppWidgetManagerProxy.java:14-25` | host pinning only | Guest shortcuts/widgets | Y (M / L) | Med / High |
| `ILocaleManagerProxy` | `fake/service/ILocaleManagerProxy.java` | none | Per-guest language | probably | Med |

Does **not** exist in the engine [engine C]: per-app permission control (blanket grants in `IAppOpsManagerProxy.java:112-202`, `IPackageManagerProxy.java:411-455`), per-app device profile (IMEI = `md5(hostPkg)`, fixed Wi-Fi identifiers `IWifiManagerProxy.java:40-47`), per-space VPN/proxy, split-APK install, clipboard hook, hide/freeze setter.

---

## 4. Competitor matrix (condensed)

Y = documented, ? = not verified. From [comp]; most rows are snippet-level (Medium confidence) except Island, Shelter (README) and Private Space (High).

| Feature | Parallel Space | Dual Space | App Cloner | Island/Insular | Shelter | Secure Folder | Private Space | APKEnclave today |
|---|---|---|---|---|---|---|---|---|
| Space lock | Y | Y (VIP) | ? | OS profile | OS profile | Y | Y | No |
| Auto-lock timer | ? | ? | ? | n/a | n/a | Y | Y | No |
| Hide / incognito install | Y | Y | ? | Y | ? | Y | Y | No |
| Name/icon disguise | ? | Y | Y | ? | ? | Y | ? | Art only, not wired |
| Freeze apps | ? | ? | ? | Y | Y | ? | Y | Stop only |
| Notification control | ? | manual | block autostart | n/a | n/a | Y | Y | No |
| Per-side VPN/proxy | ? | ? | SOCKS, Wi-Fi only | Y | ? | ? | ? | No |
| Permission stripping | ? | ? | Y (premium) | ? | ? | ? | ? | No |
| File transfer between sides | ? | ? | ? | ? | Y | Y | ? | No |
| Backup/restore | ? | ? | ? | ? | ? | restore only | ? | No |
| Allow screenshots toggle | ? | ? | Y | ? | ? | ? | ? | Global FLAG_SECURE disable |

Recurring complaints [comp F10]: unreliable/delayed clone notifications, battery drain with screen off, ads and paywalls (even for paid users), crashes after Android updates. Differentiators therefore include ad-free operation, reliability diagnostics and a running-apps manager. Android 15+ Private Space gives users a native lock, so APKEnclave's lock must add per-space granularity and work on older versions [comp F8]. VirtualXposed issue #942 requests per-instance parameter editing, supporting a per-space settings profile [comp F9]. Shelter is in maintenance mode [comp F6].

---

## 5. Platform enablers and targetSdk 28 constraints

All from [plat]; host `targetSdk 28`, `minSdk 21`, `compileSdk 35`.

| Enabler | Works under targetSdk 28? | Notes |
|---|---|---|
| Static + dynamic shortcuts | Yes; `ShortcutManagerCompat` (API 25+, pinned API 26) | Only main/launcher activity can own shortcuts; `createWithAdaptiveBitmap` for icons |
| Biometric | Yes; `androidx.biometric` supports minSdk 21 | On API 29 and below `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` is unsupported; use weak/credential or `KeyguardManager`. UI gate only, not encryption |
| Per-app language | Yes; `AppCompatDelegate.setApplicationLocales` (AppCompat 1.7.0 present), framework API 33 | Host only; guests keep their own locale |
| SAF export/import | Yes; `CreateDocument`/`OpenDocument`, no runtime permission | Run with progress, stop app before copy; do not use Auto Backup (25 MB cap, `allowBackup=false`) |
| Notification channels | Required (targetSdk >= 26) | On Android 13+ with targetSdk <= 32 the system shows the `POST_NOTIFICATIONS` prompt automatically after first channel creation; no re-prompt after one decline, so explain first |
| QS tile | API 24; `requestAddTileService` API 33 | Docs discourage launch-only tiles; fake-location toggle is the only natural fit |
| Widgets | `RemoteViews` fine; Glance is Compose-only (not suited) | Overlaps pinned shortcuts |
| Icon switching (`activity-alias`) | Yes, API 21+ | Launcher delay, pinned shortcuts to old alias break; add `monochrome` layer (missing today) |
| Two-pane / window size classes | Yes via `-w600dp`/`-w840dp` qualifiers | `values-w600dp` exists; `AppsFragment` span hard-coded 4 |
| Edge-to-edge, predictive back | Not forced at 28 (forced at 35 / 36) | `enableOnBackInvokedCallback="true"` already set; optional polish |
| Play target-API rule | **Blocks Play release at targetSdk 28** | 36 for new apps/updates from 2026-08-31 (extension to 2026-11-01), summary-level fact |
| Android 16 | Affects all apps regardless of target | Non-SDK interface reliance, JobScheduler quotas, intent redirection hardening may hit Bcore; only Android 13 phone available |

---

## 6. Risk view

### Sideload/GitHub build vs Play build

| Idea | Sideload/GitHub | Play build | Basis |
|---|---|---|---|
| Biometric lock, shortcuts, locale, health screen, running manager, freeze, diagnostics, app grid | Yes | Yes (low) | [policy] |
| Fake location | Yes | Possible (medium; page not fetched) | [policy] |
| Backup/export via SAF | Yes | Yes (medium; low if no All-Files-Access) | [policy] |
| All Files Access onboarding | Yes | Needs declaration (restricted permission) | [policy], [engine B4] |
| Disable FLAG_SECURE (existing) | Yes | **Exclude** (high; policy forbids workarounds) | [policy] |
| Hide root / anti-detection (existing) | Yes | **Exclude** (high, medium confidence) | [policy] |
| Device/ID spoofing profiles | Yes | Risky (medium-high, low confidence) | [policy] |
| APK "Open with" handler, install from URL/XAPK | Yes | Risky (REQUEST_INSTALL_PACKAGES, medium-high) | [policy], [plat 8] |
| Import from installed host apps (existing) | Yes | Use targeted `<queries>`, not `QUERY_ALL_PACKAGES` | [policy] |
| Disguise icon/name | Yes | Risky (Deceptive Behavior) | [plat 14] |
| Per-space VPN/proxy | Not now (engine L-XL; host VPN removed) | `VpnService` declaration needed | [engine C], [policy] |

The core container itself is medium risk on Play (must not load `REQUIRE_SECURE_ENV` apps; no installs without consent) [policy]. The `targetSdk 28` blocker comes first [plat 12]. Recommendation: build for sideload/GitHub now, keep Play-sensitive switches behind one build flavour flag later (not now; K5/minimal-implementation).

### Ideas to avoid

- **Notification listener** (`NotificationListenerService`): read-only, special access, high policy sensitivity, low value [plat 6, policy].
- **Accessibility service features**: reserved for disability support; high risk [policy].
- **Xposed/LSPosed reintroduction** and **host VPN mode**: removed by team decision (`runtime.md:6-10`) [docs].
- **Crash/log upload or telemetry**: removed; diagnostics must stay local [docs E].
- **Telegram integration, cloud sync/accounts, ads, paywalls** (excluded in brief; ads are a top competitor complaint [comp F10]).
- **Auto Backup of the virtual data dir** (quota, broken device ids) [plat 7].
- **Cell-tower spoof** until `setNeighboringCell` bug is checked [engine B3].
- **Cross-space account copying** (credentials, high policy/security risk) [engine B7].
- **Guest home-screen widgets** (`IAppWidgetManagerProxy`, L, high risk) [engine B14].
- **Planning from `Docs.md`** APIs [engine D].
- **Marketing the lock as encryption** [plat 5].

---

## 7. Housekeeping and stale claims (separate from features)

- `Docs.md`: fictional APIs (`clearAppData`, `getInstalledApps`, `getAppInfo`, `isAppInstalled`, `setAppEnabled`, `setAppPermission`, `setAppSetting`, `startVirtualProcess`, `stopVirtualProcess`, `getProcessInfo`, `optimizeMemory`, `clearUnusedResources`, `WebViewProxy`); "Join BlackBox forums" [engine D, docs C].
- `README.md`: platforms (Android 5-14, x86, ARMv7) vs ARM only tested 10-15; JDK 17 vs 21; `your-repo/NewBlackbox` URL; "No Root Required" vs `Docs.md` "Root (recommended)"; credits and title still upstream [docs C].
- `roadmap.md`/`vision.md` still list Xposed-style modules and VPN toggle; `RELEASE_NOTES.md` documents VPN without a Removed note and has no 2026-09-30 entry; `tech-stack.md:76` says CI has no tests/lint, `roadmap.md:26` says it does [docs C].
- Dead Xposed-era resources: 5 orphan layouts (`activity_xp`, `item_xp`, `view_switch`, `item_viewpager`, `base_empty`), `bean/XpModuleInfo.kt`, 14 unused strings [app F12].
- Strings: "BlackBox" remains in `add_shortcut_fail_msg` (en `strings.xml:39`, zh-CN `:38`); "Launched Failed"/"Installed Failed"/"Uninstalled Failed"; `fake_location` = "Fake Location(Preview)" [app F11].
- Launcher icon variants exist only as assets (`assets/launcher-icons/{blue,orange}`), not wired via `activity-alias` [app, plat 14].
- `ShortcutActivity` exported with no validation of `pkg`/`userId` extras (`AndroidManifest.xml:59-62`) [app F2].
- Dead code: `requestInstallPackage` always false (`BlackBoxLoader.kt:227-246`); broadcast `REQUEST_STORAGE_PERMISSION` with no receiver (`:156-163`); unused `FakeManagerActivity.finishWithResult`, `ListActivity.start`; no-op service callback (`:259-264`); `STORAGE_POSTPONED_KEY` never reset (`MainActivity.kt:96-112`) [app F4,F12].
- Release-readiness: debug-signed release (`app/build.gradle:32`), cleartext allowed host-wide (`network_security_config.xml`), inline osmdroid dep (`build.gradle:66`, K2), `main_git` link to `github.com/kosmowariat/KBlackbox` while repo not public (`MainActivity.kt:275`) [app].
- Possible bugs: `FollowMyLocationOverlay.kt:106-122` re-requests all permissions; Hangzhou default centre (`:55`); `BLocationManagerService.java:96-98` writes `allCell` in `setNeighboringCell` [app F6, engine].
- Accessibility gaps: only 6 `contentDescription`s, no non-drag reorder [app F13].
- Deferred earlier: Bcore launcher dark-mode white flash, osmdroid dark tiles, dynamic-colour toggle, M3 manual QA never executed [docs A#10-12,22].

---

## 8. Contradictions between findings and confidence

### Contradictions

| # | Conflict | Sources | Resolution |
|---|---|---|---|
| C1 | Brief lists "launcher icon variants" as a current feature; manifest has a single LAUNCHER activity and no `activity-alias`, icon sets are swapped by copying res folders | brief vs [app], [plat 14] | Treated as unbuilt: art exists, runtime switching does not. Next-tier S feature |
| C2 | Play target-API rule: [plat] states dates and calls it a blocker; [policy] lists it as unverified general knowledge. Both rely on summarised fetches | [plat 12], [policy] | Treated as likely true, confidence Medium; verify page before any publish decision |
| C3 | Competitors say lock "may need an engine hook"; platform says pure UI gate | [comp candidate 1] vs [plat 5] | Both true: UI gate is enough for host entry points, hard enforcement on every launch path is unverified |
| C4 | Unused string count: 16 in first count, corrected to 14 in the same file; docs say 103 strings / 101 zh, app says 104 / 102 | [app F12,F11], [docs A#9] | Minor counting differences; use "about 14" and "about 100"; recount before cleanup |
| C5 | VPN: `INDEX.md` copy and old docs mention VPN toggle; `runtime.md` and source say removed | [docs], [engine C] | Trust `runtime.md` and code (no VPN code in Bcore) |
| C6 | `tech-stack.md` says no CI tests/lint; `roadmap.md` says CI runs them | [docs C] | Tech-stack flagged stale by the docs finding |
| C7 | Sandbox overall Play risk "medium" while two shipped toggles are "high" | [policy] | Both recorded; risk depends on build flavour |
| C8 | `BActivityManager.getRunningAppProcesses` name suggests a list but returns one info | [engine Open Questions] | Do not promise a process list until semantics are confirmed |

### Confidence per major conclusion

| Conclusion | Confidence | Reason |
|---|---|---|
| Ten features above are UI-only (no Bcore change) | High for #2,#3,#4,#9; Medium-High for #1,#5,#6,#7,#8,#10 | Hooks cited with file:line; nothing executed on device |
| Biometric lock is the top-validated feature | Medium | 4 of 7 competitor families, but snippet-level evidence |
| Backup via host-side zip needs no Bcore change | Medium-High | Mirrors duplicate-user path; file ownership and live processes untested |
| Running indicator reliability | Low-Medium | Reflection on `mTasks`, affinity substring match |
| Docs.md APIs are largely fictional | High | Greps with zero definitions [engine D] |
| Play risk tags | Low-Medium | Policy pages summarised; several unknown (mock location, notification access, overlay) |
| targetSdk 28 blocks Play | Medium | Summarised page, consistent across two findings |
| Platform API levels (shortcuts, biometric, locale, SAF) | High | Official docs fetched plus prior knowledge |
| Overall report | **Medium** | Static analysis plus secondary competitor sources; single-device verification |

---

## 9. Methodology, conclusions, appendices

### Research objectives
Primary question: what features could APKEnclave still add? Sub-questions: current app state and gaps; unexposed engine capabilities; roadmap/docs; competitor offerings and complaints; platform enablers and targetSdk 28 limits; Play-policy risk. Scope excludes Xposed, Telegram, monetisation, cloud sync and the targetSdk engine spike.

### Methodology
Mixed gap analysis: six gatherers (two codebase, one docs, three external) wrote findings; synthesis cross-referenced claims (code vs docs, competitor vs platform vs policy), scored each candidate on value, effort, Bcore need and policy risk, and ordered by value/effort with UI-only first. Sources: 6 finding files, about 40 external URLs cited across the three external findings, plus repository files cited by path.

### Conclusions
1. **Primary**: APKEnclave has a large UI gap over its own engine. The best ten ideas are all UI-only. Confidence Medium-High.
2. **Direct answer**: add, in order, biometric lock, launcher shortcuts with hardening, Polish/per-app language, permissions/health screen, running-apps manager, fake-location upgrade, backup/export, app grid upgrades, local diagnostics and freeze/auto-stop.
3. **Secondary**: housekeeping (Section 7) should ride along with the first batch; it reduces release risk. Stale docs are the main planning hazard.
4. **Secondary**: Bcore work is justified only for notification mute, per-app hideRoot/FLAG_SECURE, permission deny-list and spoof profiles, after the UI batch.
5. **Secondary**: Play is a separate decision; identify Play-incompatible toggles now (FLAG_SECURE, hide root) and keep them out of any future Play flavour.

### Recommendations
| Priority | Action | Effort | Rationale / risk |
|---|---|---|---|
| 1 | Harden `ShortcutActivity` (validate extras, optional non-exported + lock) | S | Real exposure: any app can launch any sandboxed app [app F2] |
| 2 | Run first batch (Section 2) | M total | UI-only, low risk |
| 3 | Add `androidx.biometric` via `gradle/libs.versions.toml` only | S | Standard K2 |
| 4 | Update roadmap: remove Xposed/VPN, add ranked list | S | Stops stale planning |
| 5 | Verify Play target-API page before any Play decision | S | Medium-confidence fact |
| 6 | Test running-indicator on a service-only guest app before expanding to a task manager | S | Low-confidence API |

### Appendix A: Gaps and uncertainties
Nothing executed on device; only Android 13 available; competitor sources are snippets; mock location, notification access and overlay policy not fetched; `isRunningApplication` accuracy; whether `res` launcher icon has a `monochrome` layer (app finding says unverified, plat says none); behaviour of guest notifications, shortcuts and locales under host features; Android 16 impact on Bcore hooks.

### Appendix B: Source files
`analysis/findings/codebase-app-gaps.md`, `codebase-engine-gap-analysis.md`, `docs-roadmap-and-claims.md`, `external-competitors-feature-matrix.md`, `external-platform-enablers.md`, `external-policy-google-play.md`; `planning/research-brief.md`, `planning/research-plan.md`. Companion: `analysis/synthesis.md`, `outputs/research-report.html`.
