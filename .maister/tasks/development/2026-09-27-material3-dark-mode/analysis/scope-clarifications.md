# Scope Clarifications (Phase 2)

## TL;DR
The theme moves to M3 DayNight with a custom blue palette (seed ~#3F6FD8). Dynamic color is always on for API 31+ (main process and host activities only). material-dialogs is styled through `md_*` attributes. Typography/shape tokens go into the theme and into the layouts we touch anyway. Only obsolete theme/color entries are removed (dead layouts stay). Map tiles stay light. The toolbar subtitle click is re-implemented robustly.

## Key Decisions
- Seed color: blue ~#3F6FD8, with neutral surfaces. Light and night schemes are generated from it (Material Theme Builder style tonal palettes).
- material-dialogs: styled via `md_background_color`, `md_color_button_text`, `md_corner_radius` (28dp) in the theme. Call sites stay unchanged. Migrating to MaterialAlertDialogBuilder remains a separate roadmap task.
- Token breadth: theme-level `textAppearance*` / `shapeAppearance*`, plus TextAppearance + `dimens.xml` only in layouts already touched by this task.
- Dead-resource cleanup: remove only the obsolete theme/color entries (ToolBarColorStyle, `accent`, `primary_dark`, `primary_light`). KEEP the dead layouts (view_switch, activity_xp, item_xp, item_viewpager) and the shadowed PNGs for a later task.
- osmdroid map: tiles stay light; only the screen chrome is themed.
- Toolbar subtitle click (MainActivity.kt:253, `getChildAt(1)`): replace with a robust lookup that does not depend on child order, e.g. find the TextView whose text equals the subtitle, or use a custom subtitle view.
- Dynamic color: always on for API 31+ with no user toggle. It applies only in the main process and only to host activities.

## Open Questions / Risks
- The dead layouts keep their hard-coded colors and the old toolbar include. That is acceptable only while they stay unreferenced.
- Colors from Material You differ per device, so the custom palette is what users see on API < 31 or when the wallpaper provides no palette.

## Carried-over decisions (Phase 1)
- The toolbar uses the M3 surface style.
- Settings get a theme-mode ListPreference (System/Light/Dark, default System). It applies without a restart, and the loader delegate is its single source of truth.
- The Bcore LauncherActivity flash is out of scope.

## Settled by standards (no decision needed)
- Remove `forceDarkAllowed=false`.
- Replace the framework Switch in item_gms with MaterialSwitch.
- Re-theme SimpleSearchView.
- Give the splash a DayNight theme with a night-aware background (no new dependency).
- Add a night variant of `ic_empty`.
- Strings go into en, zh-rCN and zh-rTW.
