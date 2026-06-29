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

class AuthRepositoryInvitationTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun repo(store: FakeTokenStore, engine: MockEngine) =
        AuthRepository(TrainingManagerApi(store, baseUrl = "https://test/api/v1/", engine = engine), store)

    @Test fun acceptStoresTokensAndReturnsProfile() = runTest {
        val store = FakeTokenStore()
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.contains("invitations/lookup/") ->
                    respond("""{"detail":"joined","email":"a@b.co","access":"a","refresh":"r"}""", HttpStatusCode.Created, jsonHeader)
                else -> respond("""{"id":7,"email":"a@b.co"}""", HttpStatusCode.OK, jsonHeader)
            }
        }
        val p = repo(store, engine).acceptInvitation("tok", "password1").getOrThrow()
        assertEquals(7, p.id)
        assertEquals("a", store.getAccessToken())
        assertTrue(store.getRemember())
    }

    @Test fun acceptFailureLeavesTokensUntouched() = runTest {
        val store = FakeTokenStore()
        val engine = MockEngine { respond("""{"code":"email_taken"}""", HttpStatusCode.Conflict, jsonHeader) }
        assertTrue(repo(store, engine).acceptInvitation("tok", "password1").isFailure)
        assertNull(store.getAccessToken())
    }
}
