package com.foxugly.trainingmanager_app.ui.magiclink

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.data.api.TrainingManagerApi
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.StringsFr
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MagicLinkRequestViewModelTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun vm(engine: MockEngine): MagicLinkRequestViewModel {
        val store = FakeTokenStore()
        return MagicLinkRequestViewModel(AuthRepository(TrainingManagerApi(store, baseUrl = "https://test/api/v1/", engine = engine), store))
    }

    @Test fun cannotSubmitWithoutTurnstileToken() = runTest {
        val sut = vm(MockEngine { respond("""{"detail":"ok"}""", HttpStatusCode.OK, jsonHeader) })
        sut.email = "a@b.co"
        assertFalse(sut.canSubmit)
        sut.onTurnstileToken("tok")
        assertTrue(sut.canSubmit)
    }

    @Test fun submitWithoutTokenDoesNothing() = runTest {
        var calls = 0
        val sut = vm(MockEngine { calls++; respond("""{"detail":"ok"}""", HttpStatusCode.OK, jsonHeader) })
        sut.email = "a@b.co"
        sut.submit()
        assertEquals(0, calls)
        assertFalse(sut.sent)
    }

    @Test fun submitSendsTurnstileToken() = runTest {
        var body = ""
        val sut = vm(MockEngine { request ->
            body = request.body.toByteArray().decodeToString()
            respond("""{"detail":"ok"}""", HttpStatusCode.OK, jsonHeader)
        })
        sut.email = "a@b.co"; sut.onTurnstileToken("tok")
        sut.submit()
        assertTrue(body.contains("\"turnstile_token\":\"tok\""), body)
    }

    @Test fun submitSetsSentOnSuccess() = runTest {
        val sut = vm(MockEngine { respond("""{"detail":"ok"}""", HttpStatusCode.OK, jsonHeader) })
        sut.email = "a@b.co"; sut.onTurnstileToken("tok")
        sut.submit()
        assertTrue(sut.sent)
        assertFalse(sut.isLoading)
        assertNull(sut.error)
    }

    @Test fun submitSetsRateLimitedOn429AndResetsToken() = runTest {
        val sut = vm(MockEngine { respond("""{"detail":"x"}""", HttpStatusCode.TooManyRequests, jsonHeader) })
        sut.email = "a@b.co"; sut.onTurnstileToken("tok")
        sut.submit()
        assertFalse(sut.sent)
        assertEquals(StringsFr.magicRateLimited, sut.error)
        assertNull(sut.turnstileToken)
    }

    @Test fun captchaFailureShowsTurnstileErrorAndReloadsWidget() = runTest {
        val sut = vm(MockEngine { respond("""{"code":"captcha_failed","detail":"Captcha verification failed."}""", HttpStatusCode.BadRequest, jsonHeader) })
        sut.email = "a@b.co"; sut.onTurnstileToken("tok")
        val keyBefore = sut.turnstileResetKey
        sut.submit()
        assertFalse(sut.sent)
        assertEquals(StringsFr.turnstileFailed, sut.error)
        assertNull(sut.turnstileToken)
        assertFalse(sut.canSubmit)
        assertEquals(keyBefore + 1, sut.turnstileResetKey)
    }

    @Test fun otherFailureKeepsGenericErrorAndReloadsWidget() = runTest {
        val sut = vm(MockEngine { respond("""{"code":"server_error","detail":"x"}""", HttpStatusCode.InternalServerError, jsonHeader) })
        sut.email = "a@b.co"; sut.onTurnstileToken("tok")
        sut.submit()
        assertEquals(StringsFr.magicRequestFailed, sut.error)
        assertEquals(1, sut.turnstileResetKey)
    }

    @Test fun successDoesNotReloadWidget() = runTest {
        val sut = vm(MockEngine { respond("""{"detail":"ok"}""", HttpStatusCode.OK, jsonHeader) })
        sut.email = "a@b.co"; sut.onTurnstileToken("tok")
        sut.submit()
        assertEquals(0, sut.turnstileResetKey)
    }

    @Test fun turnstileErrorClearsTokenAndShowsError() = runTest {
        val sut = vm(MockEngine { respond("""{"detail":"ok"}""", HttpStatusCode.OK, jsonHeader) })
        sut.email = "a@b.co"; sut.onTurnstileToken("tok")
        sut.onTurnstileError()
        assertNull(sut.turnstileToken)
        assertEquals(StringsFr.turnstileFailed, sut.error)
    }
}
