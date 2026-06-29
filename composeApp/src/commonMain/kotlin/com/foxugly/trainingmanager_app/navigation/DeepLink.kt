package com.foxugly.trainingmanager_app.navigation

/** A deep link the app knows how to route. Extended in S1c-b/c (email-confirm,
 *  reset-password, invitation). */
sealed interface DeepLinkTarget {
    data class MagicLinkExchange(val token: String) : DeepLinkTarget
}

/**
 * Parse an incoming deep-link URI (HTTPS App Link OR custom scheme) into a target.
 * Matches on the known path marker rather than parsing scheme/authority, so it is
 * robust to both `https://tm.foxugly.com/auth/magic-link/{t}` and
 * `trainingmanager://…/auth/magic-link/{t}` (where the custom scheme's authority
 * is ambiguous). Pure + commonTest-able.
 */
fun parseDeepLink(uri: String?): DeepLinkTarget? {
    if (uri == null) return null
    val marker = "/auth/magic-link/"
    val i = uri.indexOf(marker)
    if (i >= 0) {
        val token = uri.substring(i + marker.length)
            .substringBefore('/').substringBefore('?').substringBefore('#')
        if (token.isNotBlank()) return DeepLinkTarget.MagicLinkExchange(token)
    }
    return null
}
