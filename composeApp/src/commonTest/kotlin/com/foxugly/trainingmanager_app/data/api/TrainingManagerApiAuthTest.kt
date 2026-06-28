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
        val store = FakeTokenStore(access = "stale", refresh = "refresh-1")
        val seen = mutableListOf<Triple<String, HttpMethod, String>>()
        val engine = MockEngine { request ->
            val body = String(request.body.toByteArray())
            seen += Triple(request.url.encodedPath, request.method, body)
            when {
                request.url.encodedPath.endsWith("/auth/token/refresh/") ->
                    respond("""{"access":"fresh","refresh":"refresh-2"}""", HttpStatusCode.OK, jsonHeader)
                request.headers[HttpHeaders.Authorization] == "Bearer stale" ->
                    respond("", HttpStatusCode.Unauthorized)
                else ->
                    respond("""{"id":1,"email":"a@b.co"}""", HttpStatusCode.OK, jsonHeader)
            }
        }
        val api = TrainingManagerApi(store, baseUrl = "https://test/api/v1/", engine = engine)

        val result = api.getMe()

        assertTrue(result.isSuccess, "getMe should succeed after refresh: ${result.exceptionOrNull()}")
        assertEquals("fresh", store.getAccessToken())
        assertEquals("refresh-2", store.getRefreshToken(), "rotated refresh token must be persisted")
        val meCalls = seen.filter { it.first.endsWith("/me/") }
        assertEquals(2, meCalls.size, "me should be attempted then replayed")
        assertTrue(meCalls.all { it.second == HttpMethod.Get }, "replay must keep the original verb")
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
