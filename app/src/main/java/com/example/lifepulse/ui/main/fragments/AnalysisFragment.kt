package com.example.lifepulse.ui.main.fragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.lifepulse.R
import com.example.lifepulse.data.PrefsManager
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*


class AnalysisFragment : Fragment() {

    private val moods = mutableListOf<MoodEntry>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_analysis, container, false)

        val pieChart = view.findViewById<PieChart>(R.id.pieChart)
        val barChart = view.findViewById<BarChart>(R.id.barChart)
        val habitCompletionChart = view.findViewById<LineChart>(R.id.habitCompletionChart)
        val barChartWater = view.findViewById<BarChart>(R.id.barChartWater)
        val barChartSteps = view.findViewById<BarChart>(R.id.barChartSteps)

        // ✅ Load moods
        loadMoods()

        // ✅ Setup existing charts
        setupPieChart(pieChart)
        setupBarChart(barChart)
        setupHabitCompletionChart(habitCompletionChart)

        // ✅ Setup new charts
        setupWaterIntakeChart(barChartWater)
        setupStepsChart(barChartSteps)

        return view
    }

    private fun loadMoods() {
        val prefs = requireActivity().getSharedPreferences("LifePulsePrefs", Context.MODE_PRIVATE)
        val json = prefs.getString("moods", null)
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<MutableList<MoodEntry>>() {}.type
            moods.clear()
            moods.addAll(Gson().fromJson(json, type))
        }
    }

    // 🔹 Filter moods to last 7 days only
    private fun getLast7DaysMoods(): List<MoodEntry> {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val weekAgo = calendar.time

        return moods.filter {
            try {
                val date = sdf.parse(it.timestamp)
                date != null && !date.before(weekAgo)
            } catch (e: Exception) {
                false
            }
        }
    }

    // 🔹 Pie chart: mood distribution (last 7 days)
    private fun setupPieChart(pieChart: PieChart) {
        val lastWeekMoods = getLast7DaysMoods()
        val moodCounts = lastWeekMoods.groupingBy { it.mood }.eachCount()

        if (moodCounts.isEmpty()) {
            pieChart.clear()
            pieChart.centerText = "No data"
            return
        }

        val entries = moodCounts.map { (mood, count) ->
            PieEntry(count.toFloat(), mood)
        }

        val dataSet = PieDataSet(entries, "Moods (Last 7 Days)").apply {
            colors = listOf(
                Color.YELLOW, Color.BLUE, Color.GRAY,
                Color.GREEN, Color.MAGENTA, Color.RED, Color.CYAN
            )
            valueTextColor = Color.BLACK
            valueTextSize = 12f
        }

        pieChart.data = PieData(dataSet)
        pieChart.description.isEnabled = false
        pieChart.setUsePercentValues(true)
        pieChart.invalidate()
    }

    // 🔹 Bar chart: moods per day (last 7 days)
    private fun setupBarChart(barChart: BarChart) {
        val sdfInput = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val sdfOutput = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val lastWeekMoods = getLast7DaysMoods()

        val dateCounts = lastWeekMoods.groupingBy {
            try {
                val parsed = sdfInput.parse(it.timestamp)
                if (parsed != null) sdfOutput.format(parsed) else "Unknown"
            } catch (e: Exception) {
                "Unknown"
            }
        }.eachCount()

        if (dateCounts.isEmpty()) {
            barChart.clear()
            barChart.setNoDataText("No data")
            return
        }

        val entries = dateCounts.entries.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.value.toFloat())
        }

        val dataSet = BarDataSet(entries, "Mood Counts (Last 7 Days)").apply {
            colors = listOf(Color.GREEN, Color.RED, Color.MAGENTA, Color.CYAN, Color.YELLOW, Color.BLUE)
            valueTextColor = Color.BLACK
            valueTextSize = 12f
        }

        barChart.data = BarData(dataSet)
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(dateCounts.keys.toList())
        barChart.xAxis.granularity = 1f
        barChart.xAxis.labelRotationAngle = -45f
        barChart.description.isEnabled = false
        barChart.invalidate()
    }

    // 🔹 Line chart: Habit Completion % per day (last 7 days)
    private fun setupHabitCompletionChart(lineChart: LineChart) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -6)

        val dates = mutableListOf<String>()
        val entries = mutableListOf<Entry>()

        for (i in 0..6) {
            val date = sdf.format(calendar.time)
            dates.add(date)

            val planned = PrefsManager.getHabitsForDate(requireContext(), date)
            val completed = planned.count { PrefsManager.getHabitStatus(requireContext(), date, it.name) == "done" }

            val percent = if (planned.isNotEmpty()) (completed * 100f) / planned.size else 0f
            entries.add(Entry(i.toFloat(), percent))

            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val dataSet = LineDataSet(entries, "Habit Completion % (Last 7 Days)").apply {
            color = Color.BLUE
            circleColors = listOf(Color.RED)
            valueTextColor = Color.BLACK
            valueTextSize = 12f
            lineWidth = 2f
        }

        lineChart.data = LineData(dataSet)
        lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(dates)
        lineChart.xAxis.granularity = 1f
        lineChart.xAxis.labelRotationAngle = -45f
        lineChart.axisLeft.axisMinimum = 0f
        lineChart.axisLeft.axisMaximum = 100f
        lineChart.axisRight.isEnabled = false
        lineChart.description.isEnabled = false
        lineChart.invalidate()
    }

    //  Bar chart: Water Intake (Last 7 Days)
    private fun setupWaterIntakeChart(barChart: BarChart) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -6)

        val dates = mutableListOf<String>()
        val entries = mutableListOf<BarEntry>()

        for (i in 0..6) {
            val date = sdf.format(calendar.time)
            val intake = PrefsManager.getWaterIntake(requireContext(), date)
            val goal = PrefsManager.getWaterGoal(requireContext())
            val percent = if (goal > 0) (intake * 100f) / goal else 0f
            entries.add(BarEntry(i.toFloat(), percent))
            dates.add(date)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val dataSet = BarDataSet(entries, "Water Intake % (Last 7 Days)").apply {
            colors = listOf(Color.CYAN)
            valueTextColor = Color.BLACK
            valueTextSize = 12f
        }

        barChart.data = BarData(dataSet)
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(dates)
        barChart.xAxis.granularity = 1f
        barChart.xAxis.labelRotationAngle = -45f
        barChart.axisLeft.axisMinimum = 0f
        barChart.axisLeft.axisMaximum = 100f
        barChart.axisRight.isEnabled = false
        barChart.description.isEnabled = false
        barChart.invalidate()
    }

    //  Bar chart: Steps Progress (Last 7 Days)
    private fun setupStepsChart(barChart: BarChart) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -6)

        val dates = mutableListOf<String>()
        val entries = mutableListOf<BarEntry>()
        val stepGoal = 2000

        for (i in 0..6) {
            val date = sdf.format(calendar.time)
            val steps = PrefsManager.getStepsForDate(requireContext(), date)
            val percent = if (stepGoal > 0) (steps * 100f) / stepGoal else 0f
            entries.add(BarEntry(i.toFloat(), percent))
            dates.add(date)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val dataSet = BarDataSet(entries, "Steps % (Last 7 Days)").apply {
            colors = listOf(Color.parseColor("#1E88E5"))
            valueTextColor = Color.BLACK
            valueTextSize = 12f
        }

        barChart.data = BarData(dataSet)
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(dates)
        barChart.xAxis.granularity = 1f
        barChart.xAxis.labelRotationAngle = -45f
        barChart.axisLeft.axisMinimum = 0f
        barChart.axisLeft.axisMaximum = 100f
        barChart.axisRight.isEnabled = false
        barChart.description.isEnabled = false
        barChart.invalidate()
    }
}
