# Design Context Index

## TL;DR
There are 11 HTML mockups (light and dark side by side) of the M3 re-theme with seed #3F6FD8. The screen structure is unchanged; only colors, the toolbar style, the FAB shape, the switches, the dialog styling and the new Theme preference differ. Palette values are listed in `design-resources.md` and in the mockup CSS (`--primary`, `--surface`, ...). They are an approximation; the final values will be generated as M3 tonal palettes.

| ID | Type | Source | Description |
|----|------|--------|-------------|
| screen:main-apps-grid | screen | analysis/design-context/mockups/main-apps-grid.html | MainActivity: M3 surface toolbar with clickable "User 0" subtitle, WormDotsIndicator in primary, 4-column app grid with CornerLabelView badge, primaryContainer FAB |
| screen:main-empty-state | screen | analysis/design-context/mockups/main-empty-state.html | AppsFragment empty state with night-variant ic_empty illustration |
| screen:choose-app-list-with-search | screen | analysis/design-context/mockups/choose-app-list-with-search.html | ListActivity with re-themed SimpleSearchView and item_package rows (onSurface / onSurfaceVariant) |
| screen:settings-with-theme-preference | screen | analysis/design-context/mockups/settings-with-theme-preference.html | SettingActivity with the new "Appearance > Theme" ListPreference and existing "Others" switches |
| component:theme-picker-dialog | component | analysis/design-context/mockups/theme-picker-dialog.html | Theme ListPreference dialog (System default / Light / Dark) |
| screen:gms-manager-with-materialswitch | screen | analysis/design-context/mockups/gms-manager-with-materialswitch.html | GmsManagerActivity rows with MaterialSwitch |
| component:dialog-confirm | component | analysis/design-context/mockups/dialog-confirm-material-dialogs.html | afollestad MaterialDialog confirm styled via md_* attributes (surfaceContainerHigh, 28dp corners, primary buttons) |
| component:dialog-input | component | analysis/design-context/mockups/dialog-input-user-remark.html | MaterialDialog input variant (outlined TextInputLayout) |
| component:app-long-press-menu | component | analysis/design-context/mockups/app-long-press-menu.html | PopupMenu on the M3 menu surface |
| screen:splash-welcome | screen | analysis/design-context/mockups/splash-welcome.html | WelcomeActivity splash on ?android:attr/colorBackground (no white flash in dark mode) |
| screen:fake-location | screen | analysis/design-context/mockups/fake-location.html | FakeManagerActivity list and map picker; osmdroid tiles stay light |
