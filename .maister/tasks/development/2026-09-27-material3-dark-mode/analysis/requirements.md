# Requirements: Material 3 theme + dark mode (app module)

## TL;DR
Re-theme the host app to Material 3 DayNight: a custom blue palette (seed #3F6FD8), dynamic color on Android 12+, an M3 surface toolbar, full dark-mode support with no hard-coded light colors, and a new Settings > Appearance > Theme preference (System/Light/Dark, default System) that applies without a restart. Only the app module changes; Bcore and guest apps are unaffected. No new dependencies and no new tests; verification is manual QA.

## Key Decisions
- The Phase 4 mockups (analysis/design-context/INDEX.md) are binding for colors, toolbar, FAB, switches, dialog styling and the Theme preference.
- Theme mode is stored in BlackBoxLoader (AppSharedPreferenceDelegate) as the single source of truth. The ListPreference is non-persistent, so it does not also write to the default preferences.
- material-dialogs is styled via `md_*` theme attributes; its 12 call sites stay unchanged.
- Dead layouts are kept. Only obsolete theme/color entries are removed.

## Open Questions / Risks
- Manual QA is the only safety net (no automated tests).
- Dynamic color makes the look device-dependent on API 31+.

## Initial description
"migracja motywu na Material 3 + dark mode": migrate Theme.BlackBox from Theme.MaterialComponents.Light.NoActionBar to Material 3 with a real color scheme, typography/shape tokens, a values-night dark theme, and removal of forceDarkAllowed=false. This covers two roadmap High Priority GUI items: "Visual refresh / design system" and "Dark mode".

## Q&A (all rounds)
| Phase | Question | Answer |
|---|---|---|
| 1 | Toolbar style | M3 surface (colorSurface bg, colorOnSurface text/icons) |
| 1 | Palette | Custom M3 palette + dynamic color on Android 12+ |
| 1 | Theme setting | Yes: System/Light/Dark, default System, no restart |
| 1 | Bcore LauncherActivity flash | Out of scope (separate task) |
| 2 | Seed color | Blue ~#3F6FD8 |
| 2 | material-dialogs | Style via md_* attrs; call sites untouched |
| 2 | Token breadth | Theme-level textAppearance/shapeAppearance + TextAppearance/dimens only in touched layouts |
| 2 | Dead resources | Remove only obsolete theme/color entries (ToolBarColorStyle, accent, primary_dark, primary_light); keep dead layouts and shadowed PNGs |
| 2 | osmdroid map | Tiles stay light |
| 2 | Toolbar subtitle click (MainActivity.kt:253) | Re-implement robustly (no getChildAt(1)) |
| 2 | Dynamic color toggle | None; always on for API 31+ |
| 4 | Mockups | Accepted as binding (11 screens) |
| 5 | Init location | App.onCreate, gated by BlackBoxCore.get().isMainProcess(); DynamicColors precondition limits it to host activities |
| 5 | Palette resource layout | md_theme_* in values/colors.xml + values-night/colors.xml, single DayNight Theme.BlackBox |
| 5 | Testing | Manual QA only |
| 5 | Dependencies | None added (preference 1.1.1, no core-splashscreen) |

## User journey
- Default: the app follows the system dark/light setting automatically from first launch. There is no action needed, and no white flash on the splash.
- Discovery: Main toolbar ⚙ (or overflow) opens Settings. A new "Appearance" category, placed first, holds "Theme" with the current value as its summary (System default / Light / Dark). Tapping it opens a single-choice dialog. The choice applies immediately (activities recreate) and persists across restarts.
- Personas: end users cloning or sandboxing apps, typically on Android 10–15. Dark mode matters most for night use.
- Existing workflows are unchanged (install, launch, long-press menu, GMS, fake location, settings switches).

## Similar features / reuse
- Settings persistence: BlackBoxLoader's AppSharedPreferenceDelegate properties (view/main/BlackBoxLoader.kt:18-26), which `SettingFragment` already reads.
- Toolbar: the shared `layout/view_toolbar.xml` include with `BaseActivity.initToolbar`, so it changes in one place.
- Theme: the existing `Theme.BlackBox` name is kept (manifest unchanged) and re-parented.
- Main-process detection: `BlackBoxCore.get().isMainProcess()` (Bcore/.../BlackBoxCore.java:1250).
- String translations: values, values-zh-rCN, values-zh-rTW.

## Visual assets
- analysis/design-context/INDEX.md lists 11 HTML mockups (light and dark side by side); they are binding.
- analysis/design-context/design-resources.md holds the tokens mapping and standards.

## Functional requirements
1. `Theme.BlackBox` extends `Theme.Material3.DayNight.NoActionBar` and defines the full M3 color-role set from `md_theme_*` resources, day and night. `forceDarkAllowed` is removed everywhere.
2. `values-night/colors.xml` provides the night values for every role and for any custom color token used by layouts or libraries.
3. Dynamic color is applied on API 31+ via `DynamicColors.applyToActivitiesIfAvailable` with a precondition that limits it to host activities, and only in the main process.
4. The toolbar (`view_toolbar.xml`) uses the M3 surface style: colorSurface background, colorOnSurface title and navigation/overflow icons, colorOnSurfaceVariant subtitle. The overflow popup uses the M3 popup theme. The toolbar `ic_search` icon is tinted with a theme attribute.
5. No hard-coded light colors in touched layouts. Item text uses `?attr/colorOnSurface` / `?attr/colorOnSurfaceVariant` (or M3 TextAppearance). CornerLabelView badge, SimpleSearchView and WormDotsIndicator use tokens with night values. AppsAdapter placeholder colors come from theme/night-aware resources.
6. The FAB follows M3 (primaryContainer) and `ic_add` no longer forces a white tint.
7. `item_gms.xml` uses MaterialSwitch; the GmsManagerActivity / GmsAdapter type references are updated.
8. WelcomeTheme is Material3 DayNight and the splash background follows `?android:attr/colorBackground` (no white flash). `ic_empty` has a night variant (`drawable-night`).
9. material-dialogs is themed via `md_background_color`, `md_color_button_text` and `md_corner_radius` (28dp) and is readable in both modes.
10. Settings gets a new first "Appearance" PreferenceCategory with a "Theme" ListPreference (entries System default / Light / Dark). Its value is stored in BlackBoxLoader (default system) with persistence off on the preference. A dedicated change listener calls `AppCompatDelegate.setDefaultNightMode` (no restart toast). Strings go into en, zh-rCN and zh-rTW.
11. The stored mode is applied at startup in `App.onCreate` (main process only), before any activity is created.
12. The toolbar subtitle click in MainActivity no longer depends on the toolbar's child index.
13. Theme-level typography and shape tokens: `textAppearance*` defaults and `shapeAppearance*` roles. Touched layouts use TextAppearance.Material3.* and `dimens.xml` tokens for the values they already change.
14. Obsolete entries are removed: `ToolBarColorStyle`, colors `accent`, `primary_dark`, `primary_light`, and `primary_text`/`secondary_text` if they become unused. Dead layouts stay.

## Reusability opportunities
- A single `view_toolbar.xml` change covers five screens.
- The existing BlackBoxLoader delegate pattern is reused for the theme mode.
- Theme attributes replace per-layout colors, so future screens inherit the scheme automatically.

## Scope boundaries
In scope: `app` module resources (themes, colors, values-night, dimens, drawables, layouts touched for color), `App.kt`, `SettingFragment.kt`, `BlackBoxLoader.kt`, `MainActivity.kt` (subtitle click), `GmsManagerActivity`/`GmsAdapter` (MaterialSwitch), `AppsAdapter` placeholder colors, `setting.xml`, strings in 3 locales.
Out of scope:
- Bcore (LauncherActivity flash, stub themes)
- migration to MaterialAlertDialogBuilder
- replacing SimpleSearchView/CornerLabelView/RVAdapter
- preference library upgrade and core-splashscreen
- osmdroid tile inversion
- a dynamic-color toggle
- deleting dead layouts
- full-sweep typography refactor of untouched layouts
- automated tests
- RockerView/EnFloatView overlay (drawn in guest processes)

## Technical considerations
- targetSdk 28: edge-to-edge is not enforced. Status bar colors can be set; `windowLightStatusBar` needs API 23+ (use `tools:targetApi` or `values-v23`). minSdk is 21.
- App.onCreate runs in every `:pN` stub process and in the server process, so night-mode and DynamicColors setup must be gated with `BlackBoxCore.get().isMainProcess()`.
- New style names must not collide with Bcore's `BTheme`/`LauncherTheme` (library resources merge into the app namespace).
- CatLoadingView takes a color resource id (`LoadingActivity:19` is correct). A night-aware `@color/primary` token keeps it working.
- MDC 1.12.0 already includes Material3, DynamicColors and MaterialSwitch.
- Manual QA checklist: 8 screens, dialogs, popup menu, empty/loading states and splash, each in light, dark and dynamic color (API 31+).
