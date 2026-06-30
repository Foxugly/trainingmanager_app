package com.foxugly.trainingmanager_app.ui.teams

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.api.generated.models.Member
import com.foxugly.trainingmanager_app.api.generated.models.Team
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.Strings
import com.foxugly.trainingmanager_app.i18n.StringsFr
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class TeamDetailViewModel(
    private val authRepository: AuthRepository,
    private val strings: Strings = StringsFr,
) {
    var isLoading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var team by mutableStateOf<Team?>(null)
        private set
    var members by mutableStateOf<List<Member>>(emptyList())
        private set

    suspend fun load(id: Int) {
        isLoading = true
        error = null
        // getTeam and listMembers are independent; run them concurrently.
        coroutineScope {
            val teamDeferred = async { authRepository.getTeam(id) }
            val membersDeferred = async { authRepository.listMembers() }
            teamDeferred.await().fold(
                onSuccess = { team = it },
                onFailure = { error = strings.teamLoadFailed },
            )
            // Roster is best-effort: /members/ is scoped to the caller's teams; filter to this one.
            membersDeferred.await().onSuccess { page ->
                members = page.results.filter { it.teams.contains(id) }
            }
        }
        isLoading = false
    }
}
