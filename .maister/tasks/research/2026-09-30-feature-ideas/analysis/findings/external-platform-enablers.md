# External / Platform: Android enablers for APKEnclave host-app features

## TL;DR
- Almost every candidate is host-side, pure `app`-module work with stable androidx APIs; none needs Bcore changes (except: guest-facing notification/locale behaviour, see below).
- Cheapest wins: static/dynamic shortcuts (pinned shortcut code already exists), biometric app lock, per-app language (zh/pl strings already exist), launcher-icon switching via activity-alias (icon variants exist), SAF zip export/import. Quick Settings tile and widgets are M and mostly a "launch space/app" convenience.
- Host targetSdk 28 means: no forced edge-to-edge (Android 15) or forced predictive back (Android 16), no 16 KB/large-screen-ignore-orientation rules (those are targetSdk 36 gated), but Android 13+ auto-shows the POST_NOTIFICATIONS prompt, and Play's target-API rule (see external-policy) blocks a Play release at targetSdk 28.
- Auto Backup is the wrong tool (25 MB cap, guest data is huge, `allowBackup="false"` today); a user-driven SAF export of space metadata (+ optional per-app data zip) is the right one.

## Open Questions / Risks
- Whether guest apps' shortcuts/widgets/notifications are visible to these host features is engine behaviour (hooks `IShortcutManagerProxy`, `IAppWidgetManagerProxy`, `ILocaleManagerProxy`) and is for the codebase-engine gatherer; this file covers host-only capability.
- Doc pages were summarised by a fetch model; API-level facts below marked High are ones I also know from platform docs, Medium are summary-only.
- Play target-API rule summary (36 for new apps/updates from 2026-08-31, 35 for existing apps, extension to 2026-11-01) comes from a summarised fetch; verify the page directly before relying on it.

---

## Repo facts relevant here (verified by reading the repo)
- `app/src/main/java/top/niunaijun/blackboxa/util/ShortcutUtil.kt`: pinned shortcuts already implemented via `ShortcutManagerCompat.requestPinShortcut` with `ShortcutActivity` (`pkg`, `userId` extras); shortcut id `packageName+userID`. No static `shortcuts.xml` exists (`app/src/main/res/xml` has only `network_security_config.xml`, `setting.xml`); no dynamic shortcuts.
- `AndroidManifest.xml`: `allowBackup="false"` (line 17), `enableOnBackInvokedCallback="true"` (line 25), no `activity-alias`, no `TileService`, no widget receiver, no `localeConfig`.
- `build.gradle` root: compileSdk 35, targetSdk 28, minSdk 21. `libs.versions.toml`: appcompat 1.7.0 present; no biometric, splashscreen, window, or glance deps.
- `assets/launcher-icons/` has `blue` (active) and `orange` variants that are swapped by copying res folders (README); no runtime switching. Adaptive icon in `mipmap-anydpi-v26` has no `monochrome` layer (grep found none).
- `res/values-w600dp` exists; `values-zh-rCN`/`zh-rTW` exist; Polish absent.

---

## 1. App Shortcuts (static / dynamic / pinned)
- **What**: launcher long-press shortcuts (static in XML, dynamic at runtime) and user-pinned icons. Source: https://developer.android.com/develop/ui/views/launch/shortcuts and .../creating-shortcuts (fetched).
- **API level**: static/dynamic API 25 (ShortcutManager; `ShortcutManagerCompat` makes it safe on minSdk 21 as no-op <25); pinned requires API 26 (doc: "Requires Android 8.0"). Confidence High.
- **Limits**: launchers usually show max 4 static+dynamic; `getMaxShortcutCountPerActivity()`; pinned unlimited; `shortLabel` <= 10 chars, `longLabel` <= 25. Only ACTION_MAIN/LAUNCHER activities can own shortcuts. Shortcut ids should be stable (backup/restore of pinned shortcuts works by id).
- **Integration**: already half done. Add (a) dynamic shortcuts "Open space N" / "Recent guest apps" via `ShortcutManagerCompat.pushDynamicShortcut` reusing `ShortcutActivity` intent; (b) static "Add app", "Fake location" shortcuts in `res/xml/shortcuts.xml` referenced from `MainActivity` via `android.app.shortcuts` meta-data.
- **Gotchas**: pinned shortcut currently passes `null` callback and shows a dialog about launcher permission (MIUI/OEM); shortcut intents are launcher-visible, so avoid sensitive info (doc security note) - relevant if hiding apps. Bitmap icons via `createWithBitmap` (not adaptive) look off on some launchers; use `createWithAdaptiveBitmap`. Do not store package names of hidden apps if an "app lock/hide" feature is added.
- **Effort**: S (dynamic/static), Bcore change: no. Play risk: low.

## 2. Quick Settings tile (TileService)
- **What**: tile in QS panel. https://developer.android.com/develop/ui/views/quicksettings-tiles (fetched).
- **API level**: TileService API 24 (works on minSdk 21 only as no-op; need manifest service to be ignored below 24, harmless). `requestAddTileService()` prompt API 33 (doc says Android 13+). `startActivityAndCollapse(PendingIntent)` is the API 34 replacement of the Intent variant (the fetched summary said "API 28+" for FLAG_ACTIVITY_NEW_TASK; treat the PendingIntent variant as API 34 - Medium).
- **Doc guidance**: do NOT make tiles that only launch an app (use shortcuts); recommended for toggles; max 2 tiles per app.
- **Good fits**: toggle "Fake location on/off", toggle "Daemon/VPN mode" (engine settings need restart, so poor fit). Fake-location toggle is the only natural one.
- **Gotchas**: tile runs in host process; needs engine/fake-location service reachable; `ACTIVE_TILE` meta-data for state updates; `unlockAndRun` when device secure.
- **Effort**: M. Bcore change: maybe (only if toggle needs a global on/off API). Value: low-medium.

## 3. App widgets (AppWidgetProvider / Glance)
- **What**: home-screen widget. https://developer.android.com/develop/ui/views/appwidgets/overview (fetched).
- **API level**: AppWidgetProvider since API 3; responsive/exact layouts and reconfigurable widgets from Android 12 (API 31); min `updatePeriodMillis` 30 min. `requestPinAppWidget` API 26 (prior knowledge, High). Jetpack Glance is Compose-only (new dependency; project uses Views, so prefer `RemoteViews`). Glance minSdk is 23 (prior knowledge, Medium) which is above project minSdk 21.
- **Use case**: widget grid of favourite guest apps per space (tap launches via same `ShortcutActivity` path) or a "stop all" button. Widgets are effectively shortcuts on a home screen; pinned shortcuts already cover the main need.
- **Gotchas**: collection widgets need `RemoteViewsService` and icon bitmaps via the engine `BPackageManager`; widget process is launcher-driven; Material 3 dynamic-colour widgets need API 31 `@android:color/system_*`; also note the engine has `IAppWidgetManagerProxy` (guest widgets), unrelated to a host widget.
- **Effort**: M. Bcore: no. Value: low-medium (pinned shortcuts overlap).

## 4. Per-app language (LocaleManager / AppCompatDelegate)
- **What**: user picks app language without changing system language. https://developer.android.com/guide/topics/resources/app-languages (fetched).
- **API level**: framework `LocaleManager` API 33; `AppCompatDelegate.setApplicationLocales()` backports to API 21 via AppCompat 1.6+ (project has 1.7.0). Confidence High.
- **Integration**: add `res/xml/locales_config.xml` (en, zh-CN, zh-TW, and pl if added) + `android:localeConfig` in manifest (system settings entry on 13+) or AGP `androidResources { generateLocaleConfig = true }` with `resources.properties` (`unqualifiedResLocale=en-US`); for <13 add `AppLocalesMetadataHolderService` with `autoStoreLocales=true`; add a language row in Settings (`PreferenceFragmentCompat`). All activities must extend `AppCompatActivity` (they do via `BaseActivity`; verify).
- **Gotchas**: host-only; guest apps use their own locale (engine has `ILocaleManagerProxy`, separate). Needs Polish strings to be meaningful (user writes Polish); `values-zh-*` already exist. Bcore: no.
- **Effort**: S (plus translation effort). Value: medium (enables Polish, broadens audience). Play risk: none.

## 5. Biometric app lock (androidx.biometric / BiometricPrompt)
- **What**: gate opening the host app or individual spaces/apps with fingerprint/face/device credential. https://developer.android.com/identity/sign-in/biometric-auth (fetched).
- **API level**: androidx.biometric supports minSdk 21 (Medium; library handles fallback), BiometricPrompt native from 28. `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` is NOT supported on API 29 and below, so on Android 10 and lower use `BIOMETRIC_WEAK`/credential only, or a KeyguardManager fallback (doc note). Cannot combine `setNegativeButtonText` with `DEVICE_CREDENTIAL`.
- **Integration**: new dep in `gradle/libs.versions.toml`; `BiometricPrompt` needs a `FragmentActivity` (AppCompatActivity OK). Lock in `MainActivity.onStart`/after timeout; per-space lock gates `UserAppsActivity` and `ShortcutActivity` (important: shortcuts otherwise bypass the lock). Store lock preference in settings.
- **Gotchas**: this is a UI gate, not encryption: guest data in the app's private dir is not protected from root; do not market as encryption. Also `FLAG_SECURE` on host screens (recents thumbnail) needs care; the engine already has a "disable FLAG_SECURE" for guests, so the host may want the opposite for its own window. Launcher-shortcut launching guest apps bypasses host UI unless `ShortcutActivity` also authenticates.
- **Effort**: S-M. Bcore: no. Value: high for a privacy sandbox. Play risk: low.

## 6. Notification channels and notification listener
- **Channels**: API 26; post without a channel is dropped when targetSdk >= 26 (host targets 28, so it must create channels). https://developer.android.com/develop/ui/views/notifications/channels (fetched). Channel settings intent `ACTION_CHANNEL_NOTIFICATION_SETTINGS`; importance cannot be changed programmatically after creation.
- **POST_NOTIFICATIONS with targetSdk 28**: per https://developer.android.com/about/versions/13/changes/notification-permission (search snippet), apps targeting 32 or lower get the system permission dialog automatically the first time an activity starts after a channel is created; if the user declines once there is no re-prompt. So the host cannot control timing on Android 13+; a notification-heavy feature (e.g., per-space notification mute UI) should be introduced with an explanatory screen before the first channel is created. Confidence High.
- **Guest notifications**: produced through engine `INotificationManagerProxy`/`BNotificationManager`; per-space channel grouping (`NotificationChannelGroup`) is a possible engine-side feature (per-space "mute"). Needs Bcore; leave to engine gatherer.
- **NotificationListenerService**: API 18, special access granted in Settings; read-only (cannot block), Play policy requires disclosure (https://developer.android.com/reference/android/service/notification/NotificationListenerService, summary Medium). Only useful for a "unified notification inbox" across spaces; overkill and high policy sensitivity. Not recommended.
- **Effort**: channels S; listener L with high policy risk.

## 7. Backup and restore
- **Auto Backup**: API 23, 25 MB cap per app, default includes files/prefs/db, needs `allowBackup=true` plus `dataExtractionRules` (Android 12+) and `fullBackupContent` (<=11). https://developer.android.com/identity/data/autobackup (fetched). Repo currently `allowBackup="false"`. Guest data lives under host private dir (virtual storage), which would blow the 25 MB quota and could restore broken state/device ids; recommend enabling it (if at all) only for host prefs via `include domain="sharedpref"` and excluding the virtual data dir. Not useful for the headline feature.
- **SAF export/import**: `ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT` (API 19), `OPEN_DOCUMENT_TREE` API 21; no runtime permission needed; system will not overwrite existing files; stream via `contentResolver.openOutputStream` to write a zip; use `ActivityResultContracts.CreateDocument/OpenDocument` (`registerForActivityResult` standard in project). https://developer.android.com/training/data-storage/shared/documents-files (fetched). Android 11+: cannot pick `Android/data`, `Android/obb`, download root for tree - irrelevant for a file-picker zip.
- **What to export**: (a) space list + per-space app list + spoof profiles (small JSON) - S; (b) per-app data zip via existing `DataDirCopier.kt` logic - M/L (size, consistency while app stopped); (c) APK split re-install - L. Gotchas: file can be large, run in a service/worker with progress, stop app before copying, handle `InstallResult.success`.
- **Effort**: (a) S, (b) M. Bcore: no for (a); (b) uses existing copy util. Play risk: low.

## 8. Share sheet / ACTION_SEND into a space
- **What**: host registers `ACTION_SEND`/`ACTION_VIEW` intent filters so a user can "share to a space" (e.g., open a link/APK/file in a chosen cloned app). Standard intent filters; Direct Share targets via `ShortcutInfoCompat` with categories (API 29 ChooserTargetService replaced by sharing shortcuts, `androidx.sharetarget`, API 23+). Medium; no page fetched.
- **Engine angle**: launching a guest activity with an intent is a Bcore capability (`BActivityManager.startActivity`); a resolver sheet showing guest apps per space is UI. The repo currently has no `ACTION_SEND` filters (grep). Gotcha: an APK-open intent filter (`application/vnd.android.package-archive`) makes the host a competing installer handler; Play-risk flag.
- **Effort**: M. Bcore: maybe (intent resolution across virtual packages). Value: medium-high (cloner competitors commonly support "open in clone" - confirm with competitors file).

## 9. Dynamic colour, edge-to-edge, predictive back
- **Dynamic colour**: already done (per brief). API 31 for system wallpaper colours.
- **Edge-to-edge**: enforced by default only for apps targeting 35 (https://developer.android.com/about/versions/15/behavior-changes-15, fetched). Host targets 28, so no change on Android 15, but `enableEdgeToEdge()` (androidx.activity 1.8+) is optional polish; on targetSdk 28 nav bar/status bar colours still settable. When/if targetSdk moves to 35/36 it becomes mandatory (opt-out attribute removed at 36).
- **Predictive back**: Android 16 turns it on by default only for apps targeting 36 and stops calling `onBackPressed()` (https://developer.android.com/about/versions/16/behavior-changes-16, fetched). Repo already opts in (`enableOnBackInvokedCallback="true"`). Needs all back handling on `OnBackPressedDispatcher` (project standard). Cross-activity animation in Android 14/15 is system-drawn; Material components animate. Effort S audit.

## 10. Tablet / foldable adaptive layouts
- Window size classes: compact <600, medium 600-840, expanded 840-1200 dp (https://developer.android.com/develop/ui/views/layout/window-size-classes, fetched); use Jetpack WindowManager `WindowMetricsCalculator` (new dep; `androidx.window`) or simple resource qualifiers `-w600dp`/`-w840dp` (repo already has `values-w600dp`). Material 3 navigation rail / two-pane (spaces list left, apps grid right) is the natural split for this app.
- Android 16 ignores manifest orientation/resizability/aspect-ratio restrictions on >=600dp displays, but only for apps targeting 36 (fetched), so host is unaffected today; guest apps may be affected separately (engine).
- **Effort**: M (two-pane main screen, grid span count by width). Bcore: no. Value: medium (roadmap "tablet/landscape layouts").

## 11. Splash Screen API
- Platform SplashScreen API Android 12 (API 31); `androidx.core:core-splashscreen` 1.0.0 compat back to API 23 (https://developer.android.com/develop/ui/views/launch/splash-screen, fetched). Apps with a custom splash activity must migrate on 12+ (the system shows its own anyway). Repo has `WelcomeTheme` activity (likely a custom splash), and `assets/launcher-icons` mentions splash icons. Gotcha: animated icon 432 dp AVD; adaptive icon masking applies. Effort S, polish only, low value.

## 12. Play features (only a note)
- In-app updates and Play Integrity are Play-distribution features; the project is not public and Play is undecided. Sideloaded builds would need a custom update checker (GitHub releases). Not evaluated further. Play target-API rule: new apps/updates must target 36 from 2026-08-31 (extension to 2026-11-01), existing apps must target 35 to stay visible to new users on newer OSes (https://developer.android.com/google/play/requirements/target-sdk, summary Medium). The host's targetSdk 28 is a blocker for Play; forward to external-policy.

## 13. Accessibility (TalkBack)
- Project standard requires `contentDescription` from string resources, 48 dp targets, TalkBack testing (`.maister/docs/standards/frontend/accessibility.md`). Android 16 deprecates `announceForAccessibility()` and `TYPE_ANNOUNCEMENT` in favour of live regions / pane titles (https://developer.android.com/about/versions/16/behavior-changes-all, fetched, applies to all apps). Action items: audit drag-reorder with accessibility custom actions (drag is not TalkBack-usable), map joystick alternative controls, avoid `announceForAccessibility`. Effort S-M; no Play risk.

## 14. App icon alternatives (activity-alias icon switching)
- **Mechanism**: declare one `<activity-alias>` per icon with `android:icon`, `android:enabled` false except the default, targetActivity=`MainActivity`, LAUNCHER intent filter; switch with `PackageManager.setComponentEnabledSetting(alias, STATE_ENABLED, DONT_KILL_APP)`. Works on API 21+ (prior knowledge, High; not fetched). Existing assets: `assets/launcher-icons/{blue,orange}` (each has mipmap + drawable sets incl. `integers.xml`), currently swapped by file copy.
- **Gotchas**: changing the component may kill/relaunch the app on some launchers, launcher shows the new icon after a delay; pinned shortcuts to the old alias break; `ShortcutActivity`/intent-filter for `MAIN/LAUNCHER` should be on all aliases; the adaptive `mipmap-anydpi-v26` icon needs both variants as separate resources (`ic_launcher_blue`, `ic_launcher_orange`); add a `monochrome` layer for Android 13 themed icons (the adaptive icon has none today; Android 16 QPR2 auto-themes icons lacking one, https://developer.android.com/develop/ui/views/launch/icon_design_adaptive, fetched). A "disguise" icon (calculator etc.) is a stealth feature with Play risk (Deceptive Behavior) - flag to external-policy.
- **Effort**: S. Bcore: no. Value: low-medium, fun; already has art.

## 15. Android 15 Private Space interplay
- Private Space is a separate user profile with separate app copies, hidden when locked, apps cannot detect or be moved into it, no special APIs (https://developer.android.com/about/versions/15/features#private-space, fetched). For APKEnclave: the host can be installed into Private Space and works there independently; the host can't detect it, so don't rely on `UserManager` assumptions (work-profile logic assuming non-main profile = work). Marketing angle: virtual spaces complement but do not replace Private Space. Also the app's own lock feature overlaps conceptually. Effort: test only (S). No feature to build.

## 16. Android 16 changes affecting a sandbox (host targetSdk 28)
Apply to all apps regardless of targetSdk (https://developer.android.com/about/versions/16/behavior-changes-all, fetched):
- JobScheduler quota tightening (affects WorkManager/JobScheduler, so guest background jobs proxied via `IJobServiceProxy`/`WorkManagerProxy` may run less; engine concern).
- ART internal changes may break non-SDK-interface reliance (Android 12+ via Mainline) - directly relevant to Bcore's reflection/FreeReflection/Dobby hooks; needs device testing on Android 16 (only test phone is Android 13). Confidence High that it is stated; impact on Bcore unknown.
- Ordered broadcast priority only respected within the same process - may affect guest receivers dispatched through the host stub process model.
- Intent redirection hardening (default) - may affect the engine's intent rewriting to `Proxy*` stub components (potential breakage; verify, unknown).
- 16 KB page-size compat mode dialog unless built with the new SDK; repo notes 16 KB alignment for native libs (native standard) - ok.
- Targeting-36-only items (edge-to-edge opt-out removed, predictive back default, large-screen restrictions ignored, safer intents opt-in) do not hit host at 28.
- These are risks to existing behaviour, not features; feed to the targetSdk spike.

---

## Summary matrix (host-side feasibility)

| Enabler | Min API for host | Needs new dep | Bcore change | Effort | Value | Play/policy risk |
|---|---|---|---|---|---|---|
| Static + dynamic shortcuts | 25 (compat) | no | no | S | medium | low |
| Pinned shortcut improvements | 26 | no | no | S | low | low |
| QS tile (fake location toggle) | 24 | no | maybe | M | low | low |
| Widget (RemoteViews) | 21 | no | no | M | low-med | low |
| Per-app language | 21 via AppCompat | no | no | S + translations | medium | none |
| Biometric lock | 21 (lib) | biometric | no | S-M | high | low |
| SAF export/import | 19/21 | no | no (meta) | S / M | high | low |
| Share-to-space intents | 21 | no (sharetarget optional) | maybe | M | med-high | medium (APK handler) |
| Icon switching | 21 | no | no | S | low-med | low (disguise = higher) |
| Window size classes / two-pane | 21 | androidx.window optional | no | M | medium | none |
| Splash API | 23 compat | core-splashscreen | no | S | low | none |
| Notification listener | 18 | no | no | L | low | high |
| Auto Backup | 23 | no | no | S | low | low |

## Sources fetched
- https://developer.android.com/develop/ui/views/launch/shortcuts
- https://developer.android.com/develop/ui/views/launch/shortcuts/creating-shortcuts (404 at this path; content from .../guide/topics/ui/shortcuts/creating-shortcuts redirect)
- https://developer.android.com/develop/ui/views/quicksettings-tiles
- https://developer.android.com/guide/topics/resources/app-languages
- https://developer.android.com/identity/sign-in/biometric-auth
- https://developer.android.com/identity/data/autobackup
- https://developer.android.com/training/data-storage/shared/documents-files
- https://developer.android.com/develop/ui/views/appwidgets/overview
- https://developer.android.com/about/versions/15/behavior-changes-15
- https://developer.android.com/about/versions/15/features#private-space
- https://developer.android.com/about/versions/16/behavior-changes-all
- https://developer.android.com/about/versions/16/behavior-changes-16
- https://developer.android.com/develop/ui/views/launch/icon_design_adaptive
- https://developer.android.com/develop/ui/views/launch/splash-screen
- https://developer.android.com/develop/ui/views/layout/window-size-classes
- https://developer.android.com/develop/ui/views/notifications/channels
- https://developer.android.com/about/versions/13/changes/notification-permission (search result snippet only)
- https://developer.android.com/reference/android/service/notification/NotificationListenerService
- https://developer.android.com/google/play/requirements/target-sdk
