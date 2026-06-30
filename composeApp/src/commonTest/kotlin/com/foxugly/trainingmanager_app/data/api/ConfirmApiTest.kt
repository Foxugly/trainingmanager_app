package com.foxugly.trainingmanager_app.data.api

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.api.generated.models.EmailConfirmRequest
import com.foxugly.trainingmanager_app.api.generated.models.PasswordResetConfirmRequest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ConfirmApiTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun api(engine: MockEngine) =
        TrainingManagerApi(FakeTokenStore(), baseUrl = "https://test/api/v1/", engine = engine)

    @Test fun confirmEmailDecodesTokenPairIgnoringUser() = runTest {
        val api = api(MockEngine { respond("""{"access":"a","refresh":"r","user":{"id":1,"email":"x@y.z"}}""", HttpStatusCode.OK, jsonHeader) })
        assertEquals("a", api.confirmEmail(EmailConfirmRequest("k")).getOrThrow().access)
    }

    @Test fun resetConfirmDecodesTokenPair() = runTest {
        val api = api(MockEngine { respond("""{"access":"a2","refresh":"r2","user":{"id":1,"email":"x@y.z"}}""", HttpStatusCode.OK, jsonHeader) })
        assertEquals("r2", api.confirmPasswordReset(PasswordResetConfirmRequest("k", "password1")).getOrThrow().refresh)
    }

    @Test fun confirmEmailInvalidSurfaces400() = runTest {
        val api = api(MockEngine { respond("""{"code":"invalid_or_expired_token"}""", HttpStatusCode.BadRequest, jsonHeader) })
        assertEquals(400, (api.confirmEmail(EmailConfirmRequest("k")).exceptionOrNull() as ApiException).statusCode)
    }
}
