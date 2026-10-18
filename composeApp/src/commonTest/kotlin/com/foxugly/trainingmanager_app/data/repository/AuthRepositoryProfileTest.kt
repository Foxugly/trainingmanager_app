package com.foxugly.trainingmanager_app.data.repository

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.meJson
import com.foxugly.trainingmanager_app.api.generated.models.PatchedMeRequest
import com.foxugly.trainingmanager_app.data.api.TrainingManagerApi
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthRepositoryProfileTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun repo(engine: MockEngine, store: FakeTokenStore = FakeTokenStore(access = "tok")): AuthRepository {
        return AuthRepository(TrainingManagerApi(store, baseUrl = "https://test/api/v1/", engine = engine), store)
    }

    @Test fun updateProfileReturnsUpdatedProfile() = runTest {
        val r = repo(MockEngine { respond(meJson(lastName = "Lee"), HttpStatusCode.OK, jsonHeader) })
        assertEquals("Lee", r.updateProfile(PatchedMeRequest(lastName = "Lee")).getOrThrow().lastName)
    }

    @Test fun changePasswordStoresTheNewTokenPair() = runTest {
        // The server revokes every previous refresh token: the session must
        // carry on with the pair it returns.
        val store = FakeTokenStore(access = "a1", refresh = "r1")
        val r = repo(MockEngine { respond("""{"detail":"ok","access":"a2","refresh":"r2"}""", HttpStatusCode.OK, jsonHeader) }, store)
        assertTrue(r.changePassword("old12345", "new12345").isSuccess)
        assertEquals("a2", store.getAccessToken())
        assertEquals("r2", store.getRefreshToken())
    }

    @Test fun changePasswordFailureKeepsTheTokens() = runTest {
        val store = FakeTokenStore(access = "a1", refresh = "r1")
        val r = repo(MockEngine { respond("""{"code":"current_password_invalid"}""", HttpStatusCode.BadRequest, jsonHeader) }, store)
        assertTrue(r.changePassword("bad", "new12345").isFailure)
        assertEquals("a1", store.getAccessToken())
        assertEquals("r1", store.getRefreshToken())
    }
}
