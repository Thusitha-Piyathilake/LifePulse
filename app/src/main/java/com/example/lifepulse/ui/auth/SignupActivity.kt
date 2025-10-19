package com.example.lifepulse.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lifepulse.databinding.ActivitySignupBinding
import com.example.lifepulse.ui.main.MainActivity

class SignupActivity : AppCompatActivity() {
    private lateinit var b: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnCreate.setOnClickListener {
            val name = b.etName.text.toString().trim()
            val email = b.etEmail.text.toString().trim()
            val pass = b.etPassword.text.toString()
            val confirm = b.etConfirm.text.toString()

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pass != confirm) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val prefs = getSharedPreferences("LifePulsePrefs", Context.MODE_PRIVATE)

            //  Save account details consistently
            prefs.edit()
                .putString("userName", name)      //  Save userName
                .putString("userEmail", email)
                .putString("userPassword", pass)
                .putBoolean("isLoggedIn", true)   // auto-login after signup
                .apply()

            Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()

            //  Go straight to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
