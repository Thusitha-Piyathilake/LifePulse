package com.example.lifepulse.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.lifepulse.databinding.ActivitySplashBinding
import com.example.lifepulse.ui.main.MainActivity

class SplashActivity : AppCompatActivity() {
    private lateinit var b: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(b.root)

        //  Small delay for splash effect
        b.root.postDelayed({
            val prefs = getSharedPreferences("LifePulsePrefs", MODE_PRIVATE)
            val isLoggedIn = prefs.getBoolean("isLoggedIn", false)

            if (isLoggedIn) {
                // User already logged in → go to MainActivity
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                // First time or logged out → go to LoginActivity
                startActivity(Intent(this, LoginActivity::class.java))
            }

            finish()
        }, 1500) // 1.5s delay for splash
    }
}
