package com.foxugly.trainingmanager_app.ui.profile

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.data.api.TrainingManagerApi
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.LanguageProvider
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
    private fun vm(engine: MockEngine, lp: LanguageProvider = LanguageProvider()): ProfileViewModel {
        val s = FakeTokenStore(access = "tok")
        return ProfileViewModel(AuthRepository(TrainingManagerApi(s, baseUrl = "https://test/api/v1/", engine = engine), s), lp)
    }

    @Test fun loadFillsFields() = runTest {
        val sut = vm(MockEngine { respond("""{"id":1,"email":"a@b.co","first_name":"Ann","last_name":"Lee","language":"nl","weekly_recap_opt_in":false,"digest_email":true}""", HttpStatusCode.OK, jsonHeader) })
        sut.load()
        assertEquals("a@b.co", sut.email)
        assertEquals("Ann", sut.firstName)
        assertEquals("nl", sut.language)
        assertEquals(false, sut.weeklyRecapOptIn)
        assertEquals(true, sut.digestEmail)
    }

    @Test fun saveSuccessSetsSavedAndUpdatesLanguageTag() = runTest {
        val lp = LanguageProvider()
        val sut = vm(MockEngine { respond("""{"id":1,"email":"a@b.co","language":"es"}""", HttpStatusCode.OK, jsonHeader) }, lp)
        sut.firstName = "Ann"; sut.language = "es"
        sut.save()
        assertTrue(sut.saved)
        assertEquals("es", lp.activeTag)
    }

    @Test fun saveFailureSetsError() = runTest {
        val sut = vm(MockEngine { respond("""{"detail":"x"}""", HttpStatusCode.InternalServerError, jsonHeader) })
        sut.save()
        assertEquals(ProfileStrings.saveFailed, sut.saveError)
    }
}
