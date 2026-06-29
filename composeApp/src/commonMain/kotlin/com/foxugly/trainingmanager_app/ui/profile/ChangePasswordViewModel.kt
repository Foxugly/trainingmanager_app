package com.foxugly.trainingmanager_app.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.data.api.ApiException
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.Strings
import com.foxugly.trainingmanager_app.i18n.StringsFr

class ChangePasswordViewModel(
    private val authRepository: AuthRepository,
    private val strings: Strings = StringsFr,
) {
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
        if (newPassword.length < 8) { error = strings.tooShort; return }
        if (newPassword != confirmPassword) { error = strings.mismatch; return }
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
                m.contains("current_password_invalid") -> strings.cpCurrentInvalid
                m.contains("password_unchanged") -> strings.cpUnchanged
                else -> strings.cpWeak
            }
        }
        return strings.cpFailed
    }
}
