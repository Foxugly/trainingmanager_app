package com.foxugly.trainingmanager_app.ui.register

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.data.api.TrainingManagerApi
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.StringsFr
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RegisterViewModelTest {
    private fun vm(engine: MockEngine): RegisterViewModel {
        val s = FakeTokenStore(access = "x")
        return RegisterViewModel(
            AuthRepository(TrainingManagerApi(s, baseUrl = "https://test/api/v1/", engine = engine), s),
            language = "en",
        )
    }

    private fun RegisterViewModel.fillValid() {
        firstName = "Ann"; lastName = "Lee"; email = "a@b.c"; password = "password1"
    }

    @Test fun cannotSubmitWithoutTurnstileToken() = runTest {
        val sut = vm(MockEngine { respond("", HttpStatusCode.Created) })
        sut.fillValid()
        assertFalse(sut.canSubmit) // no captcha token yet
        sut.onTurnstileToken("tok")
        assertTrue(sut.canSubmit)
    }

    @Test fun shortPasswordIsInvalid() = runTest {
        val sut = vm(MockEngine { respond("", HttpStatusCode.Created) })
        sut.password = "short" // 5 chars
        assertFalse(sut.passwordValid)
        sut.password = "password1" // 9 chars
        assertTrue(sut.passwordValid)
    }

    @Test fun successSetsRegistered() = runTest {
        val sut = vm(MockEngine { respond("", HttpStatusCode.Created) })
        sut.fillValid(); sut.onTurnstileToken("tok")
        sut.submit()
        assertTrue(sut.registered)
        assertNull(sut.error)
    }

    @Test fun error400MapsAndClearsToken() = runTest {
        val sut = vm(MockEngine { respond("""{"detail":"bad"}""", HttpStatusCode.BadRequest) })
        sut.fillValid(); sut.onTurnstileToken("tok")
        sut.submit()
        assertEquals(StringsFr.registerInvalid, sut.error)
        assertNull(sut.turnstileToken) // single-use token cleared so user re-solves
        assertFalse(sut.registered)
    }

    @Test fun turnstileErrorClearsTokenAndSetsMessage() = runTest {
        val sut = vm(MockEngine { respond("", HttpStatusCode.Created) })
        sut.onTurnstileToken("tok")
        sut.onTurnstileError()
        assertNull(sut.turnstileToken)
        assertEquals(StringsFr.turnstileFailed, sut.error)
    }
}
