package com.example.lifepulse.ui.workers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lifepulse.R
import com.example.lifepulse.data.PrefsManager
import java.text.SimpleDateFormat
import java.util.*

class HydrationHistoryFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvTotalIntake: TextView
    private lateinit var rangeGroup: RadioGroup
    private lateinit var rbToday: RadioButton
    private lateinit var rbLast7: RadioButton

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_hydration_history, container, false)
        recycler = v.findViewById(R.id.recyclerHydration)
        tvEmpty = v.findViewById(R.id.tvEmpty)
        tvTotalIntake = v.findViewById(R.id.tvTotalIntake)
        rangeGroup = v.findViewById(R.id.rangeGroup)
        rbToday = v.findViewById(R.id.rbToday)
        rbLast7 = v.findViewById(R.id.rbLast7)

        recycler.layoutManager = LinearLayoutManager(requireContext())

        rangeGroup.setOnCheckedChangeListener { _, _ -> loadData() }

        loadData()
        return v
    }

    private fun loadData() {
        val items = mutableListOf<HydrationEntry>()
        var totalMl = 0

        if (rbToday.isChecked) {
            val today = dateFmt.format(Date())
            val dayLogs = getHydrationForDate(today)
            items += dayLogs
            totalMl = sumAmounts(dayLogs)
            tvTotalIntake.text = "Total today: ${totalMl} ml"
        } else {
            val cal = Calendar.getInstance()
            repeat(7) {
                val d = dateFmt.format(cal.time)
                val dayLogs = getHydrationForDate(d)
                items += dayLogs
                totalMl += sumAmounts(dayLogs)
                cal.add(Calendar.DAY_OF_YEAR, -1)
            }
            tvTotalIntake.text = "Total last 7 days: ${totalMl} ml"
        }

        items.sortWith(compareBy<HydrationEntry> { it.date }.thenBy { it.time })

        tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        recycler.adapter = HydrationHistoryAdapter(items)
    }

    // ✅ Helper to extract ml from logs
    private fun sumAmounts(logs: List<HydrationEntry>): Int {
        var total = 0
        for (entry in logs) {
            val ml = entry.amount.filter { it.isDigit() }.toIntOrNull() ?: 0
            total += ml
        }
        return total
    }

    private fun getHydrationForDate(date: String): List<HydrationEntry> {
        val raw = PrefsManager.getMoodByDate(requireContext(), date)
        val list = mutableListOf<HydrationEntry>()

        for (line in raw) {
            if (line.startsWith("💧")) {
                // Example: "💧 500 ml at 14:20"
                val time = line.substringAfter(" at ").trim()
                val amount = line.substringBefore(" at ").removePrefix("💧").trim()
                list += HydrationEntry(date, time, amount)
            }
        }
        return list
    }
}
