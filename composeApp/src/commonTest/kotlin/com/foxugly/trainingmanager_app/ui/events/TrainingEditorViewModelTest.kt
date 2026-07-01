package com.foxugly.trainingmanager_app.ui.events

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.data.api.TrainingManagerApi
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.eventJson
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

class TrainingEditorViewModelTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun repo(engine: MockEngine): AuthRepository {
        val s = FakeTokenStore(access = "tok")
        return AuthRepository(TrainingManagerApi(s, baseUrl = "https://test/api/v1/", engine = engine), s)
    }

    @Test fun loadWithNoRoundsHasNoTraining() = runTest {
        val vm = TrainingEditorViewModel(repo(MockEngine { respond(eventJson(id = 5), HttpStatusCode.OK, jsonHeader) }))
        vm.load(5)
        assertFalse(vm.isLoading)
        assertFalse(vm.hasTraining)
    }

    @Test fun generateConflictShowsConflictMessage() = runTest {
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("generate-training/")) {
                respond("""{"code":"event_has_rounds"}""", HttpStatusCode.Conflict, jsonHeader)
            } else {
                respond(eventJson(id = 5), HttpStatusCode.OK, jsonHeader)
            }
        }
        val vm = TrainingEditorViewModel(repo(engine))
        vm.load(5)
        vm.generate()
        assertEquals(StringsFr.trainingConflict, vm.actionError)
    }
}
