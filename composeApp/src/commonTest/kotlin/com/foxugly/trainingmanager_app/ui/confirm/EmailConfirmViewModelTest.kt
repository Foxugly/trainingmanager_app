package com.foxugly.trainingmanager_app.ui.confirm

import com.foxugly.trainingmanager_app.FakeTokenStore
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
import kotlin.test.assertTrue

class EmailConfirmViewModelTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun vm(engine: MockEngine): EmailConfirmViewModel {
        val s = FakeTokenStore()
        return EmailConfirmViewModel(AuthRepository(TrainingManagerApi(s, baseUrl = "https://test/api/v1/", engine = engine), s))
    }

    @Test fun successOnValidKey() = runTest {
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("email/confirm/") -> respond("""{"access":"a","refresh":"r"}""", HttpStatusCode.OK, jsonHeader)
                else -> respond("""{"id":1,"email":"a@b.co"}""", HttpStatusCode.OK, jsonHeader)
            }
        }
        val sut = vm(engine); var ok = false
        sut.confirm("k") { ok = true }
        assertTrue(ok)
        assertEquals(EmailConfirmViewModel.State.Success, sut.state)
    }

    @Test fun invalidOn400() = runTest {
        val sut = vm(MockEngine { respond("""{"code":"invalid_or_expired_token"}""", HttpStatusCode.BadRequest, jsonHeader) })
        sut.confirm("k") {}
        assertEquals(EmailConfirmViewModel.State.Invalid, sut.state)
    }
}
