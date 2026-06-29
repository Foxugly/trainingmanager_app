package com.foxugly.trainingmanager_app.ui.forgot

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.data.api.NetworkErrorKind
import com.foxugly.trainingmanager_app.data.api.NetworkException
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.Strings
import com.foxugly.trainingmanager_app.i18n.StringsFr

class ForgotPasswordViewModel(
    private val authRepository: AuthRepository,
    private val strings: Strings = StringsFr,
) {
    var email by mutableStateOf("")
    var turnstileToken by mutableStateOf<String?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var sent by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    val canSubmit: Boolean get() = !isLoading && email.isNotBlank() && turnstileToken != null

    fun clearError() { error = null }

    fun onTurnstileToken(token: String) {
        turnstileToken = token
        if (error != null) error = null
    }

    fun onTurnstileError() {
        turnstileToken = null
        error = strings.turnstileFailed
    }

    suspend fun submit() {
        val token = turnstileToken
        if (isLoading || email.isBlank() || token == null) return
        isLoading = true
        error = null
        // Always-200 server-side (no account enumeration); we only surface
        // transport-level failures.
        authRepository.requestPasswordReset(email, token).fold(
            onSuccess = { sent = true },
            onFailure = {
                error = mapError(it)
                turnstileToken = null
            },
        )
        isLoading = false
    }

    private fun mapError(t: Throwable): String? = when {
        t is NetworkException && t.kind == NetworkErrorKind.TIMEOUT -> strings.networkTimeout
        t is NetworkException -> strings.networkOffline
        else -> strings.forgotFailed
    }
}
