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
    private var pendingTimeout: Runnable? = null

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
                                    ) != true &&
                                    voice.name.isNotBlank() && voice.name.length <= 256 &&
                                    voice.name.none { it.isISOControl() }
                            }
                            .mapNotNull { voice ->
                                val locale = voice.locale
                                val tag = locale.toLanguageTag()
                                if (locale.language.isBlank() || locale.language == "und" ||
                                    !Regex("^[a-zA-Z]{2,3}(-[a-zA-Z0-9]{2,8})*$").matches(tag)
                                ) {
                                    null
                                } else {
                                    LocalSpeechVoiceEntry(
                                        voiceName = voice.name,
                                        languageTag = tag,
                                        displayName = locale.getDisplayName(Locale.getDefault())
                                            .take(100).filterNot { it.isISOControl() },
                                        quality = voice.quality
                                    )
                                }
                            }
                            .distinctBy { it.voiceName to it.languageTag }
                            .take(MAX_INSTALLED_VOICES)
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
            // Some vendor TextToSpeech implementations never deliver onInit.
            // End the read-only inspection and return control to Refresh.
            val timeout = Runnable {
                if (generation == requestGeneration && engine === candidate) {
                    close()
                    callback(Result.failure(IllegalStateException(
                        "Android reageerde niet bij het uitlezen van geïnstalleerde stemmen. " +
                            "Tik bewust op Ververs om het opnieuw te proberen."
                    )))
                }
            }
            pendingTimeout = timeout
            if (!mainHandler.postDelayed(timeout, INSPECTION_TIMEOUT_MS)) {
                close()
                callback(Result.failure(IllegalStateException(
                    "Android kon de stemcontrole niet veilig plannen."
                )))
            }
        } catch (_: RuntimeException) {
            if (generation == requestGeneration) {
                close()
                callback(Result.failure(
                    IllegalStateException("Android-spraakdienst niet beschikbaar.")
                ))
            }
        }
    }

    private companion object {
        const val INSPECTION_TIMEOUT_MS = 15_000L
        const val MAX_INSTALLED_VOICES = 128
    }

    fun close() {
        generation++
        pendingTimeout?.let(mainHandler::removeCallbacks)
        pendingTimeout = null
        val old = engine
        engine = null
        if (old != null) runCatching { old.shutdown() }
    }
}
