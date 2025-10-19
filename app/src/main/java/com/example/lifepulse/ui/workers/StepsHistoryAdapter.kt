package com.example.lifepulse.ui.workers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lifepulse.R

class StepsHistoryAdapter(
    private val stepsList: List<StepEntry>
) : RecyclerView.Adapter<StepsHistoryAdapter.StepViewHolder>() {

    inner class StepViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvDateSteps)
        val tvSteps: TextView = itemView.findViewById(R.id.tvStepsCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_step_entry, parent, false)
        return StepViewHolder(view)
    }

    override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
        val entry = stepsList[position]
        holder.tvDate.text = entry.date
        holder.tvSteps.text = "${entry.steps} steps"
    }

    override fun getItemCount(): Int = stepsList.size
}
