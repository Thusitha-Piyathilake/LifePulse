package com.example.lifepulse.ui.main.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lifepulse.R

class LogAdapter(
    private val logs: MutableList<String>,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text: TextView = itemView.findViewById(R.id.logText)
        val btnDelete: Button = itemView.findViewById(R.id.btnDeleteLog)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logs[position]
        holder.text.text = log

        holder.btnDelete.setOnClickListener {
            onDelete(log)   // callback to fragment
        }
    }

    override fun getItemCount(): Int = logs.size

    fun setData(newLogs: List<String>) {
        logs.clear()
        logs.addAll(newLogs)
        notifyDataSetChanged()
    }
}
