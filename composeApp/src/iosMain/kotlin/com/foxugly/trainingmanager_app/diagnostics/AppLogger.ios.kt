@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package com.foxugly.trainingmanager_app.diagnostics

import kotlin.native.Platform

/**
 * Console logging for debug binaries only: a release build prints nothing, since
 * the device console is readable by anyone with the phone and a cable (audit A-M8).
 */
actual object AppLogger {
    private val enabled: Boolean = Platform.isDebugBinary

    actual fun debug(tag: String, message: String) {
        if (enabled) println("DEBUG/$tag: $message")
    }

    actual fun info(tag: String, message: String) {
        if (enabled) println("INFO/$tag: $message")
    }

    actual fun warn(tag: String, message: String, throwable: Throwable?) {
        if (enabled) println("WARN/$tag: $message ${throwable?.message.orEmpty()}")
    }

    actual fun error(tag: String, message: String, throwable: Throwable?) {
        if (enabled) println("ERROR/$tag: $message ${throwable?.message.orEmpty()}")
    }
}
