package com.foxugly.trainingmanager_app.ui.confirm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.data.api.ApiException
import com.foxugly.trainingmanager_app.data.repository.AuthRepository

class ResetPasswordViewModel(private val authRepository: AuthRepository) {
    var newPassword by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var tokenInvalid by mutableStateOf(false)
        private set

    val canSubmit: Boolean get() = !isLoading && newPassword.length >= 8 && confirmPassword.isNotBlank()

    fun clearError() { error = null }

    /** Validate locally, then submit; on success auto-login + [onSuccess]. */
    suspend fun submit(key: String, onSuccess: () -> Unit) {
        if (isLoading) return
        if (newPassword.length < 8) { error = ConfirmStrings.tooShort; return }
        if (newPassword != confirmPassword) { error = ConfirmStrings.mismatch; return }
        isLoading = true
        error = null
        authRepository.confirmPasswordReset(key, newPassword).fold(
            onSuccess = { onSuccess() },
            onFailure = { t ->
                if (t is ApiException && t.statusCode == 400 && t.message?.contains("invalid_or_expired_token") == true) {
                    tokenInvalid = true
                } else {
                    error = ConfirmStrings.resetFailed
                }
            },
        )
        isLoading = false
    }
}
