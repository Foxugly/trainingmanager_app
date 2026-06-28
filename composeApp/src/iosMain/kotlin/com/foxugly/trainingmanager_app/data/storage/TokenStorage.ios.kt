package com.foxugly.trainingmanager_app.data.storage

import com.foxugly.trainingmanager_app.diagnostics.AppLogger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * iOS token storage backed by the native iOS Keychain for JWT secrets and
 * NSUserDefaults for non-secret preferences.
 *
 * **Why the Keychain directly rather than multiplatform-settings?**
 * multiplatform-settings' iOS backend defaults to NSUserDefaults, which is a
 * plain plist file — not a secure store. Tokens are secrets and must live in
 * the Keychain (`kSecClassGenericPassword`,
 * `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`: encrypted at rest, device-
 * only, excluded from iCloud and iTunes backups). Non-secret prefs (remember
 * flag, UI language) are safe in NSUserDefaults and stay there.
 *
 * The Android actual mirrors this split with `EncryptedSharedPreferences` (AES256)
 * for secrets and regular SharedPreferences conventions for non-secrets.
 *
 * iosMain compiles only on macOS/Xcode; verified by review, not the Windows host
 * test.
 */
@OptIn(ExperimentalForeignApi::class)
actual class TokenStorage {

    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun getAccessToken(): String? = keychainGet(KEY_ACCESS)
    actual fun setAccessToken(token: String?) = keychainSet(KEY_ACCESS, token)

    actual fun getRefreshToken(): String? = keychainGet(KEY_REFRESH)
    actual fun setRefreshToken(token: String?) = keychainSet(KEY_REFRESH, token)

    actual fun getRemember(): Boolean = defaults.boolForKey(KEY_REMEMBER)
    actual fun setRemember(value: Boolean) = defaults.setBool(value, forKey = KEY_REMEMBER)

    actual fun getLanguage(): String? = defaults.stringForKey(KEY_LANGUAGE)
    actual fun setLanguage(code: String?) {
        if (code != null) defaults.setObject(code, forKey = KEY_LANGUAGE)
        else defaults.removeObjectForKey(KEY_LANGUAGE)
    }

    actual fun clearAuthTokens() {
        keychainSet(KEY_ACCESS, null)
        keychainSet(KEY_REFRESH, null)
    }

    private fun keychainBaseQuery(account: String): Map<Any?, Any?> = mapOf(
        kSecClass to kSecClassGenericPassword,
        kSecAttrService to SERVICE,
        kSecAttrAccount to account,
    )

    private fun keychainGet(account: String): String? = memScoped {
        val query = (
            keychainBaseQuery(account) + mapOf(
                kSecReturnData to kCFBooleanTrue,
                kSecMatchLimit to kSecMatchLimitOne,
            )
        ).toCFDictionary()
        try {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)
            if (status != errSecSuccess) return@memScoped null
            val data = CFBridgingRelease(result.value) as? NSData ?: return@memScoped null
            NSString.create(data, NSUTF8StringEncoding) as String?
        } finally {
            CFBridgingRelease(query)
        }
    }

    private fun keychainSet(account: String, value: String?) {
        keychainDelete(account)
        if (value == null) return
        val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        val attributes = (
            keychainBaseQuery(account) + mapOf(
                kSecValueData to data,
                kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
            )
        ).toCFDictionary()
        try {
            val status = SecItemAdd(attributes, null)
            if (status != errSecSuccess) {
                AppLogger.error(TAG, "SecItemAdd failed for '$account', OSStatus=$status")
            }
        } finally {
            CFBridgingRelease(attributes)
        }
    }

    private fun keychainDelete(account: String) {
        val query = keychainBaseQuery(account).toCFDictionary()
        try {
            val status = SecItemDelete(query)
            if (status != errSecSuccess && status != errSecItemNotFound) {
                AppLogger.error(TAG, "SecItemDelete failed for '$account', OSStatus=$status")
            }
        } finally {
            CFBridgingRelease(query)
        }
    }

    private fun Map<Any?, Any?>.toCFDictionary(): CFDictionaryRef? {
        val dict = platform.Foundation.NSMutableDictionary()
        for ((k, v) in this) {
            if (k != null && v != null) dict.setObject(v, forKey = k as Any)
        }
        return CFBridgingRetain(dict) as CFDictionaryRef?
    }

    companion object {
        private const val TAG = "TM/TokenStorage"
        private const val SERVICE = "com.foxugly.trainingmanager_app"
        private const val KEY_ACCESS = "tm_access_token"
        private const val KEY_REFRESH = "tm_refresh_token"
        private const val KEY_REMEMBER = "tm_remember"
        private const val KEY_LANGUAGE = "tm_ui_language"
    }
}
