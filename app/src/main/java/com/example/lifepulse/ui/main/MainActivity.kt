package com.example.lifepulse.ui.main

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.lifepulse.R
import com.example.lifepulse.data.PrefsManager
import com.example.lifepulse.ui.auth.LoginActivity
import com.example.lifepulse.ui.main.fragments.*
import com.example.lifepulse.ui.main.fragments.RelaxFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var initialSteps = -1
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val todayDate: String = dateFormat.format(Date())

    //  For shake detection
    private var accelCurrent = 0f
    private var accelLast = 0f
    private var shakeThreshold = 12f
    private var lastShakeTime = 0L

    //  Drawer
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle

    //  Launcher for requesting POST_NOTIFICATIONS permission
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _: Boolean -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //  Drawer setup
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        //  Setup hamburger toggle
        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        //  Enable hamburger in ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment(), "ProfileFragment")
                }
                R.id.nav_hydrate -> {
                    loadFragment(HydrateFragment(), "HydrateFragment")
                }
                R.id.nav_relax -> {
                    loadFragment(RelaxFragment(), "RelaxFragment")
                }
                R.id.nav_steps -> {
                    loadFragment(StepsFragment(), "StepsFragment")
                }
                R.id.nav_logout -> {
                    AlertDialog.Builder(this)
                        .setTitle("Log Out")
                        .setMessage("Are you sure you want to log out?")
                        .setPositiveButton("Yes") { _, _ ->
                            val prefs = getSharedPreferences("LifePulsePrefs", Context.MODE_PRIVATE)
                            prefs.edit()
                                .putBoolean("isLoggedIn", false) //  only reset login state
                                .apply()

                            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()

                            //  Redirect back to LoginActivity
                            val intent = Intent(this, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
            drawerLayout.closeDrawers()
            true
        }

        //  Bottom nav
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_habits -> loadFragment(ChooseHabitsFragment())
                R.id.nav_mood -> loadFragment(MoodFragment())
                R.id.nav_analysis -> loadFragment(AnalysisFragment())
                R.id.nav_calendar -> loadFragment(DashboardFragment(), "DashboardFragment")
                R.id.nav_settings -> loadFragment(SettingsFragment())
            }
            true
        }

        //  Handle direct navigation from CustomHabitActivity
        if (intent.getStringExtra("navigateTo") == "ChooseHabitsFragment") {
            val fragment = ChooseHabitsFragment().apply {
                arguments = Bundle().apply {
                    putString("habitName", intent.getStringExtra("habitName"))
                    putInt(
                        "habitIcon",
                        intent.getIntExtra("habitIcon", R.drawable.ic_launcher_foreground)
                    )
                }
            }
            loadFragment(fragment)
        } else {
            // Normal startup
            if (savedInstanceState == null) {
                loadFragment(ChooseHabitsFragment())
            }
        }

        //  Setup sensors
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }

        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelSensor != null) {
            sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_UI)
            accelCurrent = SensorManager.GRAVITY_EARTH
            accelLast = SensorManager.GRAVITY_EARTH
        }

        //  Ask for notification permission
        askNotificationPermission()

        //  Notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "habit_channel",
                "Habit Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for habit reminder notifications"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun loadFragment(fragment: Fragment, tag: String? = null) {
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment, tag)
        transaction.commit()
    }

    //  Sensors
    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val totalSteps = event.values[0].toInt()
                if (initialSteps == -1) {
                    initialSteps = totalSteps
                }
                val todaySteps = totalSteps - initialSteps
                PrefsManager.saveSteps(this, todayDate, todaySteps)
            }

            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                accelLast = accelCurrent
                accelCurrent = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                val delta = accelCurrent - accelLast

                if (delta > shakeThreshold) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastShakeTime > 1500) {
                        lastShakeTime = currentTime
                        showMoodDialog()
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    //  Notifications
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    //  Dashboard refresh
    fun updateDashboardProgress() {
        val dashboardFragment =
            supportFragmentManager.findFragmentByTag("DashboardFragment")
        if (dashboardFragment is DashboardFragment) {
            dashboardFragment.updateProgressUI()
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("LifePulsePrefs", Context.MODE_PRIVATE)
        val userName = prefs.getString("userName", "User")
        val headerGreeting = findViewById<TextView>(R.id.tvGreeting)
        headerGreeting?.text = "Hello, $userName "
    }

    //  Mood dialog
    fun showMoodDialog() {
        val moods = arrayOf("😃 Happy", "⚡ Energized", "😠 Frustrated", "😵 Overwhelmed", "😌 Peaceful", "💪 Motivated")

        AlertDialog.Builder(this)
            .setTitle("How are you feeling?")
            .setItems(moods) { _, which ->
                val mood = moods[which]
                val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

                val prefs = getSharedPreferences("LifePulsePrefs", Context.MODE_PRIVATE)
                val gson = Gson()
                val json = prefs.getString("moods", null)
                val type = object : TypeToken<MutableList<MoodEntry>>() {}.type
                val moodList: MutableList<MoodEntry> = if (json != null) {
                    gson.fromJson(json, type)
                } else mutableListOf()

                moodList.add(0, MoodEntry(mood, timestamp))
                prefs.edit().putString("moods", gson.toJson(moodList)).apply()

                Toast.makeText(this, "Mood saved: $mood", Toast.LENGTH_SHORT).show()

                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (currentFragment is MoodFragment) {
                    currentFragment.refreshMoods()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    //  Handle toggle clicks
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return if (toggle.onOptionsItemSelected(item)) true
        else super.onOptionsItemSelected(item)
    }
}
