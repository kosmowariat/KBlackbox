# Specification: Material 3 theme + dark mode (app module)

## TL;DR
The host app theme `Theme.BlackBox` is re-parented to `Theme.Material3.DayNight.NoActionBar`. It gets a full M3 tonal scheme generated from seed `#3F6FD8`, stored as `md_theme_*` colors in `values/` and `values-night/`, plus dynamic color on API 31+ (main process, host activities only).
Hard-coded light colors in the touched layouts, drawables and `AppsAdapter` move to theme attributes. The toolbar becomes an M3 surface toolbar, `forceDarkAllowed=false` is removed, and the splash follows the theme.
A new Settings "Appearance > Theme" ListPreference (System/Light/Dark, default System) is stored only in `BlackBoxLoader` and applied through `AppCompatDelegate.setDefaultNightMode` without a restart.
Only the `app` module changes. There are no new dependencies and no new automated tests; verification is a manual QA checklist.

## Key Decisions
- One DayNight `Theme.BlackBox` whose color roles come from `md_theme_*` color resources (day in `values/colors.xml`, night in `values-night/colors.xml`). The manifest theme name is unchanged. This decision is final (Phase 5).
- Night mode and DynamicColors are initialized in `App.onCreate`, gated by `BlackBoxCore.get().isMainProcess()`. The DynamicColors precondition limits it to host activities by class-name prefix `top.niunaijun.blackboxa.`, with the trailing dot. Without the dot, Bcore's `top.niunaijun.blackbox.app.LauncherActivity` would also match. This decision is final (Phase 5).
- The theme mode is stored once, as a new `AppSharedPreferenceDelegate` String property in `BlackBoxLoader`. The ListPreference is `persistent=false` and gets its value from the loader. A dedicated listener handles it (no `invalidHideState`, no restart toast).
- The `@color/primary` token stays only as the CatLoadingView background (`LoadingActivity.kt:19`, unchanged). It is aliased to `md_theme_primary` (day) and `md_theme_primary_container` (night), so the library's fixed white label keeps at least 4.5:1 contrast in both modes.
- System bars: the status bar uses `colorSurface` with light-status-bar icons on API 23+, via a `values-v23` override of the same theme (`Base.Theme.BlackBox` pattern). On API 21–22 the M3 parent's default status bar is kept, because dark icons are impossible there. The navigation bar is left at the system default.
- `WelcomeTheme` extends `Theme.BlackBox`, so it inherits the palette and DayNight behaviour. Only `windowBackground` is overridden.
- material-dialogs is themed only through `md_*` theme attributes. The 12 call sites are untouched.
- The toolbar overlay `ToolBarColorStyle` is replaced by `ThemeOverlay.BlackBox.Toolbar`, an M3 overlay that tints toolbar controls with `colorOnSurface`. Obsolete colors `accent`, `primary_dark` and `primary_light` are removed. `primary_text` and `secondary_text` stay because the dead layout `item_xp.xml` still references them, and dead layouts are kept.
- Color resource names use snake_case (for example `md_theme_on_primary_container`) per `android-resources.md`. This deliberately differs from Material Theme Builder's camelCase export names.

## Open Questions / Risks
- **Manual QA is the only safety net.** There are no tests and no lint. The checklist below must be run in light, dark and dynamic color.
- **Third-party attribute resolution is unverified.** CornerLabelView 1.0.0, SimpleSearchView 0.2.0 and dotsindicator 4.2 are not in the local Gradle cache, so it is unconfirmed that they resolve `?attr/` colors in their custom XML attributes.
  - Fallback: if one of them ignores or crashes on `?attr/`, reference the equivalent night-aware `@color/md_theme_*` token instead. That token does not follow dynamic color, which is accepted.
  - SimpleSearchView attribute names must be confirmed against the library during implementation.
- **The ListPreference dialog uses the AppCompat AlertDialog** (preference 1.1.1), not MaterialAlertDialogBuilder. Colors follow the theme. The 28dp corner is only guaranteed where the platform `dialogCornerRadius` applies (API 28+); a smaller corner on older APIs is an accepted deviation from `component:theme-picker-dialog`.
- **material-dialogs input field** (`component:dialog-input`): whether the inner TextInputLayout picks up the M3 outlined style depends on the library's dialog theme. Readable colors in both modes are required; an exact M3 field shape is best-effort.
- **Dynamic color makes the look device-dependent on API 31+.** The seed palette is what users see on API ≤ 30 or when the wallpaper provides no palette.

## Goal
Deliver a Material 3 look with a real color scheme and full dark mode across all host screens. Users can follow the system theme or force Light or Dark from Settings without restarting, and Bcore and guest apps stay untouched.

## User Stories
- As an end user who uses dark mode at night, I want the sandbox UI to follow my system dark theme automatically, so that it does not blind me or flash white on launch.
- As an end user, I want to force Light or Dark from Settings > Appearance > Theme, so that the app looks the way I prefer regardless of the system setting. The change applies immediately and persists.
- As an end user on Android 12+, I want the app to pick up my wallpaper colors (Material You), so that it matches the rest of my device.
- As a user of existing features, I want install, launch, the long-press menu, GMS, fake location and the settings switches to behave exactly as before, so that the re-theme is purely visual.

## Core Requirements
1. **Theme parent and roles.** `Theme.BlackBox` extends `Theme.Material3.DayNight.NoActionBar`.
   - It maps every M3 color role listed in the Palette section to the matching `md_theme_*` color.
   - `colorPrimaryVariant` and `colorSecondaryVariant` are removed, since they have no M3 role.
   - `android:forceDarkAllowed` is removed from every style.
2. **Palette resources.** `values/colors.xml` holds the day `md_theme_*` values generated from seed `#3F6FD8` with the Material Theme Builder algorithm (tonal palettes, neutral surfaces). A new `values-night/colors.xml` holds the night values for every role plus the `primary` alias override.
3. **Dynamic color.** On API 31+, `DynamicColors.applyToActivitiesIfAvailable` is registered only in the main process. A precondition accepts only activities whose class name starts with `top.niunaijun.blackboxa.`. There is no user toggle.
4. **Surface toolbar.** `view_toolbar.xml` changes as follows:
   - It uses `style="@style/Widget.Material3.Toolbar.Surface"` and the new `ThemeOverlay.BlackBox.Toolbar`.
   - The hard-coded `background`, `titleTextColor` and `subtitleTextColor` are removed.
   - The title is `colorOnSurface`, the subtitle `colorOnSurfaceVariant`, and navigation, overflow and action icons are `colorOnSurface`.
   - The overflow popup inherits the M3 popup menu style.
   - `ic_search.xml` tint becomes `?attr/colorControlNormal`, which the overlay resolves to `colorOnSurface`, and its 0.8 alpha is removed.
   - The theme's `actionMenuTextColor` becomes `?attr/colorOnSurface`.
5. **No hard-coded light colors in touched layouts.**
   - Item text in `item_app`, `item_fake`, `item_package` and `item_gms` uses `?attr/colorOnSurface` for primary lines and `?attr/colorOnSurfaceVariant` for secondary lines.
   - The CornerLabelView badge uses `?attr/colorTertiaryContainer` / `?attr/colorOnTertiaryContainer`.
   - SimpleSearchView and WormDotsIndicator use tokens (see Visual Design).
6. **AppsAdapter placeholders.** The `#CCCCCC` constant and `Color.GRAY` are removed. Placeholder drawables resolve `colorSurfaceContainerHighest` from the view's theme (for example `MaterialColors.getColor`), so they follow night mode and dynamic color.
7. **FAB.**
   - The FAB relies on the M3 default FAB style (`colorPrimaryContainer` / `colorOnPrimaryContainer`, 16dp corners).
   - `ic_add.xml` loses its forced `#FFFFFF` tint and 0.8 alpha.
   - The FAB's `contentDescription="TODO"` becomes the existing `@string/choose_app` (accessibility standard, "fix when touching").
8. **GMS switch.** `item_gms.xml` uses `com.google.android.material.materialswitch.MaterialSwitch`. The `android.widget.Switch` references in `GmsManagerActivity.kt` (import and lines 82, 93, 107) become `MaterialSwitch`. The ViewBinding type in `GmsAdapter` updates automatically.
9. **Splash.** `WelcomeTheme` extends `Theme.BlackBox`. The `splash.xml` background solid becomes `?android:attr/colorBackground`, so there is no white flash in dark mode. The launcher-icon bitmap stays.
10. **Empty-state illustration.** A new `drawable-night/ic_empty.xml` is a copy of `drawable/ic_empty.xml` with its light fills replaced by a dimmed dark-neutral palette. The day variant is unchanged.
11. **material-dialogs theming.**
    - `Theme.BlackBox` sets `md_background_color` = `?attr/colorSurfaceContainerHigh`, `md_color_button_text` = `?attr/colorPrimary` and `md_corner_radius` = 28dp.
    - Title and content text must be readable in both modes. If QA shows otherwise, also set `md_color_title` / `md_color_content` to `colorOnSurface` / `colorOnSurfaceVariant`.
    - Call sites are unchanged.
12. **Theme preference UI.**
    - `xml/setting.xml` gets a new first `PreferenceCategory` "Appearance" containing a `ListPreference` with key `theme_mode`, `persistent=false` and simple summary provider.
    - Its entries are System default / Light / Dark, and its entry values `system` / `light` / `dark` are non-translatable.
    - The existing "Others" category is unchanged.
13. **Theme preference persistence (single source of truth).**
    - `BlackBoxLoader` gets a String delegate property (default `system`) with a getter and setter that follow the existing accessor pattern. For example: `themeMode()` / `invalidThemeMode(value)`.
    - It also gets one mapping from the stored value to the `AppCompatDelegate.MODE_NIGHT_*` constant: `system` → FOLLOW_SYSTEM, `light` → NO, `dark` → YES. Any unknown value maps to FOLLOW_SYSTEM.
    - The mapping lives next to the accessor and is shared by `App` and `SettingFragment`, so there is no duplicated `when`.
14. **Theme preference behaviour.**
    - In `SettingFragment.onCreatePreferences`, the ListPreference `value` is set from the loader.
    - A dedicated change listener first writes the new value to the loader and then calls `AppCompatDelegate.setDefaultNightMode(mapped)`. Host activities recreate and the setting screen shows the new summary.
    - The listener shows no restart toast and does not go through `invalidHideState`.
15. **Startup application.** `App.onCreate`, in the main process only, applies the stored night mode before registering DynamicColors, and before any activity is created. It must not be skipped when `AppManager.doOnCreate` logs a failure; place it right after `super.onCreate()`.
16. **Robust subtitle click.** `MainActivity.initToolbarSubTitle` no longer uses `toolbar.getChildAt(1)`. After the subtitle is set, it finds the toolbar's child `TextView` whose text equals `toolbar.subtitle` and attaches the existing remark-dialog click listener there. The dialog behaviour is unchanged.
17. **Typography and shape tokens.**
    - The theme declares the type-scale defaults used by touched views (`textAppearanceTitleLarge`, `textAppearanceBodyLarge`, `textAppearanceBodyMedium`, `textAppearanceLabelMedium` → `TextAppearance.Material3.*`). It also declares the M3 corner roles used by components (`shapeAppearanceCornerLarge` for the FAB at 16dp, `shapeAppearanceCornerExtraLarge` for dialogs at 28dp) → `ShapeAppearance.Material3.Corner.*`.
    - Touched layouts replace inline `textSize` with `TextAppearance.Material3.*`:
      - `item_app` name → LabelMedium
      - `item_package` / `item_fake` name → BodyLarge, secondary line → BodyMedium
      - `item_gms` title → BodyLarge
    - A new `values/dimens.xml` provides 8dp-grid tokens (8/16/24/48dp) for the values these touched layouts already use. Component-specific literals (CornerLabelView geometry, dots-indicator geometry, 2dp spacing) stay inline.
18. **Strings.** New keys are added to `values`, `values-zh-rCN` and `values-zh-rTW`: the category title "Appearance", the preference title "Theme", and the three entries. The entry-values array is non-translatable and lives only in `values/`.
19. **Obsolete entries removed.** Remove `ToolBarColorStyle` and the colors `accent`, `primary_dark` and `primary_light`. Also remove `white` if it ends up unreferenced. Keep `primary_text` / `secondary_text`, which `item_xp.xml` still uses, and keep dead layouts and shadowed density PNGs.
20. **Documentation.**
    - Add a `RELEASE_NOTES.md` entry under New Features (Material 3 theme, dark mode, Theme preference) listing the UI location, the System default, the behaviour and the files changed, per `docs/release-notes.md`.
    - Tick the roadmap "Visual refresh / design system" and "Dark mode" items.
    - Update the theme lines in `project/tech-stack.md` / `architecture.md` (theme parent is now Material3 DayNight).

## Palette (M3 role set, day and night)
Generate both schemes from seed `#3F6FD8` with the Material Theme Builder / Material Color Utilities "tonal spot" algorithm and neutral surfaces. Every role below needs a day value in `values/colors.xml` and a night value in `values-night/colors.xml`. Each is mapped in `Theme.BlackBox`: the theme attribute comes first, then the color resource.

> **Note:** primary palette generated with chroma 48 (user decision during implementation; day primary `#395BA9`, night primary `#B1C5FF` / primary_container `#1D438F`); other roles tonal-spot.

| Group | Theme attribute → color resource |
|---|---|
| Primary | `colorPrimary` → `md_theme_primary`; `colorOnPrimary` → `md_theme_on_primary`; `colorPrimaryContainer` → `md_theme_primary_container`; `colorOnPrimaryContainer` → `md_theme_on_primary_container`; `colorPrimaryInverse` → `md_theme_inverse_primary` |
| Secondary | `colorSecondary`, `colorOnSecondary`, `colorSecondaryContainer`, `colorOnSecondaryContainer` → `md_theme_secondary*` equivalents |
| Tertiary | `colorTertiary`, `colorOnTertiary`, `colorTertiaryContainer`, `colorOnTertiaryContainer` → `md_theme_tertiary*` equivalents |
| Error | `colorError`, `colorOnError`, `colorErrorContainer`, `colorOnErrorContainer` → `md_theme_error*` equivalents |
| Background | `android:colorBackground` → `md_theme_background`; `colorOnBackground` → `md_theme_on_background` |
| Surface | `colorSurface`, `colorOnSurface`, `colorSurfaceVariant`, `colorOnSurfaceVariant`, `colorSurfaceDim`, `colorSurfaceBright`, `colorSurfaceContainerLowest`, `colorSurfaceContainerLow`, `colorSurfaceContainer`, `colorSurfaceContainerHigh`, `colorSurfaceContainerHighest` → `md_theme_surface*` equivalents |
| Inverse | `colorSurfaceInverse` → `md_theme_inverse_surface`; `colorOnSurfaceInverse` → `md_theme_inverse_on_surface` |
| Outline | `colorOutline` → `md_theme_outline`; `colorOutlineVariant` → `md_theme_outline_variant` |
| App alias | `primary` → `@color/md_theme_primary` (day) / `@color/md_theme_primary_container` (night); used only by `LoadingActivity` |

In total there are 34 M3 roles plus 1 alias. Fixed-accent roles (`colorPrimaryFixed` and similar), scrim and shadow are not needed: nothing in the app consumes them.

Reference values from the mockups (approximate, for sanity-checking the generated output only):

| Role | Day | Night |
|---|---|---|
| primary / onPrimary | #2F5BC7 / #FFFFFF | #B2C5FF / #002B73 |
| primaryContainer / onPrimaryContainer | #DAE2FF / #001848 | #1A43A6 / #DAE2FF |
| tertiaryContainer | #FCD7FB | #583E5B |
| surface = background | #FAF8FF | #121318 |
| surfaceContainer / High / Highest | #EEEDF4 / #E8E7EF / #E2E2E9 | #1E1F25 / #292A2F / #33343A |
| onSurface / onSurfaceVariant | #1A1B21 / #44464F | #E3E2E9 / #C5C6D0 |
| outline / outlineVariant | #757780 / #C5C6D0 | #8F9099 / #44464F |
| error | #BA1A1A | #FFB4AB |

## Visual Design
The mockups in `analysis/design-context/` are binding inputs. The implementation-planner will attach `Visual References` to UI task groups. Fidelity is **approximate**: the structure and role usage are binding, but the hex values are approximations superseded by the generated palette. Screen structure is unchanged; the "User 0 ▾" glyph in the mockups marks a clickable subtitle and does not add a view.

| Mockup ID | File | Resources / attributes to change |
|---|---|---|
| `screen:main-apps-grid` | `mockups/main-apps-grid.html` | `view_toolbar.xml`: surface style + `ThemeOverlay.BlackBox.Toolbar`; `MainActivity` subtitle lookup (Req 16). `activity_main.xml`: WormDotsIndicator `dotsColor` and `dotsStrokeColor` = `?attr/colorPrimary`; FAB default M3 style + `contentDescription`; `ic_add.xml` tint removed. `item_app.xml`: CornerLabelView `bg_color` = `?attr/colorTertiaryContainer`, `text_color` = `?attr/colorOnTertiaryContainer`; name `TextAppearance.Material3.LabelMedium` + `?attr/colorOnSurface`; dimens tokens. `AppsAdapter` placeholders (Req 6). |
| `screen:main-empty-state` | `mockups/main-empty-state.html` | New `drawable-night/ic_empty.xml` (dimmed palette; mockup illustration tones roughly #23262F / #3A3F4D / #6B7488). `base_empty.xml` unchanged; its text follows the theme text color. |
| `screen:choose-app-list-with-search` | `mockups/choose-app-list-with-search.html` | `activity_list.xml`: SimpleSearchView container background `?attr/colorSurface` (was `@color/primary`), search card background `?attr/colorSurfaceContainerHigh`, text `?attr/colorOnSurface`, hint and icons `?attr/colorOnSurfaceVariant`, cursor `?attr/colorPrimary`, all via the library's attributes. `item_package.xml`: name BodyLarge/`colorOnSurface`, packageName BodyMedium/`colorOnSurfaceVariant`, badge tertiary-container pair. `ic_search.xml` tint. |
| `screen:settings-with-theme-preference` | `mockups/settings-with-theme-preference.html` | `xml/setting.xml`: new first "Appearance" category with `theme_mode` ListPreference; strings in 3 locales; `SettingFragment` + `BlackBoxLoader` (Req 12–14). Category titles and switches restyle through the theme (`colorPrimary`). |
| `component:theme-picker-dialog` | `mockups/theme-picker-dialog.html` | ListPreference single-choice dialog (preference 1.1.1 AppCompat AlertDialog) colored by the theme; applies immediately without a toast (Req 14). Corner radius: see Risks. |
| `screen:gms-manager-with-materialswitch` | `mockups/gms-manager-with-materialswitch.html` | `item_gms.xml`: MaterialSwitch; title BodyLarge/`colorOnSurface`; dimens tokens. `GmsManagerActivity.kt` type references. |
| `component:dialog-confirm` | `mockups/dialog-confirm-material-dialogs.html` | `Theme.BlackBox`: `md_background_color`, `md_color_button_text`, `md_corner_radius` (Req 11). |
| `component:dialog-input` | `mockups/dialog-input-user-remark.html` | Same `md_*` attributes. The input field inherits the theme's text-input style (best-effort, see Risks). |
| `component:app-long-press-menu` | `mockups/app-long-press-menu.html` | No layout change. The AppCompat PopupMenu (`AppsFragment.kt:338`) picks up the M3 `popupMenuStyle` (surfaceContainer) from the new parent. |
| `screen:splash-welcome` | `mockups/splash-welcome.html` | `WelcomeTheme` parent → `Theme.BlackBox`; `splash.xml` solid → `?android:attr/colorBackground`. |
| `screen:fake-location` | `mockups/fake-location.html` | List reuses `activity_list.xml` + `item_fake.xml` (name BodyLarge/`colorOnSurface`, fakeLocation BodyMedium/`colorOnSurfaceVariant`, badge pair). osmdroid tiles are intentionally not inverted. `FollowMyLocationOverlay` chrome follows the theme background. |

## Reusable Components

### Existing Code to Leverage
- `app/src/main/java/top/niunaijun/blackboxa/biz/cache/AppSharedPreferenceDelegate.kt`: already supports `String` values. The theme mode reuses it as-is.
- `app/src/main/java/top/niunaijun/blackboxa/view/main/BlackBoxLoader.kt:18-26` and its accessor pattern (`hideRoot()` / `invalidHideRoot()`): the model for `themeMode()` / `invalidThemeMode()`.
- `AppManager.mBlackBoxLoader` (`app/AppManager.kt:13`): the existing singleton access point used by `SettingFragment` and available in `App`.
- `BlackBoxCore.get().isMainProcess()` (`Bcore/.../BlackBoxCore.java:1250`): the process type is set during `doAttachBaseContext` (line 832), so it is valid in `App.onCreate`.
- `app/src/main/res/layout/view_toolbar.xml` + `BaseActivity.initToolbar`: one change restyles the Main, List/FakeManager, GMS and Settings toolbars.
- MDC 1.12.0 (`libs.material`): `Theme.Material3.DayNight.NoActionBar`, `Widget.Material3.Toolbar.Surface`, `ThemeOverlay.Material3.Toolbar.Surface`, `DynamicColors` / `DynamicColorsOptions`, `MaterialSwitch`, `MaterialColors`, `TextAppearance.Material3.*` and `ShapeAppearance.Material3.Corner.*`. No dependency change.
- AppCompat 1.7.0 `AppCompatDelegate.setDefaultNightMode`: recreates AppCompat activities with no custom restart logic.
- `PreferenceFragmentCompat` / `ListPreference` (preference-ktx 1.1.1) with `useSimpleSummaryProvider`: the summary shows the current entry with no custom code.
- The `LoadingActivity.kt:19` CatLoadingView call takes a color resource id resolved per configuration, so it follows the night-aware `@color/primary` alias unchanged.
- The existing string `@string/choose_app` is reused for the FAB content description.
- StateView loading and retry layouts are already theme-aware and need no change.

### New Components Required
- `values-night/colors.xml`: there is no night resource folder today. It is required for DayNight.
- `values-v23/themes.xml` (`Theme.BlackBox` extending `Base.Theme.BlackBox`, adding `statusBarColor` = `?attr/colorSurface` and `windowLightStatusBar` = `?attr/isLightTheme`): light status-bar icons cannot exist below API 23. Without the override, a surface-colored status bar would have invisible icons on API 21–22.
- `drawable-night/ic_empty.xml`: vector fills are literal hex values, so a night variant is the decided approach.
- `values/dimens.xml`: there are no dimension tokens today, and Req 17 requires them for touched layouts.
- `ThemeOverlay.BlackBox.Toolbar` (in `values/themes.xml`): M3 surface toolbars tint overflow and action icons with `colorOnSurfaceVariant` by default. The overlay sets `colorControlNormal` and `actionMenuTextColor` to `colorOnSurface`, as the mockup requires. It replaces `ToolBarColorStyle`, so the style count does not grow.
- New string keys and the entry-values array: there is no existing string for these labels.
- No new Kotlin classes or files. The mode mapping lives in `BlackBoxLoader`, and the listener in `SettingFragment`.

## Technical Approach
- **Theme resolution:**
  - Manifest → `Theme.BlackBox` (DayNight). The `values/themes.xml` `Base.Theme.BlackBox` defines all roles, type and shape tokens, the `md_*` attributes and `actionMenuTextColor`; `Theme.BlackBox` extends it. The `values-v23/themes.xml` `Theme.BlackBox` adds the status-bar items.
  - Night selection happens through the resource qualifier: `values-night/colors.xml` swaps every `md_theme_*`. There are no separate night theme files.
  - New style names use the `Theme.BlackBox.*`, `ThemeOverlay.BlackBox.*` and `Base.Theme.BlackBox` prefixes and never collide with Bcore's `BTheme` / `LauncherTheme`.
- **Startup (main process only), in `App.onCreate` after `super.onCreate()`:**
  1. If `BlackBoxCore.get().isMainProcess()`, call `AppCompatDelegate.setDefaultNightMode(mapped loader value)`.
  2. Then register `DynamicColors.applyToActivitiesIfAvailable(this, options)` with a precondition that returns true only for class names starting with `top.niunaijun.blackboxa.`. DynamicColors is a no-op below API 31.
  3. `:pN` guest processes and the server process skip both calls. Bcore `LauncherActivity` (main process, `android.app.Activity`) fails the precondition.
  - This sits in the Application lifecycle hook, which is the architecture standard's allowed exception for direct `BlackBoxCore` access.
- **Setting change:** the ListPreference listener runs loader setter → `setDefaultNightMode`. AppCompat recreates the resumed host activities. `SettingFragment` is recreated, reads `value` from the loader and shows the updated summary. The default SharedPreferences file is never written for `theme_mode`.
- **Color flow:** layouts and drawables → `?attr/` roles → theme → `md_theme_*` (day/night) or dynamic overlay (API 31+). Only three spots are bound to `@color/` resources: `LoadingActivity` via the `primary` alias, the splash via `?android:attr/colorBackground`, and any third-party fallback noted in Risks. They follow night mode but not dynamic color, which is accepted.
- **Data:** one new key in the `AppSharedPreferenceDelegate` file, named after the new delegate property. Existing keys and engine settings are untouched. Clearing app data resets it to `system`.
- **Guest isolation:** no Bcore resource or code changes. Guest activities keep their own `ActivityInfo.theme`.

## Implementation Guidance

### Testing Approach
- Per team decision (K5, Phase 5), this task adds **no automated tests**. The usual "2-8 focused tests per implementation step group" is replaced by the manual QA checklist below. Each task group ends with a compile check (`./gradlew :app:assembleDebug`) and the checklist rows relevant to that group.
- Verification runs only the checks relevant to the changed screens. There is no full-suite run, since no suite exists.

### Manual QA Checklist
Run each row in **Light**, **Dark** and **Dynamic color** (API 31+ device with a colorful wallpaper). Also do one pass on an API 23–30 device or emulator, and if available one pass on API 21–22 for the status bar.

| # | Area | Check |
|---|---|---|
| 1 | Splash (WelcomeActivity) | No white flash in dark mode; background matches the theme; icon centered |
| 2 | Main: toolbar | Surface background; title onSurface; subtitle "User N" onSurfaceVariant and **tappable** (opens remark dialog, save updates subtitle); overflow icon visible; status-bar icons readable (API 23+) |
| 3 | Main: grid | App names readable; XP badge tertiary-container pair readable; placeholder icon tone fits the theme; dots indicator visible in primary |
| 4 | Main: FAB | primaryContainer with a readable + icon; TalkBack reads "Choose App" |
| 5 | Main: long-press popup menu | M3 menu surface, readable items in both modes |
| 6 | Main: empty state | Night illustration used in dark mode; text readable |
| 7 | Loading (install/uninstall/GMS) | CatLoadingView background readable with white label in both modes |
| 8 | Choose app (ListActivity) | Search icon visible; open search: card surface, text, hint, cursor and back icon readable; rows readable; badge readable |
| 9 | Fake location list + map picker | Rows and badge readable; map tiles light (expected); chrome themed |
| 10 | Settings | "Appearance" is the first category; Theme summary shows the current entry; existing switches work, show the restart toast, and persist as before |
| 11 | Theme picker | Dialog readable; System, Light and Dark each apply immediately without a restart toast; the choice persists after force-stop and relaunch; System follows the OS toggle live |
| 12 | GMS manager | MaterialSwitch renders and toggles; install/uninstall dialogs and flows unchanged |
| 13 | material-dialogs (confirm + input: user remark, shortcut name, uninstall, clear, stop) | surfaceContainerHigh background, primary buttons, rounded corners, readable title, message and input |
| 14 | Guest isolation | Launch a guest app: its UI is not recolored by dynamic color or night mode; Bcore LauncherActivity looks as before (its flash is a known separate issue) |
| 15 | Contrast | Spot-check text and icon pairs against 4.5:1 in both modes (badge, toolbar subtitle, dialog buttons, loading label) |
| 16 | TalkBack | Settings Theme preference and dialog focus order, FAB label |

### Standards Compliance
- `standards/frontend/android-resources.md`: M3 theme with `values-night`, `forceDarkAllowed` removed, `?attr/` / `@color/` tokens only (no hex in layouts or Kotlin), snake_case semantic color names, `dimens.xml` 8dp grid, `TextAppearance.*`, MaterialSwitch instead of `android.widget.Switch`, strings in en / zh-rCN / zh-rTW, settings persisted through the `BlackBoxLoader` delegate. The theme preference deliberately does not use the restart toast, because it is not engine-affecting.
- `standards/frontend/android-views.md`: the toolbar stays the shared `view_toolbar` include used with `initToolbar`. The `findViewById` in GmsManagerActivity is only retyped, not refactored (out of scope).
- `standards/frontend/android-feedback.md`: material-dialogs is legacy with call sites untouched, so no migration is triggered. No new dialog usages are added.
- `standards/frontend/android-architecture.md`: the settings UI reaches the loader via `AppManager`, as today. The direct `BlackBoxCore` call in `App` is the allowed lifecycle-hook exception.
- `standards/frontend/accessibility.md`: 4.5:1 contrast in both modes (on-* role pairs, the `primary` loading alias), the FAB "TODO" description fixed, TalkBack check in QA.
- `standards/global/minimal-implementation.md`: no new classes, no speculative tokens (only roles and dimens actually consumed), obsolete theme and color entries removed, no dynamic-color toggle.
- `standards/global/kotlin-style.md` / `error-handling.md`: follow the existing loader accessor pattern, use no new blanket try/catch in UI code, map an unknown stored value to the safe default `system` (allowlist).
- `standards/global/conventions.md` + `build/gradle.md`: no new dependencies (catalog untouched), release notes and project docs updated.

## Out of Scope
- Bcore changes: the `LauncherActivity` white flash, `BTheme` / `LauncherTheme`, `activity_launcher.xml` (separate task).
- Migrating material-dialogs to `MaterialAlertDialogBuilder`; replacing SimpleSearchView, CornerLabelView or RVAdapter.
- Upgrading preference-ktx beyond 1.1.1; adding `core-splashscreen`.
- osmdroid tile inversion; a dynamic-color toggle.
- Deleting dead layouts (`view_switch`, `activity_xp`, `item_xp`, `item_viewpager`) or shadowed density PNGs.
- A full typography and dimension sweep of untouched layouts.
- Navigation-bar coloring and edge-to-edge.
- Automated tests and lint enablement.
- The RockerView / EnFloatView overlay and `ic_location_change` (drawn in guest processes).
- Other legacy clean-ups in touched files: hard-coded "Unknown App" / "Send Logs" literals, the `findViewById` in GmsManagerActivity, and blanket try/catch blocks in `App` / `AppsAdapter`.

## Success Criteria
- `./gradlew :app:assembleDebug` succeeds with no new dependencies. `Bcore` is unmodified (`git diff --stat Bcore/` is empty).
- No hex color literals and no `@color/primary_text`, `@color/white` or `@color/primary` remain in the touched layouts (`view_toolbar`, `activity_main`, `activity_list`, `item_app`, `item_fake`, `item_package`, `item_gms`), in `ic_add.xml` / `ic_search.xml`, or in `AppsAdapter.kt`.
- `android:forceDarkAllowed` appears nowhere in `app/src/main/res`.
- Every M3 role in the Palette table has a value in both `values/colors.xml` and `values-night/colors.xml`.
- Theme preference: default System on a fresh install. Light and Dark apply immediately without a restart and persist across process death. `theme_mode` is absent from the default SharedPreferences file.
- On API 31+ host screens use wallpaper colors, while guest apps and Bcore LauncherActivity are unaffected.
- All 16 manual QA rows pass in Light, Dark and Dynamic color, with text and icon contrast of at least 4.5:1.
- All existing flows behave as before: install, launch, users and remarks, the long-press menu, GMS, fake location, shortcuts, and the settings switches with their restart toast.
