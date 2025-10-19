package com.example.lifepulse.ui.main.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lifepulse.R
import com.example.lifepulse.data.Habit

class TodayHabitAdapter(private val habits: List<Habit>) :
    RecyclerView.Adapter<TodayHabitAdapter.HabitViewHolder>() {

    class HabitViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHabitName: TextView = view.findViewById(R.id.tvHabitName)
        val tvHabitTime: TextView = view.findViewById(R.id.tvHabitTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_today_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]
        holder.tvHabitName.text = "${habit.name} (${habit.duration} mins)"
        holder.tvHabitTime.text = "At ${habit.time}"
    }

    override fun getItemCount(): Int = habits.size
}
