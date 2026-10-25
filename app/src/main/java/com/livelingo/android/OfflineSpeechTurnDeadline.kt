package com.livelingo.android

import android.os.Handler
import android.os.Looper

/**
 * Bound a single Android on-device recognizer turn, even if the vendor never
 * sends onResults() or onError(). The owning Activity manages the microphone,
 * generation fencing, retries and explicit Stop; this class stores no audio,
 * transcript, language or timestamps outside the current in-memory session.
 */
internal class OfflineSpeechTurnDeadline {
    private val handler = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null
    private var revision = 0L

    fun cancel() {
        revision++
        pending?.let(handler::removeCallbacks)
        pending = null
    }

    /**
     * Must be armed before SpeechRecognizer.startListening(), so even fast
     * vendor callbacks can cancel this turn before any deadline exists.
     */
    fun arm(onExpired: () -> Unit): Boolean {
        cancel()
        val current = revision
        val task = Runnable {
            if (current != revision) return@Runnable
            pending = null
            revision++
            onExpired()
        }
        pending = task
        if (!handler.postDelayed(task, MAX_TURN_MS)) {
            cancel()
            return false
        }
        return true
    }

    companion object {
        // Longer than a typical utterance while still bounding a hung service.
        const val MAX_TURN_MS = 60_000L
        const val MAX_AUTOMATIC_STALL_RETRIES = 2
    }
}
