package com.example.fomofocus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.widget.PopupMenu
import android.util.Log

class DashboardActivity : AppCompatActivity() {

    private lateinit var tvTimer: TextView
    private lateinit var btnStart: Button
    private lateinit var btnSwitchMode: Button
    private lateinit var tvMode: TextView
    private lateinit var etDuration: EditText
    private lateinit var dashboardLayout: ConstraintLayout
    private lateinit var btnMenu: ImageButton
    private lateinit var alarmSound: MediaPlayer

    private var countDownTimer: CountDownTimer? = null
    private var isRunning = false
    private var selectedTimeInMinutes = 0
    private var isBreakMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Inisialisasi View
        tvTimer = findViewById(R.id.tvTimer)
        btnStart = findViewById(R.id.btnStart)
        btnSwitchMode = findViewById(R.id.btnSwitchMode)
        tvMode = findViewById(R.id.tvMode)
        etDuration = findViewById(R.id.etDuration)
        dashboardLayout = findViewById(R.id.dashboardLayout)
        btnMenu = findViewById(R.id.btn_menu)

        alarmSound = MediaPlayer.create(this, R.raw.alarm_sound)
        createNotificationChannel()

        btnMenu.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.menu_dashboard, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.settings -> {
                        startActivity(Intent(this, ActivityProfile::class.java))
                        true
                    }
                    R.id.action_signout -> {
                        val sharedPref = getSharedPreferences("UserData", MODE_PRIVATE)
                        val username = sharedPref.getString("username", "unknown")
                        val email = sharedPref.getString("email", "unknown")
                        sharedPref.edit().putBoolean("isLoggedIn", false).apply()
                        Log.d("USER_SIGNOUT", "User $username ($email) logout — data tetap tersimpan.")
                        Toast.makeText(this, "Berhasil keluar akun", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        // Tombol Start / Stop
        btnStart.setOnClickListener {
            val inputText = etDuration.text.toString().trim()
            if (inputText.isNotEmpty()) {
                selectedTimeInMinutes = try {
                    inputText.toInt()
                } catch (e: NumberFormatException) {
                    25
                }
            }

            if (!isRunning) {
                startTimer(selectedTimeInMinutes * 60 * 1000L)
            } else {
                stopTimer()
            }
        }

        // Tombol Switch Mode
        btnSwitchMode.setOnClickListener {
            isBreakMode = !isBreakMode
            updateModeUI()
        }

        // Set tampilan awal
        updateModeUI()
    }

    private fun startTimer(timeInMillis: Long) {
        isRunning = true
        btnStart.text = "STOP"

        // Ubah background saat timer berjalan
        if (isBreakMode) {
            dashboardLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
        } else {
            dashboardLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.biru_tosca))
        }

        countDownTimer = object : CountDownTimer(timeInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 1000 / 60
                val seconds = (millisUntilFinished / 1000) % 60
                tvTimer.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                isRunning = false
                tvTimer.text = "00:00"
                btnStart.text = "START"
                dashboardLayout.setBackgroundColor(ContextCompat.getColor(this@DashboardActivity, R.color.white))
                alarmSound.start()

                val msg = if (isBreakMode)
                    "Waktu istirahat selesai, ayo fokus lagi!"
                else
                    "Waktu habis, istirahat dulu!"
                showNotification("FomoFocus", msg)
            }
        }.start()
    }

    private fun stopTimer() {
        countDownTimer?.cancel()
        isRunning = false
        btnStart.text = "START"

        // Reset background ke putih saat stop ditekan
        dashboardLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
    }

    private fun updateModeUI() {
        if (isBreakMode) {
            tvMode.text = "Break Mode"
            dashboardLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
            btnStart.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
            btnStart.setTextColor(ContextCompat.getColor(this, R.color.green))
        } else {
            tvMode.text = "FomoFocus Mode"
            dashboardLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
            btnStart.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
            btnStart.setTextColor(ContextCompat.getColor(this, R.color.white))
        }
    }

    private fun showNotification(title: String, message: String) {
        val builder = NotificationCompat.Builder(this, "timer_channel")
            .setSmallIcon(R.drawable.fomofocusic)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = NotificationManagerCompat.from(this)
        if (Build.VERSION.SDK_INT < 33 || checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(1001, builder.build())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "TimerChannel"
            val descriptionText = "Channel for FomoFocus Timer Notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("timer_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        alarmSound.release()
    }
}
