# System Architecture

## Overview
KBlackbox has two parts:
- a **host app** (`app`, Kotlin UI) for managing virtual apps
- a **virtualization engine** (`Bcore`, Java + native) that runs guest APKs inside the host's own processes, using stub components and hooked system services

The `black-reflection` and `compiler` modules provide compile-time code generation that the engine uses to access hidden Android framework APIs.

```
┌──────────────────────────── app (Kotlin, host UI) ────────────────────────────┐
│ View (Activities/Fragments, XML + ViewBinding)                                │
│   └─ ViewModel (StateFlow + event Channel, BaseViewModel.launch)              │
│        └─ Repository (AppsRepository, GmsRepository, FakeLocationRepository)  │
│             └─ BlackBoxCore API ─────────────────────────────┐                │
└──────────────────────────────────────────────────────────────┼────────────────┘
                                                               ▼
┌──────────────────────────── Bcore (engine) ───────────────────────────────────┐
│ BlackBoxCore (facade)                                                         │
│ Server side ("system" process): core/system/* virtual services                │
│    BPackageManagerService, BActivityManagerService, BUserManagerService, ...  │
│    location, notification, permission — exposed via AIDL + ServiceManager     │
│ Client side (each virtual app process):                                       │
│    fake/hook   — hook installation framework (binder/method proxies)          │
│    fake/service— I*Proxy classes intercepting system-service binders          │
│    fake/delegate, fake/frameworks — Instrumentation/Callback delegates        │
│    proxy/      — stub Activity/Service/Provider/Receiver declared in manifest │
│ Native (cpp/): Dobby + xDL hooks — IO redirect, DexFile, ClassLoader, Binder, │
│    hidden-API, anti-detection, device spoofing (JNI via NativeCore)           │
└───────────────────────────────────────────────────────────────────────────────┘
          ▲ uses generated accessors
┌─────────┴──── black-reflection (annotations) + compiler (APT, JavaPoet) ──────┐
│ black/android/** interfaces annotated @BClassName/@BField/@BMethod →          │
│ generated *Context/*Static classes for type-safe hidden-API reflection        │
└───────────────────────────────────────────────────────────────────────────────┘
```

## Architecture Pattern
**Pattern**:
- **Host app**: layered MVVM (View → ViewModel → Repository → Engine facade)
- **Engine**: client/server virtualization through binder proxying, stub components and native hooks

## System Structure

### app — Host UI
- **Location**: `app/src/main/java/top/niunaijun/blackboxa/`
- **Purpose**: The user-facing app for installing, launching and managing virtual apps and their spoofing settings.
- **Structure**:
  - `app/`: `App` (Application; calls the `BlackBoxCore` lifecycle hooks in `attachBaseContext`/`onCreate`), `AppManager`, `rocker/` (activity lifecycle callbacks for the floating joystick)
  - `view/main/`: `WelcomeActivity` (splash), `MainActivity` (toolbar, ViewPager2 of user spaces, FAB, permission prompts), `ShortcutActivity`, `BlackBoxLoader`
  - `view/apps/`: `AppsFragment` (grid of virtual apps for one user), `AppsAdapter`, `AppsViewModel`, `AppsTouchCallBack` (drag-to-reorder)
  - `view/list/`: `ListActivity` (picker for installed device apps and APK files)
  - `view/fake/`: `FakeManagerActivity` (osmdroid map for fake location)
  - `view/gms/`: `GmsManagerActivity`
  - `view/setting/`: `SettingActivity` + `SettingFragment` (preferences)
  - `view/base/`: `BaseActivity` (toolbar helper), `LoadingActivity`, `BaseViewModel`
  - `data/`: repositories that wrap `BlackBoxCore` calls
  - `bean/`: UI models (`AppInfo`, `InstalledAppBean`, `GmsBean`, `FakeLocationBean`, `XpModuleInfo`)
  - `util/`: `InjectionUtil` (manual DI; singleton repositories + `ViewModelProvider` factories), `ViewBindingEx` (`by inflate()`), `ToastEx`, `ShortcutUtil`, `ResUtil`
  - `widget/`: `EnFloatView`, `RockerView` (floating joystick overlay)
- **Resources**: `res/layout` (17 layouts), `res/menu` (4 menus), `res/values` (`themes.xml`, `colors.xml`, `dimens.xml`, `strings.xml`), `values-night` (night palette), `values-v23` (status bar), `drawable-night`, plus `values-zh-rCN` and `values-zh-rTW` translations.

### Bcore — Virtualization engine
- **Location**: `Bcore/src/main/java/top/niunaijun/blackbox/`
- **Purpose**: Installs and runs APKs virtually and isolates their data, identity and system-service view.
- **Key parts**:
  - `BlackBoxCore.java`: the public facade (`installPackageAsUser`, `launchApk`, `uninstallPackageAsUser`, lifecycle hooks, users, GMS)
  - `core/system/`: virtual system server (`BlackBoxSystem`, `ServiceManager`, `BProcessManagerService`, subpackages `am`, `pm`, `user`, `accounts`, `location`, `notification`, `permission`, `os`)
  - `core/`: `NativeCore` (JNI bridge), `IOCore` (path redirection), `GmsCore`, `CrashHandler`, `env/`
  - `fake/hook/`: base classes for binder and method-invocation hooks
  - `fake/service/`: ~60 `I*Proxy` interceptors (ActivityManager, PackageManager, Location, Telephony, etc.)
  - `proxy/`: stub components that host guest activities, services, providers and receivers. The Bcore manifest declares ~300 components.
  - `entity/`, `utils/`, `util/`: data classes and helpers
  - `black/**` and `android/**` (top-level packages): reflection declarations and compile-only stubs of hidden framework classes
  - `src/main/aidl/`: IPC contracts between guest processes and the virtual system server
  - `src/main/cpp/`: `BoxCore.cpp`, `IO.cpp`, `hidden_api.cpp`, `Hook/` (DexFile, FileSystem, VMClassLoader, UnixFileSystem, Binder), `JniHook/`, `Utils/` (AntiDetection, VirtualSpoof, elf_util), vendored `Dobby/` and `xdl/`

### black-reflection — Annotations
- **Location**: `black-reflection/src/main/java/top/niunaijun/blackreflection/`
- **Purpose**: Annotation definitions and a runtime helper for type-safe reflection.

### compiler — Annotation processor
- **Location**: `compiler/src/main/java/top/niunaijun/blackreflection/`
- **Purpose**: `BlackReflectionProcessor` (registered with AutoService) generates proxy classes with JavaPoet from the annotated interfaces in `Bcore/black/**`.

## Data Flow
**Example: installing and launching a virtual app from the UI**
1. The user picks an app or APK in `ListActivity`. The result goes back to `AppsFragment`, which calls `AppsViewModel.install(source, userId)`.
2. `AppsViewModel` runs `AppsRepository.installApk()` in a coroutine (`BaseViewModel.launch`, the repository switches to IO). The repository calls `BlackBoxCore.installPackageAsUser()`.
3. `BPackageManagerService` in the engine parses the APK, copies it into the virtual file system and records the package settings.
4. The result message is sent as an `AppsEvent.Message`, and the fragment reloads `AppsViewModel.apps`.
5. On launch, `BlackBoxCore.launchApk()` asks `BActivityManagerService` to start a stub `ProxyActivity` in a stub process. The guest process initializes, the hooks are installed (Java proxies + native), and the guest's real Activity is instantiated inside the stub.

## External Integrations
- **Android framework**: accessed through hidden APIs (FreeReflection plus generated accessors) and binder interception
- **Google Mobile Services**: optional GMS install and support (`GmsCore`, `GmsManagerActivity`)
- **OpenStreetMap tiles**: osmdroid for the fake-location map

## Configuration
- Build: the root `build.gradle` `ext` (SDK levels, version), `gradle.properties` (JVM args, AndroidX), `gradle/libs.versions.toml`
- Runtime (app): SharedPreferences through `AppSharedPreferenceDelegate`, preference screens (`res/xml`)
- Runtime (engine): `JarConfig` / TOML config, per-user virtual file system

## Deployment Architecture
- An APK per ABI (armeabi-v7a, arm64-v8a) plus a universal APK, built by GitHub Actions and uploaded as GitHub Actions artifacts
- `Bcore`, `black-reflection` and `compiler` also ship as AAR/JAR artifacts for embedding in other host apps
- No backend or server component

## UI Development Notes (focus area)
- Add new screens under `view/<feature>/` with the matching `Activity`/`Fragment`, `ViewModel`, `Adapter`, `Factory` set, and register the factory in `InjectionUtil`.
- UI code should talk to the engine **only through repositories** in `data/`. Do not call `BlackBoxCore` from views.
- The theme is defined in `res/values/themes.xml`: `Base.Theme.BlackBox` (parent `Theme.Material3.DayNight.NoActionBar`) → `Theme.BlackBox`, `WelcomeTheme`, and `ThemeOverlay.BlackBox.Toolbar`, plus typography/shape tokens and `dimens.xml`. The 34 M3 color roles are `md_theme_*` colors in `values/colors.xml` (day) and `values-night/colors.xml` (night), generated from seed `#3F6FD8`; views use `?attr/color*`, never hex.
- Dark mode follows the Settings → Appearance → Theme preference (`theme_mode`: System default / Light / Dark), stored in `BlackBoxLoader` (`AppSharedPreferenceDelegate`) and applied with `AppCompatDelegate.setDefaultNightMode`. `App.onCreate` applies it and enables `DynamicColors` (API 31+) only in the main process, for host activities only.

---
*Based on codebase analysis performed 2026-09-26.*
