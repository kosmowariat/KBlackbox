## Java Style

### Formatting
4-space indentation (no tabs), K&R braces, no wildcard imports.

### Import order
Groups separated by blank lines: `android.*`/`androidx.*` → `java.*`/`javax.*` → `black.*` → `top.*` → static imports (Android Studio default).

### Field naming
Private/protected instance fields use the `m` prefix (`mService`); static non-final fields use `s` (`sService`); `static final` constants are `UPPER_SNAKE_CASE`; public data fields on entities/records are plain names.

Source: code (Bcore analysis, 494/501 files).
