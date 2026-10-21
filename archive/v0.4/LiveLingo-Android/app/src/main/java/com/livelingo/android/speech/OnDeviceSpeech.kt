package com.livelingo.android.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.livelingo.android.model.Languages

/** Only creates the on-device recognition service. Never falls back to a network recognizer. */
class OnDeviceSpeech(
    private val context: Context,
    private val onPartial: (String) -> Unit,
    private val onFinal: (String) -> Unit,
    private val onDetected: (String?) -> Unit,
    private val onState: (Boolean) -> Unit,
    private val onFailure: (String) -> Unit,
) {
    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var wanted = false
    private var locale = "en-US"
    private var sequence = 0L
    private var restart: Runnable? = null

    val available: Boolean get() = Build.VERSION.SDK_INT >= 31 &&
        SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

    fun start(languageTag: String) {
        stop()
        if (!available) {
            onFailure("No offline speech service found. Install an on-device language pack in Android settings.")
            return
        }
        locale = languageTag
        onDetected(null)
        wanted = true
        val request = ++sequence
        val engine = try {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } catch (error: Exception) {
            wanted = false
            onFailure("Cannot start offline recognition: ${error.localizedMessage}")
            return
        }
        recognizer = engine
        engine.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { if (valid(request)) onState(true) }
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
            override fun onLanguageDetection(results: Bundle) {
                if (Build.VERSION.SDK_INT >= 34 && valid(request)) {
                    onDetected(Languages.normalize(results.getString(SpeechRecognizer.DETECTED_LANGUAGE)))
                    if (results.getInt(SpeechRecognizer.LANGUAGE_SWITCH_RESULT, -1) ==
                        SpeechRecognizer.LANGUAGE_SWITCH_RESULT_SKIPPED_NO_MODEL) {
                        onFailure("Auto language switch needs offline speech packs for the spoken language.")
                    }
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                if (valid(request)) best(partialResults)?.let(onPartial)
            }
            override fun onResults(results: Bundle?) {
                if (!valid(request)) return
                best(results)?.let(onFinal)
                schedule(request, 350)
            }
            override fun onError(error: Int) {
                if (!valid(request)) return
                if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
                    error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
                    error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE) {
                    onFailure("Offline speech model unavailable or permission missing (code $error).")
                    stop()
                    return
                }
                // Silence/no-match/timeouts are normal in continuous caption mode.
                if (error != SpeechRecognizer.ERROR_NO_MATCH &&
                    error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    onFailure("Recognizer restarted after error $error.")
                }
                schedule(request, 850)
            }
        })
        onState(true)
        launch(request)
    }

    private fun best(bundle: Bundle?): String? = bundle
        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        ?.firstOrNull()?.trim()?.takeIf(String::isNotBlank)

    private fun valid(token: Long): Boolean = wanted && token == sequence

    private fun launch(token: Long) {
        if (!valid(token)) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            if (Build.VERSION.SDK_INT >= 34) {
                val tags = ArrayList(Languages.voiceHints)
                putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION, true)
                putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_SWITCH, RecognizerIntent.LANGUAGE_SWITCH_BALANCED)
                putStringArrayListExtra(RecognizerIntent.EXTRA_LANGUAGE_SWITCH_ALLOWED_LANGUAGES, tags)
            }
        }
        try {
            recognizer?.startListening(intent)
        } catch (error: Exception) {
            onFailure("Unable to start microphone: ${error.localizedMessage}")
            stop()
        }
    }

    private fun schedule(token: Long, delay: Long) {
        restart?.let(main::removeCallbacks)
        restart = Runnable { if (valid(token)) launch(token) }
        main.postDelayed(restart!!, delay)
    }

    fun stop() {
        wanted = false
        sequence++
        restart?.let(main::removeCallbacks)
        restart = null
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
        onState(false)
    }

    fun close() = stop()
}
