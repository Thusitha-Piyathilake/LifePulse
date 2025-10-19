package com.example.lifepulse.ui.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.lifepulse.data.PrefsManager
import java.text.SimpleDateFormat
import java.util.*

class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "ACTION_QUICK_LOG") {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

            // ✅ Quick log hydration
            PrefsManager.saveMood(context, today, "💧 Water Drank", time)
            Toast.makeText(context, "Water logged!", Toast.LENGTH_SHORT).show()

            // ✅ Refresh widget
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = android.content.ComponentName(context, HabitWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (id in appWidgetIds) {
                HabitWidget.updateAppWidget(context, appWidgetManager, id)
            }
        }
    }
}
