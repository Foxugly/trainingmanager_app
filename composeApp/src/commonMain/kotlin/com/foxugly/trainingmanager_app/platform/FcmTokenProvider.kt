package com.foxugly.trainingmanager_app.platform

/** Provides the device's FCM registration token + platform tag for /devices/register/. */
expect class FcmTokenProvider() {
    val platform: String
    suspend fun token(): String?
}
