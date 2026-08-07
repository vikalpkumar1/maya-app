package com.vikalpai.maya.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import com.vikalpai.maya.MainActivity

/**
 * Approximates "Hey Maya" always-on listening using Android's built-in
 * SpeechRecognizer in a restart loop, inside a foreground service.
 *
 * Important honesty note: this is NOT the same technology as Siri/Google
 * Assistant's hotword detection, which runs on a dedicated always-on
 * low-power DSP chip with a proprietary model baked into the phone's
 * firmware. Third-party apps have no access to that hardware path, on
 * any phone. This service is the best available software-only approximation
 * — it works, but costs more battery, needs the mic to be free, and (by
 * Android's own privacy rules) must show a persistent notification the
 * whole time it's listening. There is no way to background-listen without
 * that notification — that's an OS-level rule, not a Maya limitation.
 */
class HotwordService : Service() {

    private var recognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification())
        startListeningLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun buildNotification(): Notification {
        val channelId = "maya_hotword"
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(channelId) == null) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Maya background listening", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Maya sun rahi hai")
            .setContentText("'Hey Maya' bolke command do")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun startListeningLoop() {
        isRunning = true
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        listenOnce()
    }

    private fun listenOnce() {
        if (!isRunning) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?: ""
                if (text.lowercase().contains("maya")) {
                    val command = text.lowercase().substringAfter("maya").trim().ifBlank { text }
                    launchWithCommand(command)
                }
                handler.postDelayed({ listenOnce() }, 400)
            }

            override fun onError(error: Int) {
                // Common on-device: no speech detected, timeout — just relisten.
                handler.postDelayed({ listenOnce() }, 800)
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer?.startListening(intent)
    }

    private fun launchWithCommand(command: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            putExtra(MainActivity.EXTRA_VOICE_COMMAND, command)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        isRunning = false
        recognizer?.destroy()
        super.onDestroy()
    }

    companion object {
        const val NOTIF_ID = 42
    }
}
