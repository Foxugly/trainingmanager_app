# S1c-c — Invitation accept (lookup + complete) Implementation Plan

> Executes inline (autonomous). Reuses the S1c-a/b deep-link foundation + `autoLogin` helper.

**Goal:** Accept a team invitation arriving by deep link `/invitation/{token}`: look it up (GET, shows team name + status), and if `pending`, set a password to create the account + join + auto-login.

**Contracts (schema):** `GET invitations/lookup/{token}/` → `ValidateInvitation {email, team_name, status(pending|completed|expired|cancelled), expires_at}` (400 not-pending, 404 not-found, 410 expired). `POST invitations/lookup/{token}/ {password}` → **201** `{detail,email,access,refresh}` (400 bad state/weak pw, 404, **409 `code=email_taken`**). `AUTH_PATHS` already contains `invitations/`. Token is a **path param**, not a body. Complete response decodes to `TokenPair` (`detail`/`email` ignored). 201 passes `isSuccess()`.

## Global Constraints
Same as S1c-a/b. Verify `:composeApp:testAndroidHostTest`; APK `:androidApp:assembleDebug`. No new iOS code (generic bridge). Web deep-link path is top-level `/invitation/{token}` (NOT under `/auth/`).

---

## Task 1 — DTOs + API (TDD)
Create `data/api/InvitationDtos.kt`:
```kotlin
package com.foxugly.trainingmanager_app.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** GET invitations/lookup/{token}/ */
@Serializable
data class ValidateInvitation(
    val email: String,
    @SerialName("team_name") val teamName: String,
    val status: String,
    @SerialName("expires_at") val expiresAt: String,
)

/** POST invitations/lookup/{token}/ */
@Serializable
data class CompleteInvitationBody(val password: String)
```
`TrainingManagerApi` (after `confirmPasswordReset`):
```kotlin
    suspend fun lookupInvitation(token: String): Result<ValidateInvitation> = apiCall {
        client.get("invitations/lookup/$token/")
    }

    suspend fun completeInvitation(token: String, request: CompleteInvitationBody): Result<TokenPair> = apiCall {
        client.post("invitations/lookup/$token/") { setBody(request) }
    }
```
Test `data/api/InvitationApiTest.kt`: lookup decodes ValidateInvitation (pending); complete decodes TokenPair from a 201 `{detail,email,access,refresh}`; lookup 410 → ApiException 410. Verify, commit `feat(s1c-c): invitation lookup + complete API + tests`.

---

## Task 2 — AuthRepository (TDD)
Add (after `confirmPasswordReset`), import `CompleteInvitationBody`, `ValidateInvitation`:
```kotlin
    suspend fun lookupInvitation(token: String): Result<ValidateInvitation> = api.lookupInvitation(token)

    /** POST invitations/lookup/{token}/ → create account + join + auto-login. */
    suspend fun acceptInvitation(token: String, password: String): Result<UserProfile> =
        autoLogin { api.completeInvitation(token, CompleteInvitationBody(password)) }
```
Test `AuthRepositoryInvitationTest.kt`: accept stores tokens + returns profile (complete 201 → me); accept failure leaves tokens untouched. Verify, commit `feat(s1c-c): AuthRepository invitation lookup + accept (auto-login) + tests`.

---

## Task 3 — Deep-link (TDD)
`DeepLink.kt`: add `data class Invitation(val token: String) : DeepLinkTarget` and, in `parseDeepLink`, `extract(uri, "/invitation/")?.let { return DeepLinkTarget.Invitation(it) }` (add AFTER the `/auth/*` markers). Add 1 test (`parsesInvitation` for `https://tm.foxugly.com/invitation/TKN`). Verify, commit `feat(s1c-c): deep-link parsing for /invitation/{token}`.

---

## Task 4 — ViewModel + strings (TDD)
`ui/invitation/InvitationStrings.kt` (FR): join title, "Rejoindre {team}", password/confirm, join button, statuses (alreadyHandled/expired), email-taken, mismatch/tooShort, lookupFailed, backToLogin.

`ui/invitation/InvitationViewModel.kt`:
```kotlin
class InvitationViewModel(authRepository) {
    var isLoading by mutableStateOf(true) private set         // lookup in flight
    var teamName by mutableStateOf<String?>(null) private set
    var status by mutableStateOf<String?>(null) private set    // "pending" etc.
    var lookupError by mutableStateOf<String?>(null) private set
    var password by mutableStateOf(""); var confirmPassword by mutableStateOf("")
    var isSubmitting by mutableStateOf(false) private set
    var submitError by mutableStateOf<String?>(null) private set
    val isPending get() = status == "pending"
    val canSubmit get() = isPending && !isSubmitting && password.length >= 8 && confirmPassword.isNotBlank()
    fun clearSubmitError(){ submitError = null }
    suspend fun load(token) { isLoading=true; lookupError=null
        authRepository.lookupInvitation(token).fold(
          onSuccess={ teamName=it.teamName; status=it.status },
          onFailure={ lookupError = InvitationStrings.lookupFailed }); isLoading=false }
    suspend fun accept(token, onSuccess) { if(isSubmitting) return
        if(password.length<8){submitError=InvitationStrings.tooShort;return}
        if(password!=confirmPassword){submitError=InvitationStrings.mismatch;return}
        isSubmitting=true; submitError=null
        authRepository.acceptInvitation(token,password).fold(
          onSuccess={onSuccess()},
          onFailure={ t -> submitError = if(t is ApiException && t.statusCode==409) InvitationStrings.emailTaken else InvitationStrings.joinFailed }); isSubmitting=false }
}
```
Test `InvitationViewModelTest.kt`: load pending sets teamName+isPending; load 410 sets lookupError; accept mismatch → submitError; accept success → onSuccess; accept 409 → emailTaken. Verify, commit `feat(s1c-c): invitation ViewModel + strings + tests`.

---

## Task 5 — Screen (compile)
`ui/invitation/InvitationScreen.kt`: `LaunchedEffect(token){vm.load(token)}`; isLoading→spinner; lookupError→message+back; teamName!=null && !isPending→"invitation déjà traitée/expirée"+back; pending→"Rejoindre {teamName}" + 2 password fields + submit(canSubmit) + submitError banner + back. Verify compile, commit `feat(s1c-c): invitation screen`.

---

## Task 6 — Wiring (compile + tests + APK)
- `Routes.kt`: `@Serializable data class InvitationRoute(val token: String)`.
- `AppModule.kt`: `factory { InvitationViewModel(get()) }`.
- `App.kt`: deep-link `when` += `is DeepLinkTarget.Invitation -> navigate(InvitationRoute(d.token))`; `composable<InvitationRoute>` (toRoute, koinInject, onSuccess→Home popUpTo Login, onBackToLogin→Login popUpTo(0)).
- `AndroidManifest.xml`: App Links `pathPrefix=/invitation/` on host tm.foxugly.com.
- Ops doc note: `/invitation/*` now active (already listed). Verify `:composeApp:testAndroidHostTest` + `:androidApp:assembleDebug`. Commit `feat(s1c-c): routes + factory + App routing + manifest`.

## Self-Review
Covers S1 §7 invitation (lookup + accept, auto-login, 201, 409 email-taken). Types: `ValidateInvitation`/`CompleteInvitationBody` → API → repo (`lookupInvitation`/`acceptInvitation` via `autoLogin`) → VM → screen → route. `DeepLinkTarget.Invitation` parsed + routed. After this, S1c remaining = register + forgot-password (Turnstile-blocked) + resend (needs register/CheckEmail). Then S1d (profile + i18n) and the codegen slice.
