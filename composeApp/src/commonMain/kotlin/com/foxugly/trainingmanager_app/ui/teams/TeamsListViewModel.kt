package com.foxugly.trainingmanager_app.ui.teams

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.api.generated.models.Team
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.Strings
import com.foxugly.trainingmanager_app.i18n.StringsFr

/**
 * The athlete's teams. /teams/ only lists managed + public teams, so we derive the
 * athlete's team ids from the dashboard (member_teams) and fetch each team's detail.
 */
class TeamsListViewModel(
    private val authRepository: AuthRepository,
    private val strings: Strings = StringsFr,
) {
    var isLoading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var teams by mutableStateOf<List<Team>>(emptyList())
        private set

    suspend fun load() {
        isLoading = true
        error = null
        authRepository.getDashboard().fold(
            onSuccess = { summary ->
                teams = summary.memberTeams.mapNotNull { authRepository.getTeam(it.teamId).getOrNull() }
            },
            onFailure = { error = strings.teamsLoadFailed },
        )
        isLoading = false
    }
}
