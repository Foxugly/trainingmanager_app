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

class DashboardApiTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun api(engine: MockEngine) =
        TrainingManagerApi(FakeTokenStore(access = "tok"), baseUrl = "https://test/api/v1/", engine = engine)

    @Test fun decodesAthleteSubsetIgnoringCoachFields() = runTest {
        val body = """
            {"coach_teams":[],"coach_upcoming":[],"coach_upcoming_total":0,"coach_attendance_pending":[],
             "coach_pending_truncated":false,
             "member_teams":[{"team_id":3,"members_count":12,"my_member_id":7}],
             "member_upcoming":[{"event":{"id":5,"name":"Séance","date":"2026-07-01","hour_start":"18:00","hour_end":"19:30","location":"Gym","place":null},"team_id":3,"team_name":"Sharks","program_id":null,"program_name":"Prépa"}],
             "member_upcoming_total":1,
             "member_attendance_history":[{"event":{"id":4,"name":"Match","date":"2026-06-01","hour_start":null,"hour_end":null,"location":"","place":null},"team_id":3,"team_name":"Sharks","program_id":null,"program_name":"","attendance_id":9,"status_code":"present","status":null}],
             "member_history_truncated":false}
        """.trimIndent()
        val api = api(MockEngine { respond(body, HttpStatusCode.OK, jsonHeader) })
        val d = api.getDashboard().getOrThrow()
        assertEquals(1, d.memberTeams.size)
        assertEquals(12, d.memberTeams[0].membersCount)
        assertEquals("Sharks", d.memberUpcoming[0].teamName)
        assertEquals("Séance", d.memberUpcoming[0].event.name)
        assertEquals("present", d.memberAttendanceHistory[0].statusCode)
    }

    @Test fun surfaces401() = runTest {
        val api = api(MockEngine { respond("", HttpStatusCode.Unauthorized) })
        assertEquals(401, (api.getDashboard().exceptionOrNull() as ApiException).statusCode)
    }
}
