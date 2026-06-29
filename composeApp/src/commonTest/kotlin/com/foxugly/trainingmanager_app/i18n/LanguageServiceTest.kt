package com.foxugly.trainingmanager_app.i18n

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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LanguageServiceTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun service(engine: MockEngine): Pair<LanguageService, LanguageProvider> {
        val store = FakeTokenStore(access = "tok")
        val lp = LanguageProvider()
        val repo = AuthRepository(TrainingManagerApi(store, baseUrl = "https://test/api/v1/", engine = engine), store)
        return LanguageService(repo, lp) to lp
    }

    @Test fun setActiveUpdatesTagAndStrings() {
        val (svc, lp) = service(MockEngine { respond("", HttpStatusCode.OK) })
        svc.setActive("en")
        assertEquals("en", svc.activeLang)
        assertEquals("en", lp.activeTag)
        assertEquals(StringsEn.loginTitle, svc.strings.loginTitle)
    }

    @Test fun switchLanguageSuccessPersists() = runTest {
        val (svc, _) = service(MockEngine { respond("""{"id":1,"email":"a@b.co","language":"nl"}""", HttpStatusCode.OK, jsonHeader) })
        assertTrue(svc.switchLanguage("nl"))
        assertEquals("nl", svc.activeLang)
    }

    @Test fun switchLanguageFailureRollsBack() = runTest {
        val (svc, _) = service(MockEngine { respond("""{"detail":"x"}""", HttpStatusCode.InternalServerError, jsonHeader) })
        svc.setActive("en")
        assertFalse(svc.switchLanguage("es"))
        assertEquals("en", svc.activeLang)
    }
}
