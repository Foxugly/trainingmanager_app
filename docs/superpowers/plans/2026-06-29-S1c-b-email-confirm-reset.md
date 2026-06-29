# S1c-b — Email-confirm + Reset-password-confirm Implementation Plan

> Executes inline (autonomous). Reuses the S1c-a deep-link foundation + magic-link pattern verbatim.

**Goal:** Add the two remaining deep-link-arriving, captcha-free auto-login flows: **email confirmation** (`/auth/confirm-email/{key}`) and **password-reset confirm** (`/auth/reset-password/{key}`). Both land via deep link, exchange for a JWT pair, and auto-login. (Resend + the Turnstile-gated request screens are out of scope.)

**Architecture:** Identical shape to S1c-a magic-link. New DTOs + `TrainingManagerApi` methods (`confirmEmail`, `confirmPasswordReset` → `Result<TokenPair>`, `ignoreUnknownKeys` drops the extra `user` object), `AuthRepository` auto-login methods (store tokens → `/me/`), extend `DeepLinkTarget`/`parseDeepLink`, two routes, two ViewModels + screens (email-confirm = auto like magic-link exchange; reset-password = a new-password form), wire into `App()` + Android manifest path prefixes. `AUTH_PATHS` already lists both endpoints.

**Contracts (from schema):** `POST auth/email/confirm/ {key}` → 200 `{access,refresh,user}`, 400 `code=invalid_or_expired_token`. `POST auth/password/reset/confirm/ {key,new_password}` → 200 `{access,refresh,user}`, 400 `invalid_or_expired_token` OR `fields.new_password`. Web link shapes: `/auth/confirm-email/{key}`, `/auth/reset-password/{key}`.

## Global Constraints
Same as S1c-a. Verify: `.\gradlew.bat :composeApp:testAndroidHostTest`; APK: `:androidApp:assembleDebug`. Never build iOS on Windows (iOS `handleDeepLink` bridge from S1c-a is generic — no new iOS code, only manifest path-prefixes + ops-doc paths already listed).

---

## Task 1 — DTOs + API methods (TDD)
Create `data/api/ConfirmDtos.kt`; modify `TrainingManagerApi.kt`; test `data/api/ConfirmApiTest.kt`.

`ConfirmDtos.kt`:
```kotlin
package com.foxugly.trainingmanager_app.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** POST auth/email/confirm/ — confirm registration via the e-mailed key. */
@Serializable
data class EmailConfirmBody(val key: String)

/** POST auth/password/reset/confirm/ — set a new password via the e-mailed key. */
@Serializable
data class PasswordResetConfirmBody(
    val key: String,
    @SerialName("new_password") val newPassword: String,
)
```

API methods (after `magicLinkExchange`, before `// --- Helpers ---`). Both decode to `TokenPair` (the `user` field is ignored):
```kotlin
    suspend fun confirmEmail(request: EmailConfirmBody): Result<TokenPair> = apiCall {
        client.post("auth/email/confirm/") { setBody(request) }
    }

    suspend fun confirmPasswordReset(request: PasswordResetConfirmBody): Result<TokenPair> = apiCall {
        client.post("auth/password/reset/confirm/") { setBody(request) }
    }
```

Test `ConfirmApiTest.kt`:
```kotlin
package com.foxugly.trainingmanager_app.data.api

import com.foxugly.trainingmanager_app.FakeTokenStore
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ConfirmApiTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun api(engine: MockEngine) =
        TrainingManagerApi(FakeTokenStore(), baseUrl = "https://test/api/v1/", engine = engine)

    @Test fun confirmEmailDecodesTokenPairIgnoringUser() = runTest {
        val api = api(MockEngine { respond("""{"access":"a","refresh":"r","user":{"id":1,"email":"x@y.z"}}""", HttpStatusCode.OK, jsonHeader) })
        assertEquals("a", api.confirmEmail(EmailConfirmBody("k")).getOrThrow().access)
    }

    @Test fun resetConfirmDecodesTokenPair() = runTest {
        val api = api(MockEngine { respond("""{"access":"a2","refresh":"r2","user":{"id":1,"email":"x@y.z"}}""", HttpStatusCode.OK, jsonHeader) })
        assertEquals("r2", api.confirmPasswordReset(PasswordResetConfirmBody("k", "password1")).getOrThrow().refresh)
    }

    @Test fun confirmEmailInvalidSurfaces400() = runTest {
        val api = api(MockEngine { respond("""{"code":"invalid_or_expired_token"}""", HttpStatusCode.BadRequest, jsonHeader) })
        assertEquals(400, (api.confirmEmail(EmailConfirmBody("k")).exceptionOrNull() as ApiException).statusCode)
    }
}
```
Verify: `.\gradlew.bat :composeApp:testAndroidHostTest --tests "*ConfirmApiTest*"` → PASS. Commit `feat(s1c-b): confirm-email + reset-password API methods + tests`.

---

## Task 2 — AuthRepository methods (TDD)
Modify `AuthRepository.kt` (+ imports `EmailConfirmBody`, `PasswordResetConfirmBody`); test `data/repository/AuthRepositoryConfirmTest.kt`.

Methods (after `exchangeMagicLink`):
```kotlin
    /** POST auth/email/confirm/ → store tokens → GET me/ → profile. */
    suspend fun confirmEmail(key: String): Result<UserProfile> = autoLogin { api.confirmEmail(EmailConfirmBody(key)) }

    /** POST auth/password/reset/confirm/ → store tokens → GET me/ → profile. */
    suspend fun confirmPasswordReset(key: String, newPassword: String): Result<UserProfile> =
        autoLogin { api.confirmPasswordReset(PasswordResetConfirmBody(key, newPassword)) }
```
Refactor the shared store-then-me step into a private helper, and route `exchangeMagicLink` through it too (DRY):
```kotlin
    private suspend inline fun autoLogin(
        crossinline tokenCall: suspend () -> Result<com.foxugly.trainingmanager_app.data.api.TokenPair>,
    ): Result<UserProfile> = tokenCall().mapCatching { pair ->
        tokenStorage.setAccessToken(pair.access)
        tokenStorage.setRefreshToken(pair.refresh)
        tokenStorage.setRemember(true)
        api.getMe().getOrThrow()
    }.onFailure {
        if (it is CancellationException) throw it
        AppLogger.error(tag, "Auto-login failed: ${it.message}", it)
    }
```
And change `exchangeMagicLink` body to `autoLogin { api.magicLinkExchange(MagicLinkExchangeBody(token)) }` (import `TokenPair` not needed if fully-qualified in helper). Keep `requestMagicLink` as-is.

Test `AuthRepositoryConfirmTest.kt`:
```kotlin
package com.foxugly.trainingmanager_app.data.repository

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.data.api.TrainingManagerApi
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthRepositoryConfirmTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun repo(store: FakeTokenStore, engine: MockEngine) =
        AuthRepository(TrainingManagerApi(store, baseUrl = "https://test/api/v1/", engine = engine), store)

    @Test fun confirmEmailStoresTokensAndReturnsProfile() = runTest {
        val store = FakeTokenStore()
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("email/confirm/") -> respond("""{"access":"a","refresh":"r"}""", HttpStatusCode.OK, jsonHeader)
                else -> respond("""{"id":3,"email":"a@b.co"}""", HttpStatusCode.OK, jsonHeader)
            }
        }
        val p = repo(store, engine).confirmEmail("k").getOrThrow()
        assertEquals(3, p.id)
        assertEquals("a", store.getAccessToken())
        assertTrue(store.getRemember())
    }

    @Test fun resetConfirmFailureLeavesTokensUntouched() = runTest {
        val store = FakeTokenStore()
        val engine = MockEngine { respond("""{"code":"invalid_or_expired_token"}""", HttpStatusCode.BadRequest, jsonHeader) }
        assertTrue(repo(store, engine).confirmPasswordReset("k", "password1").isFailure)
        assertNull(store.getAccessToken())
    }
}
```
Verify `--tests "*AuthRepositoryConfirmTest*"` → PASS. Commit `feat(s1c-b): AuthRepository confirm-email + reset-confirm (auto-login, DRY helper) + tests`.

---

## Task 3 — Extend deep-link parsing (TDD)
Modify `navigation/DeepLink.kt`; extend `DeepLinkParseTest.kt`.

`DeepLink.kt` — add targets + markers:
```kotlin
sealed interface DeepLinkTarget {
    data class MagicLinkExchange(val token: String) : DeepLinkTarget
    data class EmailConfirm(val key: String) : DeepLinkTarget
    data class PasswordResetConfirm(val key: String) : DeepLinkTarget
}

fun parseDeepLink(uri: String?): DeepLinkTarget? {
    if (uri == null) return null
    extract(uri, "/auth/magic-link/")?.let { return DeepLinkTarget.MagicLinkExchange(it) }
    extract(uri, "/auth/confirm-email/")?.let { return DeepLinkTarget.EmailConfirm(it) }
    extract(uri, "/auth/reset-password/")?.let { return DeepLinkTarget.PasswordResetConfirm(it) }
    return null
}

private fun extract(uri: String, marker: String): String? {
    val i = uri.indexOf(marker)
    if (i < 0) return null
    val seg = uri.substring(i + marker.length).substringBefore('/').substringBefore('?').substringBefore('#')
    return seg.ifBlank { null }
}
```
Add tests:
```kotlin
    @Test fun parsesEmailConfirm() =
        assertEquals(DeepLinkTarget.EmailConfirm("K1"), parseDeepLink("https://tm.foxugly.com/auth/confirm-email/K1"))
    @Test fun parsesResetPassword() =
        assertEquals(DeepLinkTarget.PasswordResetConfirm("uid-tok"), parseDeepLink("trainingmanager://app/auth/reset-password/uid-tok"))
```
Verify `--tests "*DeepLinkParseTest*"` → PASS (8). Commit `feat(s1c-b): extend deep-link parsing (confirm-email, reset-password)`.

---

## Task 4 — ViewModels + strings (TDD)
Create `ui/confirm/ConfirmStrings.kt`, `ui/confirm/EmailConfirmViewModel.kt`, `ui/confirm/ResetPasswordViewModel.kt`; tests `EmailConfirmViewModelTest.kt`, `ResetPasswordViewModelTest.kt`.

`ConfirmStrings.kt`:
```kotlin
package com.foxugly.trainingmanager_app.ui.confirm

object ConfirmStrings {
    const val emailConfirmLoading = "Confirmation en cours…"
    const val emailInvalidTitle = "Lien invalide ou expiré"
    const val emailInvalidBody = "Ce lien de confirmation n'est plus valide."
    const val resetTitle = "Nouveau mot de passe"
    const val newPassword = "Nouveau mot de passe"
    const val confirmPassword = "Confirmer le mot de passe"
    const val submit = "Valider"
    const val mismatch = "Les mots de passe ne correspondent pas."
    const val tooShort = "Le mot de passe doit faire au moins 8 caractères."
    const val tokenInvalidTitle = "Lien invalide ou expiré"
    const val tokenInvalidBody = "Ce lien de réinitialisation n'est plus valide. Demandez-en un nouveau."
    const val resetFailed = "La réinitialisation a échoué. Réessayez."
    const val backToLogin = "Retour à la connexion"
}
```

`EmailConfirmViewModel.kt` (mirror MagicLinkExchangeViewModel, no 410 split):
```kotlin
package com.foxugly.trainingmanager_app.ui.confirm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.data.repository.AuthRepository

class EmailConfirmViewModel(private val authRepository: AuthRepository) {
    enum class State { Loading, Success, Invalid }
    var state by mutableStateOf(State.Loading)
        private set

    suspend fun confirm(key: String, onSuccess: () -> Unit) {
        state = State.Loading
        authRepository.confirmEmail(key).fold(
            onSuccess = { state = State.Success; onSuccess() },
            onFailure = { state = State.Invalid },
        )
    }
}
```

`ResetPasswordViewModel.kt`:
```kotlin
package com.foxugly.trainingmanager_app.ui.confirm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.data.api.ApiException
import com.foxugly.trainingmanager_app.data.repository.AuthRepository

class ResetPasswordViewModel(private val authRepository: AuthRepository) {
    var newPassword by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var tokenInvalid by mutableStateOf(false)
        private set

    val canSubmit: Boolean get() = !isLoading && newPassword.length >= 8 && confirmPassword.isNotBlank()

    fun clearError() { error = null }

    /** Validate locally, then submit; on success auto-login + [onSuccess]. */
    suspend fun submit(key: String, onSuccess: () -> Unit) {
        if (isLoading) return
        if (newPassword.length < 8) { error = ConfirmStrings.tooShort; return }
        if (newPassword != confirmPassword) { error = ConfirmStrings.mismatch; return }
        isLoading = true
        error = null
        authRepository.confirmPasswordReset(key, newPassword).fold(
            onSuccess = { onSuccess() },
            onFailure = { t ->
                if (t is ApiException && t.statusCode == 400 && t.message?.contains("invalid_or_expired_token") == true) {
                    tokenInvalid = true
                } else {
                    error = ConfirmStrings.resetFailed
                }
            },
        )
        isLoading = false
    }
}
```

Tests — `EmailConfirmViewModelTest.kt`:
```kotlin
package com.foxugly.trainingmanager_app.ui.confirm

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.data.api.TrainingManagerApi
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmailConfirmViewModelTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun vm(engine: MockEngine): EmailConfirmViewModel {
        val s = FakeTokenStore()
        return EmailConfirmViewModel(AuthRepository(TrainingManagerApi(s, baseUrl = "https://test/api/v1/", engine = engine), s))
    }

    @Test fun successOnValidKey() = runTest {
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("email/confirm/") -> respond("""{"access":"a","refresh":"r"}""", HttpStatusCode.OK, jsonHeader)
                else -> respond("""{"id":1,"email":"a@b.co"}""", HttpStatusCode.OK, jsonHeader)
            }
        }
        val sut = vm(engine); var ok = false
        sut.confirm("k") { ok = true }
        assertTrue(ok)
        assertEquals(EmailConfirmViewModel.State.Success, sut.state)
    }

    @Test fun invalidOn400() = runTest {
        val sut = vm(MockEngine { respond("""{"code":"invalid_or_expired_token"}""", HttpStatusCode.BadRequest, jsonHeader) })
        sut.confirm("k") {}
        assertEquals(EmailConfirmViewModel.State.Invalid, sut.state)
    }
}
```
`ResetPasswordViewModelTest.kt`:
```kotlin
package com.foxugly.trainingmanager_app.ui.confirm

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.data.api.TrainingManagerApi
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResetPasswordViewModelTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun vm(engine: MockEngine): ResetPasswordViewModel {
        val s = FakeTokenStore()
        return ResetPasswordViewModel(AuthRepository(TrainingManagerApi(s, baseUrl = "https://test/api/v1/", engine = engine), s))
    }
    private fun okEngine() = MockEngine { request ->
        when {
            request.url.encodedPath.endsWith("reset/confirm/") -> respond("""{"access":"a","refresh":"r"}""", HttpStatusCode.OK, jsonHeader)
            else -> respond("""{"id":1,"email":"a@b.co"}""", HttpStatusCode.OK, jsonHeader)
        }
    }

    @Test fun mismatchSetsErrorAndDoesNotSubmit() = runTest {
        val sut = vm(okEngine())
        sut.newPassword = "password1"; sut.confirmPassword = "password2"
        var ok = false
        sut.submit("k") { ok = true }
        assertFalse(ok)
        assertEquals(ConfirmStrings.mismatch, sut.error)
    }

    @Test fun successAutoLogins() = runTest {
        val sut = vm(okEngine())
        sut.newPassword = "password1"; sut.confirmPassword = "password1"
        var ok = false
        sut.submit("k") { ok = true }
        assertTrue(ok)
    }

    @Test fun tokenInvalidOn400ExpiredToken() = runTest {
        val sut = vm(MockEngine { respond("""{"code":"invalid_or_expired_token"}""", HttpStatusCode.BadRequest, jsonHeader) })
        sut.newPassword = "password1"; sut.confirmPassword = "password1"
        sut.submit("k") {}
        assertTrue(sut.tokenInvalid)
    }
}
```
Verify `--tests "*ConfirmViewModelTest*" --tests "*ResetPasswordViewModelTest*"` → PASS. Commit `feat(s1c-b): email-confirm + reset-password ViewModels + strings + tests`.

---

## Task 5 — Screens (compile)
Create `ui/confirm/EmailConfirmScreen.kt` (mirror MagicLinkExchangeScreen: Loading/Success → spinner, Invalid → message + back) and `ui/confirm/ResetPasswordScreen.kt` (two password fields + submit + tokenInvalid state + ErrorBanner). Full code mirrors S1c-a screens; use `ConfirmStrings`. Verify `:composeApp:compileAndroidMain`. Commit `feat(s1c-b): email-confirm + reset-password screens`.

(Concrete screen code is written during execution following the MagicLink screen templates — Loading/terminal-state for confirm; form for reset with `OutlinedTextField` x2 (PasswordVisualTransformation), `Button` gated on `canSubmit`, `ErrorBanner` for `error`, and a tokenInvalid branch showing `tokenInvalidTitle/Body` + back-to-login.)

---

## Task 6 — Routes + Koin + App wiring + manifest (compile + tests + APK)
- `Routes.kt`: `@Serializable data class EmailConfirmRoute(val key: String)`, `@Serializable data class ResetPasswordRoute(val key: String)`.
- `AppModule.kt`: `factory { EmailConfirmViewModel(get()) }`, `factory { ResetPasswordViewModel(get()) }`.
- `App.kt`: extend the deep-link `LaunchedEffect` `when` with `EmailConfirm` → navigate `EmailConfirmRoute(key)`, `PasswordResetConfirm` → navigate `ResetPasswordRoute(key)`; add `composable<EmailConfirmRoute>`/`composable<ResetPasswordRoute>` (read `toRoute()`, koinInject VM, onSuccess → Home popUpTo Login, onBackToLogin → Login popUpTo(0)).
- `AndroidManifest.xml`: add two App Links intent-filters (or extend) for `pathPrefix=/auth/confirm-email/` and `/auth/reset-password/` (host `tm.foxugly.com`, autoVerify). Custom scheme filter already matches all `trainingmanager://` paths.
- Verify `:composeApp:testAndroidHostTest` + `:androidApp:assembleDebug`. Commit `feat(s1c-b): routes + factories + App deep-link routing + manifest path-prefixes`.

## Self-Review
Covers S1 §7 email-confirm + reset-confirm (deep-link, auto-login). Deferred: resend (needs register/CheckEmail entry point), invitation (S1c-c), register/forgot-password (Turnstile). Deep-link HTTPS activation: ops doc already lists `/auth/confirm-email/*` + `/auth/reset-password/*`. Types: `EmailConfirmBody`/`PasswordResetConfirmBody` → API `Result<TokenPair>` → repo `autoLogin` → `Result<UserProfile>` → VMs → screens → routes. `DeepLinkTarget.{EmailConfirm,PasswordResetConfirm}` parsed + routed. Consistent with S1c-a.
