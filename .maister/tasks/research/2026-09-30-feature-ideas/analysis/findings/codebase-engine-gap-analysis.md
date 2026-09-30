# Engine (Bcore) capability vs app usage: gap analysis

## TL;DR
- The `app` module touches only about 25 engine entry points: install/uninstall/launch/stop/clear, list apps, users CRUD, GMS, fake location (`setLocation/getLocation/setPattern/getPattern`), `ClientConfiguration` flags, and `BEnvironment` paths (for duplicate-user data copy). Everything else in Bcore is unexposed.
- Cheapest UI-only wins already backed by the engine: running-app status (`BlackBoxCore.isRunningApplication`), process list/kill, per-app fake-location extras (cell info, global location, disable), storage-permission onboarding (`hasAllFilesAccess` etc.), backup/restore via `BEnvironment` dir copy (same pattern as duplicate-user).
- Several expected features do NOT exist in the engine: per-app permission control (everything is blanket-granted), per-app device/IMEI/Android-ID/Build spoofing (values fixed or derived from host package), per-space VPN/network routing, split-APK/XAPK install, hide/freeze state setter, clipboard bridge.
- Docs.md API section is largely fictional: `clearAppData`, `getInstalledApps`, `getAppInfo`, `isAppInstalled`, `setAppEnabled`, `setAppPermission`, `setAppSetting`, `startVirtualProcess`, `stopVirtualProcess`, `getProcessInfo`, `optimizeMemory`, `clearUnusedResources`, `WebViewProxy` have zero definitions in `Bcore/src/main/java`. Do not plan features from Docs.md.

## Key Decisions (recommendations for synthesis)
- Treat section B ("exists in engine, no UI") as the cheap-feature pool; treat section C ("does not exist") as Bcore-change features with engine risk.
- Per-app spoof profiles need new Bcore plumbing: a per-(user,pkg) config service modelled on `BLocationManagerService`, the only per-app config precedent.
- Backup/export can be done host-side with no Bcore change, reusing the `BEnvironment` dir helpers already used in `AppsRepository.kt:414-418`.

## Open Questions / Risks
- "Exists" means a code path exists; nothing was run. One test phone (Android 13), so other versions are unverified.
- `isRunningApplication` uses reflection on private field `mTasks` (BlackBoxCore.java:1570-1600) and a taskAffinity substring match; accuracy for service-only apps is low-confidence. `BActivityManager.getRunningAppProcesses` (BActivityManager.java:378) returns a single `RunningAppProcessInfo`, not a list; confirm semantics before use.
- `BLocationManagerService.setNeighboringCell` writes `allCell` (BLocationManagerService.java:96-98), suspected bug; treat cell APIs as unreliable.
- Play-policy risk is handled by the external-policy gatherer; the risk column here is engine/stability only.

---

## A. What `app/` actually calls (evidence)

| Engine API | Defined | App call site |
|---|---|---|
| `BlackBoxCore.get()/doCreate/closeCodeInit/on*Attach/on*ActivityOnCreate/isMainProcess/addServiceAvailableCallback` | BlackBoxCore.java:116, 946, 1331-1360, 1231, 1603 | app/App.kt:32-71; view/main/BlackBoxLoader.kt:256-260; MainActivity.kt:71-81; AppsFragment.kt:119 |
| `installPackageAsUser(String/File/Uri, userId)` | BlackBoxCore.java:1118-1151 | data/AppsRepository.kt:245-247, 400, 407 |
| `uninstallPackageAsUser`, `launchApk`, `clearPackage`, `stopPackage` | BlackBoxCore.java:1110, 1079, 1168, 1172 | AppsRepository.kt:278, 289, 298, 414; AppsFragment.kt:252; ShortcutActivity.kt:19 |
| `getInstalledApplications`, `isInstalled`, `isBlackBoxApp` | BlackBoxCore.java:1160, 1106, 1663 | AppsRepository.kt:115, 161, 226, 454 |
| `getUsers/createUser/deleteUser` | BlackBoxCore.java:1176-1184 | AppsRepository.kt:308, 387-391, 437 |
| `isSupportGms/isInstallGms/installGms/uninstallGms`, `GmsCore.isGoogleAppOrService` | BlackBoxCore.java:1200-1212 | GmsRepository.kt:15-39; AppsRepository.kt:326-351; SettingFragment.kt:58 |
| `BLocationManager.get().setLocation/getLocation/setPattern/getPattern` | BLocationManager.java:141, 149, 40, 48 | data/FakeLocationRepository.kt:17-29; app/rocker/RockerManager.kt:141, 166 |
| `ClientConfiguration` overrides (hostPackage, hideRoot, daemon, disableFlagSecure, requestInstallPackage) and `AppLifecycleCallback.onStoragePermissionNeeded` | app/configuration/ClientConfiguration.java; AppLifecycleCallback.java:20 | view/main/BlackBoxLoader.kt:87-250 |
| `BEnvironment.getAppRootDir/getCacheDir/getDataDir/getDeDataDir/getExternalDataDir` | core/env/BEnvironment.java:130, 42, 96, 87, 91 | AppsRepository.kt:399-418 |

## B. Gap table: engine capability exists, app does not use it

| # | Engine capability | Defined | Used in app? | Feature idea it enables | Bcore change? | Risk |
|---|---|---|---|---|---|---|
| 1 | `BlackBoxCore.isRunningApplication(pkg,userId)` | BlackBoxCore.java:1570 | no | "Running" badge on app tiles, "Stop all running" | No (reflective on `mTasks`) | Low/Med |
| 2 | `BActivityManager.getRunningAppProcesses/getRunningServices`; server `BProcessManagerService.getPackageProcessAsUser`, `killPackageAsUser`, `killAllByPackageName` | BActivityManager.java:378, 387; BProcessManagerService.java:263, 249, 230 | no (app only uses `stopPackage`) | Task-manager screen (per-space processes, kill), "kill all in space" | Small: process list not exposed via `BlackBoxCore`; needs client wrapper | Med |
| 3 | Fake-location extras: `setCell/setAllCell/setNeighboringCell/getCell/getAllCell`, `setGlobalLocation/getGlobalLocation/setGlobalCell...`, `disableFakeLocation`, `isFakeLocationEnable` | BLocationManager.java:57-166, 32-36 | no (only per-app set/get location + pattern) | Saved location presets/favourites, per-space default location, cell-tower spoof, "disable fake for this app" toggle | No for presets/global/disable; cell API suspected buggy | Low (UI) / Med (cell) |
| 4 | Storage permission helpers `hasStoragePermission/hasAllFilesAccess/hasFullFileAccess/request*/handle*Result` | BlackBoxCore.java:1030-1075 | no (app reimplements via `onStoragePermissionNeeded`, BlackBoxLoader.kt:144) | Onboarding/permissions checklist with live status (roadmap idea) | No | Low (MANAGE_EXTERNAL_STORAGE is Play-sensitive) |
| 5 | Notification service: channels, enqueue/cancel, `deletePackageNotification` | BNotificationManager.java:19-100; BNotificationManagerService.java:256 | no | Per-app/per-space notification mute, clear-all, channel viewer | Yes for mute (hook `INotificationManagerProxy.EnqueueNotificationWithTag`, L61-68, needs config lookup) | Med |
| 6 | Jobs: `BJobManager.schedule/queryJobRecord/cancelAll/cancel` | BJobManager.java:24-50 | no | Background-jobs viewer / cancel jobs (battery control) | No for viewer | Low/Med |
| 7 | Accounts (`BAccountManager`, 30+ methods, visibility, tokens) | BAccountManager.java:19-327; core/system/accounts/* | no | Per-space accounts list, add/remove; copy account across spaces (`copyAccountToUser`, L128) | No (UI) | Med/High (credentials, policy) |
| 8 | `BUserManager`, `getAllUsers`, `exists` | BUserManagerService.java:39-82 | partial | Space metadata (colour/icon, created date) | Small: `BUserInfo` has no name/colour field; app keeps names in prefs | Low |
| 9 | `BPackageUserState{installed,stopped,hidden}`, `BPackageSettings`, `getInstalledPackagesAsUser` | BPackageUserState.java:8-12; BPackageManagerService.java:611, 795-799 | no | Hide-from-grid / freeze flag, install date | `hidden` has no client setter; small Bcore addition | Low/Med |
| 10 | `InstallOption` flags (SYSTEM/STORAGE/URI_FILE) | entity/pm/InstallOption.java:10-14 | no | Install-from-device-apps vs file distinction; "update in place" is implicit re-install | No | Low |
| 11 | `AppLifecycleCallback` (beforeMainLaunchApk + activity callbacks) | AppLifecycleCallback.java:15-23; BlackBoxCore.java:1188-1196 | only `onStoragePermissionNeeded` | Launch history, last-used sort, usage counters | No | Low |
| 12 | `PackageMonitor` / `addPackageMonitor` | BPackageManagerService.java:773 | no | Live refresh of app grid | Server side only | Low |
| 13 | Shortcut hooks swallow guest shortcut requests (`requestPinShortcut` returns true, dynamic shortcuts no-op) | IShortcutManagerProxy.java:65-101 | host-side pinning exists (`util/ShortcutUtil.kt`, `ShortcutActivity.kt`) | Let guests add their own launcher shortcuts | Yes (M) | Med |
| 14 | AppWidget hook is pass-through | IAppWidgetManagerProxy.java:14-25 | no | Guest widgets on host launcher (unsupported) | Yes (L) | High |
| 15 | `hideRoot`, `disableFlagSecure` (global) | BlackBoxCore.java:1240-1245; IWindowSessionProxy.java:56, 79 | yes, global prefs (BlackBoxLoader.kt:200-218) | Make per-app/per-space | Yes (S): hooks read global `BlackBoxCore.get().isX()` | Low/Med |
| 16 | `Slog` engine logging | utils/Slog | no in-app viewer | In-app log viewer / diagnostics export | No | Low |
| 17 | `ILocaleManagerProxy` (per-app language hook) | fake/service/ILocaleManagerProxy.java | no | Per-app language picker for guests (unverified behaviour) | Probably | Med |
| 18 | Daemon service toggle | ClientConfiguration.isEnableDaemonService | yes | already exposed | - | - |

## C. Capabilities the engine does NOT have

| Feature area | Current engine behaviour (evidence) | Needed Bcore change | Effort / risk |
|---|---|---|---|
| Per-app/per-space permission management | Everything blanket-granted: `IAppOpsManagerProxy` returns `MODE_ALLOWED` for checkOperation/noteOp/startOp (IAppOpsManagerProxy.java:112-202); `IPackageManagerProxy.SimpleAudioPermissionHook` force-grants audio, storage/media, notification and network (IPackageManagerProxy.java:411-455); `IActivityManagerProxy.checkPermission` same (L807-830); `RequestPermissions` passes through (L516-526). Guests use what the host holds. | Per-(user,pkg) deny-list consulted by these hooks (camera/mic/location/contacts toggles) plus UI sheet | M-L; engine risk Med (apps crash on deny; must degrade per K1) |
| Per-app device profile (IMEI, Android ID, Build.*, MAC, SSID, operator) | IMEI/MEID/IMSI = `md5(hostPkg)`, same for every guest (ITelephonyManagerProxy.java:47-101); Wi-Fi SSID/BSSID/MAC hardcoded "BlackBox_Wifi"/`ac:62:5a:82:65:c4` (IWifiManagerProxy.java:40-47); AndroidId returns real value or random fallback (AndroidIdProxy.java:32-49); no `Build.*` spoofing in source (grep only hits Xiaomi checks). | Per-(user,pkg) profile store + Build field rewrite + hooks reading it | L; Med/High (app breakage, detection) |
| Network/VPN per space | Only NOT_VPN capability shim and synthetic connectivity info (IConnectivityManagerProxy.java:149, 193+); no VPN/proxy code in Bcore java or cpp; runtime standard's VPN mode is not in source. | "Block network for app" via `IConnectivityManagerProxy` returning no network; real VPN/proxy needs host `VpnService` + routing | Block: M; VPN/proxy: L-XL, High |
| Data export/import/backup | No engine API. Data sits in plain host dirs (`BEnvironment.getDataDir/getDeDataDir/getExternalDataDir/getBaseApkDir`, BEnvironment.java:87-96, 138) | None: stop app then zip dirs host-side (as duplicate-user at AppsRepository.kt:414-418); include `getUserInfoConf()` | M; low engine risk |
| Split APK / XAPK / APKS install | Single-APK assumption (ABI check `AbiUtils.isSupport(apkFile)` BPackageManagerService.java:685); no split handling in Bcore; picker only `application/vnd.android.package-archive` (ListActivity.kt:100) | Host-side unpack + engine support for split configs (resources/libs) | L; High |
| App freeze / lock / hide | `hidden`/`stopped` state fields exist but no client setter; no biometric lock anywhere | Lock: app-only BiometricPrompt before `launchApk`; hide/freeze: setter + grid filter | Lock S (UI-only); hide S-M |
| Clipboard / intent sharing host<->guest | No clipboard hook in Bcore (grep ClipboardManager/IClipboard: 0 hits); guests share the real clipboard | `IClipboard` proxy for isolation | M; Med |
| Multi-user limits | `BUserManagerService.createUser(int)` has no cap; UI uses `maxOf(id)+1` (AppsRepository.kt:390) | none | - |

## D. Docs.md API claims verified against source

| Docs.md claim | Verdict |
|---|---|
| `installPackageAsUser(File|String|Uri, userId)` (Docs.md:63-93) | TRUE (BlackBoxCore.java:1118-1151) |
| `uninstallPackage(pkg, userId[, true])` (Docs.md:101-104) | PARTIAL: real are `uninstallPackage(String)` (L1114) and `uninstallPackageAsUser(pkg,userId)` (L1110) |
| `clearAppData(pkg,userId[,"cache"/"data"])` (Docs.md:110-114) | FALSE: only `clearPackage(pkg,userId)` (L1168) |
| `getInstalledApps/getAppInfo/isAppInstalled` (Docs.md:122-128) | FALSE: real names `getInstalledApplications`, `getInstalledPackages`, `isInstalled` |
| `setAppEnabled/setAppPermission/setAppSetting` (Docs.md:134-140) | FALSE (no definitions) |
| `startVirtualProcess/stopVirtualProcess/getProcessInfo/optimizeMemory/clearUnusedResources` (Docs.md:309-322) | FALSE |
| `WorkManagerProxy.enqueueWork/cancelWork`, `WebViewProxy`, `GoogleAccountManagerProxy.getAccounts` (Docs.md:426-443) | FALSE or unverified: `WorkManagerProxy` is a hook class, `WebViewProxy` has no source; `UIDSpoofingHelper` does exist |

## E. Hook inventory relevant to features (`fake/service/*Proxy`)
Activity/ActivityTask/ActivityClient, Package, Permission, AppOps, Notification, Alarm, Job, Shortcut, AppWidget, Location, Telephony, PhoneSubInfo, Wifi, Connectivity, SettingsProvider, Storage/StorageStats, Power (40 lines, no wake-lock policy), Audio/AudioRecord/MediaRecorder, DevicePolicy, Fingerprint, Autofill, Accessibility, InputMethod, Window/WindowSession (FLAG_SECURE), Locale, `AntiVirtualDetectProxy` (neutralises known anti-emulator classes, L77-86), Xiaomi/MIUI proxies, `GmsProxy`, `GoogleAccountManagerProxy`, `WorkManagerProxy`, `SQLiteDatabaseProxy`, `LevelDbProxy`.

## F. Ranked candidate ideas from the engine side (input to synthesis)
1. Backup/restore and export of a space (host-side zip of BEnvironment dirs; no Bcore change) - M, low risk.
2. Running indicator + "stop all" + process/task screen (B1-B2) - S-M, low-med.
3. Location presets/favourites, per-space default, "disable fake for app" (B3) - S, low.
4. Permissions onboarding checklist with live status (B4) - S, low.
5. Launch history / last-used sort / counters via `AppLifecycleCallback` (B11) - S, low.
6. Biometric/PIN lock per app or space (app-only) - S-M, low.
7. In-app log viewer / diagnostics export (B16) - S, low.
8. Per-app hideRoot / disableFlagSecure toggles (B15) - S-M, small Bcore edit.
9. Per-app notification mute/clear (B5) - M, Bcore hook edit.
10. Per-app "block network" and permission deny-list (section C) - M-L, engine risk.
11. Later/high risk: per-app spoof profiles (L), split/XAPK install (L), VPN per space (XL).
