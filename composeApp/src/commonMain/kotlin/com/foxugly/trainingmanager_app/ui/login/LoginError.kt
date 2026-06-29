package com.foxugly.trainingmanager_app.ui.login

import com.foxugly.trainingmanager_app.data.api.ApiException
import com.foxugly.trainingmanager_app.data.api.NetworkErrorKind
import com.foxugly.trainingmanager_app.data.api.NetworkException
import kotlinx.coroutines.CancellationException

/**
 * Maps an auth failure to a user-facing FR message, or null for cancellation
 * (the screen left composition mid-request — show nothing).
 *
 * S1b scope: 401 / 400-email-not-verified / 5xx / offline / timeout. The full
 * DRF field-error map (`applyServerError`) and 429 `Retry-After` countdown are
 * S1c (needed by register/forgot, not login).
 */
fun mapLoginError(throwable: Throwable): String? = when {
    throwable is CancellationException -> null
    throwable is NetworkException && throwable.kind == NetworkErrorKind.TIMEOUT -> LoginStrings.networkTimeout
    throwable is NetworkException -> LoginStrings.networkOffline
    throwable is ApiException && throwable.statusCode in 500..599 -> LoginStrings.serverError
    throwable is ApiException && throwable.statusCode == 401 -> LoginStrings.invalidCredentials
    throwable is ApiException && throwable.statusCode == 400 &&
        throwable.message?.contains("email_not_verified") == true -> LoginStrings.emailNotVerified
    throwable is ApiException -> LoginStrings.loginFailed
    else -> LoginStrings.loginFailed
}
