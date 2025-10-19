package com.example.lifepulse.ui.main.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.lifepulse.R
import com.example.lifepulse.data.BackupManager
import com.example.lifepulse.ui.auth.LoginActivity
import com.example.lifepulse.ui.auth.SignupActivity
import com.example.lifepulse.ui.workers.HydrationWorkerActivity
import com.example.lifepulse.ui.workers.StepsHistoryFragment

class SettingsFragment : Fragment() {

    private val CREATE_BACKUP_FILE = 1001
    private val PICK_RESTORE_FILE = 1002

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val prefs = requireActivity().getSharedPreferences("LifePulsePrefs", Context.MODE_PRIVATE)
        val userName = prefs.getString("userName", "Unknown User")
        val userEmail = prefs.getString("userEmail", "No Email")

        //  Show user info
        val tvUserInfo = view.findViewById<TextView>(R.id.tvUserInfo)
        tvUserInfo.text = "Logged in as:\n$userName\n$userEmail"

        val btnLogout = view.findViewById<Button>(R.id.btnLogout)
        val btnHydration = view.findViewById<Button>(R.id.btnHydration)
        val btnDeleteAccount = view.findViewById<Button>(R.id.btnDeleteAccount)
        val btnStepsHistory = view.findViewById<Button>(R.id.btnStepsHistory)

        val btnBackup = view.findViewById<Button>(R.id.btnBackup)   //  Backup button
        val btnRestore = view.findViewById<Button>(R.id.btnRestore) //  Restore button

        //  Logout
        btnLogout.setOnClickListener {
            prefs.edit().putBoolean("isLoggedIn", false).apply()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        //  Hydration
        btnHydration.setOnClickListener {
            startActivity(Intent(requireContext(), HydrationWorkerActivity::class.java))
        }

        //  Steps History
        btnStepsHistory.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, StepsHistoryFragment())
                .addToBackStack(null)
                .commit()
        }

        //  Delete Account
        btnDeleteAccount.setOnClickListener {
            val dialogView = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 40, 50, 10)

                val etEmail = EditText(context).apply { hint = "Enter your email" }
                val etPassword = EditText(context).apply {
                    hint = "Enter your password"
                    inputType = android.text.InputType.TYPE_CLASS_TEXT or
                            android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                }

                addView(etEmail)
                addView(etPassword)

                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Account")
                    .setMessage("Enter your email and password to confirm deletion.")
                    .setView(this)
                    .setPositiveButton("Delete") { _, _ ->
                        val enteredEmail = etEmail.text.toString().trim()
                        val enteredPass = etPassword.text.toString().trim()
                        val savedEmail = prefs.getString("userEmail", null)
                        val savedPass = prefs.getString("userPassword", null)

                        if (enteredEmail == savedEmail && enteredPass == savedPass) {
                            prefs.edit().clear().apply()
                            Toast.makeText(requireContext(), "Account deleted successfully.", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(requireContext(), SignupActivity::class.java))
                            requireActivity().finish()
                        } else {
                            Toast.makeText(requireContext(), "Invalid email or password.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        //  Backup
        btnBackup.setOnClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, "lifepulse_backup.json")
            }
            startActivityForResult(intent, CREATE_BACKUP_FILE)
        }

        //  Restore
        btnRestore.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
            }
            startActivityForResult(intent, PICK_RESTORE_FILE)
        }

        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data?.data == null) return
        val uri: Uri = data.data!!

        when (requestCode) {
            CREATE_BACKUP_FILE -> {
                BackupManager.backupData(requireContext(), uri)
                Toast.makeText(requireContext(), "Backup created!", Toast.LENGTH_SHORT).show()
            }
            PICK_RESTORE_FILE -> {
                BackupManager.restoreData(requireContext(), uri)
                Toast.makeText(requireContext(), "Data restored!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
