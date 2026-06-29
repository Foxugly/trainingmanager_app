package com.foxugly.trainingmanager_app.ui.teams

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
                    respond("""{"member_teams":[{"team_id":3,"members_count":5}],"member_upcoming":[],"member_upcoming_total":0,"member_attendance_history":[]}""", HttpStatusCode.OK, jsonHeader)
                request.url.encodedPath.endsWith("teams/3/") ->
                    respond("""{"id":3,"name":"Sharks","sport":{"id":1,"name":"Natation"},"logo_url":null,"owner":{"id":1,"first_name":"Ann","last_name":"Lee"},"managers":[]}""", HttpStatusCode.OK, jsonHeader)
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val vm = TeamsListViewModel(repo(engine))
        vm.load()
        assertEquals(1, vm.teams.size)
        assertEquals("Sharks", vm.teams[0].name)
        assertEquals("Natation", vm.teams[0].sport?.name)
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
                    respond("""{"count":2,"results":[{"id":1,"firstname":"A","lastname":"X","fullname":"A X","teams":[3]},{"id":2,"firstname":"B","lastname":"Y","fullname":"B Y","teams":[9]}]}""", HttpStatusCode.OK, jsonHeader)
                else ->
                    respond("""{"id":3,"name":"Sharks","sport":{"id":1,"name":"Natation"},"logo_url":null,"owner":null,"managers":[]}""", HttpStatusCode.OK, jsonHeader)
            }
        }
        val vm = TeamDetailViewModel(repo(engine))
        vm.load(3)
        assertEquals(1, vm.members.size)
        assertEquals("A X", vm.members[0].fullname)
    }

    @Test fun detailLoadsTeam() = runTest {
        val engine = MockEngine { respond("""{"id":3,"name":"Sharks","sport":{"id":1,"name":"Natation"},"logo_url":null,"owner":{"id":1,"first_name":"Ann","last_name":"Lee"},"managers":[{"id":2,"first_name":"Bob","last_name":"K"}]}""", HttpStatusCode.OK, jsonHeader) }
        val vm = TeamDetailViewModel(repo(engine))
        vm.load(3)
        assertNotNull(vm.team)
        assertEquals("Sharks", vm.team?.name)
        assertEquals(1, vm.team?.managers?.size)
    }
}
