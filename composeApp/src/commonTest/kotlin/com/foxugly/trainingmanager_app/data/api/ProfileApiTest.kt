package com.foxugly.trainingmanager_app.data.api

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.api.generated.models.LanguageEnum
import com.foxugly.trainingmanager_app.api.generated.models.PasswordChangeRequest
import com.foxugly.trainingmanager_app.api.generated.models.PatchedMeRequest
import com.foxugly.trainingmanager_app.meJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileApiTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun api(engine: MockEngine) =
        TrainingManagerApi(FakeTokenStore(access = "tok"), baseUrl = "https://test/api/v1/", engine = engine)

    @Test fun patchMeDecodesUpdatedProfile() = runTest {
        val api = api(MockEngine { respond(meJson(firstName = "Ann", language = "nl"), HttpStatusCode.OK, jsonHeader) })
        val p = api.patchMe(PatchedMeRequest(firstName = "Ann", language = LanguageEnum.NL)).getOrThrow()
        assertEquals("Ann", p.firstName)
        assertEquals("nl", p.language?.value)
    }

    @Test fun changePasswordSuccessOn200() = runTest {
        val api = api(MockEngine { respond("""{"detail":"ok"}""", HttpStatusCode.OK, jsonHeader) })
        assertTrue(api.changePassword(PasswordChangeRequest("old12345", "new12345")).isSuccess)
    }

    @Test fun changePasswordSurfaces400() = runTest {
        val api = api(MockEngine { respond("""{"code":"current_password_invalid"}""", HttpStatusCode.BadRequest, jsonHeader) })
        assertEquals(400, (api.changePassword(PasswordChangeRequest("bad", "new12345")).exceptionOrNull() as ApiException).statusCode)
    }
}
