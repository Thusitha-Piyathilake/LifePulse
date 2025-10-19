package com.example.lifepulse.ui.main.fragments

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.lifepulse.R
import com.example.lifepulse.data.PrefsManager
import com.github.lzyzsd.circleprogress.DonutProgress
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment(), SensorEventListener {

    private lateinit var calendarView: CalendarView
    private lateinit var btnSaveToday: Button
    private lateinit var sensorManager: SensorManager

    //  Donut Progress UI
    private lateinit var donutWater: DonutProgress
    private lateinit var donutSteps: DonutProgress
    private lateinit var donutHabits: DonutProgress
    private lateinit var tvWaterLabel: TextView
    private lateinit var tvStepsLabel: TextView
    private lateinit var tvHabitsLabel: TextView
    private lateinit var tvStreak: TextView

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var todayDate: String = dateFormat.format(Date())
    private var selectedDate: String = todayDate

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        calendarView = view.findViewById(R.id.calendarView)
        btnSaveToday = view.findViewById(R.id.btnSaveToday)

        //  Donuts
        donutWater = view.findViewById(R.id.donutWater)
        donutSteps = view.findViewById(R.id.donutSteps)
        donutHabits = view.findViewById(R.id.donutHabits)

        tvWaterLabel = view.findViewById(R.id.tvWaterLabel)
        tvStepsLabel = view.findViewById(R.id.tvStepsLabel)
        tvHabitsLabel = view.findViewById(R.id.tvHabitsLabel)
        tvStreak = view.findViewById(R.id.tvStreak)

        //  Setup step sensor
        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            Toast.makeText(requireContext(), "Step sensor not available", Toast.LENGTH_SHORT).show()
        }

        //  Handle calendar date selection → OPEN DateDetailsFragment
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            updateProgress(selectedDate)

            val fragment = DateDetailsFragment.newInstance(selectedDate)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        //  Save dummy calories & sleep for today
        btnSaveToday.setOnClickListener {
            val calories = (1500..3000).random()
            val sleep = (5..9).random().toFloat()
            PrefsManager.saveDailyStats(requireContext(), todayDate, calories, sleep)

            updateProgress(todayDate)

            Toast.makeText(requireContext(), "Saved today’s stats!", Toast.LENGTH_SHORT).show()
        }

        //  Initial load
        updateProgress(todayDate)

        return view
    }

    // 🔹 Live footsteps update
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSteps = event.values[0].toInt()

            if (PrefsManager.getStepBaseline(requireContext()) == -1) {
                PrefsManager.saveStepBaseline(requireContext(), totalSteps)
            }
            val baseline = PrefsManager.getStepBaseline(requireContext())
            val todaySteps = totalSteps - baseline

            PrefsManager.saveSteps(requireContext(), todayDate, todaySteps)
            updateProgress(todayDate)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onDestroyView() {
        super.onDestroyView()
        sensorManager.unregisterListener(this)
    }

    // 🔹 Calculate and update progress + streak dynamically
    private fun updateProgress(date: String) {
        //  Habits
        val plannedHabits = PrefsManager.getHabitsForDate(requireContext(), date)
        val completedHabits = plannedHabits.count {
            PrefsManager.getHabitStatus(requireContext(), date, it.name) == "done"
        }
        val completionPercent = if (plannedHabits.isNotEmpty()) {
            (completedHabits * 100) / plannedHabits.size
        } else 0
        PrefsManager.saveDailyCompletion(requireContext(), date, completionPercent)
        if (completionPercent == 100) {
            PrefsManager.updateStreak(requireContext(), date, true)
        }

        //  Water Donut
        val waterIntake = PrefsManager.getWaterIntake(requireContext(), date)
        val waterGoal = PrefsManager.getWaterGoal(requireContext())
        val waterPercent = if (waterGoal > 0) (waterIntake * 100) / waterGoal else 0
        donutWater.progress = waterPercent.toFloat()
        donutWater.text = "$waterPercent%"
        tvWaterLabel.text = "Water Intake: $waterIntake / $waterGoal ml"

        //  Steps Donut
        val steps = PrefsManager.getStepsForDate(requireContext(), date)
        val stepGoal = 5000
        val stepPercent = if (stepGoal > 0) (steps * 100) / stepGoal else 0
        donutSteps.progress = stepPercent.toFloat()
        donutSteps.text = "$stepPercent%"
        tvStepsLabel.text = "Steps: $steps / $stepGoal"

        //  Habits Donut
        donutHabits.progress = completionPercent.toFloat()
        donutHabits.text = "$completionPercent%"
        tvHabitsLabel.text = "Daily Progress"

        //  Streak
        tvStreak.text = "Current Streak: ${PrefsManager.getStreak(requireContext())} days"
    }

    //  Public method so MainActivity can trigger refresh
    fun updateProgressUI() {
        updateProgress(todayDate)
    }
}
