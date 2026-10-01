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
- Hard requirement: every release is signed with the same key. Release builds are currently debug-signed (tracked as tech debt in the roadmap); before the first public release create the real keystore and back it up.

## Order of work

1. Update mechanism in the app plus `latest.json` (independent of payments).
2. Site with downloads on Cloudflare Pages.
3. License backend (Worker + D1) and the key screen in the app, once the Pro features are decided.
4. Dependency licenses, privacy policy, terms.
