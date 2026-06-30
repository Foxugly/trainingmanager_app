package com.foxugly.trainingmanager_app.data.api

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.api.generated.models.MagicLinkExchangeRequestRequest
import com.foxugly.trainingmanager_app.api.generated.models.MagicLinkRequestRequest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MagicLinkApiTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun api(engine: MockEngine) =
        TrainingManagerApi(FakeTokenStore(), baseUrl = "https://test/api/v1/", engine = engine)

    @Test fun requestReturnsSuccessOnAlways200() = runTest {
        val api = api(MockEngine { respond("""{"detail":"ok"}""", HttpStatusCode.OK, jsonHeader) })
        assertTrue(api.magicLinkRequest(MagicLinkRequestRequest("a@b.co")).isSuccess)
    }

    @Test fun exchangeReturnsTokenPairOn200() = runTest {
        val api = api(MockEngine { respond("""{"access":"acc","refresh":"ref"}""", HttpStatusCode.OK, jsonHeader) })
        val pair = api.magicLinkExchange(MagicLinkExchangeRequestRequest("tok")).getOrThrow()
        assertEquals("acc", pair.access)
        assertEquals("ref", pair.refresh)
    }

    @Test fun exchangeExpiredSurfaces410() = runTest {
        val api = api(MockEngine { respond("""{"detail":"token_expired"}""", HttpStatusCode.Gone, jsonHeader) })
        val err = api.magicLinkExchange(MagicLinkExchangeRequestRequest("tok")).exceptionOrNull()
        assertEquals(410, (err as ApiException).statusCode)
    }

    @Test fun exchangeInvalidSurfaces400() = runTest {
        val api = api(MockEngine { respond("""{"detail":"token_invalid"}""", HttpStatusCode.BadRequest, jsonHeader) })
        val err = api.magicLinkExchange(MagicLinkExchangeRequestRequest("tok")).exceptionOrNull()
        assertEquals(400, (err as ApiException).statusCode)
    }
}
