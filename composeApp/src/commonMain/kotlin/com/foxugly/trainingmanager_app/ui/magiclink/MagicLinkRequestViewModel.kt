package com.foxugly.trainingmanager_app.ui.magiclink

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.data.api.ApiException
import com.foxugly.trainingmanager_app.data.api.NetworkErrorKind
import com.foxugly.trainingmanager_app.data.api.NetworkException
import com.foxugly.trainingmanager_app.data.api.isCaptchaFailed
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.Strings
import com.foxugly.trainingmanager_app.i18n.StringsFr

class MagicLinkRequestViewModel(
    private val authRepository: AuthRepository,
    private val strings: Strings = StringsFr,
) {
    var email by mutableStateOf("")
    var turnstileToken by mutableStateOf<String?>(null)
        private set
    /** Bumped when a request fails so the screen re-creates the Turnstile widget (new challenge). */
    var turnstileResetKey by mutableStateOf(0)
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
        authRepository.requestMagicLink(email, token).fold(
            onSuccess = { sent = true },
            onFailure = {
                error = mapError(it)
                // Turnstile tokens are single-use: force a fresh challenge.
                turnstileToken = null
                turnstileResetKey++
            },
        )
        isLoading = false
    }

    private fun mapError(t: Throwable): String? = when {
        t is NetworkException && t.kind == NetworkErrorKind.TIMEOUT -> strings.networkTimeout
        t is NetworkException -> strings.networkOffline
        t.isCaptchaFailed -> strings.turnstileFailed
        t is ApiException && t.statusCode == 429 -> strings.magicRateLimited
        else -> strings.magicRequestFailed
    }
}
