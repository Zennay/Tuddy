package com.livelingo.android

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Speaks a small built-in, locale-aware preview through one exact installed local voice.
 * For unlisted languages it uses numeric syllables, never an invented translation.
 * It never accepts user text, requests voice downloads, or falls back to another voice.
 */
internal class LocalSpeechVoicePreview(context: Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var generation = 0L
    private var timeoutRunnable: Runnable? = null

    fun preview(entry: LocalSpeechVoiceEntry, callback: (Result<Unit>) -> Unit) {
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
                        finish(
                            candidate,
                            requestGeneration,
                            callback,
                            Result.failure(IllegalStateException("Android-spraakdienst niet beschikbaar."))
                        )
                        return@post
                    }

                    try {
                        val voice = candidate.voices.orEmpty().firstOrNull { available ->
                            available.name == entry.voiceName &&
                                available.locale.toLanguageTag() == entry.languageTag &&
                                !available.isNetworkConnectionRequired &&
                                available.features?.contains(
                                    TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED
                                ) != true
                        }
                        if (voice == null) {
                            finish(
                                candidate,
                                requestGeneration,
                                callback,
                                Result.failure(
                                    IllegalStateException(
                                        "Deze lokale stem is niet meer beschikbaar. Er is niets gedownload."
                                    )
                                )
                            )
                            return@post
                        }

                        if (candidate.setVoice(voice) != TextToSpeech.SUCCESS ||
                            candidate.voice?.name != entry.voiceName ||
                            candidate.voice?.locale?.toLanguageTag() != entry.languageTag
                        ) {
                            finish(
                                candidate,
                                requestGeneration,
                                callback,
                                Result.failure(
                                    IllegalStateException(
                                        "Android kon de gekozen lokale stem niet instellen. Er is geen andere stem gebruikt."
                                    )
                                )
                            )
                            return@post
                        }

                        val listener = object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) = Unit

                            override fun onDone(utteranceId: String?) {
                                mainHandler.post {
                                    finish(
                                        candidate,
                                        requestGeneration,
                                        callback,
                                        Result.success(Unit)
                                    )
                                }
                            }

                            override fun onError(utteranceId: String?) {
                                mainHandler.post {
                                    finish(
                                        candidate,
                                        requestGeneration,
                                        callback,
                                        Result.failure(
                                            IllegalStateException(
                                                "De lokale stemproef kon niet worden afgespeeld. Er is niets gedownload."
                                            )
                                        )
                                    )
                                }
                            }

                            override fun onError(utteranceId: String?, errorCode: Int) {
                                mainHandler.post {
                                    finish(
                                        candidate,
                                        requestGeneration,
                                        callback,
                                        Result.failure(
                                            IllegalStateException(
                                                "De lokale stemproef is mislukt (code $errorCode). Er is niets gedownload."
                                            )
                                        )
                                    )
                                }
                            }
                        }
                        candidate.setOnUtteranceProgressListener(listener)

                        val speakStatus = candidate.speak(
                            sampleFor(entry.languageTag),
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "tuddy-local-voice-preview-$requestGeneration"
                        )
                        if (speakStatus != TextToSpeech.SUCCESS) {
                            finish(
                                candidate,
                                requestGeneration,
                                callback,
                                Result.failure(
                                    IllegalStateException(
                                        "De lokale stemproef kon niet starten. Er is niets gedownload."
                                    )
                                )
                            )
                        }
                    } catch (_: RuntimeException) {
                        finish(
                            candidate,
                            requestGeneration,
                            callback,
                            Result.failure(
                                IllegalStateException(
                                    "De lokale stemproef kon niet worden gestart. Er is niets gedownload."
                                )
                            )
                        )
                    }
                }
            })
            engine = candidate
            val timeout = Runnable {
                if (generation == requestGeneration && engine === candidate) {
                    finish(
                        candidate,
                        requestGeneration,
                        callback,
                        Result.failure(IllegalStateException(
                            "De stemproef duurde te lang en is veilig gestopt."
                        ))
                    )
                }
            }
            timeoutRunnable = timeout
            mainHandler.postDelayed(timeout, PREVIEW_TIMEOUT_MS)
        } catch (_: RuntimeException) {
            if (generation == requestGeneration) {
                close()
                callback(
                    Result.failure(
                        IllegalStateException("Android-spraakdienst niet beschikbaar. Er is niets gedownload.")
                    )
                )
            }
        }
    }

    fun close() {
        timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        timeoutRunnable = null
        generation++
        val old = engine
        engine = null
        if (old != null) {
            runCatching { old.stop() }
            runCatching { old.shutdown() }
        }
    }

    private fun finish(
        candidate: TextToSpeech,
        requestGeneration: Long,
        callback: (Result<Unit>) -> Unit,
        result: Result<Unit>
    ) {
        if (generation != requestGeneration || engine !== candidate) {
            runCatching { candidate.shutdown() }
            return
        }
        timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        timeoutRunnable = null
        engine = null
        generation++
        runCatching { candidate.stop() }
        runCatching { candidate.shutdown() }
        callback(result)
    }

    companion object {
        private const val PREVIEW_TIMEOUT_MS = 20_000L

        /** Built-in study-neutral phrase only; no live speech or private notes. */
        internal fun sampleFor(languageTag: String): String = when (
            Locale.forLanguageTag(languageTag).language.lowercase(Locale.ROOT)
        ) {
            "nl" -> "Hallo, dit is een voorbeeld van mijn stem."
            "en" -> "Hello, this is a sample of my voice."
            "de" -> "Hallo, dies ist eine Hörprobe meiner Stimme."
            "fr" -> "Bonjour, voici un exemple de ma voix."
            "es" -> "Hola, esta es una muestra de mi voz."
            "it" -> "Ciao, questo è un esempio della mia voce."
            "pt" -> "Olá, esta é uma amostra da minha voz."
            "sv" -> "Hej, det här är ett röstprov."
            "da" -> "Hej, dette er en stemmeprøve."
            "nb", "no", "nn" -> "Hei, dette er en stemmeprøve."
            "fi" -> "Hei, tämä on ääninäyte."
            "pl" -> "Cześć, to jest próbka mojego głosu."
            "tr" -> "Merhaba, bu benim sesimden bir örnek."
            "ru" -> "Здравствуйте, это образец моего голоса."
            "uk" -> "Вітаю, це зразок мого голосу."
            "ar" -> "مرحبًا، هذا مثال على صوتي."
            "hi" -> "नमस्ते, यह मेरी आवाज़ का नमूना है।"
            "ja" -> "こんにちは、これは音声のサンプルです。"
            "ko" -> "안녕하세요. 이것은 음성 샘플입니다."
            "zh" -> "你好，这是语音示例。"
            "id" -> "Halo, ini contoh suara saya."
            "vi" -> "Xin chào, đây là mẫu giọng nói."
            else -> "1, 2, 3."
        }
    }
}
