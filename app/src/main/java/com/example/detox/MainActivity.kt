package com.example.detox

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var b0: Button
    private lateinit var b1: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        status = TextView(this).apply { textSize = 16f }
        root.addView(status)

        fun btn(label: String, onClick: () -> Unit): Button {
            val b = Button(this)
            b.text = label
            b.setOnClickListener { onClick() }
            root.addView(b)
            return b
        }

        btn("1. 알림 접근 권한 설정") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        btn("2. 배터리 최적화 제외 설정") {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
        b0 = btn("") { pickTime(0) }
        b1 = btn("") { pickTime(1) }
        btn("지금 읽기 (테스트)") {
            startForegroundService(Intent(this, ReadService::class.java).putExtra("slot", -1))
        }
        setContentView(ScrollView(this).apply { addView(root) })

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        Scheduler.scheduleAll(this)
        refresh()
    }

    private fun fmt(slot: Int): String {
        val (h, m) = Store.getTime(this, slot)
        return "%02d:%02d".format(h, m)
    }

    private fun refresh() {
        val on = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?.contains(packageName) == true
        status.text = "알림 접근: ${if (on) "켜짐 ✅" else "꺼짐 ❌ (1번 버튼에서 켜주세요)"}\n" +
                "대기 중 메시지: ${Store.all(this).size}건\n" +
                "아침 ${fmt(0)} / 저녁 ${fmt(1)}"
        b0.text = "아침 시간 변경 (${fmt(0)})"
        b1.text = "저녁 시간 변경 (${fmt(1)})"
    }

    private fun pickTime(slot: Int) {
        val (h, m) = Store.getTime(this, slot)
        TimePickerDialog(this, { _, hh, mm ->
            Store.setTime(this, slot, hh, mm)
            Scheduler.schedule(this, slot)
            refresh()
        }, h, m, true).show()
    }
}
