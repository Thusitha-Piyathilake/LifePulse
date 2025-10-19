package com.example.lifepulse.ui.habits

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lifepulse.R
import com.example.lifepulse.data.PrefsManager
import com.example.lifepulse.ui.main.MainActivity

class CustomHabitActivity : AppCompatActivity() {

    private lateinit var etHabitName: EditText
    private lateinit var ivIcon: ImageView
    private lateinit var btnCreate: Button
    private var selectedIconRes: Int = R.drawable.ic_launcher_foreground // default icon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_habit)

        etHabitName = findViewById(R.id.etHabitName)
        ivIcon = findViewById(R.id.ivHabitIcon)
        btnCreate = findViewById(R.id.btnCreateHabit)

        //  Open icon picker dialog
        ivIcon.setOnClickListener {
            showIconPickerDialog()
        }

        btnCreate.setOnClickListener {
            val habitName = etHabitName.text.toString().trim()
            if (habitName.isEmpty()) {
                Toast.makeText(this, "Please enter a habit name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save habit with chosen icon
            PrefsManager.saveCustomHabit(this, habitName, selectedIconRes)

            // Navigate directly to MainActivity with ChooseHabitsFragment
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("navigateTo", "ChooseHabitsFragment")
                putExtra("habitName", habitName)
                putExtra("habitIcon", selectedIconRes)
            }
            startActivity(intent)
            finish()
        }
    }

    private fun showIconPickerDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_icon_picker, null)
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Choose Icon")
            .create()

        val iconMap = mapOf(
            R.id.iconMoon to R.drawable.moon,
            R.id.iconSun to R.drawable.sun,
            R.id.iconTalk to R.drawable.talk,
            R.id.iconCycling to R.drawable.cycling,
            R.id.iconWrite to R.drawable.write,
            R.id.iconDance to R.drawable.dance,
            R.id.iconTree to R.drawable.tree,
            R.id.iconClean to R.drawable.clean
        )

        iconMap.forEach { (viewId, drawableRes) ->
            dialogView.findViewById<ImageView>(viewId).setOnClickListener {
                selectedIconRes = drawableRes
                ivIcon.setImageResource(drawableRes) // update preview
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}
