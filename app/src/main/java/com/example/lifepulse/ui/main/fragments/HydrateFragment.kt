package com.example.lifepulse.ui.main.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.lifepulse.R
import com.example.lifepulse.data.PrefsManager
import com.github.lzyzsd.circleprogress.DonutProgress
import java.text.SimpleDateFormat
import java.util.*

class HydrateFragment : Fragment(R.layout.fragment_hydrate) {

    private lateinit var progressHydrate: DonutProgress
    private lateinit var tvHydrateAmount: TextView

    private var intake = 0
    private var goal = 3700  // 3.7L default daily goal
    private val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressHydrate = view.findViewById(R.id.progressHydrate)
        tvHydrateAmount = view.findViewById(R.id.tvHydrateAmount)

        // Load saved intake & goal for today
        intake = PrefsManager.getWaterIntake(requireContext(), todayDate)
        goal = PrefsManager.getWaterGoal(requireContext())

        updateUI()

        view.findViewById<Button>(R.id.btn250ml).setOnClickListener { addWater(250) }
        view.findViewById<Button>(R.id.btn500ml).setOnClickListener { addWater(500) }
        view.findViewById<Button>(R.id.btn1000ml).setOnClickListener { addWater(1000) }
        view.findViewById<Button>(R.id.btnOther).setOnClickListener { showCustomInput() }
    }

    private fun addWater(amount: Int) {
        intake += amount
        PrefsManager.saveWaterIntake(requireContext(), todayDate, intake)  //  per-date saving

        //  Save log entry for HydrationHistory
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        PrefsManager.saveMood(requireContext(), todayDate, "💧 Water Drank $amount ml", time)

        updateUI()
    }

    private fun showCustomInput() {
        val input = EditText(requireContext())
        input.hint = "Enter ml"

        AlertDialog.Builder(requireContext())
            .setTitle("Add Custom Amount")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val value = input.text.toString().toIntOrNull()
                if (value != null && value > 0) {
                    addWater(value)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateUI() {
        val percent = ((intake.toFloat() / goal) * 100).toInt().coerceAtMost(100)
        progressHydrate.progress = percent.toFloat()
        tvHydrateAmount.text = "$intake ml ($percent%)"
    }
}
