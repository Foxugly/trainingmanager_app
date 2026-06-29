package com.foxugly.trainingmanager_app.di

import com.foxugly.trainingmanager_app.data.api.TrainingManagerApi
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.data.storage.TokenStore
import com.foxugly.trainingmanager_app.i18n.LanguageProvider
import com.foxugly.trainingmanager_app.ui.login.LoginViewModel
import com.foxugly.trainingmanager_app.ui.confirm.EmailConfirmViewModel
import com.foxugly.trainingmanager_app.ui.confirm.ResetPasswordViewModel
import com.foxugly.trainingmanager_app.ui.invitation.InvitationViewModel
import com.foxugly.trainingmanager_app.ui.magiclink.MagicLinkExchangeViewModel
import com.foxugly.trainingmanager_app.ui.magiclink.MagicLinkRequestViewModel
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
    factory { LoginViewModel(get()) }
    factory { MagicLinkRequestViewModel(get()) }
    factory { MagicLinkExchangeViewModel(get()) }
    factory { EmailConfirmViewModel(get()) }
    factory { ResetPasswordViewModel(get()) }
    factory { InvitationViewModel(get()) }
}
