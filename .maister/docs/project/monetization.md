# Monetization & Distribution Plan

Status: planned, nothing implemented yet (decisions of 2026-10-01). The app is still in heavy development, so this is a backlog to pick up later.

## Decisions

- One app: free tier plus a paid **Pro** subscription. The list of Pro features is still to be decided.
- The `KBlackbox` repo stays private. The sales site lives in a separate private repo (e.g. `kblackbox-site`).
- Distribution is self-hosted (APK from our own site). Google Play is not an option because `targetSdk` stays at 28.
- No own user accounts at first: a license key identifies a Pro user; the payment provider's customer portal handles billing.
- No automatic telemetry or log upload (deliberately removed from the engine, see `standards/engine/runtime.md`).
- Dependency licenses (Bcore: Dobby, xdl, FreeReflection, ...), privacy policy and terms are handled last, before the first sale.

## Hosting (Cloudflare)

| Part | Service | Notes |
|---|---|---|
| Site | Pages (from the private repo) | 25 MiB per-file limit, so no APKs here |
| APK and `latest.json` | R2, public bucket on a subdomain (e.g. `dl.<domain>`) | no egress fees |
| License backend | Workers + D1 | webhook from the provider, `/verify`, device activations |

- Secrets live in Worker secrets, never in a repo. The app keystore stays outside Cloudflare, with an offline copy.
- Check current limits and prices on the Cloudflare pricing pages before starting.

## Payments: Polar (recommended)

- Poland is a supported seller country (payouts via Stripe Connect Express, bank account in PLN).
- Merchant of Record: handles EU VAT through OSS and B2B reverse charge. Income tax stays our responsibility.
- License keys revoked automatically when the subscription is cancelled, activation limits, public activate/validate endpoints, deactivation by the customer in the portal.
- Fees: Starter 5% + 50c, +1.5% for non-US cards; lower tiers on paid plans.
- Lemon Squeezy was rejected for now: it is migrating to Stripe Managed Payments and it is unclear whether its API and license keys survive; its license API is limited to 60 requests/min.
- Before opening the account: read Polar's account review requirements, prepare a product description (sandbox for running your own apps), terms, privacy policy and business details. Verify the accepted form of business and VAT invoices for companies.

## App side

- The app verifies the key through our own Worker, not directly against the provider API, with caching and an offline grace period (7-14 days).
- When Pro expires the app returns to the free tier; user data (virtual apps) is kept.
- Device limit per key (e.g. 2-3), with deactivation in the customer portal.

## Update channel

- `latest.json` on R2: `versionCode`, `versionName`, APK url, `sha256`, changelog, `minSupportedVersionCode`.
- The app checks on start or daily via WorkManager, downloads with `DownloadManager`, verifies `sha256`, installs with `PackageInstaller` (`REQUEST_INSTALL_PACKAGES`).
- Hard requirement: every release is signed with the same key. The release keystore exists (outside the repository, with `APKENCLAVE_*` settings, see README) and both the everyday app and the `.debug` app are signed with it; keep the offline backup of the keystore. The `.debug` app must never self-update.
- Before installing, check that the downloaded APK is signed with our certificate (`PackageManager.getPackageArchiveInfo` with signing certificates), and that `versionCode` is higher than the installed one. Android refuses an update signed with another key anyway, but the explicit check gives a clear error and blocks downgrades.
- Cleartext traffic must stay allowed in `network_security_config.xml`: sandboxed apps run under the host package, so the host's network policy applies to them. Enforce HTTPS in the update code instead (reject any `latest.json` or APK address that is not `https://`).

## Order of work

1. Update mechanism in the app plus `latest.json` (independent of payments).
2. Site with downloads on Cloudflare Pages.
3. License backend (Worker + D1) and the key screen in the app, once the Pro features are decided.
4. Dependency licenses, privacy policy, terms.

## Review notes (2026-10-01)

Things to settle before any code is written, most important first.

1. **Payment provider go/no-go.** The whole plan depends on Polar accepting the product. Read its acceptable-use policy and review requirements first and ask support in writing whether "an app that runs other apps in a sandbox" is acceptable. If not, the fallback is another Merchant of Record or Stripe with own VAT handling. Do this before the site, the Worker or the update channel. Also check with an accountant which business form is needed in Poland to sell a subscription.
2. **Signed license tokens.** A plain `/verify` answer can be faked with a hosts-file entry or a proxy. Let the Worker return a signed token (for example Ed25519: license id, installation id, expiry) and verify it in the app with an embedded public key; cache the token for the offline grace period. Identify an installation with a random id created at first start, not with hardware identifiers, which also keeps the privacy policy short. Accept that a determined user can still patch the APK; do not spend effort on obfuscation.
3. **Free versus Pro.** Keep everything that affects safety or data free (backup, restore, stop, health screen). Candidates that fit a paid tier: more than 2-3 spaces, the PC web panel, scheduled backups, favourite places in fake location. Decide this before the key screen, because it decides where the app needs a `isPro` check.
4. **Licenses (checked on GitHub on 2026-10-01).** This is the biggest legal risk, read it before the first sale.
   - **VirtualApp lineage.** The README of asLody/VirtualApp states that commercial use or publishing in app stores needs a paid license and threatens reports for copyright infringement. The repository has no license file. The engine architecture (`Bcore`) follows VirtualApp through BlackBox and NewBlackbox (Apache-2.0 on its own). Whether any Bcore code is copied from VirtualApp has not been checked. Get legal advice and a code provenance review before selling.
   - **Components without a published license:** CatLoadingView (Rogero0o) and FloatingView / EnFloatingView (leotyndale; `com.imuxuan.floatingview`). Without a license nobody may redistribute them. CatLoadingView has been replaced by an own dialog. FloatingView is still used by the fake-location joystick (`RockerManager`, `EnFloatView`) and has to be replaced by an own draggable overlay, tested on a phone with a guest app.
   - **SandHook-derived native code:** `Bcore/src/main/cpp/Utils/elf_util.*` uses the `SandHook` namespace and its license file (LICENSE.txt in ganyao114/SandHook) was not GitHub-detectable. Read it and the copied source, or rewrite the helper.
   - **Verified permissive licenses:** Dobby (Apache-2.0, prebuilt), xDL (MIT), FreeReflection (MIT), toml4j (MIT), StateView (MIT), osmdroid and AndroidX/Material (Apache-2.0), NewBlackbox (Apache-2.0). Their notices are shown in the app under Settings → Open source licenses (`assets/licenses/NOTICES.txt`); update that file whenever a dependency changes.
5. **Name and domain.** "APKEnclave" was only checked as a package id. Check trademark and domain availability before building a site on it.
6. **`targetSdk` 28 and self-distribution.** Android 14 already refuses to install apps with `targetSdk` below 23. The plan relies on 28 staying installable, so watch the minimum target level of new Android versions and keep this risk in the roadmap.
7. **Cloudflare details.** Check current limits (Pages file size, R2 and Workers free tiers, D1 write limits) at the start; the numbers here are from memory.
