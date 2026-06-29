package com.foxugly.trainingmanager_app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import com.foxugly.trainingmanager_app.data.storage.TokenStorage
import com.foxugly.trainingmanager_app.data.storage.TokenStorageStore
import com.foxugly.trainingmanager_app.di.appModule
import com.foxugly.trainingmanager_app.navigation.DeepLinkTarget
import com.foxugly.trainingmanager_app.navigation.parseDeepLink
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.KoinAppDeclaration

private const val PROD_API_BASE_URL = "https://tm-api.foxugly.com/api/v1/"

private var iosDeepLink by mutableStateOf<DeepLinkTarget?>(null)

/** Called from Swift `.onOpenURL` with the absolute URL string. */
fun handleDeepLink(uri: String) {
    parseDeepLink(uri)?.let { iosDeepLink = it }
}

fun MainViewController() = ComposeUIViewController(configure = { initKoinIfNeeded() }) {
    App(
        deepLink = iosDeepLink,
        onDeepLinkConsumed = { iosDeepLink = null },
    )
}

// ComposeUIViewController may be created more than once; start Koin only once.
private var koinStarted = false
private fun initKoinIfNeeded() {
    if (koinStarted) return
    startKoin(iosKoinDeclaration())
    koinStarted = true
}

private fun iosKoinDeclaration(): KoinAppDeclaration = {
    modules(
        appModule(
            tokenStore = TokenStorageStore(TokenStorage()),
            apiBaseUrl = PROD_API_BASE_URL,
            // No BuildConfig on iOS; release builds stay quiet. The Xcode debug
            // scheme can flip this later via a build setting if needed.
            enableHttpLogging = false,
        ),
    )
}

@Suppress("unused")
fun stopKoinForTesting() = stopKoin()
