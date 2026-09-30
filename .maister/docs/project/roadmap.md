# Development Roadmap

## Current State
- **Version**: 4.0.0 (versionCode 400), forked from ALEX5402/NewBlackbox at commit `89b5983`
- **Key Features**: Virtual app install/launch/uninstall/clear-data, multi-user spaces (ViewPager pages), fake location (osmdroid map), GMS install/management, Xposed-style module list, VPN toggle, device spoofing and anti-detection, home-screen shortcuts, floating joystick ("rocker") for location.
- **Recent Updates (upstream)**: CI builds for all modules, removal of the aliyun Maven mirror, the VPN toggle, device-info logging, Android 14/15 fixes.

## Planned Enhancements (Next 3–6 Months)

### High Priority — GUI
- [x] **Visual refresh / design system.** Migrate `Theme.BlackBox` from `MaterialComponents` to Material 3, define a real color palette (the primary/dark/light colors are all `#3F3F3F` today), and add typography and shape tokens. *Done 2026-09-27: `Theme.Material3.DayNight.NoActionBar`, custom blue `md_theme_*` palette (seed `#3F6FD8`), dynamic color on Android 12+, typography/shape tokens and `dimens.xml`.*
- [x] **Dark mode.** Add a `values-night` theme and remove `forceDarkAllowed=false`. *Done 2026-09-27: `values-night` palette plus a Settings → Appearance → Theme preference (System default / Light / Dark).*
- [x] **App grid and main screen UX.** *Done 2026-09-30: users list is the main screen (list/grid toggle, launch apps, rename/duplicate/delete), per-user apps screen with a labelled "Add app" FAB, loading and empty states.*
- [x] **String externalization.** Move hard-coded dialog texts (e.g. the storage permission dialog in `MainActivity`) to `strings.xml`, with `zh-rCN`/`zh-rTW` translations and optionally a Polish one.
- [x] **Settings redesign.** Grouped preferences (Appearance, Sandbox, Google services) with icons and summaries.

### Medium Priority — UI architecture
- [x] **Update UI dependencies.** lifecycle 2.8.7, preference 1.2.1, recyclerview 1.3.2, work 2.9.1, all in the version catalog.
- [x] **Consolidate dialogs.** `afollestad:material-dialogs` is gone; dialogs use `MaterialAlertDialogBuilder` (helpers in `util/DialogEx.kt`).
- [ ] **Replace niche UI libraries.** Consider dropping `RVAdapter` (gitee), `CornerLabelView` and `SimpleSearchView` in favor of `ListAdapter`/`DiffUtil`, Material badges and `SearchView`.
- [ ] **Consistent MVVM.** Move from `LiveData` + manual `*Factory` classes (`InjectionUtil`) to `StateFlow` and `viewModel {}` initializers; later consider Hilt/Koin if the number of screens grows.
- [ ] **Possible Jetpack Compose adoption.** Evaluate this for new screens. The current UI is XML + ViewBinding.

### Technical Debt
- [ ] **Testing.** There is effectively no test suite: one file, `JarManagerTest.java`, sits in `src/main`. Add ViewModel unit tests and a few Espresso smoke tests for the main flows.
- [ ] **CI quality gates.** CI only builds and uploads artifacts. Add `lint` and unit tests to the workflow.
- [ ] **Lint.** `Bcore` sets `checkReleaseBuilds false`. Re-enable lint for `app` at least.
- [ ] **Error handling in UI.** `MainActivity` wraps almost everything in `try/catch` + `Log.e`. Surface errors to users through a single consistent pattern.
- [ ] **Release signing.** Release builds are still debug-signed. Decide on a proper signing config. *The applicationId is now `com.kosmowariat.appenclave` (app name APKEnclave).*

## Future Considerations
- **Feature ideas**: App import/export and backup, per-app spoofing profiles in the UI, onboarding flow for the required permissions (All Files Access), tablet/landscape layouts.
- **Platform**: `targetSdk` is 28 on purpose (the engine relies on legacy behaviors). Raising it is an engine-level decision, not a GUI one. Track Android 16+ hidden-API changes that may affect `Bcore`.

---
**Effort Scale**: `S`: 2–3 days | `M`: 1 week | `L`: 2+ weeks
*Generated during Maister init on 2026-09-26.*
