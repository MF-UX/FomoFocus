package com.example.fomofocus

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.*
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.widget.PopupMenu
import com.google.android.material.progressindicator.CircularProgressIndicator

class DashboardActivity : AppCompatActivity() {

    private lateinit var tvTimer: TextView
    private lateinit var btnPlay: ImageButton
    private lateinit var btnStop: ImageButton
    private lateinit var btnSwitchMode: Button
    private lateinit var tvMode: TextView
    private lateinit var spinnerDuration: Spinner
    private lateinit var etLesson: EditText
    private lateinit var dashboardLayout: ConstraintLayout
    private lateinit var btnMenu: ImageButton
    private lateinit var alarmSound: MediaPlayer
    private lateinit var circleProgress: CircularProgressIndicator

    private var countDownTimer: CountDownTimer? = null
    private var isRunning = false
    private var isPaused = false
    private var isBreakMode = false
    private var selectedTimeInMinutes = 0
    private var fullTimeInMillis: Long = 0L
    private var remainingTimeInMillis: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        createNotificationChannel()

        tvTimer = findViewById(R.id.tvTimer)
        btnPlay = findViewById(R.id.btnPlay)
        btnStop = findViewById(R.id.btnStop)
        btnSwitchMode = findViewById(R.id.btnSwitchMode)
        tvMode = findViewById(R.id.tvMode)
        spinnerDuration = findViewById(R.id.spinnerDuration)
        etLesson = findViewById(R.id.etDuration2)
        dashboardLayout = findViewById(R.id.dashboardLayout)
        btnMenu = findViewById(R.id.btn_menu)
        circleProgress = findViewById(R.id.circleProgress)

        initAlarm()
        initSpinner()
        initMenu()
        updateModeUI()

        findViewById<Button>(R.id.btnHistory).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        btnSwitchMode.setOnClickListener {
            isBreakMode = !isBreakMode
            updateModeUI()
        }

        btnPlay.setOnClickListener {
            val lesson = etLesson.text.toString().trim()
            if (lesson.isEmpty()) {
                Toast.makeText(this, "Isi pelajaran dulu sebelum mulai!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedTimeInMinutes <= 0) {
                Toast.makeText(this, "Pilih durasi timer!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // SAVE HISTORY HANYA SEKALI SAAT START PERTAMA
            if (!isRunning && !isPaused) {
                saveHistory(selectedTimeInMinutes, lesson)
                fullTimeInMillis = selectedTimeInMinutes * 60 * 1000L
                remainingTimeInMillis = fullTimeInMillis
            }

            when {
                !isRunning && !isPaused -> startTimer(remainingTimeInMillis)  // START
                isPaused -> startTimer(remainingTimeInMillis)                 // RESUME
                isRunning -> pauseTimer()                                     // PAUSE
            }
        }

        btnStop.setOnClickListener {
            stopTimer()
        }
    }

    private fun initAlarm() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        alarmSound = MediaPlayer().apply {
            setAudioAttributes(audioAttributes)
            val afd = resources.openRawResourceFd(R.raw.audio)
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            isLooping = false
            prepare()
        }
    }

    private fun initSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.timer_minutes,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDuration.adapter = adapter

        spinnerDuration.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long
            ) {
                val selected = parent.getItemAtPosition(position).toString()
                if (selected == "Pilih Waktu") {
                    selectedTimeInMinutes = 0
                    tvTimer.text = "00:00:00"
                    return
                }

                selectedTimeInMinutes = selected.toInt()

                if (!isRunning) {
                    val hours = selectedTimeInMinutes / 60
                    val minutes = selectedTimeInMinutes % 60
                    tvTimer.text = String.format("%02d:%02d:%02d", hours, minutes, 0)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun initMenu() {
        btnMenu.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.menu_dashboard, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.settings -> {
                        startActivity(Intent(this, ActivityProfile::class.java))
                        true
                    }

                    R.id.btnHistory -> {
                        startActivity(Intent(this, HistoryActivity::class.java))
                        true
                    }

                    R.id.action_signout -> {
                        val sharedPref = getSharedPreferences("UserData", MODE_PRIVATE)
                        sharedPref.edit().putBoolean("isLoggedIn", false).apply()
                        startActivity(Intent(this, MainActivity::class.java).apply {
                            flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                        finish()
                        true
                    }

                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun startTimer(timeInMillis: Long) {
        isRunning = true
        isPaused = false
        btnPlay.setImageResource(R.drawable.outline_pause_circle_24)
        btnStop.setImageResource(R.drawable.baseline_stop_circle_24)

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(timeInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeInMillis = millisUntilFinished

                val totalSeconds = millisUntilFinished / 1000
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60
                tvTimer.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)

                val progressPercent =
                    ((millisUntilFinished.toFloat() / fullTimeInMillis) * 100).toInt()
                circleProgress.progress = progressPercent
            }

            @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
            override fun onFinish() {
                isRunning = false
                isPaused = false
                showTimerNotification()
                resetUIAfterTimer()
            }
        }.start()
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        isPaused = true
        isRunning = false
        btnPlay.setImageResource(R.drawable.round_play_circle_outline_24)
        btnStop.setImageResource(R.drawable.baseline_stop_circle_24)
    }

    private fun stopTimer() {
        countDownTimer?.cancel()
        resetUIAfterTimer()
    }

    private fun resetUIAfterTimer() {
        remainingTimeInMillis = fullTimeInMillis
        tvTimer.text = "00:00:00"
        circleProgress.progress = 0
        btnPlay.setImageResource(R.drawable.round_play_circle_outline_24)
        btnStop.setImageResource(R.drawable.outline_nest_heat_link_e_24)
        etLesson.setText("")
        spinnerDuration.setSelection(0)
        dashboardLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
        isRunning = false
        isPaused = false
    }

    private fun updateModeUI() {
        if (isBreakMode) {
            tvMode.text = "Break Mode"
            dashboardLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
        } else {
            tvMode.text = "Focus Mode"
            dashboardLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
        }
    }

    private fun saveHistory(timeInMinutes: Int, lesson: String) {
        val sharedPref = getSharedPreferences("FomoFocusHistory", MODE_PRIVATE)
        val historySet =
            sharedPref.getStringSet("history", mutableSetOf())?.toMutableSet()
                ?: mutableSetOf()
        val timestamp = System.currentTimeMillis()
        val record = "$timestamp|$timeInMinutes menit|Pelajaran: $lesson"
        historySet.add(record)
        sharedPref.edit().putStringSet("history", historySet).apply()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showTimerNotification() {
        val channelId = "timer_channel_v2"
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                channelId,
                "Timer Completed",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                enableVibration(true)
                setSound(soundUri, audioAttributes)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, DashboardActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.fomofocusic)
            .setContentTitle("Timer Selesai")
            .setContentText("Waktu belajar kamu telah selesai!")
            .setAutoCancel(true)
            .setSound(soundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        NotificationManagerCompat.from(this).notify(1001, builder.build())
        alarmSound.start()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "timer_channel",
                "TimerChannel",
                NotificationManager.IMPORTANCE_HIGH
            )
            val notifMng = getSystemService(NotificationManager::class.java)
            notifMng.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        alarmSound.release()
    }
}
