package com.foxugly.trainingmanager_app.data.api

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.i18n.LanguageProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit-tests for [AuthInterceptor] and [LanguageInterceptor] against MockEngine.
 *
 * Behaviors covered:
 *  1. Bearer + Accept-Language attached on a normal (non-auth-path) call.
 *  2. Bearer omitted on AUTH_PATHS; Accept-Language still sent.
 *  3. [AuthInterceptor.refreshIfNeeded] updates the token on 401, then a replay
 *     with the new token succeeds — exercising exactly one refresh call.
 *  4. Concurrent calls to [AuthInterceptor.refreshIfNeeded] with the same stale
 *     token hit the refresh endpoint exactly once (single-flight via Mutex guard).
 *  5. Transport failure during refresh → false returned, tokens cleared,
 *     onAuthFailure invoked (NetworkException wrapping for primary calls is in
 *     Task 6 / TrainingManagerApi.apiCall).
 *  6. Concurrent callers whose refresh returns a failure (e.g. 401) hit the
 *     refresh endpoint exactly once AND fire onAuthFailure exactly once —
 *     locking in the single-fire fix (fix #2 review finding).
 */
class InterceptorTest {

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Minimal client for bearer/language header tests (no ContentNegotiation needed). */
    private fun headerClient(
        store: FakeTokenStore,
        provider: LanguageProvider,
        captured: MutableMap<String, String?>,
    ): HttpClient {
        val auth = AuthInterceptor(store)
        val lang = LanguageInterceptor(provider)
        val engine = MockEngine { request ->
            captured["auth"] = request.headers[HttpHeaders.Authorization]
            captured["lang"] = request.headers[HttpHeaders.AcceptLanguage]
            respond("{}", HttpStatusCode.OK)
        }
        return HttpClient(engine) {
            install(auth.plugin)
            install(lang.plugin)
        }
    }

    /**
     * Client for refresh tests: adds ContentNegotiation (needed by body<TokenRefresh>())
     * and a defaultRequest base URL so `client.post("auth/token/refresh/")` resolves
     * to an absolute URL that the MockEngine can match on encodedPath.
     */
    private fun refreshClient(
        interceptor: AuthInterceptor,
        engine: MockEngine,
    ): HttpClient = HttpClient(engine) {
        install(interceptor.plugin)
        install(ContentNegotiation) { json() }
        defaultRequest {
            url("https://test/api/v1/")
        }
    }

    private val jsonCt = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    // ── Test 1 ───────────────────────────────────────────────────────────────

    @Test
    fun attachesBearerAndAcceptLanguageOnNormalCall() = runTest {
        val captured = mutableMapOf<String, String?>()
        val client = headerClient(
            FakeTokenStore(access = "tok"),
            LanguageProvider("nl"),
            captured,
        )
        client.get("https://test/api/v1/me/")
        assertEquals("Bearer tok", captured["auth"])
        assertEquals("nl", captured["lang"])
        client.close()
    }

    // ── Test 2 ───────────────────────────────────────────────────────────────

    @Test
    fun doesNotAttachBearerOnAuthPaths() = runTest {
        val captured = mutableMapOf<String, String?>()
        val client = headerClient(
            FakeTokenStore(access = "tok"),
            LanguageProvider("fr"),
            captured,
        )
        client.get("https://test/api/v1/auth/token/")
        assertNull(captured["auth"])
        // Accept-Language is still sent on auth calls so backend errors localize.
        assertEquals("fr", captured["lang"])
        client.close()
    }

    // ── Test 3 ───────────────────────────────────────────────────────────────

    /**
     * Shows the 401 → refreshIfNeeded → replay cycle that Task 6 / apiCall will
     * drive. Here we call refreshIfNeeded directly to verify: token updated, one
     * HTTP refresh, replay returns 200 with new bearer.
     */
    @Test
    fun refreshIfNeededUpdatesTokenThenReplaysSucceed() = runTest {
        var refreshCalls = 0
        val store = FakeTokenStore(access = "old-tok", refresh = "ref-tok")
        val authInterceptor = AuthInterceptor(store)

        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.contains("auth/token/refresh/") -> {
                    refreshCalls++
                    respond(
                        content = """{"access":"new-tok","refresh":"new-ref"}""",
                        status = HttpStatusCode.OK,
                        headers = jsonCt,
                    )
                }
                request.headers[HttpHeaders.Authorization] == "Bearer old-tok" ->
                    respond("{}", HttpStatusCode.Unauthorized)
                else ->
                    respond("{}", HttpStatusCode.OK)
            }
        }

        val client = refreshClient(authInterceptor, engine)

        // Primary call — plugin attaches old-tok → server returns 401.
        val stale = store.getAccessToken()
        val first = client.get("https://test/api/v1/me/")
        assertEquals(HttpStatusCode.Unauthorized, first.status)

        // Refresh (mirrors what apiCall will do in Task 6).
        val refreshed = authInterceptor.refreshIfNeeded(client, stale)
        assertTrue(refreshed, "refreshIfNeeded should return true")
        assertEquals("new-tok", store.getAccessToken())
        assertEquals("new-ref", store.getRefreshToken())
        assertEquals(1, refreshCalls, "Refresh endpoint must be called exactly once")

        // Replay — plugin now attaches new-tok → server returns 200.
        val replay = client.get("https://test/api/v1/me/")
        assertEquals(HttpStatusCode.OK, replay.status)

        client.close()
    }

    // ── Test 4 ───────────────────────────────────────────────────────────────

    /**
     * Single-flight guarantee: N concurrent refreshIfNeeded calls with the same
     * stale token must result in exactly one HTTP call to the refresh endpoint.
     * The Mutex serializes callers; the guard `current != staleAccessToken` causes
     * callers 2-N to reuse the token that caller 1 already stored.
     */
    @Test
    fun concurrentRefreshesHitEndpointOnlyOnce() = runTest {
        var refreshCalls = 0
        val store = FakeTokenStore(access = "old-tok", refresh = "ref-tok")
        val authInterceptor = AuthInterceptor(store)

        val engine = MockEngine { request ->
            if (request.url.encodedPath.contains("auth/token/refresh/")) {
                refreshCalls++
                respond(
                    content = """{"access":"new-tok","refresh":"new-ref"}""",
                    status = HttpStatusCode.OK,
                    headers = jsonCt,
                )
            } else {
                respond("{}", HttpStatusCode.OK)
            }
        }

        val client = refreshClient(authInterceptor, engine)

        val stale = "old-tok"
        // 5 async coroutines all presenting the same stale token — only one should
        // actually hit the refresh endpoint; the other four reuse the new token.
        val results = (1..5).map {
            async { authInterceptor.refreshIfNeeded(client, stale) }
        }.awaitAll()

        assertTrue(results.all { it }, "Every concurrent caller should report success")
        assertEquals(1, refreshCalls, "Single-flight: refresh endpoint must be hit exactly once")
        assertEquals("new-tok", store.getAccessToken())

        client.close()
    }

    // ── Test 5 ───────────────────────────────────────────────────────────────

    /**
     * Transport failure during token refresh: AuthInterceptor catches the exception,
     * clears auth tokens, and invokes onAuthFailure. Returns false so the caller
     * (apiCall in Task 6) can surface a NetworkException to the UI.
     */
    @Test
    fun transportFailureDuringRefreshClearsTokensAndSignalsAuthFailure() = runTest {
        var authFailureCalled = false
        val store = FakeTokenStore(access = "old-tok", refresh = "ref-tok")
        val authInterceptor = AuthInterceptor(store) { authFailureCalled = true }

        val engine = MockEngine { _ ->
            throw Exception("simulated transport failure (offline/DNS)")
        }

        val client = refreshClient(authInterceptor, engine)

        val ok = authInterceptor.refreshIfNeeded(client, "old-tok")

        assertFalse(ok, "refreshIfNeeded should return false on transport failure")
        assertTrue(store.cleared, "Tokens must be cleared after transport failure")
        assertTrue(authFailureCalled, "onAuthFailure must be invoked on transport failure")

        client.close()
    }

    // ── Test 6 ───────────────────────────────────────────────────────────────

    /**
     * Single-fire guarantee under concurrent refresh failure: N concurrent
     * [AuthInterceptor.refreshIfNeeded] calls where the refresh endpoint returns
     * a non-OK status must result in exactly one HTTP call to the refresh endpoint
     * AND exactly one invocation of [AuthInterceptor.onAuthFailure].
     *
     * Without fix #2, callers 2-N acquire the mutex, see `current == null`
     * (tokens were cleared by caller 1), fall through to the missing-refresh-token
     * path, and each fire onAuthFailure — causing N stacked navigation events
     * (e.g. N repeated "session expired" dialogs or back-stack pops).
     */
    @Test
    fun concurrentRefreshFailureFiresOnAuthFailureOnlyOnce() = runTest {
        var refreshCalls = 0
        var authFailureCount = 0
        val store = FakeTokenStore(access = "old-tok", refresh = "ref-tok")
        val authInterceptor = AuthInterceptor(store) { authFailureCount++ }

        val engine = MockEngine { request ->
            if (request.url.encodedPath.contains("auth/token/refresh/")) {
                refreshCalls++
                respond(
                    content = """{"detail":"Token is invalid or expired"}""",
                    status = HttpStatusCode.Unauthorized,
                    headers = jsonCt,
                )
            } else {
                respond("{}", HttpStatusCode.OK)
            }
        }

        val client = refreshClient(authInterceptor, engine)

        val stale = "old-tok"
        // 5 async coroutines all presenting the same stale token. Only the first
        // caller to acquire refreshMutex hits the endpoint and fires onAuthFailure;
        // callers 2-5 see current == null (tokens cleared) and return false silently.
        val results = (1..5).map {
            async { authInterceptor.refreshIfNeeded(client, stale) }
        }.awaitAll()

        assertFalse(results.any { it }, "Every concurrent caller should report failure")
        assertEquals(1, refreshCalls, "Single-flight: refresh endpoint must be hit exactly once")
        assertEquals(1, authFailureCount, "onAuthFailure must be invoked exactly once")

        client.close()
    }
}
