# Scope Clarifications (Phase 2)

## TL;DR
No RELEASE_NOTES.md entry. build.yml uses the proposed artifact defaults: 4 groups (debug APKs, release APKs, Bcore AARs, JARs), 30-day retention, if-no-files-found: error.

## Key Decisions
- release-notes-entry: skip — user treats this as maintenance, no RELEASE_NOTES.md change.
- artifact-policy: use defaults — 30-day retention, fail when an artifact is missing, include debug-signed release APKs.
