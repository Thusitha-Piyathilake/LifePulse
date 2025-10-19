package com.example.lifepulse.ui.main.fragments

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.lifepulse.R
import com.example.lifepulse.data.Habit
import com.example.lifepulse.data.PrefsManager

class ActivityAdapter(
    private val context: Context,
    private val habits: MutableList<Habit>,
    private val date: String
) : RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder>() {

    inner class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvHabitName: TextView = itemView.findViewById(R.id.tvHabitName)
        val tvHabitTime: TextView = itemView.findViewById(R.id.tvHabitTime)
        val tvHabitDuration: TextView = itemView.findViewById(R.id.tvHabitDuration)
        val tvStatus: TextView = itemView.findViewById(R.id.tvActivityStatus)
        val card: CardView = itemView.findViewById(R.id.cardActivity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val habit = habits[position]

        //  Show habit details
        holder.tvHabitName.text = habit.name
        holder.tvHabitTime.text = "Time: ${habit.time}"
        holder.tvHabitDuration.text = "Duration: ${habit.duration} mins"

        //  Load saved status ("pending", "done", "skipped")
        val status = PrefsManager.getHabitStatus(context, date, habit.name)

        when (status) {
            "done" -> {
                holder.card.setCardBackgroundColor(context.getColor(R.color.teal_200))
                holder.tvStatus.text = "✅ Done"
            }
            "skipped" -> {
                holder.card.setCardBackgroundColor(context.getColor(android.R.color.darker_gray))
                holder.tvStatus.text = "❌ Skipped"
            }
            else -> {
                holder.card.setCardBackgroundColor(context.getColor(R.color.white))
                holder.tvStatus.text = "Status: Pending"
            }
        }
    }

    override fun getItemCount(): Int = habits.size

    //  Mark habit as Done (persist + update UI)
    fun markDone(position: Int) {
        val habit = habits[position]
        PrefsManager.saveHabitStatus(context, date, habit.name, "done")
        notifyItemChanged(position)
    }

    //  Mark habit as Skipped (persist + update UI)
    fun markSkipped(position: Int) {
        val habit = habits[position]
        PrefsManager.saveHabitStatus(context, date, habit.name, "skipped")
        notifyItemChanged(position)
    }
}
