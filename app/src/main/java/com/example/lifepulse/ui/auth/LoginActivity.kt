package com.example.lifepulse.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lifepulse.databinding.ActivityLoginBinding
import com.example.lifepulse.ui.main.MainActivity

class LoginActivity : AppCompatActivity() {
    private lateinit var b: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        val prefs = getSharedPreferences("LifePulsePrefs", Context.MODE_PRIVATE)

        b.btnLogin.setOnClickListener {
            val email = b.etEmail.text.toString().trim()
            val pass = b.etPassword.text.toString()

            val savedEmail = prefs.getString("userEmail", null)
            val savedPass = prefs.getString("userPassword", null)
            val savedName = prefs.getString("userName", "User") //  consistent key

            if (savedEmail == null || savedPass == null) {
                Toast.makeText(this, "No account found. Please sign up.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (email == savedEmail && pass == savedPass) {
                //  Save login state & persist user details for Profile
                prefs.edit()
                    .putBoolean("isLoggedIn", true)
                    .putString("userEmail", savedEmail)  // make sure it's refreshed
                    .putString("userName", savedName)    // keep username consistent
                    .apply()

                Toast.makeText(this, "Welcome back, $savedName ", Toast.LENGTH_SHORT).show()

                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show()
            }
        }

        b.tvGoSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}
