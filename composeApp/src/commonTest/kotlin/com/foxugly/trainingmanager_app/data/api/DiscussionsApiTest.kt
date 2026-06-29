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

class DiscussionsApiTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun api(engine: MockEngine) =
        TrainingManagerApi(FakeTokenStore(access = "tok"), baseUrl = "https://test/api/v1/", engine = engine)

    @Test fun listTopicsDecodes() = runTest {
        val body = """{"count":1,"results":[{"id":7,"title":"Compétition","audience":"team","allow_athlete_replies":true,"author":{"id":1,"first_name":"Ann","last_name":"Lee"},"message_count":4,"updated_at":"2026-06-01T10:00:00Z"}]}"""
        val api = api(MockEngine { respond(body, HttpStatusCode.OK, jsonHeader) })
        val list = api.listTopics(3).getOrThrow()
        assertEquals("Compétition", list.results[0].title)
        assertTrue(list.results[0].allowAthleteReplies)
    }

    @Test fun postMessageDecodes() = runTest {
        val api = api(MockEngine { respond("""{"id":9,"content":"<p>Hi</p>","author":{"id":2,"first_name":"Bob","last_name":"K"},"edited_at":null,"created_at":"2026-06-02T10:00:00Z"}""", HttpStatusCode.Created, jsonHeader) })
        val m = api.postMessage(3, 7, TopicMessageRequest("<p>Hi</p>")).getOrThrow()
        assertEquals(9, m.id)
        assertEquals(2, m.author?.id)
    }

    @Test fun deleteMessageSucceedsOn204() = runTest {
        val api = api(MockEngine { respond("", HttpStatusCode.NoContent) })
        assertTrue(api.deleteMessage(3, 7, 9).isSuccess)
    }
}
