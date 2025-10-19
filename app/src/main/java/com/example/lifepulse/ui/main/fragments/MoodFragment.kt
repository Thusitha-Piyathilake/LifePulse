package com.example.lifepulse.ui.main.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lifepulse.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class MoodFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MoodAdapter
    private val moods = mutableListOf<MoodEntry>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mood, container, false)

        recyclerView = view.findViewById(R.id.recyclerMoods)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        //  Adapter with delete callback
        adapter = MoodAdapter(moods) { position ->
            if (position in moods.indices) {
                moods.removeAt(position)
                saveMoods()
                adapter.notifyItemRemoved(position)
            }
        }
        recyclerView.adapter = adapter

        //  Mood buttons → Save immediately
        val buttons = mapOf(
            R.id.btnHappy to "😃 Happy",
            R.id.btnEnergized to "⚡ Energized",
            R.id.btnFrustrated to "😠 Frustrated",
            R.id.btnOverwhelmed to "😵 Overwhelmed",
            R.id.btnPeaceful to "😌 Peaceful",
            R.id.btnMotivated to "💪 Motivated"
        )

        buttons.forEach { (id, moodText) ->
            view.findViewById<Button>(id).setOnClickListener {
                val timestamp =
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                val entry = MoodEntry(moodText, timestamp)

                moods.add(0, entry) // newest first
                saveMoods()
                adapter.notifyItemInserted(0)
                recyclerView.scrollToPosition(0)
            }
        }

        //  Load saved moods
        loadMoods()

        return view
    }

    private fun saveMoods() {
        val prefs = requireActivity().getSharedPreferences("LifePulsePrefs", Context.MODE_PRIVATE)
        val json = Gson().toJson(moods)
        prefs.edit().putString("moods", json).apply()
    }

    private fun loadMoods() {
        val prefs = requireActivity().getSharedPreferences("LifePulsePrefs", Context.MODE_PRIVATE)
        val json = prefs.getString("moods", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<MoodEntry>>() {}.type
            moods.clear()
            moods.addAll(Gson().fromJson(json, type))
            adapter.notifyDataSetChanged()
        }
    }

    //  Public method so MainActivity can refresh list after shake
    fun refreshMoods() {
        loadMoods()
    }
}
