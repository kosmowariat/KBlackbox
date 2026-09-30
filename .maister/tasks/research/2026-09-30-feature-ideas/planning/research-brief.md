# Research Brief: Feature ideas for APKEnclave

## TL;DR
Find which features APKEnclave (Android virtual-app sandbox, fork of NewBlackbox, solo project, M3 UI, not yet public) could add next. Mixed research: what the codebase and engine already support or half-support, what the roadmap lists, what comparable sandbox/cloner apps offer, and which ideas fit the project constraints.

## Key Decisions
- Research type: mixed (codebase capability + external competitor/market scan + roadmap review).
- Output: a prioritized feature list with effort, value, risk and engine-vs-UI split; not an implementation spec.

## Open Questions / Risks
- Play Store publication is undecided; features must be tagged with Google Play policy risk (a separate Play build may exclude them).
- Engine changes (Bcore) are costly and risky; UI-only features are cheap.

## Research question
"What features could we still add to APKEnclave?" (user, Polish: "jakie ficzery możemy jeszcze dodać?")

## Context
- App: APKEnclave (`com.kosmowariat.appenclave`), fork of ALEX5402/NewBlackbox. Modules: `app` (Kotlin UI, M3, StateFlow MVVM), `Bcore` (Java/C++ virtualization engine), `black-reflection`, `compiler`.
- Current features: users (spaces) list/grid, create/rename/duplicate/delete user (duplicate with per-app data copy), add/launch/stop/clear/uninstall/shortcut per app, drag-reorder apps, GMS install per user, fake location (osmdroid map + joystick), hide root, daemon, disable FLAG_SECURE, theme mode, launcher icon variants.
- Constraints: solo dev; engine relies on targetSdk 28; Xposed support removed and must not return; Telegram integration removed; no public release yet; Google Play policy fit undecided; tests optional; one physical test phone (Realme RMX3363, Android 13).
- Existing roadmap "Future ideas" (`.maister/docs/project/roadmap.md`): app import/export and backup, per-app spoofing profiles in the UI, onboarding for required permissions (All Files Access, VPN), tablet/landscape layouts.

## Scope
- Included: user-facing features of the app (UI and thin engine wiring), quality-of-life, privacy/security controls, backup/restore, per-space settings, notifications, widgets/shortcuts, accessibility, tablet layouts, localisation, diagnostics.
- Excluded: Xposed/LSPosed modules, Telegram, monetisation/ads, cloud accounts/sync services, rewriting the engine for new Android versions (covered separately as the targetSdk spike).
- Constraints: each idea must state effort (S/M/L), value, risk (engine/policy), and whether it needs Bcore changes.

## Success criteria
- Ranked list (top ~10) with rationale and evidence from codebase, docs and comparable apps.
- Each idea mapped to existing code hooks (files/APIs already present) where possible.
- Clear recommendation of a first batch that is UI-only and low-risk.
