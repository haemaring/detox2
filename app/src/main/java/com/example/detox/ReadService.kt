package com.example.detox

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class ReadService : Service(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var text = ""
    private var slot = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        slot = intent?.getIntExtra("slot", -1) ?: -1
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("read", "메시지 낭독", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "read")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("메시지를 읽는 중입니다")
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else startForeground(1, n)
        text = Briefing.build(this, slot)
        tts = TextToSpeech(this, this)
        return START_NOT_STICKY
    }

    override fun onInit(status: Int) {
        val t = tts ?: return finish()
        if (status != TextToSpeech.SUCCESS) return finish()
        t.language = Locale.KOREAN
        t.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
        )
        t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) { if (id == "last") finish(true) }
            @Deprecated("deprecated")
            override fun onError(id: String?) { finish() }
        })
        val parts = text.split("\n").filter { it.isNotBlank() }
        parts.forEachIndexed { i, s ->
            t.speak(s, TextToSpeech.QUEUE_ADD, null, if (i == parts.lastIndex) "last" else "p$i")
        }
    }

    private fun finish(done: Boolean = false) {
        // 정시 낭독을 끝까지 마친 경우에만 쌓인 메시지를 비움 (테스트는 유지)
        if (done && slot >= 0) Store.clear(this)
        stopSelf()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
