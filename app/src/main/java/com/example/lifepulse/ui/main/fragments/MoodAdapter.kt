package com.example.lifepulse.ui.main.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lifepulse.R

class MoodAdapter(
    private val moods: MutableList<MoodEntry>,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<MoodAdapter.MoodViewHolder>() {

    class MoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMood: TextView = itemView.findViewById(R.id.tvMood)
        val tvMoodText: TextView = itemView.findViewById(R.id.tvMoodText)
        val tvTime: TextView = itemView.findViewById(R.id.tvMoodTime)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteMood) //  ImageButton now
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoodViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mood, parent, false)
        return MoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: MoodViewHolder, position: Int) {
        val mood = moods[position]

        //  Split emoji + text safely
        val parts = mood.mood.split(" ", limit = 2)
        holder.tvMood.text = parts[0]                               // emoji
        holder.tvMoodText.text = if (parts.size > 1) parts[1] else "" // text
        holder.tvTime.text = mood.timestamp

        //  Handle delete
        holder.btnDelete.setOnClickListener {
            onDelete(position)
        }
    }

    override fun getItemCount(): Int = moods.size
}
