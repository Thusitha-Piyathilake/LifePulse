package com.example.lifepulse.ui.main.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lifepulse.R
import java.text.SimpleDateFormat
import java.util.*

class DateAdapter(
    private val dates: List<Calendar>,
    private val onDateSelected: (Calendar) -> Unit
) : RecyclerView.Adapter<DateAdapter.DateViewHolder>() {

    private var selectedPosition = -1

    inner class DateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDayOfWeek: TextView = view.findViewById(R.id.tvDayOfWeek)
        val tvDayNumber: TextView = view.findViewById(R.id.tvDayNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_date, parent, false)
        return DateViewHolder(view)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        val calendar = dates[position]

        val dayOfWeek = SimpleDateFormat("EEE", Locale.getDefault()).format(calendar.time)
        val dayNumber = SimpleDateFormat("d", Locale.getDefault()).format(calendar.time)

        holder.tvDayOfWeek.text = dayOfWeek
        holder.tvDayNumber.text = dayNumber

        if (position == selectedPosition) {
            holder.tvDayNumber.setBackgroundResource(R.drawable.bg_date_selected)
            holder.tvDayNumber.setTextColor(holder.itemView.context.getColor(android.R.color.white))
        } else {
            holder.tvDayNumber.setBackgroundResource(R.drawable.bg_date_unselected)
            holder.tvDayNumber.setTextColor(holder.itemView.context.getColor(android.R.color.black))
        }

        holder.itemView.setOnClickListener {
            val previous = selectedPosition
            selectedPosition = position
            notifyItemChanged(previous)
            notifyItemChanged(selectedPosition)
            onDateSelected(calendar)
        }
    }

    override fun getItemCount(): Int = dates.size

    fun setTodayAsSelected() {
        val today = Calendar.getInstance()
        selectedPosition = dates.indexOfFirst {
            it.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH) &&
                    it.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                    it.get(Calendar.YEAR) == today.get(Calendar.YEAR)
        }
    }
}
