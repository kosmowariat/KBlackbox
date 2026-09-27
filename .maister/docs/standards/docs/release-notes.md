## Release Notes & Project Docs

### Release notes structure
`RELEASE_NOTES.md` entries: a dated version header, then sections New Features, Bug Fixes, Removed Features, Stability Improvements, Known Issues, Compatibility (a table per Android version).
Each bug fix: **Problem / Root Cause / Solution / Files Changed** (full repo-relative paths). Features list the location in the UI, default value, behaviour and files changed.

### Known issues
Harmless OEM log noise is documented under Known Issues with the exact log line and why it is harmless (e.g. Oppo/ColorOS thermal stats `:p0`).

### Docs layout
`README.md` = overview, requirements, build, integration snippet, troubleshooting, credits; `Docs.md` = detailed API/user guide.
Keep them consistent with the build config (several claims are stale: JDK 17, API 26/root, x86, non-existent APIs such as clearAppData/setDebugMode).

Source: docs.
