package com.foxugly.trainingmanager_app.data.api

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.api.generated.models.TopicMessageRequest
import com.foxugly.trainingmanager_app.customUserPublicJson
import com.foxugly.trainingmanager_app.topicJson
import com.foxugly.trainingmanager_app.topicListJson
import com.foxugly.trainingmanager_app.topicMessageJson
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
        val body = topicListJson(
            topicJson(id = 7, title = "Compétition", audience = "team", allowAthleteReplies = true, messageCount = 4),
        )
        val api = api(MockEngine { respond(body, HttpStatusCode.OK, jsonHeader) })
        val list = api.listTopics(3).getOrThrow()
        assertEquals("Compétition", list.results[0].title)
        assertTrue(list.results[0].allowAthleteReplies == true)
    }

    @Test fun postMessageDecodes() = runTest {
        val body = topicMessageJson(id = 9, content = "<p>Hi</p>", author = customUserPublicJson(id = 2, firstName = "Bob", lastName = "K"), createdAt = "2026-06-02T10:00:00Z")
        val api = api(MockEngine { respond(body, HttpStatusCode.Created, jsonHeader) })
        val m = api.postMessage(3, 7, TopicMessageRequest("<p>Hi</p>")).getOrThrow()
        assertEquals(9, m.id)
        assertEquals(2, m.author.id)
    }

    @Test fun deleteMessageSucceedsOn204() = runTest {
        val api = api(MockEngine { respond("", HttpStatusCode.NoContent) })
        assertTrue(api.deleteMessage(3, 7, 9).isSuccess)
    }
}
