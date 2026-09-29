## CI & Distribution

### Pipeline
`.github/workflows/build.yml` runs on push to `main` and on `workflow_dispatch`: Temurin JDK 21 with Gradle cache, Android SDK, builds all APKs/AARs/JARs, and uploads them as 4 GitHub Actions artifacts (`app-debug-apks`, `app-release-apks`, `bcore-aars`, `libs`) with 30-day retention, downloadable from the workflow run page.
No tests or lint run in CI today.

### Secrets
The workflow needs no secrets. If a future step needs credentials, they come only from GitHub repository secrets — never commit them.

Source: config, docs.
