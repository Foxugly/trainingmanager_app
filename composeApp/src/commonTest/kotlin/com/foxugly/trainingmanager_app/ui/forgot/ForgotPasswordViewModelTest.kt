package com.foxugly.trainingmanager_app.ui.forgot

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.data.api.TrainingManagerApi
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.StringsFr
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

class ForgotPasswordViewModelTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun vm(engine: MockEngine): ForgotPasswordViewModel {
        val s = FakeTokenStore(access = "x")
        return ForgotPasswordViewModel(
            AuthRepository(TrainingManagerApi(s, baseUrl = "https://test/api/v1/", engine = engine), s),
        )
    }

    @Test fun cannotSubmitWithoutTurnstileToken() = runTest {
        val sut = vm(MockEngine { respond("", HttpStatusCode.OK) })
        sut.email = "a@b.c"
        assertFalse(sut.canSubmit)
        sut.onTurnstileToken("tok")
        assertTrue(sut.canSubmit)
    }

    @Test fun successSetsSent() = runTest {
        val sut = vm(MockEngine { respond("", HttpStatusCode.OK) })
        sut.email = "a@b.c"; sut.onTurnstileToken("tok")
        sut.submit()
        assertTrue(sut.sent)
        assertNull(sut.error)
        assertEquals(0, sut.turnstileResetKey)
    }

    @Test fun captchaFailureShowsTurnstileErrorAndReloadsWidget() = runTest {
        val sut = vm(MockEngine { respond("""{"code":"captcha_failed","detail":"Captcha verification failed."}""", HttpStatusCode.BadRequest, jsonHeader) })
        sut.email = "a@b.c"; sut.onTurnstileToken("tok")
        sut.submit()
        assertFalse(sut.sent)
        assertEquals(StringsFr.turnstileFailed, sut.error)
        assertNull(sut.turnstileToken)
        assertEquals(1, sut.turnstileResetKey)
    }

    @Test fun otherFailureKeepsGenericErrorAndReloadsWidget() = runTest {
        val sut = vm(MockEngine { respond("""{"code":"server_error","detail":"x"}""", HttpStatusCode.InternalServerError, jsonHeader) })
        sut.email = "a@b.c"; sut.onTurnstileToken("tok")
        sut.submit()
        assertEquals(StringsFr.forgotFailed, sut.error)
        assertNull(sut.turnstileToken)
        assertEquals(1, sut.turnstileResetKey)
    }
}
