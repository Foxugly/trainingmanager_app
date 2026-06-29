package com.foxugly.trainingmanager_app.ui.magiclink

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
import kotlin.test.assertTrue

class MagicLinkRequestViewModelTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun vm(engine: MockEngine): MagicLinkRequestViewModel {
        val store = FakeTokenStore()
        return MagicLinkRequestViewModel(AuthRepository(TrainingManagerApi(store, baseUrl = "https://test/api/v1/", engine = engine), store))
    }

    @Test fun submitSetsSentOnSuccess() = runTest {
        val sut = vm(MockEngine { respond("""{"detail":"ok"}""", HttpStatusCode.OK, jsonHeader) })
        sut.email = "a@b.co"
        sut.submit()
        assertTrue(sut.sent)
        assertFalse(sut.isLoading)
    }

    @Test fun submitSetsRateLimitedOn429() = runTest {
        val sut = vm(MockEngine { respond("""{"detail":"x"}""", HttpStatusCode.TooManyRequests, jsonHeader) })
        sut.email = "a@b.co"
        sut.submit()
        assertFalse(sut.sent)
        assertEquals(StringsFr.magicRateLimited, sut.error)
    }
}
