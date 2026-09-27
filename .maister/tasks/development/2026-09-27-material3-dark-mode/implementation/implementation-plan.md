# Implementation Plan: Material 3 theme + dark mode (app module)

## TL;DR
6 task groups, 46 steps, executed as a strict linear chain: Theme foundation → Theme-mode preference and startup → Toolbar and main screen → List screens and GMS → Cleanup and docs → Final build and manual QA.
There are no automated tests (team decision K5). Every group ends with `:app:assembleDebug` plus the manual QA checklist rows from spec.md that it owns. Group 6 runs the full build and the full 16-row checklist in Light, Dark and Dynamic color.
Only the `app` module and project docs change. No new dependencies, and Bcore stays untouched.
All 11 mockup IDs are covered (see `visual-coverage.md`).

## Key Decisions
- **Linear chain, no parallel groups.** Every group verifies with the same Gradle build in the same project directory, so concurrent groups would fight over the Gradle daemon and lock. Groups 3–5 also depend on the palette, tokens and `dimens.xml` from Group 1.
- **The palette and theme land first (Group 1), and removals are staged.** `ToolBarColorStyle` and `@color/white` are still referenced by `view_toolbar.xml` / `item_app.xml`. `ToolBarColorStyle` is therefore deleted in Group 3, together with its last consumer, and `white` in Group 5, after a grep. Each group keeps the build green.
- **`ThemeOverlay.BlackBox.Toolbar` is defined in Group 3, next to its only consumer** (`view_toolbar.xml`). As a result `themes.xml` appears in both Group 1 and Group 3; the chain serializes them.
- **Verification replaces the "write 2-8 tests" steps.** Step N.1 of each group is a short pre-check (read targets, confirm third-party attribute names). The last step is the build plus the owned QA rows. The expected automated test count is 0.
- **Third-party `?attr/` fallback is pre-authorized by the spec.** If CornerLabelView, SimpleSearchView or WormDotsIndicator ignore or crash on `?attr/`, the implementer uses the night-aware `@color/md_theme_*` equivalent and records it in the work log.

## Open Questions / Risks
- **Third-party attribute resolution is unverified** (CornerLabelView 1.0.0, SimpleSearchView 0.2.0, dotsindicator 4.2). Steps 3.1 and 4.1 confirm attribute names from the resolved AARs before editing.
- **`?android:attr/colorBackground` inside `splash.xml`** (a layer-list used as `windowBackground`) resolves against the activity theme. If a device renders it wrong, fall back to `@color/md_theme_background` (night-aware, not dynamic).
- **Manual QA is the only safety net.** Group 6 needs the user to run the checklist on a device or emulator (an API 31+ device with a colorful wallpaper, plus API 23–30). The executor cannot complete row checks itself.
- **Subtitle lookup (Req 16):** the child `TextView` is looked up once, after the subtitle is set. If a Toolbar re-layout replaces the view, the click stops working. QA row 2 covers this.
- **Task-system items:** this planner runtime has no `TaskCreate` / `TaskUpdate` tools. Group-level task items and their dependencies need to be created by the orchestrator/executor, using the Execution Order below.

## Overview
Total Steps: 46 numbered sub-steps (plus 6 parent X.0 items); each implementation group opens with a pre-check (N.1) in place of the usual test-writing step
Task Groups: 6
Expected Tests: 0 automated (team decision K5). Verification = `:app:assembleDebug` per group, full `assembleDebug` plus the 16-row manual QA checklist × {Light, Dark, Dynamic color} in Group 6.

Build commands (Windows, from `D:\KBlackbox`): `.\gradlew.bat :app:assembleDebug` (PowerShell) or `bash ./gradlew :app:assembleDebug` (Git Bash). Final: `assembleDebug`.

Paths below are relative to the repo root. `APP` = `app/src/main`, `PKG` = `app/src/main/java/top/niunaijun/blackboxa`.

## Implementation Steps

### Task Group 1: Theme Foundation (palette, M3 DayNight theme, tokens, splash, dialogs)
**Dependencies:** None
**Files to Modify:** app/src/main/res/values/colors.xml, app/src/main/res/values-night/colors.xml (new), app/src/main/res/values/themes.xml, app/src/main/res/values-v23/themes.xml (new), app/src/main/res/values/dimens.xml (new), app/src/main/res/drawable/splash.xml
**Visual References:**
- mockup: analysis/design-context/mockups/splash-welcome.html
  element: screen:splash-welcome
  locator: `.board` in `#mockup-content` (line 255), `.phone.light .splash` and `.phone.dark .splash` with the centered `.logo`
  acceptance: the splash background equals the theme `colorBackground` in both modes (no white in dark); the launcher icon stays centered and unchanged; no extra view or text is added
- mockup: analysis/design-context/mockups/dialog-confirm-material-dialogs.html
  element: component:dialog-confirm
  locator: `.scrim > .dlg` including `.btns > .btn` in both phone columns (line 255)
  acceptance: the dialog background is `colorSurfaceContainerHigh`; corners are 28dp; the button text is `colorPrimary`; the title and message are readable (onSurface / onSurfaceVariant) in both modes
- mockup: analysis/design-context/mockups/dialog-input-user-remark.html
  element: component:dialog-input
  locator: `.scrim > .dlg` with `.field.focus` and `.btns` in both phone columns (line 255)
  acceptance: same `md_*` surface, corner and button colors as dialog-confirm; the input text, hint and cursor are readable in both modes; the exact outlined-field shape is best-effort (spec Risks)
- mockup: analysis/design-context/mockups/app-long-press-menu.html
  element: component:app-long-press-menu
  locator: `.menu` overlay over the `.grid` in both phone columns (line 255)
  acceptance: the PopupMenu renders on the M3 menu surface (surfaceContainer) with onSurface item text in both modes, with no layout or code change
**Estimated Steps:** 12

- [x] 1.0 Complete the theme foundation layer
  - [x] 1.1 Pre-check: read the current `APP/res/values/themes.xml` and `colors.xml` and grep every consumer of `@color/primary`, `primary_dark`, `primary_light`, `accent`, `white`, `primary_text` and `secondary_text` in `app/src/main`. Confirm that `accent`, `primary_dark` and `primary_light` are referenced only by `themes.xml`. Confirm the material-dialogs 3.x attribute names and formats (`md_background_color`, `md_color_button_text`, `md_corner_radius` as a dimension; optionally `md_color_title` / `md_color_content`) from the resolved library.
  - [x] 1.2 Generate day and night palettes from seed `#3F6FD8` with the Material Theme Builder / Material Color Utilities "tonal spot" algorithm (neutral surfaces). Sanity-check the output against the reference table in spec.md (Palette section). The mockup hex values are approximate, and the generated values win.
  - [x] 1.3 Rewrite `APP/res/values/colors.xml`:
    - Add all 34 day roles as snake_case `md_theme_*` (primary/on_primary/primary_container/on_primary_container/inverse_primary; the secondary×4, tertiary×4 and error×4 families; background/on_background; surface, on_surface, surface_variant, on_surface_variant, surface_dim, surface_bright, surface_container_lowest/low/(base)/high/highest; inverse_surface/inverse_on_surface; outline/outline_variant).
    - Change `primary` to the alias `@color/md_theme_primary`.
    - Remove `accent`, `primary_dark` and `primary_light`.
    - Keep `primary_text` and `secondary_text` (used by the dead `item_xp.xml`) and `white` (still used until Groups 3/5).
  - [x] 1.4 Create `APP/res/values-night/colors.xml` with the night value of every one of the 34 `md_theme_*` roles. Override the `primary` alias to `@color/md_theme_primary_container`, so the fixed white CatLoadingView label keeps ≥4.5:1.
  - [x] 1.5 In `values/themes.xml`, create `Base.Theme.BlackBox` with parent `Theme.Material3.DayNight.NoActionBar`:
    - Map every role in the spec Palette table: theme attr → `@color/md_theme_*`, including `android:colorBackground`, `colorPrimaryInverse`, `colorSurfaceInverse` and `colorOnSurfaceInverse`.
    - Drop `colorPrimaryVariant`, `colorSecondaryVariant` and the old `android:statusBarColor` item.
    - Set `actionMenuTextColor` to `?attr/colorOnSurface`.
    - Remove `android:forceDarkAllowed`.
    - Declare `Theme.BlackBox` with parent `Base.Theme.BlackBox`. The manifest name is unchanged.
  - [x] 1.6 Add type and shape tokens to `Base.Theme.BlackBox`:
    - `textAppearanceTitleLarge`, `textAppearanceBodyLarge`, `textAppearanceBodyMedium` and `textAppearanceLabelMedium` → `@style/TextAppearance.Material3.*`.
    - `shapeAppearanceCornerLarge` → `ShapeAppearance.Material3.Corner.Large` (16dp, FAB) and `shapeAppearanceCornerExtraLarge` → `ShapeAppearance.Material3.Corner.ExtraLarge` (28dp, dialogs).
    - Only the roles the spec lists (minimal-implementation).
  - [x] 1.7 Add material-dialogs attributes to `Base.Theme.BlackBox`: `md_background_color` = `?attr/colorSurfaceContainerHigh`, `md_color_button_text` = `?attr/colorPrimary`, `md_corner_radius` = `28dp`. The 12 call sites stay untouched. Add `md_color_title` / `md_color_content` only if Group 6 QA row 13 shows unreadable text.
  - [x] 1.8 Remove the `android:forceDarkAllowed` item from `ToolBarColorStyle`. The style itself stays until Group 3 replaces its only consumer.
  - [x] 1.9 Create `APP/res/values-v23/themes.xml` that redeclares `Theme.BlackBox` with parent `Base.Theme.BlackBox`, adding `android:statusBarColor` = `?attr/colorSurface` and `android:windowLightStatusBar` = `?attr/isLightTheme`. API 21–22 keep the M3 parent default. The navigation bar is left at the system default.
  - [x] 1.10 Splash:
    - Re-parent `WelcomeTheme` to `Theme.BlackBox`, keeping only the `android:windowBackground` = `@drawable/splash` override.
    - In `APP/res/drawable/splash.xml`, change the solid color to `?android:attr/colorBackground`. Keep the launcher-icon bitmap layer.
    - Fallback if it renders wrong: `@color/md_theme_background`.
  - [x] 1.11 Create `APP/res/values/dimens.xml` with 8dp-grid tokens, snake_case per `android-resources.md` (8dp, 16dp, 24dp spacing and the 48dp icon/touch size). Declare only the values the Group 3/4 layouts actually replace. Component geometry (CornerLabelView, dots, 2dp) stays inline.
  - [x] 1.12 Verify the theme foundation:
    - `grep -r forceDarkAllowed app/src/main/res` returns nothing.
    - Both `colors.xml` files contain the same 34 `md_theme_*` names.
    - `:app:assembleDebug` succeeds (Material3 parent resolves with MDC 1.12.0, no dependency change).
    - Manual QA rows owned: 1 (splash), 5 (long-press menu), 7 (loading alias), 13 (material-dialogs), checked in Light and Dark with System follow.

**Acceptance Criteria:**
- `:app:assembleDebug` succeeds; `gradle/libs.versions.toml` and `app/build.gradle*` are unchanged.
- `Theme.BlackBox` resolves to `Theme.Material3.DayNight.NoActionBar` through `Base.Theme.BlackBox`, with all 34 roles mapped and no `*Variant` roles.
- `values/colors.xml` and `values-night/colors.xml` hold identical `md_theme_*` key sets (34 each) plus the `primary` alias. `accent`, `primary_dark` and `primary_light` are gone.
- `forceDarkAllowed` appears nowhere in `app/src/main/res`.
- QA rows 1, 5, 7 and 13 pass, meeting each Visual References `acceptance` criterion above.

---

### Task Group 2: Theme-Mode Preference and Startup Application
**Dependencies:** 1
**Files to Modify:** app/src/main/java/top/niunaijun/blackboxa/view/main/BlackBoxLoader.kt, app/src/main/java/top/niunaijun/blackboxa/app/App.kt, app/src/main/java/top/niunaijun/blackboxa/view/setting/SettingFragment.kt, app/src/main/res/xml/setting.xml, app/src/main/res/values/strings.xml, app/src/main/res/values-zh-rCN/strings.xml, app/src/main/res/values-zh-rTW/strings.xml
**Visual References:**
- mockup: analysis/design-context/mockups/settings-with-theme-preference.html
  element: screen:settings-with-theme-preference
  locator: `.content` in both phone columns (line 255): the first `.cat` ("Appearance") with its `.pref` Theme row, followed by the unchanged "Others" `.cat` with its `.sw` switches
  acceptance: "Appearance" is the first category; the Theme row shows the title "Theme" and the summary = the current entry (System default / Light / Dark); category titles use `colorPrimary`; the existing switch rows keep their order, labels and restart-toast behaviour
- mockup: analysis/design-context/mockups/theme-picker-dialog.html
  element: component:theme-picker-dialog
  locator: `.scrim > .dlg` with three `.radio` rows (`.rb.on` = selected) and `.btns` (line 255)
  acceptance: the options are listed in the order System default, Light, Dark; the current value is pre-selected; selecting one applies immediately (activities recreate) with no restart toast; the dialog uses theme colors in both modes; a smaller corner radius below API 28 is an accepted deviation
**Estimated Steps:** 7

- [x] 2.0 Complete the theme-mode preference layer
  - [x] 2.1 Pre-check: read `BlackBoxLoader.kt` (delegate pattern at lines 18–26, `hideRoot()` / `invalidHideRoot()` accessors), `SettingFragment.kt` (`invalidHideState`), `App.kt` `onCreate` and `APP/res/xml/setting.xml`. Confirm that `AppManager.mBlackBoxLoader` is lazily created and safe to touch in `App.onCreate` (it uses `App.getContext()`, set in `attachBaseContext`).
  - [x] 2.2 `BlackBoxLoader.kt`:
    - Add `private var mThemeMode by AppSharedPreferenceDelegate(App.getContext(), "system")`.
    - Add `themeMode(): String` and `invalidThemeMode(value: String)`, mirroring the existing try/catch accessor pattern.
    - Add one mapping function next to them (for example `themeNightMode(): Int`, or `nightModeOf(value)`): `system` → `MODE_NIGHT_FOLLOW_SYSTEM`, `light` → `MODE_NIGHT_NO`, `dark` → `MODE_NIGHT_YES`, and anything else → `MODE_NIGHT_FOLLOW_SYSTEM` (allowlist). It is shared by `App` and `SettingFragment`, with no duplicated `when`.
  - [x] 2.3 `App.onCreate`: directly after `super.onCreate()`, before `AppManager.doOnCreate` (so it is not skipped when that logs a failure), add the following, only when `BlackBoxCore.get().isMainProcess()`:
    - Call `AppCompatDelegate.setDefaultNightMode(<loader mapping>)`.
    - Then call `DynamicColors.applyToActivitiesIfAvailable(this, DynamicColorsOptions.Builder().setPrecondition { activity, _ -> activity.javaClass.name.startsWith("top.niunaijun.blackboxa.") }.build())`. Keep the trailing dot so Bcore `top.niunaijun.blackbox.app.LauncherActivity` is excluded.
    - Guest `:pN` and server processes skip both calls. Add no new blanket try/catch.
  - [x] 2.4 Strings:
    - In `values/strings.xml`, add the snake_case keys: the category title ("Appearance"), the preference title ("Theme"), and a `string-array` of entries ("System default", "Light", "Dark").
    - Add a `translatable="false"` `string-array` of entry values (`system`, `light`, `dark`), only in `values/`.
    - Add translations of the title, category and entries to `values-zh-rCN` and `values-zh-rTW`.
  - [x] 2.5 `APP/res/xml/setting.xml`: insert a new first `PreferenceCategory` (Appearance title) containing a `ListPreference` with:
    - `key="theme_mode"`, `android:persistent="false"`, `app:useSimpleSummaryProvider="true"`.
    - The entries and entry-values arrays, and `defaultValue` `system`.
    - `iconSpaceReserved` consistent with the existing prefs.
    - The "Others" category is unchanged.
  - [x] 2.6 `SettingFragment.kt`: add `initThemeMode()`, called from `onCreatePreferences`:
    - Find the `ListPreference` `theme_mode` and set `value = AppManager.mBlackBoxLoader.themeMode()`.
    - Set a dedicated `setOnPreferenceChangeListener` that calls `invalidThemeMode(newValue as String)` and then `AppCompatDelegate.setDefaultNightMode(<mapping>)`, and returns `true`.
    - No toast, no `invalidHideState`.
  - [x] 2.7 Verify the theme-mode preference:
    - `:app:assembleDebug` succeeds.
    - Manual QA rows owned: 10 (Settings), 11 (theme picker: each option applies immediately, persists after force-stop and relaunch, System follows the OS toggle live), 14 (guest isolation: guest app and Bcore LauncherActivity not recolored), 16 (TalkBack on the Theme preference and dialog).
    - Confirm `theme_mode` is absent from the default SharedPreferences file (`adb shell run-as <applicationId> cat shared_prefs/<applicationId>_preferences.xml`).

**Acceptance Criteria:**
- `:app:assembleDebug` succeeds.
- A fresh install defaults to System. Light and Dark apply without a restart or toast and persist across process death.
- `theme_mode` is stored only in the `AppSharedPreferenceDelegate` file, never in the default preferences.
- DynamicColors and night-mode init run only in the main process, and DynamicColors reaches only `top.niunaijun.blackboxa.*` activities.
- Existing switches still persist and show the restart toast.
- QA rows 10, 11, 14 and 16 pass, meeting each Visual References `acceptance` criterion above.

---

### Task Group 3: Surface Toolbar and Main Screen
**Dependencies:** 2
**Files to Modify:** app/src/main/res/values/themes.xml, app/src/main/res/layout/view_toolbar.xml, app/src/main/res/drawable-anydpi/ic_search.xml, app/src/main/res/drawable-anydpi/ic_add.xml, app/src/main/res/layout/activity_main.xml, app/src/main/res/layout/item_app.xml, app/src/main/java/top/niunaijun/blackboxa/view/apps/AppsAdapter.kt, app/src/main/java/top/niunaijun/blackboxa/view/main/MainActivity.kt, app/src/main/res/drawable-night/ic_empty.xml (new)
**Visual References:**
- mockup: analysis/design-context/mockups/main-apps-grid.html
  element: screen:main-apps-grid
  locator: `.tb` (`.ttl` with `.t` title and `.st` subtitle, `.ic` icons), `.dots`, `.grid` (`.app` with `.icon`, `.badge`, `.lbl`) and `.fab` in both phone columns (line 255)
  acceptance:
  - The toolbar background is `colorSurface`, the title `colorOnSurface`, the subtitle "User N" `colorOnSurfaceVariant` and tappable (it opens the remark dialog), and the icons are `colorOnSurface`.
  - The dots are `colorPrimary`.
  - The 4-column grid is unchanged. App labels are LabelMedium in onSurface. The XP badge is the tertiaryContainer / onTertiaryContainer pair.
  - The placeholder icon uses surfaceContainerHighest.
  - The FAB is primaryContainer with an onPrimaryContainer "+" and 16dp corners.
- mockup: analysis/design-context/mockups/main-empty-state.html
  element: screen:main-empty-state
  locator: `.content > .empty` with `.illo` illustration in both phone columns (line 255)
  acceptance: dark mode uses the dimmed night illustration (tones about #23262F / #3A3F4D / #6B7488) and light mode the unchanged day illustration; the empty text follows the theme text color; the toolbar, dots and FAB match main-apps-grid
- mockup: analysis/design-context/mockups/app-long-press-menu.html
  element: component:app-long-press-menu
  locator: `.menu` anchored over `.grid .app` (line 255)
  acceptance: after the toolbar and grid changes, the menu still opens from a long-press on an app and renders on the M3 surface with readable items; the menu actions are unchanged
**Estimated Steps:** 10

- [x] 3.0 Complete the toolbar and main-screen layer
  - [x] 3.1 Pre-check:
    - Read `view_toolbar.xml`, `activity_main.xml`, `item_app.xml`, `AppsAdapter.kt` (lines 28, 133, 136, 158), `MainActivity.initToolbarSubTitle` (line ~253) and both vector drawables.
    - Confirm the CornerLabelView (`bg_color`, `text_color`) and WormDotsIndicator (`dotsColor`, `dotsStrokeColor`, `selectedDotColor`) attribute formats from the resolved AARs in the Gradle cache (`values.xml` under the transforms cache). Decide `?attr/` vs the `@color/md_theme_*` fallback.
  - [x] 3.2 `values/themes.xml`:
    - Add `ThemeOverlay.BlackBox.Toolbar` (parent `ThemeOverlay.Material3.Toolbar.Surface`) with `colorControlNormal` = `?attr/colorOnSurface` and `actionMenuTextColor` = `?attr/colorOnSurface`.
    - Delete `ToolBarColorStyle`.
  - [x] 3.3 `view_toolbar.xml`:
    - Set `style="@style/Widget.Material3.Toolbar.Surface"` and `android:theme="@style/ThemeOverlay.BlackBox.Toolbar"`.
    - Remove `android:background`, `app:titleTextColor` and `app:subtitleTextColor`.
    - Make the title color `colorOnSurface` and the subtitle `colorOnSurfaceVariant` (add `app:subtitleTextColor="?attr/colorOnSurfaceVariant"` only if the style default differs).
    - Keep the default M3 popup theme. The include and `BaseActivity.initToolbar` usage stay as they are.
  - [x] 3.4 Drawables:
    - `drawable-anydpi/ic_search.xml`: tint → `?attr/colorControlNormal`; remove `android:alpha="0.8"`.
    - `drawable-anydpi/ic_add.xml`: remove the `#FFFFFF` tint and the 0.8 alpha (the FAB `iconTint` applies).
    - Leave the density PNGs untouched.
  - [x] 3.5 `activity_main.xml`:
    - WormDotsIndicator `dotsColor` / `dotsStrokeColor` (and `selectedDotColor` if present) = `?attr/colorPrimary` (fallback `@color/md_theme_primary`).
    - Remove any hard-coded FAB tint or background so the M3 default FAB style applies, and set `android:contentDescription="@string/choose_app"`.
    - Replace the 8/16/24/48dp literals with `@dimen/*` tokens.
  - [x] 3.6 `item_app.xml`:
    - CornerLabelView `bg_color` = `?attr/colorTertiaryContainer` and `text_color` = `?attr/colorOnTertiaryContainer` (replacing `@color/primary` / `@color/white`; fallback `@color/md_theme_tertiary_container` / `@color/md_theme_on_tertiary_container`).
    - App name: `android:textAppearance="?attr/textAppearanceLabelMedium"` (or `@style/TextAppearance.Material3.LabelMedium`) and `android:textColor="?attr/colorOnSurface"`; remove the inline `textSize` and `@color/primary_text`.
    - Apply dimens tokens.
  - [x] 3.7 `AppsAdapter.kt`:
    - Remove the `#CCCCCC` constant and both `Color.GRAY` uses.
    - Build placeholder drawables with `MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurfaceContainerHighest)`, resolved from the item view's context, so they follow night mode and dynamic color.
    - Leave the existing try/catch blocks and the "Unknown App" literal alone (out of scope).
  - [x] 3.8 Create `APP/res/drawable-night/ic_empty.xml`:
    - A copy of `drawable/ic_empty.xml` with the same viewport and paths, with the light fills replaced by a dimmed dark-neutral palette (about #23262F / #3A3F4D / #6B7488).
    - The day file is unchanged.
  - [x] 3.9 `MainActivity.initToolbarSubTitle`:
    - Replace `toolbar.getChildAt(1)` with a lookup, after the subtitle is set: iterate the toolbar children and pick the `TextView` whose `text == toolbar.subtitle`.
    - Attach the existing remark-dialog click listener to it. The dialog and save behaviour are unchanged.
  - [x] 3.10 Verify the toolbar and main screen:
    - Grep the touched files (`view_toolbar.xml`, `activity_main.xml`, `item_app.xml`, `ic_add.xml`, `ic_search.xml`, `AppsAdapter.kt`) for `#[0-9A-Fa-f]{3,8}`, `@color/primary_text`, `@color/white` and `@color/primary`: expect no matches.
    - `ToolBarColorStyle` is absent from the repo.
    - `:app:assembleDebug` succeeds.
    - Manual QA rows owned: 2 (toolbar, subtitle tap, status-bar icons API 23+), 3 (grid, badge, placeholder, dots), 4 (FAB + TalkBack "Choose App"), 5 (long-press menu), 6 (empty state), 15 (contrast for the badge, subtitle and FAB), in Light and Dark.

**Acceptance Criteria:**
- `:app:assembleDebug` succeeds.
- The toolbar is a surface toolbar in Main, List/FakeManager, GMS and Settings (the shared include).
- No hex, `@color/primary_text`, `@color/white` or `@color/primary` remain in the touched layouts, drawables or `AppsAdapter.kt`.
- The subtitle click works without `getChildAt(1)`.
- QA rows 2, 3, 4, 5, 6 and 15 (main-screen pairs) pass, meeting each Visual References `acceptance` criterion above.

---

### Task Group 4: List Screens (Choose App, Fake Location) and GMS Manager
**Dependencies:** 3
**Files to Modify:** app/src/main/res/layout/activity_list.xml, app/src/main/res/layout/item_package.xml, app/src/main/res/layout/item_fake.xml, app/src/main/res/layout/item_gms.xml, app/src/main/java/top/niunaijun/blackboxa/view/gms/GmsManagerActivity.kt
**Visual References:**
- mockup: analysis/design-context/mockups/choose-app-list-with-search.html
  element: screen:choose-app-list-with-search
  locator: `.tb` with the open `.search` (`.q` query text, `.cur` cursor, `.ic` icons) and `.content .row` (`.icon`, `.tx > .a` name, `.tx > .b` package, `.badge`) in both phone columns (line 255)
  acceptance:
  - The search container is `colorSurface` and the search card `colorSurfaceContainerHigh`.
  - The query text is onSurface, the hint and icons onSurfaceVariant, and the cursor primary.
  - Rows: the name is BodyLarge onSurface and the package name BodyMedium onSurfaceVariant. The badge is the tertiary-container pair.
  - The row order and height are unchanged.
- mockup: analysis/design-context/mockups/fake-location.html
  element: screen:fake-location
  locator: `.content` with the `.cat` header, `.row` items (`.tx > .a` name, `.tx > .b` location), `.map` and `.note` in both phone columns (line 255)
  acceptance: rows are name BodyLarge onSurface and fake location BodyMedium onSurfaceVariant, with a readable badge pair; the map picker chrome follows the theme background; the osmdroid tiles stay light (expected, not a defect)
- mockup: analysis/design-context/mockups/gms-manager-with-materialswitch.html
  element: screen:gms-manager-with-materialswitch
  locator: `.content .row` with `.tx > .a` title, `.tx > .b` summary and `.sw.on` / `.sw.off` switches in both phone columns (line 255)
  acceptance: each row uses a `MaterialSwitch` (M3 track and thumb, on = primary); the title is BodyLarge onSurface; toggling triggers the same install/uninstall dialogs and flows as before
**Estimated Steps:** 7

- [x] 4.0 Complete the list and GMS screens layer
  - [x] 4.1 Pre-check:
    - Read `activity_list.xml`, `item_package.xml`, `item_fake.xml`, `item_gms.xml` and `GmsManagerActivity.kt` (import, lines 82, 93, 107), plus `GmsAdapter.kt` to confirm it binds through ViewBinding only.
    - Confirm SimpleSearchView 0.2.0 attribute names (background, search-card background, text / hint / icon / cursor colors) from the resolved AAR `values.xml`.
  - [x] 4.2 `activity_list.xml`:
    - SimpleSearchView container background `?attr/colorSurface` (replacing `@color/primary`), search card background `?attr/colorSurfaceContainerHigh`, text `?attr/colorOnSurface`, hint and icons `?attr/colorOnSurfaceVariant`, cursor `?attr/colorPrimary`, all via the library's confirmed attributes (fallback `@color/md_theme_*`).
    - Apply dimens tokens.
  - [x] 4.3 `item_package.xml`:
    - Name: `?attr/textAppearanceBodyLarge` + `?attr/colorOnSurface`. Package name: `?attr/textAppearanceBodyMedium` + `?attr/colorOnSurfaceVariant`. Remove the inline `textSize` / `@color/primary_text`.
    - CornerLabelView badge: the tertiary-container pair (same decision as 3.6).
    - Apply dimens tokens.
  - [x] 4.4 `item_fake.xml`: the same pattern as 4.3 for name / fakeLocation and the badge; dimens tokens.
  - [x] 4.5 `item_gms.xml`:
    - Replace `android.widget.Switch` with `com.google.android.material.materialswitch.MaterialSwitch` (same id).
    - Title: `?attr/textAppearanceBodyLarge` + `?attr/colorOnSurface`; remove the inline `textSize` / `@color/primary_text`.
    - Apply dimens tokens.
  - [x] 4.6 `GmsManagerActivity.kt`: change the `android.widget.Switch` import and the type references at lines 82, 93 and 107 to `MaterialSwitch`. Only retype the existing `findViewById`, with no refactor (out of scope). The `GmsAdapter` binding type updates automatically; confirm it compiles.
  - [x] 4.7 Verify the list and GMS screens:
    - Grep the four layouts for `#[0-9A-Fa-f]{3,8}`, `@color/primary_text`, `@color/white`, `@color/primary` and `android.widget.Switch` / `<Switch`: expect no matches.
    - `:app:assembleDebug` succeeds.
    - Manual QA rows owned: 8 (choose app + search), 9 (fake-location list + map picker), 12 (GMS MaterialSwitch + flows), 15 (contrast for the list rows, badge and search), in Light and Dark.

**Acceptance Criteria:**
- `:app:assembleDebug` succeeds.
- No hard-coded light colors remain in `activity_list`, `item_package`, `item_fake` or `item_gms`, and `MaterialSwitch` replaces the framework `Switch`.
- The GMS install/uninstall flows are unchanged.
- QA rows 8, 9, 12 and 15 (list pairs) pass, meeting each Visual References `acceptance` criterion above.

---

### Task Group 5: Obsolete-Resource Cleanup and Documentation
**Dependencies:** 4
**Files to Modify:** app/src/main/res/values/colors.xml, RELEASE_NOTES.md, .maister/docs/project/roadmap.md, .maister/docs/project/tech-stack.md, .maister/docs/project/architecture.md, .maister/docs/INDEX.md
**Estimated Steps:** 5

- [x] 5.0 Complete cleanup and documentation
  - [x] 5.1 Pre-check and cleanup:
    - `grep -rn "@color/white\|R.color.white" app/src/main`. If there are zero consumers, remove `white` from `values/colors.xml`.
    - Confirm that `accent`, `primary_dark`, `primary_light` and `ToolBarColorStyle` have no references left.
    - Keep `primary_text`, `secondary_text`, the dead layouts and the density PNGs (out of scope).
  - [x] 5.2 Success-criteria greps (spec Success Criteria):
    - `forceDarkAllowed` absent from `app/src/main/res`.
    - No hex / `@color/primary_text` / `@color/white` / `@color/primary` in the 7 touched layouts, `ic_add.xml`, `ic_search.xml` or `AppsAdapter.kt`.
    - The 34 `md_theme_*` names are identical in `values/` and `values-night/`.
    - `git diff --stat Bcore/` is empty.
    - `git diff --stat gradle/libs.versions.toml app/build.gradle*` is empty.
  - [x] 5.3 `RELEASE_NOTES.md`: add a New Features entry per `docs/release-notes.md`: Material 3 theme with seed `#3F6FD8` and dynamic color on Android 12+; dark mode; a Settings → Appearance → Theme preference (default System default; Light/Dark apply immediately and persist). Give full repo-relative paths under Files Changed.
  - [x] 5.4 Project docs:
    - Tick the "Visual refresh / design system" and "Dark mode" items in `.maister/docs/project/roadmap.md`.
    - Update the theme lines in `tech-stack.md` and `architecture.md` (theme parent is now `Theme.Material3.DayNight.NoActionBar`, `values-night`, dynamic color, and where the theme mode is stored).
    - Update the matching `MaterialComponents` theme phrase in `.maister/docs/INDEX.md`.
  - [x] 5.5 Verify cleanup and docs: `:app:assembleDebug` succeeds after the removals. Every grep in 5.2 passes. The docs are consistent with each other.

**Acceptance Criteria:**
- All spec Success Criteria that can be checked by grep or git are green.
- `white` is removed if unreferenced, and no obsolete theme or color entry remains.
- The release notes and project docs reflect the new theme.
- The build succeeds.

---

### Task Group 6: Final Build and Full Manual QA
**Dependencies:** 1, 2, 3, 4, 5 (all previous groups)
**Files to Modify:** .maister/tasks/development/2026-09-27-material3-dark-mode/verification/manual-qa-checklist.md (new; QA record, no source changes unless QA fixes are routed back)
**Estimated Steps:** 5

- [x] 6.0 Complete final verification
  - [x] 6.1 Run the full build from `D:\KBlackbox`: `.\gradlew.bat assembleDebug` (or `bash ./gradlew assembleDebug`). All modules build, and the per-ABI debug APKs are produced.
  - [x] 6.2 Write `verification/manual-qa-checklist.md`:
    - The 16 rows from spec.md, each with Light / Dark / Dynamic color result columns.
    - The device matrix: an API 31+ device with a colorful wallpaper, an API 23–30 device or emulator, and optionally API 21–22 for the status bar.
    - A row-to-group map for routing failures: 1, 5, 7, 13 → G1; 10, 11, 14, 16 → G2; 2, 3, 4, 5, 6 → G3; 8, 9, 12 → G4; 15 → the owning group of the failing pair.
  - [x] 6.3 (user-confirmed QA 2026-09-27): Hand the checklist to the user, who installs the debug APK and performs all 16 rows × 3 modes on a device or emulator. Record pass/fail and notes per cell. The executor must not mark rows passed on the user's behalf.
  - [x] 6.4 (user-confirmed QA 2026-09-27): Route any failure to its owning group and fix it within the spec's pre-authorized fallbacks:
    - `md_color_title` / `md_color_content` for dialogs.
    - `@color/md_theme_*` for a third-party widget that ignores `?attr/`.
    - `@color/md_theme_background` for the splash.
    - Re-run `:app:assembleDebug` and the failed rows.
  - [x] 6.5 (user-confirmed QA 2026-09-27): Confirm the full spec Success Criteria:
    - The build is green with no new dependencies, and Bcore is unmodified.
    - All 16 rows pass in Light, Dark and Dynamic color with ≥4.5:1 contrast.
    - Existing flows (install, launch, users and remarks, long-press menu, GMS, fake location, shortcuts, settings switches with restart toast) behave as before.

**Acceptance Criteria:**
- `assembleDebug` succeeds for the whole project.
- The manual QA checklist is recorded with all 16 rows passing in Light, Dark and Dynamic color (or documented, accepted deviations from spec Risks, such as the ListPreference corner below API 28 and the material-dialogs input field shape).
- Every spec Success Criterion is satisfied.

## Execution Order

1. Task Group 1: Theme Foundation (12 steps, no dependencies)
2. Task Group 2: Theme-Mode Preference and Startup (7 steps, depends on 1)
3. Task Group 3: Surface Toolbar and Main Screen (10 steps, depends on 2)
4. Task Group 4: List Screens and GMS Manager (7 steps, depends on 3)
5. Task Group 5: Cleanup and Documentation (5 steps, depends on 4)
6. Task Group 6: Final Build and Full Manual QA (5 steps, depends on all)

The chain is strictly linear: no two groups run concurrently (shared Gradle build and lock; staged removals in `themes.xml` / `colors.xml`).

Step totals: G1 12, G2 7, G3 10, G4 7, G5 5, G6 5 = 46 numbered sub-steps.

## Standards Compliance

Follow the standards in `.maister/docs/standards/`:
- `global/` (always): `coding-style.md`, `commenting.md` (no change-history comments), `minimal-implementation.md` (only consumed roles, dimens and tokens; no dynamic-color toggle; no new classes), `kotlin-style.md` (existing loader accessor pattern), `error-handling.md` (allowlist mapping to `system`, no new blanket try/catch), `conventions.md` (release notes, docs, no new dependencies).
- `frontend/android-resources.md`: M3 DayNight with `values-night`, no `forceDarkAllowed`, `?attr/` / `@color/` tokens only, snake_case names, a `dimens.xml` 8dp grid, `TextAppearance.*`, `MaterialSwitch`, strings in en / zh-rCN / zh-rTW, settings through the `BlackBoxLoader` delegate.
- `frontend/android-views.md`: the shared `view_toolbar` include plus `initToolbar`.
- `frontend/android-feedback.md`: material-dialogs call sites untouched (themed only).
- `frontend/android-architecture.md`: settings reach the loader via `AppManager`; direct `BlackBoxCore` access only in the `App` lifecycle hook.
- `frontend/accessibility.md`: 4.5:1 contrast via on-* pairs, the FAB content description, the TalkBack check.
- `build/gradle.md`: `assembleDebug`; the version catalog is untouched.
- `docs/release-notes.md`: New Features entry format.
- `testing/test-writing.md`: no automated tests this task (team decision K5); manual QA only.

## Notes

- Verification-driven: each group opens with a pre-check (N.1) and closes with `:app:assembleDebug` plus its owned QA rows. There are no automated tests.
- Build incrementally: run only `:app:assembleDebug` per group. The full `assembleDebug` runs once, in Group 6.
- Mark progress: check off each numbered step as it is completed. The HTML companion uses `data-group` / `data-step` markers.
- Reuse first: `AppSharedPreferenceDelegate`, the `BlackBoxLoader` accessor pattern, `AppManager.mBlackBoxLoader`, `BlackBoxCore.get().isMainProcess()`, the `view_toolbar` include, MDC 1.12.0 M3 styles / `DynamicColors` / `MaterialSwitch` / `MaterialColors`, AppCompat `setDefaultNightMode`, the preference `useSimpleSummaryProvider`, and the existing `@string/choose_app`.
- Bcore must not be modified. `git diff --stat Bcore/` must stay empty throughout.
