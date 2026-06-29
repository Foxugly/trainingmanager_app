package com.foxugly.trainingmanager_app.platform

actual class FcmTokenProvider actual constructor() {
    actual val platform: String = "ios"

    // Firebase iOS SDK is not wired yet (needs the CocoaPods/SPM Firebase setup + APNs,
    // Mac-only). Returns null for now → no device registration on iOS until then.
    actual suspend fun token(): String? = null
}
