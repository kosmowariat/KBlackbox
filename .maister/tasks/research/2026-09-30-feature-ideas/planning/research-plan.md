# Research Plan: Feature ideas for APKEnclave

## TL;DR
Mixed research (technical + market/platform scan). Method: map what the `app` UI and `Bcore` engine already expose (cheap wins = engine capabilities with no UI), cross-check against roadmap/docs, then benchmark against comparable sandbox/cloner apps and Android platform features. Six parallel gatherers feed a synthesis that ranks ~10 ideas by effort (S/M/L), value, risk (engine/policy) and Bcore-change need, ending with a UI-only first batch.

## Key Decisions
- Six gatherers, split by source type (two codebase, one docs, three external) so each has a distinct focus and no overlap.
- Engine-vs-UI gap analysis is the core technique: Bcore `B*Manager` APIs and `BlackBoxCore` public methods not called from `app/` are candidate cheap features.
- Policy research is deliberately small: it only tags Play-risk on candidates, it does not produce a compliance study.
- All ideas are evaluated under fixed constraints: solo dev, targetSdk 28, no Xposed/Telegram/monetisation/cloud sync, one test phone (Realme RMX3363, Android 13).

## Open Questions / Risks
- Play policy stance is undecided; risk tags may steer toward a split "Play" vs "sideload" build.
- Some engine APIs may exist but be broken on Android 13+; the single test phone limits verification (gatherers report "exists" not "works").
- Competitor "user requests" come from reviews/forums/Reddit; quality is uneven, so weight recurring themes over single mentions.
- Platform features (widgets, tiles, per-app language) may behave differently for virtualised guest apps vs the host; flag where host-only.

---

## 1. Research Overview
- **Question**: What features could we still add to APKEnclave (Android virtual-app sandbox, solo fork of NewBlackbox, Material 3 UI, not yet public)?
- **Type**: Mixed (technical codebase analysis + requirements/roadmap review + literature/competitor scan).
- **Included**: user-facing features, QoL, privacy/security controls, backup/restore, per-space settings, notifications, widgets/shortcuts, diagnostics, tablet layouts, localisation, accessibility.
- **Excluded**: Xposed/LSPosed, Telegram, monetisation, cloud sync/accounts, engine rewrite for new Android versions (separate targetSdk spike).
- **Sub-questions**
  1. What does the app UI currently do, and what is half-finished (TODOs, unused strings/resources, dead settings)?
  2. Which Bcore capabilities exist with no UI (per `B*Manager`, `BlackBoxCore`, `Docs.md`)?
  3. What does the roadmap/vision already list or rule out?
  4. What do comparable apps offer, and what do their users request or complain about?
  5. Which Android platform features enable candidate features, and what do they need under targetSdk 28?
  6. Which candidates carry Google Play policy risk?

## 2. Methodology
- **Primary**: codebase capability inventory (Glob/Grep/Read) + gap analysis (engine API vs UI usage).
- **Secondary**: documentation review; competitor feature matrix; platform capability/constraint review.
- **Fallback**: if web search is thin for a competitor, use its Play/F-Droid/GitHub listing and issue tracker; if an engine API is unclear, read the matching AIDL + `core/system/*` service.
- **Analysis framework** (per candidate idea): description; evidence (file:line / URL); existing hooks; Bcore change needed (Y/N); effort S/M/L; value (user + product); risk (engine stability, Android-version, Play policy); dependencies.

## 3. Data Sources
See `planning/sources.md` for the full manifest. Summary:
- Codebase: `app/` (view/apps, base, fake, gms, list, main, setting, users; data/; util/; res incl. values-night, values-w600dp, values-zh-*), `Bcore/` (fake/frameworks `B*Manager`, core/system/*, fake/service/*Proxy, cpp).
- Docs: `.maister/docs/**`, `README.md`, `Docs.md`, `RELEASE_NOTES.md`.
- External: competitor apps/repos, Android developer docs, Play policy pages.

## 4. Research Phases
1. **Broad discovery**: list screens, repositories, settings keys, menus, strings; list `B*Manager` public methods and `BlackBoxCore` public API; scan docs for "future/planned/TODO".
2. **Targeted reading**: for each engine capability, find UI call sites (or confirm none); read roadmap "Future ideas", release notes Known Issues; read competitor feature pages.
3. **Deep dive**: for promising gaps (backup/export, per-space settings, notification controls, JobManager/alarms, storage, shortcuts/widgets, location profiles) trace engine path and platform constraints under targetSdk 28.
4. **Verification**: cross-check claims in README/Docs.md against source (known stale); confirm each "cheap" idea has a real call path; tag unverifiable ones.

## 5. Gathering Strategy

### Instances: 6

| # | Category ID | Focus Area | Tools | Output Prefix |
|---|------------|------------|-------|---------------|
| 1 | codebase-app | `app/` UI: screens, repositories, settings, menus, TODOs, unused strings/resources, dead/half-finished features, tablet/landscape/i18n state | Glob, Grep, Read | codebase-app |
| 2 | codebase-engine | `Bcore` capabilities: `B*Manager` APIs, `BlackBoxCore` public API, core/system services, hooks; cross-referenced against `app/` usage to find unexposed capabilities | Glob, Grep, Read | codebase-engine |
| 3 | documentation | `.maister/docs/**`, README, Docs.md, RELEASE_NOTES (roadmap future ideas, known issues, upstream feature claims, stale claims) | Read, Grep | docs |
| 4 | external-competitors | Parallel Space, Dual Space, Island, Shelter, Insular, VirtualXposed, Multi Accounts, App Cloner, Samsung Secure Folder, Work Profile / Private Space: feature matrix and recurring user requests | WebSearch, WebFetch | external-competitors |
| 5 | external-platform | Android enablers and constraints: app widgets, App Shortcuts, Quick Settings tiles, per-app language, notification channels, biometric lock, Private Space, share sheet/intents, backup, SAF export; targetSdk 28 implications | WebSearch, WebFetch | external-platform |
| 6 | external-policy | Short: Google Play policy items relevant to candidate features (device/network abuse, dynamic code, QUERY_ALL_PACKAGES, VPN, accessibility, all-files access, spoofing) to tag risk | WebSearch, WebFetch | external-policy |

### Rationale
Two codebase gatherers separate UI from engine so the gap analysis is explicit (gatherer 2 must list, per engine API, whether `app/` calls it). Documentation is cheap and isolated. Three external gatherers have distinct questions (what others ship, what the platform allows, what the store forbids). Configuration is folded into gatherers 1, 2 and 5 (manifest, gradle, prefs) rather than a separate instance. Each writes to `analysis/findings/<prefix>-*.md` with file:line or URL citations.

## 6. Success Criteria
- Top ~10 ranked ideas, each with effort, value, risk, Bcore-change flag, and evidence.
- Each idea mapped to existing code hooks (files/APIs) where possible.
- Engine-vs-UI gap table produced (engine capability, UI usage, status).
- Competitor matrix covers at least 6 comparable apps with recurring user requests identified.
- Platform constraints under targetSdk 28 stated for every platform-dependent idea.
- Play policy risk tag on every idea (low/med/high/unknown).
- Clear first batch: UI-only, low-risk, verifiable on one phone.

## 7. Expected Outputs
- `analysis/findings/*` from six gatherers.
- Synthesis report: ranked feature list, gap table, competitor matrix, risk tags, recommended first batch.
- Optional follow-ups: roadmap update suggestions (not an implementation spec).
