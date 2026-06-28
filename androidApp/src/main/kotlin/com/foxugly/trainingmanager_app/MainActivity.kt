package com.foxugly.trainingmanager_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.foxugly.trainingmanager_app.data.storage.TokenStorage
import com.foxugly.trainingmanager_app.data.storage.TokenStorageStore
import com.foxugly.trainingmanager_app.di.appModule
import com.foxugly.trainingmanager_app.diagnostics.AppLogger
import org.koin.core.context.GlobalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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

        setContent { App() }
    }

    companion object {
        private const val TAG = "TM/MainActivity"
        // Both release and debug builds talk to prod; debug only adds HTTP logging.
        private const val PROD_API_BASE_URL = "https://tm-api.foxugly.com/api/v1/"
    }
}
