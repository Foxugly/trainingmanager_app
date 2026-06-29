package com.foxugly.trainingmanager_app.data.api

import com.foxugly.trainingmanager_app.FakeTokenStore
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class InvitationApiTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun api(engine: MockEngine) =
        TrainingManagerApi(FakeTokenStore(), baseUrl = "https://test/api/v1/", engine = engine)

    @Test fun lookupDecodesValidateInvitation() = runTest {
        val api = api(MockEngine { respond("""{"email":"a@b.co","team_name":"Sharks","status":"pending","expires_at":"2026-12-01T00:00:00Z"}""", HttpStatusCode.OK, jsonHeader) })
        val inv = api.lookupInvitation("tok").getOrThrow()
        assertEquals("Sharks", inv.teamName)
        assertEquals("pending", inv.status)
    }

    @Test fun completeDecodesTokenPairFrom201() = runTest {
        val api = api(MockEngine { respond("""{"detail":"joined","email":"a@b.co","access":"a","refresh":"r"}""", HttpStatusCode.Created, jsonHeader) })
        assertEquals("a", api.completeInvitation("tok", CompleteInvitationBody("password1")).getOrThrow().access)
    }

    @Test fun lookupExpiredSurfaces410() = runTest {
        val api = api(MockEngine { respond("""{"code":"invitation_expired"}""", HttpStatusCode.Gone, jsonHeader) })
        assertEquals(410, (api.lookupInvitation("tok").exceptionOrNull() as ApiException).statusCode)
    }
}
