# S1 — Mobile Foundation + Auth + Profile + i18n

**Target repo:** `trainingmanager_app` (Kotlin Multiplatform, Compose Multiplatform)
**Date:** 2026-06-28 · **Status:** Draft for review · **Depends on:** —
**Reference:** `PushIT_app` (architecture mirrored; re-namespaced to `com.foxugly.trainingmanager_app`).

---

## 1. Why

S1 is the backbone every other sub-project builds on: the KMP project scaffold, the
networking + auth stack, persistent session, the generated API client, navigation, theme,
i18n, and the full auth/onboarding + profile screens. After S1 the app **builds, runs on
Android & iOS, logs in/out, persists the session, and shows a (placeholder) home**.

## 2. Project scaffold (mirror PushIT two-module split)

- `:composeApp` — KMP **library** holding all shared code + Compose UI (`commonMain`,
  `androidMain`, `iosMain`, `commonTest`).
- `:androidApp` — runnable Android app (`MainActivity`).
- `iosApp/` — SwiftUI host embedding the Compose view (`UIViewControllerRepresentable`).
- Gradle: version catalog `gradle/libs.versions.toml`, config-cache + build-cache on,
  `kotlin.code.style=official` (copy PushIT's gradle.properties + settings.gradle.kts and
  re-namespace). Package `com.foxugly.trainingmanager_app`.
- **New vs PushIT** libraries added to the catalog: openapi-generated-client deps
  (Ktor client engines + kotlinx-serialization already present), **Navigation Compose
  Multiplatform**, **Koin** (`koin-core`, `koin-compose`). Keep PushIT's versions for
  Kotlin/Compose/AGP/Ktor/multiplatform-settings as the baseline (see overview §4.4).

## 3. Networking + auth stack (copy from PushIT, re-namespaced)

- **Ktor client** with logging gated by `BuildConfig.DEBUG`.
- **`AuthInterceptor`** plugin: attaches `Bearer <access>` on all calls except
  `AUTH_PATHS`; on 401 runs a **single-flight refresh** under a `refreshMutex` (guard that
  skips re-refresh when a concurrent call already rotated the token), then **replays the
  original request lambda** (preserves verb + body) with the fresh token; on refresh
  failure → `onAuthFailure` (navigate to Login).
- **`apiCall` / `Result<T>`** helper; **`ApiException`** (HTTP) vs **`NetworkException`**
  (transport/offline) distinction.
- **`AUTH_PATHS`** (no bearer, never refreshed): `auth/token/`, `auth/token/refresh/`,
  `auth/register/`, `auth/email/confirm/`, `auth/email/resend/`, `auth/password/reset/`,
  `auth/password/reset/confirm/`, `auth/magic-link/request/`, `auth/magic-link/exchange/`,
  invitation lookup.
- **`languageInterceptor`**: attaches `Accept-Language: <activeLang>` on every API call.

## 4. API client = generated from OpenAPI

- A Gradle task `apiGen` runs openapi-generator (`kotlin` generator, Ktor + kotlinx-
  serialization) over `trainingmanager_server/openapi-schema.yaml` → generated services +
  models under `composeApp/src/commonMain/kotlin/.../api/` (git-tracked, **never
  hand-edited**, mirrors the frontend's `src/app/api/` convention).
- Repositories wrap the generated services and return `Result<T>` via `apiCall`.
- **Prod-only base URL** injected at the platform entry point (confirm host —
  overview §7.1).

## 5. Persistent session (`TokenStorage` expect/actual)

- `expect`/`actual` `TokenStorage` over **multiplatform-settings** — Android backed by
  **`EncryptedSharedPreferences`**, iOS by **Keychain** (copy PushIT's impls). Stores
  access + refresh + a `remember` flag.
- **Stay-logged-in**: the refresh token is persisted; on launch the app auto-refreshes
  (see startup flow). `remember` toggles the backend refresh TTL (7d default / 30d) via
  the login `remember` field. Logout clears storage **and** calls
  `POST /devices/unregister/` (S4) + `auth/logout/` (blacklist refresh).
- `TokenStore` interface seam so the data layer is fakeable in tests (PushIT pattern).

## 6. Navigation + startup flow

- **Navigation Compose Multiplatform** with a typed route graph. Top-level destinations:
  `Login`, `Register`, `CheckEmail`, `ForgotPassword`, `ResetPassword`, `MagicLink`,
  `Invitation`, and the authenticated `Home` graph (tabs: Dashboard / Events / Teams /
  Notifications / Profile — Events/Teams/Notifications land in S2–S4).
- **Deep links** (Android `intent-filter` + iOS universal links / custom scheme) for:
  `…/auth/magic-link/{token}`, `…/auth/reset-password/{key}`,
  `…/auth/confirm-email/{key}`, `…/invitation/{token}`, and push taps (S4) → route to the
  right screen. (Confirm the deep-link host/scheme — tie to the prod web domain for
  universal links.)
- **Startup (bootstrap)** before showing the main UI (PushIT pattern, adapted):
  1. Read `TokenStorage`. No refresh token → `Login`.
  2. Refresh token present → `POST auth/token/refresh/`; on success `GET /me/` → `Home`;
     on failure → clear + `Login`.
  3. After reaching `Home`, register the FCM device (S4).
  - Show a splash/loading state while bootstrap runs (`null` route = loading).

## 7. Auth & onboarding screens (full parity)

Each screen calls the generated client; maps DRF errors via an `applyServerError`
equivalent (`FieldErrors = Map<String, List<String>>`) with branches for known top-level
codes; honours rate-limit `Retry-After` with a countdown (port `parseRetryAfterSeconds`).

| Screen | Endpoint(s) | Notes |
|---|---|---|
| **Login** | `POST auth/token/` then `GET /me/` | email + password + **remember** toggle; `email_not_verified` (400) → offer resend; `authentication_failed` (401); 429 countdown; `returnUrl` after deep-link. Toggle to **magic-link** request. |
| **Register** | `POST auth/register/` | first/last name, email, password (≥8), language; **Turnstile** captcha (see §9). Success → `CheckEmail` (email passed in-memory, not via route args). |
| **Email confirm** | `POST auth/email/confirm/ {key}` | from deep-link; returns JWT pair → auto-login → `Home`; 410 expired → resend. |
| **Resend confirm** | `POST auth/email/resend/ {email}` | 30s cooldown; always-200 anti-leak. |
| **Forgot password** | `POST auth/password/reset/ {email, turnstile}` | always-200; → neutral "sent" screen. |
| **Reset password** | `POST auth/password/reset/confirm/ {key, new_password}` | from deep-link; `key` opaque; returns JWT → auto-login. |
| **Magic-link request** | `POST auth/magic-link/request/ {email}` | always-200; neutral "check email". |
| **Magic-link exchange** | `POST auth/magic-link/exchange/ {token}` | from deep-link `…/auth/magic-link/{token}`; 410 expired / 400 invalid; returns JWT → auto-login. |
| **Invitation** | `GET` then `POST invitations/{token}/lookup/ {password}` | from deep-link `…/invitation/{token}`; show team name; only `pending` acceptable; creates account + joins + auto-login. |

> Exact invitation/magic endpoint shapes come from the **generated client**; prose above
> reflects the frontend's usage and may be refined at codegen time.

## 8. Profile + i18n (folded into S1)

- **Profile**: `GET /me/` / `PATCH /me/` — edit first/last name, language, the
  `weekly_recap_opt_in` / `digest_email` flags; email + confirmed status read-only.
- **Change password**: dedicated screen → `POST auth/password/change/`
  `{current_password, new_password}`; **no logout** on success (tokens stay valid).
- **Language**: a `LanguageService` (Koin singleton) is the source of truth for
  `activeLang` (5 langs). `switchLanguage(code)` applies optimistically, persists via
  `PATCH /me/`, rolls back on failure; an init reads `me.language` at bootstrap/login.
  UI strings via Compose Multiplatform string resources, 5 locales; the
  `languageInterceptor` sends `Accept-Language` so backend enum labels localize.
- **Account delete** (`POST auth/account/delete/`) — include if cheap; returns 409
  `owns_teams` (athletes rarely own teams). *Lower priority within S1.*

## 9. Platform specifics

- **Android `MainActivity`**: builds the dependency graph (Koin), injects prod base URL +
  `enableHttpLogging = BuildConfig.DEBUG`, hosts `App()`, handles deep-link intents.
- **iOS `MainViewController`** + SwiftUI host (copy PushIT), deep-link handling.
- **Turnstile**: Cloudflare captcha is web-centric. For mobile, prefer a WebView-hosted
  Turnstile widget for register/forgot-password, **or** confirm the backend accepts a
  mobile path (e.g. captcha exempt for app clients / alternative attestation). *Open
  question — flag for S1 kickoff; do not block login (login has no captcha).*
- **Theme**: Material 3 Compose theme, emerald accent + dark mode parity.

## 10. Testing (commonTest, JVM via `:composeApp:testAndroidHostTest`)

- Fake `TokenStore` + fake API to test: `AuthInterceptor` single-flight refresh + replay;
  bootstrap branches (no token / refresh ok / refresh fail); login error mapping
  (`email_not_verified`, 429 retry-after); language optimistic switch + rollback.
- Reuse PushIT's `testAndroidHostTest` setup (Windows-friendly; never run iOS targets on
  Windows).

## 11. Deliverables checklist

- [ ] KMP scaffold (modules, catalog, gradle, namespace, theme)
- [ ] Ktor + AuthInterceptor + apiCall/Result + exceptions + languageInterceptor
- [ ] OpenAPI codegen task + generated client wired via Koin
- [ ] TokenStorage expect/actual + persistent session + TokenStore seam
- [ ] Navigation graph + deep links + bootstrap/startup
- [ ] Auth screens (login/register/confirm/resend/forgot/reset/magic-link/invitation)
- [ ] Profile + change-password + language switch + 5-locale resources
- [ ] Android `MainActivity` + iOS host + deep-link handling
- [ ] commonTest suite (interceptor, bootstrap, error mapping, language)
