package com.foxugly.trainingmanager_app.ui.invitation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.data.api.ApiException
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.Strings
import com.foxugly.trainingmanager_app.i18n.StringsFr

class InvitationViewModel(
    private val authRepository: AuthRepository,
    private val strings: Strings = StringsFr,
) {
    var isLoading by mutableStateOf(true)
        private set
    var teamName by mutableStateOf<String?>(null)
        private set
    var status by mutableStateOf<String?>(null)
        private set
    var lookupError by mutableStateOf<String?>(null)
        private set

    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var isSubmitting by mutableStateOf(false)
        private set
    var submitError by mutableStateOf<String?>(null)
        private set

    val isPending: Boolean get() = status == "pending"
    val canSubmit: Boolean get() = isPending && !isSubmitting && password.length >= 8 && confirmPassword.isNotBlank()

    fun clearSubmitError() { submitError = null }

    suspend fun load(token: String) {
        isLoading = true
        lookupError = null
        authRepository.lookupInvitation(token).fold(
            onSuccess = { teamName = it.teamName; status = it.status },
            onFailure = { lookupError = strings.invitationLookupFailed },
        )
        isLoading = false
    }

    suspend fun accept(token: String, onSuccess: () -> Unit) {
        if (isSubmitting) return
        if (password.length < 8) { submitError = strings.tooShort; return }
        if (password != confirmPassword) { submitError = strings.mismatch; return }
        isSubmitting = true
        submitError = null
        authRepository.acceptInvitation(token, password).fold(
            onSuccess = { onSuccess() },
            onFailure = { t ->
                submitError = if (t is ApiException && t.statusCode == 409) strings.invitationEmailTaken
                else strings.invitationJoinFailed
            },
        )
        isSubmitting = false
    }
}
