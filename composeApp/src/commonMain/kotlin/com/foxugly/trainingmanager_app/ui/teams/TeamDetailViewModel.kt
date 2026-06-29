package com.foxugly.trainingmanager_app.ui.teams

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.data.api.TeamDto
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.Strings
import com.foxugly.trainingmanager_app.i18n.StringsFr

class TeamDetailViewModel(
    private val authRepository: AuthRepository,
    private val strings: Strings = StringsFr,
) {
    var isLoading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var team by mutableStateOf<TeamDto?>(null)
        private set

    suspend fun load(id: Int) {
        isLoading = true
        error = null
        authRepository.getTeam(id).fold(
            onSuccess = { team = it },
            onFailure = { error = strings.teamLoadFailed },
        )
        isLoading = false
    }
}
