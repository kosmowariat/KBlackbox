# Manual QA Checklist: Material 3 + Dark Mode

## TL;DR
- Install the debug APK and check all 16 rows below in Light, Dark and Dynamic color. Put the results in the empty cells.
- Priority devices: an API 31+ device with a colorful wallpaper (needed for Dynamic color) and one API 23–30 device or emulator.
- Send each failure to its owning group using the routing map. Fixes must stay within the spec's pre-authorized fallbacks.
- Some deviations are already accepted and listed at the end. They do not count as failures.
- Status: **not yet executed**. A human fills in every cell. Nothing here is pre-marked as passed.

## How to install

The debug build produces these APKs under `app/build/outputs/apk/debug/`:
- `BlackBox_4.0.0_arm64-v8a-debug.apk`: most modern phones.
- `BlackBox_4.0.0_armeabi-v7a-debug.apk`: older 32-bit devices.
- `BlackBox_4.0.0_universal-debug.apk`: any device. There is no x86 split, so use an ARM system image, or an emulator with ARM translation.

```bash
adb install -r app/build/outputs/apk/debug/BlackBox_4.0.0_arm64-v8a-debug.apk
```

Or copy the APK to the device and install it from a file manager (allow unknown sources).

Theme mode switches: the in-app setting is Settings → Appearance → Theme (System / Light / Dark). The OS toggle is Quick Settings → Dark theme (this affects the app only when Theme = System).

Dynamic color: on API 31+, set a colorful wallpaper (Settings → Wallpaper & style), then relaunch the app. It uses wallpaper-derived colors in both light and dark.

## Device matrix

| Device | API | Required | Modes to cover | Tester / date |
|---|---|---|---|---|
| Physical device or emulator with a colorful wallpaper | 31+ | Yes | Light, Dark, Dynamic color | |
| Device or emulator | 23–30 | Yes | Light, Dark (no dynamic color; status-bar icons readable) | |
| Device or emulator | 21–22 | Optional | Light, Dark (status bar check) | |

## QA rows

How to fill the cells: `PASS`, `FAIL: <short note>`, or `N/A`. Record Dynamic color only on API 31+.

| # | Area | Check | Light | Dark | Dynamic color | Notes |
|---|---|---|---|---|---|---|
| 1 | Splash (WelcomeActivity) | No white flash in dark mode; background matches the theme; icon centered | | | | |
| 2 | Main: toolbar | Surface background; title onSurface; subtitle "User N" onSurfaceVariant and **tappable** (opens remark dialog, save updates subtitle); overflow icon visible; status-bar icons readable (API 23+) | | | | |
| 3 | Main: grid | App names readable; XP badge tertiary-container pair readable; placeholder icon tone fits the theme; dots indicator visible in primary | | | | |
| 4 | Main: FAB | primaryContainer with a readable + icon; TalkBack reads "Choose App" | | | | |
| 5 | Main: long-press popup menu | M3 menu surface, readable items in both modes | | | | |
| 6 | Main: empty state | Night illustration used in dark mode; text readable | | | | |
| 7 | Loading (install/uninstall/GMS) | CatLoadingView background readable with white label in both modes | | | | |
| 8 | Choose app (ListActivity) | Search icon visible; open search: card surface, text, hint, cursor and back icon readable; rows readable; badge readable | | | | |
| 9 | Fake location list + map picker | Rows and badge readable; map tiles light (expected); chrome themed | | | | |
| 10 | Settings | "Appearance" is the first category; Theme summary shows the current entry; existing switches work, show the restart toast, and persist as before | | | | |
| 11 | Theme picker | Dialog readable; System, Light and Dark each apply immediately without a restart toast; the choice persists after force-stop and relaunch; System follows the OS toggle live | | | | |
| 12 | GMS manager | MaterialSwitch renders and toggles; install/uninstall dialogs and flows unchanged | | | | |
| 13 | material-dialogs (confirm + input: user remark, shortcut name, uninstall, clear, stop) | surfaceContainerHigh background, primary buttons, rounded corners, readable title, message and input | | | | |
| 14 | Guest isolation | Launch a guest app: its UI is not recolored by dynamic color or night mode; Bcore LauncherActivity looks as before (its flash is a known separate issue) | | | | |
| 15 | Contrast | Spot-check text and icon pairs against 4.5:1 in both modes (badge, toolbar subtitle, dialog buttons, loading label) | | | | |
| 16 | TalkBack | Settings Theme preference and dialog focus order, FAB label | | | | |

## Extra checks found during implementation

Record these in the Notes cell of the matching row.

| Row | Extra check | Result |
|---|---|---|
| 2 | Tapping the subtitle "User N" still opens the remark dialog, including after swiping between users (the subtitle must update and stay tappable for each user). | |
| 8 | SimpleSearchView cursor color. The library sets it via reflection, so on newer Android it may stay the default accent. Note whether it is readable, not whether it matches `primary`. | |
| 11 | `theme_mode` is absent from the default prefs on a fresh install, before the theme is changed: `adb shell run-as top.niunaijun.blackbox cat shared_prefs/top.niunaijun.blackbox_preferences.xml` (no `theme_mode` key until the user picks a value; after picking, the key holds `system`/`light`/`dark`). | |
| 13 | material-dialogs title and content are readable in Dark. If not, apply the pre-authorized fallback: set `md_color_title` / `md_color_content` in the theme. | |
| 14 | A guest app running in a `:pN` process keeps its own colors (no dynamic color, no forced night). Bcore `LauncherActivity` (the splash shown when a guest launches) is not dynamic-colored. Its white flash in Dark is a **known out-of-scope issue** (Bcore is unchanged) and does not count as a failure. | |

## Failure routing map

| Rows | Owning group |
|---|---|
| 1, 5, 7, 13 | G1 Theme Foundation |
| 10, 11, 14, 16 | G2 Theme-Mode Preference and Startup |
| 2, 3, 4, 5, 6 | G3 Surface Toolbar and Main Screen |
| 8, 9, 12 | G4 List Screens and GMS Manager |
| 15 | The group that owns the failing text/icon pair (badge/subtitle/FAB → G3; list rows/search → G4; dialog buttons/loading label → G1) |

Row 5 is shared between G1 (menu theme overlay) and G3 (main-screen popup usage). Start with G1.

Pre-authorized fallbacks for fixes (spec):
- Dialogs: `md_color_title` / `md_color_content`.
- A third-party widget that ignores `?attr/`: `@color/md_theme_*`.
- Splash: `@color/md_theme_background`.
- After any fix, re-run `./gradlew :app:assembleDebug` and repeat the failed rows.

## Accepted deviations (not failures)

- The ListPreference (Theme picker) dialog corner is less than 28dp below API 28.
- The exact shape of the material-dialogs input field may differ from the M3 text-field spec.
- GMS rows have no summary line.
- osmdroid map tiles stay light in Dark mode (row 9, expected).
- The Bcore LauncherActivity white flash on guest launch in Dark mode (row 14, out of scope, separate task).

## Sign-off

| Item | Result |
|---|---|
| All 16 rows pass in Light / Dark / Dynamic color (or accepted deviation) | |
| Existing flows unchanged (install, launch, users and remarks, long-press menu, GMS, fake location, shortcuts, settings switches with restart toast) | |
| Tester / date | |
