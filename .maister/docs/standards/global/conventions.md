## Development Conventions

### Predictable Structure
Organize files and directories in a logical, navigable layout.

### Up-to-Date Documentation
Keep README files current with setup steps, architecture overview, and contribution guidelines.

### Clean Version Control
Write clear commit messages, use feature branches, and add meaningful descriptions to pull requests.
In this project the CI posts the last commit subject as the Telegram artifact caption — write meaningful subjects.

### Environment Variables
Store configuration in environment variables; never commit secrets or API keys.

### Minimal Dependencies
Keep dependencies lean and up-to-date; document why major ones are included.

### Dependencies via the version catalog (K2)
Add new dependencies only to `gradle/libs.versions.toml` and reference them via `libs.*`; migrate existing inline coordinates opportunistically when touching them. Justify any new third-party UI library (prefer AndroidX/Material).

### Consistent Reviews
Follow a defined code review process with clear expectations for reviewers and authors.

### Testing Standards (K5)
Tests are encouraged, not required for now. Add unit tests for new ViewModel/repository logic where practical (see `testing/test-writing.md`).

### Feature Flags
Use flags for incomplete features instead of long-lived branches.

### Changelog Updates
Maintain a changelog or release notes for significant changes (see `docs/release-notes.md`).

### Build What's Needed
Avoid speculative code and "just in case" additions (see minimal-implementation.md).

### Licensing
The project is Apache 2.0. Keep upstream credits (VirtualApp, VirtualAPK, Dobby, xDL, BlackReflection, FreeReflection) in the README.

### Line Endings
`.gitattributes` `* text=auto` normalizes to LF in the repository (the working tree may be CRLF on Windows).
