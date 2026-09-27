## CI & Distribution

### Pipeline
`.github/workflows/build_and_telegram.yml` runs on push to `main` and on `workflow_dispatch`: Temurin JDK 21 with Gradle cache, Android SDK, builds all APKs/AARs/JARs, and posts them to Telegram with caption prefixes (`[DEBUG]`, `[RELEASE]`, `[Bcore DEBUG AAR]`, ...) plus the last commit subject, pinning the latest.
No tests or lint run in CI today.

### Secrets
Credentials (`TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID`) come only from GitHub repository secrets — never commit them. In this fork the secrets must be configured in the fork's own repository settings.

Source: config, docs.
