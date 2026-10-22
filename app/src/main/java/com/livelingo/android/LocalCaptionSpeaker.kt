package com.livelingo.android

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Explicit user-triggered pronunciation of a completed local caption.
 *
 * Only voices which the Android TTS engine itself marks as non-network and
 * already installed are admitted. Never asks for a voice/model download or
 * permits the Android engine to fall back to a network-required voice.
 *
 * The engine is destroyed on Stop/background, with stale init callbacks
 * fenced by a monotonic generation. No text, utterance or voice choice is
 * retained by this app between calls.
 */
internal enum class LocalSpeechPace(val rate: Float) {
    NORMAL(1.0f), SLOW(0.72f)
}

internal class LocalCaptionSpeaker(context: Context) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var generation = 0L

    fun stop() {
        generation++
        val old = engine
        engine = null
        if (old != null) {
            runCatching { old.stop() }
            runCatching { old.shutdown() }
        }
    }

    /**
     * Reads a bounded subtitle sequence through an already-installed local voice.
     * Each cue starts only after Android reports the previous utterance done.
     * The caller owns progress UI and must call stop() on user stop/background.
     */
    fun speakSequence(
        lines: List<String>,
        languageTag: String,
        pace: LocalSpeechPace,
        onProgress: (completed: Int, total: Int) -> Unit,
        feedback: (String) -> Unit,
        onFinished: (completed: Boolean) -> Unit
    ) {
        stop()
        val safeLines = lines.map { it.replace('\\n', ' ').replace('\\r', ' ').trim() }
        if (safeLines.isEmpty() || safeLines.size > MAX_SEQUENCE_LINES ||
            safeLines.any { line ->
                line.isBlank() || line.length > MAX_LINE_CHARS ||
                    line.any { ch -> ch == '\\u0000' || ch.isISOControl() }
            }
        ) {
            feedback("Voorlezen vereist 1–100 niet-lege regels van maximaal 400 tekens. Geen online fallback.")
            onFinished(false)
            return
        }
        if (!Regex("^[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})*$").matches(languageTag)) {
            feedback("Voor deze taal is geen gecontroleerde offline stem bekend.")
            onFinished(false)
            return
        }
        val locale = Locale.forLanguageTag(languageTag)
        if (locale.language.isBlank() || locale.language == "und") {
            feedback("Voor deze taal is geen gecontroleerde offline stem bekend.")
            onFinished(false)
            return
        }

        val version = generation
        var sequenceIndex = 0
        feedback("Geïnstalleerde lokale Android-stem controleren…")

        fun fail(message: String) {
            if (generation != version) return
            stop()
            feedback(message)
            onFinished(false)
        }

        fun speakAt(tts: TextToSpeech, index: Int) {
            if (generation != version) return
            sequenceIndex = index
            val result = runCatching {
                tts.speak(
                    safeLines[index],
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "tuddy-offline-sequence-$version-$index"
                )
            }.getOrDefault(TextToSpeech.ERROR)
            if (result != TextToSpeech.SUCCESS) {
                fail("Lokale voorleesregel ${index + 1} kon niet starten. Geen online fallback.")
            }
        }

        try {
            engine = TextToSpeech(appContext, TextToSpeech.OnInitListener { result ->
                main.post {
                    if (generation != version) return@post
                    val tts = engine
                    if (result != TextToSpeech.SUCCESS || tts == null) {
                        fail("Geen bruikbare lokale Android-spraakstem. Er is niets online afgespeeld.")
                        return@post
                    }
                    try {
                        val chosen = tts.voices.orEmpty()
                            .filter { voice ->
                                !voice.isNetworkConnectionRequired &&
                                    voice.locale.language == locale.language &&
                                    voice.features?.contains(
                                        TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED
                                    ) != true
                            }
                            .sortedWith(
                                compareByDescending<android.speech.tts.Voice> {
                                    it.locale.country.equals(locale.country, ignoreCase = true)
                                }.thenByDescending { it.quality }
                            )
                            .firstOrNull()
                        if (chosen == null ||
                            tts.setVoice(chosen) != TextToSpeech.SUCCESS ||
                            tts.setSpeechRate(pace.rate) != TextToSpeech.SUCCESS
                        ) {
                            fail("Geen geïnstalleerde lokale stem voor deze taal. Geen online fallback.")
                            return@post
                        }
                        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) = Unit

                            override fun onDone(utteranceId: String?) {
                                val finishedIndex = utteranceId
                                    ?.substringAfterLast('-')
                                    ?.toIntOrNull() ?: return
                                main.post {
                                    if (generation != version || finishedIndex != sequenceIndex) {
                                        return@post
                                    }
                                    onProgress(finishedIndex + 1, safeLines.size)
                                    if (finishedIndex + 1 >= safeLines.size) {
                                        stop()
                                        feedback("Voorlezen voltooid: ${safeLines.size} regels lokaal afgespeeld.")
                                        onFinished(true)
                                    } else {
                                        speakAt(tts, finishedIndex + 1)
                                    }
                                }
                            }

                            override fun onError(utteranceId: String?) {
                                main.post {
                                    if (generation == version) {
                                        fail("Lokale spraakfout; de resterende regels zijn gestopt. Geen online fallback.")
                                    }
                                }
                            }
                        })
                        speakAt(tts, 0)
                    } catch (_: RuntimeException) {
                        fail("Voorlezen niet mogelijk met de geïnstalleerde offline stemmen.")
                    }
                }
            })
        } catch (_: RuntimeException) {
            fail("De lokale Android-voorleesdienst kan niet worden gestart.")
        }
    }

    fun close() = stop()

    fun speak(translated: String, languageTag: String, feedback: (String) -> Unit) {
        speak(translated, languageTag, LocalSpeechPace.NORMAL, feedback)
    }

    fun speak(
        translated: String,
        languageTag: String,
        pace: LocalSpeechPace,
        feedback: (String) -> Unit
    ) {
        stop()
        // An on-device translator may produce huge or control-filled results.
        // Admit short printable completed lines only; do not retry over a
        // network speech engine or publish provider errors.
        val safe = translated.trim()
        if (safe.isEmpty() || safe.length > 400 ||
            safe.any { it == '\u0000' || it == '\r' || it == '\n' || it.isISOControl() } ||
            !Regex("^[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})*$").matches(languageTag)) {
            feedback("Deze vertaling kan niet veilig worden voorgelezen.")
            return
        }
        val locale = Locale.forLanguageTag(languageTag)
        if (locale.language.isBlank() || locale.language == "und") {
            feedback("Voor deze taal is geen gecontroleerde offline stem bekend.")
            return
        }
        val version = generation
        feedback("Geïnstalleerde lokale Android-stem controleren…")
        try {
            engine = TextToSpeech(appContext, TextToSpeech.OnInitListener { result ->
                main.post {
                    if (generation != version) return@post
                    val tts = engine
                    if (result != TextToSpeech.SUCCESS || tts == null) {
                        stop()
                        feedback("Geen bruikbare lokale Android-spraakstem. Er is niets online afgespeeld.")
                        return@post
                    }
                    try {
                        val available = tts.voices.orEmpty()
                            .filter { voice ->
                                !voice.isNetworkConnectionRequired &&
                                voice.locale.language == locale.language &&
                                voice.features?.contains(
                                    TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) != true
                            }
                        val chosen = available.sortedWith(
                            compareByDescending<android.speech.tts.Voice> {
                                it.locale.country.equals(locale.country, ignoreCase = true)
                            }.thenByDescending { it.quality }
                        ).firstOrNull()
                        if (chosen == null ||
                            tts.setVoice(chosen) != TextToSpeech.SUCCESS ||
                            tts.setSpeechRate(pace.rate) != TextToSpeech.SUCCESS ||
                            tts.speak(safe, TextToSpeech.QUEUE_FLUSH, null,
                                "tuddy-offline-caption-$version") != TextToSpeech.SUCCESS) {
                            stop()
                            feedback("Geen geïnstalleerde lokale stem voor deze taal. Geen online fallback.")
                        } else {
                            feedback(
                                if (pace == LocalSpeechPace.SLOW)
                                    "Rustig tempo met lokaal gemelde Android-stem. Uitspraak kan onnauwkeurig zijn."
                                else "Android gebruikt een als lokaal gemelde stem. Uitspraak kan onnauwkeurig zijn."
                            )
                        }
                    } catch (_: RuntimeException) {
                        stop()
                        feedback("Voorlezen niet mogelijk met de geïnstalleerde offline stemmen.")
                    }
                }
            })
        } catch (_: RuntimeException) {
            stop()
            feedback("De lokale Android-voorleesdienst kan niet worden gestart.")
        }
    }
    private companion object {
        const val MAX_SEQUENCE_LINES = 100
        const val MAX_LINE_CHARS = 400
    }

}
