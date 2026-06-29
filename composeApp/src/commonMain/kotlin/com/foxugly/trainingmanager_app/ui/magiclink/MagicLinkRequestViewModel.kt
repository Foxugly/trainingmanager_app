package com.foxugly.trainingmanager_app.ui.magiclink

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.data.api.ApiException
import com.foxugly.trainingmanager_app.data.api.NetworkErrorKind
import com.foxugly.trainingmanager_app.data.api.NetworkException
import com.foxugly.trainingmanager_app.data.repository.AuthRepository

class MagicLinkRequestViewModel(private val authRepository: AuthRepository) {
    var email by mutableStateOf("")
    var isLoading by mutableStateOf(false)
        private set
    var sent by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    val canSubmit: Boolean get() = !isLoading && email.isNotBlank()

    fun clearError() { error = null }

    suspend fun submit() {
        if (isLoading || email.isBlank()) return
        isLoading = true
        error = null
        authRepository.requestMagicLink(email).fold(
            onSuccess = { sent = true },
            onFailure = { error = mapError(it) },
        )
        isLoading = false
    }

    private fun mapError(t: Throwable): String? = when {
        t is NetworkException && t.kind == NetworkErrorKind.TIMEOUT -> MagicLinkStrings.networkTimeout
        t is NetworkException -> MagicLinkStrings.networkOffline
        t is ApiException && t.statusCode == 429 -> MagicLinkStrings.rateLimited
        else -> MagicLinkStrings.requestFailed
    }
}
