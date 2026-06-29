package com.foxugly.trainingmanager_app.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.data.api.PatchMeBody
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.Strings
import com.foxugly.trainingmanager_app.i18n.StringsFr

/**
 * Profile name + notification flags. Language is handled separately by
 * [com.foxugly.trainingmanager_app.i18n.LanguageService] (the screen's language
 * selector), so it is intentionally NOT part of this VM's save body.
 */
class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val strings: Strings = StringsFr,
) {
    var isLoading by mutableStateOf(true)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set
    var email by mutableStateOf("")
        private set

    var firstName by mutableStateOf("")
    var lastName by mutableStateOf("")
    var weeklyRecapOptIn by mutableStateOf(true)
    var digestEmail by mutableStateOf(false)

    var isSaving by mutableStateOf(false)
        private set
    var saveError by mutableStateOf<String?>(null)
        private set
    var saved by mutableStateOf(false)
        private set

    fun consumeSaved() { saved = false }

    suspend fun load() {
        isLoading = true
        loadError = null
        authRepository.getCurrentUser().fold(
            onSuccess = { u ->
                email = u.email
                firstName = u.firstName ?: ""
                lastName = u.lastName ?: ""
                weeklyRecapOptIn = u.weeklyRecapOptIn ?: true
                digestEmail = u.digestEmail ?: false
            },
            onFailure = { loadError = strings.loadFailed },
        )
        isLoading = false
    }

    suspend fun save() {
        if (isSaving) return
        isSaving = true
        saveError = null
        saved = false
        authRepository.updateProfile(
            PatchMeBody(
                firstName = firstName,
                lastName = lastName,
                weeklyRecapOptIn = weeklyRecapOptIn,
                digestEmail = digestEmail,
            ),
        ).fold(
            onSuccess = { saved = true },
            onFailure = { saveError = strings.saveFailed },
        )
        isSaving = false
    }
}
