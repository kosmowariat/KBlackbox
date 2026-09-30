# Google Play policy risk per feature type

## TL;DR
- Core product (virtual container) is not banned, but Play policy states container apps must not load apps declaring REQUIRE_SECURE_ENV and must not work around FLAG_SECURE. Our "disable FLAG_SECURE" feature collides with this directly: high risk.
- Broad permissions (All files access, QUERY_ALL_PACKAGES, REQUEST_INSTALL_PACKAGES, VpnService, Accessibility) are restricted to specific core purposes and need declarations; a sandbox arguably qualifies only weakly.
- Mock location, ID spoofing, hide-root, notification access and overlay were NOT confirmed in the fetched policy text: marked unknown/medium by inference.

## Key Decisions
- Recommend a separate "Play build" (or sideload-only distribution) that excludes FLAG_SECURE disabling, root hiding, and broad-permission features.
- Re-verify the policy pages in a browser before any submission (fetches were summarized by a small model; confidence medium).

## Open Questions / Risks
- Play Store publication undecided; targetSdk 28 is itself a blocker for new Play apps (Play requires a recent target API), so Play risk may be moot. (Not researched here; confidence: high from general knowledge, unverified in this run.)
- Mock location and notification listener pages not fetched.

## Risk table
Sources: Device and Network Abuse https://support.google.com/googleplay/android-developer/answer/9888379 ; Permissions https://support.google.com/googleplay/android-developer/answer/9888170 ; VpnService https://support.google.com/googleplay/android-developer/answer/12564964

| Feature | Risk | Basis (confidence) |
|---|---|---|
| Sandbox/virtual container (core) | medium | Containers allowed only if they respect REQUIRE_SECURE_ENV; also banned: installing apps without user consent (medium-high) |
| Disable FLAG_SECURE | high | Policy says not to "facilitate, or create workarounds to bypass" the flag (high) |
| Hide root / anti-detection | high | Falls under circumventing security measures; not explicitly named (medium) |
| Device/ID spoofing | medium-high | Not explicit; Deceptive Behavior and ad-ID rules likely relevant (low) |
| Fake/mock location | medium | Page not fetched; mock location is a developer-option API, apps are commonly on Play (low) |
| Per-app VPN/proxy | medium | VpnService needs a declaration, in-app disclosure, encryption; VPN must be core purpose or an allowed exception; manipulating other apps' traffic for monetization banned (high). Our use is an engine-internal setting, but a declaration would still be needed if the manifest uses VpnService |
| Install APK/XAPK from files/URLs | medium-high | REQUEST_INSTALL_PACKAGES limited to apps whose core function is enabling user-initiated installs; into-sandbox install may not count as it (medium) |
| Backup/export of app data | medium | All files access is restricted; use SAF/MediaStore instead to stay low (high) |
| GMS inside sandbox | medium | No explicit rule found; risk is ToS/integrity rather than Play policy (low) |
| Notification access | unknown | Not covered in fetched text; restricted-permission style review likely (low) |
| Accessibility features | high | Reserved for disability support; cannot be used to bypass security or act autonomously (high) |
| Overlay/floating windows | unknown-medium | Not covered in fetched text (low) |
| App lock with biometrics | low | Standard BiometricPrompt, no restricted permission (medium) |
| App freezing, running-apps manager | low | Internal to the sandbox, no special permission (medium) |
| Clipboard/share between spaces | low | In-app only; avoid background clipboard harvesting (medium) |
| Querying installed host apps (import from installed) | medium | QUERY_ALL_PACKAGES restricted; use targeted <queries> (high) |
