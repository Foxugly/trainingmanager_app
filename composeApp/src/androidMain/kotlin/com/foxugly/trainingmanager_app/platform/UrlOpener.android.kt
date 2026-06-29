package com.foxugly.trainingmanager_app.platform

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Holds the application Context for context-needing platform helpers. Set in MainActivity.onCreate. */
object AppContextHolder {
    lateinit var context: Context
}

actual class UrlOpener actual constructor() {
    actual fun open(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        AppContextHolder.context.startActivity(intent)
    }
}
