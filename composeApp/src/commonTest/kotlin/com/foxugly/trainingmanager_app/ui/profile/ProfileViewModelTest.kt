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
import kotlin.test.assertTrue

class ProfileViewModelTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun vm(engine: MockEngine): ProfileViewModel {
        val s = FakeTokenStore(access = "tok")
        return ProfileViewModel(AuthRepository(TrainingManagerApi(s, baseUrl = "https://test/api/v1/", engine = engine), s))
    }

    @Test fun loadFillsFields() = runTest {
        val sut = vm(MockEngine { respond("""{"id":1,"email":"a@b.co","first_name":"Ann","last_name":"Lee","weekly_recap_opt_in":false,"digest_email":true}""", HttpStatusCode.OK, jsonHeader) })
        sut.load()
        assertEquals("a@b.co", sut.email)
        assertEquals("Ann", sut.firstName)
        assertEquals("Lee", sut.lastName)
        assertEquals(false, sut.weeklyRecapOptIn)
        assertEquals(true, sut.digestEmail)
    }

    @Test fun saveSuccessSetsSaved() = runTest {
        val sut = vm(MockEngine { respond("""{"id":1,"email":"a@b.co"}""", HttpStatusCode.OK, jsonHeader) })
        sut.firstName = "Ann"
        sut.save()
        assertTrue(sut.saved)
    }

    @Test fun saveFailureSetsError() = runTest {
        val sut = vm(MockEngine { respond("""{"detail":"x"}""", HttpStatusCode.InternalServerError, jsonHeader) })
        sut.save()
        assertEquals(StringsFr.saveFailed, sut.saveError)
    }
}
