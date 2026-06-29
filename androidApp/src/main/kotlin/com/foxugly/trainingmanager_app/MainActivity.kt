package com.foxugly.trainingmanager_app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.foxugly.trainingmanager_app.data.storage.TokenStorage
import com.foxugly.trainingmanager_app.data.storage.TokenStorageStore
import com.foxugly.trainingmanager_app.di.appModule
import com.foxugly.trainingmanager_app.diagnostics.AppLogger
import com.foxugly.trainingmanager_app.navigation.DeepLinkTarget
import com.foxugly.trainingmanager_app.navigation.parseDeepLink
import com.foxugly.trainingmanager_app.platform.AppContextHolder
import org.koin.core.context.GlobalContext

class MainActivity : ComponentActivity() {
    // Pending deep link parsed from the launch / new intent (ACTION_VIEW URI).
    private val deepLink = mutableStateOf<DeepLinkTarget?>(null)

    // Pending push-notification url (e.g. "/teams/3") from a tapped FCM notification.
    private val notificationUrl = mutableStateOf<String?>(null)

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best-effort */ }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseDeepLink(intent.dataString)?.let { deepLink.value = it }
        intent.getStringExtra(TMFirebaseMessagingService.EXTRA_NOTIF_URL)?.let { notificationUrl.value = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppContextHolder.context = applicationContext
        deepLink.value = parseDeepLink(intent?.dataString)
        notificationUrl.value = intent?.getStringExtra(TMFirebaseMessagingService.EXTRA_NOTIF_URL)
        maybeRequestNotificationPermission()

        // Start Koin once for the process. The graph is prod-only: fixed base URL,
        // HTTP logging only in debug builds.
        if (GlobalContext.getOrNull() == null) {
            org.koin.core.context.startKoin {
                modules(
                    appModule(
                        tokenStore = TokenStorageStore(TokenStorage(applicationContext)),
                        apiBaseUrl = PROD_API_BASE_URL,
                        enableHttpLogging = BuildConfig.DEBUG,
                    ),
                )
            }
            AppLogger.info(TAG, "Koin graph started")
        }

        setContent {
            App(
                deepLink = deepLink.value,
                onDeepLinkConsumed = { deepLink.value = null },
                notificationUrl = notificationUrl.value,
                onNotificationConsumed = { notificationUrl.value = null },
            )
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    companion object {
        private const val TAG = "TM/MainActivity"
        // Both release and debug builds talk to prod; debug only adds HTTP logging.
        private const val PROD_API_BASE_URL = "https://tm-api.foxugly.com/api/v1/"
    }
}
