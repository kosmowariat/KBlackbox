# Visual Coverage Matrix

## TL;DR
All 11 screens and components in `analysis/design-context/INDEX.md` are covered by at least one task group (11/11, 100%).
Group 1 (theme foundation) covers the theme-only surfaces: splash, both material-dialogs variants, and the popup menu.
Groups 2–4 cover the screens with layout or code changes. `component:app-long-press-menu` is covered twice: by the theme in G1, and by a regression check in G3.
Group 6 re-checks every item through the full manual QA checklist.

Source: `analysis/design-context/INDEX.md`

| Screen/Component ID | Covered By Task Group(s) | QA rows | Status |
|---------------------|--------------------------|---------|--------|
| screen:main-apps-grid | Group 3 (Surface Toolbar and Main Screen) | 2, 3, 4, 15 | ✅ |
| screen:main-empty-state | Group 3 (Surface Toolbar and Main Screen) | 6 | ✅ |
| screen:choose-app-list-with-search | Group 4 (List Screens and GMS Manager) | 8, 15 | ✅ |
| screen:settings-with-theme-preference | Group 2 (Theme-Mode Preference and Startup) | 10, 16 | ✅ |
| component:theme-picker-dialog | Group 2 (Theme-Mode Preference and Startup) | 11, 16 | ✅ |
| screen:gms-manager-with-materialswitch | Group 4 (List Screens and GMS Manager) | 12 | ✅ |
| component:dialog-confirm | Group 1 (Theme Foundation) | 13 | ✅ |
| component:dialog-input | Group 1 (Theme Foundation) | 13 | ✅ |
| component:app-long-press-menu | Group 1 (Theme Foundation), Group 3 (Surface Toolbar and Main Screen) | 5 | ✅ |
| screen:splash-welcome | Group 1 (Theme Foundation) | 1 | ✅ |
| screen:fake-location | Group 4 (List Screens and GMS Manager) | 9 | ✅ |

The shared surface toolbar (`view_toolbar.xml`, Group 3) also restyles the toolbars shown in `screen:choose-app-list-with-search`, `screen:settings-with-theme-preference`, `screen:gms-manager-with-materialswitch` and `screen:fake-location`. Group 6 re-verifies all 11 items in Light, Dark and Dynamic color.

## Uncovered Items

All screens covered.
