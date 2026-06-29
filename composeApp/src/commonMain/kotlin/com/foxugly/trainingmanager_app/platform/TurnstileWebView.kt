package com.foxugly.trainingmanager_app.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Backend-hosted Turnstile page (the widget's hostname allowlist points here). */
const val TURNSTILE_URL: String = "https://tm-api.foxugly.com/turnstile/"

/**
 * Loads the WebView-hosted Cloudflare Turnstile widget and surfaces the token.
 *
 * The host page (served at [TURNSTILE_URL]) calls back through a JS bridge:
 *   - Android: `TMTurnstileBridge.onToken(token)` / `.onError()`
 *   - iOS:     `window.webkit.messageHandlers.turnstile.postMessage(token)`
 *
 * [onToken] fires with the captcha token on success; [onError] on widget
 * error/expiry. Both are delivered on the main thread.
 */
@Composable
expect fun TurnstileWebView(
    onToken: (String) -> Unit,
    onError: () -> Unit,
    modifier: Modifier = Modifier,
    url: String = TURNSTILE_URL,
)
