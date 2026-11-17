package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Foreground service that keeps a conversion running while the app
 * is in the background — a Pro feature (WL-14, v0.2.6).
 *
 * Free conversions run in the ViewModel scope and stop if the
 * process is killed; Pro conversions start this service, so the
 * encode survives the app going to the background. The service
 * shows a low-priority ongoing notification ("Converting …") and
 * stops itself when the queue run ends.
 *
 * No notification permission is needed: foreground-service
 * notifications are exempt from POST_NOTIFICATIONS.
 */
class ConversionService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                createChannel()
                val text = intent.getStringExtra(EXTRA_TEXT) ?: "Converting…"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        buildNotification(text),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                    )
                } else {
                    @Suppress("DEPRECATION")
                    startForeground(NOTIFICATION_ID, buildNotification(text))
                }
            }
            ACTION_UPDATE -> {
                val text = intent?.getStringExtra(EXTRA_TEXT) ?: return START_NOT_STICKY
                val manager = getSystemService(NotificationManager::class.java)
                manager?.notify(NOTIFICATION_ID, buildNotification(text))
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Conversions",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { description = "Conversion progress while the app is in the background" },
            )
        }
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Convert264")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setOngoing(true)
            .build()

    companion object {
        private const val CHANNEL_ID = "convert264_conversions"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_START = "com.example.action.CONVERSION_START"
        private const val ACTION_UPDATE = "com.example.action.CONVERSION_UPDATE"
        private const val ACTION_STOP = "com.example.action.CONVERSION_STOP"
        private const val EXTRA_TEXT = "text"

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ConversionService::class.java).setAction(ACTION_START),
            )
        }

        fun update(context: Context, text: String) {
            context.startService(
                Intent(context, ConversionService::class.java)
                    .setAction(ACTION_UPDATE)
                    .putExtra(EXTRA_TEXT, text),
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, ConversionService::class.java).setAction(ACTION_STOP),
            )
        }
    }
}
