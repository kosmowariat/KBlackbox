# Development Roadmap

## Current State
- **Version**: 4.0.0 (versionCode 400), forked from ALEX5402/NewBlackbox at commit `89b5983`
- **Key Features**: Virtual app install/launch/uninstall/clear-data, multi-user spaces (ViewPager pages), fake location (osmdroid map), GMS install/management, Xposed-style module list, VPN toggle, device spoofing and anti-detection, home-screen shortcuts, floating joystick ("rocker") for location.
- **Recent Updates (upstream)**: Build and Telegram distribution for all modules, removal of the aliyun Maven mirror, the VPN toggle, device-info logging, Android 14/15 fixes.

## Planned Enhancements (Next 3–6 Months)

### High Priority — GUI
- [ ] **Visual refresh / design system.** Migrate `Theme.BlackBox` from `MaterialComponents` to Material 3, define a real color palette (the primary/dark/light colors are all `#3F3F3F` today), and add typography and shape tokens.
- [ ] **Dark mode.** Add a `values-night` theme and remove `forceDarkAllowed=false`.
- [ ] **App grid and main screen UX.** Improve `AppsFragment` / `item_app.xml`: clearer install FAB flow, empty and loading states, better long-press actions, and user-space switching in `MainActivity`.
- [ ] **String externalization.** Move hard-coded dialog texts (e.g. the storage and VPN permission dialogs in `MainActivity`) to `strings.xml`, with `zh-rCN`/`zh-rTW` translations and optionally a Polish one.
- [ ] **Settings redesign.** Update `SettingFragment` (preference-ktx) to grouped Material 3 preferences.

### Medium Priority — UI architecture
- [ ] **Update UI dependencies.** lifecycle 2.3.1 → 2.8+, preference 1.1.1 → 1.2+, recyclerview 1.2 → 1.3+, work 2.7 → 2.9+.
- [ ] **Consolidate dialogs.** Replace `afollestad:material-dialogs` with `MaterialAlertDialogBuilder`.
- [ ] **Replace niche UI libraries.** Consider dropping `RVAdapter` (gitee), `CornerLabelView` and `SimpleSearchView` in favor of `ListAdapter`/`DiffUtil`, Material badges and `SearchView`.
- [ ] **Consistent MVVM.** Move from `LiveData` + manual `*Factory` classes (`InjectionUtil`) to `StateFlow` and `viewModel {}` initializers; later consider Hilt/Koin if the number of screens grows.
- [ ] **Possible Jetpack Compose adoption.** Evaluate this for new screens. The current UI is XML + ViewBinding.

### Technical Debt
- [ ] **Testing.** There is effectively no test suite: one file, `JarManagerTest.java`, sits in `src/main`. Add ViewModel unit tests and a few Espresso smoke tests for the main flows.
- [ ] **CI quality gates.** CI only builds and uploads to Telegram. Add `lint` and unit tests to the workflow.
- [ ] **Lint.** `Bcore` sets `checkReleaseBuilds false`. Re-enable lint for `app` at least.
- [ ] **Error handling in UI.** `MainActivity` wraps almost everything in `try/catch` + `Log.e`. Surface errors to users through a single consistent pattern.
- [ ] **Package/app ID.** The app still uses the upstream `top.niunaijun.blackbox` applicationId and debug signing for release. Decide on your own ID and signing config.

## Future Considerations
- **Feature ideas**: App import/export and backup, per-app spoofing profiles in the UI, onboarding flow for the required permissions (All Files Access, VPN), tablet/landscape layouts.
- **Platform**: `targetSdk` is 28 on purpose (the engine relies on legacy behaviors). Raising it is an engine-level decision, not a GUI one. Track Android 16+ hidden-API changes that may affect `Bcore`.

---
**Effort Scale**: `S`: 2–3 days | `M`: 1 week | `L`: 2+ weeks
*Generated during Maister init on 2026-09-26.*
