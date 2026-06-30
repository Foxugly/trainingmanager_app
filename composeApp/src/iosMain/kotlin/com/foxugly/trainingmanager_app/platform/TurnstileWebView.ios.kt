package com.foxugly.trainingmanager_app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

/**
 * iOS Turnstile WebView — a [WKWebView] (hosted via [UIKitView]) that loads the
 * backend Turnstile page ([TURNSTILE_URL]) and surfaces the captcha token.
 *
 * The host page posts back through two WKScriptMessageHandlers (see
 * `customuser/views/turnstile_page.py`):
 *   - `turnstile`      → the token, on success → [onToken]
 *   - `turnstileError` → widget error / expiry → [onError] (forces a fresh solve)
 *
 * WebKit delivers script messages on the main thread, so the callbacks need no
 * extra marshaling (contrast the Android actual, which posts back to the main
 * looper). The handlers are removed in `onRelease` to avoid leaking the WebView.
 *
 * NOTE: written but NOT YET COMPILED — this project has never been built for iOS
 * (no Mac/Xcode in the authoring environment). The Kotlin/Native WebKit bindings
 * and the message contract mirror the proven [TurnstileWebView] Android actual
 * and the backend host page; verify on a Mac before relying on it. A Mac build
 * may need to adjust the `UIKitView` opt-in or the `NSURLRequest(uRL = …)`
 * argument name depending on the exact binding versions.
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)
@Composable
actual fun TurnstileWebView(
    onToken: (String) -> Unit,
    onError: () -> Unit,
    modifier: Modifier,
    url: String,
) {
    // Keep the latest callbacks without rebuilding the WebView — `factory` runs
    // once, but the message handler reads these on every message.
    val latestOnToken by rememberUpdatedState(onToken)
    val latestOnError by rememberUpdatedState(onError)

    // A single handler serving both message names. Remembered so it survives
    // recomposition and isn't collected while the WebView still references it.
    val messageHandler = remember {
        object : NSObject(), WKScriptMessageHandlerProtocol {
            override fun userContentController(
                userContentController: WKUserContentController,
                didReceiveScriptMessage: WKScriptMessage,
            ) {
                when (didReceiveScriptMessage.name) {
                    "turnstile" ->
                        (didReceiveScriptMessage.body as? String)
                            ?.takeIf { it.isNotEmpty() }
                            ?.let { latestOnToken(it) }
                    "turnstileError" -> latestOnError()
                }
            }
        }
    }

    UIKitView(
        modifier = modifier,
        factory = {
            val controller = WKUserContentController().apply {
                addScriptMessageHandler(messageHandler, "turnstile")
                addScriptMessageHandler(messageHandler, "turnstileError")
            }
            val config = WKWebViewConfiguration().apply {
                userContentController = controller
            }
            WKWebView(frame = CGRectZero.readValue(), configuration = config).apply {
                NSURL.URLWithString(url)?.let { loadRequest(NSURLRequest(uRL = it)) }
            }
        },
        onRelease = { webView ->
            webView.configuration.userContentController.apply {
                removeScriptMessageHandlerForName("turnstile")
                removeScriptMessageHandlerForName("turnstileError")
            }
        },
    )
}
