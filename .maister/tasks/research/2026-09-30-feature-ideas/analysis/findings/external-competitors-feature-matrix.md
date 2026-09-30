# External competitors: feature matrix and user pain points

## TL;DR
- Virtual-engine clones (Parallel Space, Dual Space, Multiple Accounts, App Cloner) compete on privacy UX: lock (PIN/pattern/fingerprint), incognito/hidden install, icon disguise, themes. APKEnclave has none of the lock/hide/disguise features yet.
- Work-profile tools (Island, Shelter, Insular) compete on freeze/auto-freeze, per-side VPN, shortcuts to unfreeze, file transfer between sides. Freeze is the most copied idea and maps onto the existing "stop app" action.
- Samsung Secure Folder and Android 15 Private Space set user expectations: auto-lock timer, hidden entry point, notifications suppressed when locked, custom name/icon.
- Top recurring complaints: unreliable/delayed notifications in clones, battery drain, ads/paywalls, crashes after Android updates. Being ad-free and reliable is itself a differentiator.
- Best-supported candidate batch (UI-first): space lock (biometric), auto-stop/freeze, per-space notification toggle, icon/label disguise, clone badge, backup/restore, running-apps manager/storage usage.

## Key Decisions
- Treat "space lock + auto-lock timer" as the top externally validated feature (appears in 4 of 7 families).
- Treat freeze/auto-stop as UI-only first (APKEnclave already has stop/clear).
- Do not chase per-app permission stripping / proxy (App Cloner premium) until engine cost is known.

## Open Questions / Risks
- Play listing and appcloner.app could not be fetched fully; Parallel Space/App Cloner/Insular claims rely on search-result summaries (Medium/Low confidence).
- No first-hand Reddit/XDA review threads were retrieved; complaint themes come from review-aggregator articles (Medium).
- Competitor presence of a feature is not proof of demand; Play policy risk for disguise/hide is not assessed here (see external-policy).

## Feature matrix (Y = documented, ? = not verified)

| Feature | Parallel Space | Dual Space / Multiple Accounts | App Cloner | Island/Insular | Shelter | Secure Folder | Private Space |
|---|---|---|---|---|---|---|---|
| Space lock PIN/pattern/fingerprint | Y | Y (VIP) | ? | OS profile | OS profile | Y | Y |
| Auto-lock timer | ? | ? | ? | n/a | n/a | Y | Y (lock on demand) |
| Hide app / incognito install | Y | Y | ? | Y (hide) | ? | Y (hide folder icon) | Y (hide existence) |
| Custom name/icon disguise | ? | Y (icon changing) | Y (rename, recolor, badge) | ? | ? | Y | ? |
| Freeze apps | ? | ? | ? | Y | Y | ? | Y (apps stopped when locked) |
| Notification control | ? | manual enable needed | block background autostart | n/a | n/a | Y | Y (none when locked) |
| Per-app/side VPN | ? | ? | SOCKS proxy, Wi-Fi only | Y | ? | ? | ? |
| Permission stripping | ? | ? | Y (premium) | ? | ? | ? | ? |
| File transfer between sides | ? | ? | ? | ? | Y (File Shuttle) | Y | ? |
| Backup/restore | ? | ? | ? | ? | ? | Restore only (Samsung Cloud) | ? |
| Allow screenshots toggle | ? | ? | Y | ? | ? | ? | ? |
| Themes | Y | ? | force dark mode | ? | ? | ? | ? |

## Findings

### F1. Parallel Space: lock, incognito install, 64-bit plugin, themes
PIN/pattern/fingerprint lock on the space; incognito installation hides apps from the normal launcher; 64-bit support via separate plugin; themes; one-tap account switching.
Source: https://play.google.com/store/apps/details?id=com.lbe.parallel.intl&hl=en&gl=US (via search summary; page not fetchable). Confidence: Medium.

### F2. Dual Space / Multiple Accounts: hide apps, icon change, secret zone, paid gating
Hiding apps, icon changing, secure lock, separate data locked by password or fingerprint. Unlimited accounts, Secret Zone and Security Lock are VIP (subscription) features. Notification permission for cloned apps must be enabled manually.
Sources: https://appeight.com/review/multiple-accounts-dual-space ; https://play.google.com/store/apps/details?id=com.ludashi.dualspaceprox&hl=en-US . Confidence: Medium.

### F3. App Cloner: per-clone customisation
Rename, recolor or badge icon, custom icon, force dark mode, orientation lock, immersive mode, allow screenshots/recording; strip permissions, block auto-start and wake locks, disable trackers, SOCKS proxy, Wi-Fi-only data, isolated storage, clipboard block; 100+ options, many premium.
Sources: https://www.apkmirror.com/apk/applisto/app-cloner/ ; https://appcloner.app/ (home page fetch returned no content). Confidence: Medium (search snippet).
Relevance: badge/rename/icon per clone is UI-side (ShortcutUtil exists); permission/proxy need engine.

### F4. Island (oasisfeng): clone, freeze, hide, VPN, shortcuts
Clone then run in parallel; freeze on demand (icon vanishes, background blocked); launch shortcuts that unfreeze; hide apps; Greenify auto-freeze integration; VPN on one side or different VPN per side; SMS and location are bound to the device (not isolated).
Source: https://island.oasisfeng.com/ (fetched). Confidence: High.

### F5. Insular (Island fork)
FOSS fork: freeze/archive, unfreeze via home-screen shortcuts, re-freeze marked apps with one tap, VPN management per group, USB access prohibition.
Sources: https://secure-system.gitlab.io/Insular/faq.html ; https://github.com/proletarius101/Insular . Confidence: Medium (search snippet).

### F6. Shelter (PeterCxy)
Work-profile isolation, freeze, two copies of an app. README says project is in effective maintenance mode. File Shuttle moves files between profiles (changelog mentions media-scan and file-suffix fixes).
Sources: https://github.com/PeterCxy/Shelter/blob/master/README.md (fetched) ; https://github.com/PeterCxy/Shelter/blob/master/CHANGELOG.md (snippet). Confidence: High for README, Medium for File Shuttle.

### F7. Samsung Secure Folder
Auto-lock on screen off / immediately / on restart / at a set time; hide icon via Quick Settings; custom name and icon; notifications configurable (lockscreen, status bar, allowed while locked); add or move installed apps; backup no longer supported, restore still works.
Sources: https://www.samsung.com/us/support/answer/ANS10001401/ ; https://www.androidpolice.com/samsung-secure-folder-tips-tricks/ . Confidence: Medium-High.

### F8. Android 15 Private Space
Separate profile (usertype PRIVATE) with its own lock; hidden from recents, notifications and settings when locked; apps are stopped when locked (no notifications); optional hiding of the space's existence; lock/unlock via requestQuietModeEnabled.
Sources: https://source.android.com/docs/security/features/private-space ; https://www.androidauthority.com/android-15-private-space-settings-3431105/ . Confidence: High.
Implication: Android 15+ users get this natively, so APKEnclave's lock must add per-space granularity and work on older versions.

### F9. NewBlackbox / VirtualXposed baselines
NewBlackbox README: clone, sandbox, device spoofing, fake location, multi-arch; 23 open issues; crashes from UID/permission mismatch. VirtualXposed issue #942 requests a manager to create many instances and edit parameters per instance.
Sources: https://github.com/ALEX5402/NewBlackbox ; https://github.com/android-hacker/VirtualXposed/issues/942 . Confidence: Medium-High. Supports a per-space settings profile idea.

### F10. Recurring user complaints
- Clone notifications unreliable unless the host app is in the foreground; some false/engagement notifications.
- Battery drain even with screen off.
- Intrusive ads; lifetime-Pro buyers still see ads; support unanswered.
- Crash on launch after Android updates.
Sources: https://www.smileblogs.com/article/2114 ; https://www.mouthshut.com/review/parallel-space-review-npttqspsson ; https://xdaforums.com/t/app-4-0-3-2016-02-26-1-0-2036-parallel-space-multiple-accounts.3308715/page-13 (snippets). Confidence: Medium (secondary, not primary review scraping).

## Candidate features inferred (for synthesis)
1. Space lock with BiometricPrompt and auto-lock timer (F1, F2, F7, F8): UI-first; a hard launch gate may need an engine hook.
2. Freeze/auto-stop idle spaces or apps plus unfreeze shortcut (F4, F5, F6): reuses stop action and ShortcutUtil.
3. Per-space notification toggle (F7, F8, F10): likely engine (BNotificationManager).
4. Disguise launcher icon/label and clone badge (F2, F3, F7): largely UI (launcher icon variants exist).
5. Backup/restore of a space (F7, roadmap): DataDirCopier exists.
6. Running apps manager and storage per space (F8, F10 battery): M.
7. Per-space proxy/VPN (F3, F4): engine, L.
8. File shuttle between host and space (F6): SAF-based, M.
9. Per-app allow-screenshot (F3): app already has a global FLAG_SECURE disable.
10. Notification reliability/battery diagnostics (F10): quality differentiator.
