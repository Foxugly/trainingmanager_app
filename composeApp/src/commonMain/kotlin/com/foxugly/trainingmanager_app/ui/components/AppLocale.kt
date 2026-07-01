package com.foxugly.trainingmanager_app.ui.components

import androidx.compose.runtime.Composable

/**
 * Render [content] with the in-app [languageCode] as the effective locale, so
 * Material date/time pickers show month / day names in the app's language rather
 * than the device locale.
 */
@Composable
expect fun WithAppLocale(languageCode: String, content: @Composable () -> Unit)
