package com.foxugly.trainingmanager_app.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.data.api.ApiException
import com.foxugly.trainingmanager_app.data.repository.AuthRepository

class ChangePasswordViewModel(private val authRepository: AuthRepository) {
    var currentPassword by mutableStateOf("")
    var newPassword by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var success by mutableStateOf(false)
        private set

    val canSubmit: Boolean
        get() = !isLoading && currentPassword.isNotBlank() && newPassword.length >= 8 && confirmPassword.isNotBlank()

    fun clearError() { error = null }

    suspend fun submit() {
        if (isLoading) return
        if (newPassword.length < 8) { error = ProfileStrings.tooShort; return }
        if (newPassword != confirmPassword) { error = ProfileStrings.mismatch; return }
        isLoading = true
        error = null
        authRepository.changePassword(currentPassword, newPassword).fold(
            onSuccess = { success = true },
            onFailure = { error = mapError(it) },
        )
        isLoading = false
    }

    private fun mapError(t: Throwable): String {
        if (t is ApiException && t.statusCode == 400) {
            val m = t.message ?: ""
            return when {
                m.contains("current_password_invalid") -> ProfileStrings.cpCurrentInvalid
                m.contains("password_unchanged") -> ProfileStrings.cpUnchanged
                else -> ProfileStrings.cpWeak
            }
        }
        return ProfileStrings.cpFailed
    }
}
