package com.foxugly.trainingmanager_app.ui.login

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

class LoginViewModelTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")

    private fun vm(store: FakeTokenStore, engine: MockEngine): LoginViewModel =
        LoginViewModel(AuthRepository(TrainingManagerApi(store, baseUrl = "https://test/api/v1/", engine = engine), store))

    @Test
    fun successInvokesOnSuccessAndStoresTokens() = runTest {
        val store = FakeTokenStore()
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("auth/token/") ->
                    respond("""{"access":"acc","refresh":"ref"}""", HttpStatusCode.OK, jsonHeader)
                request.url.encodedPath.endsWith("me/") ->
                    respond("""{"id":1,"email":"a@b.co"}""", HttpStatusCode.OK, jsonHeader)
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val sut = vm(store, engine)
        sut.email = "a@b.co"; sut.password = "pw"; sut.rememberMe = true
        var ok = false
        sut.submit { ok = true }
        assertTrue(ok, "onSuccess should fire")
        assertEquals("acc", store.getAccessToken())
        assertEquals("ref", store.getRefreshToken())
        assertTrue(store.getRemember())
        assertFalse(sut.isLoading)
        assertNull(sut.error)
    }

    @Test
    fun failureSetsErrorAndDoesNotCallOnSuccess() = runTest {
        val store = FakeTokenStore()
        val engine = MockEngine { respond("""{"detail":"bad"}""", HttpStatusCode.Unauthorized, jsonHeader) }
        val sut = vm(store, engine)
        sut.email = "a@b.co"; sut.password = "wrong"
        var ok = false
        sut.submit { ok = true }
        assertFalse(ok)
        assertEquals(StringsFr.invalidCredentials, sut.error)
        assertFalse(sut.isLoading)
        assertNull(store.getAccessToken())
    }

    @Test
    fun canSubmitReflectsFieldsAndLoading() {
        val sut = LoginViewModel(
            AuthRepository(
                TrainingManagerApi(FakeTokenStore(), baseUrl = "https://test/api/v1/", engine = MockEngine { respond("", HttpStatusCode.OK) }),
                FakeTokenStore(),
            ),
        )
        assertFalse(sut.canSubmit)
        sut.email = "a@b.co"
        assertFalse(sut.canSubmit)
        sut.password = "pw"
        assertTrue(sut.canSubmit)
    }
}
