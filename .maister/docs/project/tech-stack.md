# Technology Stack

## Overview
This document describes the technology choices for KBlackbox. The project is a multi-module Gradle build with four modules: `app`, `Bcore`, `black-reflection` and `compiler`.

## Languages

### Java (JDK 21 target for Android modules, 17 for pure-Java modules)
- **Usage**: ~60% of the codebase. It covers the whole `Bcore` engine (~500 files) and the `black-reflection` and `compiler` modules.
- **Rationale**: Inherited from the original BlackBox engine. Java maps directly onto Android framework internals, which the engine hooks and proxies.

### Kotlin (1.9.23)
- **Usage**: ~8%. It covers the `app` module UI (~50 files).
- **Key features used**: Coroutines and Flow (`BaseViewModel.launch`, `StateFlow`, `Channel`), property delegates (`by inflate()` for ViewBinding, `AppSharedPreferenceDelegate`) and extension functions.

### C / C++ (NDK 29.0.13846066)
- **Usage**: ~30% by volume, mostly vendored code. It lives in `Bcore/src/main/cpp`.
- **Rationale**: Native hooks for file-system redirection, DexFile, ClassLoader, Binder, hidden-API access and anti-detection.
- **Build**: `ndk-build` (`Android.mk`, `Application.mk`).

## Frameworks

### Frontend (Android UI — `app`)
- Android Views + XML layouts + **ViewBinding**
- AndroidX AppCompat 1.7.0, Material Components 1.12.0 (Material 3 theme `Theme.Material3.DayNight.NoActionBar`: `md_theme_*` palette from seed `#3F6FD8` in `values/` + `values-night/`, dynamic color on API 31+ in the main process, theme mode System/Light/Dark stored via `BlackBoxLoader` and applied with `AppCompatDelegate.setDefaultNightMode`), ConstraintLayout 2.2.0, core-ktx 1.15.0
- Lifecycle ViewModel/Runtime KTX 2.8.7 (MVVM with manual `ViewModelProvider` factories)
- ViewPager2 (multi-user pages), RecyclerView 1.3.2, Preference KTX 1.2.1, WorkManager 2.9.1
- Third-party UI: none beyond StateView (local AAR), osmdroid and the floating-view library
- Maps: osmdroid 6.1.11 (fake location)

### Engine (`Bcore`)
- A custom virtualization framework made of:
  - server-side virtual system services (`core/system`: `BPackageManagerService`, `BActivityManagerService`, users, accounts, location, notifications)
  - client-side binder proxies (`fake/service/I*Proxy.java`)
  - stub components declared in the manifest (`proxy/`)
  - AIDL IPC (`src/main/aidl`)
- FreeReflection 3.2.2 for hidden-API bypass
- Native: Dobby (prebuilt static libraries for inline hooking), xDL (dynamic-linker helpers)

### Code generation (`black-reflection` + `compiler`)
- A custom annotation processor: `@BClassName`, `@BField`, `@BMethod`, `@BStaticMethod`, `@BConstructor`, etc.
- JavaPoet 1.13.0 generates the code, and Google AutoService 1.1.1 registers the processor.
- The generated accessors let `Bcore` read and write hidden framework members without hand-written reflection. The declarations live in `Bcore/src/main/java/black/**`.

### Testing
- Declared: JUnit 4.13.2, AndroidX Test ext-junit 1.2.1, Espresso 3.6.1, AndroidJUnitRunner
- Actual coverage: a small JVM suite in `app/src/test` (file copying for user duplication, math helpers), run in CI together with `app` lint. No `src/androidTest` yet; `JarManagerTest.java` in Bcore `src/main` is not a real test.

## Database
There is no SQL database. State lives in:
- the engine's own files under the virtual file system (package settings, user data)
- SharedPreferences in the app, through `AppSharedPreferenceDelegate`
- TOML config through toml4j 0.7.2

## Build Tools & Package Management
- Gradle 8.14.5 (wrapper), Android Gradle Plugin 8.13.2
- Version catalog `gradle/libs.versions.toml`. It is only partly used: many dependencies are still declared inline in `app/build.gradle`.
- Shared SDK and version properties come from `ext` in the root `build.gradle`.
- Groovy DSL for `app` and `Bcore`; Kotlin DSL (`build.gradle.kts`) for `black-reflection` and `compiler`.
- Repositories: Google, Maven Central, JitPack.

## Android SDK Levels
| Setting | Value | Note |
|---|---|---|
| compileSdk | 35 | |
| targetSdk | 28 | Kept low on purpose; the engine relies on pre-Q behaviors |
| minSdk | 21 | |
| ABIs | armeabi-v7a, arm64-v8a (+ universal APK) | ABI splits enabled in `app` |

## Infrastructure

### CI/CD
- GitHub Actions: `.github/workflows/build.yml`. It runs on push to `main` and on manual dispatch.
- JDK 21 (Temurin) and the Android SDK are set up, and Gradle caching is on.
- It builds `assembleDebug assembleRelease` for all modules, then uploads the APKs, AARs and JARs as GitHub Actions workflow artifacts.
- It runs no tests and no lint.

### Signing
- Release builds are signed with a keystore given by `APKENCLAVE_*` Gradle properties or environment variables (`app/build.gradle`); without them they fall back to the **debug** signing config. The debug build type has the applicationId suffix `.debug` and is a separate app.

## Development Tools

### Linting & Formatting
- No ktlint, detekt, spotless or checkstyle is configured.
- Android Lint runs with defaults. `Bcore` disables `checkReleaseBuilds`.
- ProGuard/R8 is enabled for `app` release (`minifyEnabled true`).

## Key Dependencies
| Area | Dependency | Version |
|---|---|---|
| UI | androidx.appcompat | 1.7.0 |
| UI | com.google.android.material | 1.12.0 |
| UI | androidx.constraintlayout | 2.2.0 |
| Arch | androidx.lifecycle (*-ktx) | 2.8.7 |
| Maps | osmdroid-android | 6.1.11 |
| Engine | FreeReflection | 3.2.2 |
| Codegen | javapoet / auto-service | 1.13.0 / 1.1.1 |
| Config | toml4j | 0.7.2 |

## Version Management
- `versionCode` and `versionName` are set in the root `build.gradle` `ext` (400 / 4.0.0).
- Library versions come partly from `libs.versions.toml` and partly from inline strings.

---
*Last Updated*: 2026-09-26
*Auto-detected*: all of the above, from the build files, the manifest and the sources. *User-provided*: the fork context (solo, independent of upstream) and the focus on GUI improvement.
