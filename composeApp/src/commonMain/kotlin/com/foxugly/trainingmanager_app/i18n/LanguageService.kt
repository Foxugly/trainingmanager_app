package com.foxugly.trainingmanager_app.i18n

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foxugly.trainingmanager_app.data.api.PatchMeBody
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import kotlinx.coroutines.CancellationException

/**
 * Source of truth for the active UI language. Holds Compose snapshot state so the
 * UI recomposes on change; drives [LanguageProvider] (the Accept-Language header)
 * and persists the choice via PATCH /me/.
 */
class LanguageService(
    private val authRepository: AuthRepository,
    private val languageProvider: LanguageProvider,
) {
    var activeLang by mutableStateOf("fr")
        private set

    val strings: Strings get() = stringsFor(activeLang)

    /** Set the active language locally (no network) — e.g. from /me at bootstrap/login. */
    fun setActive(lang: String) {
        if (lang !in supportedLanguages || lang == activeLang) {
            if (lang in supportedLanguages) languageProvider.activeTag = lang
            return
        }
        activeLang = lang
        languageProvider.activeTag = lang
    }

    /**
     * Optimistically switch the UI language, then persist via PATCH /me/.
     * Rolls back to the previous language if the request fails. Returns success.
     */
    suspend fun switchLanguage(lang: String): Boolean {
        if (lang !in supportedLanguages || lang == activeLang) return true
        val previous = activeLang
        setActive(lang)
        return authRepository.updateProfile(PatchMeBody(language = lang)).fold(
            onSuccess = { true },
            onFailure = {
                if (it is CancellationException) throw it
                setActive(previous)
                false
            },
        )
    }
}
