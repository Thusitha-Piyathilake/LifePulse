package com.example.lifepulse.ui.main.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.lifepulse.R
import java.io.File

class ProfileFragment : Fragment() {

    private lateinit var ivProfile: ImageView
    private lateinit var prefs: android.content.SharedPreferences
    private var cameraImageUri: Uri? = null

    //  Gallery picker
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val imageUri: Uri? = data?.data
                if (imageUri != null) {
                    saveProfileImage(imageUri)
                }
            }
        }

    //  Camera launcher
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraImageUri != null) {
                saveProfileImage(cameraImageUri!!)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        prefs = requireContext().getSharedPreferences("LifePulsePrefs", Context.MODE_PRIVATE)

        // Load data
        val userName = prefs.getString("userName", "Guest")
        val userEmail = prefs.getString("userEmail", "Not set")
        val stepsGoal = prefs.getInt("stepsGoal", 8000)
        val waterGoal = prefs.getInt("waterGoal", 2000)
        val streaks = prefs.getInt("habitStreak", 0)
        val joinedDate = prefs.getString("joinedDate", "N/A")
        val savedImageUri = prefs.getString("profileImageUri", null)

        // Bind UI
        val tvName = view.findViewById<TextView>(R.id.tvProfileName)
        val tvEmail = view.findViewById<TextView>(R.id.tvProfileEmail)
        val tvStepsGoal = view.findViewById<TextView>(R.id.tvStepsGoal)
        val tvWaterGoal = view.findViewById<TextView>(R.id.tvWaterGoal)
        val tvStreaks = view.findViewById<TextView>(R.id.tvStreaks)
        val tvJoinedDate = view.findViewById<TextView>(R.id.tvJoinedDate)
        val btnEdit = view.findViewById<Button>(R.id.btnEditProfile)
        val btnChangePic = view.findViewById<Button>(R.id.btnChangePic)
        val btnChangePassword = view.findViewById<Button>(R.id.btnChangePassword) //
        ivProfile = view.findViewById(R.id.ivProfile)

        // Show current values
        tvName.text = "Name: $userName"
        tvEmail.text = "Email: $userEmail"
        tvStepsGoal.text = "Steps Goal: $stepsGoal"
        tvWaterGoal.text = "Water Goal: ${waterGoal}ml"
        tvStreaks.text = "Habit Streak: $streaks days"
        tvJoinedDate.text = "Joined: $joinedDate"

        if (savedImageUri != null) {
            ivProfile.setImageURI(Uri.parse(savedImageUri))
        } else {
            ivProfile.setImageResource(R.drawable.ic_profile)
        }

        //  Edit Profile
        btnEdit.setOnClickListener {
            showEditDialog(userName ?: "", userEmail ?: "", stepsGoal, waterGoal)
        }

        //  Change Picture → choose Gallery or Camera
        btnChangePic.setOnClickListener {
            val options = arrayOf("Choose from Gallery", "Take Photo with Camera", "Remove Picture")
            AlertDialog.Builder(requireContext())
                .setTitle("Update Profile Picture")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            val pickIntent =
                                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                            pickImageLauncher.launch(pickIntent)
                        }

                        1 -> {
                            val photoFile = File(requireContext().cacheDir, "profile_pic.jpg")
                            cameraImageUri = FileProvider.getUriForFile(
                                requireContext(),
                                requireContext().packageName + ".provider",
                                photoFile
                            )
                            takePictureLauncher.launch(cameraImageUri)
                        }

                        2 -> {
                            ivProfile.setImageResource(R.drawable.ic_profile)
                            prefs.edit().remove("profileImageUri").apply()
                        }
                    }
                }
                .show()
        }

        //  Change Password
        btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        return view
    }

    private fun saveProfileImage(uri: Uri) {
        ivProfile.setImageURI(uri)
        prefs.edit().putString("profileImageUri", uri.toString()).apply()
    }

    private fun showEditDialog(
        currentName: String,
        currentEmail: String,
        currentSteps: Int,
        currentWater: Int
    ) {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null)

        val etName = dialogView.findViewById<EditText>(R.id.etEditName)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEditEmail)
        val etSteps = dialogView.findViewById<EditText>(R.id.etEditStepsGoal)
        val etWater = dialogView.findViewById<EditText>(R.id.etEditWaterGoal)

        etName.setText(currentName)
        etEmail.setText(currentEmail)
        etSteps.setText(currentSteps.toString())
        etWater.setText(currentWater.toString())

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                prefs.edit()
                    .putString("userName", etName.text.toString())
                    .putString("userEmail", etEmail.text.toString())
                    .putInt("stepsGoal", etSteps.text.toString().toIntOrNull() ?: currentSteps)
                    .putInt("waterGoal", etWater.text.toString().toIntOrNull() ?: currentWater)
                    .apply()

                // Refresh fragment
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, ProfileFragment())
                    .commit()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    //  Change Password Dialog
    private fun showChangePasswordDialog() {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null)

        val etOldPassword = dialogView.findViewById<EditText>(R.id.etOldPassword)
        val etNewPassword = dialogView.findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = dialogView.findViewById<EditText>(R.id.etConfirmPassword)

        AlertDialog.Builder(requireContext())
            .setTitle("Change Password")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val savedPass = prefs.getString("userPassword", null)

                val oldPass = etOldPassword.text.toString()
                val newPass = etNewPassword.text.toString()
                val confirmPass = etConfirmPassword.text.toString()

                if (savedPass != oldPass) {
                    Toast.makeText(requireContext(), "Old password incorrect", Toast.LENGTH_SHORT)
                        .show()
                    return@setPositiveButton
                }

                if (newPass.isEmpty() || newPass != confirmPass) {
                    Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT)
                        .show()
                    return@setPositiveButton
                }

                prefs.edit().putString("userPassword", newPass).apply()
                Toast.makeText(
                    requireContext(),
                    "Password updated successfully ",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
