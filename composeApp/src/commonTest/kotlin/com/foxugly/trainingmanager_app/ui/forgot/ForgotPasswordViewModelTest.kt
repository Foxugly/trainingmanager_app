package com.foxugly.trainingmanager_app.ui.forgot

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.data.api.TrainingManagerApi
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ForgotPasswordViewModelTest {
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
    }
}
