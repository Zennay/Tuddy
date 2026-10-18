package com.livelingo.android

import android.content.Context
import android.os.Handler
import android.os.Looper

/**
 * Optional per-session deadline for on-device speech capture.
 * The only retained preference is the chosen number of minutes;
 * never keep audio, session start time, captions or activity history.
 *
 * This class is called from the Android main thread and its pending
 * Runnable is cancelled on Stop / background / disposal.
 */
internal class MicrophoneSessionLimit(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "tuddy-private-microphone-limit-v1", Context.MODE_PRIVATE
    )
    private val handler = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null
    private var epoch = 0L

    fun selectedMinutes(): Int =
        runCatching { preferences.getInt("session-limit-minutes", 0) }
            .getOrDefault(0).takeIf { it in CHOICES } ?: 0

    fun changeMinutes(minutes: Int): Boolean =
        minutes in CHOICES && runCatching {
            preferences.edit().putInt("session-limit-minutes", minutes).commit()
        }.getOrDefault(false)

    fun cancel() {
        epoch++
        pending?.let(handler::removeCallbacks)
        pending = null
    }

    /**
     * Call only on first user Start or explicit in-session setting change.
     * Automatic SpeechRecognizer restarts must NOT extend this deadline.
     * A scheduling failure is reported so the caller can stop capture.
     */
    fun arm(onExpired: () -> Unit): Boolean {
        cancel()
        val minutes = selectedMinutes()
        if (minutes == 0) return true
        val thisEpoch = epoch
        val callback = Runnable {
            if (thisEpoch != epoch) return@Runnable
            pending = null
            onExpired()
        }
        pending = callback
        val accepted = handler.postDelayed(callback, minutes * 60_000L)
        if (!accepted) {
            cancel()
            return false
        }
        return true
    }

    companion object {
        val CHOICES = setOf(0, 5, 15, 30)
    }
}
