# Research Sources

## TL;DR
Sources are grouped by gatherer. Internal paths were verified to exist under `D:\KBlackbox`; external targets are named, URLs to be resolved by search. Application package root: `app/src/main/java/top/niunaijun/blackboxa/`; engine root: `Bcore/src/main/java/top/niunaijun/blackbox/`.

## Open Questions / Risks
- `Bcore/src/main/java/.../fake/frameworks/BlackManager.java` purpose unclear; gatherer 2 should read it.
- `JarManagerTest.java` sits in main sources (core/system); probably irrelevant, note only.
- `XpModuleInfo.kt` in `bean/` is Xposed leftover (dead code; must not be reintroduced).

---

## 1. codebase-app
### Directories
- `app/src/main/java/top/niunaijun/blackboxa/view/` : `apps`, `base`, `fake`, `gms`, `list`, `main`, `setting`, `users`
- `.../data/` : `AppsRepository.kt`, `AppsSortCompon.kt`, `FakeLocationRepository.kt`, `GmsRepository.kt`
- `.../util/` : `ShortcutUtil.kt`, `DataDirCopier.kt`, `DialogEx.kt`, `ToastEx.kt`, `MemoryManager.kt`, `InjectionUtil.kt`, `ResUtil.kt`, `Resolution.java`
- `.../bean/` : `AppInfo`, `DuplicateUserBean`, `FakeLocationBean`, `GmsBean`, `InstalledAppBean`, `UserBean`, `UserCreationResult`, `XpModuleInfo`
- `.../app/` : `App.kt`, `AppManager.kt`, `rocker`; `.../biz/cache`; `.../widget/`
### Resources / manifest
- `app/src/main/res/` : `layout`, `menu`, `xml` (preferences, shortcuts), `values`, `values-night`, `values-w600dp`, `values-v23`, `values-zh-rCN`, `values-zh-rTW`, `drawable*`, `mipmap-anydpi-v26`
- `app/src/main/AndroidManifest.xml`, `app/build.gradle`
### Patterns
- `TODO|FIXME|HACK|not implemented|coming soon` in `app/src`
- Strings present in `values/strings.xml` but unreferenced; layouts/menus unreferenced
- Preference keys in `res/xml` vs code usage; dead settings
- Missing translations (`values` vs `values-zh-*`; Polish absent)

## 2. codebase-engine
### Client APIs (`Bcore/src/main/java/top/niunaijun/blackbox/fake/frameworks/`)
`BAccountManager`, `BActivityManager`, `BJobManager`, `BlackManager`, `BLocationManager`, `BNotificationManager`, `BResourcesManager`, `BStorageManager`, `BUserManager`, `BPackageManager`
### Host entry points
- `Bcore/src/main/java/top/niunaijun/blackbox/BlackBoxCore.java` (public API), `Docs.md`
### Server-side services (`.../core/system/`)
`accounts`, `am`, `location`, `notification`, `os`, `permission`, `pm`, `user`, `BProcessManagerService`, `DaemonService`, `BlackBoxSystem`, `ServiceManager`
### Hooks (`.../fake/service/`)
e.g. `IAppWidgetManagerProxy`, `IShortcutManagerProxy`, `INotificationManagerProxy`, `IAlarmManagerProxy`, `IJobServiceProxy`, `ILocaleManagerProxy`, `IWifiManagerProxy`, `ITelephonyManagerProxy`, `DeviceIdProxy`, `AndroidIdProxy`, `ISettingsProviderProxy`, `AntiVirtualDetectProxy`, `IFingerprintManagerProxy`, `IAppOpsManagerProxy`, `IPermissionManagerProxy`, `IConnectivityManagerProxy`, `IStorageManagerProxy`, `WorkManagerProxy`
### Native
- `Bcore/src/main/cpp/` : `BoxCore.cpp`, `IO.cpp`, `Hook/`, `JniHook/` (excluding vendored `Dobby/`, `xdl/`)
### Method
For each public manager method, grep `app/src` for callers; output table: capability | engine file:line | UI caller or "none".

## 3. documentation
- `.maister/docs/INDEX.md`, `.maister/docs/project/{vision,roadmap,tech-stack,architecture}.md`
- `.maister/docs/standards/frontend/*.md` (android-architecture, android-views, android-feedback, android-resources, accessibility, responsive)
- `README.md`, `Docs.md`, `RELEASE_NOTES.md` (Known Issues, removed features)
- Note: README/Docs.md claims on JDK 17, API 26, x86, root are stale; verify against source.

## 4. external-competitors (WebSearch / WebFetch)
Parallel Space, Dual Space, Island (oasisfeng), Shelter (PeterCxy), Insular, VirtualXposed, Multi Accounts, App Cloner, Samsung Secure Folder, Android work profile / Private Space, plus upstream/related: NewBlackbox, BlackBox, VirtualApp (Lody), Virtual Master, SandVXposed. Look at: store listings, GitHub READMEs/issues, Reddit (r/androidapps, r/Android), XDA, F-Droid descriptions, review themes.

## 5. external-platform (WebSearch / WebFetch)
developer.android.com topics: App widgets, App shortcuts (static/dynamic/pinned), Quick Settings tiles (TileService), per-app language preferences, notification channels, BiometricPrompt, Private Space, Auto Backup / data extraction rules, Storage Access Framework, share/intents, predictive back, large-screen/window size classes, themed icons, Android 13+ notification permission. Check each for targetSdk 28 behaviour and host-vs-guest applicability.

## 6. external-policy (WebSearch / WebFetch; keep small)
Google Play Developer Policy Center: Device and Network Abuse, Deceptive Behavior, Malware/Unwanted Software, Dynamic code loading, `QUERY_ALL_PACKAGES`, `MANAGE_EXTERNAL_STORAGE`, VPN Service policy, AccessibilityService, target API level requirement (targetSdk 28 implication), spoofing/fake location. Output: risk tag table for candidate features only.

## Not included
- Xposed/LSPosed, Telegram, monetisation, cloud sync sources; `Bcore/src/main/cpp/Dobby`, `xdl` vendored code.
