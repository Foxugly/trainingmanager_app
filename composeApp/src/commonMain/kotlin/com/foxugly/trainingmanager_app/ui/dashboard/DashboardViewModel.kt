package com.foxugly.trainingmanager_app.ui.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.api.generated.models.DashboardSummary
import com.foxugly.trainingmanager_app.api.generated.models.Team
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.Strings
import com.foxugly.trainingmanager_app.i18n.StringsFr

class DashboardViewModel(
    private val authRepository: AuthRepository,
    private val strings: Strings = StringsFr,
) {
    var isLoading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var summary by mutableStateOf<DashboardSummary?>(null)
        private set

    /** All the caller's teams (member + managed), deduped. The dashboard payload
     *  carries only team ids, so we resolve each team's detail for name/sport. */
    var teams by mutableStateOf<List<Team>>(emptyList())
        private set

    suspend fun load() {
        isLoading = true
        error = null
        authRepository.getDashboard().fold(
            onSuccess = { s ->
                summary = s
                val ids = (s.memberTeams.map { it.teamId } + s.coachTeams.map { it.teamId }).distinct()
                teams = ids.mapNotNull { authRepository.getTeam(it).getOrNull() }
            },
            onFailure = { error = strings.dashboardLoadFailed },
        )
        isLoading = false
    }
}
