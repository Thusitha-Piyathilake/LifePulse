package com.example.lifepulse.ui.main.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lifepulse.R
import com.example.lifepulse.data.Habit

class HabitsAdapter(private val habits: List<Habit>) :
    RecyclerView.Adapter<HabitsAdapter.HabitViewHolder>() {

    class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvHabit: TextView = itemView.findViewById(R.id.tvHabit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]
        // Show habit name + duration
        holder.tvHabit.text = "${habit.name} (${habit.duration} mins)"
    }

    override fun getItemCount(): Int = habits.size
}
