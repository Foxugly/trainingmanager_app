package com.foxugly.trainingmanager_app.ui.magiclink

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.data.api.ApiException
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.data.repository.PendingLogin

/**
 * Magic-link sign-in. The link can be opened from anywhere (mail, browser,
 * another app), so the exchange never signs in silently: it yields a pending
 * session, the screen shows which account it belongs to (and the account it
 * would replace), and only [confirm] makes it active. [cancel] discards it.
 */
class MagicLinkExchangeViewModel(private val authRepository: AuthRepository) {
    enum class ExchangeState { Loading, AwaitingConfirmation, Success, Expired, Invalid }

    var state by mutableStateOf(ExchangeState.Loading)
        private set

    /** Account the link signs into (shown before confirming). */
    var targetEmail by mutableStateOf<String?>(null)
        private set

    /** Account currently signed in on this device, if any (it would be replaced). */
    var currentEmail by mutableStateOf<String?>(null)
        private set

    private var pending: PendingLogin? = null

    /** Exchange the token once and wait for the user's confirmation. */
    suspend fun exchange(token: String) {
        state = ExchangeState.Loading
        authRepository.previewMagicLink(token).fold(
            onSuccess = { p ->
                pending = p
                targetEmail = p.me.email
                currentEmail = authRepository.currentAccountEmail()
                state = ExchangeState.AwaitingConfirmation
            },
            onFailure = { t ->
                state = if (t is ApiException && t.statusCode == 410) ExchangeState.Expired
                else ExchangeState.Invalid
            },
        )
    }

    /** The user accepted: make the pending session active, then [onSuccess] (navigate Home). */
    suspend fun confirm(onSuccess: () -> Unit) {
        val p = pending ?: return
        pending = null
        authRepository.commitPendingLogin(p)
        state = ExchangeState.Success
        onSuccess()
    }

    /** The user declined: drop the pending tokens, the current session (if any) is untouched. */
    fun cancel() {
        pending = null
    }
}
