package com.livelingo.android

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.livelingo.android.ui.SavedWordsPanel
import com.livelingo.android.ui.MyOfflinePhrasesPanel
import com.livelingo.android.ui.FocusTranscriptDialog
import com.livelingo.android.ui.FocusCaptionLine
import java.io.ByteArrayOutputStream
import java.text.DateFormat
import java.util.Date

private data class LanguageChoice(val label: String, val speechCode: String, val translateCode: String)
private val spoken = listOf(
    LanguageChoice("Nederlands", "nl-NL", TranslateLanguage.DUTCH),
    LanguageChoice("English", "en-US", TranslateLanguage.ENGLISH),
    LanguageChoice("Deutsch", "de-DE", TranslateLanguage.GERMAN),
    LanguageChoice("Français", "fr-FR", TranslateLanguage.FRENCH),
    LanguageChoice("Español", "es-ES", TranslateLanguage.SPANISH),
    LanguageChoice("العربية", "ar-SA", TranslateLanguage.ARABIC),
    // Extra opt-in source candidates from the archived v0.4 locale hints.
    // They are NOT assertions that this phone has installed speech packs.
    LanguageChoice("فارسی · Farsi", "fa-IR", "fa"),
    LanguageChoice("Українська", "uk-UA", "uk"),
    LanguageChoice("Türkçe", "tr-TR", "tr"),
    LanguageChoice("Русский", "ru-RU", "ru"),
    LanguageChoice("اردو · Urdu", "ur-PK", "ur")
)
private data class Line(
    val id: Long,
    val original: String,
    val translated: String,
    val status: String,
    val sourceCode: String,
    val targetCode: String,
    val recordedAtMillis: Long = System.currentTimeMillis(),
    val translationRevision: Int = 0,
    val userCorrected: Boolean = false
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
    val captionPrefs = remember(context) {
        context.getSharedPreferences(CaptionDisplayPreferences.NAME, Context.MODE_PRIVATE)
    }
    var captionSize by remember {
        mutableStateOf(
            CaptionSize.restore(runCatching {
                captionPrefs.getString(CaptionDisplayPreferences.SIZE_KEY, null)
            }.getOrNull())
        )
    }
    var captionSizeMessage by remember { mutableStateOf("") }
    // Local-only bookmarks: never persisted or associated with external identity.
    var captionPins by remember { mutableStateOf(emptySet<Long>()) }
    // Human-assigned conversation turns: labels stay in RAM only.
    // No automatic identity inference, audio fingerprint, server or export.
    val manuallyAssignedSpeakers = remember { mutableStateMapOf<Long, String>() }
    var pinsOpen by remember { mutableStateOf(false) }
    var pinsNotice by remember { mutableStateOf("") }
    val captionSpeaker = remember(context) { LocalCaptionSpeaker(context) }
    var pronunciationNotice by remember { mutableStateOf("") }
    val savedVocabulary = remember(context) { PrivateVocabularyDeck(context) }
    var savedWordRevision by remember { mutableIntStateOf(0) }
    val savedWordCount = remember(savedWordRevision) { savedVocabulary.entries().size }
    var savedWordsOpen by remember { mutableStateOf(false) }
    var savedWordNotice by remember { mutableStateOf("") }
    fun saveVerifiedWord(language: String, word: String): Boolean {
        val saved = savedVocabulary.save(language, word)
        savedWordNotice = if (saved) "Woord opgeslagen in je privéwoordenlijst."
            else "Woord niet opgeslagen. Alleen gecontroleerde basiswoorden zijn toegestaan (maximaal 120)."
        if (saved) savedWordRevision++
        return saved
    }
    var partial by remember { mutableStateOf("") }
    var partialTranslation by remember { mutableStateOf("") }
    var partialRevision by remember { mutableIntStateOf(0) }
    var interimTranslationBusy by remember { mutableStateOf(false) }
    var pendingInterim by remember { mutableStateOf<Runnable?>(null) }
    val interimHandler = remember { Handler(Looper.getMainLooper()) }
    var error by remember { mutableStateOf("") }
    // Only picker codes and explicitly bookmarked curated vocabulary are kept
    // in app-private preferences. Captions, speech and model status stay in RAM.
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
    val personalPhrases = remember(context) { PrivatePhraseDeck(context) }
    var personalPhrasesOpen by remember { mutableStateOf(false) }
    var previewSource by remember { mutableStateOf("nl") }
    var previewMenuOpen by remember { mutableStateOf(false) }
    var previewInput by remember { mutableStateOf("") }
    var manualText by remember { mutableStateOf("") }
    // The export snapshot exists only between an explicit Save tap and the
    // Android document picker result; no automatic disk/history persistence.
    var pendingTranscriptExport by remember { mutableStateOf<Pair<Int, ByteArray>?>(null) }
    var exportStatus by remember { mutableStateOf("") }
    // The read-only archive is not part of the current live conversation.
    var previewArchiveRows by remember { mutableStateOf<List<TranscriptExportRow>?>(null) }
    var archiveSearchQuery by remember { mutableStateOf("") }
    var pendingArchiveGeneration by remember { mutableStateOf<Int?>(null) }
    // The user selects one completed row from a read-only, unverified
    // imported archive. No clipboard/share side effects before consent.
    var pendingArchiveExcerpt by remember {
        mutableStateOf<Triple<Int, TranscriptExportRow, String>?>(null)
    }
    var archiveExcerptNotice by remember { mutableStateOf("") }
    var copyStatus by remember { mutableStateOf("") }
    var lastCopiedCaptionId by remember { mutableStateOf<Long?>(null) }
    // Privacy gate: no caption is handed to Android's external share chooser
    // until the user confirms the exact selected line in this session.
    var pendingCaptionShare by remember { mutableStateOf<Triple<Int, Long, String>?>(null) }
    // A separate, opt-in speaker-labelled text export; ordinary archives stay unchanged.
    var pendingSpeakerExport by remember { mutableStateOf<Pair<Int, ByteArray>?>(null) }
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
    var showSettings by remember { mutableStateOf(false) }
    // Secondary export/history actions are collapsed on compact screens.
    var conversationToolsOpen by remember { mutableStateOf(false) }
    var directionNotice by remember { mutableStateOf("") }
    var focusMode by remember { mutableStateOf(false) }
    // Local search stays in memory, never persisted or sent to an API.
    // Editing is opt-in and ephemeral: never persist drafts or silently rewrite audio.
    var editingCaptionId by remember { mutableStateOf<Long?>(null) }
    var captionCorrectionDraft by remember { mutableStateOf("") }
    var correctionNotice by remember { mutableStateOf("") }
    var historySearchOpen by remember { mutableStateOf(false) }
    var historySearchQuery by remember { mutableStateOf("") }
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
    val reverseDirection = ConversationDirectionSwitch.plan(
        source.speechCode, source.translateCode, target.translateCode,
        spoken.map { it.speechCode to it.translateCode }
    )
    val normalizedHistoryQuery = historySearchQuery.trim()
    val historyMatches = if (normalizedHistoryQuery.isBlank()) emptyList<Int>()
        else lines.indices.filter { index ->
            lines[index].original.contains(normalizedHistoryQuery, ignoreCase = true) ||
                lines[index].translated.contains(normalizedHistoryQuery, ignoreCase = true)
        }

    fun captureSpeakerTranscript(): ByteArray? {
        val completed = lines.filter {
            it.status == "Lokaal vertaald" || it.status == "Lokaal"
        }
        if (completed.none {
                manuallyAssignedSpeakers[it.id] == "A" || manuallyAssignedSpeakers[it.id] == "B"
            }) return null
        return PrivateSpeakerTranscriptExport.snapshot(completed.map { line ->
            TranscriptExportRow(
                line.original, line.translated, line.status,
                line.sourceCode, line.targetCode, line.recordedAtMillis
            ) to manuallyAssignedSpeakers[line.id]
        })
    }

    fun jumpToSpeaker(label: String) {
        val matches = lines.indices.filter { manuallyAssignedSpeakers[lines[it].id] == label }
        if (matches.isEmpty()) return
        val destination = matches.firstOrNull { it > listState.firstVisibleItemIndex }
            ?: matches.first()
        follow = false
        coroutineScope.launch { listState.animateScrollToItem(destination) }
    }

    fun jumpToHistoryMatch(forward: Boolean) {
        if (historyMatches.isEmpty()) return
        val currentIndex = listState.firstVisibleItemIndex
        val destination = if (forward) {
            historyMatches.firstOrNull { it > currentIndex } ?: historyMatches.first()
        } else {
            historyMatches.lastOrNull { it < currentIndex } ?: historyMatches.last()
        }
        follow = false
        coroutineScope.launch { listState.animateScrollToItem(destination) }
    }
    // Only a real user scroll should pause lyric-style following. Programmatic
    // scrollToItem / animateScrollToItem uses non-user nested-scroll sources and
    // must not pause itself while tracking a live utterance.
    val manualHistoryScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) {
                    follow = false
                }
                return Offset.Zero
            }
        }
    }

    // Revision fences prevent an old ML Kit callback from replacing a later
    // human correction or retry, even when the text is changed back to its old value.
    fun translationStillRelevant(id: Long, token: Int, revision: Int): Boolean =
        TranslationWorkPolicy.mayContinue(token, generation,
            lines.any { it.id == id && it.translationRevision == revision })

    fun updateLine(id: Long, translation: String, status: String, token: Int, revision: Int) {
        if (!translationStillRelevant(id, token, revision)) return
        val index = lines.indexOfFirst { it.id == id }
        if (index >= 0) lines[index] = lines[index].copy(translated = translation, status = status)
    }

    fun translate(text: String, id: Long, targetCode: String, sourceCode: String, token: Int) {
        val revision = lines.firstOrNull { it.id == id }?.translationRevision ?: return
        if (!translationStillRelevant(id, token, revision)) return
        // Text language detection and translation remain ON DEVICE. Models are downloaded only
        // when the user is on Wi-Fi; without a downloaded model, fail transparently.
        val detector = LanguageIdentification.getClient()
        detector.identifyLanguage(text)
            .addOnSuccessListener { guess ->
                detector.close()
                // Clear/remove may occur while language detection is pending.
                // Do not begin a *new* Wi-Fi model download for abandoned text.
                if (!translationStillRelevant(id, token, revision)) return@addOnSuccessListener
                val identified = guess.takeIf { it != "und" && TranslateLanguage.fromLanguageTag(it) != null }
                val from = identified?.let { TranslateLanguage.fromLanguageTag(it) } ?: sourceCode
                // Each historical caption keeps its recognized original language.
                // Never relabel older lines when a different source is selected.
                val row = lines.indexOfFirst { it.id == id }
                if (row >= 0 && lines[row].translationRevision == revision) lines[row] = lines[row].copy(sourceCode = from)
                if (from == targetCode) {
                    updateLine(id, text, "Lokaal", token, revision)
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
                        if (!translationStillRelevant(id, token, revision)) {
                            translator.close()
                            return@addOnSuccessListener
                        }
                        translator.translate(text)
                            .addOnSuccessListener { output ->
                                updateLine(id, output, "Lokaal vertaald", token, revision)
                                translator.close()
                            }
                            .addOnFailureListener {
                                updateLine(id, SafeLocalFailureText.TRANSLATION, "Fout", token, revision)
                                translator.close()
                            }
                    }
                    .addOnFailureListener {
                        updateLine(id, "Taalmodel ontbreekt — verbind met wifi en probeer opnieuw.", "Model ontbreekt", token, revision)
                        translator.close()
                    }
            }
            .addOnFailureListener {
                detector.close()
                updateLine(id, "Taaldetectie mislukt.", "Fout", token, revision)
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
        captionSpeaker.stop() // No device speech output after Stop/background/clear.
        offlinePreparationEpoch.invalidate() // A stale model receipt never applies after Stop/background.
        textInventory.invalidate() // Late inventory callbacks never publish after Stop/background.
        if (textReceipt.stage == OfflineTextStage.CHECKING) {
            textReceipt = OfflineTextReceipt(source.translateCode, target.translateCode,
                OfflineTextStage.UNCHECKED, "Controle onderbroken; tik opnieuw op Controleer tekst.")
        }
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
                            val removed = lines.removeAt(0)
                            captionPins = captionPins - removed.id
                            manuallyAssignedSpeakers.remove(removed.id)
                            if (editingCaptionId == removed.id) {
                                editingCaptionId = null
                                captionCorrectionDraft = ""
                            }
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

    val saveTranscript = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val pending = pendingTranscriptExport
        pendingTranscriptExport = null
        if (uri != null && pending != null && pending.first == generation) {
            val saved = runCatching {
                context.contentResolver.openOutputStream(uri, "w")?.use {
                    it.write(pending.second)
                    it.flush()
                    true
                } ?: false
            }.getOrDefault(false)
            if (saved) {
                exportStatus = "Gesprek opgeslagen op de door jou gekozen locatie."
            } else {
                error = "Gesprek niet opgeslagen. Kies een andere locatie en probeer opnieuw."
            }
        }
    }

    val saveTranscriptJson = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val pending = pendingTranscriptExport
        pendingTranscriptExport = null
        if (uri != null && pending != null && pending.first == generation) {
            val saved = runCatching {
                context.contentResolver.openOutputStream(uri, "w")?.use {
                    it.write(pending.second)
                    it.flush()
                    true
                } ?: false
            }.getOrDefault(false)
            if (saved) {
                exportStatus = "JSON-gesprek opgeslagen op de door jou gekozen locatie."
            } else {
                error = "JSON-gesprek niet opgeslagen. Kies een andere locatie en probeer opnieuw."
            }
        }
    }

    val openTranscriptArchive = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val intendedGeneration = pendingArchiveGeneration
        pendingArchiveGeneration = null
        if (uri != null && intendedGeneration != null && intendedGeneration == generation) {
            val loaded = runCatching {
                val stream = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("Cannot read selected document")
                val bytes = stream.use { input ->
                    val buffer = ByteArray(8192)
                    val collected = ByteArrayOutputStream()
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (count == 0) continue
                        if (collected.size() + count > 262_144) throw IllegalArgumentException("Archive exceeds read limit")
                        collected.write(buffer, 0, count)
                    }
                    collected.toByteArray()
                }
                PrivateTranscriptArchive.read(bytes)
            }.getOrNull()
            pendingArchiveExcerpt = null
            archiveExcerptNotice = ""
            if (loaded == null) {
                previewArchiveRows = null
                error = "Geen geldig privégespreksarchief. Het huidige gesprek is niet gewijzigd."
            } else {
                previewArchiveRows = loaded
                archiveSearchQuery = ""
                exportStatus = ""
                error = ""
            }
        }
    }

    // Activity remaining composed in the background must NOT keep the mic open.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) stop()
            if (event == Lifecycle.Event.ON_STOP) {
                focusMode = false
                previewArchiveRows = null
                archiveSearchQuery = ""
                pendingArchiveExcerpt = null
                archiveExcerptNotice = ""
                pendingCaptionShare = null
                pendingSpeakerExport = null
                editingCaptionId = null
                captionCorrectionDraft = ""
            }
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
            previewArchiveRows = null
            archiveSearchQuery = ""
            pendingArchiveGeneration = null
            pendingArchiveExcerpt = null
            archiveExcerptNotice = ""
            pendingSpeakerExport = null
            editingCaptionId = null
            captionCorrectionDraft = ""
            manuallyAssignedSpeakers.clear()
            captionSpeaker.close()
        }
    }

    // Returning to Live dismisses a prior local search; searching never
    // retains an unseen filter that can later fight automatic following.
    LaunchedEffect(follow) {
        if (follow) {
            historySearchOpen = false
            historySearchQuery = ""
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
        if (focusMode) {
            FocusTranscriptDialog(
                captions = lines.map {
                    FocusCaptionLine(it.id, it.original, it.translated, it.status,
                        it.sourceCode, it.targetCode, manuallyAssignedSpeakers[it.id])
                },
                interimOriginal = partial,
                interimTranslated = partialTranslation,
                listening = listening,
                fontScale = captionSize.scale,
                onStop = { stop() },
                onClose = { focusMode = false }
            )
        }
        val speakerSnapshot = pendingSpeakerExport
        if (speakerSnapshot != null) {
            AlertDialog(
                onDismissRequest = { pendingSpeakerExport = null },
                title = { Text("Handmatig gelabeld gesprek bewaren?") },
                text = {
                    Text(
                        "Alleen jij hebt de sprekerlabels A/B gekozen. Ze herkennen geen echte " +
                            "personen. Je gaat de afgeronde uitspraken inclusief labels " +
                            "als TXT buiten het tijdelijke Tuddy-geheugen opslaan. " +
                            "De gekozen Android-bestandsprovider kan het document " +
                            "synchroniseren of met anderen delen. Controleer gevoelige gegevens."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        pendingSpeakerExport = null
                        val current = captureSpeakerTranscript()
                        if (listening || speakerSnapshot.first != generation ||
                            current == null || !current.contentEquals(speakerSnapshot.second)) {
                            exportStatus = "Het gesprek of de labels zijn gewijzigd. Kies opnieuw exporteren."
                        } else {
                            pendingTranscriptExport = generation to current
                            saveTranscript.launch("Tuddy-handmatige-sprekers.txt")
                        }
                    }) { Text("Ik begrijp dit, kies opslag…") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingSpeakerExport = null }) { Text("Annuleren") }
                }
            )
        }
        val correctionId = editingCaptionId
        val correctionLine = lines.firstOrNull { it.id == correctionId }
        if (correctionId != null && correctionLine != null) {
            AlertDialog(
                onDismissRequest = {
                    editingCaptionId = null
                    captionCorrectionDraft = ""
                },
                title = { Text("Herstel herkende zin") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Controleer zelf wat er werkelijk is gezegd. De originele " +
                            "zin en de bijbehorende vertaling worden in dit tijdelijke " +
                            "gesprek vervangen. Dit is geen automatische controle.")
                        OutlinedTextField(
                            value = captionCorrectionDraft,
                            onValueChange = { candidate ->
                                if (candidate.length <= OfflineTypedCaptionPolicy.MAX_INPUT_UNITS)
                                    captionCorrectionDraft = candidate
                            },
                            label = { Text("Gecorrigeerde oorspronkelijke zin") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 5
                        )
                        Text("Vertaling wordt opnieuw lokaal aangevraagd. Een ontbrekend " +
                            "model kan alleen via wifi worden voorbereid; geen cloudspraak.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = !listening &&
                            OfflineTypedCaptionPolicy.normalize(captionCorrectionDraft) != null &&
                            OfflineTypedCaptionPolicy.normalize(captionCorrectionDraft) != correctionLine.original,
                        onClick = {
                            val corrected = OfflineTypedCaptionPolicy.normalize(captionCorrectionDraft)
                                ?: return@TextButton
                            val index = lines.indexOfFirst { it.id == correctionId }
                            if (index >= 0 && !listening &&
                                lines[index].original != corrected) {
                                val previous = lines[index]
                                captionSpeaker.stop()
                                pronunciationNotice = ""
                                // Pending user-consent/export payloads must not outlive a revision.
                                pendingCaptionShare = null
                                pendingTranscriptExport = null
                                exportStatus = ""
                                lines[index] = previous.copy(
                                    original = corrected,
                                    translated = "Vertalen…",
                                    status = "Bezig",
                                    translationRevision = previous.translationRevision + 1,
                                    userCorrected = true
                                )
                                translate(corrected, previous.id, previous.targetCode,
                                    previous.sourceCode, generation)
                                correctionNotice = "Correctie opgeslagen in dit gesprek; " +
                                    "vertaling opnieuw gestart."
                            }
                            editingCaptionId = null
                            captionCorrectionDraft = ""
                        }
                    ) { Text("Corrigeer en vertaal") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        editingCaptionId = null
                        captionCorrectionDraft = ""
                    }) { Text("Annuleer") }
                }
            )
        }
        val share = pendingCaptionShare
        if (share != null) {
            AlertDialog(
                onDismissRequest = { pendingCaptionShare = null },
                title = { Text("Deze vertaalde zin extern delen?") },
                text = {
                    Text(
                        "Dit gesprek blijft normaal alleen in Tuddy. Na bevestiging gaat " +
                            "uitsluitend deze originele en lokaal afgeronde vertaling naar " +
                            "het Android-deelvenster. De gekozen app of persoon kan de tekst " +
                            "opslaan, synchroniseren of doorsturen. Deel geen gevoelige gegevens."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        pendingCaptionShare = null
                        val current = lines.firstOrNull { it.id == share.second }
                        val valid = share.first == generation && current != null &&
                            (current.status == "Lokaal vertaald" || current.status == "Lokaal") &&
                            current.translated.isNotBlank() &&
                            PrivateCaptionCopy.snapshot(
                                TranscriptExportRow(
                                    current.original, current.translated, current.status,
                                    current.sourceCode, current.targetCode,
                                    current.recordedAtMillis
                                )
                            ) == share.third
                        lastCopiedCaptionId = share.second
                        if (!valid) {
                            copyStatus = "Deze vertaling is gewijzigd. Kies de zin opnieuw."
                        } else {
                            val opened = runCatching {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, share.third)
                                }
                                context.startActivity(
                                    Intent.createChooser(intent, "Kies waar je deze zin deelt")
                                )
                                true
                            }.getOrDefault(false)
                            copyStatus = if (opened) {
                                "Android-deelvenster geopend; bestemming en verzending kies je zelf."
                            } else {
                                "Deelvenster niet beschikbaar; er is niets verzonden."
                            }
                        }
                    }) { Text("Open deelvenster") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingCaptionShare = null }) {
                        Text("Annuleren")
                    }
                }
            )
        }
        val excerptRequest = pendingArchiveExcerpt
        if (excerptRequest != null) {
            fun stillCurrent(): Boolean {
                val row = excerptRequest.second
                val clean = PrivateCaptionCopy.snapshot(row)
                return excerptRequest.first == generation && !listening &&
                    previewArchiveRows?.contains(row) == true &&
                    row.status == "Lokaal vertaald" && clean != null &&
                    "Eigen archief — inhoud niet geverifieerd\n" + clean == excerptRequest.third
            }
            AlertDialog(
                onDismissRequest = { pendingArchiveExcerpt = null },
                title = { Text("Dit archieffragment buiten Tuddy gebruiken?") },
                text = {
                    Text("Dit bestand is door jou gekozen. Tuddy controleert " +
                        "de oorspronkelijke zin of vertaling niet opnieuw. " +
                        "Het Android-klembord en andere apps vallen buiten de " +
                        "privé-opslag van Tuddy en kunnen tekst bewaren of delen. " +
                        "Alleen deze ene afgeronde tweetalige regel wordt overgedragen.")
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = {
                            pendingArchiveExcerpt = null
                            if (!stillCurrent()) {
                                archiveExcerptNotice = "Archief gewijzigd of gesloten. Kies opnieuw."
                            } else {
                                val copied = runCatching {
                                    val board = context.getSystemService(
                                        Context.CLIPBOARD_SERVICE
                                    ) as ClipboardManager
                                    val clip = ClipData.newPlainText(
                                        "Tuddy privéarchief — niet gecontroleerd",
                                        excerptRequest.third
                                    )
                                    clip.description.extras = PersistableBundle().apply {
                                        putBoolean("android.content.extra.IS_SENSITIVE", true)
                                    }
                                    board.setPrimaryClip(clip)
                                    true
                                }.getOrDefault(false)
                                archiveExcerptNotice = if (copied)
                                    "Fragment gekopieerd; Android-klembord is buiten Tuddy zichtbaar."
                                else "Kopiëren mislukt; niets overgedragen."
                            }
                        }) { Text("Kopieer") }
                        TextButton(onClick = {
                            pendingArchiveExcerpt = null
                            if (!stillCurrent()) {
                                archiveExcerptNotice = "Archief gewijzigd of gesloten. Kies opnieuw."
                            } else {
                                val opened = runCatching {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, excerptRequest.third)
                                    }
                                    context.startActivity(Intent.createChooser(
                                        intent, "Kies waar je dit archieffragment deelt"
                                    ))
                                    true
                                }.getOrDefault(false)
                                archiveExcerptNotice = if (opened)
                                    "Android-deelvenster geopend; bestemming bepaal je zelf."
                                else "Delen niet mogelijk; niets verzonden."
                            }
                        }) { Text("Open delen") }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingArchiveExcerpt = null }) {
                        Text("Annuleer")
                    }
                }
            )
        }
        val archive = previewArchiveRows
        val matchedArchive = if (archive == null) emptyList() else
            PrivateArchiveSearch.filter(archive, archiveSearchQuery)
        if (archive != null && pendingArchiveExcerpt == null) {
            AlertDialog(
                onDismissRequest = {
                    pendingArchiveExcerpt = null
                    previewArchiveRows = null
                    archiveSearchQuery = ""
                    archiveExcerptNotice = ""
                },
                title = { Text("Eigen JSON-archief · alleen lezen") },
                text = {
                    Column {
                        Text(
                            "Door jou gekozen bestand. Deze tekst is niet onafhankelijk geverifieerd " +
                                "en wordt niet aan de live opname toegevoegd.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = archiveSearchQuery,
                            onValueChange = {
                                archiveSearchQuery = PrivateArchiveSearch.normalizedQuery(it)
                            },
                            label = { Text("Zoek in dit archief") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "${matchedArchive.size} van ${archive.size} regels zichtbaar · alleen op dit toestel",
                            style = MaterialTheme.typography.labelSmall
                        )
                        if (archiveExcerptNotice.isNotBlank()) {
                            Text(archiveExcerptNotice,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(8.dp))
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(matchedArchive) { row ->
                                Column {
                                    Text(
                                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                            .format(Date(row.recordedAtMillis)) +
                                            " · " + row.sourceCode + " → " + row.targetCode,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(row.original,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = MaterialTheme.typography.bodyLarge.fontSize * captionSize.scale
                                        ))
                                    Text(
                                        if (row.status == "Lokaal vertaald") row.translated
                                        else "[Vertaling niet voltooid]",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = MaterialTheme.typography.bodyLarge.fontSize * captionSize.scale
                                        )
                                    )
                                    if (row.status == "Lokaal vertaald" &&
                                        row.translated.isNotBlank()) {
                                        TextButton(enabled = !listening, onClick = {
                                            val text = PrivateCaptionCopy.snapshot(row)
                                            if (text == null) {
                                                archiveExcerptNotice =
                                                    "Deze regel kan niet veilig buiten Tuddy worden gebruikt."
                                            } else {
                                                pendingArchiveExcerpt = Triple(
                                                    generation, row,
                                                    "Eigen archief — inhoud niet geverifieerd\n" + text
                                                )
                                                archiveExcerptNotice = ""
                                            }
                                        }) { Text("Gebruik deze archiefregel…") }
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        pendingArchiveExcerpt = null
                        archiveExcerptNotice = ""
                        previewArchiveRows = null
                        archiveSearchQuery = ""
                    }) { Text("Sluiten") }
                }
            )
        }

        if (pinsOpen) {
            val pinnedLines = lines.filter { it.id in captionPins }
            AlertDialog(
                onDismissRequest = { pinsOpen = false },
                title = { Text("Gemarkeerde gespreksregels") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Alleen in dit gesprek, maximaal ${SessionCaptionPins.MAX_PINS}. " +
                                "Sluiten wist de markeringen niet; Wis gesprek wel.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (pinnedLines.isEmpty()) {
                            Text("Geen gemarkeerde zinnen in dit gesprek.")
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().heightIn(max = 370.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(pinnedLines, key = { it.id }) { line ->
                                    Column {
                                        Text(
                                            DateFormat.getTimeInstance(DateFormat.SHORT)
                                                .format(Date(line.recordedAtMillis)) +
                                                " · " + line.sourceCode + " → " + line.targetCode,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Text(line.original, style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = MaterialTheme.typography.bodyMedium.fontSize * captionSize.scale
                                        ))
                                        if (line.status == "Lokaal vertaald" || line.status == "Lokaal") {
                                            Text(line.translated,
                                                color = MaterialTheme.colorScheme.primary,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * captionSize.scale
                                                ))
                                        } else {
                                            Text("[Vertaling niet voltooid]",
                                                style = MaterialTheme.typography.bodySmall)
                                        }
                                        TextButton(onClick = {
                                            val index = lines.indexOfFirst { it.id == line.id }
                                            pinsOpen = false
                                            if (index >= 0) {
                                                follow = false
                                                coroutineScope.launch {
                                                    listState.animateScrollToItem(index)
                                                }
                                            }
                                        }) { Text("Ga naar uitspraak") }
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                        if (pinsNotice.isNotBlank()) {
                            Text(pinsNotice, style = MaterialTheme.typography.bodySmall)
                        }
                        if (pinnedLines.isNotEmpty()) {
                            TextButton(onClick = {
                                captionPins = emptySet()
                                pinsNotice = "Alle markeringen van dit gesprek zijn gewist."
                            }) { Text("Wis markeringen") }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { pinsOpen = false }) { Text("Sluiten") }
                },
                dismissButton = {
                    TextButton(
                        enabled = pinnedLines.isNotEmpty() && !listening,
                        onClick = {
                            val snapshot = PrivateTranscriptExport.snapshot(
                                pinnedLines.map { line ->
                                    TranscriptExportRow(
                                        line.original, line.translated, line.status,
                                        line.sourceCode, line.targetCode, line.recordedAtMillis
                                    )
                                }
                            )
                            if (snapshot == null) {
                                pinsNotice = "Selectie kan niet veilig worden geëxporteerd."
                            } else {
                                pendingTranscriptExport = generation to snapshot
                                pinsOpen = false
                                saveTranscript.launch("Tuddy-gemarkeerde-zinnen.txt")
                            }
                        }
                    ) { Text("Bewaar selectie…") }
                }
            )
        }
        if (personalPhrasesOpen) {
            MyOfflinePhrasesPanel(
                deck = personalPhrases,
                initialLanguage = previewTarget,
                onDismiss = { personalPhrasesOpen = false }
            )
        }
        if (savedWordsOpen) {
            SavedWordsPanel(
                deck = savedVocabulary,
                revision = savedWordRevision,
                onChanged = { savedWordRevision++ },
                onDismiss = { savedWordsOpen = false }
            )
        }
        Surface(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Tuddy", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("Gesprekken begrijpen, ook zonder internet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Spreektaal: ${source.label}  ·  Vertaling: ${target.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
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
                        pendingTranscriptExport = null
                        exportStatus = ""
                        previewArchiveRows = null
                        archiveSearchQuery = ""
                        pendingArchiveGeneration = null
                        pendingArchiveExcerpt = null
                        archiveExcerptNotice = ""
                        copyStatus = ""
                        lastCopiedCaptionId = null
                        pendingCaptionShare = null
                        pendingSpeakerExport = null
                        editingCaptionId = null
                        captionCorrectionDraft = ""
                        correctionNotice = ""
                        stop()
                        offlineModelStatus = ""
                        lines.clear()
                        manuallyAssignedSpeakers.clear()
                        captionPins = emptySet()
                        pinsOpen = false
                        pinsNotice = ""
                        historySearchQuery = ""
                        historySearchOpen = false
                        directionNotice = ""
                        previewInput = ""
                        manualText = ""
                        previewResult = null
                        error = ""
                    }) { Text("Wis gesprek") }
                    TextButton(onClick = { follow = !follow }) {
                        Text(if (follow) "Live ✓" else "Volg live")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { focusMode = true }) { Text("⛶ Leesmodus") }
                    OutlinedButton(
                        enabled = reverseDirection != null,
                        onClick = {
                            val reversed = reverseDirection
                            val previousSource = source
                            val nextSource = reversed?.let { next ->
                                spoken.firstOrNull { it.speechCode == next.sourceSpeechTag }
                            }
                            if (reversed != null && nextSource != null) {
                                stop()
                                pendingCaptionShare = null
                                pendingSpeakerExport = null
                                pendingTranscriptExport = null
                                source = nextSource
                                target = LanguageChoice(
                                    previousSource.label, reversed.targetTextCode,
                                    reversed.targetTextCode
                                )
                                val persisted = runCatching {
                                    languageSettings.edit()
                                        .putString(PrivateLanguageSelectionPolicy.SOURCE_KEY, nextSource.speechCode)
                                        .putString(PrivateLanguageSelectionPolicy.TARGET_KEY, reversed.targetTextCode)
                                        .commit()
                                }.getOrDefault(false)
                                textReceipt = OfflineTextReceipt(
                                    nextSource.translateCode, reversed.targetTextCode,
                                    OfflineTextStage.UNCHECKED,
                                    "Gespreksrichting gewijzigd: controleer beide tekstmodellen."
                                )
                                speechPackReceipt = SpeechPackReceipt(
                                    nextSource.speechCode, SpeechPackStage.UNCHECKED,
                                    "Nieuwe spreekrichting: controleer het lokale Android-spraakpakket."
                                )
                                offlineModelStatus = ""
                                error = ""
                                showSettings = true
                                conversationToolsOpen = false
                                directionNotice = "Richting: ${nextSource.label} → ${previousSource.label}. " +
                                    "Microfoon gestopt. Controleer spraak- en tekstmodellen, " +
                                    "tik daarna opnieuw op Luister." +
                                    if (persisted) "" else " Deze keuze geldt alleen voor deze sessie."
                            }
                        }
                    ) { Text("⇄ Wissel richting") }
                }
                if (directionNotice.isNotBlank()) {
                    Text(directionNotice, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                OutlinedButton(
                    onClick = {
                        conversationToolsOpen = !conversationToolsOpen
                        if (conversationToolsOpen) showSettings = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (conversationToolsOpen) "Verberg gesprekstools"
                        else "Gesprekstools · export, sprekers en archief")
                }
                if (conversationToolsOpen) {
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 256.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Sprekers zelf markeren: tik op ‘Wijzig spreker’ bij een uitspraak. " +
                            "De labels zijn geen stemherkenning, worden niet geëxporteerd en " +
                            "verdwijnen wanneer deze gesprekssessie wordt gewist.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val countA = lines.count { manuallyAssignedSpeakers[it.id] == "A" }
                        val countB = lines.count { manuallyAssignedSpeakers[it.id] == "B" }
                        TextButton(enabled = countA > 0, onClick = { jumpToSpeaker("A") }) {
                            Text("Volgende A ($countA)")
                        }
                        TextButton(enabled = countB > 0, onClick = { jumpToSpeaker("B") }) {
                            Text("Volgende B ($countB)")
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        enabled = lines.isNotEmpty() && !listening,
                        onClick = {
                            exportStatus = ""
                            val snapshot = PrivateTranscriptExport.snapshot(
                                lines.map { line ->
                                    TranscriptExportRow(
                                        line.original, line.translated, line.status,
                                        line.sourceCode, line.targetCode,
                                    line.recordedAtMillis
                                    )
                                }
                            )
                            if (snapshot == null) {
                                error = "Kan dit gesprek niet veilig exporteren. Wis ongeldige tekst en probeer opnieuw."
                            } else {
                                pendingTranscriptExport = generation to snapshot
                                saveTranscript.launch("Tuddy-gesprek.txt")
                            }
                        }
                    ) { Text("Bewaar gesprek…") }
                    Text(
                        "Alleen op verzoek. Externe opslag kan synchroniseren.",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        enabled = lines.isNotEmpty() && !listening,
                        onClick = {
                            exportStatus = ""
                            val snapshot = PrivateTranscriptJsonExport.snapshot(
                                lines.map { line ->
                                    TranscriptExportRow(
                                        line.original, line.translated, line.status,
                                        line.sourceCode, line.targetCode, line.recordedAtMillis
                                    )
                                }
                            )
                            if (snapshot == null) {
                                error = "Kan dit gesprek niet veilig als JSON exporteren."
                            } else {
                                pendingTranscriptExport = generation to snapshot
                                saveTranscriptJson.launch("Tuddy-gesprek.json")
                            }
                        }
                    ) { Text("Bewaar JSON…") }
                    Text(
                        "Voor eigen archief of verwerking; onvoltooide vertalingen blijven leeg.",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        enabled = !listening,
                        onClick = {
                            pendingArchiveGeneration = generation
                            openTranscriptArchive.launch(arrayOf("application/json", "text/plain"))
                        }
                    ) { Text("Open JSON-archief…") }
                    Text(
                        "Bekijk een bewaard gesprek zonder je live gesprek te vervangen.",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        enabled = !listening && lines.any {
                            (it.status == "Lokaal vertaald" || it.status == "Lokaal") &&
                                (manuallyAssignedSpeakers[it.id] == "A" ||
                                    manuallyAssignedSpeakers[it.id] == "B")
                        },
                        onClick = {
                            val snapshot = captureSpeakerTranscript()
                            if (snapshot == null) {
                                exportStatus = "Geen afgerond gesprek met veilige sprekerlabels om te bewaren."
                            } else {
                                exportStatus = ""
                                pendingSpeakerExport = generation to snapshot
                            }
                        }
                    ) { Text("Bewaar gesprek met sprekerlabels…") }
                    Text(
                        "Afzonderlijke toestemming · alleen afgeronde zinnen · TXT",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                    }
                }
                if (exportStatus.isNotBlank()) Text(
                    exportStatus,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
                if (pronunciationNotice.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(pronunciationNotice, modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = {
                            captionSpeaker.stop()
                            pronunciationNotice = "Voorlezen gestopt."
                        }) { Text("Stop stem") }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { savedWordsOpen = true }) {
                        Text("Mijn woorden ($savedWordCount)")
                    }
                    Text(
                        "Zelf bewaarde basiswoorden · offline",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (savedWordNotice.isNotBlank()) {
                    Text(savedWordNotice, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(
                    onClick = {
                        showSettings = !showSettings
                        if (showSettings) conversationToolsOpen = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showSettings) "Verberg taal- en modelinstellingen" else "Taal- en modelinstellingen")
                }
                // Settings used to occupy the full unscrollable outer Column on
                // phones, starving the weighted transcript of its available height.
                // Keep listening/clear/follow controls pinned above a separately
                // scrollable, bounded setup area; default to the actual conversation.
                if (showSettings) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .weight(0.48f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                Text("Ondertitelgrootte", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CaptionSize.entries.forEach { option ->
                        TextButton(onClick = {
                            captionSize = option
                            val saved = runCatching {
                                captionPrefs.edit()
                                    .putString(CaptionDisplayPreferences.SIZE_KEY, option.name)
                                    .commit()
                            }.getOrDefault(false)
                            captionSizeMessage = if (saved) "" else
                                "Grootte geldt nu alleen voor deze sessie."
                        }) {
                            Text(if (captionSize == option) "${option.label} ✓" else option.label)
                        }
                    }
                }
                if (captionSizeMessage.isNotBlank()) {
                    Text(captionSizeMessage, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "Alleen de presentatie verandert; het gesprek en de vertaling blijven gelijk.",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(onClick = { sourceOpen = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Van: ${source.label}", maxLines = 1)
                        }
                        DropdownMenu(expanded = sourceOpen, onDismissRequest = { sourceOpen = false }) {
                            Text(
                                "Kiezen is niet hetzelfde als een geïnstalleerd offline spraakpakket.",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
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
                    Text("Alleen de elf gekozen spraakkandidaten; elk offline spraakpakket moet op dit toestel echt aanwezig zijn. " +
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
                    OutlinedButton(onClick = { personalPhrasesOpen = true }) {
                        Text("Mijn eigen Kurmanji/Sorani/Pasjtoe zinnen")
                    }
                    Text("Je kunt zelf zinnen en eigen betekenissen offline opslaan. " +
                        "Tuddy vertaalt of controleert jouw eigen zinnen niet automatisch.",
                        style = MaterialTheme.typography.bodySmall)
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
                                val removed = lines.removeAt(0)
                                manuallyAssignedSpeakers.remove(removed.id)
                                captionPins = captionPins - removed.id
                                if (editingCaptionId == removed.id) {
                                    editingCaptionId = null
                                    captionCorrectionDraft = ""
                                }
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
                            Text(if (preparingOfflineModel) "Tekstmodel voorbereiden…" else "Tekstmodel offline voorbereiden")
                        }
                    }
                    if (textReceipt.stage == OfflineTextStage.CHECKING) {
                        TextButton(onClick = {
                            textInventory.invalidate()
                            textReceipt = OfflineTextReceipt(source.translateCode, target.translateCode,
                                OfflineTextStage.UNCHECKED, "Tekstcontrole gestopt. Probeer opnieuw.")
                        }) { Text("Stop tekstcontrole") }
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
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        historySearchOpen = !historySearchOpen
                        if (historySearchOpen) follow = false
                        else historySearchQuery = ""
                    }, enabled = lines.isNotEmpty()) {
                        Text(if (historySearchOpen) "Sluit zoeken" else "Zoek in gesprek")
                    }
                    TextButton(
                        enabled = captionPins.isNotEmpty(),
                        onClick = { pinsOpen = true }
                    ) { Text("★ ${captionPins.size} gemarkeerd") }
                    if (historySearchOpen && normalizedHistoryQuery.isNotBlank()) {
                        Text("${historyMatches.size} gevonden",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (historySearchOpen) {
                    OutlinedTextField(
                        value = historySearchQuery,
                        onValueChange = { historySearchQuery = it.take(80) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Zoek originele tekst of vertaling") },
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { jumpToHistoryMatch(false) },
                            enabled = historyMatches.isNotEmpty()
                        ) { Text("← Vorige") }
                        OutlinedButton(
                            onClick = { jumpToHistoryMatch(true) },
                            enabled = historyMatches.isNotEmpty()
                        ) { Text("Volgende →") }
                    }
                }
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
                if (correctionNotice.isNotBlank()) {
                    Text(correctionNotice, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                if (!follow) {
                    Text(
                        "Teruggelezen: automatisch volgen is gepauzeerd. Tik op Volg live om de laatste regel te zien.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                HorizontalDivider()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth().nestedScroll(manualHistoryScroll),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (lines.isEmpty() && partial.isBlank()) {
                        item(key = "empty-conversation") {
                            Text(
                                "Nog geen ondertitels. Tik op Luister voor offline spraak " +
                                    "of open Taal- en modelinstellingen om een zin te typen. " +
                                    "Je gesprek blijft op dit apparaat in tijdelijk geheugen.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(lines, key = { it.id }) { line ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(
                                if (normalizedHistoryQuery.isNotBlank() && (
                                        line.original.contains(normalizedHistoryQuery, ignoreCase = true) ||
                                            line.translated.contains(normalizedHistoryQuery, ignoreCase = true)
                                    )) 2.dp else 1.dp,
                                if (normalizedHistoryQuery.isNotBlank() && (
                                        line.original.contains(normalizedHistoryQuery, ignoreCase = true) ||
                                            line.translated.contains(normalizedHistoryQuery, ignoreCase = true)
                                    )) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    DateFormat.getTimeInstance(DateFormat.SHORT)
                                        .format(Date(line.recordedAtMillis)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Origineel: ${line.sourceCode.uppercase()}  →  vertaling: ${line.targetCode.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val speaker = manuallyAssignedSpeakers[line.id]
                                    Text(
                                        if (speaker == null) "Spreker: onbekend (handmatig)"
                                        else "Spreker $speaker (handmatig)",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    TextButton(onClick = {
                                        when (speaker) {
                                            null -> manuallyAssignedSpeakers[line.id] = "A"
                                            "A" -> manuallyAssignedSpeakers[line.id] = "B"
                                            else -> manuallyAssignedSpeakers.remove(line.id)
                                        }
                                    }) { Text("Wijzig spreker") }
                                }
                                if (line.userCorrected) {
                                    Text("✎ Zelf gecorrigeerd · niet door Android gecontroleerd",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(
                                        enabled = !listening,
                                        onClick = {
                                            editingCaptionId = line.id
                                            captionCorrectionDraft = line.original
                                            correctionNotice = ""
                                        }
                                    ) { Text("✎ Corrigeer tekst") }
                                    if (line.status == "Fout" || line.status == "Model ontbreekt") {
                                        TextButton(
                                            enabled = !listening,
                                            onClick = {
                                                val index = lines.indexOfFirst { it.id == line.id }
                                                if (index >= 0) {
                                                    val old = lines[index]
                                                    captionSpeaker.stop()
                                                    pendingCaptionShare = null
                                                    pendingTranscriptExport = null
                                                    exportStatus = ""
                                                    lines[index] = old.copy(
                                                        translated = "Vertalen…",
                                                        status = "Bezig",
                                                        translationRevision = old.translationRevision + 1
                                                    )
                                                    translate(old.original, old.id, old.targetCode,
                                                        old.sourceCode, generation)
                                                    correctionNotice = "Lokale vertaling opnieuw aangevraagd."
                                                }
                                            }
                                        ) { Text("↻ Probeer opnieuw") }
                                    }
                                }
                                OfflineWordText(
                                    line.original, line.sourceCode,
                                    MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = MaterialTheme.typography.bodyLarge.fontSize * captionSize.scale
                                    ),
                                    onSaveWord = ::saveVerifiedWord
                                )
                                if (line.status == "Lokaal vertaald" || line.status == "Lokaal") {
                                    OfflineWordText(
                                        line.translated, line.targetCode,
                                        MaterialTheme.typography.titleMedium.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = MaterialTheme.typography.titleMedium.fontSize * captionSize.scale
                                        ),
                                        onSaveWord = ::saveVerifiedWord
                                    )
                                    TextButton(onClick = {
                                        if (listening) {
                                            pronunciationNotice =
                                                "Stop eerst de microfoon voordat je een vertaling laat voorlezen."
                                        } else {
                                            captionSpeaker.speak(line.translated, line.targetCode) {
                                                pronunciationNotice = it
                                            }
                                        }
                                    }) { Text("🔊 Spreek vertaling uit") }
                                } else {
                                    Text(
                                        line.translated,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = MaterialTheme.typography.titleMedium.fontSize * captionSize.scale
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(line.status, style = MaterialTheme.typography.labelSmall)
                                TextButton(onClick = {
                                    val before = captionPins
                                    val currentIds = lines.map { it.id }.toSet()
                                    captionPins = SessionCaptionPins.toggle(before, line.id, currentIds)
                                    pinsNotice = when {
                                        line.id in before -> "Markering verwijderd."
                                        line.id in captionPins -> "Uitspraak tijdelijk gemarkeerd."
                                        else -> "Maximaal ${SessionCaptionPins.MAX_PINS} markeringen."
                                    }
                                }) {
                                    Text(if (line.id in captionPins) "★ Gemarkeerd" else "☆ Markeer")
                                }
                                TextButton(onClick = {
                                    val payload = PrivateCaptionCopy.snapshot(
                                        TranscriptExportRow(
                                            line.original, line.translated, line.status,
                                            line.sourceCode, line.targetCode, line.recordedAtMillis
                                        )
                                    )
                                    lastCopiedCaptionId = line.id
                                    if (payload == null) {
                                        copyStatus = "Deze regel kan niet veilig worden gekopieerd."
                                    } else {
                                        val copied = runCatching {
                                            val clipboard = context.getSystemService(
                                                Context.CLIPBOARD_SERVICE
                                            ) as ClipboardManager
                                            val clip = ClipData.newPlainText("Tuddy privézin", payload)
                                            clip.description.extras = PersistableBundle().apply {
                                                putBoolean("android.content.extra.IS_SENSITIVE", true)
                                            }
                                            clipboard.setPrimaryClip(clip)
                                            true
                                        }.getOrDefault(false)
                                        copyStatus = if (copied) {
                                            "Zin gekopieerd. Het Android-klembord is buiten Tuddy zichtbaar."
                                        } else {
                                            "Kopiëren niet gelukt."
                                        }
                                    }
                                }) { Text("Kopieer deze zin") }
                                if ((line.status == "Lokaal vertaald" || line.status == "Lokaal") &&
                                    line.translated.isNotBlank()) {
                                    TextButton(onClick = {
                                        val payload = PrivateCaptionCopy.snapshot(
                                            TranscriptExportRow(
                                                line.original, line.translated, line.status,
                                                line.sourceCode, line.targetCode, line.recordedAtMillis
                                            )
                                        )
                                        if (payload == null) {
                                            lastCopiedCaptionId = line.id
                                            copyStatus = "Deze zin kan niet veilig worden gedeeld."
                                        } else {
                                            pendingCaptionShare = Triple(generation, line.id, payload)
                                            copyStatus = ""
                                        }
                                    }) { Text("Deel deze zin…") }
                                }
                                if (lastCopiedCaptionId == line.id && copyStatus.isNotBlank()) {
                                    Text(copyStatus, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    if (partial.isNotBlank()) {
                        item {
                            Card {
                                Column(Modifier.padding(14.dp)) {
                                    Text("● Spreekt nu…", color = MaterialTheme.colorScheme.primary)
                                    Text(
                                        partial,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = MaterialTheme.typography.bodyLarge.fontSize * captionSize.scale
                                        )
                                    )
                                    if (partialTranslation.isNotBlank()) {
                                        Text(partialTranslation,
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontSize = MaterialTheme.typography.titleMedium.fontSize * captionSize.scale
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Text(
                    "Privacy: gesprekken blijven tijdelijk, behalve bij expliciete export. " +
                        "Alleen bewust bewaarde basiswoorden en zelf ingevoerde zinnen worden privé op je telefoon opgeslagen. " +
                        "Eigen zinnen zijn niet automatisch vertaald of gecontroleerd. Voorlezen gebruikt Android-gemelde offline stemmen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
