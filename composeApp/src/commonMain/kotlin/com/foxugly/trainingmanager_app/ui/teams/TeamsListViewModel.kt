package com.foxugly.trainingmanager_app.ui.teams

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.api.generated.models.Team
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.Strings
import com.foxugly.trainingmanager_app.i18n.StringsFr

/**
 * The user's teams — both the ones they're a member of AND the ones they
 * manage/own. /teams/ only lists managed + public teams (never member-only), so
 * we take the team ids from the dashboard (member_teams + coach_teams), dedupe
 * (a user can be both member and coach of a team), and fetch each team's detail.
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
                val ids = (summary.memberTeams.map { it.teamId } + summary.coachTeams.map { it.teamId }).distinct()
                teams = ids.mapNotNull { authRepository.getTeam(it).getOrNull() }
            },
            onFailure = { error = strings.teamsLoadFailed },
        )
        isLoading = false
    }
}
