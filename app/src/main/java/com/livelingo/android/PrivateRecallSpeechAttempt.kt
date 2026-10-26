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
 * optional headset-microphone and thermal/battery policies and opt-in strict
 * offline radio gate.
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
    private val powerGuard = LocalSpeechPowerGuard(app)
    private val strictRadioGuard = StrictOfflineRadioSessionMonitor(app)
    // Read-only exact-language pack check is separate from the mic service.
    // Android 12 lacks per-language inventory APIs; never claim it is READY.
    private val speechPackCheck = OnDeviceSpeechPackController(app)
    private var checkingSpeechPack = false
    private var packTimeout: Runnable? = null
    private var recognizer: SpeechRecognizer? = null
    private var watchdog: Runnable? = null
    private var epoch = 0L

    /** Includes preflight so the UI can show Stop while Android checks a pack. */
    fun isRecording(): Boolean = recognizer != null || checkingSpeechPack

    fun cancel() {
        epoch++
        checkingSpeechPack = false
        speechPackCheck.cancel()
        packTimeout?.let(main::removeCallbacks)
        packTimeout = null
        watchdog?.let(main::removeCallbacks)
        watchdog = null
        micGuard.disarm()
        headsetGuard.disarm()
        powerGuard.disarm()
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
        // Never start a microphone from an unverified *known absent* pack.
        // Android 13+ can report exact installed/downloadable/online-only
        // speech languages; this is a READ-ONLY probe, never a download.
        if (Build.VERSION.SDK_INT >= 33) {
            val version = epoch
            checkingSpeechPack = true
            val timeout = Runnable {
                if (version != epoch) return@Runnable
                cancel()
                onNotice("Android reageerde niet op de lokale spraakpakketcontrole. " +
                    "Geen antwoordmicrofoon gestart; probeer later of typ zelf.")
            }
            packTimeout = timeout
            if (!main.postDelayed(timeout, PACK_CHECK_LIMIT_MS)) {
                cancel()
                onNotice("De lokale spraakpakketcontrole kon niet veilig worden bewaakt. " +
                    "Geen antwoordmicrofoon gestart.")
                return false
            }
            speechPackCheck.check(languageTag) { receipt ->
                if (version != epoch) return@check
                when (receipt.stage) {
                    SpeechPackStage.CHECKING -> onNotice(
                        "Android controleert het geïnstalleerde offline pakket voor $languageTag…")
                    SpeechPackStage.READY, SpeechPackStage.UNKNOWN -> {
                        packTimeout?.let(main::removeCallbacks)
                        packTimeout = null
                        checkingSpeechPack = false
                        // UNKNOWN never means confirmed installed. Only the
                        // actual on-device SpeechRecognizer may be attempted.
                        val unknown = receipt.stage == SpeechPackStage.UNKNOWN
                        val started = beginCapture(
                            languageTag, strictOfflineRadioOnly, version,
                            onNotice, onFinalAnswer
                        )
                        if (started && unknown) {
                            onNotice("Pakketstatus voor $languageTag onbekend; " +
                                "uitsluitend de on-device herkenner is gestart. " +
                                "Android kan deze taal alsnog weigeren. Geen cloudfallback.")
                        }
                    }
                    else -> {
                        cancel()
                        onNotice("Geen geïnstalleerd lokaal spraakpakket voor $languageTag " +
                            "bevestigd (${receipt.stage}). Geen opname of download gestart. " +
                            "Controleer het spraakpakket in Offline startcheck " +
                            "of typ je oefenantwoord zelf.")
                    }
                }
            }
            return isRecording()
        }
        // Android 12 cannot enumerate installed speech languages. Preserve
        // the prior on-device-only path without claiming per-language support.
        return beginCapture(languageTag, strictOfflineRadioOnly, epoch,
            onNotice, onFinalAnswer)
    }

    private fun beginCapture(
        languageTag: String,
        strictOfflineRadioOnly: Boolean,
        version: Long,
        onNotice: (String) -> Unit,
        onFinalAnswer: (String) -> Unit
    ): Boolean {
        if (version != epoch ||
            ContextCompat.checkSelfPermission(app, Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED ||
            !runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(app) }
                .getOrDefault(false)
        ) {
            cancel()
            onNotice("De toestel- of microfoontoestemming is gewijzigd tijdens " +
                "de pakketcontrole. Geen antwoordopname gestart.")
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
        if (powerGuard.enabled()) {
            val powerBlocker = powerGuard.currentBlocker()
            if (powerBlocker != null) {
                onNotice("$powerBlocker Geen antwoordopname gestart.")
                return false
            }
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
        if (!powerGuard.arm { reason ->
                if (version == epoch) {
                    cancel()
                    onNotice("$reason Lokale antwoordopname direct gestopt.")
                }
            }) {
            cancel()
            onNotice("Toestelbescherming voor accu/temperatuur kon niet " +
                "worden gestart. Geen antwoordopname.")
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
        private const val PACK_CHECK_LIMIT_MS = 12_000L
        private const val MAX_CAPTURE_MS = 20_000L
        private const val MAX_SPOKEN_CHARACTERS = 400
    }
}
