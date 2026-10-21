package com.foxugly.trainingmanager_app.platform

import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

actual class FcmTokenProvider actual constructor() {
    actual val platform: String = "android"

    actual suspend fun token(): String? = try {
        suspendCancellableCoroutine { cont ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                cont.resume(if (task.isSuccessful) task.result else null)
            }
        }
    } catch (e: Throwable) {
        // Firebase not initialized (no google-services.json at runtime) → no token.
        null
    }

    actual suspend fun deleteToken() {
        try {
            suspendCancellableCoroutine { cont ->
                FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener { cont.resume(Unit) }
            }
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            // Firebase not initialized: nothing was registered from this device anyway.
        }
    }
}
