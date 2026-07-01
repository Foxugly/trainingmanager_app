package com.foxugly.trainingmanager_app.data.api

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.api.generated.models.EventRequest
import com.foxugly.trainingmanager_app.api.generated.models.PatchedEventRequest
import com.foxugly.trainingmanager_app.api.generated.models.ReorderRoundsRequestRequest
import com.foxugly.trainingmanager_app.eventJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** The coach write endpoints hit the right verb + path and decode their result. */
class EventWriteApiTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun api(engine: MockEngine) =
        TrainingManagerApi(FakeTokenStore(access = "tok"), baseUrl = "https://test/api/v1/", engine = engine)

    @Test fun createEventPostsToEvents() = runTest {
        var method = ""
        var path = ""
        val api = api(MockEngine { req ->
            method = req.method.value; path = req.url.encodedPath
            respond(eventJson(id = 12, name = "Séance"), HttpStatusCode.Created, jsonHeader)
        })
        val ev = api.createEvent(EventRequest(name = "Séance", referProgramId = 3)).getOrThrow()
        assertEquals("POST", method)
        assertEquals("/api/v1/events/", path)
        assertEquals(12, ev.id)
    }

    @Test fun updateEventPatchesById() = runTest {
        var method = ""
        var path = ""
        val api = api(MockEngine { req ->
            method = req.method.value; path = req.url.encodedPath
            respond(eventJson(id = 5, name = "Modifié"), HttpStatusCode.OK, jsonHeader)
        })
        val ev = api.updateEvent(5, PatchedEventRequest(name = "Modifié")).getOrThrow()
        assertEquals("PATCH", method)
        assertEquals("/api/v1/events/5/", path)
        assertEquals("Modifié", ev.name)
    }

    @Test fun deleteEventDeletesById() = runTest {
        var method = ""
        var path = ""
        val api = api(MockEngine { req ->
            method = req.method.value; path = req.url.encodedPath
            respond("", HttpStatusCode.NoContent)
        })
        api.deleteEvent(7).getOrThrow()
        assertEquals("DELETE", method)
        assertEquals("/api/v1/events/7/", path)
    }

    @Test fun reorderRoundsPostsAndDecodesEmptyBodyAsUnit() = runTest {
        var path = ""
        val api = api(MockEngine { req ->
            path = req.url.encodedPath
            respond("", HttpStatusCode.NoContent)
        })
        api.reorderRounds(5, ReorderRoundsRequestRequest(roundIds = listOf(3, 1, 2))).getOrThrow()
        assertEquals("/api/v1/events/5/rounds/reorder/", path)
    }

    private val emptyPage = """{"count":0,"next":null,"previous":null,"results":[]}"""

    @Test fun listModalitiesHitsNestedSportPath() = runTest {
        var path = ""
        val api = api(MockEngine { req -> path = req.url.encodedPath; respond(emptyPage, HttpStatusCode.OK, jsonHeader) })
        api.listModalities(9).getOrThrow()
        assertEquals("/api/v1/sports/9/modalities/", path)
    }

    @Test fun listEnergySegmentsHitsEndpoint() = runTest {
        var path = ""
        val api = api(MockEngine { req -> path = req.url.encodedPath; respond(emptyPage, HttpStatusCode.OK, jsonHeader) })
        api.listEnergySegments().getOrThrow()
        assertEquals("/api/v1/energy-segments/", path)
    }
}
