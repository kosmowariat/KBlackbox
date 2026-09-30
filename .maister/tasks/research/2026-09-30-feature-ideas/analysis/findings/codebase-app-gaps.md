# codebase-app: gaps and feature hooks in the `app` module

## TL;DR
- The app module is small (about 3.8k Kotlin lines) and clean: zero TODO/FIXME markers, only 3 top-level screens plus fake-location, GMS, settings and an APK picker.
- There are no widgets, no notification code, no Quick Settings tile, no static/dynamic shortcuts, no onboarding, no backup/export, no per-app info screen, no tablet/landscape layouts (beyond one integer), no Polish locale.
- Half-finished or dead: a storage-permission broadcast with no receiver, `requestInstallPackage` that always returns false, 5 orphan Xposed-era layouts, 16 unused strings, `userID 0` hard-coded for fake location.
- Most cheap wins are UI-only and already have a hook in the code (see the table at the end).

All paths are relative to `D:\KBlackbox\app\src\main\` (java root `java/top/niunaijun/blackboxa/`, abbreviated `J/`).

## Open Questions / Risks
- The brief mentions "launcher icon variants" as an existing feature, but `AndroidManifest.xml` has only one LAUNCHER activity (`WelcomeActivity`, line 34-41) and no `activity-alias`; `res/mipmap-anydpi-v26` has only `ic_launcher.xml` and `ic_launcher_round.xml`. Either the feature is on another branch or it is not built yet.
- Only static code reading was done; nothing was run on a device.
- `res/menu/menu_main.xml` `main_git` points at `https://github.com/kosmowariat/KBlackbox` (MainActivity.kt:275) while the project is "not yet public"; check before release.

---

## 1. What exists today (screens and settings)

| Screen | File | Notes |
|---|---|---|
| Spaces (users) list/grid | `J/view/main/MainActivity.kt:43-285` | FAB creates a space; toolbar toggles grid/list (line 268-273); overflow: Setting, Fake Location, Open Source |
| Space detail (apps grid) | `J/view/apps/UserAppsActivity.kt`, `J/view/apps/AppsFragment.kt` | toolbar: rename / duplicate / delete (`res/menu/menu_user.xml`); long press menu: clear data, stop, uninstall, shortcut (`res/menu/app_menu.xml`) |
| Add app | `J/view/list/ListActivity.kt` | lists host-installed apps with filter, plus `GetContent("application/vnd.android.package-archive")` (line 88-96, 113-117) |
| Fake location | `J/view/fake/FakeManagerActivity.kt`, `FollowMyLocationOverlay.kt` | per-app list, tap to pick on an osmdroid map, long press to disable |
| GMS manager | `J/view/gms/GmsManagerActivity.kt` | per-space install/uninstall switch |
| Settings | `J/view/setting/SettingFragment.kt`, `res/xml/setting.xml` | theme mode, hide root, daemon, disable FLAG_SECURE, GMS manager link |

Persisted host preferences live only in `BlackBoxLoader` (`J/view/main/BlackBoxLoader.kt:19-29`): hideRoot, daemonEnable, showShortcutPermissionDialog, disableFlagSecure, themeMode, userGridView. Per-space app order is kept as a comma string `AppList<userId>` in `AppManager.mRemarkSharedPreferences` (`J/data/AppsRepository.kt:183`).

## 2. Findings

### F1. No app-info / details screen; the app model is thin
- Exists: `AppInfo(name, icon, packageName, sourceDir)` (`J/bean/AppInfo.kt:5`). Long-press menu has only 4 actions (`res/menu/app_menu.xml`).
- Missing: version, size, install time, permissions, running state, "open system-style info", "move/copy to another space", "rename label", per-app settings entry (location, network, permissions).
- Suggests: **App details bottom sheet** with per-app actions and a home for per-app toggles (spoof profile, fake location, network). Also **clone an app to another space** (repository already has `installCopyForUser` and `copyAppData`, `J/data/AppsRepository.kt:396,413`, used only for duplicate-space).

### F2. Shortcut support is pinned-only, no launcher shortcuts
- Exists: pinned shortcut via `ShortcutUtil.createShortcut` (`J/util/ShortcutUtil.kt:22-53`) launching `ShortcutActivity` (`J/view/main/ShortcutActivity.kt:10-23`). Shortcut icon is the app icon, id is `pkg + userId`.
- Missing: no `res/xml/shortcuts.xml`, no dynamic/static shortcuts on the launcher icon (long press APKEnclave -> recent/pinned sandbox apps), no `ShortcutManagerCompat.updateShortcuts` when an app is removed (stale pinned shortcuts), no badge showing which space the shortcut opens (label is just `name + userID`, line 34), `ShortcutActivity` has no error handling when the launch fails, is `exported=true` with no permission (AndroidManifest.xml:59-62) and reads extras `pkg`/`userId` without validation (any app can launch any sandboxed app).
- Suggests: dynamic launcher shortcuts ("recent apps"), "add all apps of this space to home", shortcut cleanup on uninstall, and hardening `ShortcutActivity`.

### F3. No widgets, notifications, Quick Settings tile
- `grep -ri "notification|AppWidget|TileService|POST_NOTIFICATIONS"` over `J/` and the manifest returns nothing. Host manifest declares no receivers/services at all (AndroidManifest.xml).
- The engine side has `BNotificationManager`/`IAppWidgetManagerProxy` (gatherer 2 scope), but the host never surfaces them.
- Suggests: persistent "running sandbox apps" notification with Stop-all; QS tile "Stop all sandbox apps" or "Fake location on/off"; home widget with favourite sandbox apps; notification-channel management per sandbox app (needs engine check).

### F4. Onboarding is one storage dialog
- Exists: `MainActivity.checkStoragePermission()` (MainActivity.kt:96-112) shows a single All Files Access dialog, postponable via `STORAGE_POSTPONED_KEY` (never reset, so the user is never asked again; no settings entry to grant it later). `WelcomeActivity` is a trampoline with no UI (`J/view/main/WelcomeActivity.kt:13-33`; theme `WelcomeTheme`).
- Missing: first-run explainer, permission checklist screen (All Files, battery optimization exemption for the daemon, pinned-shortcut permission, VPN when available, notifications on Android 13+), an in-app "permission status" row in Settings.
- Half-done: `BlackBoxLoader.onStoragePermissionNeeded` sends broadcast `top.niunaijun.blackboxa.REQUEST_STORAGE_PERMISSION` (BlackBoxLoader.kt:156-163) but **no receiver exists** anywhere in `app/` or `Bcore/` (grep finds only the sender), and returns false. So the guest-side "storage permission needed" callback never reaches the UI.
- Suggests: **Onboarding / Permissions & health screen** (roadmap "onboarding for required permissions"); wire the existing callback to a real prompt.

### F5. Settings are minimal and cannot express per-space or per-app policy
- `setting.xml` has 5 switch/list items + GMS link (res/xml/setting.xml:5-51). No About/version/licenses, no language picker, no clear-all-data, no log export, no "restart engine" button (every engine setting only toasts `restart_module`, SettingFragment.kt:88).
- The "restart to apply" toast is the only feedback (string `restart_module` = "Restart app to apply change"); no action button to restart.
- Missing categories: Storage (All files status), Privacy (app lock), Notifications, Advanced/Diagnostics, About.
- Suggests: **About + diagnostics** (version, engine ABI/SDK, export logcat/zip for bug reports), **Restart button**, **app lock / biometric gate** for the host (no `BiometricPrompt`/`androidx.biometric` anywhere), **per-app-language picker** (`LocaleManager` / `AppCompatDelegate.setApplicationLocales`).

### F6. Fake location is functional but stripped down and partly user-0 only
- `MainActivity.kt:279` launches `FakeManagerActivity` with `putExtra("userID", 0)`: fake location can only be managed for space 0 from the UI, although the engine API is per (userId, pkg) (`J/data/FakeLocationRepository.kt:15-45`). `BaseActivity.currentUserID()` reads the same extra (BaseActivity.kt:25). There is no space picker.
- Map screen (`FollowMyLocationOverlay.kt`): default centre is hard-coded `GeoPoint(30.2736, 120.1563)` (Hangzhou) when no location is set (line 55); `longPressHelper` returns false (no-op); no place search/geocoding, no coordinate entry, no saved favourites, no route/speed/jitter; `onRequestPermissionsResult` re-requests every permission unconditionally (lines 106-122, looks buggy); `Configuration.getInstance().load(...)` surrounded by blank-line remnants; uses `ActivityOsmdroidBinding` layout with only a MapView.
- Joystick: `RockerManager` (app/rocker/RockerManager.kt) injects a floating joystick into the **guest** app when fake location is enabled; there is no host setting to enable/disable it or to set step size, and no toggle in the UI (init is gated only by `BLocationManager.isFakeLocationEnable()`).
- String `fake_location` is still "Fake Location(Preview)" (strings.xml:12).
- Suggests: space picker for fake location, favourites/history of places, coordinate input, place search (Nominatim), global fake-location toggle, joystick options, "apply to all apps in space", per-app spoof profiles.

### F7. Install sources and update handling
- Exists: install from the host's installed-app list, from one APK via `GetContent` (single file), and URL sources (`AppsRepository.installApk`, lines 236-261 accepts http URLs via `URLUtil.isValidUrl`, but the UI never offers a URL field).
- Missing: multi-select install, `.apks`/`.xapk`/split-APK bundles (only `application/vnd.android.package-archive` MIME), install via "Open with / Share to" intent filter (the manifest has no `VIEW`/`SEND` intent filter for APKs), update detection (installed version vs host version), install progress, install result screen with reason, "already installed in this space" marker only via `isInstall` (InstalledAppBean.kt).
- `BlackBoxLoader.requestInstallPackage` always returns false (BlackBoxLoader.kt:227-246; computes `packageInfo` then ignores it): dead hook.
- Suggests: multi-select add, split-APK/XAPK import, "Open with APKEnclave" intent filter, "update app" when the host app has a newer version, URL install (code exists, UI missing).

### F8. Backup/export and move between spaces: nothing in UI
- `DataDirCopier` (`J/util/DataDirCopier.kt:8-22`) already copies a data dir while skipping `lib`, `cache`, `code_cache`, and symlinks; used by duplicate-user only. Has unit tests (`app/src/test/.../DataDirCopierTest.kt`).
- No zip/SAF export, no import, `allowBackup="false"` (AndroidManifest.xml:16) on the host.
- Suggests: per-space/per-app export to zip via SAF (`CreateDocument`), import into a new space; reuses `DataDirCopier` and the space-duplicate flow. Needs care with file ownership and running processes (engine call `stopPackage`).

### F9. Lists: no search/sort/filter in the main lists
- Spaces list: no search, order is by id (`getUserList` sorted at AppsRepository.kt:308). Apps grid: manual drag order only (AppsTouchCallBack); `SearchView` only exists in ListActivity and FakeManagerActivity. No sort (name/recent/size), no pinning/favourites, no multi-select.
- Suggests: search + sort in the app grid, favourites, multi-select actions (stop/clear/uninstall/move).

### F10. Tablet/landscape and large screens
- Only resource qualifiers: `values-w600dp/integers.xml` (user_grid_columns 3 vs 2) and `values-v23`. No `layout-land`, `layout-sw600dp`, no two-pane layout.
- `AppsFragment` hard-codes `GRID_SPAN_COUNT = 4` (AppsFragment.kt:47,75) regardless of screen width; `UsersAdapter` has grid/list only.
- No edge-to-edge (`enableEdgeToEdge`/`WindowCompat` not used anywhere); no `screenOrientation`/`configChanges` in the manifest.
- Suggests: responsive span count (`GridAutofitLayoutManager` or dimension-based), two-pane (spaces + apps) on w600dp, edge-to-edge, landscape map layout. Matches roadmap "tablet/landscape layouts".

### F11. i18n state
- `values` has 104 names; `values-zh-rCN` and `values-zh-rTW` each have 102. The only untranslated ones are `more_apps_count` and `theme_mode_values`, both intentionally `translatable="false"`. So zh-* are complete.
- No Polish (`values-pl`) even though the developer is Polish (the research question is in Polish). No `locales_config.xml` / `android:localeConfig`, no language picker.
- User-facing strings still mention "BlackBox" (strings.xml:39 `add_shortcut_fail_msg`; same in zh at values-zh-rCN/strings.xml:38) and "Open Source" menu label; `app_name` is already "APKEnclave".
- Grammar slips: "Launched Failed", "Installed Failed", "Uninstalled Failed".
- Suggests: Polish translation, `localeConfig` + per-app language, string cleanup pass.

### F12. Dead code and unused resources (cleanup candidates)
- Unused strings (16; scripted count of `R.string.x` / `@string/x` references across java, layouts, menus, xml): `enable_xposed`, `hide_xposed`, `hider`, `install_fail_no_msg`, `installed_module`, `jump_module`, `module_setting`, `remove_success`, `start_in_outside`, `uninstall_module`, `uninstall_module_hint`, `userRemark`, `user_app_count` (plurals, unused), `xp_setting`, plus the two arrays `theme_mode_entries` / `theme_mode_values` (referenced via `app:entries` in setting.xml, false positive in my script).
  Real list = 14 unused strings. `uninstall_fail` seems still referenced.
- Orphan layouts with no ViewBinding/R usage: `activity_xp.xml`, `item_xp.xml`, `view_switch.xml`, `item_viewpager.xml`, `base_empty.xml` (Xposed-era, engine standards say Xposed must not return).
- Dead class: `bean/XpModuleInfo.kt` (no references).
- Dead hook: `requestInstallPackage` (see F7); dead broadcast (F4); `FakeManagerActivity.finishWithResult` (lines 122-130) is unused and `ListActivity` companion `start` is unused (UserAppsActivity launches `ListActivity` with an explicit Intent).
- `BlackBoxLoader.addLifecycleCallback` only logs except `RockerManager.init`; `doOnCreate` registers a service-available callback that does nothing (BlackBoxLoader.kt:259-264).
- Duplicate `try/catch` blanket wrappers in BlackBoxLoader/App contradict standard K1 (UI should not blanket-catch) but they protect engine boot; leave as is.
- `build.gradle:66` keeps an inline osmdroid dependency and a commented-out stateview line (standard K2: migrate to version catalog).
- Not a feature but relevant for the store: `res/xml/network_security_config.xml` permits cleartext for the whole host app (`base-config cleartextTrafficPermitted="true"`); release builds are debug-signed (`app/build.gradle:32`).

### F13. Accessibility gaps
- Only 6 `contentDescription`s in layouts (item_user.xml:64,70,76,115; item_user_grid.xml:51; activity_xp.xml:36 - dead). No description on app icons in `item_app.xml`, `item_package.xml`, `item_fake.xml`, `item_gms.xml`, FAB in `activity_main.xml` / `activity_user_apps.xml`, or the toolbar grid/list toggle state beyond title.
- The long-press app menu opens on finger release (AppsFragment.kt:87-127) with a custom touch listener; TalkBack users have no equivalent custom action. The app grid drag-reorder has no non-drag alternative (move up/down).
- Touch target: the 4-column fixed app grid on small phones is tight; no scalable-text handling review.
- Suggests: contentDescriptions pass, TalkBack custom actions for app items, "Move" menu item for reorder, font scale test.

### F14. Loading/empty/error states
- Uses `LoadingActivity.showLoading()` and StateView (`ic_empty`, `empty_empty` "Empty"). The empty state is a generic "Empty" string with no call to action (e.g. "Add your first app"). Errors arrive as Toast strings from the repository (install result `install_fail: %s` passes raw engine message).
- Suggests: empty-state CTA, snackbar with "Retry", richer install-failure dialog (with engine log).

### F15. Theming
- Material 3 DayNight with dynamic colors applied only to activities whose class name starts with `top.niunaijun.blackboxa.` (`App.kt` `applyThemeMode`, precondition). Theme mode is a ListPreference (system/light/dark).
- Missing: dynamic color toggle, AMOLED black, accent picker, themed (monochrome) icon: `ic_launcher.xml` in `mipmap-anydpi-v26` - check for `<monochrome>` (not verified).

## 3. Feature ideas mapped to existing hooks

| Idea | Existing hook (file:line) | Missing | Engine change? | Effort |
|---|---|---|---|---|
| App details sheet + per-app actions | `AppsFragment.kt:189-207` menu, `AppInfo.kt:5` | version/size/permissions fields, sheet UI | no | S-M |
| Dynamic launcher shortcuts + shortcut cleanup | `ShortcutUtil.kt:22-53`, `ShortcutActivity.kt` | `shortcuts.xml`/`ShortcutManagerCompat.setDynamicShortcuts`, hardening | no | S |
| Onboarding and permission health screen | `MainActivity.kt:96-185`, `BlackBoxLoader.kt:144-177` | wizard UI, settings entry, battery-opt, notifications | no | M |
| Per-space fake location + favourites, search | `MainActivity.kt:279`, `FakeLocationRepository.kt`, `FollowMyLocationOverlay.kt` | space picker, history store, geocoder | no | S-M |
| Backup/export and import of a space/app | `DataDirCopier.kt`, `AppsRepository.kt:355-426` | zip+SAF, import flow | maybe (file owner) | M |
| Clone app to another space | `AppsRepository.kt:396,413` | menu item + target picker | no | S |
| Search/sort/favourites in app grid | `ListActivity.kt` filter pattern | UI + persisted sort | no | S |
| Multi-select and batch actions | `AppsFragment.kt:197-230` single-app ops | selection mode | no | M |
| Split-APK/XAPK import, "Open with" | `AppsRepository.kt:236-261`, `ListActivity.kt:88-96` | intent filter, bundle extraction | maybe | M |
| About + diagnostics + log export | `Slog` (engine), `SettingFragment.kt` | screen, zip logs | no | S |
| Tile / notification / widget | none in app | all | notification channel mapping (engine) | M-L |
| Polish + per-app language | `values-zh-*`, none for pl | `values-pl`, `locales_config.xml` | no | S |
| Tablet/landscape, edge-to-edge | `values-w600dp/integers.xml`, `AppsFragment.kt:47` | layouts, adaptive span | no | M |
| App lock (biometric) for host | none | `androidx.biometric`, gate in `WelcomeActivity` | no | S-M |
| Accessibility pass | see F13 | descriptions, custom actions | no | S |
| Restart-engine button | `SettingFragment.kt:88` toast only | process restart helper | no | S |

## 4. Suggested first batch from this source (UI-only, low risk)
1. Polish locale + stale string cleanup (BlackBox mentions, typos, delete 14 dead strings and 5 dead layouts).
2. Dynamic launcher shortcuts + harden `ShortcutActivity`.
3. Permissions/health screen and real wiring of the storage prompt, plus restart button.
4. Per-space fake location (space picker) and favourites.
5. Search/sort/clone-to-space in the app grid.
