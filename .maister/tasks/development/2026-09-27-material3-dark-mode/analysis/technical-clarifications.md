# Technical Clarifications (Phase 5A)

## TL;DR
Night mode and dynamic color are set up in `App.onCreate`, gated by `BlackBoxCore.get().isMainProcess()`. DynamicColors carries a precondition that limits it to host (`top.niunaijun.blackboxa`) activities. The palette is a full M3 role set exported as `md_theme_*` colors in `values/colors.xml` and `values-night/colors.xml`, with one DayNight `Theme.BlackBox`. No new dependencies. Verification is manual QA only; no new tests.

## Key Decisions
- Initialization: `App.onCreate` with a main-process gate. It runs before the first activity, so the `:pN` guest processes and the server process are skipped. The DynamicColors precondition excludes Bcore's LauncherActivity and any non-host activity.
- Palette: a Material Theme Builder-style export (seed #3F6FD8) as `md_theme_*` color resources in `values/` and `values-night/`. A single `Theme.BlackBox` extends `Theme.Material3.DayNight.NoActionBar` and maps the roles; there are no separate day/night theme files.
- Testing: manual QA only, following a light/dark checklist for every screen, dialog, menu, empty/loading state and the splash. No new unit or instrumented tests (per team decision K5: tests are encouraged, not required).
- Dependencies: none added. preference-ktx stays 1.1.1 and core-splashscreen is not added. The splash is handled by a DayNight WelcomeTheme with a `?android:attr/colorBackground`-based background.

## Open Questions / Risks
- With no automated tests, regressions in theme resolution (for example an unreadable color in one screen) depend on thorough manual QA.
