package com.foxugly.trainingmanager_app.data.api

import com.foxugly.trainingmanager_app.FakeTokenStore
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")

class TrainingManagerApiAuthTest {

    @Test
    fun replaysOriginalPostWithBodyAndFreshTokenAfter401() = runTest {
        // Uses login() — a genuine POST with a JSON body — so the test proves that
        // the replayed request preserves both the verb (POST) AND the request body.
        // login() is an AUTH_PATH so no Bearer is added; we force the first call to 401
        // by call-count so the refresh→replay path is exercised without relying on the
        // Authorization header (which is intentionally omitted for auth/token/ by design).
        val store = FakeTokenStore(access = "stale", refresh = "refresh-1")
        val loginRequest = TokenObtainRequest(email = "a@b.co", password = "secret")
        var loginCallCount = 0
        val seen = mutableListOf<Triple<String, HttpMethod, String>>()
        val engine = MockEngine { request ->
            val body = String(request.body.toByteArray())
            seen += Triple(request.url.encodedPath, request.method, body)
            when {
                request.url.encodedPath.endsWith("/auth/token/refresh/") ->
                    respond("""{"access":"fresh","refresh":"refresh-2"}""", HttpStatusCode.OK, jsonHeader)
                request.url.encodedPath.endsWith("/auth/token/") && ++loginCallCount == 1 ->
                    respond("", HttpStatusCode.Unauthorized)
                else ->
                    respond("""{"access":"new-access","refresh":"new-refresh"}""", HttpStatusCode.OK, jsonHeader)
            }
        }
        val api = TrainingManagerApi(store, baseUrl = "https://test/api/v1/", engine = engine)

        val result = api.login(loginRequest)

        assertTrue(result.isSuccess, "login should succeed after refresh: ${result.exceptionOrNull()}")
        assertEquals("fresh", store.getAccessToken())
        assertEquals("refresh-2", store.getRefreshToken(), "rotated refresh token must be persisted")
        // Filter out the /refresh/ call; only the two login attempts remain.
        val loginCalls = seen.filter { it.first.endsWith("/auth/token/") }
        assertEquals(2, loginCalls.size, "auth/token/ should be attempted then replayed")
        assertTrue(loginCalls.all { it.second == HttpMethod.Post }, "replay must keep POST verb")
        // Both attempts must carry the original body — this is the core body-replay guarantee.
        assertTrue(
            loginCalls.all { it.third.contains("a@b.co") },
            "replayed POST must preserve the original request body; got ${loginCalls.map { it.third }}",
        )
        api.close()
    }

    @Test
    fun signalsAuthFailureAndClearsTokensWhenRefreshFails() = runTest {
        val store = FakeTokenStore(access = "stale", refresh = "bad")
        var authFailed = false
        val engine = MockEngine { respond("", HttpStatusCode.Unauthorized) }
        val api = TrainingManagerApi(store, baseUrl = "https://test/api/v1/", engine = engine)
        api.onAuthFailure = { authFailed = true }

        val result = api.getMe()

        assertTrue(result.isFailure)
        assertTrue(authFailed, "onAuthFailure must fire when refresh fails")
        assertTrue(store.cleared, "tokens must be cleared on refresh failure")
        api.close()
    }

    @Test
    fun surfacesNetworkExceptionOnTransportFailure() = runTest {
        val store = FakeTokenStore(access = "tok", refresh = "r")
        val engine = MockEngine { throw IOException("connection reset") }
        val api = TrainingManagerApi(store, baseUrl = "https://test/api/v1/", engine = engine)

        val result = api.getMe()

        assertTrue(result.isFailure)
        assertTrue(
            result.exceptionOrNull() is NetworkException,
            "transport failure must surface as NetworkException, was ${result.exceptionOrNull()}",
        )
        api.close()
    }

    @Test
    fun mapsHttpErrorToApiException() = runTest {
        val store = FakeTokenStore(access = "tok", refresh = "r")
        val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        val api = TrainingManagerApi(store, baseUrl = "https://test/api/v1/", engine = engine)

        val result = api.getMe()

        assertTrue(result.exceptionOrNull() is ApiException)
        api.close()
    }
}
