package com.foxugly.trainingmanager_app.ui.invitation

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.validateInvitationJson
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

class InvitationViewModelTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun vm(engine: MockEngine): InvitationViewModel {
        val s = FakeTokenStore()
        return InvitationViewModel(AuthRepository(TrainingManagerApi(s, baseUrl = "https://test/api/v1/", engine = engine), s))
    }

    @Test fun loadPendingSetsTeam() = runTest {
        val sut = vm(MockEngine { respond(validateInvitationJson(), HttpStatusCode.OK, jsonHeader) })
        sut.load("tok")
        assertEquals("Sharks", sut.teamName)
        assertTrue(sut.isPending)
    }

    @Test fun load410SetsLookupError() = runTest {
        val sut = vm(MockEngine { respond("""{"code":"invitation_expired"}""", HttpStatusCode.Gone, jsonHeader) })
        sut.load("tok")
        assertEquals(StringsFr.invitationLookupFailed, sut.lookupError)
    }

    @Test fun acceptMismatchSetsSubmitError() = runTest {
        val sut = vm(MockEngine { respond(validateInvitationJson(teamName = "S"), HttpStatusCode.OK, jsonHeader) })
        sut.load("tok")
        sut.password = "password1"; sut.confirmPassword = "password2"
        var ok = false
        sut.accept("tok") { ok = true }
        assertFalse(ok)
        assertEquals(StringsFr.mismatch, sut.submitError)
    }

    @Test fun acceptEmailTakenOn409() = runTest {
        val engine = MockEngine { request ->
            if (request.method.value == "GET") respond(validateInvitationJson(teamName = "S"), HttpStatusCode.OK, jsonHeader)
            else respond("""{"code":"email_taken"}""", HttpStatusCode.Conflict, jsonHeader)
        }
        val sut = vm(engine)
        sut.load("tok")
        sut.password = "password1"; sut.confirmPassword = "password1"
        sut.accept("tok") {}
        assertEquals(StringsFr.invitationEmailTaken, sut.submitError)
    }
}
