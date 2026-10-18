package com.livelingo.android

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
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

    fun close() = stop()

    fun speak(translated: String, languageTag: String, feedback: (String) -> Unit) {
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
                            tts.speak(safe, TextToSpeech.QUEUE_FLUSH, null,
                                "tuddy-offline-caption-$version") != TextToSpeech.SUCCESS) {
                            stop()
                            feedback("Geen geïnstalleerde lokale stem voor deze taal. Geen online fallback.")
                        } else {
                            feedback("Android gebruikt een als lokaal gemelde stem. De uitspraak kan onnauwkeurig zijn.")
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
}
