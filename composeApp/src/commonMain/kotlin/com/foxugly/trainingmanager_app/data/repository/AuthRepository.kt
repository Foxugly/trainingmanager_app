package com.foxugly.trainingmanager_app.data.repository

import com.foxugly.trainingmanager_app.data.api.RefreshRequest
import com.foxugly.trainingmanager_app.data.api.TokenObtainRequest
import com.foxugly.trainingmanager_app.data.api.TrainingManagerApi
import com.foxugly.trainingmanager_app.data.api.UserProfile
import com.foxugly.trainingmanager_app.data.storage.TokenStore
import com.foxugly.trainingmanager_app.diagnostics.AppLogger

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
            AppLogger.error(tag, "Login failed: ${it.message}", it)
        }
    }

    suspend fun logout(): Result<Unit> {
        AppLogger.info(tag, "Logout requested")
        val refreshToken = tokenStorage.getRefreshToken()
        val result = if (refreshToken != null) api.logout(refreshToken) else Result.success(Unit)
        tokenStorage.clearAuthTokens()
        return result
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
                AppLogger.error(tag, "Startup token refresh failed", it)
                tokenStorage.clearAuthTokens()
                false
            },
        )
    }
}
