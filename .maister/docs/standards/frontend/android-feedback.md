## Dialogs, Toasts & Loading States

### Dialogs (target)
Target: `MaterialAlertDialogBuilder` (Material Components/M3), with titles/messages/buttons from string resources; confirm/cancel reuse `R.string.done` / `R.string.cancel`.
Legacy (migrate when touching): afollestad `MaterialDialog(ctx).show { ... }` DSL (used in all 5 current dialog sites). Don't add new usages.

### Toasts
Always use the `util/ToastEx.kt` helpers (`toast(@StringRes)`, `toast(String)`, `Context.toast`) — they cancel the previous toast. Never call `Toast.makeText` directly.

### Loading and empty states
List content states use StateView (`showLoading/showContent/showEmpty`); blocking operations (install/uninstall/GMS) use `LoadingActivity.showLoading()`. Every list screen must have an explicit empty state.

Source: code (app module analysis), user decision K3.
