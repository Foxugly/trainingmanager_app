package com.foxugly.trainingmanager_app.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.data.api.PatchMeBody
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.i18n.LanguageProvider

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val languageProvider: LanguageProvider,
) {
    var isLoading by mutableStateOf(true)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set
    var email by mutableStateOf("")
        private set

    var firstName by mutableStateOf("")
    var lastName by mutableStateOf("")
    var language by mutableStateOf("fr")
    var weeklyRecapOptIn by mutableStateOf(true)
    var digestEmail by mutableStateOf(false)

    var isSaving by mutableStateOf(false)
        private set
    var saveError by mutableStateOf<String?>(null)
        private set
    var saved by mutableStateOf(false)
        private set

    val languages: List<String> = listOf("fr", "nl", "en", "it", "es")

    fun consumeSaved() { saved = false }

    suspend fun load() {
        isLoading = true
        loadError = null
        authRepository.getCurrentUser().fold(
            onSuccess = { u ->
                email = u.email
                firstName = u.firstName ?: ""
                lastName = u.lastName ?: ""
                language = u.language ?: "fr"
                weeklyRecapOptIn = u.weeklyRecapOptIn ?: true
                digestEmail = u.digestEmail ?: false
            },
            onFailure = { loadError = ProfileStrings.loadFailed },
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
                language = language,
                weeklyRecapOptIn = weeklyRecapOptIn,
                digestEmail = digestEmail,
            ),
        ).fold(
            onSuccess = {
                saved = true
                // Persist the active Accept-Language tag; live UI re-translation is S1d-b.
                languageProvider.activeTag = language
            },
            onFailure = { saveError = ProfileStrings.saveFailed },
        )
        isSaving = false
    }
}
