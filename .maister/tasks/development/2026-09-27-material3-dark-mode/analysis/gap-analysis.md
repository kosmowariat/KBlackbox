# Gap Analysis: Material 3 theme migration + dark mode (host `app`)

## TL;DR
- The current state is a static, light-only MaterialComponents theme. `forceDarkAllowed=false` actively blocks dark mode, and every color is a direct `@color/*` or hex reference (0 `?attr/` uses in layouts). The target is a `Theme.Material3.DayNight` scheme with light and night tokens, dynamic color on API 31+, and a System/Light/Dark setting.
- The gap is **modificative, visual-only**. About 20 resource/source files change, there is no new dependency (MDC 1.12.0 already ships M3), and nothing changes in Bcore or the engine.
- Two correctness gaps are not visible in the codebase report and must go into the spec. (1) `DynamicColors` and night-mode init must be gated with `BlackBoxCore.get().isMainProcess()`, with a precondition that limits dynamic color to host activities. (2) The new theme-mode preference needs a single source of truth, because the existing switches store their value twice.
- Six decisions remain open. One is critical (seed color). Five are important and have safe defaults (dialogs, token breadth, dead-resource cleanup, osmdroid, subtitle-click hack).
- Risk: **Medium** (broad visual surface, no tests). Effort: **Medium**.

## Key Decisions
- Characteristics: `modifies_existing_code` + `ui_heavy` + a small `involves_data_operations` for the theme-mode preference. No defect module is needed: the dark-mode problems are the intended target state, not a regression.
- Change type is **modificative** with **moderate** compatibility. Visual behavior changes on purpose, while all functional flows, preference keys and engine settings must stay identical.
- Some items are not decisions because standards or earlier clarifications already settle them:
  - Removing `forceDarkAllowed`.
  - The surface toolbar (clarified).
  - `android.widget.Switch` → `MaterialSwitch` in `item_gms.xml`. The file is touched anyway, and `android-resources.md` mandates the Material widget.
  - SimpleSearchView re-theming. It is required for dark mode to be readable.
  - The splash moving to a DayNight parent with a night-aware background (no `core-splashscreen` dependency).
  - The night variant of `ic_empty`.

## Open Questions / Risks
- **Dynamic color reaching the wrong activities.** `DynamicColors.applyToActivitiesIfAvailable(app)` registers a lifecycle callback that overlays every Activity in the process.
  - `App.onCreate` runs in every `:pN` guest process, where it would restyle guest activities that go through stubs. It also runs in the server process.
  - Even in the main process, Bcore `LauncherActivity` (plain `android.app.Activity`, `Bcore/.../app/LauncherActivity.java:21`) would get the overlay.
  - Mitigation: call it only when `BlackBoxCore.get().isMainProcess()` (`BlackBoxCore.java:1250`), and use `DynamicColorsOptions.Builder().setPrecondition { activity, _ -> activity is BaseActivity || activity is WelcomeActivity }`, or apply it per activity in `BaseActivity`.
- **Colors bound to resources do not follow dynamic color.** `CornerLabelView` (XML color attributes), `CatLoadingView` (`R.color.primary` resource id, `LoadingActivity.kt:19`) and `AppsAdapter` placeholders resolve `@color/` resources, not theme attributes. On API 31+ they show the static seed palette next to dynamic-color widgets.
  - Mitigation: map them to neutral surface-family tokens, which look fine under any palette. Where the library accepts it, set the color at runtime from `MaterialColors.getColor(view, attr)`.
- **Two copies of settings state.** `SwitchPreferenceCompat` persists to the default SharedPreferences. `BlackBoxLoader` persists the same values to the `AppSharedPreferenceDelegate` file, keyed by property name. `SettingFragment` only calls `setDefaultValue(...)`, which has no effect once a key is stored, so the UI and the loader can drift apart.
  - The new theme-mode `ListPreference` must not copy this pattern. The loader delegate should be the source of truth, with `isPersistent=false` (or `value` set explicitly from the loader in `onCreatePreferences`).
  - The change listener must call `AppCompatDelegate.setDefaultNightMode` and must not use `invalidHideState` or the restart toast.
- **No automated tests and no lint.** Regressions will only be caught by a manual QA matrix: light, dark and dynamic color, across 8 activities, 12 dialogs, the popup menu, loading, empty and retry states, the splash and preferences.
- **Contrast (4.5:1).** White text on `@color/primary` in `CornerLabelView` fails if the night primary is light. Use the matching `on*` token pair.
- **preference 1.1.1** renders the older switch and category style under M3. It is functional, so no bump is needed. If a bump is wanted later, it should go through `libs.versions.toml` (conventions).
- **Dead density PNGs.** `drawable-{m,h,xh,xxh}dpi/ic_add.png` and `ic_search.png` are shadowed by the `drawable-anydpi` vectors (minSdk 21), so only the vectors need retinting.

---

## Summary
- **Risk Level**: Medium
- **Estimated Effort**: Medium (mostly mechanical resource edits; about 3 Kotlin files carry logic)
- **Detected Characteristics**: modifies_existing_code, ui_heavy, involves_data_operations (theme-mode preference only)

## Task Characteristics
- Has reproducible defect: no. Dark-mode unreadability is the target of the migration, not a regression.
- Modifies existing code: yes
- Creates new entities: minor. There is one new preference key and new resource files (`values-night/`, possibly `dimens.xml` and type/shape styles), but no new screens or capabilities.
- Involves data operations: yes, limited to persisting and applying the theme-mode setting.
- UI heavy: yes

## Gaps Identified

### Missing Features
- **M3 color scheme.** `values/colors.xml` has 7 colors, and primary, primary_dark and primary_light are all `#3F3F3F`. There are no surface, background, error, outline, container or inverse roles.
- **Night resources.** There is no `values-night/` and no `drawable-night/`.
- **Dynamic color.** No `DynamicColors` usage anywhere.
- **Theme-mode setting.** No `ListPreference`, no `AppCompatDelegate` usage, and no night-mode code.
- **Type and shape tokens.** No `dimens.xml`, no `TextAppearance.*` styles, and no `shapeAppearance*` theme attributes.
- **System-bar handling for light surfaces.** No `windowLightStatusBar` (API 23+), and no `navigationBarColor` or `windowLightNavigationBar` (API 27+).

### Incomplete Features
- `Theme.BlackBox` (`themes.xml:3`) sets only the primary and secondary families from the MDC2 era, plus `statusBarColor=?attr/colorPrimaryVariant`. The `*Variant` roles have no M3 meaning.
- `WelcomeTheme` (`themes.xml:28`) uses an AppCompat Light parent with a white `splash.xml`, so the splash flashes white in dark mode.
- `ToolBarColorStyle` (`themes.xml:21`) uses `ThemeOverlay.AppCompat.ActionBar` with forced white controls. `view_toolbar.xml:8-12` hard-codes a `@color/primary` background and white title and subtitle, and has no `popupTheme`.

### Behavioral Changes Needed
| From | To | Evidence |
|---|---|---|
| `forceDarkAllowed=false` in 2 styles | removed | themes.xml L18, L25 |
| Parent `Theme.MaterialComponents.Light.NoActionBar` | `Theme.Material3.DayNight.NoActionBar` | themes.xml L3 |
| Gray colored toolbar with white content | Surface toolbar (`colorSurface`/`colorOnSurface`); `ic_search` tint → `?attr/colorOnSurface`/`colorControlNormal` | view_toolbar.xml, drawable-anydpi/ic_search.xml |
| `@color/primary_text` (#212121) text | `?attr/colorOnSurface` (or a TextAppearance) | item_app L38, item_fake L43, item_package L43, item_gms L16 |
| CornerLabelView `@color/primary` / `@color/white` | on/container token pair with valid contrast in both modes | item_app L25/L29, item_fake L25, item_package L25 |
| SimpleSearchView `background=@color/primary` | surface-container background with explicit icon, hint and text colors | activity_list.xml:27 |
| WormDotsIndicator uses implicit colorPrimary | explicit `dotsColor`/`selectedDotColor` from tokens | activity_main.xml:33 |
| White splash layer-list | night-aware background color, DayNight parent | drawable/splash.xml:5 |
| Light `ic_empty` fills | `drawable-night/ic_empty.xml` variant (or retint) | drawable/ic_empty.xml |
| `#CCCCCC` / `Color.GRAY` placeholders | theme or night-aware color | AppsAdapter.kt:28, 133, 136, 158 |
| `ic_add` tint `#FFFFFF` | removed or attribute-based (the FAB tint applies anyway) | drawable-anydpi/ic_add.xml |
| framework `Switch` | `MaterialSwitch` (plus 3 type references in `GmsManagerActivity.kt:6,82,93,107`) | item_gms.xml:20 |
| material-dialogs buttons use `#3F3F3F` primary, 4dp corners | new primary automatically, plus `md_*` attributes or migration (Decision I1) | 12 call sites |

## User Journey Impact Assessment

| Dimension | Current | After | Assessment |
|---|---|---|---|
| Reachability (theme setting) | none; follows nothing, always light | Main → toolbar menu → Settings → "Theme" ListPreference | OK (1 level deep, standard location) |
| Discoverability (theme setting) | n/a | 7/10: a standard Settings entry; System default means most users never need it | OK |
| Discoverability (dark mode itself) | 1/10: impossible, forced light | 10/10: follows the system setting automatically | +9 |
| Flow integration | all flows unchanged | all flows unchanged; the setting applies without a restart | OK, as long as the theme preference skips the restart-toast path |
| Existing flows (install, launch, users, fake location, GMS, shortcuts) | work | must work identically; only colors and shapes change | Regression risk is visual only |

**Personas**
- **End users**: gain dark mode and dynamic color.
- **Developers embedding Bcore**: unaffected, because Bcore is not changed.
- **Guest apps**: unaffected, provided dynamic color and night-mode init are gated to the main process with a precondition that limits them to host activities (see Risks).

## Data Lifecycle Analysis

### Entity: theme-mode setting (`system` | `light` | `dark`)

| Operation | Backend (persistence) | UI | Access | Status |
|---|---|---|---|---|
| CREATE | new `AppSharedPreferenceDelegate` property in `BlackBoxLoader` (pattern at BlackBoxLoader.kt:18-26), default `system` | ListPreference in `xml/setting.xml` | SettingActivity via the main menu | ❌ missing (planned) |
| READ (apply) | read at startup in the main process (`App.onCreate` gated by `BlackBoxCore.get().isMainProcess()`) → `AppCompatDelegate.setDefaultNightMode` | the whole UI reflects it; the preference shows the current entry (`useSimpleSummaryProvider`) | automatic | ❌ missing (planned) |
| UPDATE | loader setter + `setDefaultNightMode` in the change listener (no restart) | same ListPreference | same | ❌ missing (planned) |
| DELETE | n/a (app data clear resets it to the default) | n/a | n/a | n/a |

**Completeness (planned design)**: 100% once CREATE, READ and UPDATE are implemented as a unit. Nothing is orphaned if the spec keeps them together. The one lifecycle risk is **drift between two stores** (default preference store vs loader store), which the existing switches already show. Mitigation: loader as the single source of truth, `isPersistent=false` or an explicit `value` binding.
**Orphaned operations**: none in the planned design.
**Missing touchpoints**: strings for the title, the three entries and the summary in `values`, `values-zh-rCN` and `values-zh-rTW` (standard). No other touchpoint reads the setting.

## Defect Analysis
Not applicable.

## Integration Points
- `App.kt` `onCreate` after `AppManager.doOnCreate` is the main-process-gated init for night mode and `DynamicColors`. The alternative is `BaseActivity`/`WelcomeActivity`, but night mode must be applied before the first activity inflates.
- `SettingFragment.kt`: a new, separate change listener for the theme preference (not `invalidHideState`).
- `BlackBoxLoader.kt`: a new delegate property plus a getter and setter, following the existing pattern.
- `AndroidManifest.xml`: changes only if style names change. Keep `Theme.BlackBox`, and do not collide with Bcore's `BTheme`/`LauncherTheme`.

## Patterns to Follow
- Settings persistence: `AppSharedPreferenceDelegate` properties in `BlackBoxLoader`, with try/catch getters and setters.
- Style naming: `Theme.BlackBox.*`, `ThemeOverlay.BlackBox.*`, `Widget.BlackBox.*`, `TextAppearance.BlackBox.*`.
- Colors: semantic names (`md_theme_*` or role names), never hex-based names. Layouts and Kotlin reference only `?attr/` or `@color/` values.

## Issues Requiring Decisions

### Critical (Must Decide Before Proceeding)
1. **C1 – Seed / brand color for the custom M3 scheme** (fallback on API < 31 and when dynamic color is unavailable).
   - Context: the current brand is fully neutral. The launcher icon is white on dark gray, and primary is `#3F3F3F`. A low-chroma seed produces an almost achromatic M3 scheme. Accents (FAB container, switches, dots, dialog buttons, category titles) then barely stand out, and the dark scheme loses affordance.
   - Options:
     - (A) **An accent seed with a clear hue (for example a blue around `#3F6FD8`, or a teal), keeping neutral surfaces** *(recommended)*.
     - (B) A neutral or monochrome seed derived from `#3F3F3F`, which keeps icon continuity but gives weak accents.
     - (C) The M3 baseline purple `#6750A4`, which is generic and does not look like the brand.
   - Rationale: M3 affordances depend on a chromatic primary, and the neutral icon pairs well with a single accent hue. The user should pick the exact hue. The scheme is then generated with Material Theme Builder into `values/` and `values-night/`.

### Important (Should Decide)
1. **I1 – material-dialogs (12 call sites, 2 of which use `input()`: MainActivity.kt:257, ShortcutUtil.kt:33)**
   - Options:
     - (A) **Theme them through `md_*` theme attributes (`md_background_color` → surface-container-high, `md_color_button_text` → `colorPrimary`, `md_corner_radius` 28dp) and leave the call sites alone** *(default)*.
     - (B) Migrate all 12 to `MaterialAlertDialogBuilder`. This is the standard's target, but the input dialogs need a custom `TextInputLayout` view.
   - Rationale: (A) keeps the diff focused, and it complies with `android-feedback.md` ("migrate when touching": the call sites are not touched). (B) is the roadmap Medium item and fits a separate task.
2. **I2 – Breadth of typography and shape tokens**
   - Options:
     - (A) **Theme-level tokens (`textAppearance*`, `shapeAppearance*Component`), plus `TextAppearance.Material3.*` and `dimens.xml` tokens applied only in the layouts this task already touches (toolbar, the 4 item layouts, activity_list, activity_main)** *(default)*.
     - (B) A full sweep that converts every inline `textSize` and dp literal in all layouts.
     - (C) Theme-level only, with layout conversion deferred.
   - Rationale: (A) delivers the tokens the task names and converts the files that are open anyway, without growing into an unrelated refactor.
3. **I3 – Dead-resource cleanup**
   - Options:
     - (A) **Delete resources that this migration makes obsolete or that are verified unreferenced**: `ToolBarColorStyle`, `@color/accent`, `primary_dark`, `primary_light`, `view_switch.xml`, `activity_xp.xml`, `item_xp.xml`, `item_viewpager.xml`, and the shadowed density PNGs of `ic_add`/`ic_search` *(default)*. A grep confirmed that none are referenced outside `colors.xml`/`themes.xml`.
     - (B) Delete only the obsolete theme and color entries, and leave the unused layouts for a separate cleanup.
   - Rationale: the unused layouts contain hard-coded colors and the old toolbar include. If they stay, they either need a pointless migration or stay out of compliance. The minimal-implementation standard treats unused code as debt. The `*_beta` mipmaps and launcher leftovers stay out of scope.
4. **I4 – osmdroid map in dark mode (FollowMyLocationOverlay)**
   - Options:
     - (A) **Leave tiles light; theme only the surrounding chrome** *(default)*.
     - (B) Apply `TilesOverlay.INVERT_COLORS` when night mode is active.
   - Rationale: inverted OSM tiles often look odd (inverted labels and parks), and the map is a secondary screen. It is nice-to-have.
5. **I5 – Toolbar subtitle click hack (`MainActivity.kt:253` `toolbar.getChildAt(1)`)**
   - Options:
     - (A) **Keep it and verify it in manual QA.** The surface style does not change `Toolbar`'s child order *(default)*.
     - (B) Replace it with a robust lookup (find the child `TextView` whose text equals `toolbar.subtitle`) or an explicit custom subtitle view.
   - Rationale: minimal change unless it breaks. It is flagged because it is the only toolbar-structure dependency in the codebase.
6. **I6 – Dynamic color control**
   - Options:
     - (A) **Always on for API 31+ (host activities only), with no user toggle** *(default)*.
     - (B) Add a "Dynamic color" `SwitchPreferenceCompat` next to the theme mode.
   - Rationale: the clarifications asked for dynamic color on 12+ but no toggle. (B) adds a second setting, strings in 3 locales, and an activity recreate on change. It is easy to add later.

## Recommendations
1. Build the tokens first: generate the scheme from the C1 seed, then write `values/colors.xml`, `values-night/colors.xml` and all M3 roles in `Theme.BlackBox` (parent `Theme.Material3.DayNight.NoActionBar`). Drop the `*Variant` roles.
2. System bars: `statusBarColor=?attr/colorSurface`, `windowLightStatusBar` true in day and false in night (API 23 guard), and the same for the navigation bar (API 27 guard). targetSdk 28 means no forced edge-to-edge.
3. Replace `ToolBarColorStyle` with a Material3 surface toolbar style plus `popupTheme`. Remove the white tints.
4. Replace every hard-coded color listed in "Behavioral Changes Needed". Keep `LoadingActivity:19` as it is (verified correct). Optionally point it at a semantic resource such as `@color/loading_background`.
5. Gate init with `BlackBoxCore.get().isMainProcess()`. Apply `setDefaultNightMode` from the loader value first, then `DynamicColors` with a precondition limited to host activities.
6. Add the theme-mode ListPreference with the loader as the single source of truth, its own listener with no restart toast, and strings in en, zh-rCN and zh-rTW.
7. Write a manual QA checklist (light, dark and dynamic color across all screens, dialogs and states) and a contrast check. Optionally add a smoke test per mode.

## Risk Assessment
- **Complexity risk**: Low to Medium. The edits are mechanical, but they are spread across about 20 files and 6+ third-party widgets.
- **Integration risk**: Medium. Dynamic color and night-mode init could reach guest or Bcore activities if they are not gated by process and limited by a precondition to host activities.
- **Regression risk**: Medium (visual only). There are no tests. Contrast failures, invisible dots or indicators, and unreadable search or dialog UI are the likely regressions. Engine and guest behavior are unaffected.
