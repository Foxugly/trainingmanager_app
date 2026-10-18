package com.foxugly.trainingmanager_app.ui.magiclink

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.meJson
import com.foxugly.trainingmanager_app.data.api.TrainingManagerApi
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
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

class MagicLinkExchangeViewModelTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun vm(engine: MockEngine): MagicLinkExchangeViewModel {
        val store = FakeTokenStore()
        return MagicLinkExchangeViewModel(AuthRepository(TrainingManagerApi(store, baseUrl = "https://test/api/v1/", engine = engine), store))
    }

    @Test fun successInvokesOnSuccess() = runTest {
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("magic-link/exchange/") ->
                    respond("""{"access":"a","refresh":"r"}""", HttpStatusCode.OK, jsonHeader)
                else -> respond(meJson(), HttpStatusCode.OK, jsonHeader)
            }
        }
        val sut = vm(engine)
        var ok = false
        sut.exchange("tok")
        // Never signs in silently: waits for the user, showing the target account.
        assertEquals(MagicLinkExchangeViewModel.ExchangeState.AwaitingConfirmation, sut.state)
        assertEquals("a@b.co", sut.targetEmail)
        assertNull(sut.currentEmail)
        assertFalse(ok)
        sut.confirm { ok = true }
        assertTrue(ok)
        assertEquals(MagicLinkExchangeViewModel.ExchangeState.Success, sut.state)
    }

    @Test fun cancelKeepsTheCurrentSession() = runTest {
        val store = FakeTokenStore(access = "old-acc", refresh = "old-ref")
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("magic-link/exchange/") ->
                    respond("""{"access":"a","refresh":"r"}""", HttpStatusCode.OK, jsonHeader)
                request.headers[HttpHeaders.Authorization] == "Bearer old-acc" ->
                    respond(meJson(email = "me@x.co"), HttpStatusCode.OK, jsonHeader)
                else -> respond(meJson(email = "attacker@x.co"), HttpStatusCode.OK, jsonHeader)
            }
        }
        val sut = MagicLinkExchangeViewModel(AuthRepository(TrainingManagerApi(store, baseUrl = "https://test/api/v1/", engine = engine), store))
        sut.exchange("tok")
        assertEquals(MagicLinkExchangeViewModel.ExchangeState.AwaitingConfirmation, sut.state)
        assertEquals("attacker@x.co", sut.targetEmail)
        assertEquals("me@x.co", sut.currentEmail)
        sut.cancel()
        var ok = false
        sut.confirm { ok = true } // no pending login any more: no-op
        assertFalse(ok)
        assertEquals("old-acc", store.getAccessToken())
        assertEquals("old-ref", store.getRefreshToken())
    }

    @Test fun expiredOn410() = runTest {
        val sut = vm(MockEngine { respond("""{"detail":"token_expired"}""", HttpStatusCode.Gone, jsonHeader) })
        sut.exchange("tok")
        assertEquals(MagicLinkExchangeViewModel.ExchangeState.Expired, sut.state)
    }

    @Test fun invalidOn400() = runTest {
        val sut = vm(MockEngine { respond("""{"detail":"token_invalid"}""", HttpStatusCode.BadRequest, jsonHeader) })
        sut.exchange("tok")
        assertEquals(MagicLinkExchangeViewModel.ExchangeState.Invalid, sut.state)
    }
}
