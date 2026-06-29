package com.foxugly.trainingmanager_app.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * iOS Turnstile WebView — NOT YET IMPLEMENTED.
 *
 * Needs a WKWebView (via UIKitView) with a WKScriptMessageHandler named
 * "turnstile" (the host page posts the token to it). This must be written and
 * verified on a Mac (the project has never been compiled for iOS). Until then
 * this renders nothing and never produces a token, so register / forgot-password
 * are non-functional on iOS. Android works.
 *
 * TODO(ios): implement WKWebView + message handler; see TurnstileWebView.android.kt.
 */
@Composable
actual fun TurnstileWebView(
    onToken: (String) -> Unit,
    onError: () -> Unit,
    modifier: Modifier,
    url: String,
) {
    Box(modifier)
}
