package com.example.lifepulse.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.lifepulse.R
import com.example.lifepulse.data.PrefsManager
import com.example.lifepulse.ui.alarm.AlarmActivity
import com.example.lifepulse.ui.workers.HydrationWorkerActivity

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitName = intent.getStringExtra("habitName") ?: "Habit"
        val isHydration = intent.getBooleanExtra("isHydration", false)
        val message = "Time for your habit: $habitName"

        //  Wake CPU and screen
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
            "LifePulse:ReminderWakeLock"
        )
        try {
            wakeLock.acquire(60 * 1000L)
        } catch (_: Exception) { }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "habit_channel"

        //  Create channel with alarm sound + vibration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmSound: Uri =
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val channel = NotificationChannel(
                channelId,
                "Habit Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for your habits"
                enableLights(true)
                lightColor = Color.GREEN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setSound(alarmSound, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }

        //  Intent to open AlarmActivity full screen
        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra("habitName", habitName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingAlarmIntent = PendingIntent.getActivity(
            context,
            habitName.hashCode(),
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        //  Show notification
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle("⏰ Habit Reminder")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setFullScreenIntent(pendingAlarmIntent, true)
                .build()

            NotificationManagerCompat.from(context)
                .notify(habitName.hashCode(), notification)
        }

        //  Save this notification → to show under bell icon
        PrefsManager.addNotification(context, "⏰ $message")

        //  Reschedule hydration reminder if ON
        if (isHydration || intent.action == "com.example.lifepulse.ACTION_HYDRATION_REMINDER") {
            val prefs = context.getSharedPreferences("HydrationPrefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("hydration_switch", false)) {
                HydrationWorkerActivity.scheduleNextHydration(context)
            }
        }

        //  Release wake lock safely
        try {
            if (wakeLock.isHeld) wakeLock.release()
        } catch (_: Exception) { }
    }
}
