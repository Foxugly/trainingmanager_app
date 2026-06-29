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

class EventsApiTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun api(engine: MockEngine) =
        TrainingManagerApi(FakeTokenStore(access = "tok"), baseUrl = "https://test/api/v1/", engine = engine)

    @Test fun listDecodesResultsFromWrapper() = runTest {
        val body = """{"count":1,"next":null,"previous":null,"results":[{"id":5,"name":"Séance","location":"Gym","equipment":"","total":12,"place":null,"refer_program":{"id":2,"name":"Prépa"},"vis_distance":"always","vis_goal":"after","vis_rounds":"never","debrief":""}]}"""
        val api = api(MockEngine { respond(body, HttpStatusCode.OK, jsonHeader) })
        val list = api.listEvents().getOrThrow()
        assertEquals(1, list.count)
        assertEquals("Séance", list.results[0].name)
        assertEquals("Prépa", list.results[0].referProgram?.name)
    }

    @Test fun detailDecodesEvent() = runTest {
        val api = api(MockEngine { respond("""{"id":5,"name":"Match","location":"Stade","equipment":"","total":0,"place":{"id":1,"name":"Stade X","address":"Rue Y"},"refer_program":null,"vis_distance":"never","vis_goal":"always","vis_rounds":"never","debrief":""}""", HttpStatusCode.OK, jsonHeader) })
        val e = api.getEvent(5).getOrThrow()
        assertEquals("Stade X", e.place?.name)
        assertEquals("always", e.visGoal)
    }

    @Test fun detailDecodesRoundsAndExercises() = runTest {
        val body = """{"id":5,"name":"S","location":"","equipment":"","total":0,"place":null,"refer_program":null,"vis_distance":"never","vis_goal":"never","vis_rounds":"always","debrief":"","rounds_detail":[{"id":1,"order":1,"count":3,"t_start":null,"t_break":null,"sport":{"id":1},"exercises":[{"id":9,"order":1,"repetition":4,"distance":100,"notes":"sprint","t_start":null,"t_break":null,"modality":{"id":2,"name":"Crawl"},"energysegment":{"id":3,"abv":"PMA"}}]}]}"""
        val api = api(MockEngine { respond(body, HttpStatusCode.OK, jsonHeader) })
        val e = api.getEvent(5).getOrThrow()
        assertEquals(1, e.roundsDetail.size)
        assertEquals(3, e.roundsDetail[0].count)
        assertEquals(100L, e.roundsDetail[0].exercises[0].distance)
        assertEquals("Crawl", e.roundsDetail[0].exercises[0].modality?.name)
        assertEquals("PMA", e.roundsDetail[0].exercises[0].energysegment?.abv)
    }

    @Test fun setRsvpDecodesMyStatus() = runTest {
        val api = api(MockEngine { respond("""{"counts":{"going":3,"maybe":1,"not_going":0,"no_response":2},"total_members":6,"my_status":"going","by_member":[]}""", HttpStatusCode.OK, jsonHeader) })
        val r = api.setRsvp(5, RsvpUpsertRequest("going")).getOrThrow()
        assertEquals("going", r.myStatus)
        assertEquals(3, r.counts.going)
    }

    @Test fun setRsvpDisabledSurfaces403() = runTest {
        val api = api(MockEngine { respond("""{"detail":"rsvp disabled"}""", HttpStatusCode.Forbidden, jsonHeader) })
        assertEquals(403, (api.setRsvp(5, RsvpUpsertRequest("going")).exceptionOrNull() as ApiException).statusCode)
    }
}
