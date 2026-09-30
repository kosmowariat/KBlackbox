# Synthesis: Feature ideas for APKEnclave

Tags: **[app]** codebase-app-gaps, **[engine]** codebase-engine-gap-analysis, **[docs]** docs-roadmap-and-claims, **[comp]** external-competitors-feature-matrix, **[plat]** external-platform-enablers, **[policy]** external-policy-google-play. Full ranked table, sequencing and risk view: `outputs/research-report.md`.

## TL;DR
- Dominant pattern: the engine is much richer than the UI. The app uses about 25 engine entry points [engine A]; most cheap features are "wire an existing capability to a screen".
- Three independent sources converge on the same top ideas: biometric/space lock (comp F1,F2,F7,F8 + plat 5 + app F5), backup/export (docs #1 + engine C + plat 7 + app F8), permissions onboarding (docs #3 + engine B4 + app F4).
- Ten ranked features are UI-only. Bcore work is justified later for notification mute, per-app hideRoot/FLAG_SECURE, permission deny-list, spoof profiles.
- Stale documentation is the main planning hazard (Docs.md APIs, README platform claims, roadmap Xposed/VPN lines).

## Key Decisions
- UI-only first; Bcore only when a shipped UI feature needs it (`vision.md:60` via [docs]).
- Shortcut hardening precedes the lock, since shortcuts bypass host UI [app F2, plat 5].
- Default to sideload/GitHub distribution; Play is undecided and blocked by targetSdk 28 [plat 12, policy].

## Open Questions / Risks
- No device execution; one phone (Android 13) [engine, app].
- `isRunningApplication` reliability (reflection on `mTasks`) [engine].
- Play target-API facts from summarised fetch [plat].
- Lock bypass paths beyond host entry points unverified [comp, plat].

---

## 1. Research question
What features could APKEnclave (Android virtual-app sandbox, solo fork of NewBlackbox, Material 3 UI, not yet public) still add?

## 2. Cross-source analysis

### Validated findings (confirmed by 2+ sources)
| Finding | Sources | Confidence |
|---|---|---|
| Host has no biometric/app lock | app F5 (no `BiometricPrompt`/`androidx.biometric`), engine C (lock is UI-only S), plat 5 (dep not in catalog) | High |
| Lock is the most externally validated feature | comp (4 of 7 families), plat 5 (value high), policy (low risk) | Medium |
| Backup/export can be host-side, no Bcore change | engine C, app F8 (`DataDirCopier`), plat 7 (SAF), docs #1 | Medium-High |
| Permission onboarding is half-built | app F4 (dead broadcast, one dialog), engine B4 (helpers unused), docs #3 | High |
| Shortcut support is pinned-only and unsafe | app F2, plat 1 (no `shortcuts.xml`, no dynamic) | High |
| Polish locale is missing though roadmap ticks it | app F11, docs #9 | High |
| Fake location is user-0 only in UI | app F6 (`MainActivity.kt:279`), engine B3 (API per user/pkg) | High |
| Docs.md API list mostly fictional | engine D (zero definitions), docs C | High |
| Xposed and host VPN are removed | docs (`runtime.md:6-10`), engine C (no VPN code) | High |
| Play blocks targetSdk 28 | plat 12, policy Open Questions | Medium |

### Contradictions and resolution
See report Section 8 (C1-C8). Key ones: icon variants exist only as assets (brief vs app/plat 14); target-API rule is summary-level in both external findings; lock needs "maybe an engine hook" (comp) vs "UI gate" (plat) — both true, scope differs.

### Evidence quality
- High: code citations with file:line, official Android docs, Island/Private Space/Shelter pages.
- Medium: competitor Play listings and aggregator articles, policy pages summarised by a small model.
- Low: Play risk for mock location, notification access, overlay (not fetched); running-app detection accuracy.

## 3. Patterns and themes

| Pattern | Type | Evidence | Prevalence | Assessment |
|---|---|---|---|---|
| Engine capability without UI | Architectural | engine B (18 rows) | Pervasive: running state, jobs, accounts, notifications, location extras, hidden state, lifecycle callback, Slog | Main source of cheap features |
| Half-finished wiring | Implementation | app F4 (dead broadcast), F7 (`requestInstallPackage` false, URL install without UI), F12 (unused callbacks) | 5+ places | Completing these beats new invention |
| Single reusable utilities (`DataDirCopier`, `installCopyForUser`, `ShortcutUtil`, filter pattern in `ListActivity`) | Design | app F1,F2,F8,F9 | 4 | Enables S/M features with low code |
| Blanket grants and global flags in engine | Implementation | engine C, B15 (`IAppOpsManagerProxy.java:112-202`, global `hideRoot`) | Engine-wide | Per-app control means Bcore change; defer |
| Stale/aspirational documentation | Organizational | docs C, engine D | README, Docs.md, roadmap, vision, tech-stack | Fix before public release |
| Privacy-UX as competitor baseline | Market | comp F1-F8 | lock, hide, disguise, freeze, notification control in most families | APKEnclave behind on all |
| Reliability as complaint theme | Market | comp F10 | notifications, battery, crashes, ads | Differentiator: ad-free, diagnostics |
| Everything policy-sensitive is engine-side | Policy | policy table (FLAG_SECURE high, hide root high) | 2 shipped toggles | Isolate per build flavour later |
| Platform APIs fit without targetSdk change | Integration | plat summary matrix | Shortcuts, biometric, locale, SAF, icon alias | Host-side features are safe at targetSdk 28 |

## 4. Key insights

1. **The UI is the bottleneck, not the engine.** Ten UI-only features exist; evidence engine A/B and app table. Implication: next weeks should avoid Bcore. Confidence Medium-High.
2. **Shortcuts are simultaneously a feature and a security hole.** `ShortcutActivity` exported, no validation [app F2]; a lock without hardening is bypassable [plat 5]. Implication: ordering constraint for the batch. Confidence High.
3. **Backup is the best roadmap-named win and needs no engine work**, but is the only M item in the top tier and needs stopped-app handling [engine C, plat 7]. Confidence Medium-High.
4. **Android 15 Private Space raises the bar for the lock**: it must be per-space and work on older versions [comp F8]. Confidence High.
5. **Policy exposure comes from existing toggles, not new ideas.** FLAG_SECURE disable and hide root are high-risk; new UI features are mostly low [policy]. Implication: isolate these two if Play is ever pursued. Confidence Medium.
6. **Play may be moot**: targetSdk 28 blocks the Play target-API rule [plat 12]. Implication: do not spend effort on Play-only variants yet. Confidence Medium.
7. **Docs cannot be the source of truth**: Docs.md API list, README platform claims [engine D, docs C]. Implication: verify each idea against source; add a docs cleanup task. Confidence High.
8. **Reliability beats novelty for retention**: complaints cluster on notifications, battery, crashes [comp F10]; running-apps manager, freeze and local diagnostics address that. Confidence Medium.

## 5. Relationships and dependencies

```
ShortcutActivity hardening --> Biometric lock (must gate shortcuts)
Permissions/health screen  --> Restart button --> faster verification of all engine settings
Running indicator          --> Stop-all --> Task manager --> Freeze/auto-stop
DataDirCopier + BEnvironment --> Duplicate-space (exists) --> Clone app to space --> Backup/export
FakeLocationRepository (per user/pkg) --> Space picker --> Favourites
Polish locale --> localeConfig --> per-app language row
Hide/freeze state (needs small Bcore setter) --> Freeze feature beyond stop
Notification mute --> Bcore hook edit (INotificationManagerProxy.EnqueueNotificationWithTag)
```
Integration boundary: UI reaches the engine only through `data/` repositories (`architecture.md` via [docs]); every new feature adds a repository method, not direct engine calls.

## 6. Gaps and uncertainties
- Not executed: all "exists" claims are static.
- Android versions other than 13, and Android 16 hook breakage, unverified [plat 16].
- Competitor demand not proven by feature presence [comp Open Questions].
- Play risk unknown for mock location, notification access, overlay [policy].
- Guest-side effects of host shortcut/locale/notification features depend on engine hooks not tested [plat Open Questions].
- Launcher icon `monochrome` layer: app finding says unverified, plat says none.

## 7. Synthesis by framework (mixed)

**Technical (component/flow)**: host app (3 screens + fake location, GMS, settings, APK picker) on repositories on the `BlackBoxCore` facade; ~25 calls used. Flow for new features: View → ViewModel (StateFlow) → repository → `BlackBoxCore`/`B*Manager`.

**Requirements**: explicit documented needs are only four (backup/export, spoof-profile UI, permission onboarding, tablet layouts) [docs]; implied needs are per-space controls [docs B]. Constraints: solo dev, targetSdk 28, no Xposed/VPN/telemetry, one test phone. Gaps: spoof profile has no engine support yet [engine C], so the UI item is L, not UI-only.

**Literature/competitors**: current state lacks lock, hide, disguise, freeze, notification control; strengths are ad-free, M3 UI, per-space duplication and GMS manager. Best practices from Secure Folder/Private Space: auto-lock timer, suppress notifications when locked, hidden entry.

**Applicability**: fits — lock, shortcuts, locale, SAF backup, health screen. Does not fit now — notification listener, accessibility, QS tile for launch-only, Glance widgets, Auto Backup.

## 8. Conclusions
- Primary: add the ten UI-only features in the report's order; first batch = housekeeping, locale, shortcuts (hardened), health screen, running indicator, lock.
- Secondary: go Bcore only for notification mute, per-app hideRoot/FLAG_SECURE, permission deny-list, then spoof profiles; keep Play a later, separate decision; clean stale docs before going public.
- Overall confidence: Medium.
