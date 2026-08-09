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
import android.os.PowerManager
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import com.vikalpai.maya.MainActivity

/**
 * Approximates "Hey Maya" always-on listening using Android's built-in
 * SpeechRecognizer in a restart loop, inside a foreground service.
 *
 * Honesty note: this is NOT the same technology as Siri/Google Assistant's
 * hotword detection, which runs on a dedicated always-on low-power DSP chip
 * — third-party apps have no access to that hardware path on any phone.
 * This is the best software-only approximation, tuned here to use less
 * battery: the wake lock is only held during an actual listening window
 * (not the whole time the service is running), idle restarts are spaced
 * out a bit more, and the service auto-stops after MAX_RUNTIME_MS so it
 * can't silently drain the battery all day if you forget to turn it off.
 */
class HotwordService : Service() {

    private var recognizer: SpeechRecognizer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var startedAt = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification("Maya sun rahi hai", "'Hey Maya' bolke command do"))
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Maya::HotwordWakeLock").apply {
            setReferenceCounted(false)
        }
        isRunning = true
        startedAt = SystemClock.elapsedRealtime()
        listenOnce()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun buildNotification(title: String, text: String): Notification {
        val channelId = "maya_hotword"
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(channelId) == null) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Maya background listening", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun listenOnce() {
        if (!isRunning) return

        val elapsed = SystemClock.elapsedRealtime() - startedAt
        if (elapsed > MAX_RUNTIME_MS) {
            // Auto-stop so a forgotten toggle doesn't drain the battery all day.
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(
                NOTIF_ID,
                buildNotification("Maya listening band ho gayi", "Battery bachane ke liye — 🎧 se dobara on karo")
            )
            stopSelf()
            return
        }

        // Only hold the wake lock for this one listening window, not the
        // whole time the service is alive — lets the CPU doze in between.
        wakeLock?.let { if (!it.isHeld) it.acquire(15_000L) }

        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                releaseWakeLock()
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?: ""
                if (text.lowercase().contains("maya")) {
                    val command = text.lowercase().substringAfter("maya").trim().ifBlank { text }
                    launchWithCommand(command)
                }
                handler.postDelayed({ listenOnce() }, IDLE_RESTART_DELAY_MS)
            }

            override fun onError(error: Int) {
                releaseWakeLock()
                // Most errors here are just "no speech detected" (the common
                // case) — a slightly longer gap before relistening saves
                // meaningful battery over a full day of near-silence.
                handler.postDelayed({ listenOnce() }, ERROR_RESTART_DELAY_MS)
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

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
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
        releaseWakeLock()
        super.onDestroy()
    }

    companion object {
        const val NOTIF_ID = 42
        private const val IDLE_RESTART_DELAY_MS = 700L
        private const val ERROR_RESTART_DELAY_MS = 1500L
        private const val MAX_RUNTIME_MS = 60 * 60 * 1000L // 1 hour auto-stop
    }
}
