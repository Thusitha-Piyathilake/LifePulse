package com.example.lifepulse.ui.workers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lifepulse.R
import com.example.lifepulse.data.PrefsManager
import com.example.lifepulse.notification.ReminderReceiver
import java.text.SimpleDateFormat
import java.util.*

class HydrationWorkerActivity : AppCompatActivity() {

    private lateinit var switchHydration: Switch
    private lateinit var btnDrinkWater: Button
    private lateinit var btnViewHistory: Button
    private lateinit var tvCountdown: TextView

    private val PREFS_NAME = "HydrationPrefs"
    private val KEY_SWITCH_STATE = "hydration_switch"
    private val KEY_NEXT_TRIGGER = "hydration_next_trigger"

    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hydration_worker)

        switchHydration = findViewById(R.id.switchHydration)
        btnDrinkWater = findViewById(R.id.btnDrinkWater)
        btnViewHistory = findViewById(R.id.btnViewHistory)
        tvCountdown = findViewById(R.id.tvCountdown) // <-- Add a TextView with this id in the layout

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isOn = prefs.getBoolean(KEY_SWITCH_STATE, false)

        // ✅ Restore switch state + keep alarm alive if ON
        switchHydration.isChecked = isOn
        if (isOn) {
            val next = prefs.getLong(KEY_NEXT_TRIGGER, -1L)
            if (next > System.currentTimeMillis()) {
                startCountdown(next)
            } else {
                scheduleNextHydration(this) // immediately reschedule from now
            }
        } else {
            stopCountdown()
        }

        // ✅ Toggle ON/OFF
        switchHydration.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(KEY_SWITCH_STATE, checked).apply()
            if (checked) {
                scheduleNextHydration(this)
                Toast.makeText(this, "Hydration reminder ON (every 1 hour)", Toast.LENGTH_SHORT).show()
            } else {
                cancelHydrationReminder(this)
                stopCountdown()
                Toast.makeText(this, "Hydration reminder OFF", Toast.LENGTH_SHORT).show()
            }
        }

        // ✅ Quick log
        btnDrinkWater.setOnClickListener {
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            PrefsManager.saveMood(this, today(), "💧 Water Drank", time)
            Toast.makeText(this, "Water logged at $time", Toast.LENGTH_SHORT).show()
        }

        // ✅ History button
        btnViewHistory.setOnClickListener {
            startActivity(Intent(this, HydrationHistoryActivity::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCountdown()
    }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun startCountdown(nextTriggerMs: Long) {
        stopCountdown()
        val remaining = nextTriggerMs - System.currentTimeMillis()
        if (remaining <= 0) {
            tvCountdown.text = "Next reminder: soon ⏳"
            return
        }

        countDownTimer = object : CountDownTimer(remaining, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val h = millisUntilFinished / 3_600_000
                val m = (millisUntilFinished / 60_000) % 60
                val s = (millisUntilFinished / 1000) % 60
                tvCountdown.text = String.format(Locale.getDefault(), "Next reminder in %02d:%02d:%02d", h, m, s)
            }

            override fun onFinish() {
                tvCountdown.text = "Next reminder: soon ⏳"
            }
        }.start()
    }

    private fun stopCountdown() {
        countDownTimer?.cancel()
        countDownTimer = null
        tvCountdown.text = ""
    }

    companion object {
        private const val REQUEST_CODE = 2001
        private const val INTERVAL_MS = 60 * 60 * 1000L // ✅ 1 hour interval
        private const val ACTION_HYDRATION = "com.example.lifepulse.ACTION_HYDRATION_REMINDER"

        fun scheduleNextHydration(context: Context, delayMs: Long = INTERVAL_MS) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val prefs = context.getSharedPreferences("HydrationPrefs", Context.MODE_PRIVATE)

            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_HYDRATION
                putExtra("habitName", "Drink Water 💧")
                putExtra("isHydration", true)
            }

            val pi = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Cancel any existing alarm before setting a new one
            alarmManager.cancel(pi)

            val triggerAt = System.currentTimeMillis() + delayMs
            try {
                // ✅ Works reliably in Doze mode
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pi
                )
            } catch (e: Exception) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pi
                )
            }

            // Save for countdown restoration
            prefs.edit().putLong("hydration_next_trigger", triggerAt).apply()

            // If we're currently inside the activity, start/refresh the countdown immediately
            if (context is HydrationWorkerActivity) {
                context.startCountdown(triggerAt)
            }

            // Optional debug toast
            Toast.makeText(context, "Next hydration reminder in 1 hour ⏰", Toast.LENGTH_SHORT).show()
        }

        fun cancelHydrationReminder(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_HYDRATION
            }
            val pi = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pi)
            context.getSharedPreferences("HydrationPrefs", Context.MODE_PRIVATE)
                .edit().remove("hydration_next_trigger").apply()
        }
    }
}
