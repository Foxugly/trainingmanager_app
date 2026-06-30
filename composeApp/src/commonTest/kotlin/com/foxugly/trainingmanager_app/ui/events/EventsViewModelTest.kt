package com.foxugly.trainingmanager_app.ui.events

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.api.generated.models.RsvpStatusEnum
import com.foxugly.trainingmanager_app.api.generated.models.RsvpSummary
import com.foxugly.trainingmanager_app.data.api.TrainingManagerApi
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.eventJson
import com.foxugly.trainingmanager_app.eventListJson
import com.foxugly.trainingmanager_app.i18n.StringsFr
import com.foxugly.trainingmanager_app.rsvpCountsJson
import com.foxugly.trainingmanager_app.rsvpSummaryJson
import com.foxugly.trainingmanager_app.rotiSummaryJson
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
        val body = eventListJson(eventJson(id = 5, name = "Séance", location = "Gym"))
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
                    respond(rsvpSummaryJson(counts = rsvpCountsJson(going = 1), totalMembers = 1, myStatus = "going"), HttpStatusCode.OK, jsonHeader)
                else ->
                    respond(eventJson(id = 5, name = "Match", location = "Stade", visDistance = "always"), HttpStatusCode.OK, jsonHeader)
            }
        }
        val vm = EventDetailViewModel(repo(engine))
        vm.load(5)
        assertNotNull(vm.event)
        assertEquals(RsvpSummary.MyStatus.GOING, vm.rsvp?.myStatus)
        assertEquals(true, vm.showDistance)
    }

    @Test fun setRsvpUpdatesStatus() = runTest {
        val engine = MockEngine { respond(rsvpSummaryJson(counts = rsvpCountsJson(going = 2), totalMembers = 2, myStatus = "maybe"), HttpStatusCode.OK, jsonHeader) }
        val vm = EventDetailViewModel(repo(engine))
        vm.setRsvp(5, RsvpStatusEnum.MAYBE)
        assertEquals(RsvpSummary.MyStatus.MAYBE, vm.rsvp?.myStatus)
    }

    @Test fun setRsvpDisabledSetsError() = runTest {
        val engine = MockEngine { respond("""{"detail":"disabled"}""", HttpStatusCode.Forbidden, jsonHeader) }
        val vm = EventDetailViewModel(repo(engine))
        vm.setRsvp(5, RsvpStatusEnum.GOING)
        assertEquals(StringsFr.rsvpDisabled, vm.rsvpError)
    }

    @Test fun setRotiUpdatesScore() = runTest {
        val engine = MockEngine { respond(rotiSummaryJson(average = 4.0, count = 3, myScore = 4), HttpStatusCode.OK, jsonHeader) }
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

    @Test fun downloadOpensPresignedUrl() = runTest {
        var opened: String? = null
        val engine = MockEngine { respond("""{"url":"https://s3/file.pdf"}""", HttpStatusCode.OK, jsonHeader) }
        val vm = EventDetailViewModel(repo(engine), openUrl = { opened = it })
        vm.downloadAttachment(9)
        assertEquals("https://s3/file.pdf", opened)
    }

    @Test fun downloadFailureSetsError() = runTest {
        val engine = MockEngine { respond("""{"detail":"not ready"}""", HttpStatusCode.Conflict, jsonHeader) }
        val vm = EventDetailViewModel(repo(engine))
        vm.downloadAttachment(9)
        assertEquals(StringsFr.downloadFailed, vm.attachmentError)
    }
}
