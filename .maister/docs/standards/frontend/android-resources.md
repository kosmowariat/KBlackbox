## Android Resources, Theming & i18n

### Layout naming
`activity_<screen>`, `fragment_<screen>`, `item_<row>`, `view_<widget>`. Library-mandated names (e.g. `base_empty.xml`) are exempt.

### Resource naming
Strings, colors, dimens, styles, menu ids and preference keys: `snake_case`. Menu item ids are prefixed with the menu/screen (`main_setting`, `list_search`).
Colors have semantic names (`primary`, `primary_text`), never hex-based. Layout view ids: `camelCase` (`recyclerView`, `stateView`); the toolbar include keeps `toolbar_layout`.

### Theme and colors (target)
Target: Material 3 theme (`Theme.Material3.*`) with a real color scheme (primary/secondary/surface/on-* tokens), a `values-night` variant, and dark mode allowed (remove `forceDarkAllowed=false`).
Layouts reference colors only via theme attributes (`?attr/colorPrimary`, `?attr/colorOnSurface`) or `@color/` tokens — never hex literals in layouts or Kotlin.
Legacy: `Theme.BlackBox` extends `Theme.MaterialComponents.Light.NoActionBar`, all primary colors are `#3F3F3F`; `WelcomeTheme` extends AppCompat.

### Dimensions and typography (target)
Target: spacing/size tokens in `values/dimens.xml` on an 8dp grid (8/16/24dp spacing, 48dp icons and minimum touch targets), and text via `TextAppearance.*` styles (M3 type scale) instead of inline `textSize`/`textColor`.
Legacy: all dimensions are inline literals; there is no dimens.xml and no text-appearance styles.

### Layout structure
Screen layouts use `ConstraintLayout` as root (toolbar constrained to top, content `0dp` between toolbar and bottom). Simple list rows may use `LinearLayout`.
Use `match_parent` (never `fill_parent`); use Material widgets (`MaterialSwitch`/`SwitchMaterial`, not `android.widget.Switch`).

### User-visible text and translations
All user-visible text (titles, dialogs, buttons, toasts, error messages, preference titles, placeholders like "User %d") comes from `res/values/strings.xml` with format args; no string literals in Kotlin or layouts.
Every new key must also be added to `values-zh-rCN` and `values-zh-rTW` (4 keys are currently missing there; ~15 legacy hard-coded literals exist, mainly MainActivity permission dialogs and AppsRepository messages).

### Settings screen
Settings are a `PreferenceFragmentCompat` loading `res/xml/setting.xml`, hosted in `SettingActivity`. A new toggle = `SwitchPreferenceCompat` with a snake_case key + title/summary strings + handling in `SettingFragment.kt`, persisted through `BlackBoxLoader`'s `AppSharedPreferenceDelegate`.
Engine-affecting settings flow app settings → `ClientConfiguration` → `BlackBoxCore` and need an app restart (tell the user via `R.string.restart_module`).

### Manifest and namespaces
App namespace `top.niunaijun.blackboxa` (applicationId `top.niunaijun.blackbox`), Bcore namespace `top.niunaijun.blackbox`. Activities use the app theme; only the launcher uses the splash theme.

Source: code, config, docs (RELEASE_NOTES), user decision K3.
