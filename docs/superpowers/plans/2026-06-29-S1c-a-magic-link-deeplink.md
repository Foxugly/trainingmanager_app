# S1c-a — Deep-link foundation + Magic-link end-to-end Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish the app's deep-link foundation and prove it end-to-end with the **magic-link** (passwordless) sign-in: an in-app "request a magic link" screen + an "exchange" screen reached by a deep link that auto-logs the user in. Later slices (S1c-b/c) reuse the same foundation for email-confirm / reset-confirm / invitation.

**Architecture:** Reuse S1a's tested Ktor stack (`TrainingManagerApi`/`AuthInterceptor` — `AUTH_PATHS` already lists the magic-link paths — and `AuthRepository`). Add two thin auth endpoints + repository methods, a pure `parseDeepLink` function (commonTest-able, no platform URI lib), two plain testable ViewModels + their Compose screens, two type-safe nav routes, and platform plumbing: an Android `ACTION_VIEW` intent path (custom scheme **now**, HTTPS App Links wired for later ops activation) feeding `intent.data → App(deepLink=…)`, plus the iOS `.onOpenURL` bridge (written, **not compilable on Windows**).

**Tech Stack:** Kotlin 2.3.21, Compose MP 1.10.3, Ktor 3.1.3, kotlinx-serialization 1.8.1, Koin 4.1.0, Navigation Compose 2.9.2. Tests: kotlin-test + `runTest` + Ktor `MockEngine`, JVM host.

## Global Constraints

- **Package:** `com.foxugly.trainingmanager_app`.
- **Verify task (every task):** `.\gradlew.bat :composeApp:testAndroidHostTest`. APK gate: `.\gradlew.bat :androidApp:assembleDebug`. Compile-only check: `.\gradlew.bat :composeApp:compileAndroidMain`. **Never build iOS targets on Windows.**
- **Auth stack frozen:** do not change `AuthInterceptor`/`apiCall` behavior. `AUTH_PATHS` already contains `auth/magic-link/request/` and `auth/magic-link/exchange/` — no edit needed.
- **Magic-link contract** (from schema): request `POST auth/magic-link/request/ {email}` → always-200 `{detail}` (rate-limited 5/h/IP, no captcha); exchange `POST auth/magic-link/exchange/ {token}` → 200 `{access, refresh}`, **410** `{"detail":"token_expired"}`, **400** `{"detail":"token_invalid"}`. Bare-`detail` error style (NOT `code=`). Distinguish 410 vs 400 by `ApiException.statusCode`.
- **Deep-link host (prod):** `tm.foxugly.com` (confirmed). Web link shape: `https://tm.foxugly.com/auth/magic-link/{token}` (no trailing slash, optional `?returnUrl=`). **Dev/test custom scheme:** `trainingmanager://…/auth/magic-link/{token}`.
- **HTTPS App Links activation is OPS-EXTERNAL** (host `assetlinks.json` + iOS AASA + release keystore SHA-256). This slice ships the app-side plumbing + the immediately-testable custom scheme; HTTPS auto-verification is a documented ops prerequisite, not executed here.
- **i18n:** minimal FR strings (full 5-locale i18n is S1d).
- DRY, YAGNI, TDD, frequent commits.

---

## File Structure

**Created:**
- `composeApp/src/commonMain/kotlin/.../data/api/MagicLinkDtos.kt` — `MagicLinkRequestBody`, `MagicLinkExchangeBody`.
- `composeApp/src/commonMain/kotlin/.../navigation/DeepLink.kt` — `DeepLinkTarget` sealed + pure `parseDeepLink(uri): DeepLinkTarget?`.
- `composeApp/src/commonMain/kotlin/.../navigation/Routes.kt` — extend with `MagicLinkRequestRoute`, `MagicLinkExchangeRoute(token)`.
- `composeApp/src/commonMain/kotlin/.../ui/magiclink/MagicLinkStrings.kt`
- `composeApp/src/commonMain/kotlin/.../ui/magiclink/MagicLinkRequestViewModel.kt`
- `composeApp/src/commonMain/kotlin/.../ui/magiclink/MagicLinkExchangeViewModel.kt`
- `composeApp/src/commonMain/kotlin/.../ui/magiclink/MagicLinkRequestScreen.kt`
- `composeApp/src/commonMain/kotlin/.../ui/magiclink/MagicLinkExchangeScreen.kt`
- commonTest: `MagicLinkApiTest.kt`, `DeepLinkParseTest.kt`, `MagicLinkRequestViewModelTest.kt`, `MagicLinkExchangeViewModelTest.kt`, `AuthRepositoryMagicLinkTest.kt`.

**Modified:**
- `data/api/TrainingManagerApi.kt` — add `magicLinkRequest`, `magicLinkExchange`.
- `data/repository/AuthRepository.kt` — add `requestMagicLink`, `exchangeMagicLink`.
- `di/AppModule.kt` — factories for the two ViewModels.
- `App.kt` — `deepLink`/`onDeepLinkConsumed` params; nav routes for magic-link; deep-link routing; Login entry button.
- `ui/login/LoginScreen.kt` — add `onMagicLink` callback + a "sign in by email link" text button; `ui/login/LoginStrings.kt` — one string.
- `androidApp/.../MainActivity.kt` — read `intent.data` (launch + `onNewIntent`) → `parseDeepLink` → state → `App(...)`.
- `androidApp/src/main/AndroidManifest.xml` — `ACTION_VIEW` intent-filters (custom scheme + HTTPS App Links).
- `composeApp/src/iosMain/.../MainViewController.kt` + `iosApp/iosApp/ContentView.swift` + `iosApp/iosApp/Info.plist` — iOS URL plumbing (**written, unverified on Windows**).

---

## Task 1 — Magic-link DTOs + API methods (TDD)

**Files:** Create `data/api/MagicLinkDtos.kt`; Modify `data/api/TrainingManagerApi.kt`; Test `commonTest/.../data/api/MagicLinkApiTest.kt`.

**Interfaces:**
- Produces: `data class MagicLinkRequestBody(email)`, `data class MagicLinkExchangeBody(token)`; `suspend fun TrainingManagerApi.magicLinkRequest(MagicLinkRequestBody): Result<Unit>`; `suspend fun magicLinkExchange(MagicLinkExchangeBody): Result<TokenPair>`.
- Consumes: S1a `apiCall`, `TokenPair`, `ApiException`.

- [ ] **Step 1: Create `MagicLinkDtos.kt`**

```kotlin
package com.foxugly.trainingmanager_app.data.api

import kotlinx.serialization.Serializable

/** POST auth/magic-link/request/ — always-200, no captcha. */
@Serializable
data class MagicLinkRequestBody(val email: String)

/** POST auth/magic-link/exchange/ — trades a magic token for a JWT pair. */
@Serializable
data class MagicLinkExchangeBody(val token: String)
```

- [ ] **Step 2: Write the failing test** — `commonTest/.../data/api/MagicLinkApiTest.kt`

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
import kotlin.test.assertTrue

class MagicLinkApiTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun api(engine: MockEngine) =
        TrainingManagerApi(FakeTokenStore(), baseUrl = "https://test/api/v1/", engine = engine)

    @Test fun requestReturnsSuccessOnAlways200() = runTest {
        val api = api(MockEngine { respond("""{"detail":"ok"}""", HttpStatusCode.OK, jsonHeader) })
        assertTrue(api.magicLinkRequest(MagicLinkRequestBody("a@b.co")).isSuccess)
    }

    @Test fun exchangeReturnsTokenPairOn200() = runTest {
        val api = api(MockEngine { respond("""{"access":"acc","refresh":"ref"}""", HttpStatusCode.OK, jsonHeader) })
        val pair = api.magicLinkExchange(MagicLinkExchangeBody("tok")).getOrThrow()
        assertEquals("acc", pair.access)
        assertEquals("ref", pair.refresh)
    }

    @Test fun exchangeExpiredSurfaces410() = runTest {
        val api = api(MockEngine { respond("""{"detail":"token_expired"}""", HttpStatusCode.Gone, jsonHeader) })
        val err = api.magicLinkExchange(MagicLinkExchangeBody("tok")).exceptionOrNull()
        assertEquals(410, (err as ApiException).statusCode)
    }

    @Test fun exchangeInvalidSurfaces400() = runTest {
        val api = api(MockEngine { respond("""{"detail":"token_invalid"}""", HttpStatusCode.BadRequest, jsonHeader) })
        val err = api.magicLinkExchange(MagicLinkExchangeBody("tok")).exceptionOrNull()
        assertEquals(400, (err as ApiException).statusCode)
    }
}
```

- [ ] **Step 3: Run — verify it fails**

Run: `.\gradlew.bat :composeApp:testAndroidHostTest --tests "*MagicLinkApiTest*"`
Expected: FAIL (`magicLinkRequest`/`magicLinkExchange` unresolved).

- [ ] **Step 4: Add the two methods to `TrainingManagerApi.kt`** (after the `register(...)` method, before `// --- Helpers ---`)

```kotlin
    suspend fun magicLinkRequest(request: MagicLinkRequestBody): Result<Unit> = apiCall {
        client.post("auth/magic-link/request/") { setBody(request) }
    }

    suspend fun magicLinkExchange(request: MagicLinkExchangeBody): Result<TokenPair> = apiCall {
        client.post("auth/magic-link/exchange/") { setBody(request) }
    }
```

- [ ] **Step 5: Run — verify it passes**

Run: `.\gradlew.bat :composeApp:testAndroidHostTest --tests "*MagicLinkApiTest*"`
Expected: PASS (4 tests).

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/data/api/MagicLinkDtos.kt composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/data/api/TrainingManagerApi.kt composeApp/src/commonTest/kotlin/com/foxugly/trainingmanager_app/data/api/MagicLinkApiTest.kt
git commit -m "feat(magic-link): TrainingManagerApi request + exchange endpoints + tests"
```

---

## Task 2 — AuthRepository magic-link methods (TDD)

**Files:** Modify `data/repository/AuthRepository.kt`; Test `commonTest/.../data/repository/AuthRepositoryMagicLinkTest.kt`.

**Interfaces:**
- Produces: `suspend fun requestMagicLink(email: String): Result<Unit>`; `suspend fun exchangeMagicLink(token: String): Result<UserProfile>` (exchange → persist access+refresh+remember=true → `getMe`). Mirrors `login()`.
- Consumes: Task 1 API methods, S1a `UserProfile`/`TokenStore`.

- [ ] **Step 1: Write the failing test**

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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthRepositoryMagicLinkTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun repo(store: FakeTokenStore, engine: MockEngine) =
        AuthRepository(TrainingManagerApi(store, baseUrl = "https://test/api/v1/", engine = engine), store)

    @Test fun exchangeStoresTokensAndReturnsProfile() = runTest {
        val store = FakeTokenStore()
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("magic-link/exchange/") ->
                    respond("""{"access":"acc","refresh":"ref"}""", HttpStatusCode.OK, jsonHeader)
                request.url.encodedPath.endsWith("me/") ->
                    respond("""{"id":9,"email":"a@b.co"}""", HttpStatusCode.OK, jsonHeader)
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val profile = repo(store, engine).exchangeMagicLink("tok").getOrThrow()
        assertEquals(9, profile.id)
        assertEquals("acc", store.getAccessToken())
        assertEquals("ref", store.getRefreshToken())
        assertTrue(store.getRemember())
    }

    @Test fun exchangeFailureLeavesTokensUntouched() = runTest {
        val store = FakeTokenStore()
        val engine = MockEngine { respond("""{"detail":"token_expired"}""", HttpStatusCode.Gone, jsonHeader) }
        val result = repo(store, engine).exchangeMagicLink("tok")
        assertTrue(result.isFailure)
        assertNull(store.getAccessToken())
    }

    @Test fun requestReturnsSuccess() = runTest {
        val store = FakeTokenStore()
        val engine = MockEngine { respond("""{"detail":"ok"}""", HttpStatusCode.OK, jsonHeader) }
        assertTrue(repo(store, engine).requestMagicLink("a@b.co").isSuccess)
        assertNull(store.getAccessToken())
        assertFalse(store.getRemember())
    }
}
```

- [ ] **Step 2: Run — verify it fails**

Run: `.\gradlew.bat :composeApp:testAndroidHostTest --tests "*AuthRepositoryMagicLinkTest*"`
Expected: FAIL (methods unresolved).

- [ ] **Step 3: Add methods to `AuthRepository.kt`** (after `login(...)`, mirroring its token-store pattern). Add imports `MagicLinkExchangeBody`, `MagicLinkRequestBody`.

```kotlin
    /** POST auth/magic-link/request/ — always-200, no token handling. */
    suspend fun requestMagicLink(email: String): Result<Unit> {
        AppLogger.info(tag, "Magic-link requested")
        return api.magicLinkRequest(MagicLinkRequestBody(email.trim()))
            .onFailure { if (it is CancellationException) throw it }
    }

    /** POST auth/magic-link/exchange/ → store tokens → GET me/ → profile. */
    suspend fun exchangeMagicLink(token: String): Result<UserProfile> {
        AppLogger.info(tag, "Magic-link exchange requested")
        return api.magicLinkExchange(MagicLinkExchangeBody(token)).mapCatching { pair ->
            tokenStorage.setAccessToken(pair.access)
            tokenStorage.setRefreshToken(pair.refresh)
            tokenStorage.setRemember(true)
            api.getMe().getOrThrow()
        }.onFailure {
            if (it is CancellationException) throw it
            AppLogger.error(tag, "Magic-link exchange failed: ${it.message}", it)
        }
    }
```

- [ ] **Step 4: Run — verify it passes**

Run: `.\gradlew.bat :composeApp:testAndroidHostTest --tests "*AuthRepositoryMagicLinkTest*"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/data/repository/AuthRepository.kt composeApp/src/commonTest/kotlin/com/foxugly/trainingmanager_app/data/repository/AuthRepositoryMagicLinkTest.kt
git commit -m "feat(magic-link): AuthRepository request + exchange (auto-login) + tests"
```

---

## Task 3 — Deep-link parsing (pure, TDD)

**Files:** Create `navigation/DeepLink.kt`; Test `commonTest/.../navigation/DeepLinkParseTest.kt`.

**Interfaces:**
- Produces: `sealed interface DeepLinkTarget { data class MagicLinkExchange(val token: String) : DeepLinkTarget }`; `fun parseDeepLink(uri: String?): DeepLinkTarget?`.
- Consumes: nothing (no platform URI lib — substring match works for both HTTPS host URLs and custom-scheme URIs regardless of authority parsing).

- [ ] **Step 1: Write the failing test**

```kotlin
package com.foxugly.trainingmanager_app.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeepLinkParseTest {
    @Test fun parsesHttpsMagicLink() =
        assertEquals(
            DeepLinkTarget.MagicLinkExchange("ABC123"),
            parseDeepLink("https://tm.foxugly.com/auth/magic-link/ABC123"),
        )

    @Test fun parsesHttpsMagicLinkWithReturnUrlQuery() =
        assertEquals(
            DeepLinkTarget.MagicLinkExchange("ABC123"),
            parseDeepLink("https://tm.foxugly.com/auth/magic-link/ABC123?returnUrl=%2Fdashboard"),
        )

    @Test fun parsesCustomSchemeMagicLink() =
        assertEquals(
            DeepLinkTarget.MagicLinkExchange("TOK"),
            parseDeepLink("trainingmanager://app/auth/magic-link/TOK"),
        )

    @Test fun nullForUnknownPath() =
        assertNull(parseDeepLink("https://tm.foxugly.com/dashboard"))

    @Test fun nullForBlankToken() =
        assertNull(parseDeepLink("https://tm.foxugly.com/auth/magic-link/"))

    @Test fun nullForNullInput() = assertNull(parseDeepLink(null))
}
```

- [ ] **Step 2: Run — verify it fails**

Run: `.\gradlew.bat :composeApp:testAndroidHostTest --tests "*DeepLinkParseTest*"`
Expected: FAIL (unresolved).

- [ ] **Step 3: Create `DeepLink.kt`**

```kotlin
package com.foxugly.trainingmanager_app.navigation

/** A deep link the app knows how to route. Extended in S1c-b/c (email-confirm,
 *  reset-password, invitation). */
sealed interface DeepLinkTarget {
    data class MagicLinkExchange(val token: String) : DeepLinkTarget
}

/**
 * Parse an incoming deep-link URI (HTTPS App Link OR custom scheme) into a target.
 * Matches on the known path marker rather than parsing scheme/authority, so it is
 * robust to both `https://tm.foxugly.com/auth/magic-link/{t}` and
 * `trainingmanager://…/auth/magic-link/{t}` (where the custom scheme's authority
 * is ambiguous). Pure + commonTest-able.
 */
fun parseDeepLink(uri: String?): DeepLinkTarget? {
    if (uri == null) return null
    val marker = "/auth/magic-link/"
    val i = uri.indexOf(marker)
    if (i >= 0) {
        val token = uri.substring(i + marker.length)
            .substringBefore('/').substringBefore('?').substringBefore('#')
        if (token.isNotBlank()) return DeepLinkTarget.MagicLinkExchange(token)
    }
    return null
}
```

- [ ] **Step 4: Run — verify it passes**

Run: `.\gradlew.bat :composeApp:testAndroidHostTest --tests "*DeepLinkParseTest*"`
Expected: PASS (6 tests).

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/navigation/DeepLink.kt composeApp/src/commonTest/kotlin/com/foxugly/trainingmanager_app/navigation/DeepLinkParseTest.kt
git commit -m "feat(deep-link): pure parseDeepLink + DeepLinkTarget + tests"
```

---

## Task 4 — Magic-link ViewModels (TDD)

**Files:** Create `ui/magiclink/MagicLinkRequestViewModel.kt`, `ui/magiclink/MagicLinkExchangeViewModel.kt`, `ui/magiclink/MagicLinkStrings.kt`; Test `commonTest/.../ui/magiclink/MagicLinkRequestViewModelTest.kt`, `MagicLinkExchangeViewModelTest.kt`.

**Interfaces:**
- Produces:
  - `class MagicLinkRequestViewModel(authRepository)` with `var email`, `isLoading` (private set), `sent` (private set), `error` (private set), `canSubmit`, `clearError()`, `suspend fun submit()`. always-200 → `sent = true`; failure → `error` set (offline/timeout/429-rate-limited/generic).
  - `class MagicLinkExchangeViewModel(authRepository)` with `var state: ExchangeState` (private set) where `enum class ExchangeState { Loading, Success, Expired, Invalid }`, and `suspend fun exchange(token: String, onSuccess: () -> Unit)`. 410 → Expired, 400 → Invalid, other/transport → Invalid, success → Success + `onSuccess()`.
- Consumes: Task 2 repo methods, `ApiException` (statusCode), `NetworkException`.

- [ ] **Step 1: Create `MagicLinkStrings.kt`**

```kotlin
package com.foxugly.trainingmanager_app.ui.magiclink

object MagicLinkStrings {
    const val requestTitle = "Connexion par lien e-mail"
    const val email = "E-mail"
    const val send = "Envoyer le lien"
    const val sentTitle = "Vérifiez vos e-mails"
    const val sentBody = "Si un compte existe pour cette adresse, un lien de connexion vient d'être envoyé."
    const val rateLimited = "Trop de demandes. Réessayez plus tard."
    const val networkOffline = "Impossible de joindre le serveur. Vérifiez votre connexion."
    const val networkTimeout = "La requête a expiré. Réessayez."
    const val requestFailed = "L'envoi a échoué. Réessayez."
    const val exchangeLoading = "Connexion en cours…"
    const val expiredTitle = "Lien expiré"
    const val expiredBody = "Ce lien de connexion a expiré. Demandez-en un nouveau."
    const val invalidTitle = "Lien invalide"
    const val invalidBody = "Ce lien de connexion n'est pas valide."
    const val backToLogin = "Retour à la connexion"
}
```

- [ ] **Step 2: Write the failing tests**

`MagicLinkRequestViewModelTest.kt`:
```kotlin
package com.foxugly.trainingmanager_app.ui.magiclink

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

class MagicLinkRequestViewModelTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun vm(engine: MockEngine): MagicLinkRequestViewModel {
        val store = FakeTokenStore()
        return MagicLinkRequestViewModel(AuthRepository(TrainingManagerApi(store, baseUrl = "https://test/api/v1/", engine = engine), store))
    }

    @Test fun submitSetsSentOnSuccess() = runTest {
        val sut = vm(MockEngine { respond("""{"detail":"ok"}""", HttpStatusCode.OK, jsonHeader) })
        sut.email = "a@b.co"
        sut.submit()
        assertTrue(sut.sent)
        assertFalse(sut.isLoading)
    }

    @Test fun submitSetsRateLimitedOn429() = runTest {
        val sut = vm(MockEngine { respond("""{"detail":"x"}""", HttpStatusCode.TooManyRequests, jsonHeader) })
        sut.email = "a@b.co"
        sut.submit()
        assertFalse(sut.sent)
        assertEquals(MagicLinkStrings.rateLimited, sut.error)
    }
}
```

`MagicLinkExchangeViewModelTest.kt`:
```kotlin
package com.foxugly.trainingmanager_app.ui.magiclink

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

class MagicLinkExchangeViewModelTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun vm(engine: MockEngine): MagicLinkExchangeViewModel {
        val store = FakeTokenStore()
        return MagicLinkExchangeViewModel(AuthRepository(TrainingManagerApi(store, baseUrl = "https://test/api/v1/", engine = engine), store))
    }

    @Test fun successInvokesOnSuccess() = runTest {
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("magic-link/exchange/") ->
                    respond("""{"access":"a","refresh":"r"}""", HttpStatusCode.OK, jsonHeader)
                else -> respond("""{"id":1,"email":"a@b.co"}""", HttpStatusCode.OK, jsonHeader)
            }
        }
        val sut = vm(engine)
        var ok = false
        sut.exchange("tok") { ok = true }
        assertTrue(ok)
        assertEquals(MagicLinkExchangeViewModel.ExchangeState.Success, sut.state)
    }

    @Test fun expiredOn410() = runTest {
        val sut = vm(MockEngine { respond("""{"detail":"token_expired"}""", HttpStatusCode.Gone, jsonHeader) })
        sut.exchange("tok") {}
        assertEquals(MagicLinkExchangeViewModel.ExchangeState.Expired, sut.state)
    }

    @Test fun invalidOn400() = runTest {
        val sut = vm(MockEngine { respond("""{"detail":"token_invalid"}""", HttpStatusCode.BadRequest, jsonHeader) })
        sut.exchange("tok") {}
        assertEquals(MagicLinkExchangeViewModel.ExchangeState.Invalid, sut.state)
    }
}
```

- [ ] **Step 3: Run — verify they fail**

Run: `.\gradlew.bat :composeApp:testAndroidHostTest --tests "*MagicLink*ViewModelTest*"`
Expected: FAIL (unresolved).

- [ ] **Step 4: Create `MagicLinkRequestViewModel.kt`**

```kotlin
package com.foxugly.trainingmanager_app.ui.magiclink

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.data.api.ApiException
import com.foxugly.trainingmanager_app.data.api.NetworkErrorKind
import com.foxugly.trainingmanager_app.data.api.NetworkException
import com.foxugly.trainingmanager_app.data.repository.AuthRepository

class MagicLinkRequestViewModel(private val authRepository: AuthRepository) {
    var email by mutableStateOf("")
    var isLoading by mutableStateOf(false)
        private set
    var sent by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    val canSubmit: Boolean get() = !isLoading && email.isNotBlank()

    fun clearError() { error = null }

    suspend fun submit() {
        if (isLoading || email.isBlank()) return
        isLoading = true
        error = null
        authRepository.requestMagicLink(email).fold(
            onSuccess = { sent = true },
            onFailure = { error = mapError(it) },
        )
        isLoading = false
    }

    private fun mapError(t: Throwable): String? = when {
        t is NetworkException && t.kind == NetworkErrorKind.TIMEOUT -> MagicLinkStrings.networkTimeout
        t is NetworkException -> MagicLinkStrings.networkOffline
        t is ApiException && t.statusCode == 429 -> MagicLinkStrings.rateLimited
        else -> MagicLinkStrings.requestFailed
    }
}
```

- [ ] **Step 5: Create `MagicLinkExchangeViewModel.kt`**

```kotlin
package com.foxugly.trainingmanager_app.ui.magiclink

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.data.api.ApiException
import com.foxugly.trainingmanager_app.data.repository.AuthRepository

class MagicLinkExchangeViewModel(private val authRepository: AuthRepository) {
    enum class ExchangeState { Loading, Success, Expired, Invalid }

    var state by mutableStateOf(ExchangeState.Loading)
        private set

    /** Exchange the token once; on success invoke [onSuccess] (navigate Home). */
    suspend fun exchange(token: String, onSuccess: () -> Unit) {
        state = ExchangeState.Loading
        authRepository.exchangeMagicLink(token).fold(
            onSuccess = { state = ExchangeState.Success; onSuccess() },
            onFailure = { t ->
                state = if (t is ApiException && t.statusCode == 410) ExchangeState.Expired
                else ExchangeState.Invalid
            },
        )
    }
}
```

- [ ] **Step 6: Run — verify they pass**

Run: `.\gradlew.bat :composeApp:testAndroidHostTest --tests "*MagicLink*ViewModelTest*"`
Expected: PASS (5 tests).

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/magiclink/MagicLinkStrings.kt composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/magiclink/MagicLinkRequestViewModel.kt composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/magiclink/MagicLinkExchangeViewModel.kt composeApp/src/commonTest/kotlin/com/foxugly/trainingmanager_app/ui/magiclink/MagicLinkRequestViewModelTest.kt composeApp/src/commonTest/kotlin/com/foxugly/trainingmanager_app/ui/magiclink/MagicLinkExchangeViewModelTest.kt
git commit -m "feat(magic-link): request + exchange ViewModels + FR strings + tests"
```

---

## Task 5 — Magic-link screens (compile)

**Files:** Create `ui/magiclink/MagicLinkRequestScreen.kt`, `ui/magiclink/MagicLinkExchangeScreen.kt`.

**Interfaces:**
- Produces: `@Composable fun MagicLinkRequestScreen(viewModel, onBack)`; `@Composable fun MagicLinkExchangeScreen(viewModel, token, onSuccess, onBackToLogin)`.
- Consumes: Task 4 ViewModels + strings, S1a `ErrorBanner`.

> No unit test (composables); validated by `assembleDebug` (Task 7).

- [ ] **Step 1: Create `MagicLinkRequestScreen.kt`**

```kotlin
package com.foxugly.trainingmanager_app.ui.magiclink

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.foxugly.trainingmanager_app.ui.components.ErrorBanner
import kotlinx.coroutines.launch

@Composable
fun MagicLinkRequestScreen(viewModel: MagicLinkRequestViewModel, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        if (viewModel.sent) {
            Text(MagicLinkStrings.sentTitle, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text(MagicLinkStrings.sentBody, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))
            TextButton(onClick = onBack) { Text(MagicLinkStrings.backToLogin) }
            return@Column
        }

        Text(MagicLinkStrings.requestTitle, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        viewModel.error?.let { ErrorBanner(it); Spacer(Modifier.height(12.dp)) }
        OutlinedTextField(
            value = viewModel.email,
            onValueChange = { viewModel.email = it; viewModel.clearError() },
            label = { Text(MagicLinkStrings.email) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { scope.launch { viewModel.submit() } },
            enabled = viewModel.canSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(MagicLinkStrings.send)
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) { Text(MagicLinkStrings.backToLogin) }
    }
}
```

- [ ] **Step 2: Create `MagicLinkExchangeScreen.kt`**

```kotlin
package com.foxugly.trainingmanager_app.ui.magiclink

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MagicLinkExchangeScreen(
    viewModel: MagicLinkExchangeViewModel,
    token: String,
    onSuccess: () -> Unit,
    onBackToLogin: () -> Unit,
) {
    LaunchedEffect(token) { viewModel.exchange(token, onSuccess) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (viewModel.state) {
            MagicLinkExchangeViewModel.ExchangeState.Loading,
            MagicLinkExchangeViewModel.ExchangeState.Success -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(MagicLinkStrings.exchangeLoading)
            }
            MagicLinkExchangeViewModel.ExchangeState.Expired -> {
                Text(MagicLinkStrings.expiredTitle, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(MagicLinkStrings.expiredBody)
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = onBackToLogin) { Text(MagicLinkStrings.backToLogin) }
            }
            MagicLinkExchangeViewModel.ExchangeState.Invalid -> {
                Text(MagicLinkStrings.invalidTitle, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(MagicLinkStrings.invalidBody)
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = onBackToLogin) { Text(MagicLinkStrings.backToLogin) }
            }
        }
    }
}
```

- [ ] **Step 3: Verify compile**

Run: `.\gradlew.bat :composeApp:compileAndroidMain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/magiclink/MagicLinkRequestScreen.kt composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/magiclink/MagicLinkExchangeScreen.kt
git commit -m "feat(magic-link): request + exchange screens"
```

---

## Task 6 — Routes + Koin factories + Login entry point

**Files:** Modify `navigation/Routes.kt`, `di/AppModule.kt`, `ui/login/LoginScreen.kt`, `ui/login/LoginStrings.kt`.

**Interfaces:**
- Produces: `@Serializable object MagicLinkRequestRoute`; `@Serializable data class MagicLinkExchangeRoute(val token: String)`; Koin factories for both ViewModels; `LoginScreen(..., onMagicLink: () -> Unit)`.

- [ ] **Step 1: Extend `Routes.kt`**

```kotlin
@Serializable
object MagicLinkRequestRoute

@Serializable
data class MagicLinkExchangeRoute(val token: String)
```

- [ ] **Step 2: Add factories in `AppModule.kt`** (imports + inside `module { }`)

```kotlin
import com.foxugly.trainingmanager_app.ui.magiclink.MagicLinkExchangeViewModel
import com.foxugly.trainingmanager_app.ui.magiclink.MagicLinkRequestViewModel
// …
    factory { MagicLinkRequestViewModel(get()) }
    factory { MagicLinkExchangeViewModel(get()) }
```

- [ ] **Step 3: Add a string in `LoginStrings.kt`**

```kotlin
    const val signInByEmailLink = "Connexion par lien e-mail"
```

- [ ] **Step 4: Add `onMagicLink` to `LoginScreen.kt`** — change the signature and append a text button after the login Button.

Change signature:
```kotlin
fun LoginScreen(viewModel: LoginViewModel, onLoginSuccess: () -> Unit, onMagicLink: () -> Unit) {
```
Add import `androidx.compose.material3.TextButton`, and after the login `Button { … }` block add:
```kotlin
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onMagicLink, modifier = Modifier.fillMaxWidth()) {
            Text(LoginStrings.signInByEmailLink)
        }
```

- [ ] **Step 5: Verify compile**

Run: `.\gradlew.bat :composeApp:compileAndroidMain`
Expected: `BUILD SUCCESSFUL`. (App.kt's `LoginScreen(...)` call will now be missing `onMagicLink` — it is updated in Task 7; this step may fail to compile App.kt. If so, proceed to Task 7 which fixes the call site, then compile. To keep this task self-contained, do Step 6 commit after Task 7's App.kt edit if needed.)

> NOTE: Steps 4 and Task 7's App.kt edit are interdependent (adding a required param to `LoginScreen` breaks its existing call site in App.kt). Make the LoginScreen signature change here, then immediately do Task 7 Step 3 (App.kt) before compiling. Commit both together at Task 7 Step 6 if a clean separate compile isn't possible.

- [ ] **Step 6: Commit (routes + factories + strings + LoginScreen)**

```bash
git add composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/navigation/Routes.kt composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/di/AppModule.kt composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/login/LoginScreen.kt composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/ui/login/LoginStrings.kt
git commit -m "feat(magic-link): nav routes + Koin factories + login entry point"
```

---

## Task 7 — App.kt wiring + deep-link routing (compile + tests)

**Files:** Modify `App.kt`.

**Interfaces:**
- Consumes: all prior tasks. Produces: `App(authRepository, deepLink: DeepLinkTarget? = null, onDeepLinkConsumed: () -> Unit = {})` with magic-link routes + deep-link routing.

- [ ] **Step 1: Rewrite `App.kt`** — add deep-link params, the two magic-link routes, the Login `onMagicLink` wiring, and a `LaunchedEffect` that routes an incoming `DeepLinkTarget.MagicLinkExchange` to `MagicLinkExchangeRoute`.

```kotlin
package com.foxugly.trainingmanager_app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.navigation.DeepLinkTarget
import com.foxugly.trainingmanager_app.navigation.HomeRoute
import com.foxugly.trainingmanager_app.navigation.LoginRoute
import com.foxugly.trainingmanager_app.navigation.MagicLinkExchangeRoute
import com.foxugly.trainingmanager_app.navigation.MagicLinkRequestRoute
import com.foxugly.trainingmanager_app.navigation.StartupRoute
import com.foxugly.trainingmanager_app.navigation.startupRoute
import com.foxugly.trainingmanager_app.ui.home.HomePlaceholderScreen
import com.foxugly.trainingmanager_app.ui.login.LoginScreen
import com.foxugly.trainingmanager_app.ui.login.LoginViewModel
import com.foxugly.trainingmanager_app.ui.magiclink.MagicLinkExchangeScreen
import com.foxugly.trainingmanager_app.ui.magiclink.MagicLinkExchangeViewModel
import com.foxugly.trainingmanager_app.ui.magiclink.MagicLinkRequestScreen
import com.foxugly.trainingmanager_app.ui.magiclink.MagicLinkRequestViewModel
import com.foxugly.trainingmanager_app.ui.theme.TrainingManagerTheme
import org.koin.compose.koinInject

@Composable
fun App(
    authRepository: AuthRepository = koinInject(),
    deepLink: DeepLinkTarget? = null,
    onDeepLinkConsumed: () -> Unit = {},
) {
    var route by remember { mutableStateOf(StartupRoute.Loading) }
    LaunchedEffect(authRepository) {
        val hasRefresh = authRepository.hasRefreshToken()
        val refreshed = hasRefresh && authRepository.tryRefresh()
        route = startupRoute(hasRefresh, refreshed)
    }

    TrainingManagerTheme {
        when (route) {
            StartupRoute.Loading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            else -> {
                val navController = rememberNavController()
                val startDestination: Any =
                    if (route == StartupRoute.Authenticated) HomeRoute else LoginRoute

                // Route an incoming deep link once the graph is live.
                LaunchedEffect(deepLink) {
                    when (val d = deepLink) {
                        is DeepLinkTarget.MagicLinkExchange -> {
                            navController.navigate(MagicLinkExchangeRoute(d.token)) { launchSingleTop = true }
                            onDeepLinkConsumed()
                        }
                        null -> Unit
                    }
                }

                NavHost(navController = navController, startDestination = startDestination) {
                    composable<LoginRoute> {
                        val vm: LoginViewModel = koinInject()
                        LoginScreen(
                            viewModel = vm,
                            onLoginSuccess = {
                                navController.navigate(HomeRoute) {
                                    popUpTo<LoginRoute> { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onMagicLink = {
                                navController.navigate(MagicLinkRequestRoute) { launchSingleTop = true }
                            },
                        )
                    }
                    composable<MagicLinkRequestRoute> {
                        val vm: MagicLinkRequestViewModel = koinInject()
                        MagicLinkRequestScreen(viewModel = vm, onBack = { navController.popBackStack() })
                    }
                    composable<MagicLinkExchangeRoute> { entry ->
                        val args = entry.toRoute<MagicLinkExchangeRoute>()
                        val vm: MagicLinkExchangeViewModel = koinInject()
                        MagicLinkExchangeScreen(
                            viewModel = vm,
                            token = args.token,
                            onSuccess = {
                                navController.navigate(HomeRoute) {
                                    popUpTo<LoginRoute> { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onBackToLogin = {
                                navController.navigate(LoginRoute) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                    composable<HomeRoute> {
                        HomePlaceholderScreen(
                            authRepository = authRepository,
                            onLoggedOut = {
                                navController.navigate(LoginRoute) {
                                    popUpTo<HomeRoute> { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Run the full test suite + compile**

Run: `.\gradlew.bat :composeApp:testAndroidHostTest`
Expected: `BUILD SUCCESSFUL` — all S1a/S1b + new magic-link tests green; App.kt + LoginScreen compile together.

- [ ] **Step 3: Commit** (App.kt; plus LoginScreen/Routes/AppModule/LoginStrings if not already committed in Task 6)

```bash
git add composeApp/src/commonMain/kotlin/com/foxugly/trainingmanager_app/App.kt
git commit -m "feat(magic-link): wire nav graph + deep-link routing into App()"
```

---

## Task 8 — Android deep-link plumbing (assembleDebug)

**Files:** Modify `androidApp/src/main/AndroidManifest.xml`, `androidApp/src/main/kotlin/.../MainActivity.kt`.

**Interfaces:** Consumes `parseDeepLink`, `App(deepLink, onDeepLinkConsumed)`.

- [ ] **Step 1: Add `ACTION_VIEW` intent-filters to the `MainActivity` activity in `AndroidManifest.xml`** (after the existing LAUNCHER filter, inside `<activity>`).

```xml
            <!-- Custom scheme: works immediately (no hosted assetlinks needed) — dev + fallback. -->
            <intent-filter>
                <action android:name="android.intent.action.VIEW"/>
                <category android:name="android.intent.category.DEFAULT"/>
                <category android:name="android.intent.category.BROWSABLE"/>
                <data android:scheme="trainingmanager"/>
            </intent-filter>
            <!-- HTTPS App Links. autoVerify requires /.well-known/assetlinks.json hosted
                 on tm.foxugly.com containing this app's release signing SHA-256 (OPS prerequisite). -->
            <intent-filter android:autoVerify="true">
                <action android:name="android.intent.action.VIEW"/>
                <category android:name="android.intent.category.DEFAULT"/>
                <category android:name="android.intent.category.BROWSABLE"/>
                <data android:scheme="https" android:host="tm.foxugly.com" android:pathPrefix="/auth/magic-link/"/>
            </intent-filter>
```

- [ ] **Step 2: Wire `intent.data` into `App()` in `MainActivity.kt`** — add a `deepLink` state, read it in `onCreate` + `onNewIntent`, pass to `App(...)`. Add imports `android.content.Intent`, `androidx.compose.runtime.mutableStateOf`, `com.foxugly.trainingmanager_app.navigation.DeepLinkTarget`, `com.foxugly.trainingmanager_app.navigation.parseDeepLink`.

Add a field + override + change `setContent`:
```kotlin
    private val deepLink = mutableStateOf<DeepLinkTarget?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseDeepLink(intent.dataString)?.let { deepLink.value = it }
    }
```
In `onCreate`, before `setContent`:
```kotlin
        deepLink.value = parseDeepLink(intent?.dataString)
```
Change the `setContent { App() }` to:
```kotlin
        setContent {
            App(
                deepLink = deepLink.value,
                onDeepLinkConsumed = { deepLink.value = null },
            )
        }
```
(`App()`'s `authRepository` keeps its `koinInject()` default.)

- [ ] **Step 3: Assemble the APK**

Run: `.\gradlew.bat :androidApp:assembleDebug`
Expected: `BUILD SUCCESSFUL`; APK at `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

- [ ] **Step 4: (Optional manual) verify the deep link routes** — with an emulator/device:
`adb shell am start -W -a android.intent.action.VIEW -d "trainingmanager://app/auth/magic-link/TESTTOKEN" com.foxugly.trainingmanager_app`
Expected: app opens on the magic-link exchange screen and shows "Lien invalide" (TESTTOKEN is rejected by the backend). *(Not run in CI — documented for manual QA.)*

- [ ] **Step 5: Commit**

```bash
git add androidApp/src/main/AndroidManifest.xml androidApp/src/main/kotlin/com/foxugly/trainingmanager_app/MainActivity.kt
git commit -m "feat(deep-link): Android ACTION_VIEW intent-filters + MainActivity intent->App wiring"
```

---

## Task 9 — iOS deep-link plumbing (written, unverified) + ops prerequisite doc

**Files:** Modify `composeApp/src/iosMain/.../MainViewController.kt`, `iosApp/iosApp/ContentView.swift`, `iosApp/iosApp/Info.plist`; Create `docs/superpowers/specs/2026-06-29-deep-link-ops-prereq.md`.

> **iOS targets do not build on Windows** — this code is written to mirror Android and reviewed by inspection, NOT compiled/tested here. Verify on macOS later.

- [ ] **Step 1: Expose a deep-link sink from `MainViewController.kt`** — add a module-level holder + setter the Swift side can call, bridged into `App()`.

```kotlin
import androidx.compose.runtime.mutableStateOf
import com.foxugly.trainingmanager_app.navigation.DeepLinkTarget
import com.foxugly.trainingmanager_app.navigation.parseDeepLink

private val iosDeepLink = mutableStateOf<DeepLinkTarget?>(null)

/** Called from Swift `.onOpenURL` with the absolute URL string. */
fun handleDeepLink(uri: String) { parseDeepLink(uri)?.let { iosDeepLink.value = it } }
```
Then change the `App()` call inside `MainViewController()` to pass `deepLink = iosDeepLink.value, onDeepLinkConsumed = { iosDeepLink.value = null }`.

- [ ] **Step 2: Bridge `.onOpenURL` in `ContentView.swift`**

```swift
ComposeView()
    .ignoresSafeArea(.keyboard)
    .onOpenURL { url in
        MainViewControllerKt.handleDeepLink(uri: url.absoluteString)
    }
```

- [ ] **Step 3: Register the custom scheme in `Info.plist`** (add a `CFBundleURLTypes` array with scheme `trainingmanager`). Universal Links (associated-domains entitlement + AASA on `tm.foxugly.com`) are an ops prerequisite — note only.

- [ ] **Step 4: Write the ops prerequisite doc** `docs/superpowers/specs/2026-06-29-deep-link-ops-prereq.md` capturing: Android `assetlinks.json` (release SHA-256, package `com.foxugly.trainingmanager_app`) hosted at `https://tm.foxugly.com/.well-known/assetlinks.json`; iOS `apple-app-site-association` at `https://tm.foxugly.com/.well-known/apple-app-site-association` + associated-domains entitlement `applinks:tm.foxugly.com`; paths `/auth/magic-link/*` (+ future `/auth/confirm-email/*`, `/auth/reset-password/*`, `/invitation/*` for S1c-b/c). Until hosted, only the custom scheme triggers the app.

- [ ] **Step 5: Compile the Android side (sanity — iOS not built)**

Run: `.\gradlew.bat :composeApp:testAndroidHostTest`
Expected: `BUILD SUCCESSFUL` (iOS sources are not compiled on Windows; this confirms commonMain/androidMain still green).

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/iosMain/kotlin/com/foxugly/trainingmanager_app/MainViewController.kt iosApp/iosApp/ContentView.swift iosApp/iosApp/Info.plist docs/superpowers/specs/2026-06-29-deep-link-ops-prereq.md
git commit -m "feat(deep-link): iOS onOpenURL plumbing (unverified on Windows) + ops prerequisite doc"
```

---

## Self-Review

**1. Spec coverage (S1 §7 magic-link + §6 deep links).** Magic-link request (`POST auth/magic-link/request/`, always-200) = Tasks 1,2,4,5. Magic-link exchange (`POST auth/magic-link/exchange/`, 410/400 split, auto-login) = Tasks 1,2,4,5. Deep-link routing + bootstrap interplay = Tasks 3,7,8,9. Login entry point to magic-link = Task 6. Deferred (not gaps): email-confirm/reset-confirm (S1c-b), invitation (S1c-c), register/forgot-password (S1c-b, Turnstile). HTTPS App Links activation = ops prerequisite (Task 9 doc).

**2. Placeholder scan.** Every code step is complete; every run step has a command + expected output. Task 6 Step 5 documents the LoginScreen/App.kt interdependency explicitly (not a TODO). Task 8 Step 4 is an optional manual QA step (clearly marked, not CI). iOS (Task 9) is explicitly written-but-unverified per the project's Windows constraint.

**3. Type consistency.** `MagicLinkRequestBody(email)`/`MagicLinkExchangeBody(token)` (Task 1) consumed by `TrainingManagerApi.magicLinkRequest/Exchange` (Task 1) → `AuthRepository.requestMagicLink/exchangeMagicLink` (Task 2) → ViewModels (Task 4) → screens (Task 5) → routes/App (Tasks 6,7). `DeepLinkTarget.MagicLinkExchange(token)` (Task 3) produced by `parseDeepLink` (Task 3), consumed in `App` deep-link `LaunchedEffect` (Task 7) + `MainActivity` (Task 8) + iOS `handleDeepLink` (Task 9). `MagicLinkExchangeRoute(token)` (Task 6) navigated to in Task 7 and read via `toRoute()`. `exchangeMagicLink` persists access+refresh+remember mirroring `login()` (verified against S1a `AuthRepository`). `ApiException.statusCode` (S1a) drives 410-vs-400 in the exchange VM (Task 4). Consistent.
