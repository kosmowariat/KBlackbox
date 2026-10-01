# Release Notes - NewBlackbox

## Version: Spaces and Tools (2026-10-01)

---

### New Features

#### Backup and Restore of a Space
Export a space (its apps, their data and the app order) to a zip file and create a new space from such a file.

- **Location:** space menu → Export to backup file; main menu → Import from backup file
- Uses the system file picker, so the file can live anywhere (Downloads, a cloud drive)
- Google apps and services are not included; native libs and caches are skipped
- Apps split into several APK files are not supported (only the base APK is saved)

#### App Menu: Info and Copy to Another Space
- **App info** shows the package, version and the size of the app data
- **Copy to another space** installs the app into a chosen space, with or without its data
- The app menu opens when you release a long press without moving; dragging an icon still reorders the grid

#### Running Indicator and Stop All
- A dot on an app icon shows that it is running
- **Stop all apps** (space menu) and **Stop all apps in all spaces** (main menu)

#### Permissions and Health Screen
Settings → Sandbox → Permissions & health shows all-files access, battery optimization, notifications, launcher shortcuts support and the engine version, with buttons that open the right system screen. A Close app button applies engine settings that need a restart.

#### Launcher Shortcuts
Long-press the launcher icon for Fake location and the recently launched apps. Shortcuts of uninstalled apps are removed.

#### Language
Polish translation and an in-app language picker (Settings → Appearance → Language).

#### Fake Location Upgrade
Pick the space, type coordinates, and keep favourite places (long press removes one). Reachable from the space menu as well.

#### About and Logs
Settings → About shows the version and credits and can share the app log file.

#### Space Card Polish
- In the grid view a space without apps shows a "No apps yet" hint with a plus instead of only its name
- Long-pressing a card in the grid view shows only rename, duplicate and delete (it also listed items the card could not handle)
- Decorative icons are hidden from screen readers

---

### Bug Fixes

#### Running apps were never detected
**Problem:** `BlackBoxCore.isRunningApplication` always returned false on current devices.
**Root Cause:** It read the private `mTasks` map of the activity stack by reflection.
**Solution:** Ask the engine process service, which tracks the live processes of each user.
**Files Changed:** `Bcore/src/main/java/top/niunaijun/blackbox/BlackBoxCore.java`

#### App context menu never opened
**Problem:** Long-pressing an app in the grid did nothing.
**Root Cause:** `ItemTouchHelper` took over the long press for drag and the view long-click listener never fired.
**Solution:** The menu is shown from the drag callback when the press ends without moving the icon.
**Files Changed:** `app/src/main/java/top/niunaijun/blackboxa/view/apps/AppsTouchCallBack.kt`, `AppsFragment.kt`

---

### Known Issues

- A sandboxed app that starts background work right after being stopped can crash on its first database access (`SQLiteGlobal.getDefaultSyncMode`, `ArrayIndexOutOfBoundsException`). Seen once with FreeCell, not reproduced.
- Freezing apps and automatic stop of idle apps are not implemented; they need an engine setter for the hidden flag and a background service.

---

## Version: GUI Refresh (2026-09-27)

---

### New Features

#### Material 3 Theme and Dark Mode
The host app UI moves from the grey `MaterialComponents` theme to Material 3 with full light and dark color schemes.

- **Location:** Whole host app (main screen, toolbar, app grid, lists, GMS manager, settings, dialogs, splash)
- **Default:** Custom blue palette (seed `#3F6FD8`); follows the system light/dark setting
- On Android 12+ the palette follows the wallpaper (dynamic color); older versions use the custom palette
- The status bar, toolbar, FAB, page dots, badges, search bar, list rows and GMS switches (now `MaterialSwitch`) use theme colors in both modes
- Sandboxed (guest) apps are not themed

#### Theme Preference
Added a setting to choose the app theme.

- **Location:** Settings → Appearance → Theme
- **Default:** System default
- Options: System default / Light / Dark
- Applies immediately, without a restart, and persists across launches

**Files Changed:**
- `app/src/main/java/top/niunaijun/blackboxa/app/App.kt`
- `app/src/main/java/top/niunaijun/blackboxa/view/main/BlackBoxLoader.kt`
- `app/src/main/java/top/niunaijun/blackboxa/view/main/MainActivity.kt`
- `app/src/main/java/top/niunaijun/blackboxa/view/setting/SettingFragment.kt`
- `app/src/main/java/top/niunaijun/blackboxa/view/apps/AppsAdapter.kt`
- `app/src/main/java/top/niunaijun/blackboxa/view/gms/GmsManagerActivity.kt`
- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/values/dimens.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-night/colors.xml`
- `app/src/main/res/values-v23/themes.xml`
- `app/src/main/res/values-zh-rCN/strings.xml`
- `app/src/main/res/values-zh-rTW/strings.xml`
- `app/src/main/res/drawable/splash.xml`
- `app/src/main/res/drawable-night/ic_empty.xml`
- `app/src/main/res/drawable-anydpi/ic_add.xml`
- `app/src/main/res/drawable-anydpi/ic_search.xml`
- `app/src/main/res/layout/view_toolbar.xml`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/res/layout/item_app.xml`
- `app/src/main/res/layout/activity_list.xml`
- `app/src/main/res/layout/item_package.xml`
- `app/src/main/res/layout/item_fake.xml`
- `app/src/main/res/layout/item_gms.xml`
- `app/src/main/res/xml/setting.xml`

---

## Version: Latest Build (2026-01-31)

---

### New Features

#### VPN Network Mode Toggle
Added a new setting to choose between VPN and normal network mode for sandboxed apps.

- **Location:** Settings → Others → Use VPN Network
- **Default:** OFF (normal network mode)
- When enabled, traffic is routed through BlackBox's VPN service
- Requires app restart to take effect

**Files Changed:**
- `app/src/main/java/top/niunaijun/blackboxa/view/main/BlackBoxLoader.kt`
- `app/src/main/java/top/niunaijun/blackboxa/view/setting/SettingFragment.kt`
- `app/src/main/res/xml/setting.xml`
- `app/src/main/res/values/strings.xml`
- `Bcore/src/main/java/top/niunaijun/blackbox/app/configuration/ClientConfiguration.java`
- `Bcore/src/main/java/top/niunaijun/blackbox/BlackBoxCore.java`

#### Device Information Logging
Added comprehensive device info header in logcat for easier debugging:
- Android version, SDK level, security patch
- Device manufacturer, brand, model, hardware
- Supported CPU/ABIs (32-bit and 64-bit)
- Memory info (heap usage)
- App version and package info
- Build fingerprint and timestamps

---

### Bug Fixes

#### VPN Permission Fix
**Problem:** VPN service failed to establish interface (`builder.establish()` returned null).

**Root Cause:** Android requires `VpnService.prepare()` to be called from an Activity before VPN can be established.

**Solution:** Added VPN permission request to `MainActivity.kt` on app launch.

**Files Changed:**
- `app/src/main/java/top/niunaijun/blackboxa/view/main/MainActivity.kt`

---

#### Android 10 Black Screen Fix
**Problem:** Apps would show a black screen and timeout on Android 10 (API 29).

**Root Cause:** 
- `BRAttributionSource.getRealClass()` returns `null` on Android < 31
- `SystemProviderStub.invoke()` crashed calling `.getName()` on null class
- `ClassInvocationStub.injectHook()` crashed when `getWho()` returned null

**Solution:**
- Added null checks in `SystemProviderStub.java` for API version checks
- Added null check in `ClassInvocationStub.java` to skip hooks when services don't exist

**Files Changed:**
- `Bcore/src/main/java/top/niunaijun/blackbox/fake/service/context/providers/SystemProviderStub.java`
- `Bcore/src/main/java/top/niunaijun/blackbox/fake/hook/ClassInvocationStub.java`

---

### Removed Features

#### Xposed Framework Support
- Removed `BXposedManagerService` and related AIDL interfaces
- Removed "Install Xposed Module" UI and Settings entries
- Cleaned up Xposed-related flags and package checks

---

### Stability Improvements

#### Anti-Detection Native Hook Stability
- Removed `LOGD` calls from critical native hooks to prevent infinite recursion
- Fixed syntax errors in hook implementations
- Hooks now silently return `ENOENT` for blocked paths

---

### Known Issues

#### Oppo/ColorOS Thermal Stats Error
On Oppo/ColorOS devices, you may see errors like:
```
OppoThermalStats: PackageManager$NameNotFoundException: top.niunaijun.blackboxa:p0
```
**This is harmless** - it's an Oppo system bug where their thermal management incorrectly uses process names (with `:p0` suffix) instead of package names. The app works normally.

---

### Compatibility

| Android Version | Status |
|-----------------|--------|
| Android 10 (Q)  | ✅ Fixed |
| Android 11 (R)  | ✅ Supported |
| Android 12 (S)  | ✅ Supported |
| Android 13 (T)  | ✅ Supported |
| Android 14 (U)  | ✅ Supported |
| Android 15+     | ✅ Supported |
