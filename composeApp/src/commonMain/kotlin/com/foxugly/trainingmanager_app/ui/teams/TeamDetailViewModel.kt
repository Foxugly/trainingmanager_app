package com.foxugly.trainingmanager_app.ui.teams

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.api.generated.models.Member
import com.foxugly.trainingmanager_app.api.generated.models.Program
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
    private var teamId: Int = 0

    var isLoading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var team by mutableStateOf<Team?>(null)
        private set
    var members by mutableStateOf<List<Member>>(emptyList())
        private set
    var programs by mutableStateOf<List<Program>>(emptyList())
        private set
    var isSavingProgram by mutableStateOf(false)
        private set
    var programError by mutableStateOf<String?>(null)
        private set

    /** True if the caller owns/manages this team — gates the coach affordances. */
    var canManage by mutableStateOf(false)
        private set

    suspend fun load(id: Int) {
        teamId = id
        isLoading = true
        error = null
        // getTeam, listMembers, me and programs are independent; run them concurrently.
        coroutineScope {
            val teamDeferred = async { authRepository.getTeam(id) }
            val membersDeferred = async { authRepository.listMembers() }
            val meDeferred = async { authRepository.getCurrentUser() }
            val programsDeferred = async { authRepository.listPrograms(id) }
            teamDeferred.await().fold(
                onSuccess = { team = it },
                onFailure = { error = strings.teamLoadFailed },
            )
            canManage = team?.isManagedBy(meDeferred.await().getOrNull()?.id) == true
            // Roster is best-effort: /members/ is scoped to the caller's teams; filter to this one.
            membersDeferred.await().onSuccess { page ->
                members = page.results.filter { it.teams.contains(id) }
            }
            programsDeferred.await().onSuccess { programs = it.results }
        }
        isLoading = false
    }

    suspend fun addProgram(name: String) {
        if (name.isBlank()) return
        isSavingProgram = true
        programError = null
        authRepository.createProgram(teamId, name).fold(
            onSuccess = { authRepository.listPrograms(teamId).onSuccess { programs = it.results } },
            onFailure = { programError = strings.programSaveFailed },
        )
        isSavingProgram = false
    }
}
