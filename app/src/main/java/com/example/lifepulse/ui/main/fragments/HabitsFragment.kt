package com.example.lifepulse.ui.main.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lifepulse.R
import com.example.lifepulse.data.Habit
import com.example.lifepulse.notification.NotificationHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class HabitsFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HabitsAdapter
    private val habits = mutableListOf<Habit>()   //  Store Habit objects

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_habits, container, false)

        recyclerView = view.findViewById(R.id.recyclerHabits)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = HabitsAdapter(habits)
        recyclerView.adapter = adapter

        val etHabit = view.findViewById<EditText>(R.id.etHabit)
        val btnAdd = view.findViewById<Button>(R.id.btnAddHabit)

        loadHabits()



        btnAdd.setOnClickListener {
            val habitName = etHabit.text.toString()
            if (habitName.isNotEmpty()) {
                //  Default values (later replace with TimePicker/Duration input)
                val newHabit = Habit(
                    name = habitName,
                    time = "08:00",   // Default reminder time (24hr format)
                    duration = 30
                )
                habits.add(newHabit)
                saveHabits()

                //  Schedule reminder for this habit (today’s date for now)
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                scheduleReminderForHabit(newHabit, todayDate)

                adapter.notifyDataSetChanged()
                etHabit.text.clear()
            }
        }

        return view
    }

    private fun saveHabits() {
        val prefs = requireActivity().getSharedPreferences("LifePulsePrefs", Context.MODE_PRIVATE)
        val json = Gson().toJson(habits)
        prefs.edit().putString("habits", json).apply()
    }

    private fun loadHabits() {
        val prefs = requireActivity().getSharedPreferences("LifePulsePrefs", Context.MODE_PRIVATE)
        val json = prefs.getString("habits", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<Habit>>() {}.type
            habits.clear()
            habits.addAll(Gson().fromJson(json, type))
        }
    }

    //  Cancel and delete habit
    fun deleteHabit(habitName: String) {
        habits.removeAll { it.name == habitName }
        saveHabits()

        //  Cancel reminder when habit is deleted (today’s date assumed)
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        NotificationHelper.cancelHabitReminder(requireContext(), habitName, todayDate)

        adapter.notifyDataSetChanged()
    }

    //  Helper to schedule reminders dynamically using NotificationHelper
    private fun scheduleReminderForHabit(habit: Habit, date: String) {
        try {
            NotificationHelper.scheduleHabitReminder(
                context = requireContext(),
                habitName = habit.name,
                time = habit.time,   // "HH:mm"
                date = date,         // "yyyy-MM-dd"
                duration = habit.duration
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
