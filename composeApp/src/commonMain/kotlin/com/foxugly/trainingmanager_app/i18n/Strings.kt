package com.foxugly.trainingmanager_app.i18n

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * All user-facing UI strings, one property per message. Implemented once per
 * locale ([StringsFr], [StringsNl], [StringsEn], [StringsIt], [StringsEs]).
 * The active set is provided through [LocalStrings] (UI) and read directly off
 * [LanguageService.strings] in ViewModels (error messages). French is the source.
 */
interface Strings {
    // Common
    val appName: String
    val email: String
    val password: String
    val newPassword: String
    val confirmPassword: String
    val back: String
    val backToLogin: String
    val submit: String
    val mismatch: String
    val tooShort: String
    val networkOffline: String
    val networkTimeout: String
    val serverError: String

    // Login
    val loginTitle: String
    val rememberMe: String
    val loginAction: String
    val showPassword: String
    val hidePassword: String
    val invalidCredentials: String
    val emailNotVerified: String
    val loginFailed: String
    val signInByEmailLink: String

    // Magic link
    val magicRequestTitle: String
    val magicSend: String
    val magicSentTitle: String
    val magicSentBody: String
    val magicRateLimited: String
    val magicRequestFailed: String
    val magicExchangeLoading: String
    val magicExpiredTitle: String
    val magicExpiredBody: String
    val magicInvalidTitle: String
    val magicInvalidBody: String

    // Email confirm
    val emailConfirmLoading: String
    val emailInvalidTitle: String
    val emailInvalidBody: String

    // Reset password
    val resetTitle: String
    val resetFailed: String
    val tokenInvalidTitle: String
    val tokenInvalidBody: String

    // Invitation
    val invitationLoadingTitle: String
    val invitationJoin: String
    val invitationAlreadyHandledTitle: String
    val invitationAlreadyHandledBody: String
    val invitationEmailTaken: String
    val invitationLookupFailed: String
    val invitationJoinFailed: String
    fun invitationJoinTitle(team: String): String

    // Profile
    val profileTitle: String
    val emailLabel: String
    val firstName: String
    val lastName: String
    val languageLabel: String
    val weeklyRecap: String
    val digestEmail: String
    val save: String
    val saved: String
    val loadFailed: String
    val saveFailed: String
    val changePasswordCta: String

    // Change password
    val cpTitle: String
    val currentPassword: String
    val cpSubmit: String
    val cpCurrentInvalid: String
    val cpUnchanged: String
    val cpWeak: String
    val cpSuccess: String
    val cpFailed: String

    // Home
    val homeSubtitle: String
    val logout: String

    // Dashboard
    val dashboardTitle: String
    val dashboardUpcoming: String
    val dashboardNoUpcoming: String
    val dashboardHistory: String
    val dashboardNoHistory: String
    val dashboardLoadFailed: String
    val retry: String
    fun dashboardTeams(count: Int): String

    // Events
    val eventsTitle: String
    val eventsEntry: String
    val eventsEmpty: String
    val eventsLoadFailed: String
    val eventGoal: String
    val eventDistance: String
    val eventLocation: String
    val eventProgram: String
    val eventEquipment: String
    val eventLoadFailed: String
    val rsvpGoing: String
    val rsvpMaybe: String
    val rsvpNotGoing: String
    val rsvpDisabled: String
    val rsvpFailed: String
    val rotiLabel: String
    val rotiFailed: String
    val trainingSection: String
    fun roundLabel(order: Int): String
    val attachmentsSection: String
    val download: String
    val downloadFailed: String
}

/** Native language display names (always shown in their own language, never translated). */
val languageDisplayNames: Map<String, String> = mapOf(
    "fr" to "Français",
    "nl" to "Nederlands",
    "en" to "English",
    "it" to "Italiano",
    "es" to "Español",
)

val supportedLanguages: List<String> = listOf("fr", "nl", "en", "it", "es")

fun stringsFor(lang: String): Strings = when (lang) {
    "nl" -> StringsNl
    "en" -> StringsEn
    "it" -> StringsIt
    "es" -> StringsEs
    else -> StringsFr
}

/** UI access to the active locale's strings. Default Fr; overridden by App's provider. */
val LocalStrings = staticCompositionLocalOf<Strings> { StringsFr }
