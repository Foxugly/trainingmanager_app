package com.foxugly.trainingmanager_app.ui.profile

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.data.api.TrainingManagerApi
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.StringsEn
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeleteAccountViewModelTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")

    private fun sut(store: FakeTokenStore, engine: MockEngine) =
        DeleteAccountViewModel(
            AuthRepository(TrainingManagerApi(store, baseUrl = "https://test/api/v1/", engine = engine), store),
            StringsEn,
        )

    @Test fun deletesAccountAndClearsTheLocalSession() = runTest {
        val store = FakeTokenStore(access = "tok", refresh = "ref")
        var method: HttpMethod? = null
        var path: String? = null
        var body: String? = null
        var auth: String? = null
        val engine = MockEngine { request ->
            method = request.method
            path = request.url.encodedPath
            body = (request.body as? TextContent)?.text
            auth = request.headers[HttpHeaders.Authorization]
            respond("", HttpStatusCode.NoContent)
        }
        val vm = sut(store, engine)
        vm.currentPassword = "secret123"

        assertTrue(vm.submit())
        assertEquals(HttpMethod.Post, method)
        assertEquals("/api/v1/auth/account/delete/", path)
        assertEquals("""{"current_password":"secret123"}""", body)
        assertEquals("Bearer tok", auth)
        assertTrue(store.cleared)
        assertNull(store.getRefreshToken())
        assertNull(vm.error)
    }

    @Test fun wrongPasswordKeepsTheSession() = runTest {
        val store = FakeTokenStore(access = "tok", refresh = "ref")
        val engine = MockEngine {
            respond("""{"code":"current_password_invalid","detail":"Current password is incorrect."}""", HttpStatusCode.BadRequest, jsonHeader)
        }
        val vm = sut(store, engine)
        vm.currentPassword = "bad"

        assertFalse(vm.submit())
        assertEquals(StringsEn.cpCurrentInvalid, vm.error)
        assertFalse(store.cleared)
        assertEquals("ref", store.getRefreshToken())
    }

    @Test fun ownerOfATeamIsRefused() = runTest {
        val store = FakeTokenStore(access = "tok", refresh = "ref")
        val engine = MockEngine {
            respond("""{"code":"owns_teams","detail":"Transfer ownership first."}""", HttpStatusCode.Conflict, jsonHeader)
        }
        val vm = sut(store, engine)
        vm.currentPassword = "secret123"

        assertFalse(vm.submit())
        assertEquals(StringsEn.deleteAccountOwnsTeams, vm.error)
        assertFalse(store.cleared)
    }

    @Test fun blankPasswordDoesNotCallTheServer() = runTest {
        var calls = 0
        val engine = MockEngine { calls++; respond("", HttpStatusCode.NoContent) }
        val vm = sut(FakeTokenStore(access = "tok"), engine)

        assertFalse(vm.canSubmit)
        assertFalse(vm.submit())
        assertEquals(0, calls)
    }
}
