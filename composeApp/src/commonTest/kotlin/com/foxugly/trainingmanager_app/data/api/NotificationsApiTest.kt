package com.foxugly.trainingmanager_app.data.api

import com.foxugly.trainingmanager_app.FakeTokenStore
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotificationsApiTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun api(engine: MockEngine) =
        TrainingManagerApi(FakeTokenStore(access = "tok"), baseUrl = "https://test/api/v1/", engine = engine)

    @Test fun listDecodes() = runTest {
        val body = """{"count":1,"results":[{"id":7,"type":"MESSAGE_NEW_TOPIC","title":"Nouveau sujet","body":"…","url":"/teams/3","is_read":false,"created_at":"2026-06-01T10:00:00Z"}]}"""
        val api = api(MockEngine { respond(body, HttpStatusCode.OK, jsonHeader) })
        val list = api.listNotifications().getOrThrow()
        assertEquals("Nouveau sujet", list.results[0].title)
        assertEquals("/teams/3", list.results[0].url)
        assertEquals(false, list.results[0].isRead)
    }

    @Test fun markReadSucceeds() = runTest {
        val api = api(MockEngine { respond("", HttpStatusCode.OK) })
        assertTrue(api.markNotificationRead(7).isSuccess)
    }

    @Test fun markAllReadSucceeds() = runTest {
        val api = api(MockEngine { respond("", HttpStatusCode.OK) })
        assertTrue(api.markAllNotificationsRead().isSuccess)
    }
}
