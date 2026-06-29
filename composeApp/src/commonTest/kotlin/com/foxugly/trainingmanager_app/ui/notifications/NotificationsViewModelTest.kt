package com.foxugly.trainingmanager_app.ui.notifications

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.data.api.TrainingManagerApi
import com.foxugly.trainingmanager_app.notificationJson
import com.foxugly.trainingmanager_app.notificationListJson
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

class NotificationsViewModelTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun vm(engine: MockEngine): NotificationsViewModel {
        val s = FakeTokenStore(access = "tok")
        return NotificationsViewModel(AuthRepository(TrainingManagerApi(s, baseUrl = "https://test/api/v1/", engine = engine), s))
    }

    @Test fun loadPopulates() = runTest {
        val body = notificationListJson(notificationJson(id = 7, url = "/teams/3"))
        val sut = vm(MockEngine { respond(body, HttpStatusCode.OK, jsonHeader) })
        sut.load()
        assertEquals(1, sut.notifications.size)
        assertEquals(false, sut.notifications[0].isRead)
    }

    @Test fun loadFailureSetsError() = runTest {
        val sut = vm(MockEngine { respond("", HttpStatusCode.InternalServerError) })
        sut.load()
        assertEquals(StringsFr.notificationsLoadFailed, sut.error)
    }

    @Test fun markReadUpdatesLocally() = runTest {
        val body = notificationListJson(notificationJson(id = 7, url = ""))
        var calls = 0
        val engine = MockEngine { request ->
            calls++
            if (request.url.encodedPath.endsWith("/read/")) respond("", HttpStatusCode.OK)
            else respond(body, HttpStatusCode.OK, jsonHeader)
        }
        val sut = vm(engine)
        sut.load()
        sut.markRead(7)
        assertTrue(sut.notifications[0].isRead)
    }
}
