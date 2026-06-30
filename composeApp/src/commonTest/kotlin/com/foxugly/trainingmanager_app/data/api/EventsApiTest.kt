package com.foxugly.trainingmanager_app.data.api

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.api.generated.models.RsvpStatusEnum
import com.foxugly.trainingmanager_app.api.generated.models.RsvpSummary
import com.foxugly.trainingmanager_app.api.generated.models.RsvpUpsertRequest
import com.foxugly.trainingmanager_app.api.generated.models.VisibilityMode
import com.foxugly.trainingmanager_app.eventJson
import com.foxugly.trainingmanager_app.eventListJson
import com.foxugly.trainingmanager_app.placeMinimalJson
import com.foxugly.trainingmanager_app.programMinimalJson
import com.foxugly.trainingmanager_app.roundDetailJson
import com.foxugly.trainingmanager_app.rsvpCountsJson
import com.foxugly.trainingmanager_app.rsvpSummaryJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class EventsApiTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun api(engine: MockEngine) =
        TrainingManagerApi(FakeTokenStore(access = "tok"), baseUrl = "https://test/api/v1/", engine = engine)

    @Test fun listDecodesResultsFromWrapper() = runTest {
        val body = eventListJson(eventJson(id = 5, name = "Séance", referProgram = programMinimalJson(name = "Prépa")))
        val api = api(MockEngine { respond(body, HttpStatusCode.OK, jsonHeader) })
        val list = api.listEvents().getOrThrow()
        assertEquals(1, list.count)
        assertEquals("Séance", list.results[0].name)
        assertEquals("Prépa", list.results[0].referProgram.name)
    }

    @Test fun detailDecodesEvent() = runTest {
        val body = eventJson(
            id = 5,
            name = "Match",
            place = placeMinimalJson(name = "Stade X", address = "Rue Y"),
            location = "Stade",
            visGoal = "always",
        )
        val api = api(MockEngine { respond(body, HttpStatusCode.OK, jsonHeader) })
        val e = api.getEvent(5).getOrThrow()
        assertEquals("Stade X", e.place?.name)
        assertEquals(VisibilityMode.ALWAYS, e.visGoal)
    }

    @Test fun detailDecodesRoundsAndExercises() = runTest {
        val body = eventJson(
            id = 5,
            name = "S",
            location = "",
            visRounds = "always",
            roundsDetail = "[${roundDetailJson(id = 1, order = 1, count = 3)}]",
        )
        val api = api(MockEngine { respond(body, HttpStatusCode.OK, jsonHeader) })
        val e = api.getEvent(5).getOrThrow()
        assertEquals(1, e.roundsDetail.size)
        assertEquals(3, e.roundsDetail[0].count)
        assertEquals(100L, e.roundsDetail[0].exercises[0].distance)
        assertEquals("Crawl", e.roundsDetail[0].exercises[0].modality.name)
        assertEquals("PMA", e.roundsDetail[0].exercises[0].energysegment.abv)
    }

    @Test fun setRsvpDecodesMyStatus() = runTest {
        val body = rsvpSummaryJson(
            counts = rsvpCountsJson(going = 3, maybe = 1, noResponse = 2),
            totalMembers = 6,
            myStatus = "going",
        )
        val api = api(MockEngine { respond(body, HttpStatusCode.OK, jsonHeader) })
        val r = api.setRsvp(5, RsvpUpsertRequest(RsvpStatusEnum.GOING)).getOrThrow()
        assertEquals(RsvpSummary.MyStatus.GOING, r.myStatus)
        assertEquals(3, r.counts.going)
    }

    @Test fun setRsvpDisabledSurfaces403() = runTest {
        val api = api(MockEngine { respond("""{"detail":"rsvp disabled"}""", HttpStatusCode.Forbidden, jsonHeader) })
        assertEquals(403, (api.setRsvp(5, RsvpUpsertRequest(RsvpStatusEnum.GOING)).exceptionOrNull() as ApiException).statusCode)
    }
}
