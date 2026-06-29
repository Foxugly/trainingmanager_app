package com.foxugly.trainingmanager_app.ui.confirm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.data.repository.AuthRepository

class EmailConfirmViewModel(private val authRepository: AuthRepository) {
    enum class State { Loading, Success, Invalid }
    var state by mutableStateOf(State.Loading)
        private set

    suspend fun confirm(key: String, onSuccess: () -> Unit) {
        state = State.Loading
        authRepository.confirmEmail(key).fold(
            onSuccess = { state = State.Success; onSuccess() },
            onFailure = { state = State.Invalid },
        )
    }
}
