## Gradle Build

### Toolchain
JDK 21 (Temurin; app/Bcore compile with Java 21 and Kotlin jvmTarget 21; black-reflection/compiler target Java 17), Gradle wrapper 8.14.5, AGP 8.13.2, Kotlin 1.9.23, NDK 29.0.13846066 pinned in app and Bcore. README's "JDK 17" is stale.

### Commands
`./gradlew assembleDebug` / `assembleRelease`; full CI set: `assembleDebug assembleRelease :Bcore:assembleDebug :Bcore:assembleRelease :black-reflection:assemble :compiler:assemble`.

### Dependencies (K2)
Plugins and libraries are declared in `gradle/libs.versions.toml` and referenced via `alias(libs.plugins.*)` / `libs.*`. New dependencies go ONLY into the catalog; migrate existing inline coordinates when touching them.
Repositories are declared centrally in `settings.gradle` with `FAIL_ON_PROJECT_REPOS` (no per-module repositories).

### Shared properties
SDK levels and version (`compileSdkVersion`, `targetSdkVersion`, `minSdk`, `versionCode`, `versionName`) live once in the root `build.gradle` `ext` block and are read via `rootProject.ext.*`.

### Platform targets (K4)
compileSdk 35, targetSdk 28 (deliberately low — the engine relies on pre-Q behaviour; raising it is an engine decision), minSdk 21. ABIs: armeabi-v7a + arm64-v8a (+ universal APK); x86 is not built. Tested on Android 10–15+.
The build config is the source of truth over README/Docs.md claims (JDK 17, API 26, x86, root are stale).

### Module configuration
Groovy DSL for Android modules, Kotlin DSL (`build.gradle.kts`, `java-library`) for pure-Java modules. Enable only needed `buildFeatures` (app: viewBinding; Bcore: aidl, prefab).
Release: `minifyEnabled true` with `proguard-android-optimize.txt` + module rules; Bcore exports `consumer-rules.pro`. APKs are named `BlackBox_<versionName>_<abi>-<buildType>.apk`.

### Properties and lint state
`gradle.properties`: AndroidX, non-transitive R, Jetifier, `-Xmx2048m` UTF-8, incremental + parallel annotation processing.
Bcore lint is relaxed (`abortOnError false`, `checkReleaseBuilds false`) and javac deprecation/unchecked/rawtypes warnings are suppressed because of hidden-API use.
No ktlint/detekt/checkstyle/.editorconfig is configured — don't assume a formatter; follow the style standards manually.

Source: config (build files, gradle.properties, settings.gradle), docs (README).
