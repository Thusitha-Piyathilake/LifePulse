package com.example.lifepulse.ui.main.fragments

import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.lifepulse.R
import com.github.lzyzsd.circleprogress.DonutProgress
import java.util.Locale

class RelaxFragment : Fragment() {

    private lateinit var timer: CountDownTimer
    private lateinit var progressCircle: DonutProgress
    private lateinit var tvTime: TextView
    private lateinit var btnPlayPause: ImageButton

    private var mediaPlayer: MediaPlayer? = null
    private var isRunning = false
    private var totalTime: Long = 0
    private var remainingTime: Long = 0
    private var selectedSound: Int = R.raw.relax1 // default sound

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_relax, container, false)

        //  UI elements
        progressCircle = view.findViewById(R.id.progressCircle)
        tvTime = view.findViewById(R.id.tvTime)
        btnPlayPause = view.findViewById(R.id.btnPlayPause)

        // ⏱ Pick time
        tvTime.setOnClickListener { showTimePicker() }

        // ▶ Play/Pause
        btnPlayPause.setOnClickListener {
            if (isRunning) pauseTimer()
            else startTimer()
        }

        //  Music selection
        view.findViewById<ImageView>(R.id.btnMusic1).setOnClickListener {
            changeSound(R.raw.relax1, "Ocean Waves 🌊")
        }
        view.findViewById<ImageView>(R.id.btnMusic2).setOnClickListener {
            changeSound(R.raw.relax2, "Rain 🌧")
        }
        view.findViewById<ImageView>(R.id.btnMusic3).setOnClickListener {
            changeSound(R.raw.relax3, "Birds 🐦")
        }
        view.findViewById<ImageView>(R.id.btnMusic4).setOnClickListener {
            changeSound(R.raw.relax4, "Piano 🎹")
        }

        return view
    }

    private fun showTimePicker() {
        val options = arrayOf("1 min", "3 min", "5 min", "10 min")
        val times = arrayOf(60_000L, 180_000L, 300_000L, 600_000L)

        AlertDialog.Builder(requireContext())
            .setTitle("Select Relax Time")
            .setItems(options) { _, which ->
                totalTime = times[which]
                remainingTime = totalTime
                updateUI(totalTime)
            }
            .show()
    }

    /**  Change sound and restart if playing */
    private fun changeSound(newSound: Int, label: String) {
        selectedSound = newSound
        Toast.makeText(requireContext(), "$label selected", Toast.LENGTH_SHORT).show()

        if (isRunning) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(requireContext(), selectedSound)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        }
    }

    private fun startTimer() {
        if (totalTime == 0L) {
            Toast.makeText(requireContext(), "Please set time", Toast.LENGTH_SHORT).show()
            return
        }

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(requireContext(), selectedSound)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()

        timer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTime = millisUntilFinished
                updateUI(remainingTime)
            }

            override fun onFinish() {
                stopTimer()
                Toast.makeText(requireContext(), "Relax session finished 🌿", Toast.LENGTH_LONG).show()
            }
        }.start()

        isRunning = true
        btnPlayPause.setImageResource(R.drawable.ic_pause)
    }

    private fun pauseTimer() {
        timer.cancel()
        mediaPlayer?.pause()
        isRunning = false
        btnPlayPause.setImageResource(R.drawable.ic_play)
    }

    private fun stopTimer() {
        timer.cancel()
        mediaPlayer?.stop()
        isRunning = false
        remainingTime = totalTime
        btnPlayPause.setImageResource(R.drawable.ic_play)
        updateUI(totalTime)
    }

    private fun updateUI(timeMillis: Long) {
        val minutes = (timeMillis / 1000) / 60
        val seconds = (timeMillis / 1000) % 60
        tvTime.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

        if (totalTime > 0) {
            val progress = (timeMillis.toFloat() / totalTime) * 100f
            progressCircle.progress = progress
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::timer.isInitialized) {
            timer.cancel()
        }
        mediaPlayer?.release()
    }
}
