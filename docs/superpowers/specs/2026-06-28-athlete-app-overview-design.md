# TrainingManager Athlete App — Overview & Decomposition

**Date:** 2026-06-28
**Status:** Draft for review (brainstorming output — not yet approved for implementation)
**Author:** Claude (brainstorming session, owner offline — defaults taken where noted)

---

## 1. Goal

Build a native **Android + iOS** mobile app for **TrainingManager athletes** (the
*sportifs*). It mirrors the athlete-facing functionality of the existing Angular web
app (`trainingmanager_frontend`) against the existing `trainingmanager_server` DRF API,
and reuses the proven Kotlin Multiplatform architecture of `PushIT_app`.

**Target user = the athlete (team member).** They *consult and respond*; they do **not**
create or manage teams/events, and have **no admin/superuser** access. All manager-only
and superuser-only surfaces of the web app are explicitly out of scope.

## 2. Confirmed scope decisions (from brainstorming)

| Decision | Choice |
|---|---|
| Target user | Athletes (team members) only |
| Feature breadth | **Full athlete app**: events/sessions, teams, team discussions, notifications, profile |
| Onboarding/auth | **Full**: login + in-app register + email confirm + password reset + magic-link + invitation deep-link |
| Session | **Persistent** ("stay logged in" like WhatsApp/Facebook) |
| Push (FCM) | **Yes, in v1** — requires backend work first (see S0) |
| Languages | **5** (fr default, nl/en/it/es) — parity with web |
| Architecture | Mirror `PushIT_app` (KMP + Compose Multiplatform) |

## 3. Isolation principle (watertight vs PushIT) — **hard rule**

This project is **fully isolated** from PushIT. PushIT is used as a **read-only
reference** for patterns only; **no PushIT code or repo is modified**, and there is **no
build- or runtime-time dependency** on PushIT.

| Axis | PushIT | TrainingManager |
|---|---|---|
| App repo | `Foxugly/PushIT_app` | `Foxugly/trainingmanager_app` |
| Backend repo | `PushIT_server` | `trainingmanager_server` |
| App package | `com.foxugly.pushit_app` | `com.foxugly.trainingmanager_app` |
| API base | `pushit-api.foxugly.com` | `tm-api.foxugly.com` (confirm exact host) |
| Firebase project | PushIT's own | **separate, dedicated TM project** |

⚠️ **Firebase must be a separate project** for TrainingManager: its own
`google-services.json` (Android), `GoogleService-Info.plist` (iOS), and a **dedicated
service-account JSON** on the backend. Never reuse PushIT's FCM project/sender.

## 4. Cross-cutting architecture decisions (defaults — confirm at S1)

These apply across all mobile sub-projects. Defaults chosen because the app is much
larger than PushIT (PushIT had ~3 models and 5 screens; this app has many).

1. **API client = generated from OpenAPI.** Generate a Kotlin client from
   `trainingmanager_server/openapi-schema.yaml` (openapi-generator, `kotlin` +
   Ktor/kotlinx.serialization) rather than hand-writing models like PushIT. Rationale:
   large API surface (events, rounds, exercises, teams, members, topics, rsvp, roti,
   notifications, …) — hand-writing is error-prone and the schema is the contract.
   *Trade-off:* codegen tuning effort up front; mitigated by a `make api-gen` task
   mirroring the frontend's `npm run api:gen`.
2. **Navigation = Navigation Compose Multiplatform** (Jetpack Navigation for KMP), not
   PushIT's manual `sealed class Screen` + `when`. Rationale: deep nav (lists → detail →
   tabs → sub-views) + deep links (magic-link, invitation, push taps) need a real router.
3. **DI = Koin** (lightweight), not PushIT's hand-wired `App.kt` graph. Rationale: the
   dependency graph (many repositories/services) outgrows manual wiring.
4. **Reuse verbatim from PushIT** (pattern-copied, re-namespaced): Ktor client +
   `AuthInterceptor` (bearer + single-flight 401 refresh + request replay),
   `TokenStorage` expect/actual (Android `EncryptedSharedPreferences` / iOS Keychain via
   multiplatform-settings), `FcmTokenProvider` expect/actual, the `apiCall`/`Result<T>`
   helper, `ApiException` vs `NetworkException`, the two-module split (`:composeApp`
   library + `:androidApp` + `iosApp/`), version-catalog + config-cache Gradle setup.
5. **Prod-only backend** like PushIT (fixed prod base URL; `enableHttpLogging =
   BuildConfig.DEBUG`). No runtime backend switch.
6. **i18n** = Compose Multiplatform resources (`stringResource`) for UI strings in 5
   languages + an `Accept-Language` Ktor header (mirrors the web `languageInterceptor`)
   so the backend localizes translatable model fields; language persisted via
   `PATCH /me/`.
7. **Theme** = Material 3 Compose theme reproducing the web's emerald accent + dark mode.

## 5. Decomposition into sequenced sub-projects

The whole is too large for one spec/plan. Each sub-project gets its own
**spec → plan → implementation** cycle. Detailed specs accompany this overview for
**S0** and **S1**; **S2–S4** have outline specs to be deepened when reached.

| # | Sub-project | Repo | Depends on | Spec |
|---|---|---|---|---|
| **S0** | Backend FCM push infrastructure | `trainingmanager_server` | — | `…-S0-backend-fcm-push-design.md` (detailed) |
| **S1** | Mobile foundation + auth + profile + i18n | `trainingmanager_app` | — | `…-S1-mobile-foundation-auth-design.md` (detailed) |
| **S2** | Events & training (dashboard, sessions, RSVP, ROTI, rounds/exercises, attachments) | `trainingmanager_app` | S1 | `…-S2-events-training-design.md` (outline) |
| **S3** | Teams + discussions + messages hub | `trainingmanager_app` | S1 | `…-S3-teams-discussions-design.md` (outline) |
| **S4** | Push client + notifications inbox | `trainingmanager_app` | S0 + S1 | `…-S4-push-client-notifications-design.md` (outline) |

**Note on "messaging":** the backend has **no private DMs**. What the web calls
*Messages* is a cross-team aggregation of **team discussion topics**. Messaging is
therefore folded into **S3** (not a separate sub-project).

**Recommended build order:** S0 (small, self-contained, "phase 1", unblocks push) →
S1 (the app backbone) → S2 → S3 → S4. S0 and S1 are independent and may proceed in
parallel; S4 needs both S0 and S1.

## 6. Athlete scope — in vs out (from frontend analysis)

**In (athlete-visible):** login/register/reset/magic-link/invitation; dashboard
(upcoming sessions, attendance history, my teams); events list/calendar + event detail
(RSVP going/maybe/not_going, ROTI 1–5, training rounds/exercises **read-only**,
attachments **download**, respecting `vis_distance`/`vis_goal`/`vis_rounds` =
always/after/never); teams list + detail (members read-only, stats, discussions);
discussions (browse topics with `audience=team`, post/edit/delete own messages when the
topic allows athlete replies, emoji); profile (name, language, change password);
notifications inbox + preferences; push receipt + deep-link.

**Out (manager/superuser-only — exclude):** create/edit/delete teams, events, programs;
attendance marking; debrief authoring; slots editor; managers; place pool; join-request
approvals; audit log; templates; AI generation; all taxonomy CRUD (sports, energy
systems/segments, modalities); `coaches`-audience topics.

## 7. Open questions for the owner (to resolve before/at S1 & S0)

1. **API host** for the mobile app — is it `https://tm-api.foxugly.com/api/v1/`?
   (frontend CSP references `tm-api.foxugly.com`; confirm the exact prod base URL.)
2. **Firebase project** — create a new dedicated TM Firebase project (Android + iOS apps
   registered). Who owns the console / service-account provisioning? (SSM secret name,
   e.g. `/trainingmanager/prod/FCM_SERVICE_ACCOUNT`.)
3. **Push send sync vs async** — `trainingmanager_server` has **no Celery** (unlike
   PushIT). v1 default: send push **synchronously** inside `notify()` with a short
   timeout + token-invalidation on failure; revisit async later. OK?
4. **iOS push** — APNs key + Apple Developer setup needed for iOS push; confirm Apple
   Developer account access. Android-first is acceptable if iOS push lags.
5. **Confirm the cross-cutting defaults** in §4 (codegen, Navigation Compose, Koin).

## 8. Self-review notes

- No placeholders/TBDs left except the explicitly-flagged open questions in §7 (owner
  decisions, not implementation gaps).
- Endpoint exact payloads (RSVP `POST` vs `PUT`, response shapes) differ slightly between
  the frontend reading and the backend reading; the **generated client from
  `openapi-schema.yaml` is the authoritative contract** and supersedes prose here.
- Scope is decomposed into independently-plannable units (S0–S4); each is small enough
  for a single implementation plan.
