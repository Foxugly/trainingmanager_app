package com.foxugly.trainingmanager_app.ui.dashboard

import com.foxugly.trainingmanager_app.FakeTokenStore
import com.foxugly.trainingmanager_app.dashboardMemberTeamJson
import com.foxugly.trainingmanager_app.dashboardSummaryJson
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class DashboardViewModelTest {
    private val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
    private fun vm(engine: MockEngine): DashboardViewModel {
        val s = FakeTokenStore(access = "tok")
        return DashboardViewModel(AuthRepository(TrainingManagerApi(s, baseUrl = "https://test/api/v1/", engine = engine), s))
    }

    @Test fun loadSuccessPopulatesSummary() = runTest {
        val body = dashboardSummaryJson(
            memberTeams = "[${dashboardMemberTeamJson(teamId = 1, membersCount = 4)}]",
        )
        val sut = vm(MockEngine { respond(body, HttpStatusCode.OK, jsonHeader) })
        sut.load()
        assertNotNull(sut.summary)
        assertEquals(1, sut.summary?.memberTeams?.size)
        assertFalse(sut.isLoading)
    }

    @Test fun loadFailureSetsError() = runTest {
        val sut = vm(MockEngine { respond("", HttpStatusCode.InternalServerError) })
        sut.load()
        assertEquals(StringsFr.dashboardLoadFailed, sut.error)
    }
}
