package com.example.fomofocus

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import java.text.SimpleDateFormat
import java.util.*

data class HistoryRecord(val timestamp: Long, val duration: String, val lesson: String)

class HistoryActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var btnBack: ImageButton
    private lateinit var btnClearHistory: Button
    private lateinit var historyList: MutableList<HistoryRecord>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        listView = findViewById(R.id.listViewHistory)
        btnBack = findViewById(R.id.btnBackHistory)
        btnClearHistory = findViewById(R.id.btnClearHistory)

        val sharedPref = getSharedPreferences("FomoFocusHistory", MODE_PRIVATE)
        val historySet = sharedPref.getStringSet("history", setOf())?.toList() ?: listOf()

        historyList = historySet.mapNotNull { record ->
            val parts = record.split("|")
            val time = parts.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
            val duration = parts.getOrNull(1) ?: ""
            val lesson = parts.getOrNull(2) ?: ""
            HistoryRecord(time, duration, lesson)
        }.sortedByDescending { it.timestamp }.toMutableList()

        val adapter = object : BaseAdapter() {
            override fun getCount() = historyList.size
            override fun getItem(position: Int) = historyList[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val view = convertView ?: LayoutInflater.from(this@HistoryActivity)
                    .inflate(R.layout.history_item, parent, false)
                val record = historyList[position]

                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                view.findViewById<TextView>(R.id.tvDateTime).text = sdf.format(Date(record.timestamp))
                view.findViewById<TextView>(R.id.tvDuration).text = "Durasi: ${record.duration}"
                view.findViewById<TextView>(R.id.tvLesson).text = "${record.lesson}"
                return view
            }
        }

        listView.adapter = adapter

        btnBack.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }

        btnClearHistory.setOnClickListener {
            sharedPref.edit().clear().apply() // hapus semua history
            historyList.clear()
            (listView.adapter as BaseAdapter).notifyDataSetChanged()
        }
    }
}
