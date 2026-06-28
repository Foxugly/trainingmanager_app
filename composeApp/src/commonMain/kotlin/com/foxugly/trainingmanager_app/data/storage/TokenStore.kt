package com.foxugly.trainingmanager_app.data.storage

/**
 * Token persistence seam used by the data layer (TrainingManagerApi /
 * AuthInterceptor / repositories). [TokenStorage] is an `expect class` and so
 * can't be faked in commonTest; depending on this interface — with
 * [TokenStorageStore] adapting the real platform storage — lets tests inject a
 * fake without touching the expect/actual declarations.
 */
interface TokenStore {
    fun getAccessToken(): String?
    fun setAccessToken(token: String?)
    fun getRefreshToken(): String?
    fun setRefreshToken(token: String?)
    // "Stay logged in" preference. Sent as the login `remember` field (backend
    // chooses a 7d vs 30d refresh TTL); persisted so a relaunch keeps the choice.
    fun getRemember(): Boolean
    fun setRemember(value: Boolean)
    fun clearAuthTokens()
}

/** Adapts the platform [TokenStorage] to [TokenStore] (pure commonMain — no
 * expect/actual change, so no iOS-side risk). Internal: all callers (DI
 * wiring in androidMain / iosMain) are within the same `:composeApp` module. */
internal class TokenStorageStore(private val storage: TokenStorage) : TokenStore {
    override fun getAccessToken(): String? = storage.getAccessToken()
    override fun setAccessToken(token: String?) = storage.setAccessToken(token)
    override fun getRefreshToken(): String? = storage.getRefreshToken()
    override fun setRefreshToken(token: String?) = storage.setRefreshToken(token)
    override fun getRemember(): Boolean = storage.getRemember()
    override fun setRemember(value: Boolean) = storage.setRemember(value)
    override fun clearAuthTokens() = storage.clearAuthTokens()
}
