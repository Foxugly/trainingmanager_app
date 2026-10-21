# CLAUDE.md

Guidance for Claude Code in this repo. Fleet / ops context lives in the user-level
`~/.claude/CLAUDE.md` and in `OPERATIONS.md` (repo `Foxugly/foxugly-ops`).

## What this is

TrainingManager mobile app — Kotlin Multiplatform (Android + iOS), Compose Multiplatform UI.
It ships no server: it consumes the DRF API of `trainingmanager_server` (`https://tm-api.foxugly.com/api/v1/`,
hard-coded in `MainActivity.kt`; debug builds also talk to prod, they only add HTTP logging).

- `composeApp/` — shared code. `commonMain` (UI, view models, `data/api` Ktor client, `AuthRepository`,
  Koin graph in `di/AppModule.kt`), `androidMain` / `iosMain` (`expect`/`actual`: token storage, FCM, logger…).
- `androidApp/` — the Android application (`MainActivity`, `TMFirebaseMessagingService`, Sentry in
  `TrainingManagerApplication`, manifest, R8 rules).
- `iosApp/` — iOS entry point. **Not buildable yet** (no Xcode project / entitlements / FCM, audit A-B3).
- `openapi/Training_Manager_API.yaml` — vendored copy of the backend schema. API models are **generated at
  build time** into `composeApp/build/generated/openapi` (package `…api.generated`); nothing generated is committed.

## Commands (Windows, from the repo root)

```powershell
.\gradlew :composeApp:testAndroidHostTest --no-configuration-cache   # unit tests (commonTest, JVM host)
.\gradlew :androidApp:assembleDebug --no-configuration-cache
.\gradlew :androidApp:assembleRelease --no-configuration-cache       # R8; unsigned without keystore.properties
.\gradlew :androidApp:lintDebug --no-configuration-cache             # Android lint (errors fail CI)
```

The PC is low on memory: one Gradle build at a time, `--no-daemon -Dorg.gradle.jvmargs=-Xmx2g`,
`.\gradlew --stop` afterwards, no emulator.

Git-ignored files a build needs: `local.properties` (SDK path) and `androidApp/google-services.json`
(CI uses `.github/ci/google-services.placeholder.json` when the secret is absent).

## CI / release

- `ci.yml`: host tests → debug APK + lint + unsigned release APK (R8). `api-drift.yml`: fails when the vendored
  schema lags behind the backend's. `release.yml`: signed AAB on a `v*` tag or manual run (secrets listed in the file).
- `main` is protected (required checks); non-major Dependabot PRs auto-merge once green.

## Conventions

- Every authenticated call goes through `apiCall` in `TrainingManagerApi` (401 → refresh under the
  `AuthInterceptor` mutex → replay). The startup refresh uses the same mutex (`refreshSession`).
- Only a 401/403 from `auth/token/refresh/` ends the session; offline / 5xx keep the tokens.
- Never put a response body, token or e-mail in an exception message or a log line: messages reach logcat and
  Sentry. `ApiException.responseBody` is for matching backend error codes only.
- Release builds strip `Log.v/d/i` (R8 `-assumenosideeffects`); iOS logs only in debug binaries.
- Commits and PRs in French.
