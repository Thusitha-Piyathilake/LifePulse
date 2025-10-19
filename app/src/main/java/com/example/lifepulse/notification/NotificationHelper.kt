package com.example.lifepulse.notification

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.lifepulse.R
import com.example.lifepulse.data.PrefsManager
import java.text.SimpleDateFormat
import java.util.*

object NotificationHelper {

    private const val CHANNEL_ID = "habit_channel"

    // 🔹 Schedule habit reminder at a given time/date
    fun scheduleHabitReminder(
        context: Context,
        habitName: String,
        time: String,   // "HH:mm"
        date: String,   // "yyyy-MM-dd"
        duration: Int = 0
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Parse selected date+time → Date
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val triggerDate: Date = formatter.parse("$date $time") ?: return

        // Put parsed Date into Calendar
        val calendar = Calendar.getInstance().apply {
            this.time = triggerDate
        }

        //  Unique requestCode = habitName + date hash
        val requestCode = (habitName + date).hashCode()

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("habitName", habitName)
            putExtra("habitTime", time)
            putExtra("habitDate", date)
            putExtra("habitDuration", duration)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancel any existing alarm first
        alarmManager.cancel(pendingIntent)

        //  Schedule exact alarm (works even in Doze mode)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    // 🔹 Cancel habit reminder
    fun cancelHabitReminder(context: Context, habitName: String, date: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val requestCode = (habitName + date).hashCode()

        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }

    // 🔹 Show instant notification (used by hydration or habit reminders)
    fun showNotification(context: Context, title: String, message: String) {
        createChannel(context)

        //  Save the notification with timestamp so it appears in-app
        val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val formattedMessage = "[$time] $title - $message"
        PrefsManager.addNotification(context, formattedMessage)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        with(NotificationManagerCompat.from(context)) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }

    //  Create notification channel (O+)
    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Habit Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for habits & hydration reminders"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                setSound(
                    android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                    Notification.AUDIO_ATTRIBUTES_DEFAULT
                )
            }

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
