package com.example.lifepulse.ui.main.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lifepulse.R
import com.example.lifepulse.data.Habit

class TodayHabitsAdapter(
    private val habits: MutableList<Habit>,
    private val onEdit: (Habit) -> Unit,
    private val onDelete: (Habit) -> Unit
) : RecyclerView.Adapter<TodayHabitsAdapter.HabitViewHolder>() {

    class HabitViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHabitName: TextView = view.findViewById(R.id.tvHabitName)
        val tvHabitTime: TextView = view.findViewById(R.id.tvHabitTime)
        val btnEdit: Button = view.findViewById(R.id.btnEditHabit)
        val btnDelete: Button = view.findViewById(R.id.btnDeleteHabit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_today_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]
        holder.tvHabitName.text = habit.name
        holder.tvHabitTime.text = "At ${habit.time} for ${habit.duration} mins"

        holder.btnEdit.setOnClickListener { onEdit(habit) }

        holder.btnDelete.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                val deletedHabit = habits[currentPosition]

                //  Remove from list
                habits.removeAt(currentPosition)
                notifyItemRemoved(currentPosition)

                //  Callback to remove from PrefsManager
                onDelete(deletedHabit)
            }
        }
    }

    override fun getItemCount(): Int = habits.size
}
