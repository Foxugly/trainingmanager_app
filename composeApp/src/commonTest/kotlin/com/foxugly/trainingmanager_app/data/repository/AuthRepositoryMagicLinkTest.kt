package com.foxugly.trainingmanager_app.data.repository

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.meJson
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
                    respond(meJson(id = 9), HttpStatusCode.OK, jsonHeader)
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
