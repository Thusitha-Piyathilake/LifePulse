package com.example.lifepulse.ui.alarm

import android.media.MediaPlayer
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.lifepulse.R

class AlarmActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //  Force activity to show over lock screen & wake the device
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        setContentView(R.layout.activity_alarm)

        val habitName = intent.getStringExtra("habitName") ?: "Habit"
        val tvHabit = findViewById<TextView>(R.id.tvHabitName)
        val btnStop = findViewById<Button>(R.id.btnStopAlarm)

        tvHabit.text = "Time for your habit: $habitName"

        //  Play alarm sound in loop
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()

        //  Stop alarm
        btnStop.setOnClickListener {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}
