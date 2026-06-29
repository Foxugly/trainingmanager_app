package com.foxugly.trainingmanager_app.platform

/** Opens an external URL (e.g. a presigned attachment download) in the platform browser. */
expect class UrlOpener() {
    fun open(url: String)
}
