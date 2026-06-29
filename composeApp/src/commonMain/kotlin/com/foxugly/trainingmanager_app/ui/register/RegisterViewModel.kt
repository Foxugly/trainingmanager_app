package com.foxugly.trainingmanager_app.ui.register

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.data.api.ApiException
import com.foxugly.trainingmanager_app.data.api.NetworkErrorKind
import com.foxugly.trainingmanager_app.data.api.NetworkException
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.Strings
import com.foxugly.trainingmanager_app.i18n.StringsFr

const val MIN_PASSWORD_LENGTH = 8

class RegisterViewModel(
    private val authRepository: AuthRepository,
    private val language: String = "en",
    private val strings: Strings = StringsFr,
) {
    var firstName by mutableStateOf("")
    var lastName by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var turnstileToken by mutableStateOf<String?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var registered by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    val passwordValid: Boolean get() = password.length >= MIN_PASSWORD_LENGTH
    val formValid: Boolean
        get() = firstName.isNotBlank() && lastName.isNotBlank() && email.isNotBlank() && passwordValid
    val canSubmit: Boolean get() = !isLoading && formValid && turnstileToken != null

    fun clearError() { error = null }

    /** Called by the Turnstile WebView on success. */
    fun onTurnstileToken(token: String) {
        turnstileToken = token
        if (error != null) error = null
    }

    /** Called by the Turnstile WebView on error/expiry — force a fresh solve. */
    fun onTurnstileError() {
        turnstileToken = null
        error = strings.turnstileFailed
    }

    suspend fun submit() {
        val token = turnstileToken
        if (isLoading || !formValid || token == null) return
        isLoading = true
        error = null
        authRepository.register(firstName, lastName, email, password, language, token).fold(
            onSuccess = { registered = true },
            onFailure = {
                error = mapError(it)
                turnstileToken = null // a token is single-use; require a new solve on retry
            },
        )
        isLoading = false
    }

    private fun mapError(t: Throwable): String? = when {
        t is NetworkException && t.kind == NetworkErrorKind.TIMEOUT -> strings.networkTimeout
        t is NetworkException -> strings.networkOffline
        t is ApiException && t.statusCode == 429 -> strings.registerRateLimited
        t is ApiException && t.statusCode == 400 -> strings.registerInvalid
        else -> strings.registerFailed
    }
}
