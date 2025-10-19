package com.example.lifepulse.data

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.example.lifepulse.notification.NotificationHelper
import com.example.lifepulse.ui.widget.HabitWidget
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object PrefsManager {
    private const val PREFS_NAME = "LifePulsePrefs"
    private const val HABITS_KEY = "habits"
    private const val SELECTED_HABITS_KEY = "selected_habits"
    private const val HABIT_LOGS_KEY = "habit_logs"
    private const val STATS_KEY = "daily_stats"
    private const val MOOD_LOGS_KEY = "mood_logs"
    private const val STEPS_KEY = "steps_logs"
    private const val BASELINE_KEY = "step_baseline"
    private const val HABIT_STATUS_KEY = "habit_status_logs"
    private const val COMPLETION_KEY = "completion_logs"
    private const val STREAK_KEY = "habit_streak"
    private const val LAST_COMPLETION_DATE = "last_completion_date"

    //  Water keys
    private const val KEY_WATER_GOAL = "water_goal"
    private const val WATER_LOGS_KEY = "water_logs" // per-date water intake

    //  Custom habits key
    private const val CUSTOM_HABITS_KEY = "customHabits"

    //  Notifications key (improved)
    private const val KEY_NOTIFICATIONS = "notifications_list"

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    //  Save a habit + schedule its reminder
    fun saveHabit(context: Context, name: String, time: String, duration: Int) {
        val prefs = getPrefs(context)
        val habits = JSONArray(prefs.getString(HABITS_KEY, "[]"))

        val habit = JSONObject().apply {
            put("name", name)
            put("time", time)
            put("duration", duration)
        }
        habits.put(habit)
        prefs.edit().putString(HABITS_KEY, habits.toString()).apply()

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val now = Calendar.getInstance()
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)

        val habitTime = sdf.parse("$todayDate $time")
        val calendar = Calendar.getInstance()
        if (habitTime != null && habitTime.before(now.time)) {
            calendar.time = habitTime
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        } else if (habitTime != null) {
            calendar.time = habitTime
        }

        NotificationHelper.scheduleHabitReminder(
            context,
            name,
            time,
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time),
            duration
        )
    }

    fun getHabits(context: Context): List<Habit> {
        val prefs = getPrefs(context)
        val habits = JSONArray(prefs.getString(HABITS_KEY, "[]"))
        val list = mutableListOf<Habit>()
        for (i in 0 until habits.length()) {
            val obj = habits.getJSONObject(i)
            list.add(Habit(obj.getString("name"), obj.getString("time"), obj.getInt("duration")))
        }
        return list
    }

    fun saveSelectedHabits(context: Context, habits: List<String>) {
        getPrefs(context).edit().putStringSet(SELECTED_HABITS_KEY, habits.toSet()).apply()
    }

    fun getSelectedHabits(context: Context): List<String> {
        return getPrefs(context).getStringSet(SELECTED_HABITS_KEY, emptySet())?.toList() ?: emptyList()
    }

    fun saveHabitLog(context: Context, habitName: String, time: String, duration: Int, date: String) {
        val prefs = getPrefs(context)
        val logs = JSONArray(prefs.getString(HABIT_LOGS_KEY, "[]"))

        val log = JSONObject().apply {
            put("date", date)
            put("habit", habitName)
            put("time", time)
            put("duration", duration)
        }
        logs.put(log)
        prefs.edit().putString(HABIT_LOGS_KEY, logs.toString()).apply()
    }

    fun getHabitLogsByDate(context: Context, date: String): List<String> {
        val prefs = getPrefs(context)
        val logs = JSONArray(prefs.getString(HABIT_LOGS_KEY, "[]"))
        val list = mutableListOf<String>()
        for (i in 0 until logs.length()) {
            val obj = logs.getJSONObject(i)
            if (obj.getString("date") == date) {
                val habit = obj.getString("habit")
                val time = obj.getString("time")
                val duration = obj.getInt("duration")
                list.add("$habit at $time for $duration mins")
            }
        }
        return list
    }

    fun saveHabitsForDate(context: Context, date: String, newHabits: List<Habit>) {
        val prefs = getPrefs(context)
        val existing = getHabitsForDate(context, date).toMutableList()

        newHabits.forEach { habit ->
            if (existing.none { it.name == habit.name }) {
                existing.add(habit)
            }
        }

        val jsonArray = JSONArray()
        existing.forEach { habit ->
            val obj = JSONObject().apply {
                put("name", habit.name)
                put("time", habit.time)
                put("duration", habit.duration)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("habits_$date", jsonArray.toString()).apply()
    }

    fun getHabitsForDate(context: Context, date: String): List<Habit> {
        val prefs = getPrefs(context)
        val jsonString = prefs.getString("habits_$date", null) ?: return emptyList()
        val jsonArray = JSONArray(jsonString)
        val list = mutableListOf<Habit>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            list.add(Habit(obj.getString("name"), obj.getString("time"), obj.getInt("duration")))
        }
        return list
    }

    private fun overwriteHabitsForDate(context: Context, date: String, habits: List<Habit>) {
        val prefs = getPrefs(context)
        val jsonArray = JSONArray()
        habits.forEach { habit ->
            val obj = JSONObject().apply {
                put("name", habit.name)
                put("time", habit.time)
                put("duration", habit.duration)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("habits_$date", jsonArray.toString()).apply()
    }

    fun removeHabit(context: Context, date: String, habitName: String) {
        val prefs = getPrefs(context)
        val current = getHabitsForDate(context, date).toMutableList()
        current.removeAll { it.name == habitName }
        overwriteHabitsForDate(context, date, current)

        val logs = JSONArray(prefs.getString(HABIT_LOGS_KEY, "[]"))
        val newLogs = JSONArray()
        for (i in 0 until logs.length()) {
            val obj = logs.getJSONObject(i)
            if (!(obj.getString("date") == date && obj.getString("habit") == habitName)) {
                newLogs.put(obj)
            }
        }
        prefs.edit().putString(HABIT_LOGS_KEY, newLogs.toString()).apply()

        NotificationHelper.cancelHabitReminder(context, habitName, date)
    }

    fun isHabitLogged(context: Context, date: String, habitName: String): Boolean {
        val prefs = getPrefs(context)
        val logs = JSONArray(prefs.getString(HABIT_LOGS_KEY, "[]"))
        for (i in 0 until logs.length()) {
            val obj = logs.getJSONObject(i)
            if (obj.getString("date") == date && obj.getString("habit") == habitName) {
                return true
            }
        }
        return false
    }

    fun saveHabitStatus(context: Context, date: String, habit: String, status: String) {
        val prefs = getPrefs(context)
        val statuses = JSONArray(prefs.getString(HABIT_STATUS_KEY, "[]"))
        val newArray = JSONArray()
        for (i in 0 until statuses.length()) {
            val obj = statuses.getJSONObject(i)
            if (!(obj.getString("date") == date && obj.getString("habit") == habit)) {
                newArray.put(obj)
            }
        }
        val entry = JSONObject().apply {
            put("date", date)
            put("habit", habit)
            put("status", status)
        }
        newArray.put(entry)
        prefs.edit().putString(HABIT_STATUS_KEY, newArray.toString()).apply()
    }

    fun getHabitStatus(context: Context, date: String, habit: String): String {
        val prefs = getPrefs(context)
        val statuses = JSONArray(prefs.getString(HABIT_STATUS_KEY, "[]"))
        for (i in 0 until statuses.length()) {
            val obj = statuses.getJSONObject(i)
            if (obj.getString("date") == date && obj.getString("habit") == habit) {
                return when {
                    obj.has("status") -> obj.getString("status")
                    obj.has("done") -> if (obj.getBoolean("done")) "done" else "pending"
                    else -> "pending"
                }
            }
        }
        return "pending"
    }

    fun saveDailyStats(context: Context, date: String, calories: Int, sleep: Float) {
        val prefs = getPrefs(context)
        val stats = JSONArray(prefs.getString(STATS_KEY, "[]"))
        val entry = JSONObject().apply {
            put("date", date)
            put("calories", calories)
            put("sleep", sleep)
        }
        stats.put(entry)
        prefs.edit().putString(STATS_KEY, stats.toString()).apply()
    }

    fun getDailyStats(context: Context, date: String): Pair<Int, Float> {
        val prefs = getPrefs(context)
        val stats = JSONArray(prefs.getString(STATS_KEY, "[]"))
        for (i in 0 until stats.length()) {
            val obj = stats.getJSONObject(i)
            if (obj.getString("date") == date) {
                return Pair(obj.getInt("calories"), obj.getDouble("sleep").toFloat())
            }
        }
        return Pair(0, 0f)
    }

    fun saveMood(context: Context, date: String, mood: String, time: String) {
        val prefs = getPrefs(context)
        val moods = JSONArray(prefs.getString(MOOD_LOGS_KEY, "[]"))
        val entry = JSONObject().apply {
            put("date", date)
            put("mood", mood)
            put("time", time)
        }
        moods.put(entry)
        prefs.edit().putString(MOOD_LOGS_KEY, moods.toString()).apply()
    }

    fun getMoodByDate(context: Context, date: String): List<String> {
        val prefs = getPrefs(context)
        val moods = JSONArray(prefs.getString(MOOD_LOGS_KEY, "[]"))
        val list = mutableListOf<String>()
        for (i in 0 until moods.length()) {
            val obj = moods.getJSONObject(i)
            if (obj.getString("date") == date) {
                list.add("${obj.getString("mood")} at ${obj.getString("time")}")
            }
        }
        return list
    }

    //  Steps
    fun saveSteps(context: Context, date: String, steps: Int) {
        val prefs = getPrefs(context)
        val stepsArray = JSONArray(prefs.getString(STEPS_KEY, "[]"))
        val newArray = JSONArray()
        var updated = false

        for (i in 0 until stepsArray.length()) {
            val obj = stepsArray.getJSONObject(i)
            if (obj.getString("date") == date) {
                obj.put("steps", steps)
                updated = true
            }
            newArray.put(obj)
        }

        if (!updated) {
            val entry = JSONObject().apply {
                put("date", date)
                put("steps", steps)
            }
            newArray.put(entry)
        }

        prefs.edit().putString(STEPS_KEY, newArray.toString()).apply()
    }

    fun getStepsForDate(context: Context, date: String): Int {
        val prefs = getPrefs(context)
        val stepsArray = JSONArray(prefs.getString(STEPS_KEY, "[]"))
        for (i in 0 until stepsArray.length()) {
            val obj = stepsArray.getJSONObject(i)
            if (obj.getString("date") == date) {
                return obj.getInt("steps")
            }
        }
        return 0
    }

    fun saveStepBaseline(context: Context, baseline: Int) {
        getPrefs(context).edit().putInt(BASELINE_KEY, baseline).apply()
    }

    fun getStepBaseline(context: Context): Int {
        return getPrefs(context).getInt(BASELINE_KEY, -1)
    }

    //  Daily completion & streak tracking
    fun saveDailyCompletion(context: Context, date: String, percent: Int) {
        val prefs = getPrefs(context)
        val logs = JSONArray(prefs.getString(COMPLETION_KEY, "[]"))
        val newArray = JSONArray()
        for (i in 0 until logs.length()) {
            val obj = logs.getJSONObject(i)
            if (obj.getString("date") != date) newArray.put(obj)
        }
        val entry = JSONObject().apply {
            put("date", date)
            put("percent", percent)
        }
        newArray.put(entry)
        prefs.edit().putString(COMPLETION_KEY, newArray.toString()).apply()

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, HabitWidget::class.java)
        val ids = appWidgetManager.getAppWidgetIds(componentName)
        for (id in ids) {
            HabitWidget.updateAppWidget(context, appWidgetManager, id)
        }
    }

    fun getDailyCompletion(context: Context, date: String): Int {
        val prefs = getPrefs(context)
        val logs = JSONArray(prefs.getString(COMPLETION_KEY, "[]"))
        for (i in 0 until logs.length()) {
            val obj = logs.getJSONObject(i)
            if (obj.getString("date") == date) {
                return obj.getInt("percent")
            }
        }
        return 0
    }

    fun updateStreak(context: Context, date: String, allCompleted: Boolean) {
        val prefs = getPrefs(context)
        val lastDate = prefs.getString(LAST_COMPLETION_DATE, null)
        var streak = prefs.getInt(STREAK_KEY, 0)

        if (allCompleted) {
            if (lastDate != null) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val last = sdf.parse(lastDate)
                val current = sdf.parse(date)
                val diff = ((current.time - last.time) / (1000 * 60 * 60 * 24)).toInt()
                if (diff == 1) {
                    streak += 1
                } else if (diff > 1) {
                    streak = 1
                }
            } else {
                streak = 1
            }
            prefs.edit().putString(LAST_COMPLETION_DATE, date).putInt(STREAK_KEY, streak).apply()
        }
    }

    fun getStreak(context: Context): Int {
        return getPrefs(context).getInt(STREAK_KEY, 0)
    }

    //  Water Tracking (per date)
    //  Store daily step history for graphs and reset tracking
    fun addStepHistory(context: Context, date: String, steps: Int) {
        val prefs = getPrefs(context)
        val json = prefs.getString("steps_history", "[]")
        val array = JSONArray(json)

        // Prevent duplicates for same date
        val newArray = JSONArray()
        var updated = false
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            if (obj.getString("date") == date) {
                obj.put("steps", steps)
                updated = true
            }
            newArray.put(obj)
        }

        if (!updated) {
            val obj = JSONObject().apply {
                put("date", date)
                put("steps", steps)
            }
            newArray.put(obj)
        }

        prefs.edit().putString("steps_history", newArray.toString()).apply()
    }

    //  Retrieve step history list (for StepsHistoryFragment)
    fun getStepsHistory(context: Context): List<Pair<String, Int>> {
        val prefs = getPrefs(context)
        val json = prefs.getString("steps_history", "[]")
        val array = JSONArray(json)
        val list = mutableListOf<Pair<String, Int>>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(Pair(obj.getString("date"), obj.getInt("steps")))
        }
        return list.sortedByDescending { it.first } // newest first
    }

    fun saveWaterGoal(context: Context, goal: Int) {
        getPrefs(context).edit().putInt(KEY_WATER_GOAL, goal).apply()
    }

    fun getWaterGoal(context: Context): Int {
        return getPrefs(context).getInt(KEY_WATER_GOAL, 2000)
    }

    fun saveWaterIntake(context: Context, date: String, intake: Int) {
        val prefs = getPrefs(context)
        val logs = JSONArray(prefs.getString(WATER_LOGS_KEY, "[]"))
        val newArray = JSONArray()
        var updated = false

        for (i in 0 until logs.length()) {
            val obj = logs.getJSONObject(i)
            if (obj.getString("date") == date) {
                obj.put("intake", intake)
                updated = true
            }
            newArray.put(obj)
        }

        if (!updated) {
            val entry = JSONObject().apply {
                put("date", date)
                put("intake", intake)
            }
            newArray.put(entry)
        }

        prefs.edit().putString(WATER_LOGS_KEY, newArray.toString()).apply()
    }

    fun getWaterIntake(context: Context, date: String): Int {
        val prefs = getPrefs(context)
        val logs = JSONArray(prefs.getString(WATER_LOGS_KEY, "[]"))
        for (i in 0 until logs.length()) {
            val obj = logs.getJSONObject(i)
            if (obj.getString("date") == date) {
                return obj.getInt("intake")
            }
        }
        return 0
    }

    fun resetWaterIntake(context: Context, date: String) {
        saveWaterIntake(context, date, 0)
    }

    //  =========================
    //  Custom Habits
    //  =========================
    fun saveCustomHabit(context: Context, habit: String, iconRes: Int) {
        val prefs = getPrefs(context)
        val existingJson = prefs.getString(CUSTOM_HABITS_KEY, "[]")
        val jsonArray = JSONArray(existingJson)

        var exists = false
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            if (obj.getString("name") == habit) {
                exists = true
                break
            }
        }

        if (!exists) {
            val obj = JSONObject().apply {
                put("name", habit)
                put("icon", iconRes)
            }
            jsonArray.put(obj)
            prefs.edit().putString(CUSTOM_HABITS_KEY, jsonArray.toString()).apply()
        }
    }

    fun getCustomHabits(context: Context): List<Pair<String, Int>> {
        val prefs = getPrefs(context)
        val jsonArray = JSONArray(prefs.getString(CUSTOM_HABITS_KEY, "[]"))
        val list = mutableListOf<Pair<String, Int>>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            list.add(Pair(obj.getString("name"), obj.getInt("icon")))
        }
        return list
    }

    fun removeCustomHabit(context: Context, habit: String) {
        val prefs = getPrefs(context)
        val jsonArray = JSONArray(prefs.getString(CUSTOM_HABITS_KEY, "[]"))
        val newArray = JSONArray()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            if (obj.getString("name") != habit) {
                newArray.put(obj)
            }
        }
        prefs.edit().putString(CUSTOM_HABITS_KEY, newArray.toString()).apply()
    }

    //  =========================
    //  Notifications Management
    //  =========================
    fun addNotification(context: Context, message: String) {
        val current = getNotifications(context).toMutableList()
        if (!current.contains(message)) {
            current.add(0, message) // newest first
        }
        val json = Gson().toJson(current)
        getPrefs(context).edit().putString(KEY_NOTIFICATIONS, json).apply()
    }

    fun getNotifications(context: Context): List<String> {
        val json = getPrefs(context).getString(KEY_NOTIFICATIONS, null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun clearNotifications(context: Context) {
        getPrefs(context).edit().remove(KEY_NOTIFICATIONS).apply()
    }
}
