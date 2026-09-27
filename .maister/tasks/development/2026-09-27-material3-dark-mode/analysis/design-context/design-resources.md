# Design Resources

## TL;DR
The binding standards are `.maister/docs/standards/frontend/android-resources.md`, android-views.md, android-feedback.md and accessibility.md. Current design-system files are `app/src/main/res/values/themes.xml` and `colors.xml` (monochrome #3F3F3F, which is being replaced). This task introduces the target tokens as a new M3 tonal scheme from seed #3F6FD8. No design skills or MCP tools apply.

## Standards (tier 1)
- standards/frontend/android-resources.md: Material 3 theme with `values-night`, theme attributes and `@color` tokens only, dimens on an 8dp grid, TextAppearance, MaterialSwitch.
- standards/frontend/android-views.md: `view_toolbar` include + initToolbar, ViewBinding.
- standards/frontend/android-feedback.md: dialogs, toasts, StateView empty/loading states.
- standards/frontend/accessibility.md: 48dp touch targets, 4.5:1 contrast in both modes, contentDescription on images.

## Design system (tier 2)
- theme: app/src/main/res/values/themes.xml (Theme.BlackBox, being replaced by an M3 DayNight theme)
- token: app/src/main/res/values/colors.xml (the mockups' `--md-sys-color-*` variables map 1:1 to the new M3 color roles)
- component-lib: MaterialToolbar (view_toolbar.xml), FloatingActionButton, WormDotsIndicator, CornerLabelView, SimpleSearchView, StateView, afollestad MaterialDialog, PreferenceFragmentCompat

## Skill hints (tier 3)
- None relevant: no Android/Material design skill or Figma MCP is available.
