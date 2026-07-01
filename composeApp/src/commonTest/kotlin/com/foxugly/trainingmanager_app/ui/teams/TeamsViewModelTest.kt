package com.foxugly.trainingmanager_app.ui.teams

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.customUserPublicJson
import com.foxugly.trainingmanager_app.dashboardMemberTeamJson
import com.foxugly.trainingmanager_app.dashboardSummaryJson
import com.foxugly.trainingmanager_app.data.api.TrainingManagerApi
import com.foxugly.trainingmanager_app.memberJson
import com.foxugly.trainingmanager_app.memberListJson
import com.foxugly.trainingmanager_app.teamJson
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

class TeamsViewModelTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun repo(engine: MockEngine): AuthRepository {
        val s = FakeTokenStore(access = "tok")
        return AuthRepository(TrainingManagerApi(s, baseUrl = "https://test/api/v1/", engine = engine), s)
    }

    @Test fun listResolvesTeamsFromDashboard() = runTest {
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("dashboard/summary/") ->
                    respond(
                        dashboardSummaryJson(
                            memberTeams = "[${dashboardMemberTeamJson(teamId = 3, membersCount = 5)}]",
                        ),
                        HttpStatusCode.OK,
                        jsonHeader,
                    )
                request.url.encodedPath.endsWith("teams/3/") ->
                    respond(teamJson(id = 3, name = "Sharks"), HttpStatusCode.OK, jsonHeader)
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val vm = TeamsListViewModel(repo(engine))
        vm.load()
        assertEquals(1, vm.teams.size)
        assertEquals("Sharks", vm.teams[0].name)
        assertEquals("Natation", vm.teams[0].sport.name)
    }

    @Test fun listIncludesManagedTeamsAndDedupes() = runTest {
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("dashboard/summary/") ->
                    respond(
                        dashboardSummaryJson(
                            memberTeams = "[${dashboardMemberTeamJson(teamId = 3, membersCount = 5)}]",
                            // team 3 is also managed (dedupe), team 7 is coach-only.
                            coachTeams = """[{"team_id":3,"programs_active":1,"events_next_7d":0,"members_count":5},""" +
                                """{"team_id":7,"programs_active":2,"events_next_7d":1,"members_count":8}]""",
                        ),
                        HttpStatusCode.OK,
                        jsonHeader,
                    )
                request.url.encodedPath.endsWith("teams/3/") ->
                    respond(teamJson(id = 3, name = "Sharks"), HttpStatusCode.OK, jsonHeader)
                request.url.encodedPath.endsWith("teams/7/") ->
                    respond(teamJson(id = 7, name = "Dolphins"), HttpStatusCode.OK, jsonHeader)
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val vm = TeamsListViewModel(repo(engine))
        vm.load()
        assertEquals(2, vm.teams.size)
        assertEquals(setOf("Sharks", "Dolphins"), vm.teams.map { it.name }.toSet())
    }

    @Test fun listFailureSetsError() = runTest {
        val vm = TeamsListViewModel(repo(MockEngine { respond("", HttpStatusCode.InternalServerError) }))
        vm.load()
        assertEquals(StringsFr.teamsLoadFailed, vm.error)
    }

    @Test fun detailLoadsRosterFilteredByTeam() = runTest {
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("members/") ->
                    respond(
                        memberListJson(
                            memberJson(id = 1, firstname = "A", lastname = "X", fullname = "A X", teams = "[3]"),
                            memberJson(id = 2, firstname = "B", lastname = "Y", fullname = "B Y", teams = "[9]"),
                        ),
                        HttpStatusCode.OK,
                        jsonHeader,
                    )
                else ->
                    respond(teamJson(id = 3, name = "Sharks"), HttpStatusCode.OK, jsonHeader)
            }
        }
        val vm = TeamDetailViewModel(repo(engine))
        vm.load(3)
        assertEquals(1, vm.members.size)
        assertEquals("A X", vm.members[0].fullname)
    }

    @Test fun detailLoadsTeam() = runTest {
        val engine = MockEngine {
            respond(
                teamJson(
                    id = 3,
                    name = "Sharks",
                    managers = "[${customUserPublicJson(id = 2, firstName = "Bob", lastName = "K")}]",
                ),
                HttpStatusCode.OK,
                jsonHeader,
            )
        }
        val vm = TeamDetailViewModel(repo(engine))
        vm.load(3)
        assertNotNull(vm.team)
        assertEquals("Sharks", vm.team?.name)
        assertEquals(1, vm.team?.managers?.size)
    }
}
