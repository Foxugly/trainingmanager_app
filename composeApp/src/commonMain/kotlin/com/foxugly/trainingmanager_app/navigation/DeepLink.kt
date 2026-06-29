package com.foxugly.trainingmanager_app.navigation

/** A deep link the app knows how to route. */
sealed interface DeepLinkTarget {
    data class MagicLinkExchange(val token: String) : DeepLinkTarget
    data class EmailConfirm(val key: String) : DeepLinkTarget
    data class PasswordResetConfirm(val key: String) : DeepLinkTarget
}

/**
 * Parse an incoming deep-link URI (HTTPS App Link OR custom scheme) into a target.
 * Matches on the known path marker rather than parsing scheme/authority, so it is
 * robust to both `https://tm.foxugly.com/...` and `trainingmanager://.../...`.
 * Pure + commonTest-able.
 */
fun parseDeepLink(uri: String?): DeepLinkTarget? {
    if (uri == null) return null
    extract(uri, "/auth/magic-link/")?.let { return DeepLinkTarget.MagicLinkExchange(it) }
    extract(uri, "/auth/confirm-email/")?.let { return DeepLinkTarget.EmailConfirm(it) }
    extract(uri, "/auth/reset-password/")?.let { return DeepLinkTarget.PasswordResetConfirm(it) }
    return null
}

private fun extract(uri: String, marker: String): String? {
    val i = uri.indexOf(marker)
    if (i < 0) return null
    val seg = uri.substring(i + marker.length).substringBefore('/').substringBefore('?').substringBefore('#')
    return seg.ifBlank { null }
}
