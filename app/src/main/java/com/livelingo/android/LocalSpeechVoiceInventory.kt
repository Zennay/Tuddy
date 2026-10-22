package com.livelingo.android

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import java.util.Locale

/** A single language/locale represented by an already-installed, local TTS voice. */
internal data class LocalSpeechVoiceEntry(
    val voiceName: String,
    val languageTag: String,
    val displayName: String,
    val quality: Int
)

/**
 * Read-only inventory of installed, non-network Android TTS voices.
 *
 * It never speaks, changes the selected voice, or requests a voice download.
 * Every inspection owns a short-lived engine and stale callbacks are discarded.
 */
internal class LocalSpeechVoiceInventory(context: Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var generation = 0L

    fun inspect(callback: (Result<List<LocalSpeechVoiceEntry>>) -> Unit) {
        close()
        val requestGeneration = generation
        lateinit var candidate: TextToSpeech

        try {
            candidate = TextToSpeech(appContext, TextToSpeech.OnInitListener { status ->
                mainHandler.post {
                    if (generation != requestGeneration || engine !== candidate) {
                        runCatching { candidate.shutdown() }
                        return@post
                    }
                    if (status != TextToSpeech.SUCCESS) {
                        close()
                        callback(Result.failure(
                            IllegalStateException("Android-spraakdienst niet beschikbaar.")
                        ))
                        return@post
                    }

                    val entries: List<LocalSpeechVoiceEntry>
                    try {
                        entries = candidate.voices.orEmpty()
                            .asSequence()
                            .filter { voice ->
                                !voice.isNetworkConnectionRequired &&
                                    voice.features?.contains(
                                        TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED
                                    ) != true
                            }
                            .mapNotNull { voice ->
                                val locale = voice.locale
                                val tag = locale.toLanguageTag()
                                if (locale.language.isBlank() || locale.language == "und" ||
                                    tag.isBlank()
                                ) {
                                    null
                                } else {
                                    LocalSpeechVoiceEntry(
                                        voiceName = voice.name,
                                        languageTag = tag,
                                        displayName = locale.getDisplayName(Locale.getDefault()),
                                        quality = voice.quality
                                    )
                                }
                            }
                            .distinctBy { it.voiceName }
                            .sortedWith(
                                compareBy<LocalSpeechVoiceEntry> {
                                    it.displayName.lowercase(Locale.ROOT)
                                }.thenByDescending { it.quality }
                                    .thenBy { it.voiceName.lowercase(Locale.ROOT) }
                            )
                            .toList()
                    } catch (_: RuntimeException) {
                        close()
                        callback(Result.failure(
                            IllegalStateException("De lokale stemlijst kon niet worden gelezen.")
                        ))
                        return@post
                    }

                    close()
                    callback(Result.success(entries))
                }
            })
            engine = candidate
        } catch (_: RuntimeException) {
            if (generation == requestGeneration) {
                close()
                callback(Result.failure(
                    IllegalStateException("Android-spraakdienst niet beschikbaar.")
                ))
            }
        }
    }

    fun close() {
        generation++
        val old = engine
        engine = null
        if (old != null) runCatching { old.shutdown() }
    }
}
