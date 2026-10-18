package com.foxugly.trainingmanager_app.ui.confirm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.data.repository.PendingLogin

/**
 * E-mail confirmation link. Confirming the address happens server-side as soon
 * as the key is posted; the sign-in that comes with it is only applied after
 * the user confirms which account it is (see MagicLinkExchangeViewModel).
 */
class EmailConfirmViewModel(private val authRepository: AuthRepository) {
    enum class State { Loading, AwaitingConfirmation, Success, Invalid }
    var state by mutableStateOf(State.Loading)
        private set
    var targetEmail by mutableStateOf<String?>(null)
        private set
    var currentEmail by mutableStateOf<String?>(null)
        private set

    private var pending: PendingLogin? = null

    suspend fun confirm(key: String) {
        state = State.Loading
        authRepository.previewEmailConfirm(key).fold(
            onSuccess = { p ->
                pending = p
                targetEmail = p.me.email
                currentEmail = authRepository.currentAccountEmail()
                state = State.AwaitingConfirmation
            },
            onFailure = { state = State.Invalid },
        )
    }

    suspend fun signIn(onSuccess: () -> Unit) {
        val p = pending ?: return
        pending = null
        authRepository.commitPendingLogin(p)
        state = State.Success
        onSuccess()
    }

    fun cancel() {
        pending = null
    }
}
