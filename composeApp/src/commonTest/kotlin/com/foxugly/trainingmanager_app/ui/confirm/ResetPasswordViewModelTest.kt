package com.foxugly.trainingmanager_app.ui.confirm

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
import kotlin.test.assertTrue

class ResetPasswordViewModelTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun vm(engine: MockEngine): ResetPasswordViewModel {
        val s = FakeTokenStore()
        return ResetPasswordViewModel(AuthRepository(TrainingManagerApi(s, baseUrl = "https://test/api/v1/", engine = engine), s))
    }
    private fun okEngine() = MockEngine { request ->
        when {
            request.url.encodedPath.endsWith("reset/confirm/") -> respond("""{"access":"a","refresh":"r"}""", HttpStatusCode.OK, jsonHeader)
            else -> respond("""{"id":1,"email":"a@b.co"}""", HttpStatusCode.OK, jsonHeader)
        }
    }

    @Test fun mismatchSetsErrorAndDoesNotSubmit() = runTest {
        val sut = vm(okEngine())
        sut.newPassword = "password1"; sut.confirmPassword = "password2"
        var ok = false
        sut.submit("k") { ok = true }
        assertFalse(ok)
        assertEquals(StringsFr.mismatch, sut.error)
    }

    @Test fun successAutoLogins() = runTest {
        val sut = vm(okEngine())
        sut.newPassword = "password1"; sut.confirmPassword = "password1"
        var ok = false
        sut.submit("k") { ok = true }
        assertTrue(ok)
    }

    @Test fun tokenInvalidOn400ExpiredToken() = runTest {
        val sut = vm(MockEngine { respond("""{"code":"invalid_or_expired_token"}""", HttpStatusCode.BadRequest, jsonHeader) })
        sut.newPassword = "password1"; sut.confirmPassword = "password1"
        sut.submit("k") {}
        assertTrue(sut.tokenInvalid)
    }
}
