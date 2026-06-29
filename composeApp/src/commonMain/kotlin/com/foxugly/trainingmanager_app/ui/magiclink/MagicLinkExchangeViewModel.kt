package com.foxugly.trainingmanager_app.ui.magiclink

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.data.api.ApiException
import com.foxugly.trainingmanager_app.data.repository.AuthRepository

class MagicLinkExchangeViewModel(private val authRepository: AuthRepository) {
    enum class ExchangeState { Loading, Success, Expired, Invalid }

    var state by mutableStateOf(ExchangeState.Loading)
        private set

    /** Exchange the token once; on success invoke [onSuccess] (navigate Home). */
    suspend fun exchange(token: String, onSuccess: () -> Unit) {
        state = ExchangeState.Loading
        authRepository.exchangeMagicLink(token).fold(
            onSuccess = { state = ExchangeState.Success; onSuccess() },
            onFailure = { t ->
                state = if (t is ApiException && t.statusCode == 410) ExchangeState.Expired
                else ExchangeState.Invalid
            },
        )
    }
}
