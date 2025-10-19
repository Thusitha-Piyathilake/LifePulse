package com.example.lifepulse.ui.workers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lifepulse.R

class HydrationHistoryAdapter(
    private val items: List<HydrationEntry>
) : RecyclerView.Adapter<HydrationHistoryAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount) // ✅ new
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hydration_entry, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvDate.text = item.date
        holder.tvTime.text = item.time
        holder.tvAmount.text = item.amount // ✅ show ml
    }

    override fun getItemCount(): Int = items.size
}
