package com.livelingo.android

import android.os.Handler
import android.os.Looper

/**
 * A lifecycle-owned timeout for explicit local Android TTS playback.
 * The on-device speech engine may not return initialization/onDone/onError.
 * Without a bounded response the app can retain audio focus indefinitely.
 *
 * Does not log text, persist audio, download voices or retry through network.
 * All callers close the TextToSpeech engine and audio focus on expiry.
 */
internal class LocalSpeechPlaybackDeadline {
    private val handler = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null

    fun clear() {
        pending?.let(handler::removeCallbacks)
        pending = null
    }

    fun awaitEngine(onTimeout: () -> Unit) = schedule(ENGINE_START_MS, onTimeout)

    /** Replaced on every new spoken line; capped for the bounded 400-char TTS input. */
    fun awaitUtterance(length: Int, pace: LocalSpeechPace, onTimeout: () -> Unit) {
        val perCharacter = if (pace == LocalSpeechPace.SLOW) 250L else 180L
        val duration = (length.coerceIn(1, 400) * perCharacter)
            .coerceIn(MIN_UTTERANCE_MS, MAX_UTTERANCE_MS)
        schedule(duration, onTimeout)
    }

    private fun schedule(durationMs: Long, onTimeout: () -> Unit) {
        clear()
        val task = Runnable {
            pending = null
            onTimeout()
        }
        pending = task
        if (!handler.postDelayed(task, durationMs)) {
            pending = null
            onTimeout() // fail closed rather than leaving focus indefinitely
        }
    }

    companion object {
        private const val ENGINE_START_MS = 15_000L
        private const val MIN_UTTERANCE_MS = 20_000L
        private const val MAX_UTTERANCE_MS = 100_000L
    }
}
