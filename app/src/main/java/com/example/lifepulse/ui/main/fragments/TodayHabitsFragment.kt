package com.example.lifepulse.ui.main.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lifepulse.R
import com.example.lifepulse.data.Habit
import com.example.lifepulse.data.PrefsManager
import com.example.lifepulse.notification.NotificationHelper   //  import NotificationHelper

class TodayHabitsFragment : Fragment() {

    private lateinit var adapter: TodayHabitsAdapter
    private val habits = mutableListOf<Habit>()
    private var selectedDate: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_today_habits, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerTodayHabits)
        val btnSave = view.findViewById<Button>(R.id.btnSaveHabits)

        habits.clear()
        arguments?.getParcelableArrayList<Habit>("selectedHabits")?.let {
            habits.addAll(it)
        }
        selectedDate = arguments?.getString("selectedDate")

        //  Adapter handles edit/delete logic
        adapter = TodayHabitsAdapter(
            habits,
            onEdit = { habit ->
                val dialog = HabitTimeDialog(habit.name, habit.time, habit.duration) { updatedHabit, time, duration ->
                    val index = habits.indexOf(habit)
                    if (index != -1) {
                        habits[index] = Habit(updatedHabit, time, duration)
                        adapter.notifyItemChanged(index)
                    }
                }
                dialog.show(parentFragmentManager, "HabitTimeDialog")
            },
            onDelete = { habit ->
                val index = habits.indexOf(habit)
                if (index != -1) {
                    habits.removeAt(index)
                    adapter.notifyItemRemoved(index)
                    Toast.makeText(requireContext(), "${habit.name} removed", Toast.LENGTH_SHORT).show()
                }
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        btnSave.setOnClickListener {
            selectedDate?.let { date ->
                //  Save habits for that date
                PrefsManager.saveHabitsForDate(requireContext(), date, habits)

                habits.forEach { habit ->
                    // Save log
                    PrefsManager.saveHabitLog(requireContext(), habit.name, habit.time, habit.duration, date)

                    //  Schedule notification reminder
                    NotificationHelper.scheduleHabitReminder(requireContext(), habit.name, habit.time, date)
                }

                Toast.makeText(requireContext(), "Habits saved for $date!", Toast.LENGTH_SHORT).show()
            }

            // Navigate to Dashboard
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, DashboardFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }
}
