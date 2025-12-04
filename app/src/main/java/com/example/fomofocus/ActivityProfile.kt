package com.example.fomofocus

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import com.example.fomofocus.databinding.ActivityProfileBinding

class ActivityProfile : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private var isPasswordVisible = false
    private var isEditing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val username = sharedPref.getString("username", "")
        val email = sharedPref.getString("email", "")
        val password = sharedPref.getString("password", "")

        if (username.isNullOrEmpty()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding.username.setText(username)
        binding.email.setText(email)
        binding.username3.setText(password)
        binding.textView11.text = username

        setEditable(false)

        binding.btnSave.alpha = 0f
        binding.btnSave.isEnabled = false

        binding.btnBack.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }

        binding.settingsProfile.setOnClickListener {
            val popup = PopupMenu(this, binding.settingsProfile)
            popup.menuInflater.inflate(R.menu.menu_profile, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {

                    R.id.simpan_profile -> {
                        if (!isEditing) enterEditMode()
                        true
                    }

                    R.id.hapus_akun -> {
                        sharedPref.edit().clear().apply()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                        true
                    }

                    else -> false
                }
            }

            popup.show()
        }

        binding.btnSave.setOnClickListener {
            saveProfile(sharedPref)
        }

        binding.ivShowPassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible

            if (isPasswordVisible) {
                binding.username3.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.ivShowPassword.setImageResource(R.drawable.baseline_visibility_24)
            } else {
                binding.username3.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.ivShowPassword.setImageResource(R.drawable.round_visibility_off_24)
            }

            binding.username3.setSelection(binding.username3.text.length)
        }
    }

    private fun enterEditMode() {
        isEditing = true
        setEditable(true)

        binding.btnSave.animate().alpha(1f).setDuration(200)
        binding.btnSave.isEnabled = true

        Toast.makeText(this, "Silahkan di perbarui!", Toast.LENGTH_SHORT).show()
    }

    private fun saveProfile(sharedPref: android.content.SharedPreferences) {
        if (!isEditing) return

        val newUser = binding.username.text.toString().trim()
        val newEmail = binding.email.text.toString().trim()
        val newPass = binding.username3.text.toString().trim()

        if (newUser.isEmpty() || newEmail.isEmpty() || newPass.isEmpty()) {
            Toast.makeText(this, "Semua field wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        sharedPref.edit().apply {
            putString("username", newUser)
            putString("email", newEmail)
            putString("password", newPass)
            apply()
        }

        binding.textView11.text = newUser

        setEditable(false)

        binding.btnSave.animate().alpha(0f).setDuration(200)
        binding.btnSave.isEnabled = false

        isEditing = false
        Toast.makeText(this, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        binding.username3.inputType = if (isPasswordVisible) {
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        binding.username3.setSelection(binding.username3.text?.length ?: 0)
    }

    private fun setEditable(isEnabled: Boolean) {
        binding.username.isEnabled = isEnabled
        binding.email.isEnabled = isEnabled
        binding.username3.isEnabled = isEnabled
    }
}
