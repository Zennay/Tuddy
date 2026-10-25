package com.livelingo.android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Handler
import android.os.Looper

/**
 * Permission-free (beyond existing RECORD_AUDIO) main-thread guard for an
 * explicitly user-started on-device SpeechRecognizer session.
 *
 * Android can revoke mic permission, mute the microphone or switch into a
 * phone/VoIP audio mode *after* the user initially tapped Listen. Check at
 * admission and periodically during capture; a suspicious/unknown state
 * stops the existing recognizer and all queued restarts through the owner.
 *
 * AudioManager signals are best effort and this cannot detect every privacy
 * toggle or independently prove physical microphone isolation. No audio,
 * utterances, call details, timestamps, phone state or network IDs are stored.
 */
internal class LocalMicrophonePrivacyGuard(context: Context) {
    private val appContext = context.applicationContext
    private val audio = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private var active = false
    private var epoch = 0L
    private var checkTask: Runnable? = null
    private var onInterrupted: ((String) -> Unit)? = null

    fun isActive(): Boolean = active

    fun currentBlocker(): String? {
        val permission = runCatching {
            appContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        }.getOrNull()
        if (permission != true) {
            return "De Android-microfoontoestemming is ingetrokken of kon niet worden gelezen."
        }

        val currentAudio = audio ?: return "Androids audio- en microfoonstatus is niet beschikbaar."
        val state = runCatching {
            currentAudio.isMicrophoneMute to currentAudio.mode
        }.getOrNull() ?: return "Android kon de microfoonstatus niet betrouwbaar controleren."
        if (state.first) return "Android meldt dat de microfoon is gedempt."
        if (state.second == AudioManager.MODE_IN_CALL ||
            state.second == AudioManager.MODE_IN_COMMUNICATION
        ) {
            return "Android meldt een actieve oproep- of communicatiemodus."
        }
        if (state.second != AudioManager.MODE_NORMAL &&
            state.second != AudioManager.MODE_RINGTONE
        ) {
            return "Android meldt een onbekende audio-/oproepmodus."
        }
        return null
    }

    /**
     * Fail closed if a check cannot be scheduled. Only one active guard is
     * retained; each new SpeechRecognizer turn replaces the previous watcher.
     */
    fun arm(interrupted: (String) -> Unit): Boolean {
        disarm()
        if (currentBlocker() != null) return false

        active = true
        onInterrupted = interrupted
        val myEpoch = epoch
        val task = object : Runnable {
            override fun run() {
                if (!active || epoch != myEpoch) return
                val reason = currentBlocker()
                if (reason != null) {
                    val notify = onInterrupted
                    disarm()
                    notify?.invoke(reason)
                } else if (!handler.postDelayed(this, CHECK_INTERVAL_MS)) {
                    val notify = onInterrupted
                    disarm()
                    notify?.invoke("Android kon de microfoonveiligheid niet verder bewaken.")
                }
            }
        }
        checkTask = task
        if (!handler.postDelayed(task, CHECK_INTERVAL_MS)) {
            disarm()
            return false
        }
        // Recheck after registering: the OS state may have changed between
        // preflight and scheduling, before startListening is invoked.
        if (currentBlocker() != null) {
            disarm()
            return false
        }
        return true
    }

    fun disarm() {
        active = false
        epoch++
        onInterrupted = null
        checkTask?.let(handler::removeCallbacks)
        checkTask = null
    }

    companion object {
        private const val CHECK_INTERVAL_MS = 2_000L
    }
}
