## CI & Distribution

### Pipeline
`.github/workflows/build.yml` runs on push to `main` and on `workflow_dispatch`: Temurin JDK 21 with Gradle cache (the runner ships the Android SDK), runs `:app:lintDebug :app:testDebugUnitTest`, then builds all APKs/AARs/JARs, and uploads them as 4 GitHub Actions artifacts (`app-debug-apks`, `app-release-apks`, `bcore-aars`, `libs`) with 30-day retention, downloadable from the workflow run page.
Lint and unit tests of the `app` module run before the build and fail the workflow; on failure their reports are uploaded as the `check-reports` artifact (7 days). Lint for `Bcore` is still relaxed and no instrumented tests run.

### Secrets
The workflow needs no secrets. If a future step needs credentials, they come only from GitHub repository secrets — never commit them.

Source: config, docs.
