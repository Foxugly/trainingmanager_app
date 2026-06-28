package com.foxugly.trainingmanager_app.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.foxugly.trainingmanager_app.diagnostics.AppLogger

actual class TokenStorage(context: Context) {

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        "trainingmanager_secure_prefs",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    actual fun getAccessToken(): String? = readString(KEY_ACCESS)
    actual fun setAccessToken(token: String?) = writeString(KEY_ACCESS, token)

    actual fun getRefreshToken(): String? = readString(KEY_REFRESH)
    actual fun setRefreshToken(token: String?) = writeString(KEY_REFRESH, token)

    actual fun getRemember(): Boolean =
        runCatching { prefs.getBoolean(KEY_REMEMBER, false) }
            .onFailure { AppLogger.error(TAG, "Failed to read $KEY_REMEMBER", it) }
            .getOrDefault(false)

    actual fun setRemember(value: Boolean) {
        val committed = runCatching { prefs.edit().putBoolean(KEY_REMEMBER, value).commit() }
            .onFailure { AppLogger.error(TAG, "Failed to write $KEY_REMEMBER", it) }
            .getOrDefault(false)
        if (!committed) AppLogger.error(TAG, "Write of $KEY_REMEMBER was not committed")
    }

    actual fun getLanguage(): String? = readString(KEY_LANGUAGE)
    actual fun setLanguage(code: String?) = writeString(KEY_LANGUAGE, code)

    actual fun clearAuthTokens() {
        val committed = runCatching {
            prefs.edit().remove(KEY_ACCESS).remove(KEY_REFRESH).commit()
        }.onFailure { AppLogger.error(TAG, "Failed to clear auth tokens", it) }
            .getOrDefault(false)
        if (committed) AppLogger.info(TAG, "Auth tokens cleared")
        else AppLogger.error(TAG, "Auth token clear was not committed")
    }

    private fun readString(key: String): String? =
        runCatching { prefs.getString(key, null) }
            .onFailure { AppLogger.error(TAG, "Failed to read $key", it) }
            .getOrNull()

    private fun writeString(key: String, value: String?) {
        // commit() (synchronous): an EncryptedSharedPreferences write failure must
        // not be swallowed — a token that never landed reads back null next launch
        // and surfaces as an unexpected logout.
        val committed = runCatching { prefs.edit().putString(key, value).commit() }
            .onFailure { AppLogger.error(TAG, "Failed to write $key", it) }
            .getOrDefault(false)
        if (!committed) AppLogger.error(TAG, "Write of $key was not committed")
    }

    companion object {
        private const val TAG = "TM/TokenStorage"
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_REMEMBER = "remember"
        private const val KEY_LANGUAGE = "ui_language"
    }
}
