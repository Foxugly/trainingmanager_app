package com.foxugly.trainingmanager_app.ui.discussions

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.customUserPublicJson
import com.foxugly.trainingmanager_app.meJson
import com.foxugly.trainingmanager_app.topicJson
import com.foxugly.trainingmanager_app.topicListJson
import com.foxugly.trainingmanager_app.topicMessageJson
import com.foxugly.trainingmanager_app.topicMessageListJson
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

class DiscussionsViewModelTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun repo(engine: MockEngine): AuthRepository {
        val s = FakeTokenStore(access = "tok")
        return AuthRepository(TrainingManagerApi(s, baseUrl = "https://test/api/v1/", engine = engine), s)
    }

    @Test fun topicsLoadFiltersTeamAudience() = runTest {
        val body = topicListJson(
            topicJson(id = 1, title = "Team", audience = "team", allowAthleteReplies = true),
            topicJson(id = 2, title = "Staff", audience = "coaches", allowAthleteReplies = false),
        )
        val vm = TopicsListViewModel(repo(MockEngine { respond(body, HttpStatusCode.OK, jsonHeader) }))
        vm.load(3)
        assertEquals(1, vm.topics.size)
        assertEquals("Team", vm.topics[0].title)
    }

    @Test fun threadLoadsMessagesAndMyId() = runTest {
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("me/") ->
                    respond(meJson(id = 42), HttpStatusCode.OK, jsonHeader)
                else ->
                    respond(topicMessageListJson(topicMessageJson(id = 9, content = "hi", author = customUserPublicJson(id = 42, firstName = "A", lastName = "B"))), HttpStatusCode.OK, jsonHeader)
            }
        }
        val vm = TopicThreadViewModel(repo(engine))
        vm.load(3, 7)
        assertEquals(1, vm.messages.size)
        assertEquals(42, vm.myUserId)
        assertTrue(vm.isMine(vm.messages[0]))
    }

    @Test fun sendAppendsMessage() = runTest {
        val engine = MockEngine { respond(topicMessageJson(id = 10, content = "new", author = customUserPublicJson(id = 42, firstName = "A", lastName = "B")), HttpStatusCode.Created, jsonHeader) }
        val vm = TopicThreadViewModel(repo(engine))
        vm.composeText = "new"
        vm.send(3, 7)
        assertEquals(1, vm.messages.size)
        assertEquals("", vm.composeText)
        assertFalse(vm.isSending)
    }

    @Test fun sendFailureSetsError() = runTest {
        val engine = MockEngine { respond("""{"detail":"no"}""", HttpStatusCode.Forbidden, jsonHeader) }
        val vm = TopicThreadViewModel(repo(engine))
        vm.composeText = "x"
        vm.send(3, 7)
        assertEquals(StringsFr.postFailed, vm.sendError)
    }
}
