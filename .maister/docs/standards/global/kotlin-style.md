## Kotlin Style

### Formatting
4-space indentation, no tabs, continuation indent 4 spaces (a few reformatted files use 8 — don't spread it). One primary class per file (related bean data classes may share a file). No runs of empty lines.

### Idioms
Beans are `data class`; stateless managers/utilities are `object`; shared helpers are top-level extension functions (`toast`, `getString`, `inflate`); use safe calls/`let` and string templates.

### Logging
`private const val TAG = "<ClassName>"` in a `companion object`; log failures as `Log.e(TAG, "Error <doing X>", e)` (pass the throwable), `Log.w` for degraded paths, `Log.d` for flow.

Source: code (app analysis, 53 files).
