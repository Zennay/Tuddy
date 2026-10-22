package com.livelingo.android

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener

/**
 * Speaks one fixed, user-requested preview through an exact installed local voice.
 * It never accepts user text, requests voice downloads, or falls back to another voice.
 */
internal class LocalSpeechVoicePreview(context: Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var generation = 0L

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

                        if (candidate.setVoice(voice) != TextToSpeech.SUCCESS) {
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
                            PREVIEW_TEXT,
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
        engine = null
        generation++
        runCatching { candidate.stop() }
        runCatching { candidate.shutdown() }
        callback(result)
    }

    private companion object {
        const val PREVIEW_TEXT = "Tuddy voice preview."
    }
}
