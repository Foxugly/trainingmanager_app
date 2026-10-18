package com.foxugly.trainingmanager_app.ui.teams

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.data.api.TrainingManagerApi
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.StringsFr
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TeamDetailInviteTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun vm(engine: MockEngine): TeamDetailViewModel {
        val s = FakeTokenStore(access = "tok")
        return TeamDetailViewModel(AuthRepository(TrainingManagerApi(s, baseUrl = "https://test/api/v1/", engine = engine), s))
    }

    private fun failingCreate(status: HttpStatusCode, body: String) = MockEngine { request ->
        if (request.method == HttpMethod.Post) {
            respond(body, status, jsonHeader)
        } else {
            respond("""{"count":0,"next":null,"previous":null,"results":[]}""", HttpStatusCode.OK, jsonHeader)
        }
    }

    @Test fun throttledInvitationShowsTheRateLimitMessage() = runTest {
        val sut = vm(failingCreate(HttpStatusCode.TooManyRequests, """{"detail":"Request was throttled."}"""))
        sut.invite("athlete@example.com", "Ann", "Lee")
        assertEquals(StringsFr.magicRateLimited, sut.inviteError)
        assertFalse(sut.isSavingInvite)
    }

    @Test fun otherInvitationErrorsShowTheGenericMessage() = runTest {
        val sut = vm(failingCreate(HttpStatusCode.BadRequest, """{"code":"email_already_invited"}"""))
        sut.invite("athlete@example.com", "Ann", "Lee")
        assertEquals(StringsFr.inviteFailed, sut.inviteError)
    }
}
