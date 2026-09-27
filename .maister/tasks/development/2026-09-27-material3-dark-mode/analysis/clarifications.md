# Clarifications (Phase 1)

## TL;DR
The toolbar becomes an M3 surface toolbar. The palette is a custom M3 scheme with dynamic color on Android 12+. Settings get a System/Light/Dark theme choice. The Bcore LauncherActivity white flash stays out of scope and becomes a separate task.

## Key Decisions
- Toolbar: M3 surface style. Background is colorSurface; text and icons use colorOnSurface; no colored app bar. Chosen because it is standard M3 and reads well in both modes.
- Palette: a custom M3 scheme generated from a seed color, plus dynamic color (Material You) on Android 12+ (API 31+). Dynamic color needs a runtime guard.
- A theme-mode setting is in scope: a ListPreference with System / Light / Dark, default System, applied without an app restart via AppCompatDelegate.setDefaultNightMode.
- The Bcore LauncherActivity dark-mode flash is out of scope. It is a separate follow-up task, and Bcore resources stay unchanged in this task.

## Open Questions / Risks
- The seed/brand color for the custom palette is not chosen yet. Resolve it in the gap-analysis or spec phase.
- Still open: material-dialogs theming vs migration to MaterialAlertDialogBuilder, and dead-resource cleanup. Resolve them in the gap-analysis decisions.

## Q&A
| # | Question | Answer |
|---|----------|--------|
| 1 | Toolbar style in the new theme? | M3 surface (colorSurface / colorOnSurface) |
| 2 | Color palette? | Custom M3 palette + dynamic color on Android 12+ |
| 3 | Add a theme choice to settings (System/Light/Dark)? | Yes: ListPreference, default System, no restart |
| 4 | Fix the Bcore LauncherActivity white splash in this task? | No, separate task |
