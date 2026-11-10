package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.config.AppConfig

/**
 * Foreground Service that keeps the Walkie-Talkie radio engine and offline call signaling
 * active in the background and while the screen is locked.
 */
class WalkieTalkieService : Service() {

    private val binder = LocalBinder()
    private var wakeLock: PowerManager.WakeLock? = null
    private val CHANNEL_ID = "offline_walkie_talkie_channel"
    private val CALL_CHANNEL_ID = "offline_incoming_call_channel"
    private val NOTIFICATION_ID = 1001
    private val CALL_NOTIFICATION_ID = 2002

    companion object {
        const val ACTION_ACCEPT_CALL = "com.example.ACTION_ACCEPT_CALL"
        const val ACTION_REJECT_CALL = "com.example.ACTION_REJECT_CALL"
        const val EXTRA_CALL_ID = "extra_call_id"
    }

    inner class LocalBinder : Binder() {
        fun getService(): WalkieTalkieService = this@WalkieTalkieService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        acquireWakeLock()
        startInForeground("Ready • 0 Connected Devices", "Channel Clear", isTransmitting = false)
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "OfflineWalkieTalkie::RadioWakeLock"
            ).apply {
                acquire(15 * 60 * 1000L /* 15 minutes timeout */)
            }
        } catch (e: Exception) {
            Log.w("WalkieTalkieService", "Wake lock acquire failed: ${e.message}")
        }
    }

    fun updateNotification(statusText: String, speakerText: String, isTransmitting: Boolean = false) {
        startInForeground(statusText, speakerText, isTransmitting)
    }

    private fun startInForeground(statusText: String, speakerText: String, isTransmitting: Boolean = false) {
        val notification = buildNotification(statusText, speakerText)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val hasMicPermission = ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    if (isTransmitting && hasMicPermission) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    } else {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                    }
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                }
                startForeground(NOTIFICATION_ID, notification, type)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e("WalkieTalkieService", "Failed to startForeground: ${e.message}")
            try {
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, notification)
            } catch (ignored: Exception) {}
        }
    }

    fun showIncomingCallNotification(callerName: String, callId: String) {
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            101,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Accept Action Intent
        val acceptIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_ACCEPT_CALL
            putExtra(EXTRA_CALL_ID, callId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val acceptPendingIntent = PendingIntent.getActivity(
            this,
            102,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Reject Action Intent
        val rejectIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_REJECT_CALL
            putExtra(EXTRA_CALL_ID, callId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val rejectPendingIntent = PendingIntent.getActivity(
            this,
            103,
            rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callNotification = NotificationCompat.Builder(this, CALL_CHANNEL_ID)
            .setContentTitle("Incoming Offline Voice Call")
            .setContentText("$callerName is calling…")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_call, "ACCEPT", acceptPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "REJECT", rejectPendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(CALL_NOTIFICATION_ID, callNotification)
    }

    fun cancelIncomingCallNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(CALL_NOTIFICATION_ID)
    }

    private fun buildNotification(statusText: String, speakerText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(AppConfig.APP_NAME)
            .setContentText("$statusText | $speakerText")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val radioChannel = NotificationChannel(
                CHANNEL_ID,
                "Offline Walkie-Talkie Radio Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active peer-to-peer radio connection and audio transmission status"
                setShowBadge(false)
            }

            val callChannel = NotificationChannel(
                CALL_CHANNEL_ID,
                "Incoming Offline Voice Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent incoming offline peer-to-peer voice calls"
                setShowBadge(true)
                enableVibration(true)
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(radioChannel)
            manager.createNotificationChannel(callChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelIncomingCallNotification()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (ignored: Exception) {}
    }
}
