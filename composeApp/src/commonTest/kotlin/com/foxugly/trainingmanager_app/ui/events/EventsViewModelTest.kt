package com.foxugly.trainingmanager_app.ui.events

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
import kotlin.test.assertNotNull

class EventsViewModelTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun repo(engine: MockEngine): AuthRepository {
        val s = FakeTokenStore(access = "tok")
        return AuthRepository(TrainingManagerApi(s, baseUrl = "https://test/api/v1/", engine = engine), s)
    }

    @Test fun listLoadPopulatesEvents() = runTest {
        val body = """{"count":1,"next":null,"previous":null,"results":[{"id":5,"name":"Séance","location":"Gym","equipment":"","total":0,"place":null,"refer_program":null,"vis_distance":"never","vis_goal":"never","vis_rounds":"never","debrief":""}]}"""
        val vm = EventsListViewModel(repo(MockEngine { respond(body, HttpStatusCode.OK, jsonHeader) }))
        vm.load()
        assertEquals(1, vm.events.size)
        assertEquals("Séance", vm.events[0].name)
    }

    @Test fun listLoadFailureSetsError() = runTest {
        val vm = EventsListViewModel(repo(MockEngine { respond("", HttpStatusCode.InternalServerError) }))
        vm.load()
        assertEquals(StringsFr.eventsLoadFailed, vm.error)
    }

    @Test fun detailLoadsEventAndRsvp() = runTest {
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("/rsvp/") ->
                    respond("""{"counts":{"going":1,"maybe":0,"not_going":0,"no_response":0},"total_members":1,"my_status":"going","by_member":[]}""", HttpStatusCode.OK, jsonHeader)
                else ->
                    respond("""{"id":5,"name":"Match","location":"Stade","equipment":"","total":0,"place":null,"refer_program":null,"vis_distance":"always","vis_goal":"never","vis_rounds":"never","debrief":""}""", HttpStatusCode.OK, jsonHeader)
            }
        }
        val vm = EventDetailViewModel(repo(engine))
        vm.load(5)
        assertNotNull(vm.event)
        assertEquals("going", vm.rsvp?.myStatus)
        assertEquals(true, vm.showDistance)
    }

    @Test fun setRsvpUpdatesStatus() = runTest {
        val engine = MockEngine { respond("""{"counts":{"going":2,"maybe":0,"not_going":0,"no_response":0},"total_members":2,"my_status":"maybe","by_member":[]}""", HttpStatusCode.OK, jsonHeader) }
        val vm = EventDetailViewModel(repo(engine))
        vm.setRsvp(5, "maybe")
        assertEquals("maybe", vm.rsvp?.myStatus)
    }

    @Test fun setRsvpDisabledSetsError() = runTest {
        val engine = MockEngine { respond("""{"detail":"disabled"}""", HttpStatusCode.Forbidden, jsonHeader) }
        val vm = EventDetailViewModel(repo(engine))
        vm.setRsvp(5, "going")
        assertEquals(StringsFr.rsvpDisabled, vm.rsvpError)
    }

    @Test fun setRotiUpdatesScore() = runTest {
        val engine = MockEngine { respond("""{"average":4.0,"count":3,"distribution":{},"my_score":4}""", HttpStatusCode.OK, jsonHeader) }
        val vm = EventDetailViewModel(repo(engine))
        vm.setRoti(5, 4)
        assertEquals(4, vm.rotiScore)
    }

    @Test fun setRotiFailureSetsError() = runTest {
        val engine = MockEngine { respond("""{"detail":"disabled"}""", HttpStatusCode.Forbidden, jsonHeader) }
        val vm = EventDetailViewModel(repo(engine))
        vm.setRoti(5, 4)
        assertEquals(StringsFr.rotiFailed, vm.rotiError)
    }
}
