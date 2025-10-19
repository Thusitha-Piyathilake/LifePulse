package com.example.lifepulse.ui.main.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.lifepulse.R
import com.example.lifepulse.data.PrefsManager
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import java.text.SimpleDateFormat
import java.util.*

class StepsFragment : Fragment(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private lateinit var tvSteps: TextView
    private lateinit var tvCalories: TextView
    private lateinit var pieChart: PieChart

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val stepGoal = 2000
    private val caloriesPerStep = 0.04f   // kcal per step (approx)

    companion object {
        private const val REQ_ACTIVITY_RECOGNITION = 42
        private const val KEY_LAST_DATE = "last_step_date"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_steps, container, false)

        tvSteps = view.findViewById(R.id.tvSteps)
        tvCalories = view.findViewById(R.id.tvCalories)
        pieChart = view.findViewById(R.id.stepsPieChart)

        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        setupPieChart()
        checkDateChange()
        loadStepsForToday()
        ensureActivityRecognitionPermission()

        return view
    }

    //  Check if day changed -> archive old steps & reset for today
    private fun checkDateChange() {
        val prefs = requireContext().getSharedPreferences("LifePulsePrefs", Context.MODE_PRIVATE)
        val lastDate = prefs.getString(KEY_LAST_DATE, null)
        val today = dateFmt.format(Date())

        if (lastDate != null && lastDate != today) {
            val lastSteps = PrefsManager.getStepsForDate(requireContext(), lastDate)

            //  Save previous day's steps into history
            if (lastSteps > 0) {
                PrefsManager.addStepHistory(requireContext(), lastDate, lastSteps)
            }

            //  Reset today's step data
            PrefsManager.saveSteps(requireContext(), today, 0)
            PrefsManager.saveStepBaseline(requireContext(), -1)
        }

        prefs.edit().putString(KEY_LAST_DATE, today).apply()
    }

    // ---- UI ----
    private fun setupPieChart() {
        pieChart.description.isEnabled = false
        pieChart.isRotationEnabled = false
        pieChart.setDrawEntryLabels(false)
        pieChart.legend.isEnabled = false

        pieChart.isDrawHoleEnabled = true
        pieChart.holeRadius = 70f
        pieChart.transparentCircleRadius = 0f
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.setCenterTextColor(Color.BLACK)
        pieChart.setCenterTextSize(20f)
    }

    private fun updateUI(steps: Int) {
        val percentExact = (steps.toFloat() / stepGoal) * 100f
        val percentForRing = when {
            percentExact <= 0f -> 0f
            percentExact < 1f -> 1f
            else -> percentExact.coerceAtMost(100f)
        }

        val entries = listOf(
            PieEntry(percentForRing, ""),
            PieEntry(100f - percentForRing, "")
        )

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                Color.parseColor("#1E88E5"), // blue
                Color.parseColor("#E0E0E0")  // grey
            )
            setDrawValues(false)
            sliceSpace = 0f
        }

        pieChart.centerText =
            "${steps}\n${String.format(Locale.getDefault(), "%.1f", percentExact.coerceAtMost(100f))}%"
        pieChart.data = PieData(dataSet)
        pieChart.invalidate()

        tvSteps.text = "$steps / $stepGoal steps"
        val burnedCalories = steps * caloriesPerStep
        tvCalories.text = "Burned: ${String.format(Locale.getDefault(), "%.1f", burnedCalories)} kcal"
    }

    private fun loadStepsForToday() {
        val today = dateFmt.format(Date())
        updateUI(PrefsManager.getStepsForDate(requireContext(), today))
    }

    // ---- Permission ----
    private fun ensureActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val granted = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestPermissions(
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    REQ_ACTIVITY_RECOGNITION
                )
                return
            }
        }
        startListening()
    }

    private fun startListening() {
        stepSensor?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        checkDateChange()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startListening()
        }
    }

    //  Save today's steps into history whenever user leaves the screen
    override fun onPause() {
        super.onPause()
        stopListening()

        val today = dateFmt.format(Date())
        val stepsToday = PrefsManager.getStepsForDate(requireContext(), today)
        if (stepsToday > 0) {
            PrefsManager.addStepHistory(requireContext(), today, stepsToday)
        }
    }

    // ---- Step logic ----
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return

        val cumulative = event.values[0].toInt()
        val today = dateFmt.format(Date())

        var baseline = PrefsManager.getStepBaseline(requireContext())
        val savedToday = PrefsManager.getStepsForDate(requireContext(), today)

        if (baseline == -1) {
            baseline = cumulative - savedToday
            PrefsManager.saveStepBaseline(requireContext(), baseline)
        }

        val currentSteps = (cumulative - baseline).coerceAtLeast(0)
        PrefsManager.saveSteps(requireContext(), today, currentSteps)

        updateUI(currentSteps)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
