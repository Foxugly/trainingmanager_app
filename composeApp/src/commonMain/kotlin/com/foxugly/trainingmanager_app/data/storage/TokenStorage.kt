package com.foxugly.trainingmanager_app.data.storage

expect class TokenStorage {
    fun getAccessToken(): String?
    fun setAccessToken(token: String?)
    fun getRefreshToken(): String?
    fun setRefreshToken(token: String?)
    fun getRemember(): Boolean
    fun setRemember(value: Boolean)
    fun clearAuthTokens()
    // UI language preference (lowercase ISO code, e.g. "fr"). Used by the
    // Accept-Language interceptor; full LanguageService persistence is deferred.
    fun getLanguage(): String?
    fun setLanguage(code: String?)
}
