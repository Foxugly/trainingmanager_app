package com.foxugly.trainingmanager_app.diagnostics

import android.util.Log
import io.sentry.Sentry
import io.sentry.SentryLevel

actual object AppLogger {
    actual fun debug(tag: String, message: String) {
        Log.d(tag, message)
    }

    actual fun info(tag: String, message: String) {
        Log.i(tag, message)
    }

    actual fun warn(tag: String, message: String, throwable: Throwable?) {
        Log.w(tag, message, throwable)
        // Breadcrumb: context that rides along with the next captured event.
        Sentry.addBreadcrumb(message, tag)
    }

    actual fun error(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
        // Report handled errors too (most failures here are caught, not crashes).
        // All Sentry calls are no-ops until/unless Sentry is initialized with a DSN.
        if (throwable != null) {
            Sentry.captureException(throwable)
        } else {
            Sentry.captureMessage(message, SentryLevel.ERROR)
        }
    }
}
