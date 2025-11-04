package com.example.fomofocus

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.fomofocus.databinding.ActivityProfileBinding

class ActivityProfile : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPref = getSharedPreferences("UserData", MODE_PRIVATE)
        val username = sharedPref.getString("username", null)
        val email = sharedPref.getString("email", null)
        val password = sharedPref.getString("password", null)

        if (username == null || email == null || password == null) {
            Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Tampilkan data user
        binding.username.setText(username)
        binding.email.setText(email)
        binding.username3.setText(password)
        binding.textView11.text = username

        Log.d("USER_PROFILE", "Username: $username | Email: $email | Password: $password")

        // Tombol back
        binding.btnBack.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            finish()
        }

        // Simpan perubahan profil
        binding.editProfile.setOnClickListener {
            val newUsername = binding.username.text.toString().trim()
            val newEmail = binding.email.text.toString().trim()
            val newPassword = binding.username3.text.toString().trim()

            if (newUsername.isEmpty() || newEmail.isEmpty() || newPassword.isEmpty()) {
                Toast.makeText(this, "Semua field wajib diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            with(sharedPref.edit()) {
                putString("username", newUsername)
                putString("email", newEmail)
                putString("password", newPassword)
                apply()
            }

            binding.textView11.text = newUsername
            Log.d("USER_UPDATE", "Data diperbarui → Username: $newUsername | Email: $newEmail | Password: $newPassword")
            Toast.makeText(this, "Profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
        }

        // Hapus akun
        binding.hapusAkun.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Hapus Akun")
                .setMessage("Apakah kamu yakin ingin menghapus akun ini? Semua data akan hilang permanen bro!")
                .setPositiveButton("Y, Hapus aja") { _, _ ->
                    sharedPref.edit().clear().apply()
                    Log.d("USER_DELETE", "Akun berhasil dihapus dari SharedPreferences")
                    Toast.makeText(this, "Akun berhasil dihapus!", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, SignupActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("kaga jadi hapus", null)
                .show()
        }

        // 👁 Toggle password visibility
        binding.ivShowPassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                binding.username3.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.ivShowPassword.setImageResource(R.drawable.baseline_visibility_24)
            } else {
                binding.username3.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.ivShowPassword.setImageResource(R.drawable.baseline_visibility_24)
            }
            // Geser cursor ke akhir teks
            binding.username3.setSelection(binding.username3.text?.length ?: 0)
        }
    }
}
