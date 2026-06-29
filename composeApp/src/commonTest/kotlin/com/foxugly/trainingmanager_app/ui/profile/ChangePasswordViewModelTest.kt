package com.foxugly.trainingmanager_app.ui.profile

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

class ChangePasswordViewModelTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun vm(engine: MockEngine): ChangePasswordViewModel {
        val s = FakeTokenStore(access = "tok")
        return ChangePasswordViewModel(AuthRepository(TrainingManagerApi(s, baseUrl = "https://test/api/v1/", engine = engine), s))
    }

    @Test fun mismatchSetsError() = runTest {
        val sut = vm(MockEngine { respond("""{"detail":"ok"}""", HttpStatusCode.OK, jsonHeader) })
        sut.currentPassword = "old12345"; sut.newPassword = "new12345"; sut.confirmPassword = "nope12345"
        sut.submit()
        assertEquals(StringsFr.mismatch, sut.error)
    }

    @Test fun successSetsSuccess() = runTest {
        val sut = vm(MockEngine { respond("""{"detail":"ok"}""", HttpStatusCode.OK, jsonHeader) })
        sut.currentPassword = "old12345"; sut.newPassword = "new12345"; sut.confirmPassword = "new12345"
        sut.submit()
        assertTrue(sut.success)
        assertFalse(sut.isLoading)
    }

    @Test fun currentPasswordInvalidMapped() = runTest {
        val sut = vm(MockEngine { respond("""{"code":"current_password_invalid"}""", HttpStatusCode.BadRequest, jsonHeader) })
        sut.currentPassword = "wrong123"; sut.newPassword = "new12345"; sut.confirmPassword = "new12345"
        sut.submit()
        assertEquals(StringsFr.cpCurrentInvalid, sut.error)
    }
}
