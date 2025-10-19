package com.example.lifepulse.ui.main.fragments

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import com.example.lifepulse.R
import java.util.*

class HabitTimeDialog(
    private val habitName: String,
    private val currentTime: String? = null,       //  pass existing time
    private val currentDuration: Int? = null,      //  pass existing duration
    private val onSave: (habit: String, time: String, duration: Int) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_set_time)

        val timePicker = dialog.findViewById<TimePicker>(R.id.timePicker)
        val etDuration = dialog.findViewById<EditText>(R.id.etDuration)
        val btnSave = dialog.findViewById<Button>(R.id.btnSave)

        timePicker.setIs24HourView(false) //  show 12h or 24h clock based on device

        //  Pre-fill if editing
        currentTime?.let {
            val parts = it.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toIntOrNull() ?: 0
                val minute = parts[1].toIntOrNull() ?: 0
                timePicker.hour = hour
                timePicker.minute = minute
            }
        }
        etDuration.setText(currentDuration?.toString() ?: "")

        btnSave.setOnClickListener {
            val hour = timePicker.hour
            val minute = timePicker.minute
            val duration = etDuration.text.toString().toIntOrNull() ?: 0

            //  Format time properly (HH:mm)
            val time = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

            //  Send back habit + time + duration
            onSave(habitName, time, duration)

            dismiss()
        }

        return dialog
    }
}
