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
import kotlin.test.assertTrue

private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")

class AuthRepositoryTest {

    @Test
    fun loginStoresTokensRememberAndReturnsProfile() = runTest {
        val store = FakeTokenStore()
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("/auth/token/") ->
                    respond("""{"access":"acc","refresh":"ref"}""", HttpStatusCode.OK, jsonHeader)
                request.url.encodedPath.endsWith("/me/") ->
                    respond("""{"id":3,"email":"a@b.co","language":"fr"}""", HttpStatusCode.OK, jsonHeader)
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val api = TrainingManagerApi(store, baseUrl = "https://test/api/v1/", engine = engine)
        val repo = AuthRepository(api, store)

        val result = repo.login("a@b.co", "pw", remember = true)

        assertTrue(result.isSuccess, "${result.exceptionOrNull()}")
        assertEquals(3, result.getOrNull()?.id)
        assertEquals("acc", store.getAccessToken())
        assertEquals("ref", store.getRefreshToken())
        assertTrue(store.getRemember())
        api.close()
    }

    @Test
    fun tryRefreshPersistsRotatedTokenOnSuccess() = runTest {
        val store = FakeTokenStore(refresh = "r1")
        val engine = MockEngine {
            respond("""{"access":"a2","refresh":"r2"}""", HttpStatusCode.OK, jsonHeader)
        }
        val api = TrainingManagerApi(store, baseUrl = "https://test/api/v1/", engine = engine)
        val repo = AuthRepository(api, store)

        assertTrue(repo.tryRefresh())
        assertEquals("a2", store.getAccessToken())
        assertEquals("r2", store.getRefreshToken())
        api.close()
    }

    @Test
    fun tryRefreshClearsAndReturnsFalseOnFailure() = runTest {
        val store = FakeTokenStore(refresh = "bad")
        val engine = MockEngine { respond("", HttpStatusCode.Unauthorized) }
        val api = TrainingManagerApi(store, baseUrl = "https://test/api/v1/", engine = engine)
        val repo = AuthRepository(api, store)

        assertFalse(repo.tryRefresh())
        assertTrue(store.cleared)
        api.close()
    }

    @Test
    fun hasRefreshTokenReflectsStore() = runTest {
        assertFalse(AuthRepository(
            TrainingManagerApi(FakeTokenStore(), baseUrl = "https://test/api/v1/", engine = MockEngine { respond("", HttpStatusCode.OK) }),
            FakeTokenStore(),
        ).hasRefreshToken())
    }
}
