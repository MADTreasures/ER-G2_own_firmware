package ch.madtreasures.g2watch.glasses

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import ch.madtreasures.g2watch.MainActivity
import ch.madtreasures.g2watch.R

/**
 * Keeps the process in the foreground while the watch talks to the glasses, so Wear OS does not
 * stop the connection when the watch display goes dark. The connection itself lives in
 * [GlassesConnection]; this service only holds the ongoing notification.
 */
class GlassesService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val text = intent?.getStringExtra(EXTRA_TEXT) ?: getString(R.string.notification_connected)
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification(this, text),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
        )
        // Without the process there is no connection to keep, so no restart.
        return START_NOT_STICKY
    }

    companion object {
        private const val TAG = "G2Watch"
        private const val CHANNEL_ID = "glasses"
        private const val NOTIFICATION_ID = 1
        private const val EXTRA_TEXT = "text"

        /** Call while the app is in the foreground (Android refuses it from the background). */
        fun start(context: Context, text: String) {
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, GlassesService::class.java).putExtra(EXTRA_TEXT, text),
                )
            } catch (e: RuntimeException) {
                // For example ForegroundServiceStartNotAllowedException: the connection still
                // works while the app stays open.
                Log.w(TAG, "foreground service not started", e)
            }
        }

        /** New notification text without restarting the service. */
        fun update(context: Context, text: String) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.activeNotifications.any { it.id == NOTIFICATION_ID }) {
                manager.notify(NOTIFICATION_ID, notification(context, text))
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GlassesService::class.java))
        }

        private fun notification(context: Context, text: String): Notification {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager != null && manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.notification_channel),
                        NotificationManager.IMPORTANCE_LOW,
                    ),
                )
            }
            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE,
            )
            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(text)
                .setContentIntent(open)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build()
        }
    }
}
