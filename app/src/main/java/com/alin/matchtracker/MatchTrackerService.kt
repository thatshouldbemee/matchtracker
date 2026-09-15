package com.alin.matchtracker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MatchTrackerService : Service() {

    companion object {
        const val CHANNEL_ID = "match_tracker_channel"
        const val NOTIF_ID = 1001
        const val ACTION_WIN = "com.alin.matchtracker.ACTION_WIN"
        const val ACTION_LOSE = "com.alin.matchtracker.ACTION_LOSE"
        const val ACTION_UPDATE = "com.alin.matchtracker.ACTION_UPDATE"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_UPDATE) {
            updateNotification()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                startForeground(
                    NOTIF_ID, buildNotification(),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIF_ID, buildNotification())
            }
        }
        return START_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Match Tracker", NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Hitung match ranked MLBB"
            channel.setShowBadge(true)
            channel.enableVibration(false)
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIF_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val winIntent = Intent(this, IncrementReceiver::class.java).apply { action = ACTION_WIN }
        val winPending = PendingIntent.getBroadcast(
            this, 1, winIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val loseIntent = Intent(this, IncrementReceiver::class.java).apply { action = ACTION_LOSE }
        val losePending = PendingIntent.getBroadcast(
            this, 2, loseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val summary = Prefs.summaryText(this)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_add)
            .setContentTitle(Prefs.titleText(this))
            .setContentText(summary.substringBefore("\n"))
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSortKey("0")
            .addAction(android.R.drawable.ic_menu_add, "✅ Menang", winPending)
            .addAction(android.R.drawable.ic_delete, "❌ Kalah", losePending)
            .build()
    }
}
