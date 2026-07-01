package com.foxugly.trainingmanager_app.ui.components

import androidx.compose.runtime.Composable

// iOS locale override for Compose pickers is not wired up yet (the app is not
// compiled/run on iOS). Render as-is for now.
@Composable
actual fun WithAppLocale(languageCode: String, content: @Composable () -> Unit) {
    content()
}
