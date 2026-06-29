package com.foxugly.trainingmanager_app.di

import com.foxugly.trainingmanager_app.data.api.TrainingManagerApi
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.data.storage.TokenStore
import com.foxugly.trainingmanager_app.i18n.LanguageProvider
import com.foxugly.trainingmanager_app.i18n.LanguageService
import com.foxugly.trainingmanager_app.platform.UrlOpener
import com.foxugly.trainingmanager_app.ui.confirm.EmailConfirmViewModel
import com.foxugly.trainingmanager_app.ui.dashboard.DashboardViewModel
import com.foxugly.trainingmanager_app.ui.events.EventDetailViewModel
import com.foxugly.trainingmanager_app.ui.events.EventsListViewModel
import com.foxugly.trainingmanager_app.ui.confirm.ResetPasswordViewModel
import com.foxugly.trainingmanager_app.ui.invitation.InvitationViewModel
import com.foxugly.trainingmanager_app.ui.login.LoginViewModel
import com.foxugly.trainingmanager_app.ui.magiclink.MagicLinkExchangeViewModel
import com.foxugly.trainingmanager_app.ui.magiclink.MagicLinkRequestViewModel
import com.foxugly.trainingmanager_app.ui.profile.ChangePasswordViewModel
import com.foxugly.trainingmanager_app.ui.profile.ProfileViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The shared dependency graph. Takes a [TokenStore] (already wrapped from the
 * platform [com.foxugly.trainingmanager_app.data.storage.TokenStorage]) plus the
 * prod base URL + logging flag from the platform entry point, so the module is
 * fully constructible in commonTest with a fake store.
 */
fun appModule(
    tokenStore: TokenStore,
    apiBaseUrl: String,
    enableHttpLogging: Boolean,
): Module = module {
    single { tokenStore }
    single { LanguageProvider() }
    single { TrainingManagerApi(get(), apiBaseUrl, enableHttpLogging, get()) }
    single { AuthRepository(get(), get()) }
    single { LanguageService(get(), get()) }
    single { UrlOpener() }
    // ViewModels that surface localized error/UI text get the active locale's
    // Strings captured at construction (screens recompose on language change).
    factory { LoginViewModel(get(), get<LanguageService>().strings) }
    factory { MagicLinkRequestViewModel(get(), get<LanguageService>().strings) }
    factory { MagicLinkExchangeViewModel(get()) }
    factory { EmailConfirmViewModel(get()) }
    factory { ResetPasswordViewModel(get(), get<LanguageService>().strings) }
    factory { InvitationViewModel(get(), get<LanguageService>().strings) }
    factory { ProfileViewModel(get(), get<LanguageService>().strings) }
    factory { ChangePasswordViewModel(get(), get<LanguageService>().strings) }
    factory { DashboardViewModel(get(), get<LanguageService>().strings) }
    factory { EventsListViewModel(get(), get<LanguageService>().strings) }
    factory { val opener = get<UrlOpener>(); EventDetailViewModel(get(), get<LanguageService>().strings) { url -> opener.open(url) } }
}
