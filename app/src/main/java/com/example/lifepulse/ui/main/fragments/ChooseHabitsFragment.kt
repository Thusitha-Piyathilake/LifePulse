package com.example.lifepulse.ui.main.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lifepulse.R
import com.example.lifepulse.data.Habit
import com.example.lifepulse.data.PrefsManager
import com.example.lifepulse.ui.habits.AddHabitActivity
import java.text.SimpleDateFormat
import java.util.*

class ChooseHabitsFragment : Fragment() {

    private val selectedHabits = mutableListOf<Habit>()
    private var selectedDate: String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private lateinit var habitsContainer: GridLayout

    private val builtInHabits = listOf(
        "Workout", "Eat Food", "Music", "Art & Design",
        "Traveling", "Read Book", "Gaming", "Mechanic"
    )

    private val addHabitLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult

            val habitName = data.getStringExtra("habitName")
            val habitIcon = data.getIntExtra("habitIcon", R.drawable.ic_launcher_foreground)
            if (!habitName.isNullOrEmpty()) {
                if (!isHabitAlreadyAdded(habitName)) {
                    addHabitView(habitName, habitIcon, persist = true)
                } else {
                    Toast.makeText(requireContext(), "$habitName already exists!", Toast.LENGTH_SHORT).show()
                }
            }

            val habitsList = data.getStringArrayListExtra("habits")
            habitsList?.forEach { habit ->
                if (!isHabitAlreadyAdded(habit)) {
                    addHabitView(habit, R.drawable.ic_launcher_foreground, persist = true)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_choose_habits, container, false)

        val prefs = requireContext().getSharedPreferences("LifePulsePrefs", Context.MODE_PRIVATE)
        val userName = prefs.getString("userName", "User")
        val tvGreeting = view.findViewById<TextView>(R.id.tvGreeting)
        tvGreeting.text = getString(R.string.greeting, userName)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerDates)
        val tvMonthYear = view.findViewById<TextView>(R.id.tvMonthYear)

        recyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val calendar = Calendar.getInstance()
        val days = mutableListOf<Calendar>()
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in calendar.get(Calendar.DAY_OF_MONTH)..maxDay) {
            val day = Calendar.getInstance()
            day.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), i)
            days.add(day)
        }

        tvMonthYear.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)

        val adapter = DateAdapter(days) { date ->
            tvMonthYear.text =
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date.time)
            selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date.time)
        }
        adapter.setTodayAsSelected()
        recyclerView.adapter = adapter

        val menuIcon = view.findViewById<ImageView>(R.id.ivMenu)
        menuIcon.setOnClickListener {
            val drawerLayout = requireActivity().findViewById<DrawerLayout>(R.id.drawerLayout)
            drawerLayout.openDrawer(Gravity.START)
        }

        //  Notification bell
        val ivBell = view.findViewById<ImageView>(R.id.btnNotifications)
        ivBell?.setOnClickListener {
            showNotificationsPopup()
        }

        val habits = mapOf(
            R.id.btnWorkout to "Workout",
            R.id.btnEatFood to "Eat Food",
            R.id.btnMusic to "Music",
            R.id.btnArt to "Art & Design",
            R.id.btnTravel to "Traveling",
            R.id.btnReadBook to "Read Book",
            R.id.btnGaming to "Gaming",
            R.id.btnMechanic to "Mechanic"
        )

        habits.forEach { (id, name) ->
            val habitLayout = view.findViewById<LinearLayout>(id)

            habitLayout.setOnClickListener {
                if (habitLayout.isSelected) {
                    habitLayout.isSelected = false
                    selectedHabits.removeAll { it.name == name }
                    Toast.makeText(requireContext(), "$name removed", Toast.LENGTH_SHORT).show()
                } else {
                    val dialog = HabitTimeDialog(name, "", 0) { habit, time, duration ->
                        selectedHabits.add(Habit(habit, time, duration))
                        habitLayout.isSelected = true
                        Toast.makeText(
                            requireContext(),
                            "$habit selected at $time for $duration mins",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    dialog.show(parentFragmentManager, "HabitTimeDialog")
                }
            }
        }

        habitsContainer = view.findViewById(R.id.habitsContainer)

        PrefsManager.getCustomHabits(requireContext()).forEach { (habitName, iconRes) ->
            addHabitView(habitName, iconRes, persist = false)
        }

        view.findViewById<View>(R.id.btnAddHabit).setOnClickListener {
            val intent = Intent(requireContext(), AddHabitActivity::class.java)
            addHabitLauncher.launch(intent)
        }

        view.findViewById<View>(R.id.btnGetStarted).setOnClickListener {
            val bundle = Bundle().apply {
                putParcelableArrayList("selectedHabits", ArrayList(selectedHabits))
                putString("selectedDate", selectedDate)
            }

            val fragment = TodayHabitsFragment().apply { arguments = bundle }

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    //  Notification popup with Clear All support
    private fun showNotificationsPopup() {
        val notifications = PrefsManager.getNotifications(requireContext())

        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_notifications, null)
        val container = dialogView.findViewById<LinearLayout>(R.id.notificationsContainer)

        if (notifications.isEmpty()) {
            val emptyView = TextView(requireContext()).apply {
                text = "No notifications yet"
                textSize = 16f
                setPadding(8, 8, 8, 8)
            }
            container.addView(emptyView)
        } else {
            notifications.forEach { msg ->
                val tv = TextView(requireContext()).apply {
                    text = msg
                    textSize = 16f
                    setPadding(8, 8, 8, 8)
                }
                container.addView(tv)
            }
        }

        //  Clear All button
        val clearBtn: TextView? = dialogView.findViewById(R.id.btnClearAll)
        clearBtn?.setOnClickListener {
            PrefsManager.clearNotifications(requireContext())
            container.removeAllViews()
            val emptyView = TextView(requireContext()).apply {
                text = "All notifications cleared"
                textSize = 16f
                setPadding(8, 8, 8, 8)
            }
            container.addView(emptyView)
            Toast.makeText(requireContext(), "All notifications cleared", Toast.LENGTH_SHORT).show()
        }

        val closeBtn: TextView = dialogView.findViewById(R.id.btnClose)
        val dialog = builder.setView(dialogView).create()
        closeBtn.setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun addHabitView(habit: String, iconRes: Int, persist: Boolean) {
        val habitView = layoutInflater.inflate(R.layout.item_habit_card, habitsContainer, false)

        val tvHabitName = habitView.findViewById<TextView>(R.id.tvHabitName)
        val ivHabitIcon = habitView.findViewById<ImageView>(R.id.ivHabitIcon)

        val iconMap = mapOf(
            "Wake up" to R.drawable.wake_up,
            "Self-care" to R.drawable.self_care,
            "Nature walk" to R.drawable.nature_walk,
            "Eat Fruits and Veggies" to R.drawable.eat_fruits_and_veggies,
            "Work/study" to R.drawable.work_study,
            "Reflect" to R.drawable.reflect
        )

        tvHabitName.text = habit
        ivHabitIcon.setImageResource(iconMap[habit] ?: iconRes)

        if (persist) {
            PrefsManager.saveCustomHabit(requireContext(), habit, iconRes)
        }

        habitView.setOnClickListener {
            if (habitView.isSelected) {
                habitView.isSelected = false
                selectedHabits.removeAll { it.name == habit }
                Toast.makeText(requireContext(), "$habit removed", Toast.LENGTH_SHORT).show()
            } else {
                val dialog = HabitTimeDialog(habit, "", 0) { habitName, time, duration ->
                    selectedHabits.add(Habit(habitName, time, duration))
                    habitView.isSelected = true
                    Toast.makeText(
                        requireContext(),
                        "$habitName selected at $time for $duration mins",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                dialog.show(parentFragmentManager, "HabitTimeDialog")
            }
        }

        habitView.setOnLongClickListener {
            if (builtInHabits.contains(habit)) {
                Toast.makeText(requireContext(), "$habit is a default habit and cannot be removed", Toast.LENGTH_SHORT).show()
                false
            } else {
                PrefsManager.removeCustomHabit(requireContext(), habit)
                habitsContainer.removeView(habitView)
                Toast.makeText(requireContext(), "$habit removed", Toast.LENGTH_SHORT).show()
                true
            }
        }

        val params = GridLayout.LayoutParams().apply {
            width = 0
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(8, 8, 8, 8)
        }
        habitView.layoutParams = params

        habitsContainer.addView(habitView)
    }

    private fun isHabitAlreadyAdded(habitName: String): Boolean {
        for (i in 0 until habitsContainer.childCount) {
            val child = habitsContainer.getChildAt(i)
            val tvHabitName = child.findViewById<TextView>(R.id.tvHabitName)
            if (tvHabitName.text.toString() == habitName) return true
        }
        return false
    }
}
