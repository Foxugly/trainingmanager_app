package com.foxugly.trainingmanager_app.ui.login

import com.foxugly.trainingmanager_app.data.api.ApiException
import com.foxugly.trainingmanager_app.data.api.NetworkErrorKind
import com.foxugly.trainingmanager_app.data.api.NetworkException
import com.foxugly.trainingmanager_app.i18n.Strings
import kotlinx.coroutines.CancellationException

/**
 * Maps an auth failure to a localized message, or null for cancellation (the
 * screen left composition mid-request — show nothing).
 *
 * Scope: 401 / 400-email-not-verified / 5xx / offline / timeout. The full DRF
 * field-error map + 429 Retry-After countdown are a later concern.
 */
fun mapLoginError(throwable: Throwable, strings: Strings): String? = when {
    throwable is CancellationException -> null
    throwable is NetworkException && throwable.kind == NetworkErrorKind.TIMEOUT -> strings.networkTimeout
    throwable is NetworkException -> strings.networkOffline
    throwable is ApiException && throwable.statusCode in 500..599 -> strings.serverError
    throwable is ApiException && throwable.statusCode == 401 -> strings.invalidCredentials
    throwable is ApiException && throwable.statusCode == 400 &&
        throwable.message?.contains("email_not_verified") == true -> strings.emailNotVerified
    throwable is ApiException -> strings.loginFailed
    else -> strings.loginFailed
}
