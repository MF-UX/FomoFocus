package com.example.fomofocus

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.fomofocus.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // ==============================
    // 🔹 TAMBAHAN UNTUK SHOW PASSWORD
    // ==============================
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        // 🔹 Konfigurasi Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        initUI()
    }

    private fun initUI() {

        // ======================================================
        // 🔹 SHOW / HIDE PASSWORD (Kode FIX Ditambahkan Disini)
        // ======================================================
        binding.ivShowPassword?.setOnClickListener {
            isPasswordVisible = !isPasswordVisible

            if (isPasswordVisible) {
                binding.etPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.ivShowPassword?.setImageResource(R.drawable.baseline_visibility_24)
            } else {
                binding.etPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.ivShowPassword?.setImageResource(R.drawable.round_visibility_off_24)
            }

            binding.etPassword.setSelection(binding.etPassword.text.length)
        }


        // 🔹 Tombol login manual (XAMPP)
        binding.btnSignIn.setOnClickListener {
            val loginInput = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (loginInput.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Isi semua data!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val url = "http://172.20.10.3/Database/login.php"

            val request = object : StringRequest(
                Request.Method.POST, url,
                { response ->
                    try {
                        val json = JSONObject(response)
                        val success = json.getBoolean("success")
                        val message = json.getString("message")

                        if (success) {
                            val username = json.optString("username")
                            val gmail = json.optString("gmail")
                            val passwordUser = json.optString("password")

                            val sharedPref = getSharedPreferences("UserData", MODE_PRIVATE)
                            sharedPref.edit {
                                putString("username", username)
                                putString("email", gmail)
                                putString("password", passwordUser)
                                putBoolean("isLoggedIn", true)
                            }

                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, DashboardActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this, "Kesalahan parsing data: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                },
                { error ->
                    Toast.makeText(this, "Koneksi gagal: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            ) {
                override fun getParams(): MutableMap<String, String> {
                    val params = HashMap<String, String>()
                    params["loginInput"] = loginInput
                    params["password"] = password
                    return params
                }
            }

            Volley.newRequestQueue(this).add(request)
        }


        // 🔹 Tombol daftar
        binding.tvSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        // 🔹 Tombol lupa password (Firebase)
        binding.tvForgetPassword.setOnClickListener {
            val input = android.widget.EditText(this)
            input.hint = "Masukkan email kamu"

            AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setView(input)
                .setPositiveButton("Kirim") { _, _ ->
                    val email = input.text.toString().trim()
                    if (email.isEmpty()) {
                        Toast.makeText(this, "Email tidak boleh kosong", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    firebaseAuth.sendPasswordResetEmail(email)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Link reset dikirim ke $email", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                            Log.e("RESET_PASSWORD", "Error: ${e.message}")
                        }
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        // 🔹 Tombol login Google
        binding.btnGoogle?.setOnClickListener {
            signInWithGoogle()
        }
    }

    // ==================================================
    //                  LOGIN GOOGLE
    // ==================================================
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    val name = user?.displayName
                    val email = user?.email
                    val photoUrl = user?.photoUrl

                    Toast.makeText(this, "Selamat datang, $name!", Toast.LENGTH_SHORT).show()
                    Log.d("GOOGLE_LOGIN", "Login sukses: $name ($email)")

                    val sharedPref = getSharedPreferences("UserData", MODE_PRIVATE)
                    sharedPref.edit {
                        putString("email", email)
                        putString("username", name)
                        putString("photoUrl", photoUrl?.toString())
                        putString("password", "")
                        putBoolean("isLoggedIn", true)
                    }

                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Autentikasi gagal!", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
