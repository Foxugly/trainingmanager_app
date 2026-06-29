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
