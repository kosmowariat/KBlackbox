# Documentation Index

**IMPORTANT**: Read this file at the beginning of any development task to understand available documentation and standards.

**Project**: KBlackbox, an Android virtual-app sandbox engine (fork of ALEX5402/NewBlackbox). Modules: `app` (Kotlin UI), `Bcore` (Java/C++ virtualization engine), `black-reflection` and `compiler` (reflection annotation + annotation processor).

**Key team decisions** (reflected across the standards):
- **K1 Error handling is split**: the engine (Bcore hooks/proxies) degrades gracefully, never crashes the guest app, and logs via `Slog`. The app UI uses no blanket try/catch and shows failures to the user through string resources.
- **K2 Dependencies**: new dependencies go only into `gradle/libs.versions.toml`; existing inline ones are migrated when touched.
- **K3 UI conventions**: standards describe the target direction; the current pattern is marked "Legacy (migrate when touching)".
- **K4 Platforms**: the build config is the source of truth (minSdk 21, ARM only; tested on Android 10–15+). README/Docs.md claims (JDK 17, API 26, x86, root) are stale.
- **K5 Tests**: encouraged, not required (for now).

## Quick Reference

### Project Documentation
Project-level documentation covering vision, goals, architecture, and technology choices.

### Technical Standards
Coding standards, conventions, and best practices organized by domain: global, frontend (web baseline + Android `app` module), engine (`Bcore` Java), native (`Bcore` C++), build (Gradle/CI), docs (release notes), and testing.

---

## Project Documentation

Located in `.maister/docs/project/`

### Vision (`project/vision.md`)
What KBlackbox is: an Android sandbox that runs APKs without really installing them, and a solo, independent fork of ALEX5402/NewBlackbox (no upstream sync). Covers the current state (v4.0.0, active), the target users (end users cloning or sandboxing apps, and developers embedding `Bcore`) and the product purpose (multi-instance, device spoofing, GMS, Xposed-style modules). The 6–12 month goal is to improve the GUI of the `app` module (modern look, dark mode, better UX for the core flows, a cleaner MVVM UI layer) while keeping the engine stable.

### Roadmap (`project/roadmap.md`)
The current feature set and upstream history, then planned work. High priority (GUI): Material 3 visual refresh and design tokens, dark mode, app grid and main screen UX, moving hard-coded strings into resources, settings redesign. Medium priority (UI architecture): updating lifecycle/preference/recyclerview/work, replacing material-dialogs and other niche UI libraries, StateFlow-based MVVM, evaluating Compose. Technical debt: no tests, CI without lint or tests, lint disabled in `Bcore`, ad-hoc `try/catch` error handling, debug-signed release builds. Also covers future ideas and why `targetSdk` stays at 28.

### Tech Stack (`project/tech-stack.md`)
Languages: Java (engine and codegen), Kotlin 1.9.23 (UI), C/C++ via NDK and `ndk-build` (native hooks). UI stack: Views + XML + ViewBinding, AppCompat, Material Components 1.12 (Material 3 DayNight theme with `values-night` and dynamic color), lifecycle 2.3.1, ViewPager2, osmdroid, plus third-party UI libraries. Engine: virtual system services, binder proxies, stub components, AIDL, FreeReflection, Dobby/xDL. Also covers the JavaPoet/AutoService annotation processor, testing (declared but effectively absent), state storage (no SQL database), Gradle 8.14 / AGP 8.13 with a partly used version catalog, SDK levels (compile 35, target 28, min 21), GitHub Actions build CI with artifact upload, debug-signed release builds, no linters, and a key dependency table.

### Architecture (`project/architecture.md`)
The system diagram and patterns: layered MVVM in the host app (View → ViewModel → Repository → `BlackBoxCore` facade), and client/server virtualization in `Bcore` (virtual system services, `I*Proxy` binder hooks, manifest stub components, native Dobby/xDL hooks) driven by generated hidden-API accessors from `black-reflection`/`compiler`. Includes a package-by-package map of `app` and `Bcore`, the install-and-launch data flow, external integrations, configuration, and deployment (per-ABI APKs plus AAR/JAR artifacts). UI development notes: where new screens go (`view/<feature>/` with a Factory registered in `InjectionUtil`), UI reaches the engine only through repositories in `data/`, and where the theme and color resources live.

---

## Technical Standards

### Global Standards

Located in `.maister/docs/standards/global/`

These standards apply to the whole codebase: Kotlin, Java, and C++ modules alike.

#### Coding Style (`standards/global/coding-style.md`)
Consistent naming across the codebase, automatic formatting/linting, descriptive names, small focused functions, uniform indentation, removal of dead code and commented-out blocks, no backward-compatibility shims unless required, DRY extraction of repeated logic.

#### Java Style (`standards/global/java-style.md`)
4-space indentation with K&R braces and no wildcard imports; import groups ordered `android`/`androidx` → `java`/`javax` → `black` → `top` → static; `m` prefix for private/protected instance fields, `s` for static non-final fields, `UPPER_SNAKE_CASE` constants, plain names for public entity fields.

#### Kotlin Style (`standards/global/kotlin-style.md`)
4-space indentation and 4-space continuation indent, one primary class per file, no runs of empty lines; `data class` beans, `object` for stateless managers/utilities, top-level extension helpers (`toast`, `getString`, `inflate`); `private const val TAG` in a companion object with `Log.e(TAG, "Error <doing X>", e)`, `Log.w` for degraded paths, `Log.d` for flow.

#### Commenting (`standards/global/commenting.md`)
Let code explain itself through naming and structure, comment sparingly (only non-obvious "why"), no comments describing changes or history (that belongs in version control).

#### Conventions (`standards/global/conventions.md`)
Predictable project structure, up-to-date docs, clean version control with meaningful commit subjects, secrets never committed, minimal dependencies added only via `gradle/libs.versions.toml` with `libs.*` references (K2), consistent code reviews, tests encouraged but not required for now (K5), feature flags for incomplete work, release notes for significant changes, build only what is needed, Apache 2.0 licensing with upstream credits kept in the README, and LF line endings via `.gitattributes`.

#### Error Handling (`standards/global/error-handling.md`)
Clear user-facing messages without internals, fail fast on invalid state, typed/specific exceptions, centralized handling at boundaries, graceful degradation, retry with exponential backoff, resource cleanup in finally blocks. Adds the engine vs UI split (K1): Bcore hooks catch, log via `Slog` and fall back to the original call or a neutral default, and never crash the guest app; the app UI fails visibly with string-resource messages and no blanket try/catch.

#### Minimal Implementation (`standards/global/minimal-implementation.md`)
Build only what the task needs, every method has a clear purpose/caller, delete exploration artifacts, no stubs for future features, no speculative abstractions, review the diff before committing, treat unused code as debt.

#### Validation (`standards/global/validation.md`)
Authoritative validation on the trusted side, client-side checks for feedback only, validate early at boundaries, specific field-level error messages, allowlists over blocklists, type and format checks, input sanitization against injection, business-rule validation, consistent enforcement across entry points.

---

### Frontend Standards

Located in `.maister/docs/standards/frontend/`

These standards apply to UI code in the Kotlin `app` module. The Android-specific files (`android-*.md`) are the primary UI standards for this project. The web-baseline files (accessibility, components, css, responsive) keep their generic principles and each ends with an "Android interpretation (this project)" section that maps them to Android idioms.

#### Android UI Architecture (`standards/frontend/android-architecture.md`)
Package-by-feature under `view/<feature>/` with role class suffixes (`*Activity`, `*ViewModel`, `*Factory`, `*Repository`, `*Bean`, `*VH`). Views reach the engine only through `data/` repositories. ViewModels are created via `InjectionUtil` factories with the `[ ]` indexer. Target state exposure is read-only `StateFlow`/`LiveData` with `suspend` repositories (legacy `MutableLiveData` passing and `launchOnUI` are migrated when touched). Observe state with lifecycle-owner lambdas, and UI error handling follows K1 (catch at the boundary, show string-resource messages, never fail silently).

#### Android Views & Screens (`standards/frontend/android-views.md`)
ViewBinding via `by inflate()` (field `viewBinding`, no `findViewById`), the `view_toolbar` include with `initToolbar(...)`, `BaseActivity`/`LoadingActivity` base classes, and `onCreate` split into `init*()` helpers. Lists target `ListAdapter` + `DiffUtil` (cbfg `RVAdapter` is legacy) with an explicit LayoutManager. Screens expose `companion start(context)`/`newInstance` helpers with shared intent-extra constants. Results go through `registerForActivityResult` and `finishWithResult()`, and back handling uses `OnBackPressedDispatcher`.

#### Dialogs, Toasts & Loading States (`standards/frontend/android-feedback.md`)
Dialogs target `MaterialAlertDialogBuilder` with string resources (afollestad `MaterialDialog` is legacy, so no new usages). Toasts go only through the `util/ToastEx.kt` helpers, never `Toast.makeText`. List screens use StateView loading/content/empty states with an explicit empty state, and blocking operations use `LoadingActivity.showLoading()`.

#### Android Resources, Theming & i18n (`standards/frontend/android-resources.md`)
Layout names use `activity_`/`fragment_`/`item_`/`view_` prefixes. Resource names are snake_case with semantic color names; view ids are camelCase. The target theme is Material 3 with a color scheme, `values-night` and dark mode, and colors come only from theme attributes/tokens. Dimens are tokens on an 8dp grid with `TextAppearance` typography. Screens use a `ConstraintLayout` root and Material widgets. All user-visible text lives in `strings.xml` and is mirrored in `values-zh-rCN`/`values-zh-rTW`. Settings use `PreferenceFragmentCompat` + `SwitchPreferenceCompat` (engine settings need a restart). Covers the namespace/applicationId layout.

#### Accessibility (`standards/frontend/accessibility.md`)
Semantic elements, keyboard navigation, sufficient color contrast, labels for all controls/images, screen-reader testing, ARIA only when needed, logical heading structure, focus management. On Android this means `contentDescription` from strings (decorative images `@null`/`importantForAccessibility="no"`, no "TODO" descriptions), 48dp touch targets, contrast checks on the theme colors, and TalkBack verification.

#### Components (`standards/frontend/components.md`)
Single-responsibility, reusable, composable components with clear, encapsulated interfaces, consistent naming, local state, and minimal parameters. On Android, components are custom Views in `widget/`, reusable `view_*` include layouts and adapters, configurable via attributes/parameters.

#### CSS (`standards/frontend/css.md`)
One consistent styling methodology, work with the framework, design tokens, minimal custom styling, production optimization. On Android, styling means themes, styles, `TextAppearance`, and color/dimen resources, with tokens in `colors.xml`/`dimens.xml`/`themes.xml` and no inline styling when a token exists.

#### Responsive Design (`standards/frontend/responsive.md`)
Mobile-first, standard breakpoints, fluid layouts, relative units, cross-device testing, touch-friendly targets, mobile performance, readable typography, content priority. On Android: dp/sp units, `ConstraintLayout`, `-land`/`-sw600dp` resource qualifiers, RTL-safe `start`/`end`, and testing on small phones and large screens.

---

### Engine Standards

Located in `.maister/docs/standards/engine/`

These standards apply to the Java virtualization engine in the `Bcore` module (hooks, virtual services, reflection mirrors, runtime), plus the `black-reflection` and `compiler` modules.

#### Engine Hooks (`standards/engine/hooks.md`)
Binder proxies are `I<Service>Proxy extends BinderInvocationStub`, registered in `HookManager.init()` (SDK-gated). Method hooks are nested `@ProxyMethod` `MethodHook` classes that rewrite args via `MethodParameterUtils` and delegate with `method.invoke`. `ValueMethodProxy`/`PkgMethodProxy`/`UidMethodProxy` cover trivial hooks, and an `invoke()` override handles cross-cutting rewriting. Non-binder hooks use `ClassInvocationStub` and `@ScanClass` holders. Null-check reflection handles and skip the hook if they are absent. Gate by SDK with `BuildCompat.isX()`, noting the off-by-one bug in `isTiramisu()`/`isU()`.

#### Virtual System Services & Engine API (`standards/engine/services.md`)
Each service is a triad: an AIDL `IB<Name>Service`, a `B<Name>Service` singleton registered in `ServiceManager`, and a client `B<Name>Manager`. AIDL methods take `userId` last with `in` Parcelables. Client wrappers catch `RemoteException` and return neutral defaults. Parcelable entities live in `entity/` with public fields. Stub components are `Proxy<Type>` with `P0..P49` subclasses (processes `<host>:pN`). Singletons use an eager `get()`. `BlackBoxCore.get()` is the host entry point, operations are scoped by `userId`, and `InstallResult.success` must always be checked.

#### Hidden-API Reflection (`standards/engine/reflection.md`)
Mirror interfaces go in `black.<package>` with `@BClassName` and `@BField`/`@BMethod`/`@BConstructor`/`@BParamClassName` annotations, versioned interfaces for SDK/OEM differences, and meaningful parameter names. Access goes through generated `BR*` accessors (`get()`, `_set_`, `_check_`), with raw reflection only as a fallback. The `compiler` annotation processor (AutoService + JavaPoet) generates the accessors. ProGuard keep rules are required for new reflection-driven classes outside the kept trees.

#### Engine Runtime Rules (`standards/engine/runtime.md`)
Engine logging uses `Slog` with a class-name `TAG` for logcat filtering. Xposed support, the VPN mode and the log upload were removed and must not be reintroduced; its leftover UI resources are dead code. Bcore is distributed as an AAR, and Docs.md APIs must be verified against the source.

---

### Native Standards

Located in `.maister/docs/standards/native/`

These standards apply to the C/C++ code in `Bcore/src/main/cpp` (native hooks, JNI), excluding the vendored `Dobby/` and `xdl/` sources.

#### Native Code (`standards/native/native.md`)
ndk-build with C++17, hidden visibility, `c++_static`, android-24, ARM ABIs only, 16KB page alignment, and every `.cpp` listed in `LOCAL_SRC_FILES`. JNI registration is dynamic (`RegisterNatives` in `JNI_OnLoad`, with no `Java_*` exports). Hook modules are `<Area>Hook : BaseHook` classes using `HOOK_JNI` and the `orig_`/`new_` naming. Critical libc hooks must not log (to avoid recursion). Elsewhere, use `ALOGD`/`ALOGE` with a module prefix. Headers use `#ifndef` guards and `#include`, and vendored libraries are never modified.

---

### Build Standards

Located in `.maister/docs/standards/build/`

These standards apply to the Gradle build of all modules, the build configuration files, and the GitHub Actions CI/distribution pipeline.

#### Gradle Build (`standards/build/gradle.md`)
Toolchain: JDK 21, Gradle 8.14.5, AGP 8.13.2, Kotlin 1.9.23 and a pinned NDK. Covers the standard assemble commands and the full CI command set. Dependencies are declared only in the version catalog, with central repositories (`FAIL_ON_PROJECT_REPOS`) (K2). SDK/version properties are shared in the root `ext` block. Platform targets are compileSdk 35, targetSdk 28 (deliberate) and minSdk 21, ARM ABIs only, and the build config beats README claims (K4). Groovy DSL is used for Android modules and KTS for pure-Java ones, with minimal `buildFeatures`, minified releases and the APK naming scheme. Documents the `gradle.properties` settings and relaxed Bcore lint, and notes that no formatter or linter is configured.

#### CI & Distribution (`standards/build/ci.md`)
The `build.yml` workflow (push to `main`/manual) runs `app` lint and unit tests, builds all APKs/AARs/JARs with JDK 21 and uploads them with upload-artifact as 4 GitHub Actions artifacts (30-day retention), downloadable from the run page. The workflow needs no secrets, and secrets are never committed.

---

### Docs Standards

Located in `.maister/docs/standards/docs/`

These standards apply to the repository's own documentation: `RELEASE_NOTES.md`, `README.md`, and `Docs.md`.

#### Release Notes & Project Docs (`standards/docs/release-notes.md`)
Release-note entries have a dated version header and fixed sections (New Features, Bug Fixes, Removed Features, Stability Improvements, Known Issues, Compatibility table). Bug fixes use the Problem / Root Cause / Solution / Files Changed format. Harmless OEM log noise is documented under Known Issues with the exact log line. The README/Docs.md split must stay consistent with the build config (stale claims noted).

---

### Backend Standards

*Not initialized for this project. If you need backend standards, you can:*
- *Add them manually using the docs-manager skill*
- *Run `/maister:standards-discover --scope=backend` to auto-discover*

---

### Testing Standards

Located in `.maister/docs/standards/testing/`

These standards apply to all testing code (unit, instrumented, integration).

#### Test Writing (`standards/testing/test-writing.md`)
Test behavior rather than implementation details, clear descriptive test names, mock external dependencies, fast tests, risk-based prioritization, balance coverage with velocity, focus on critical paths, appropriate test depth. Current state: a small JVM unit-test suite exists in `app/src/test` and runs in CI. Tests are encouraged, not required (K5). Unit tests go in `app/src/test/...` (JUnit 4) and instrumented tests in `app/src/androidTest/...`, with ViewModels and repositories as the first targets. The engine is verified manually via `adb logcat` per Android version.

---

## How to Use This Documentation

1. **Start Here**: Always read this INDEX.md first to understand what documentation exists
2. **Project Context**: Read relevant project documentation before starting work
3. **Standards**: This index only points to the standards — open and follow the specific standard files relevant to your task; don't rely on the index alone
4. **Keep Updated**: Update documentation when making significant changes
5. **Customize**: Adapt all documentation to your project's specific needs

## Updating Documentation

- Project documentation should be updated when goals, tech stack, or architecture changes
- Technical standards should be updated when team conventions evolve
- Always update INDEX.md when adding, removing, or significantly changing documentation

---

**Last Generated**: 2026-09-27
**Maintained by**: Documentation Manager skill
