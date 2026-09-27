# Codebase Analysis Report

**Date**: 2026-09-27
**Task**: Material 3 theme migration + dark mode for the host `app` module
**Description**: Migrate the Android host app theme (module `app`, D:\KBlackbox) from Theme.MaterialComponents.Light.NoActionBar to Material 3 with a real color scheme, typography/shape tokens, a values-night dark theme, and removal of forceDarkAllowed=false.
**Analyzer**: codebase-analyzer skill (2 Explore agents: File Discovery + Code Analysis (combined), Context Discovery)

---

## TL;DR
- The theme swap is cheap: MDC 1.12.0 already ships `Theme.Material3.DayNight.*`, `MaterialSwitch` and surface-container roles, so no dependency bump is needed. `values/` currently has only `themes.xml` (31 lines), `colors.xml` (9 lines, one gray `#3F3F3F` used for all three primary tones) and strings. There is no `values-night` folder and no night-mode code.
- The real work is about 20 hard-coded color usages. The worst are `#212121` text in 5 item layouts, a white-on-`@color/primary` toolbar and search card, a white splash, and a light `ic_empty` illustration. There are also third-party widgets that read `colorPrimary` (dotsindicator, material-dialogs buttons), where a `#3F3F3F` primary would be almost invisible on a dark surface.
- The host theme does not leak into guest apps. Bcore stubs have explicit themes, and guest classloaders do not share the host's AppCompat. The one host-visible Bcore screen, `LauncherActivity`, hard-codes white and black, so it will flash white in dark mode unless Bcore resources are changed.
- There are no tests of any kind in `app`, so verification is manual (light and dark on 8 screens, plus dialogs, loading and empty states).
- Overall: **Moderate** complexity, **Medium** risk.

## Key Decisions
- **Stay on MDC 1.12.0 and use `Theme.Material3.DayNight.NoActionBar` as the parent.** It is already on the classpath and minSdk 21 is supported. Dynamic color (API 31+) is optional and needs a runtime guard.
- **Define a real M3 color scheme in `values/colors.xml` and `values-night/colors.xml` and reference it through theme attributes.** Layouts currently have zero `?attr/` references, so every hard-coded color has to move to `?attr/colorOnSurface`, `?attr/colorSurface*`, `?attr/colorPrimary` and similar.
- **The `LoadingActivity.kt:19` conflict is resolved: NOT a bug (verified by bytecode).** `CatLoadingView.setBackgroundColor(int)` only stores the value in field `color`. `onCreateDialog` then calls `ContextCompat.getColor(view.getContext(), color)` and tints the library's background drawable with `DrawableCompat.setTint`. The parameter is therefore a color **resource id**, so passing `R.color.primary` is correct. Because it is resolved per configuration, a night-qualified `@color/primary` (or a new semantic color resource) is picked up automatically. Agent 2 was right and Agent 1's claim is withdrawn.
- **Apply the night mode outside the stub processes.** `App.onCreate` runs in every virtual `:pN` process. Gate `AppCompatDelegate.setDefaultNightMode` to the main process, or apply it in `WelcomeActivity`/`BaseActivity`.
- **Keep new style names distinct from Bcore's `BTheme` and `LauncherTheme`.** Library resources merge into the app namespace, so a same-named app style would override Bcore's.

## Open Questions / Risks
- **Toolbar direction (product decision):** keep a colored app bar (`colorPrimary` background, `colorOnPrimary` content) or move to the M3 default surface toolbar. This choice drives `ToolBarColorStyle`, `view_toolbar.xml`, the `ic_search` white tint, `statusBarColor` and `windowLightStatusBar`.
- **Brand palette:** the current "primary" is neutral gray `#3F3F3F`. M3 needs a real seed color. Should dynamic color (Material You) be enabled on API 31+?
- **User-facing theme setting** (System/Light/Dark `ListPreference`): the task does not mention it, but the roadmap's settings redesign implies it. Confirm whether it is in scope. If so, it must not reuse the `invalidHideState` → restart-toast path in `SettingFragment`.
- **Bcore `LauncherActivity` white flash:** it runs in the host main process on every guest launch. Fixing it needs changes to Bcore resources (DayNight `LauncherTheme`, attribute colors in `activity_launcher.xml`), which falls outside the "app module only" focus. The scope needs a decision.
- **material-dialogs 3.3.0** (12 call sites, unmaintained): it will switch MD_Light/MD_Dark on its own, but it will not look like M3, and its buttons use `colorPrimary`. Either theme it (`md_background_color`, `md_color_button_text`, `md_corner_radius`) or migrate to `MaterialAlertDialogBuilder` (roadmap Medium item). Decide what is in scope.
- **SimpleSearchView 0.2.0** shows a white card with dark icons in dark mode. It stays readable but is unthemed, and needs explicit tint, hint and background attributes.
- **No automated tests**, and the roadmap lists lint as disabled or absent. Regressions show up only through manual visual QA.
- **Accessibility:** a 4.5:1 contrast ratio is required in both modes. `CornerLabelView` and `CatLoadingView` draw white text on `@color/primary`, so a light night primary would break contrast.

---

## Summary

The host app's theming is small and shallow: one app theme with MaterialComponents attributes, one toolbar overlay, one splash theme and 7 color resources, with no attribute-based colors anywhere in layouts. Upgrading the theme parent is trivial. The effort goes into (a) building a complete M3 light and dark color scheme plus type and shape tokens, (b) replacing hard-coded colors in about 10 layouts, 4 vector drawables and 2 Kotlin files with theme attributes, (c) setting explicit colors on third-party widgets that are not theme-aware, and (d) handling splash and status bar behavior in dark mode. Guest-app isolation is not at risk.

---

## Files Identified

### Primary Files

**app/src/main/res/values/themes.xml** (31 lines)
- `Theme.BlackBox` (L3), parent `Theme.MaterialComponents.Light.NoActionBar`:
  - `colorPrimary`/`colorPrimaryVariant`/`colorOnPrimary` (L5-7)
  - `colorSecondary`/`colorSecondaryVariant`/`colorOnSecondary` (L9-11)
  - `android:statusBarColor=?attr/colorPrimaryVariant` (L13)
  - `actionMenuTextColor=@color/primary_text` (L16)
  - `android:forceDarkAllowed=false` (L18)
- `ToolBarColorStyle` (L21), parent `ThemeOverlay.AppCompat.ActionBar`: white `actionMenuTextColor` and `colorControlNormal`, `forceDarkAllowed=false` (L25).
- `WelcomeTheme` (L28), parent `Theme.AppCompat.Light.NoActionBar`: `windowBackground=@drawable/splash`.
- The migration centers on this file. It defines no surface, background, error, `windowLightStatusBar` or `navigationBarColor` roles.

**app/src/main/res/values/colors.xml** (9 lines)
- Contents: `primary`, `primary_dark` and `primary_light` all `#3F3F3F`; `accent #ffffff` (unused); `primary_text #212121`; `secondary_text #757575`; `white`.
- It needs a full M3 role palette and a `values-night` counterpart.

**app/src/main/res/layout/view_toolbar.xml** (14 lines)
- L8 `android:theme=ToolBarColorStyle`, L9 `background=@color/primary`, L11-12 title and subtitle `@color/white`. No `app:popupTheme`.
- Included by activity_main, activity_list, activity_gms, activity_setting and activity_xp (the last is unused).

**app/src/main/res/layout/item_app.xml** (41), **item_fake.xml** (52), **item_package.xml** (53), **item_gms.xml** (25)
- `CornerLabelView` uses `bg_color=@color/primary` and `text_color=@color/white` (item_app L25/L29, item_fake L25, item_package L25).
- Hard-coded `textColor=@color/primary_text` (item_app L38, item_fake L43, item_package L43, item_gms L16) becomes unreadable in dark mode.
- item_gms L20 uses the framework `android.widget.Switch`. The standard calls for `MaterialSwitch`.

**app/src/main/res/layout/activity_list.xml** (43 lines)
- L21/L27: `SimpleSearchView` (type=card) with `background=@color/primary`. Shared by ListActivity and FakeManagerActivity.

**app/src/main/res/layout/activity_main.xml** (59 lines)
- L33 `WormDotsIndicator` sets no colors, so it defaults to `colorPrimary`. L49 `FloatingActionButton` becomes a rounded square using `colorPrimaryContainer` under M3.

**app/src/main/res/drawable/splash.xml** (13 lines)
- L5 solid `@android:color/white`, L12 bitmap `@mipmap/ic_launcher`. Used by `WelcomeTheme`, so it flashes white in dark mode.

**app/src/main/res/drawable/ic_empty.xml** (180 lines) and **layout/base_empty.xml** (17 lines)
- The illustration uses many light hex fills (such as `#f3f3fa`) and glares in dark mode. It overrides StateView's empty layout.

**app/src/main/res/drawable-anydpi/ic_add.xml, ic_search.xml, ic_location_change.xml**
- `ic_add`: tint `#FFFFFF`, alpha .8 (FAB icon; the FAB tint overrides it).
- `ic_search`: tint `#FFFFFF` (toolbar menu icon in menu_list and menu_search). It is invisible on a light toolbar.
- `ic_location_change`: tint `#333333`, alpha .6. Used in view_float_rocker.xml:18, which is shown over guest apps and is out of theme scope.

**app/src/main/AndroidManifest.xml** (55 lines)
- The application theme is `Theme.BlackBox`, inherited by MainActivity, ListActivity, FakeManagerActivity, SettingActivity, GmsManagerActivity, FollowMyLocationOverlay and ShortcutActivity.
- WelcomeActivity (launcher, singleTop) uses `WelcomeTheme` and only redirects to MainActivity.

### Related Files

**app/src/main/java/top/niunaijun/blackboxa/view/apps/AppsAdapter.kt** (165 lines)
- L28 `DEFAULT_ICON_COLOR = Color.parseColor("#CCCCCC")`, used at L133/L158 as a `ColorDrawable`. L136 uses `ColorDrawable(Color.GRAY)`. Icon placeholders need a theme-aware color (`MaterialColors.getColor` or a night-qualified color resource).

**app/src/main/java/top/niunaijun/blackboxa/view/base/LoadingActivity.kt** (38 lines)
- L19 `loadingView.setBackgroundColor(R.color.primary)` is **correct** (verified; see Key Decisions). It will follow a night-qualified color resource. CatLoadingView otherwise uses its own `cart_dialog` style (Holo.Light.Dialog, transparent) and fixed library colors (eyelid, white label).

**app/src/main/java/top/niunaijun/blackboxa/view/base/BaseActivity.kt** (27 lines)
- L9 `initToolbar` → `setSupportActionBar`. A possible place to apply night mode or edge-to-edge handling.

**app/src/main/java/top/niunaijun/blackboxa/view/main/MainActivity.kt** (434 lines)
- MaterialDialog at L151, L238 and L255 (input). FAB animation at L335-337.
- L253 `toolbar.getChildAt(1)?.setOnClickListener` is fragile: it depends on toolbar child order and could break if the M3 toolbar style changes that order.

**app/src/main/res/xml/setting.xml** (40 lines) and **view/setting/SettingFragment.kt** (111 lines)
- One "other" category: gms_manager, 4 × `SwitchPreferenceCompat` (root_hide, daemon_enable, use_vpn_network, disable_flag_secure) and send_logs, whose English title and summary are hard-coded.
- Every switch calls `invalidHideState` → `AppManager.mBlackBoxLoader` and shows a restart toast.
- preference-ktx 1.1.1 gives the old look. Category titles use `colorAccent`, which maps to `colorPrimary` under M3.

**app/src/main/java/top/niunaijun/blackboxa/view/main/BlackBoxLoader.kt** (323 lines)
- L18-26 persist settings via `AppSharedPreferenceDelegate(App.getContext(), …)`. This is the natural home for a theme-mode preference.

**app/src/main/java/top/niunaijun/blackboxa/app/App.kt** (72 lines)
- `onCreate` (about L65, after `AppManager.doOnCreate`) runs in every stub process, so any night-mode setup must be gated to the main process.

**Other MaterialDialog call sites:** GmsManagerActivity.kt:68, 94, 108; ShortcutUtil.kt:31, 63; FakeManagerActivity.kt:65; AppsFragment.kt:451, 473, 494. There are 12 in total, all title, message, buttons or input only.

**AppsFragment.kt:338** uses an AppCompat `PopupMenu` (menu `app_menu`) that follows `popupMenuStyle`.

**widget/RockerView.java, EnFloatView.kt, app/rocker/RockerManager.kt (about L124)**
- These are created with `App.getContext()` inside guest processes, so the host theme does not apply. Out of scope.

**Bcore (reference only, no change required for guest isolation)**
- Bcore/src/main/AndroidManifest.xml:
  - 50 × `ProxyActivity$P*` use `@style/BTheme`.
  - 100 × `ProxyPendingActivity`/`TransparentProxyActivity` use `@android:style/Theme.Translucent`.
  - `LauncherActivity` uses `@style/LauncherTheme`.
- Bcore/src/main/res/values/styles.xml: `BTheme` extends `@android:style/Theme.Light.NoTitleBar`, `LauncherTheme` extends `Theme.AppCompat.Light.NoActionBar`. No values-night.
- Bcore/src/main/res/layout/activity_launcher.xml hard-codes `@android:color/white`/`black`, so it flashes white.
- Guest themes are applied by `HCallbackProxy.java:155-194`, `AppInstrumentation.java:112-113` (`applyStyle(info.theme, true)`) and `ActivityStack.java:339-351`.

**Local AARs (app/libs)**
- StateView-v3.0.6: theme-aware (`?android:attr/colorBackground`, `textColorSecondary`).
- catloading-release: fixed purple `#5d4773` and white. Its background tint resolves our color resource.
- floatingview-release: image only.

**Dead or unused resources (candidates for removal, per minimal-implementation standard):** `activity_xp.xml`, `item_xp.xml`, `view_switch.xml` (`SwitchMaterial`), `item_viewpager.xml`, `@color/accent`, `@color/primary_dark`, `@color/primary_light` (if variants are dropped), unused launcher leftovers (`drawable/ic_launcher_background.xml`, `drawable-v24/ic_launcher_foreground.xml`, `*_beta` mipmaps).

---

## Current Functionality

- **Theming model:** a static light theme. Colors are set almost entirely by direct `@color/*` references in layouts and drawables. There are no `?attr/` references, no `style=` attributes and no `backgroundTint` in app resources.
- **Night-mode handling:** night mode is actively suppressed with `forceDarkAllowed=false` in both styles. Nothing calls `setTheme`, `AppCompatDelegate`, `setDefaultNightMode`, `uiMode` or `obtainStyledAttributes`.
- **Status bar:** colored with `?attr/colorPrimaryVariant` (`#3F3F3F`). No `windowLightStatusBar`, and no navigation bar color.
- **Splash:** `WelcomeTheme` shows a white `splash.xml` layer-list, then redirects to MainActivity. On every guest launch, Bcore `LauncherActivity` shows another white screen.

### Key Components/Functions

- **Theme.BlackBox**: the app theme for all host activities except Welcome.
- **ToolBarColorStyle**: forces white icons and menu text on the gray toolbar. Used only in view_toolbar.xml.
- **WelcomeTheme**: the splash theme, AppCompat Light.
- **LoadingActivity.showLoading()**: shows the CatLoadingView dialog tinted with `R.color.primary` (resource id, resolved per configuration).
- **AppsAdapter**: draws placeholder icons with hard-coded gray `ColorDrawable`s.
- **SettingFragment**: the preference screen. The switch-change path triggers a loader update and a restart toast.

### Widget inventory vs M3 behavior

| Widget | Location | M3 impact |
|---|---|---|
| MaterialToolbar | view_toolbar.xml (5 screens) | Explicit colors override M3 defaults. Needs a new overlay/style plus `popupTheme`. |
| FloatingActionButton | activity_main.xml:49 | Becomes a rounded square using `colorPrimaryContainer`/`onPrimaryContainer` automatically. The `ic_add` tint is overridden. |
| android.widget.Switch | item_gms.xml:20 | Not auto-upgraded. Replace with `MaterialSwitch` (the GmsAdapter cast needs updating). |
| SwitchPreferenceCompat ×4 | xml/setting.xml | Old look with preference 1.1.1. Optional bump to 1.2.1 via the version catalog. |
| WormDotsIndicator | activity_main.xml:33 | Defaults to `colorPrimary`. Set `dotsColor` explicitly or guarantee contrast. |
| SimpleSearchView (card) | activity_list.xml:21 | Its defaults are not theme-aware. Needs tint, hint, background and cursor attributes. |
| CornerLabelView | item_* layouts | Fixed colors. Rebind to theme or night-qualified colors. |
| material-dialogs 3.3.0 | 12 call sites | Switches light/dark on its own. Buttons use `colorPrimary`, 4dp corners, not M3. |
| StateView | loading/retry/empty | Theme-aware except the app's `ic_empty` override. |
| CatLoadingView | LoadingActivity | Independent Holo style. Background follows our color resource. |
| osmdroid MapView | FollowMyLocationOverlay | Light tiles. Optional `TilesOverlay.INVERT_COLORS` in night mode. |
| MaterialTextView | item layouts (auto-inflated) | Default text colors follow the theme once hard-coded colors are removed. |

### Data Flow

Manifest `android:theme` → Activity window theme → view inflation.
- Layout `@color/*` references bypass the theme entirely.
- Implicit consumers read theme attributes: FAB, toolbar overlay, PopupMenu, switches, preferences, material-dialogs, dotsindicator, StateView.
- Night mode can only reach the UI through (a) system `uiMode` with a DayNight parent and night-qualified resources, or (b) `AppCompatDelegate.setDefaultNightMode` persisted in prefs and applied before activities are created.
- Guest processes get their own `ActivityInfo.theme` through Bcore hooks and are unaffected.

---

## Dependencies

### Imports (What This Depends On)

- `com.google.android.material:material` 1.12.0 (`libs.material`, app/build.gradle L68): provides Theme.Material3.*, DayNight, DynamicColors, MaterialSwitch, MaterialColors, M3 toolbar styles.
- `androidx.appcompat` 1.7.0: AppCompatDelegate night mode.
- `androidx.preference:preference-ktx` 1.1.1 (inline, L75): preference visuals. Optional upgrade to 1.2.1 through the catalog (decision K2).
- `androidx.core` 1.15.0: ContextCompat. Optional `core-splashscreen` for a proper API-agnostic splash.
- Third-party UI: material-dialogs 3.3.0, SimpleSearchView 0.2.0, dotsindicator 4.2, CornerLabelView 1.0.0, osmdroid 6.1.11, RVAdapter 0.3.7 (no theme impact), local AARs (StateView, catloading, floatingview).
- SDK levels: compileSdk 35, targetSdk 28, minSdk 21. At targetSdk 28, Android 15's edge-to-edge enforcement does not apply and `statusBarColor` still works. `windowLightStatusBar` needs API 23 and `windowLightNavigationBar` API 27, via `tools:targetApi` or `values-v23`/`values-v27`.

### Consumers (What Depends On This)

- **8 host activities**:
  - MainActivity and GmsManagerActivity (via LoadingActivity)
  - ListActivity and FakeManagerActivity (via BaseActivity, sharing activity_list.xml)
  - SettingActivity with SettingFragment
  - FollowMyLocationOverlay (no toolbar)
  - ShortcutActivity (trampoline that briefly shows `windowBackground`)
  - WelcomeActivity (WelcomeTheme)
- **Implicit theme consumers**: MaterialToolbar, FAB, PopupMenu, framework Switch, SwitchPreferenceCompat/PreferenceCategory, material-dialogs, WormDotsIndicator, StateView, default TextViews.
- **Not consumers**: Bcore guest stubs (explicit themes), floating rocker UI (base context in guest processes), notifications and toasts.

**Consumer Count**: about 8 activities + 1 fragment + about 12 layouts/drawables + 12 dialog call sites
**Impact Scope**: Medium. Every host screen changes visually, but no logic or engine behavior is affected, and guest apps are isolated.

---

## Test Coverage

### Test Files

- None in `app`: no `src/test` and no `src/androidTest`. (`JarManagerTest` sits in Bcore `src/main` and is unrelated.)

### Coverage Assessment

- **Test count**: 0 relevant tests.
- **Gaps**: all of it. There are no UI, screenshot or theme tests, and CI has no lint or tests.
- The testing standard asks for tests on changes. Realistic options:
  - a manual QA matrix (light/dark × 8 screens + dialogs + loading/empty/splash + popup menu)
  - a small Espresso or screenshot smoke test that launches the main screens in both night modes
  - enabling Android Lint for the `app` module to catch hard-coded colors or contrast issues

---

## Coding Patterns

### Naming Conventions

- **Styles**: `Theme.BlackBox` (dotted) mixed with `ToolBarColorStyle`/`WelcomeTheme` (PascalCase). New styles should follow `Theme.BlackBox.*`, `ThemeOverlay.BlackBox.*` and `Widget.BlackBox.*` and must not collide with Bcore's `BTheme`/`LauncherTheme`.
- **Colors**: currently role-ish but inaccurate (`primary_dark` = `primary`). The target standard (`android-resources.md`) calls for semantic names such as `md_theme_primary` or `color_surface`.
- **Layouts**: `activity_*`, `item_*`, `view_*`, `base_*`. Vector drawables use `ic_*` in `drawable-anydpi`.
- **Kotlin**: ViewBinding, `view/<feature>/` packages, activities extend `BaseActivity`/`LoadingActivity`.

### Architecture Patterns

- **Style**: Android Views + XML + ViewBinding, MVVM (ViewModel → Repository → BlackBoxCore), Factories via `InjectionUtil`.
- **State Management**: LiveData/ViewModel. Settings are persisted through `AppSharedPreferenceDelegate` in `BlackBoxLoader`.
- **Localization**: strings in `values`, `values-zh-rCN` and `values-zh-rTW`. Any new setting needs all three.

---

## Complexity Assessment

| Factor | Value | Level |
|--------|-------|-------|
| File count | ~20 resource/source files to touch (themes, colors, 6-7 layouts, 4 drawables, 2-3 Kotlin, manifest, settings) | High |
| File size | Small files (9-180 lines; largest Kotlin touched ~434) | Low |
| Dependencies | No new required dependencies (MDC 1.12.0 present); optional core-splashscreen / preference 1.2.1 | Low |
| Consumers | 8 activities + 12 dialog sites + multiple third-party widgets | High |
| Test Coverage | 0 tests | High (risk) |

### Overall: Moderate

Each change is mechanical and small, but many files are touched. Third-party widgets need individual attention, and there is no test safety net. There is no algorithmic or engine complexity.

---

## Key Findings

### Strengths
- MDC 1.12.0 already includes all M3 themes and components, so no version churn is needed.
- The theme surface is tiny and centralized (one themes.xml and one colors.xml), with no programmatic theme code to untangle.
- Guest isolation is solid: Bcore stubs carry explicit themes, and guest classloaders do not share host AppCompat state.
- StateView loading and retry screens and the default MaterialTextView are already theme-aware.
- `CatLoadingView` resolves color resources per configuration, so it follows night colors for free.

### Concerns
- Hard-coded `#212121` text in 5 item layouts will be dark-on-dark in night mode (HIGH).
- The white toolbar icons, title, `ic_search` tint and `ToolBarColorStyle` only work on the dark gray bar (HIGH/MEDIUM, depends on the toolbar decision).
- A `#3F3F3F` primary is consumed implicitly by the dots indicator, dialog buttons, FAB and switches, and is invisible on dark surfaces (MEDIUM).
- White splash in `WelcomeTheme`, plus the Bcore `LauncherActivity` white flash on every guest launch (MEDIUM).
- material-dialogs and SimpleSearchView are unmaintained and not M3 (MEDIUM).
- The toolbar child-index hack at `MainActivity.kt:253` is fragile if M3 styling changes the toolbar's children.
- `App.onCreate` runs in all stub processes, so night-mode init must be gated.
- No tests, and lint is absent.

### Opportunities
- Introduce design tokens (color roles, `TextAppearance.Material3.*` type scale, shape overlays, `dimens.xml` on an 8dp grid), as called for by the roadmap and the `android-resources.md` standard.
- Upgrade the GMS switch to `MaterialSwitch` and delete dead layouts (`view_switch`, `activity_xp`, `item_xp`, `item_viewpager`) and unused colors.
- Optional dynamic color on API 31+ (`DynamicColors.applyToActivitiesIfAvailable`).
- Optional System/Light/Dark preference using `AppCompatDelegate.setDefaultNightMode` (no restart needed).
- Optional `core-splashscreen` to unify splash behavior, and an adaptive launcher icon (`mipmap-anydpi-v26`).

---

## Impact Assessment

- **Primary changes**:
  - `values/themes.xml` and `values/colors.xml`
  - new `values-night/colors.xml` (and/or `values-night/themes.xml`)
  - optional `values/dimens.xml` and type/shape styles
  - `layout/view_toolbar.xml`, `activity_list.xml`, `item_app.xml`, `item_fake.xml`, `item_package.xml`, `item_gms.xml`
  - `drawable/splash.xml`, `drawable-anydpi/ic_add.xml`, `ic_search.xml`, `drawable/ic_empty.xml` (night variant or tint)
- **Related changes**:
  - `AppsAdapter.kt` (placeholder colors)
  - `GmsManagerActivity`/GmsAdapter (if moving to `MaterialSwitch`)
  - `activity_main.xml` (dots colors, optional FAB style)
  - `AndroidManifest.xml` (only if theme names change)
  - material-dialogs theme attributes, or migration to `MaterialAlertDialogBuilder`
  - if a theme preference is in scope: `App.kt` (main-process-gated init) or `WelcomeActivity`/`BaseActivity`, plus `xml/setting.xml`, `SettingFragment.kt`, `BlackBoxLoader.kt` and strings (en, zh-rCN, zh-rTW)
- **Out of scope unless decided**: Bcore `LauncherActivity`/`LauncherTheme`/`activity_launcher.xml`; the rocker overlay; osmdroid tile inversion.
- **Test updates**: none exist. Add a manual QA checklist and optionally screenshot/Espresso smoke tests and lint.

### Risk Level: Medium

The changes are visual only and easy to revert, with no engine or guest impact. Risk comes from the broad visual surface (every host screen), many implicit consumers of `colorPrimary`, unthemed third-party widgets, contrast and accessibility in two modes, and no automated tests to catch regressions.

---

## Recommendations

The task modifies existing code, so these are implementation strategy recommendations.

1. **Tokens first.** Generate an M3 light and dark scheme from a chosen seed, for example with the Material Theme Builder. Put the role colors in `values/colors.xml` and `values-night/colors.xml` with semantic names. Define every role in `Theme.BlackBox` with parent `Theme.Material3.DayNight.NoActionBar`:
   - primary, onPrimary, primaryContainer, onPrimaryContainer
   - secondary and tertiary families
   - error and onError
   - surface, onSurface, surfaceVariant, onSurfaceVariant, surfaceContainer*
   - outline and outlineVariant
   - `android:colorBackground`
   - inverse roles

   Drop `colorPrimaryVariant`/`colorSecondaryVariant`, which have no M3 role (map them to the container roles). Add type (`textAppearance*`) and shape (`shapeAppearance*Component`) tokens.
2. **Remove `forceDarkAllowed=false`** from both styles (themes.xml L18 and L25).
3. **System bars:** `statusBarColor` → `?attr/colorSurface`, or `colorPrimary` if the colored app bar is kept. Add `windowLightStatusBar` (true for day, false for night; API 23+ via `tools:targetApi`/`values-v23`). Optionally set `navigationBarColor` and `windowLightNavigationBar` (API 27+).
4. **Toolbar:** replace `ToolBarColorStyle` with a Material3 toolbar style or overlay (for example `Widget.Material3.Toolbar.Surface`, or an `OnPrimary` variant if colored). Background, title, subtitle and navigation/menu icon tints should come from theme attributes, and add `app:popupTheme`. Remove the hard-coded white from `ic_search`/`ic_add` (use `?attr/colorOnSurface` or `colorControlNormal`, or rely on the FAB/toolbar tint). Check the `MainActivity.kt:253` child-index click hack.
5. **Layouts:**
   - `@color/primary_text` → `?attr/colorOnSurface` (or a `TextAppearance.Material3.*` style); `secondary_text` → `?attr/colorOnSurfaceVariant`.
   - `CornerLabelView` background and text → `?attr/colorPrimary` and `?attr/colorOnPrimary`, or night-qualified color resources if the library does not accept attributes.
   - SearchView background → `?attr/colorSurfaceContainerHigh`, with explicit icon, hint and cursor tints.
   - WormDotsIndicator: set `dotsColor` and `selectedDotColor` from tokens.
6. **Drawables:** `splash.xml` → `?android:attr/colorBackground` or a night-qualified `@color/splash_background`. Move `WelcomeTheme` to a Material3 DayNight parent, or adopt `Theme.SplashScreen`. For `ic_empty`, add a `drawable-night` variant or retint it with theme attributes.
7. **Kotlin:** `AppsAdapter` placeholders → `MaterialColors.getColor(view, R.attr.colorSurfaceVariant)` or a night-aware color resource. Leave `LoadingActivity:19` as is, since it is verified correct. Optionally point it at a semantic resource such as `@color/loading_background`.
8. **Components:** framework `Switch` in item_gms → `MaterialSwitch`. Theme material-dialogs (`md_background_color` → `?attr/colorSurfaceContainerHigh`, `md_color_button_text` → `?attr/colorPrimary`, `md_corner_radius` 28dp), or schedule the `MaterialAlertDialogBuilder` migration.
9. **Night-mode selection (if in scope):**
   - Add a `ListPreference` (System/Light/Dark) persisted via `BlackBoxLoader`'s delegate.
   - Apply it with `AppCompatDelegate.setDefaultNightMode` in the main process only (gate `App.onCreate`) and again on preference change.
   - Do not route it through `invalidHideState` or the restart toast.
   - Add strings in all three locales.
10. **Keep Bcore untouched** unless the team decides to fix the `LauncherActivity` flash. That fix would need a `values-night` folder and DayNight `LauncherTheme` in Bcore, and attribute-based colors in `activity_launcher.xml`.
11. **Verification:** a manual matrix of light and dark (and dynamic color if enabled) across all 8 activities, the popup menu, all dialog types, loading, empty, retry, splash and preferences, checked against the 4.5:1 contrast rule. Consider adding one instrumented or screenshot smoke test per mode, and enabling lint for `app`.
12. **Cleanup** (minimal-implementation standard): delete `view_switch.xml`, `activity_xp.xml`, `item_xp.xml`, `item_viewpager.xml`, `@color/accent`, and the redundant `primary_dark`/`primary_light` once they are unreferenced.

---

## Next Steps

Invoke the gap-analyzer with this report. Resolve the Open Questions with the user before specification:
- toolbar style (colored vs surface)
- seed color and dynamic color
- whether an in-app theme setting is in scope
- Bcore `LauncherActivity` scope
- material-dialogs: theme it or migrate
- whether dead-resource cleanup is included
