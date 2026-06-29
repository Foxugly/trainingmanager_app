package com.foxugly.trainingmanager_app.data.repository

import com.foxugly.trainingmanager_app.data.api.EmailConfirmBody
import com.foxugly.trainingmanager_app.data.api.MagicLinkExchangeBody
import com.foxugly.trainingmanager_app.data.api.MagicLinkRequestBody
import com.foxugly.trainingmanager_app.data.api.PasswordResetConfirmBody
import com.foxugly.trainingmanager_app.data.api.RefreshRequest
import com.foxugly.trainingmanager_app.data.api.TokenObtainRequest
import com.foxugly.trainingmanager_app.data.api.TokenPair
import com.foxugly.trainingmanager_app.data.api.TrainingManagerApi
import com.foxugly.trainingmanager_app.data.api.UserProfile
import com.foxugly.trainingmanager_app.data.storage.TokenStore
import com.foxugly.trainingmanager_app.diagnostics.AppLogger
import kotlinx.coroutines.CancellationException

class AuthRepository(
    private val api: TrainingManagerApi,
    private val tokenStorage: TokenStore,
) {
    private val tag = "TM/AuthRepository"

    /** POST auth/token/ → store tokens + remember → GET me/ → profile. */
    suspend fun login(email: String, password: String, remember: Boolean): Result<UserProfile> {
        // Don't log the email — it's PII and would land in Logcat.
        AppLogger.info(tag, "Login requested (remember=$remember)")
        return api.login(TokenObtainRequest(email, password, remember)).mapCatching { pair ->
            tokenStorage.setAccessToken(pair.access)
            tokenStorage.setRefreshToken(pair.refresh)
            tokenStorage.setRemember(remember)
            api.getMe().getOrThrow()
        }.onFailure {
            if (it is CancellationException) throw it
            AppLogger.error(tag, "Login failed: ${it.message}", it)
        }
    }

    suspend fun logout(): Result<Unit> {
        AppLogger.info(tag, "Logout requested")
        val refreshToken = tokenStorage.getRefreshToken()
        if (refreshToken != null) {
            api.logout(refreshToken).onFailure {
                if (it is CancellationException) throw it
                AppLogger.error(tag, "Logout API call failed (tokens cleared locally): ${it.message}", it)
            }
        }
        tokenStorage.clearAuthTokens()
        return Result.success(Unit)
    }

    /** POST auth/magic-link/request/ — always-200, no token handling. */
    suspend fun requestMagicLink(email: String): Result<Unit> {
        AppLogger.info(tag, "Magic-link requested")
        return api.magicLinkRequest(MagicLinkRequestBody(email.trim()))
            .onFailure { if (it is CancellationException) throw it }
    }

    /** POST auth/magic-link/exchange/ → auto-login. */
    suspend fun exchangeMagicLink(token: String): Result<UserProfile> =
        autoLogin { api.magicLinkExchange(MagicLinkExchangeBody(token)) }

    /** POST auth/email/confirm/ → auto-login. */
    suspend fun confirmEmail(key: String): Result<UserProfile> =
        autoLogin { api.confirmEmail(EmailConfirmBody(key)) }

    /** POST auth/password/reset/confirm/ → auto-login. */
    suspend fun confirmPasswordReset(key: String, newPassword: String): Result<UserProfile> =
        autoLogin { api.confirmPasswordReset(PasswordResetConfirmBody(key, newPassword)) }

    /** Shared: a token-pair call → persist access+refresh+remember → GET me/ → profile. */
    private suspend inline fun autoLogin(
        crossinline tokenCall: suspend () -> Result<TokenPair>,
    ): Result<UserProfile> = tokenCall().mapCatching { pair ->
        tokenStorage.setAccessToken(pair.access)
        tokenStorage.setRefreshToken(pair.refresh)
        tokenStorage.setRemember(true)
        api.getMe().getOrThrow()
    }.onFailure {
        if (it is CancellationException) throw it
        AppLogger.error(tag, "Auto-login failed: ${it.message}", it)
    }

    suspend fun getCurrentUser(): Result<UserProfile> = api.getMe()

    fun isAuthenticated(): Boolean = tokenStorage.getAccessToken() != null

    fun hasRefreshToken(): Boolean = tokenStorage.getRefreshToken() != null

    /** Startup refresh: POST auth/token/refresh/, persisting the rotated token pair. */
    suspend fun tryRefresh(): Boolean {
        val refreshToken = tokenStorage.getRefreshToken() ?: return false
        AppLogger.info(tag, "Trying startup token refresh")
        return api.refresh(RefreshRequest(refreshToken)).fold(
            onSuccess = { response ->
                tokenStorage.setAccessToken(response.access)
                response.refresh?.let { tokenStorage.setRefreshToken(it) }
                AppLogger.info(tag, "Startup token refresh succeeded")
                true
            },
            onFailure = {
                if (it is CancellationException) throw it
                AppLogger.error(tag, "Startup token refresh failed", it)
                tokenStorage.clearAuthTokens()
                false
            },
        )
    }
}
