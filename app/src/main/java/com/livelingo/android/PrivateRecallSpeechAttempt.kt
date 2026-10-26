package com.livelingo.android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * User-triggered, one-shot on-device answer rehearsal; deliberately separate
 * from Tuddy's continuous caption mic. No online recognizer or model download,
 * no partial text handoff, no persistence, no automated grading.
 * The one-shot input honors live capture's call/mute/permission monitor,
 * optional headset-microphone policy and opt-in strict offline radio gate.
 *
 * Owned by a foreground recall panel. Cancellation invalidates callbacks
 * before destroying the recognizer, including on screen lock/background.
 */
internal class PrivateRecallSpeechAttempt(context: Context) {
    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val lockGate = LocalScreenLockPrivacyGate(app)
    // The short one-shot learner microphone must obey the SAME admission and
    // mid-session safety boundaries as Tuddy's ordinary live microphone.
    private val micGuard = LocalMicrophonePrivacyGuard(app)
    private val headsetGuard = LocalHeadsetMicrophoneGuard(app)
    private val strictRadioGuard = StrictOfflineRadioSessionMonitor(app)
    private var recognizer: SpeechRecognizer? = null
    private var watchdog: Runnable? = null
    private var epoch = 0L

    fun isRecording(): Boolean = recognizer != null

    fun cancel() {
        epoch++
        watchdog?.let(main::removeCallbacks)
        watchdog = null
        micGuard.disarm()
        headsetGuard.disarm()
        strictRadioGuard.disarm()
        lockGate.disarm()
        val old = recognizer
        recognizer = null
        if (old != null) {
            runCatching { old.cancel() }
            runCatching { old.destroy() }
        }
    }

    /** Returns false with a safe notice if Android may not start recording. */
    fun start(
        languageTag: String,
        strictOfflineRadioOnly: Boolean,
        onNotice: (String) -> Unit,
        onFinalAnswer: (String) -> Unit
    ): Boolean {
        cancel()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(app, Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED ||
            !runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(app) }
                .getOrDefault(false)
        ) {
            onNotice("Geen toegestane lokale Android-spraakherkenning. Typ je antwoord zelf.")
            return false
        }
        if (!Regex("^[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})*$").matches(languageTag)) {
            onNotice("Deze antwoordtaal heeft geen geldige Android-spraakcode.")
            return false
        }
        val locale = Locale.forLanguageTag(languageTag)
        if (locale.language.isBlank() || locale.language == "und") {
            onNotice("Deze antwoordtaal is niet geschikt voor lokaal inspreken.")
            return false
        }
        val micBlocker = micGuard.currentBlocker()
        if (micBlocker != null) {
            onNotice("$micBlocker Geen antwoordopname gestart.")
            return false
        }
        val headsetBlocker = headsetGuard.currentBlocker()
        if (headsetBlocker != null) {
            onNotice("$headsetBlocker Geen antwoordopname gestart.")
            return false
        }
        if (strictOfflineRadioOnly &&
            !StrictOfflineRadioSessionMonitor.strictlyOffline(
                OfflineRadioSnapshot.capture(app)
            )) {
            onNotice("Strikte vliegtuigmodus gekozen: Android meldt een " +
                "actieve of onbekende radio of verbinding. Antwoordmicrofoon blijft uit.")
            return false
        }
        if (!lockGate.arm {
                cancel()
                onNotice("Scherm vergrendeld: lokale antwoordopname afgebroken en gewist.")
            }
        ) {
            onNotice("Android is vergrendeld of de schermbewaking ontbreekt. Microfoon blijft uit.")
            return false
        }
        val version = epoch
        val gate = SpeechTurnCompletionGate()
        // One-shot capture never restarts itself. Install each available
        // Android observer BEFORE starting the recognizer, and refuse on
        // unknown status or failed registration. Callbacks are epoch-fenced.
        if (!micGuard.arm { reason ->
                if (version == epoch) {
                    cancel()
                    onNotice("$reason Lokale antwoordopname direct gestopt.")
                }
            }) {
            cancel()
            onNotice("Androids microfoon-/oproepbewaking kon niet veilig starten. " +
                "Geen antwoordopname.")
            return false
        }
        if (!headsetGuard.arm { reason ->
                if (version == epoch) {
                    cancel()
                    onNotice("$reason Lokale antwoordopname direct gestopt.")
                }
            }) {
            cancel()
            onNotice("Een vereiste headsetmicrofoon ontbreekt of de bewaking " +
                "kon niet starten. Geen antwoordopname.")
            return false
        }
        if (strictOfflineRadioOnly && !strictRadioGuard.arm { reason ->
                if (version == epoch) {
                    cancel()
                    onNotice("$reason Strikte offline antwoordopname direct gestopt.")
                }
            }) {
            cancel()
            onNotice("Strikte offline-radiobewaking kon niet starten of " +
                "Android meldt verbinding. Geen antwoordopname.")
            return false
        }
        try {
            val local = SpeechRecognizer.createOnDeviceSpeechRecognizer(app)
            recognizer = local
            local.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    if (version == epoch && gate.isAwaitingResult()) {
                        onNotice("Lokale microfoon actief · spreek één antwoord.")
                    }
                }
                override fun onBeginningOfSpeech() {
                    if (version == epoch && gate.isAwaitingResult()) {
                        onNotice("Antwoord gehoord · Android verwerkt het lokaal.")
                    }
                }
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
                override fun onError(error: Int) {
                    if (version != epoch || !gate.acceptTerminalError()) return
                    cancel()
                    onNotice("Lokaal antwoord niet herkend. Probeer opnieuw of typ zelf. Geen cloudfallback.")
                }
                override fun onResults(results: Bundle?) {
                    if (version != epoch || !gate.acceptFinalResult()) return
                    val candidate = SpeechHypothesisPolicy.select(
                        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    )
                    val safe = LocalTextBoundaryPolicy.speechHypothesis(candidate)
                        ?.takeIf { it.length <= MAX_SPOKEN_CHARACTERS }
                    cancel()
                    if (safe.isNullOrBlank()) {
                        onNotice("Geen bruikbaar lokaal antwoord. Je kunt opnieuw inspreken.")
                    } else {
                        onFinalAnswer(safe)
                        onNotice("Gesproken poging lokaal herkend. Onthul zelf de referentie.")
                    }
                }
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
            val timeout = Runnable {
                if (version == epoch) {
                    cancel()
                    onNotice("Lokale microfoon na 20 seconden gestopt. Geen antwoord opgeslagen.")
                }
            }
            watchdog = timeout
            if (!main.postDelayed(timeout, MAX_CAPTURE_MS)) {
                cancel()
                onNotice("Opnamelimiet kon niet worden bewaakt; microfoon niet gestart.")
                return false
            }
            local.startListening(intent)
            onNotice("Lokale Android-herkenning starten…")
            return true
        } catch (_: RuntimeException) {
            cancel()
            onNotice("Android kon de lokale herkenning niet veilig starten. Geen cloudfallback.")
            return false
        }
    }

    companion object {
        private const val MAX_CAPTURE_MS = 20_000L
        private const val MAX_SPOKEN_CHARACTERS = 400
    }
}
