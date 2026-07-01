package com.foxugly.trainingmanager_app.ui.components

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@Composable
actual fun WithAppLocale(languageCode: String, content: @Composable () -> Unit) {
    val baseConfig = LocalConfiguration.current
    val baseContext = LocalContext.current
    val locale = remember(languageCode) { Locale(languageCode) }
    val localizedConfig = remember(baseConfig, locale) {
        Configuration(baseConfig).apply { setLocale(locale) }
    }
    // LocalConfiguration drives the calendar content (month / weekday names);
    // LocalContext drives Material's own strings ("Select date", "OK", …).
    val localizedContext = remember(baseContext, localizedConfig) {
        baseContext.createConfigurationContext(localizedConfig)
    }
    CompositionLocalProvider(
        LocalConfiguration provides localizedConfig,
        LocalContext provides localizedContext,
    ) {
        content()
    }
}
