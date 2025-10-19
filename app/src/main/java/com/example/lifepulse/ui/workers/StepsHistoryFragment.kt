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

data class StepEntry(val date: String, val steps: Int)

class StepsHistoryFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var rangeGroup: RadioGroup
    private lateinit var rbToday: RadioButton
    private lateinit var rbLast7: RadioButton

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_steps_history, container, false)

        recycler = v.findViewById(R.id.recyclerSteps)
        tvEmpty = v.findViewById(R.id.tvEmptySteps)
        rangeGroup = v.findViewById(R.id.rangeGroupSteps)
        rbToday = v.findViewById(R.id.rbTodaySteps)
        rbLast7 = v.findViewById(R.id.rbLast7Steps)

        recycler.layoutManager = LinearLayoutManager(requireContext())
        rangeGroup.setOnCheckedChangeListener { _, _ -> loadData() }

        loadData()
        return v
    }

    // ✅ Load from PrefsManager step history
    private fun loadData() {
        val allHistory = PrefsManager.getStepsHistory(requireContext())
        val items = mutableListOf<StepEntry>()

        if (rbToday.isChecked) {
            val today = dateFmt.format(Date())
            allHistory.filter { it.first == today }.forEach {
                items.add(StepEntry(it.first, it.second))
            }
        } else {
            val cal = Calendar.getInstance()
            repeat(7) {
                val d = dateFmt.format(cal.time)
                allHistory.filter { it.first == d }.forEach {
                    items.add(StepEntry(it.first, it.second))
                }
                cal.add(Calendar.DAY_OF_YEAR, -1)
            }
        }

        tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        recycler.adapter = StepsHistoryAdapter(items)
    }
}
