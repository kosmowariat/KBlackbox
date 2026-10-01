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
- [x] **Replace niche UI libraries.** `RVAdapter`, `CornerLabelView` and `SimpleSearchView` are gone: lists use `util/BindingAdapter` (DiffUtil), the badge is a themed label, search is a standard `SearchView`.
- [x] **Consistent MVVM.** *Done 2026-09-30: all ViewModels expose `StateFlow` plus event `Channel`s and repositories are `suspend`; LiveData is gone. The manual `*Factory` classes (`InjectionUtil`) remain; consider `viewModel {}` initializers or Hilt/Koin if the number of screens grows.*
- [ ] **Possible Jetpack Compose adoption.** Evaluate this for new screens. The current UI is XML + ViewBinding.

### Technical Debt
- [ ] **Testing.** First JVM unit tests exist (`DataDirCopier`, `MathUtil`). Next: ViewModel unit tests and a few Espresso smoke tests for the main flows; `JarManagerTest.java` still sits in Bcore `src/main`.
- [x] **CI quality gates.** CI runs `app` lint and unit tests before building; failing runs upload the reports.
- [x] **Lint.** `app` lint runs clean of errors and gates CI (warnings remain); `Bcore` still sets `checkReleaseBuilds false`.
- [ ] **Error handling in UI.** `MainActivity` wraps almost everything in `try/catch` + `Log.e`. Surface errors to users through a single consistent pattern.
- [ ] **Release signing.** Release builds are still debug-signed. Decide on a proper signing config. *The applicationId is now `com.kosmowariat.appenclave` (app name APKEnclave).*

## Future Considerations
- **Feature ideas** (see `.maister/tasks/research/2026-09-30-feature-ideas/outputs/research-report.md`): space backup/export via SAF, per-app spoofing profiles in the UI, tablet/landscape layouts, app grid details sheet and clone-to-space, fake-location favourites and space picker, local diagnostics export, freeze/auto-stop idle apps. Biometric lock and shortcut hardening were deliberately dropped.
- **Shipped from that list (2026-09-30)**: Polish locale with in-app language picker, launcher shortcuts (recent apps, fake location), permissions and health screen, running indicator with "Stop all apps".
- **Shipped (2026-10-01)**: export and import of a space as a backup zip (apps, data, order; split APKs are not included).
- **Shipped (2026-10-01)**: app info and "Copy to another space" (with or without data) in the app menu; the app menu now opens on release after a long press, which ItemTouchHelper used to swallow.
- **Shipped (2026-10-01)**: About dialog (version, upstream credits, source link) with shareable logcat export.
- **Shipped (2026-10-01)**: fake location upgrade: space picker, coordinate entry, favourite places, and the "Preview" label dropped.
- **Platform**: `targetSdk` is 28 on purpose (the engine relies on legacy behaviors). Raising it is an engine-level decision, not a GUI one. Track Android 16+ hidden-API changes that may affect `Bcore`.

---
**Effort Scale**: `S`: 2–3 days | `M`: 1 week | `L`: 2+ weeks
*Generated during Maister init on 2026-09-26.*
