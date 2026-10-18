package com.foxugly.trainingmanager_app

import android.app.Application
import com.foxugly.trainingmanager_app.data.api.NetworkException
import com.foxugly.trainingmanager_app.diagnostics.AppLogger
import io.sentry.Breadcrumb
import io.sentry.SentryLevel
import io.sentry.android.core.SentryAndroid

/**
 * Process entry point. Its job is crash reporting: Sentry is initialised here,
 * before any Activity or the FCM service runs, and before
 * MainActivity.installCrashLogger(), so the uncaught-exception chain is
 * our logger -> Sentry -> platform handler (the process dies, Play Vitals records it).
 *
 * The DSN comes from BuildConfig.SENTRY_DSN (Gradle property / env var
 * TM_SENTRY_DSN, a CI secret for release builds). Empty means Sentry is not
 * initialised at all: local and PR builds never report.
 */
class TrainingManagerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initSentry()
    }

    private fun initSentry() {
        val dsn = BuildConfig.SENTRY_DSN
        if (dsn.isBlank()) {
            AppLogger.info(TAG, "Sentry DSN not configured; crash reporting disabled")
            return
        }
        SentryAndroid.init(this) { options ->
            options.dsn = dsn
            options.environment = if (BuildConfig.DEBUG) "debug" else "production"
            options.release = "${BuildConfig.APPLICATION_ID}@${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"
            // No PII: no IP address, no user, no request bodies/headers.
            options.isSendDefaultPii = false
            options.isAttachScreenshot = false
            options.isAttachViewHierarchy = false
            options.isEnableUserInteractionBreadcrumbs = false
            options.tracesSampleRate = 0.0
            options.setBeforeBreadcrumb { breadcrumb, _ -> scrubBreadcrumb(breadcrumb) }
            options.setBeforeSend { event, _ ->
                // Offline / timeouts are expected on a phone: not worth an issue.
                if (event.throwable is NetworkException) return@setBeforeSend null
                event.user = null
                event.request = null
                event
            }
        }
        AppLogger.info(TAG, "Sentry initialised")
    }

    companion object {
        private const val TAG = "TM/Application"

        // JWTs (eyJ...), Bearer headers and link tokens must never ride along in a report.
        private val SECRET_PATTERNS = listOf(
            Regex("""eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+"""),
            Regex("""(?i)bearer\s+\S+"""),
            Regex("""(?i)(token|password|authorization|magic-link|reset-password|confirm-email|invitation)"""),
        )

        /** Drop HTTP breadcrumbs entirely and any breadcrumb that looks like it carries a secret. */
        internal fun scrubBreadcrumb(breadcrumb: Breadcrumb): Breadcrumb? {
            val category = breadcrumb.category.orEmpty()
            if (category.startsWith("http") || category == "network.event") return null
            val text = buildString {
                append(breadcrumb.message.orEmpty())
                breadcrumb.data.forEach { (k, v) -> append(' ').append(k).append('=').append(v) }
            }
            if (SECRET_PATTERNS.any { it.containsMatchIn(text) }) return null
            if (breadcrumb.level == SentryLevel.DEBUG) return null
            return breadcrumb
        }
    }
}
