package com.foxugly.trainingmanager_app.ui.teams

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.api.generated.models.Program
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.Strings
import com.foxugly.trainingmanager_app.i18n.StringsFr

class ProgramDetailViewModel(
    private val authRepository: AuthRepository,
    private val strings: Strings = StringsFr,
) {
    var isLoading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var program by mutableStateOf<Program?>(null)
        private set

    suspend fun load(id: Int) {
        isLoading = true
        error = null
        authRepository.getProgram(id).fold(
            onSuccess = { program = it },
            onFailure = { error = strings.programLoadFailed },
        )
        isLoading = false
    }
}
