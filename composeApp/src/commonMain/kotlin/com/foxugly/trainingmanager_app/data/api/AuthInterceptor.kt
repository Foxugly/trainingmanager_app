package com.foxugly.trainingmanager_app.data.api

import com.foxugly.trainingmanager_app.api.generated.models.TokenRefresh
import com.foxugly.trainingmanager_app.api.generated.models.TokenRefreshRequest
import com.foxugly.trainingmanager_app.data.storage.TokenStore
import com.foxugly.trainingmanager_app.diagnostics.AppLogger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AuthInterceptor(
    private val tokenStorage: TokenStore,
    var onAuthFailure: (() -> Unit)? = null,
) {
    private val tag = "TM/AuthInterceptor"
    private val refreshMutex = Mutex()

    val plugin = createClientPlugin("AuthInterceptor") {
        onRequest { request, _ ->
            // Match against the URL path only (not scheme+host) so AUTH_PATHS
            // entries are not accidentally triggered by a hostname substring.
            val path = request.url.encodedPath
            val isAuthPath = AUTH_PATHS.any { path.contains(it) }
            val token = tokenStorage.getAccessToken()
            if (token != null && !isAuthPath) {
                request.headers.append(HttpHeaders.Authorization, "Bearer $token")
                AppLogger.debug(tag, "Authorization header attached to $path")
            }
        }
    }

    /**
     * Refresh the access token after a 401, then signal the caller to REPLAY its
     * original request lambda (preserving verb + body; the fresh token is
     * re-attached by [plugin]). Returns true if a usable access token is now in
     * place.
     *
     * Under [refreshMutex] we first check whether a concurrent caller already
     * refreshed (current token differs from the stale one): if so we skip a
     * redundant — and, with refresh-token rotation, failure-prone — second
     * auth/token/refresh/ round-trip. If the current token is null, a concurrent
     * caller's refresh already failed and cleared the store; we return false
     * WITHOUT re-firing [onAuthFailure] to prevent stacked navigation events.
     */
    suspend fun refreshIfNeeded(client: HttpClient, staleAccessToken: String?): Boolean =
        refreshMutex.withLock {
            val current = tokenStorage.getAccessToken()
            if (current != null && current != staleAccessToken) {
                AppLogger.info(tag, "Access token already refreshed by a concurrent call; reusing it")
                return@withLock true
            }
            if (current == null) {
                // A concurrent caller already attempted the refresh, failed, and
                // cleared the tokens. Do NOT fire onAuthFailure a second time —
                // that would cause stacked navigation events (N dialogs / pops).
                AppLogger.info(tag, "Tokens already cleared by a concurrent refresh failure; returning false silently")
                return@withLock false
            }
            // Still holding the stale token — we are the caller that performs the refresh.
            val refreshToken = tokenStorage.getRefreshToken() ?: run {
                AppLogger.warn(tag, "Unauthorized response and no refresh token available")
                onAuthFailure?.invoke()
                return@withLock false
            }
            try {
                AppLogger.info(tag, "Refreshing access token after HTTP 401")
                val refreshResponse = client.post("auth/token/refresh/") {
                    contentType(ContentType.Application.Json)
                    setBody(TokenRefreshRequest(refreshToken))
                }
                if (refreshResponse.status == HttpStatusCode.OK) {
                    val body = refreshResponse.body<TokenRefresh>()
                    tokenStorage.setAccessToken(body.access)
                    // Persist the rotated refresh token (ROTATE_REFRESH_TOKENS +
                    // BLACKLIST_AFTER_ROTATION). The generated TokenRefresh always carries a
                    // refresh (rotation is on server-side), so write it unconditionally —
                    // else the next refresh sends a blacklisted token and ejects the user.
                    tokenStorage.setRefreshToken(body.refresh)
                    AppLogger.info(tag, "Access token refresh succeeded")
                    true
                } else {
                    AppLogger.warn(tag, "Access token refresh failed with HTTP ${refreshResponse.status.value}")
                    tokenStorage.clearAuthTokens()
                    onAuthFailure?.invoke()
                    false
                }
            } catch (e: CancellationException) {
                // Propagate coroutine cancellation — do NOT clear tokens or fire
                // onAuthFailure; the user was not logged out, the operation was cancelled.
                throw e
            } catch (e: Exception) {
                AppLogger.error(tag, "Access token refresh threw an exception", e)
                tokenStorage.clearAuthTokens()
                onAuthFailure?.invoke()
                false
            }
        }

    companion object {
        // Paths that NEVER receive a bearer and are NEVER refreshed: the token
        // lifecycle + unauthenticated onboarding endpoints (S1 spec §3).
        val AUTH_PATHS: List<String> = listOf(
            "auth/token/",
            "auth/token/refresh/",
            "auth/register/",
            "auth/email/confirm/",
            "auth/email/resend/",
            "auth/password/reset/",
            "auth/password/reset/confirm/",
            "auth/magic-link/request/",
            "auth/magic-link/exchange/",
            // Unauthenticated invitation-lookup endpoint. TODO: narrow to the exact
            // path once the API contract is finalized — a future authenticated
            // /invitations/ endpoint must not accidentally lose its bearer.
            "invitations/",
        )
    }
}
