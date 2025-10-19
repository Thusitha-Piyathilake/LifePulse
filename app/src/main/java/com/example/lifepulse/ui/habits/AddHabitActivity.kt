package com.example.lifepulse.ui.habits

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.lifepulse.R

class AddHabitActivity : AppCompatActivity() {

    private val selectedHabits = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_habit)

        //  Mapping habit cards to names (IDs must exactly match XML)
        val habitViews: Map<Int, String> = mapOf(
            R.id.btnWakeUp to "Wake up",
            R.id.btnSelfCare to "Self-care",
            R.id.btnNatureWalk to "Nature walk",
            R.id.btnEatFruitsVeggies to "Eat Fruits and Veggies",
            R.id.btnWorkStudy to "Work/study",
            R.id.btnReflect to "Reflect"
        )

        //  Set click listeners
        habitViews.forEach { (id, habitName) ->
            val layout = findViewById<LinearLayout>(id)
            layout.setOnClickListener {
                if (layout.isSelected) {
                    // Unselect
                    layout.isSelected = false
                    selectedHabits.remove(habitName)
                } else {
                    // Select
                    layout.isSelected = true
                    selectedHabits.add(habitName)
                }
            }
        }

        findViewById<Button>(R.id.btnCustomHabit).setOnClickListener {
            val intent = Intent(this, CustomHabitActivity::class.java)
            startActivity(intent)
        }

        // 🔹 Save Selection button
        val btnSave = findViewById<Button>(R.id.btnSaveSelection) //
        btnSave.setOnClickListener {
            val resultIntent = Intent()
            resultIntent.putStringArrayListExtra("habits", ArrayList(selectedHabits))
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }


    }
}
