package com.livelingo.android

import android.content.Context

/**
 * Only the listening *mode* is kept in app-private preferences.
 * No audio, captions, recognition hypotheses or timestamps are persisted.
 *
 * Continuous remains the existing default; single sentence is explicit and
 * means never schedule an automatic recognizer restart after a terminal event.
 */
internal enum class LocalSpeechCaptureMode {
    CONTINUOUS,
    SINGLE_SENTENCE;

    val label: String get() = when (this) {
        CONTINUOUS -> "Doorlopend"
        SINGLE_SENTENCE -> "Eén zin"
    }
}

internal class LocalSpeechCaptureModePreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "tuddy-private-speech-capture-mode-v1", Context.MODE_PRIVATE
    )

    fun selected(): LocalSpeechCaptureMode = runCatching {
        val raw = preferences.getString("mode", null)
        LocalSpeechCaptureMode.entries.firstOrNull { it.name == raw }
            ?: LocalSpeechCaptureMode.CONTINUOUS
    }.getOrDefault(LocalSpeechCaptureMode.CONTINUOUS)

    fun choose(mode: LocalSpeechCaptureMode): Boolean = runCatching {
        preferences.edit().putString("mode", mode.name).commit()
    }.getOrDefault(false)
}
