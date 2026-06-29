package com.foxugly.trainingmanager_app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.foxugly.trainingmanager_app.data.repository.AuthRepository
import com.foxugly.trainingmanager_app.diagnostics.AppLogger
import com.foxugly.trainingmanager_app.platform.FcmTokenProvider
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

/**
 * Receives FCM pushes and posts a system notification. Tapping it opens
 * [MainActivity] with the payload `url` (e.g. "/teams/3") in an extra, which the
 * app routes via parseNotificationTarget. Data payload contract (set by the
 * backend notify() push channel): {type, url, notification_id}.
 *
 * Written without a device to test on — verify on a real device/emulator.
 */
class TMFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = message.notification?.title ?: data["title"] ?: "TrainingManager"
        val body = message.notification?.body ?: data["body"] ?: ""
        showNotification(title, body, data["url"].orEmpty())
    }

    override fun onNewToken(token: String) {
        // Re-register the rotated token if someone is signed in. Best-effort.
        val koin = GlobalContext.getOrNull() ?: return
        val repo = koin.get<AuthRepository>()
        if (!repo.isAuthenticated()) return
        val platform = koin.get<FcmTokenProvider>().platform
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { repo.registerDevice(token, platform) }
                .onFailure { AppLogger.error(TAG, "onNewToken re-register failed: ${it.message}", it) }
        }
    }

    private fun showNotification(title: String, body: String, url: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "General", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (url.isNotEmpty()) putExtra(EXTRA_NOTIF_URL, url)
        }
        val pi = PendingIntent.getActivity(
            this,
            url.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        nm.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        const val EXTRA_NOTIF_URL = "tm_notif_url"
        private const val CHANNEL_ID = "tm_default"
        private const val TAG = "TM/FCM"
    }
}
