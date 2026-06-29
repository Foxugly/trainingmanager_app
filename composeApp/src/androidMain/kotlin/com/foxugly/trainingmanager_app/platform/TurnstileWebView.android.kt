package com.foxugly.trainingmanager_app.platform

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun TurnstileWebView(
    onToken: (String) -> Unit,
    onError: () -> Unit,
    modifier: Modifier,
    url: String,
) {
    val main = Handler(Looper.getMainLooper())
    // Capture into locals so the @JavascriptInterface methods (named onToken/onError
    // to match the host page's bridge) don't shadow the composable parameters.
    val tokenCb = onToken
    val errorCb = onError

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                // JS bridge calls arrive off the main thread → marshal back to it.
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onToken(token: String) {
                            main.post { tokenCb(token) }
                        }

                        @JavascriptInterface
                        fun onError() {
                            main.post { errorCb() }
                        }
                    },
                    "TMTurnstileBridge",
                )
                loadUrl(url)
            }
        },
    )
}
