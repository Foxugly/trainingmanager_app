package com.foxugly.trainingmanager_app.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.data.repository.AuthRepository

/**
 * Plain testable state holder for the Login screen (Compose snapshot state, not
 * an androidx ViewModel — mirrors PushIT's SessionViewModel: no lifecycle to
 * manage, constructs trivially in tests; suspend work runs on the caller's scope).
 */
class LoginViewModel(private val authRepository: AuthRepository) {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var rememberMe by mutableStateOf(false)
    var passwordVisible by mutableStateOf(false)

    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    val canSubmit: Boolean
        get() = !isLoading && email.isNotBlank() && password.isNotBlank()

    fun clearError() { error = null }

    suspend fun submit(onSuccess: () -> Unit) {
        if (isLoading || email.isBlank() || password.isBlank()) return
        isLoading = true
        error = null
        authRepository.login(email.trim(), password, rememberMe).fold(
            onSuccess = { onSuccess() },
            onFailure = { error = mapLoginError(it) },
        )
        isLoading = false
    }
}
