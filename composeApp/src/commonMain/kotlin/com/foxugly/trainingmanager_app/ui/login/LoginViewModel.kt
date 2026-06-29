package com.foxugly.trainingmanager_app.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.Strings
import com.foxugly.trainingmanager_app.i18n.StringsFr

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val strings: Strings = StringsFr,
) {
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
            onFailure = { error = mapLoginError(it, strings) },
        )
        isLoading = false
    }
}
