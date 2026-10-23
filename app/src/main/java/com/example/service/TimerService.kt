package com.example.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class TimerService : Service() {

    companion object {
        const val CHANNEL_ID = "IstaTimerChannel"
        const val NOTIFICATION_ID = 101

        const val ACTION_START_SITTING = "action_start_sitting"
        const val ACTION_START_STANDING = "action_start_standing"
        const val ACTION_SITTING_TIME_UP = "action_sitting_time_up"
        const val EXTRA_DURATION_MIN = "extra_duration_min"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_STICKY

        when (action) {
            ACTION_START_SITTING -> {
                val duration = intent.getIntExtra(EXTRA_DURATION_MIN, 25)
                startForeground(NOTIFICATION_ID, buildNotification(
                    "ایستا (Ista) - ردیابی فعال",
                    "در حال ردیابی نشستن شما. هدف: $duration دقیقه دیگر."
                ))
            }
            ACTION_START_STANDING -> {
                startForeground(NOTIFICATION_ID, buildNotification(
                    "زمان برخاستن است!",
                    "پیاده‌روی کنید! سنسورهای حرکتی در حال ردیابی هستند."
                ))
            }
            ACTION_SITTING_TIME_UP -> {
                startForeground(NOTIFICATION_ID, buildNotification(
                    "زمان برخاستن از صندلی است!",
                    "شما بیش از حد مجاز نشسته‌اید. لطفا برخیزید یا دکمه تاخیر را بزنید."
                ))
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(title: String, content: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.presence_online)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "ردیاب هوشمند ایستا",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "ردیابی هوشمند وضعیت ایستادن و نشستن"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}
