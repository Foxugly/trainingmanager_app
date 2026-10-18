package com.foxugly.trainingmanager_app.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.data.api.ApiException
import com.foxugly.trainingmanager_app.data.api.NetworkException
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.Strings
import com.foxugly.trainingmanager_app.i18n.StringsFr

/**
 * In-app account deletion (Play / App Store requirement for apps that let users
 * create an account). POST auth/account/delete/ {current_password}; on success
 * the local session is already wiped by the repository and [submit] reports true
 * so the screen navigates to Login.
 */
class DeleteAccountViewModel(
    private val authRepository: AuthRepository,
    private val strings: Strings = StringsFr,
) {
    var currentPassword by mutableStateOf("")
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    val canSubmit: Boolean
        get() = !isLoading && currentPassword.isNotBlank()

    fun clearError() { error = null }

    /** Returns true once the account is deleted (the caller then leaves for Login). */
    suspend fun submit(): Boolean {
        if (!canSubmit) return false
        isLoading = true
        error = null
        val result = authRepository.deleteAccount(currentPassword)
        isLoading = false
        return result.fold(
            onSuccess = { currentPassword = ""; true },
            onFailure = { error = mapError(it); false },
        )
    }

    private fun mapError(t: Throwable): String = when {
        t is ApiException && t.errorCode == "current_password_invalid" -> strings.cpCurrentInvalid
        t is ApiException && (t.statusCode == 409 || t.errorCode == "owns_teams") -> strings.deleteAccountOwnsTeams
        t is NetworkException -> strings.networkOffline
        else -> strings.deleteAccountFailed
    }
}
