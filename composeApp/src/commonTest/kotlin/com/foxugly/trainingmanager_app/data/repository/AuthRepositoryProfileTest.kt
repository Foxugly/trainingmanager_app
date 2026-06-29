package com.foxugly.trainingmanager_app.data.repository

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.meJson
import com.foxugly.trainingmanager_app.data.api.PatchMeBody
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
    private fun repo(engine: MockEngine): AuthRepository {
        val store = FakeTokenStore(access = "tok")
        return AuthRepository(TrainingManagerApi(store, baseUrl = "https://test/api/v1/", engine = engine), store)
    }

    @Test fun updateProfileReturnsUpdatedProfile() = runTest {
        val r = repo(MockEngine { respond(meJson(lastName = "Lee"), HttpStatusCode.OK, jsonHeader) })
        assertEquals("Lee", r.updateProfile(PatchMeBody(lastName = "Lee")).getOrThrow().lastName)
    }

    @Test fun changePasswordSuccess() = runTest {
        val r = repo(MockEngine { respond("""{"detail":"ok"}""", HttpStatusCode.OK, jsonHeader) })
        assertTrue(r.changePassword("old12345", "new12345").isSuccess)
    }

    @Test fun changePasswordFailureSurfaces() = runTest {
        val r = repo(MockEngine { respond("""{"code":"current_password_invalid"}""", HttpStatusCode.BadRequest, jsonHeader) })
        assertTrue(r.changePassword("bad", "new12345").isFailure)
    }
}
