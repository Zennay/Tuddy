package com.livelingo.android

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.launch
import com.livelingo.android.model.SafeCommunityPhrasebook
import com.livelingo.android.ui.OfflineWordText

private data class LanguageChoice(val label: String, val speechCode: String, val translateCode: String)
private val spoken = listOf(
    LanguageChoice("Nederlands", "nl-NL", TranslateLanguage.DUTCH),
    LanguageChoice("English", "en-US", TranslateLanguage.ENGLISH),
    LanguageChoice("Deutsch", "de-DE", TranslateLanguage.GERMAN),
    LanguageChoice("Français", "fr-FR", TranslateLanguage.FRENCH),
    LanguageChoice("Español", "es-ES", TranslateLanguage.SPANISH),
    LanguageChoice("العربية", "ar-SA", TranslateLanguage.ARABIC)
)
private data class Line(
    val id: Long,
    val original: String,
    val translated: String,
    val status: String,
    val sourceCode: String,
    val targetCode: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TuddyScreen() }
    }
}

@Composable
private fun TuddyScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colors = darkColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFFAB9BFF),
        background = androidx.compose.ui.graphics.Color(0xFF10111C),
        surface = androidx.compose.ui.graphics.Color(0xFF202238)
    )
    val lines = remember { mutableStateListOf<Line>() }
    var partial by remember { mutableStateOf("") }
    var partialTranslation by remember { mutableStateOf("") }
    var partialRevision by remember { mutableIntStateOf(0) }
    var interimTranslationBusy by remember { mutableStateOf(false) }
    var pendingInterim by remember { mutableStateOf<Runnable?>(null) }
    val interimHandler = remember { Handler(Looper.getMainLooper()) }
    var error by remember { mutableStateOf("") }
    // Only the picker codes are private on-disk preferences; all captions and
    // speech/model status remain ephemeral in RAM and never restored.
    val languageSettings = remember(context) {
        context.getSharedPreferences(PrivateLanguageSelectionPolicy.SETTINGS_NAME, Context.MODE_PRIVATE)
    }
    var source by remember {
        val storedTag = runCatching {
            languageSettings.getString(PrivateLanguageSelectionPolicy.SOURCE_KEY, null)
        }.getOrNull()
        val safeTag = PrivateLanguageSelectionPolicy.restoreSource(
            storedTag, spoken.map { it.speechCode }, spoken[0].speechCode)
        mutableStateOf(spoken.first { it.speechCode == safeTag })
    }
    var target by remember {
        val storedCode = runCatching {
            languageSettings.getString(PrivateLanguageSelectionPolicy.TARGET_KEY, null)
        }.getOrNull()
        val safeCode = PrivateLanguageSelectionPolicy.restoreTarget(
            storedCode, TargetCatalog.codes, spoken[1].translateCode)
        val catalog = TargetCatalog.targets.first { it.code == safeCode }
        mutableStateOf(LanguageChoice(catalog.title, catalog.code, catalog.code))
    }
    var sourceOpen by remember { mutableStateOf(false) }
    var targetOpen by remember { mutableStateOf(false) }
    var targetQuery by remember { mutableStateOf("") }
    // A deliberately separate offline phrase preview: never passed to ML Kit or ASR.
    var previewTarget by remember { mutableStateOf("kmr") }
    var previewSource by remember { mutableStateOf("nl") }
    var previewMenuOpen by remember { mutableStateOf(false) }
    var previewInput by remember { mutableStateOf("") }
    var manualText by remember { mutableStateOf("") }
    var previewResult by remember { mutableStateOf<String?>(null) }
    var listening by remember { mutableStateOf(false) }
    var preparingOfflineModel by remember { mutableStateOf(false) }
    var offlineModelStatus by remember { mutableStateOf("") }
    val offlinePreparationEpoch = remember { OfflinePreparationEpoch() }
    val textInventory = remember { OfflineTextModelInventory() }
    var textReceipt by remember {
        mutableStateOf(OfflineTextReceipt(spoken[0].translateCode, spoken[1].translateCode,
            OfflineTextStage.UNCHECKED, "Controleer welke tekstmodellen lokaal aanwezig zijn."))
    }
    // Device-native ASR pack coverage is independent of the ML Kit text
    // translation model and of the five-entry community word preview.
    val speechPacks = remember(context) { OnDeviceSpeechPackController(context) }
    var speechPackReceipt by remember {
        mutableStateOf(SpeechPackReceipt(spoken[0].speechCode, SpeechPackStage.UNCHECKED,
            "Controleer afzonderlijk of Android het gekozen offline spraakpakket bezit."))
    }
    var follow by remember { mutableStateOf(true) }
    var generation by remember { mutableIntStateOf(0) }
    val voiceSessions = remember { VoiceSessionGeneration() }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    val restartHandler = remember { Handler(Looper.getMainLooper()) }
    var pendingRestart by remember { mutableStateOf<Runnable?>(null) }
    val offlineAvailable = remember { SpeechRecognizer.isOnDeviceRecognitionAvailable(context) }
    val speechSwitchSupported = Build.VERSION.SDK_INT >= 34 && offlineAvailable
    var automaticSpeechSource by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    fun updateLine(id: Long, translation: String, status: String, token: Int) {
        if (token != generation) return
        val index = lines.indexOfFirst { it.id == id }
        if (index >= 0) lines[index] = lines[index].copy(translated = translation, status = status)
    }

    fun translationStillRelevant(id: Long, token: Int): Boolean =
        TranslationWorkPolicy.mayContinue(token, generation, lines.any { it.id == id })

    fun translate(text: String, id: Long, targetCode: String, sourceCode: String, token: Int) {
        if (!translationStillRelevant(id, token)) return
        // Text language detection and translation remain ON DEVICE. Models are downloaded only
        // when the user is on Wi-Fi; without a downloaded model, fail transparently.
        val detector = LanguageIdentification.getClient()
        detector.identifyLanguage(text)
            .addOnSuccessListener { guess ->
                detector.close()
                // Clear/remove may occur while language detection is pending.
                // Do not begin a *new* Wi-Fi model download for abandoned text.
                if (!translationStillRelevant(id, token)) return@addOnSuccessListener
                val identified = guess.takeIf { it != "und" && TranslateLanguage.fromLanguageTag(it) != null }
                val from = identified?.let { TranslateLanguage.fromLanguageTag(it) } ?: sourceCode
                // Each historical caption keeps its recognized original language.
                // Never relabel older lines when a different source is selected.
                val row = lines.indexOfFirst { it.id == id }
                if (row >= 0) lines[row] = lines[row].copy(sourceCode = from)
                if (from == targetCode) {
                    updateLine(id, text, "Lokaal", token)
                    return@addOnSuccessListener
                }
                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(from)
                    .setTargetLanguage(targetCode)
                    .build()
                val translator = Translation.getClient(options)
                val conditions = DownloadConditions.Builder().requireWifi().build()
                translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener {
                        // A model download already underway may finish after
                        // the conversation was cleared. Close without launching
                        // an unnecessary translation for now-absent text.
                        if (!translationStillRelevant(id, token)) {
                            translator.close()
                            return@addOnSuccessListener
                        }
                        translator.translate(text)
                            .addOnSuccessListener { output ->
                                updateLine(id, output, "Lokaal vertaald", token)
                                translator.close()
                            }
                            .addOnFailureListener {
                                updateLine(id, SafeLocalFailureText.TRANSLATION, "Fout", token)
                                translator.close()
                            }
                    }
                    .addOnFailureListener {
                        updateLine(id, "Taalmodel ontbreekt — verbind met wifi en probeer opnieuw.", "Model ontbreekt", token)
                        translator.close()
                    }
            }
            .addOnFailureListener {
                detector.close()
                updateLine(id, "Taaldetectie mislukt.", "Fout", token)
            }
    }

    // Explicit offline text preparation: one supported source->target model,
    // never a hidden bulk download or an unreviewed speech recognition model.
    // Existing ML Kit model downloads cannot be stopped by clearing text;
    // stale UI completions are suppressed and the client is always closed.
    // Local read-only inventory. A green speech model is not evidence
    // that both ML Kit TEXT language models are present, or vice versa.
    fun checkTextPair() {
        val from = source.translateCode
        val to = target.translateCode
        textInventory.check(from, to, TargetCatalog.codes) { receipt ->
            if (source.translateCode == receipt.source &&
                target.translateCode == receipt.target &&
                lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                textReceipt = receipt
            }
        }
    }

    fun prepareOfflinePair() {
        if (preparingOfflineModel) return
        when (OfflineModelPreparationPolicy.decide(
            source.translateCode, target.translateCode, TargetCatalog.codes)) {
            OfflinePairDecision.UNSUPPORTED -> {
                offlineModelStatus = "Taalpaar niet ondersteund door lokale tekstvertaling."
                return
            }
            OfflinePairDecision.ALREADY_SAME_LANGUAGE -> {
                offlineModelStatus = "De talen zijn gelijk; geen vertaalmodel nodig."
                return
            }
            OfflinePairDecision.MODEL_REQUIRED -> Unit
        }
        val requestEpoch = offlinePreparationEpoch.begin()
        val sourceCode = source.translateCode
        val targetCode = target.translateCode
        preparingOfflineModel = true
        offlineModelStatus = "Tekstmodel via wifi voorbereiden…"
        val translator = try {
            Translation.getClient(TranslatorOptions.Builder()
                .setSourceLanguage(sourceCode)
                .setTargetLanguage(targetCode)
                .build())
        } catch (_: Exception) {
            preparingOfflineModel = false
            offlineModelStatus = "Tekstmodel kon niet worden geopend."
            return
        }
        try {
            translator.downloadModelIfNeeded(DownloadConditions.Builder().requireWifi().build())
                .addOnSuccessListener {
                    translator.close()
                    preparingOfflineModel = false
                    if (offlinePreparationEpoch.isCurrent(requestEpoch)) {
                        offlineModelStatus = "Download afgerond; aanwezigheid opnieuw controleren…"
                        checkTextPair() // Only installed models can justify a READY label.
                    }
                }
                .addOnFailureListener {
                    translator.close()
                    preparingOfflineModel = false
                    if (offlinePreparationEpoch.isCurrent(requestEpoch)) {
                        offlineModelStatus = "Tekstmodel niet beschikbaar. Controleer wifi en opslag; probeer opnieuw."
                    }
                }
        } catch (_: Exception) {
            translator.close()
            preparingOfflineModel = false
            if (offlinePreparationEpoch.isCurrent(requestEpoch)) {
                offlineModelStatus = "Tekstmodel kon niet worden voorbereid."
            }
        }
    }

    // A single quiet-window translation may run only after 650ms of stable
    // interim speech. It calls translate() on an *already installed* ML Kit
    // model and never starts a Wi-Fi download for provisional words.
    fun clearInterim() {
        partial = ""
        partialTranslation = ""
        partialRevision++
        pendingInterim?.let(interimHandler::removeCallbacks)
        pendingInterim = null
    }

    fun prepareInterim(text: String, sourceCode: String, targetCode: String,
                       voiceToken: Long, token: Int) {
        pendingInterim?.let(interimHandler::removeCallbacks)
        pendingInterim = null
        partialTranslation = ""
        val revision = ++partialRevision
        if (!InterimTranslationPolicy.eligible(text, sourceCode, targetCode, listening)) return
        val task = Runnable {
            pendingInterim = null
            val stillCurrent = InterimTranslationPolicy.mayPublish(
                revision, partialRevision, token, generation,
                voiceSessions.isCurrent(voiceToken), listening)
            if (!stillCurrent || interimTranslationBusy) return@Runnable
            if (sourceCode == targetCode) {
                partialTranslation = text
                return@Runnable
            }
            val translator = try {
                Translation.getClient(TranslatorOptions.Builder()
                    .setSourceLanguage(sourceCode)
                    .setTargetLanguage(targetCode)
                    .build())
            } catch (_: Exception) {
                return@Runnable
            }
            interimTranslationBusy = true
            try {
                // Do not call downloadModelIfNeeded: no per-word model traffic.
                translator.translate(text)
                    .addOnSuccessListener { translated ->
                        if (InterimTranslationPolicy.mayPublish(
                                revision, partialRevision, token, generation,
                                voiceSessions.isCurrent(voiceToken), listening)) {
                            partialTranslation = translated
                        }
                        translator.close()
                        interimTranslationBusy = false
                    }
                    .addOnFailureListener {
                        // A missing local model leaves the preview untranslated.
                        // Never display raw ML Kit error text or attempt cloud STT.
                        translator.close()
                        interimTranslationBusy = false
                    }
            } catch (_: Exception) {
                translator.close()
                interimTranslationBusy = false
            }
        }
        pendingInterim = task
        interimHandler.postDelayed(task, InterimTranslationPolicy.STABLE_WAIT_MS)
    }

    // Queued work is valid only for this *exact* on-device speech generation.
    // Clearing, stopping, backgrounding or choosing another source invalidates
    // the token and cancels its runnable before microphone access is attempted.
    fun scheduleRestart(voiceToken: Long, delayMs: Long, restart: () -> Unit) {
        if (!voiceSessions.isCurrent(voiceToken) || !listening) return
        pendingRestart?.let(restartHandler::removeCallbacks)
        val task = Runnable {
            pendingRestart = null
            if (voiceSessions.isCurrent(voiceToken) && listening &&
                MicrophoneStartPolicy.mayStart(lifecycleOwner.lifecycle.currentState) &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED) {
                restart()
            }
        }
        pendingRestart = task
        restartHandler.postDelayed(task, delayMs)
    }

    fun stop() {
        offlinePreparationEpoch.invalidate() // A stale model receipt never applies after Stop/background.
        textInventory.invalidate() // Late inventory callbacks never publish after Stop/background.
        speechPacks.cancel() // Old device coverage/download callbacks cannot change a new session.
        voiceSessions.invalidate() // Stop/background immediately invalidates prior callbacks.
        pendingRestart?.let(restartHandler::removeCallbacks)
        pendingRestart = null
        clearInterim()
        // Invalidate callbacks before releasing Android's recognizer so a
        // late interim/final result cannot revive the stopped conversation.
        val active = recognizer
        recognizer = null
        if (active != null) SpeechCleanupPolicy.release(active::cancel, active::destroy)
        listening = false
        partial = ""
    }

    fun start(forceRestart: Boolean = false) {
        error = ""
        // The runtime permission dialog may return after the Activity has
        // stopped. The microphone must never start from that stale callback.
        if (!MicrophoneStartPolicy.mayStart(lifecycleOwner.lifecycle.currentState)) {
            listening = false
            error = "Microfoon gestopt: open Tuddy en tik opnieuw op Luister."
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED) {
            listening = false
            error = "Microfoontoegang ontbreekt. Geef toestemming via Android."
            return
        }
        if (!offlineAvailable) {
            listening = false
            error = "Deze telefoon heeft geen on-device spraakherkenning. Tuddy schakelt niet over naar cloud-spraak."
            return
        }
        // Android can report that this selected locale is definitely not
        // installed locally. Never start a doomed mic session or silently use
        // an online language pack for that known state. Unknown API support
        // may still attempt createOnDeviceSpeechRecognizer (never network STT).
        if (speechPackReceipt.tag == source.speechCode &&
            speechPackReceipt.stage in setOf(
                SpeechPackStage.DOWNLOADABLE, SpeechPackStage.PENDING,
                SpeechPackStage.ONLINE_ONLY, SpeechPackStage.UNSUPPORTED
            )) {
            listening = false
            error = "Voor ${source.label} is geen geïnstalleerd lokaal spraakpakket bevestigd. " +
                "Controleer spraak en bereid het pakket voor via wifi."
            return
        }
        if (listening && !forceRestart) return
        try {
            // Every start, including a rapid restart after Android's own end-of-speech,
            // must invalidate the previous recognizer's delayed callbacks.
            val voiceToken = voiceSessions.begin()
            // Android final/end-of-speech requires a fresh recognizer.
            // Release both its microphone capture and service before restart.
            val previous = recognizer
            recognizer = null
            if (previous != null) SpeechCleanupPolicy.release(previous::cancel, previous::destroy)
            val speech = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            recognizer = speech
            val selectedTarget = target.translateCode
            val selectedSource = source.translateCode
            var observedSource = selectedSource
            val requestLanguageSwitch = OfflineSpeechLanguageSwitchPolicy.shouldRequest(
                Build.VERSION.SDK_INT, automaticSpeechSource, offlineAvailable)
            val allowedSpeechTags = spoken.map { it.speechCode }
            val token = generation
            speech.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { if (voiceSessions.isCurrent(voiceToken)) listening = true }
                override fun onBeginningOfSpeech() { if (voiceSessions.isCurrent(voiceToken)) listening = true }
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit // keep user intent active for the next utterance
                override fun onLanguageDetection(results: Bundle) {
                    if (!requestLanguageSwitch || !voiceSessions.isCurrent(voiceToken)) return
                    val switchResult = results.getInt(SpeechRecognizer.LANGUAGE_SWITCH_RESULT, -1)
                    if (switchResult == SpeechRecognizer.LANGUAGE_SWITCH_RESULT_SKIPPED_NO_MODEL) {
                        error = "Automatisch wisselen vereist geïnstalleerde lokale spraakpakketten. Kies zo nodig handmatig."
                    }
                    // Detection can succeed while switching fails or is not attempted.
                    // Never relabel a caption unless Android confirmed the actual switch.
                    if (!OfflineSpeechLanguageSwitchPolicy.confirmedSwitch(
                            switchResult, SpeechRecognizer.LANGUAGE_SWITCH_RESULT_SUCCEEDED)) return
                    observedSource = OfflineSpeechLanguageSwitchPolicy.resolveDetectedTextSource(
                        results.getString(SpeechRecognizer.DETECTED_LANGUAGE),
                        selectedSource, allowedSpeechTags)
                }
                override fun onError(code: Int) {
                    if (!voiceSessions.isCurrent(voiceToken)) return
                    clearInterim()
                    val retry = ContinuousSpeechRestartPolicy.delayForError(
                        code, SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
                    if (retry != null) {
                        scheduleRestart(voiceToken, retry) { start(forceRestart = true) }
                    } else {
                        stop() // never retry permissions, unsupported languages or unknown errors
                        error = "Offline spraakherkenning niet beschikbaar of onderbroken (code $code). Controleer het gedownloade spraakpakket."
                    }
                }
                override fun onResults(results: Bundle?) {
                    if (!voiceSessions.isCurrent(voiceToken)) return
                    val sentence = SpeechHypothesisPolicy.select(
                        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION))
                    clearInterim()
                    if (sentence.isNotBlank() && token == generation) {
                        val id = System.nanoTime()
                        lines.add(Line(id, sentence, "Vertalen…", "Bezig", observedSource, selectedTarget))
                        // Evict old RAM-only captions before scheduling more work.
                        // Late callbacks for evicted IDs are already denied by
                        // translationStillRelevant(lines.any { it.id == id }).
                        repeat(CaptionRetentionPolicy.oldestToDrop(lines.size)) {
                            lines.removeAt(0)
                        }
                        translate(sentence, id, selectedTarget, observedSource, token)
                    }
                    // The user asked for live captions, not one isolated sentence.
                    // A new recognizer generation suppresses callbacks from the old one.
                    scheduleRestart(voiceToken, ContinuousSpeechRestartPolicy.RESULT_DELAY_MS) {
                        start(forceRestart = true)
                    }
                }
                override fun onPartialResults(results: Bundle?) {
                    if (token == generation && voiceSessions.isCurrent(voiceToken)) {
                        val latest = SpeechHypothesisPolicy.select(
                            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION))
                        if (latest != partial) {
                            partial = latest
                            prepareInterim(latest, observedSource, selectedTarget, voiceToken, token)
                        }
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, source.speechCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                if (requestLanguageSwitch) {
                    putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION, true)
                    putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_SWITCH,
                        RecognizerIntent.LANGUAGE_SWITCH_BALANCED)
                    putStringArrayListExtra(RecognizerIntent.EXTRA_LANGUAGE_SWITCH_ALLOWED_LANGUAGES,
                        ArrayList(allowedSpeechTags))
                }
            }
            speech.startListening(intent)
            listening = true
        } catch (exception: Exception) {
            // startListening may throw after a recognizer was allocated.
            // Do not leave that session eligible for callbacks or reuse.
            voiceSessions.invalidate()
            pendingRestart?.let(restartHandler::removeCallbacks)
            pendingRestart = null
            val failed = recognizer
            recognizer = null
            if (failed != null) SpeechCleanupPolicy.release(failed::cancel, failed::destroy)
            listening = false
            partial = ""
            clearInterim()
            error = SafeLocalFailureText.SPEECH_START
        }
    }

    val microphonePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        when {
            !granted -> error = "Microfoontoegang is nodig. Geef toestemming via de Android-instellingen."
            MicrophoneStartPolicy.mayStart(lifecycleOwner.lifecycle.currentState) -> start()
            else -> error = "Tuddy was op de achtergrond. Open de app en tik opnieuw op Luister."
        }
    }

    // Activity remaining composed in the background must NOT keep the mic open.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) stop()
            if (event == Lifecycle.Event.ON_STOP) {
                speechPackReceipt = SpeechPackReceipt(source.speechCode, SpeechPackStage.UNCHECKED,
                    "Na terugkeer uit de achtergrond moet pakketstatus opnieuw worden gecontroleerd.")
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            generation++
            stop() // stop() already releases and nulls Android's recognizer
        }
    }

    // The partial caption grows while still visible. A Boolean key never
    // changes after the first word, so track its actual text revision.
    // Keep interim scrolling immediate rather than repeatedly restarting
    // an animation on each speech-recognition callback.
    LaunchedEffect(lines.lastOrNull()?.id, partial, partialTranslation, follow) {
        val index = LiveFollowPolicy.targetIndex(lines.size, partial.isNotBlank(), follow)
        if (index != null) {
            if (partial.isNotBlank()) listState.scrollToItem(index)
            else listState.animateScrollToItem(index)
        }
    }

    MaterialTheme(colorScheme = colors) {
        Surface(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Tuddy", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("Gesprekken begrijpen, ook zonder internet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(onClick = { sourceOpen = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Van: ${source.label}", maxLines = 1)
                        }
                        DropdownMenu(expanded = sourceOpen, onDismissRequest = { sourceOpen = false }) {
                            spoken.forEach {
                                DropdownMenuItem(text = { Text(it.label) }, onClick = {
                                    stop()
                                    source = it
                                    languageSettings.edit()
                                        .putString(PrivateLanguageSelectionPolicy.SOURCE_KEY, it.speechCode)
                                        .apply()
                                    offlineModelStatus = ""
                                    textReceipt = OfflineTextReceipt(it.translateCode, target.translateCode,
                                        OfflineTextStage.UNCHECKED, "Spreektaal veranderd: controleer tekstmodel opnieuw.")
                                    speechPackReceipt = SpeechPackReceipt(it.speechCode, SpeechPackStage.UNCHECKED,
                                        "Voor deze spreektaal is nog geen lokale pakketcontrole uitgevoerd.")
                                    sourceOpen = false
                                })
                            }
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(onClick = { targetOpen = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Naar: ${target.label}", maxLines = 1)
                        }
                        DropdownMenu(expanded = targetOpen, onDismissRequest = { targetOpen = false }) {
                            Text("59 talen · offline tekstmodellen", modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
                            OutlinedTextField(
                                value = targetQuery,
                                onValueChange = { targetQuery = it },
                                label = { Text("Zoek taal") },
                                modifier = Modifier.width(310.dp).padding(horizontal = 10.dp),
                                singleLine = true
                            )
                            TargetCatalog.search(targetQuery).take(35).forEach { choice ->
                                DropdownMenuItem(text = { Text(choice.title) }, onClick = {
                                    stop()
                                    target = LanguageChoice(choice.title, choice.code, choice.code)
                                    languageSettings.edit()
                                        .putString(PrivateLanguageSelectionPolicy.TARGET_KEY, choice.code)
                                        .apply()
                                    offlineModelStatus = ""
                                    textReceipt = OfflineTextReceipt(source.translateCode, choice.code,
                                        OfflineTextStage.UNCHECKED, "Vertaaltaal veranderd: controleer tekstmodel opnieuw.")
                                    targetQuery = ""
                                    targetOpen = false
                                })
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Automatische spraaktaal (Android 14+)",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f))
                    Switch(
                        checked = automaticSpeechSource,
                        enabled = speechSwitchSupported,
                        onCheckedChange = {
                            stop()
                            automaticSpeechSource = it
                        }
                    )
                }
                if (automaticSpeechSource) {
                    Text("Alleen de zes gekozen lokale spraaktalen; de telefoon moet hun offline spraakpakketten bezitten. " +
                        "Detectie is niet gegarandeerd en gebruikt nooit cloudspraak.",
                        style = MaterialTheme.typography.bodySmall)
                }
                // This panel has no microphone path, translator client or network request.
                // It is five locally bundled expressions only, not free-form translation.
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Offline basiswoorden · Kurmanji, Sorani, Pasjtoe",
                        style = MaterialTheme.typography.titleSmall)
                    Text("Handmatig · slechts 5 woorden/uitdrukkingen, geen spraakherkenning of volledige vertaling.",
                        style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.weight(1f)) {
                            OutlinedButton(onClick = { previewMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(when (previewTarget) {
                                    "kmr" -> "Kurmanji"
                                    "ckb" -> "Sorani"
                                    else -> "Pasjtoe"
                                })
                            }
                            DropdownMenu(expanded = previewMenuOpen, onDismissRequest = { previewMenuOpen = false }) {
                                listOf("kmr" to "Kurmanji", "ckb" to "Sorani", "ps" to "Pasjtoe").forEach { (code, label) ->
                                    DropdownMenuItem(text = { Text(label) }, onClick = {
                                        previewTarget = code
                                        previewResult = null
                                        previewMenuOpen = false
                                    })
                                }
                            }
                        }
                        OutlinedButton(onClick = {
                            previewSource = if (previewSource == "nl") "en" else "nl"
                            previewResult = null
                        }) { Text(if (previewSource == "nl") "Van: NL" else "From: EN") }
                    }
                    OutlinedTextField(value = previewInput, onValueChange = {
                        previewInput = it.take(80)
                        previewResult = null
                    }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text("Bijv. hallo, bedankt, ja, nee, water") })
                    Button(onClick = {
                        previewResult = SafeCommunityPhrasebook.translate(previewSource, previewTarget, previewInput)
                            .fold(onSuccess = { it }, onFailure = { "Niet in deze kleine woordenlijst." })
                    }, enabled = previewInput.isNotBlank()) { Text("Zoek basiswoord lokaal") }
                    if (previewResult != null) {
                        Text(
                            text = previewResult!!,
                            color = MaterialTheme.colorScheme.primary,
                            // The manually reviewed Sorani and Pashto phrases
                            // use Arabic-based scripts; do not guess based on
                            // the user's phone/system locale.
                            style = MaterialTheme.typography.bodyLarge.copy(
                                textDirection = if (previewTarget == "ckb" || previewTarget == "ps")
                                    TextDirection.Rtl else TextDirection.Ltr
                            )
                        )
                    }
                    Text("Niet geschikt voor medisch, juridisch of asieladvies zonder tolk. Controle door moedertaalsprekers ontbreekt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // A local text-only fallback works even when Android does not
                // provide an on-device speech recognizer or mic permission.
                // It shares the same RAM-only history and ML Kit translator.
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Zin typen · lokale tekstvertaling",
                        style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = manualText,
                        onValueChange = { candidate ->
                            if (candidate.length <= OfflineTypedCaptionPolicy.MAX_INPUT_UNITS) {
                                manualText = candidate
                            }
                        },
                        label = { Text("Typ een zin om lokaal te vertalen") },
                        minLines = 1,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val sentence = OfflineTypedCaptionPolicy.normalize(manualText)
                                ?: return@Button
                            val id = System.nanoTime()
                            val token = generation
                            val selectedSource = source.translateCode
                            val selectedTarget = target.translateCode
                            lines.add(Line(id, sentence, "Vertalen…", "Bezig",
                                selectedSource, selectedTarget))
                            repeat(CaptionRetentionPolicy.oldestToDrop(lines.size)) {
                                lines.removeAt(0)
                            }
                            manualText = ""
                            translate(sentence, id, selectedTarget, selectedSource, token)
                        },
                        enabled = OfflineTypedCaptionPolicy.normalize(manualText) != null
                    ) { Text("Vertaal tekst lokaal") }
                    Text("Geen microfoon nodig. Alleen de beschikbare lokale ML Kit-tekstmodellen; " +
                        "geen netwerk-spraakdienst of ondersteuning voor vrije Koerdische/Pasjtoe zinnen.",
                        style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val canStart = offlineAvailable && !listening
                    Button(
                        onClick = {
                            if (listening) stop()
                            else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                == PackageManager.PERMISSION_GRANTED) start()
                            else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        enabled = offlineAvailable,
                    ) { Text(if (listening) "■ Stop" else "🎙 Luister") }
                    OutlinedButton(onClick = {
                        generation++
                        stop()
                        offlineModelStatus = ""
                        lines.clear()
                        previewInput = ""
                        manualText = ""
                        previewResult = null
                        error = ""
                    }) { Text("Wis gesprek") }
                    TextButton(onClick = { follow = !follow }) {
                        Text(if (follow) "Live ✓" else "Volg live")
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Offline tekstvertaling · ${source.label} → ${target.label}",
                        style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        OutlinedButton(
                            onClick = { checkTextPair() },
                            enabled = !preparingOfflineModel &&
                                textReceipt.stage != OfflineTextStage.CHECKING
                        ) { Text("Controleer tekst") }
                        OutlinedButton(onClick = { prepareOfflinePair() }, enabled = !preparingOfflineModel) {
                            Text(if (preparingOfflineModel) "Voorbereiden…" else "Model via wifi")
                        }
                    }
                    Text(
                        if (textReceipt.source == source.translateCode &&
                            textReceipt.target == target.translateCode) textReceipt.detail
                        else "Taalpaar gewijzigd: controleer de lokale tekstmodellen opnieuw.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        offlineModelStatus.ifBlank {
                            "Gedownloade ML Kit-tekstmodellen zijn een andere capaciteit dan Android-spraakpakketten."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                // Android 13+ exposes actual installed, pending and supported
                // on-device recognition languages. Never infer speech readiness
                // from a successful ML Kit text-model download.
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Offline spraakpakket · ${source.label}",
                        style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (speechPackReceipt.tag == source.speechCode) speechPackReceipt.detail
                        else "Spraaktaal gewijzigd: controleer het nieuwe pakket.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        OutlinedButton(
                            onClick = {
                                speechPacks.check(source.speechCode) { result ->
                                    if (source.speechCode == result.tag &&
                                        lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                                        speechPackReceipt = result
                                    }
                                }
                            },
                            enabled = offlineAvailable && !listening &&
                                speechPackReceipt.stage !in setOf(
                                    SpeechPackStage.CHECKING, SpeechPackStage.DOWNLOADING)
                        ) { Text("Controleer spraak") }
                        if (speechPackReceipt.tag == source.speechCode && speechPackReceipt.canDownload) {
                            Button(onClick = {
                                speechPacks.download(source.speechCode) { result ->
                                    if (source.speechCode == result.tag &&
                                        lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                                        speechPackReceipt = result
                                    }
                                }
                            }, enabled = !listening) {
                                Text(if (speechPackReceipt.stage == SpeechPackStage.ERROR)
                                    "Opnieuw via wifi" else "Download via wifi")
                            }
                        }
                        if (speechPackReceipt.tag == source.speechCode &&
                            speechPackReceipt.stage in setOf(
                                SpeechPackStage.CHECKING, SpeechPackStage.DOWNLOADING
                            )) {
                            TextButton(onClick = {
                                speechPacks.cancel()
                                speechPackReceipt = SpeechPackReceipt(source.speechCode,
                                    SpeechPackStage.REQUESTED,
                                    "Statuscontrole gestopt. Android kan de download alsnog voortzetten; controleer opnieuw.")
                            }) { Text("Stop volgen") }
                        }
                    }
                    Text("Alleen de door Android gemelde lokale spreektaal. Geen cloudfallback; " +
                        "Android beheert de download en kan deze inplannen. " +
                        "Pakketstatus zegt niets over vrije Koerdische/Pasjtoe vertaling.",
                        style = MaterialTheme.typography.bodySmall)
                }
                if (!offlineAvailable) {
                    Text("Geen lokale spraakherkenning op dit apparaat. Geen cloudfallback.", color = MaterialTheme.colorScheme.error)
                }
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
                HorizontalDivider()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(lines, key = { it.id }) { line ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                OfflineWordText(
                                    line.original, line.sourceCode,
                                    MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                                )
                                if (line.status == "Lokaal vertaald" || line.status == "Lokaal") {
                                    OfflineWordText(
                                        line.translated, line.targetCode,
                                        MaterialTheme.typography.titleMedium.copy(
                                            color = MaterialTheme.colorScheme.primary)
                                    )
                                } else {
                                    Text(line.translated, style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                                Text(line.status, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    if (partial.isNotBlank()) {
                        item {
                            Card {
                                Column(Modifier.padding(14.dp)) {
                                    Text("● Spreekt nu…", color = MaterialTheme.colorScheme.primary)
                                    Text(partial, fontWeight = FontWeight.Bold)
                                    if (partialTranslation.isNotBlank()) {
                                        Text(partialTranslation,
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                            }
                        }
                    }
                }
                Text(
                    "Privacy: alleen lokaal spraakherkennen en vertalen. Taalmodellen eerst via wifi downloaden. Gesprek staat alleen in tijdelijk geheugen en wordt niet opgeslagen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
