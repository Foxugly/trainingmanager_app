package com.foxugly.trainingmanager_app.ui.events

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.data.api.TrainingManagerApi
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.eventJson
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

class EventEditorViewModelTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun repo(engine: MockEngine): AuthRepository {
        val s = FakeTokenStore(access = "tok")
        return AuthRepository(TrainingManagerApi(s, baseUrl = "https://test/api/v1/", engine = engine), s)
    }

    @Test fun editModePrefillsFromEvent() = runTest {
        val engine = MockEngine { respond(eventJson(id = 5, name = "Séance", location = "Gym"), HttpStatusCode.OK, jsonHeader) }
        val vm = EventEditorViewModel(repo(engine))
        vm.load(eventId = 5, teamId = null)
        assertFalse(vm.isNew)
        assertEquals("Séance", vm.name)
        assertEquals("Gym", vm.location)
    }

    @Test fun createModeIsNewAndLoadsTeamPrograms() = runTest {
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("programs/")) {
                respond("""{"count":0,"next":null,"previous":null,"results":[]}""", HttpStatusCode.OK, jsonHeader)
            } else {
                respond("", HttpStatusCode.NotFound)
            }
        }
        val vm = EventEditorViewModel(repo(engine))
        vm.load(eventId = null, teamId = 3)
        assertTrue(vm.isNew)
        // Name + program still required, so save stays disabled.
        assertFalse(vm.canSave)
    }
}
