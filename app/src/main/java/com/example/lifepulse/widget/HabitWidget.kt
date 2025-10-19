package com.example.lifepulse.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.lifepulse.R
import com.example.lifepulse.data.PrefsManager
import java.text.SimpleDateFormat
import java.util.*

class HabitWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_habit)

            // ✅ Get today’s completion %
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val percent = PrefsManager.getDailyCompletion(context, today)
            views.setTextViewText(R.id.tvWidgetProgress, "Progress: $percent%")

            // ✅ Quick log button → adds water entry
            val intent = Intent(context, WidgetActionReceiver::class.java).apply {
                action = "ACTION_QUICK_LOG"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btnWidgetQuickLog, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
