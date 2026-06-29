package com.foxugly.trainingmanager_app

import com.foxugly.trainingmanager_app.data.storage.TokenStore

/** In-memory [TokenStore] for tests. */
internal class FakeTokenStore(
    private var access: String? = null,
    private var refresh: String? = null,
    private var remember: Boolean = false,
) : TokenStore {
    var cleared = false
        private set

    override fun getAccessToken() = access
    override fun setAccessToken(token: String?) { access = token }
    override fun getRefreshToken() = refresh
    override fun setRefreshToken(token: String?) { refresh = token }
    override fun getRemember() = remember
    override fun setRemember(value: Boolean) { remember = value }
    override fun clearAuthTokens() { access = null; refresh = null; cleared = true }
}
