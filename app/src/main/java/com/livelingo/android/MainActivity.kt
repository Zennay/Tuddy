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
import android.view.WindowManager
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
import com.livelingo.android.ui.OfflinePhraseQuickPicker
import com.livelingo.android.ui.MyOwnWordsPanel
import com.livelingo.android.ui.SessionMeaningIndexDialog
import com.livelingo.android.ui.SessionMeaningRow
import com.livelingo.android.ui.FocusTranscriptDialog
import com.livelingo.android.ui.SubtitleDraftControls
import com.livelingo.android.ui.FullscreenCaptionCard
import com.livelingo.android.ui.FocusCaptionLine
import com.livelingo.android.ui.SessionCaptionStudyDialog
import com.livelingo.android.ui.LiveInterimWordFocus
import com.livelingo.android.ui.OfflineStarterPhraseboard
import com.livelingo.android.ui.OfflineStudyFlashcards
import com.livelingo.android.ui.LocalCaptionPlaybackMenu
import com.livelingo.android.ui.OfflineReadinessDialog
import com.livelingo.android.ui.TwoWayTypedConversationDialog
import com.livelingo.android.ui.TypedConversationLine
import com.livelingo.android.ui.CaptionLanguageCorrectionDialog
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
    LanguageChoice("العربية · algemeen", "ar-SA", TranslateLanguage.ARABIC),
    // Historical v0.4 source hints for Levantine regional recognizer packs.
    // Candidates only: local Android speech-pack presence must be checked.
    LanguageChoice("العربية · Syrië (pakketkandidaat)", "ar-SY", TranslateLanguage.ARABIC),
    LanguageChoice("العربية · Palestina (pakketkandidaat)", "ar-PS", TranslateLanguage.ARABIC),
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
    val userCorrected: Boolean = false,
    // Human-authored session-only meaning: never overwrite model result or exports.
    val personalMeaning: String? = null
)

class MainActivity : ComponentActivity() {
    // Lives only in Activity memory; no savedInstanceState or external text logs.
    private var pendingExternalText by mutableStateOf<IncomingSharedText?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrivateScreenShield.restore(this)
        // A configuration recreation must never re-import private text from
        // the original Intent after it was already dismissed or consumed.
        pendingExternalText = if (savedInstanceState == null)
            SharedPlainTextIntent.read(intent) else null
        // Do not keep the original other-app text in Activity Intent extras.
        intent?.removeExtra(Intent.EXTRA_TEXT)
        intent?.removeExtra(Intent.EXTRA_PROCESS_TEXT)
        setContent {
            TuddyScreen(
                incomingExternalText = pendingExternalText,
                onExternalTextConsumed = { pendingExternalText = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingExternalText = SharedPlainTextIntent.read(intent)
        intent.removeExtra(Intent.EXTRA_TEXT)
        intent.removeExtra(Intent.EXTRA_PROCESS_TEXT)
        setIntent(intent)
    }
}

@Composable
private fun TuddyScreen(
    incomingExternalText: IncomingSharedText?,
    onExternalTextConsumed: () -> Unit
) {
    val context = LocalContext.current
    val screenActivity = remember(context) { PrivateScreenShield.activity(context) }
    var privateScreenShieldOn by remember(context) {
        mutableStateOf(PrivateScreenShield.enabled(context))
    }
    var privateScreenShieldNotice by remember { mutableStateOf("") }
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
    val captionHaptics = remember(context) { CaptionHapticCues(context) }
    var hapticAlertsOn by remember(context) { mutableStateOf(captionHaptics.enabled()) }
    var hapticNotice by remember { mutableStateOf("") }
    var keepScreenAwakeOn by remember(context) {
        mutableStateOf(LiveCaptionScreenAwake.enabled(context))
    }
    var keepScreenAwakeNotice by remember { mutableStateOf("") }
    val savedVocabulary = remember(context) { PrivateVocabularyDeck(context) }
    var savedWordRevision by remember { mutableIntStateOf(0) }
    val savedWordCount = remember(savedWordRevision) { savedVocabulary.entries().size }
    var savedWordsOpen by remember { mutableStateOf(false) }
    val ownWordDeck = remember(context) { PrivateOwnWordDeck(context) }
    var ownWordRevision by remember { mutableIntStateOf(0) }
    var ownWordsOpen by remember { mutableStateOf(false) }
    var savedWordNotice by remember { mutableStateOf("") }
    fun saveOwnWordMeaning(language: String, word: String, meaning: String): Boolean {
        val saved = ownWordDeck.save(language, word, meaning)
        if (saved) ownWordRevision++
        return saved
    }
    fun readOwnWordMeaning(language: String, word: String): String? {
        // Recompose dependent word cards when a user explicitly edits a note.
        ownWordRevision
        return ownWordDeck.find(language, word)?.ownMeaning
    }

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
    // Only picker codes, manually pinned language pairs and reviewed words are kept
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
    var quickPhrasePickerOpen by remember { mutableStateOf(false) }
    var starterPhraseboardOpen by remember { mutableStateOf(false) }
    var starterPhraseboardTarget by remember { mutableStateOf("kmr") }
    var flashcardsOpen by remember { mutableStateOf(false) }
    var previewSource by remember { mutableStateOf("nl") }
    var previewMenuOpen by remember { mutableStateOf(false) }
    var previewInput by remember { mutableStateOf("") }
    var manualText by remember { mutableStateOf("") }
    var quickTypedOpen by remember { mutableStateOf(false) }
    var twoWayTypedOpen by remember { mutableStateOf(false) }
    // Other-app text exists only in this Activity session and only after
    // the user consciously moves it into the text-only conversation editor.
    var sharedTextToTwoWay by remember { mutableStateOf<String?>(null) }
    var draftFromExternalShare by remember { mutableStateOf(false) }
    // The export snapshot exists only between an explicit Save tap and the
    // Android document picker result; no automatic disk/history persistence.
    var pendingTranscriptExport by remember { mutableStateOf<Pair<Int, ByteArray>?>(null) }
    var subtitleConsentOpen by remember { mutableStateOf(false) }
    var printableLiveConsentOpen by remember { mutableStateOf(false) }
    var studyBookConsentOpen by remember { mutableStateOf(false) }
    // Deliberately selected private learning material only, never captions/audio.
    var pendingCombinedStudyBook by remember { mutableStateOf<ByteArray?>(null) }
    var printableArchiveConsentOpen by remember { mutableStateOf(false) }
    var printableOnlyFinished by remember { mutableStateOf(true) }
    // Generation, selected filter, and exact opt-in RAM snapshot.
    var pendingPrintableLive by remember {
        mutableStateOf<Triple<Int, Boolean, ByteArray>?>(null)
    }
    var pendingPrintableArchive by remember {
        mutableStateOf<Pair<Int, ByteArray>?>(null)
    }
    var subtitleOptions by remember { mutableStateOf(SubtitleDraftOptions()) }
    var exportStatus by remember { mutableStateOf("") }
    // The read-only archive is not part of the current live conversation.
    var previewArchiveRows by remember { mutableStateOf<List<TranscriptExportRow>?>(null) }
    var archiveStudyOpen by remember { mutableStateOf(false) }
    var archiveReaderOpen by remember { mutableStateOf(false) }
    var archiveSubtitleConsentOpen by remember { mutableStateOf(false) }
    var pendingArchiveSubtitleExport by remember { mutableStateOf<Pair<Int, ByteArray>?>(null) }
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
    val microphoneLimit = remember(context) { MicrophoneSessionLimit(context) }
    var microphoneLimitMinutes by remember(context) {
        mutableIntStateOf(microphoneLimit.selectedMinutes())
    }
    var microphoneLimitNotice by remember { mutableStateOf("") }
    val textInventory = remember { OfflineTextModelInventory() }
    // App-local text-model storage; installed packages are queried on demand,
    // never inferred from the supported-language catalogue.
    val modelStorage = remember { OfflineTextModelStorage() }
    var modelStorageOpen by remember { mutableStateOf(false) }
    var installedTextModels by remember { mutableStateOf<List<String>?>(null) }
    var modelStorageBusy by remember { mutableStateOf(false) }
    var modelStorageNotice by remember { mutableStateOf("") }
    var pendingModelDeletion by remember { mutableStateOf<String?>(null) }
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
    var offlineReadinessOpen by remember { mutableStateOf(false) }
    // Secondary export/history actions are collapsed on compact screens.
    var conversationToolsOpen by remember { mutableStateOf(false) }
    var directionNotice by remember { mutableStateOf("") }
    val favoriteStorage = remember(context) { PrivateLanguagePairFavorites(context) }
    val typedFavoriteStorage = remember(context) { PrivateTypedLanguagePairs(context) }
    val allowedSpeechTags = remember { spoken.map { it.speechCode } }
    var favoritePairs by remember {
        mutableStateOf(favoriteStorage.entries(allowedSpeechTags))
    }
    var favoritesOpen by remember { mutableStateOf(false) }
    var favoritesNotice by remember { mutableStateOf("") }
    var pendingStudyErase by remember { mutableStateOf(false) }
    var studyEraseNotice by remember { mutableStateOf("") }
    var focusMode by remember { mutableStateOf(false) }
    var sessionCaptionStudyOpen by remember { mutableStateOf(false) }
    var sessionCaptionStudyStartId by remember { mutableStateOf<Long?>(null) }
    var presentedCaptionId by remember { mutableStateOf<Long?>(null) }
    // Local search stays in memory, never persisted or sent to an API.
    // Editing is opt-in and ephemeral: never persist drafts or silently rewrite audio.
    var editingCaptionId by remember { mutableStateOf<Long?>(null) }
    // Selected caption id, translation revision and conversation generation.
    var pendingCaptionLanguageChange by remember {
        mutableStateOf<Triple<Long, Int, Int>?>(null)
    }
    var captionCorrectionDraft by remember { mutableStateOf("") }
    var editingMeaning by remember { mutableStateOf<Triple<Long, String, Int>?>(null) }
    var meaningDraft by remember { mutableStateOf("") }
    var meaningNotice by remember { mutableStateOf("") }
    var meaningIndexOpen by remember { mutableStateOf(false) }
    // One explicitly chosen row; keep its original and revision so confirmation
    // cannot accidentally delete a different or subsequently corrected caption.
    var pendingSingleCaptionDelete by remember {
        mutableStateOf<Triple<Long, String, Int>?>(null)
    }
    var captionDeleteNotice by remember { mutableStateOf("") }
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

    fun captureCombinedStudyBook(): ByteArray? =
        PrivateCombinedStudyBook.snapshot(
            savedVocabulary.entries(), ownWordDeck.entries(), personalPhrases.entries()
        )

    fun capturePrintableConversation(onlyFinished: Boolean): ByteArray? =
        PrivatePrintableConversation.snapshot(lines.map { line ->
            TranscriptExportRow(line.original, line.translated, line.status,
                line.sourceCode, line.targetCode, line.recordedAtMillis)
        }, onlyFinished)

    fun captureSubtitleDraft(): ByteArray? =
        PrivateSubtitleDraftExport.snapshot(lines.map { line ->
            TranscriptExportRow(
                line.original, line.translated, line.status,
                line.sourceCode, line.targetCode, line.recordedAtMillis
            )
        }, subtitleOptions)

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

    /**
     * One bounded local translation pipeline for speech, corrections, and
     * explicitly typed two-way turns. A user-selected typed source language
     * must NOT be silently overwritten by automatic short-text detection.
     */
    fun translateResolved(
        text: String, id: Long, from: String, targetCode: String,
        token: Int, revision: Int
    ) {
        if (!translationStillRelevant(id, token, revision)) return
        if (from !in TargetCatalog.codes || targetCode !in TargetCatalog.codes) {
            updateLine(id, "Dit teksttaalpaar wordt lokaal niet ondersteund.", "Fout", token, revision)
            return
        }
        val row = lines.indexOfFirst { it.id == id && it.translationRevision == revision }
        if (row < 0) return
        lines[row] = lines[row].copy(sourceCode = from)
        if (from == targetCode) {
            updateLine(id, text, "Lokaal", token, revision)
            return
        }
        // Inventory is read-only. A missing model is never downloaded merely
        // because somebody typed, corrected or spoke a private sentence.
        OfflineTextModelInventory().check(from, targetCode, TargetCatalog.codes) { receipt ->
            if (!translationStillRelevant(id, token, revision)) return@check
            when (receipt.stage) {
                OfflineTextStage.CHECKING -> Unit
                OfflineTextStage.READY -> {
                    val translator = try {
                        Translation.getClient(TranslatorOptions.Builder()
                            .setSourceLanguage(from)
                            .setTargetLanguage(targetCode)
                            .build())
                    } catch (_: Exception) {
                        updateLine(id, SafeLocalFailureText.TRANSLATION, "Fout", token, revision)
                        return@check
                    }
                    try {
                        translator.translate(text)
                            .addOnSuccessListener { output ->
                                updateLine(id, output, "Lokaal vertaald", token, revision)
                                translator.close()
                            }
                            .addOnFailureListener {
                                updateLine(id, SafeLocalFailureText.TRANSLATION, "Fout", token, revision)
                                translator.close()
                            }
                    } catch (_: Exception) {
                        translator.close()
                        updateLine(id, SafeLocalFailureText.TRANSLATION, "Fout", token, revision)
                    }
                }
                OfflineTextStage.MISSING -> updateLine(
                    id, "Lokaal tekstmodel ontbreekt ($from → $targetCode). " +
                        "Bereid dit teksttaalpaar bewust via wifi voor.",
                    "Model ontbreekt", token, revision
                )
                else -> updateLine(id,
                    "Lokale tekstmodelstatus niet bevestigd. Controleer modellen in instellingen.",
                    "Fout", token, revision)
            }
        }
    }

    fun translate(
        text: String, id: Long, targetCode: String, sourceCode: String,
        token: Int, explicitlyChosenSource: Boolean = false
    ) {
        val revision = lines.firstOrNull { it.id == id }?.translationRevision ?: return
        if (!translationStillRelevant(id, token, revision)) return
        if (explicitlyChosenSource) {
            translateResolved(text, id, sourceCode, targetCode, token, revision)
            return
        }
        // Speech recognition may provide a broad language tag. This local-only
        // detector is advisory and never upgrades unsupported model coverage.
        val detector = try { LanguageIdentification.getClient() }
            catch (_: Exception) {
                translateResolved(text, id, sourceCode, targetCode, token, revision)
                return
            }
        try {
            detector.identifyLanguage(text)
                .addOnSuccessListener { guess ->
                    detector.close()
                    if (!translationStillRelevant(id, token, revision)) return@addOnSuccessListener
                    val detected = guess.takeIf { it != "und" }
                        ?.let { TranslateLanguage.fromLanguageTag(it) }
                        ?.takeIf { it in TargetCatalog.codes }
                    translateResolved(text, id, detected ?: sourceCode,
                        targetCode, token, revision)
                }
                .addOnFailureListener {
                    detector.close()
                    translateResolved(text, id, sourceCode, targetCode, token, revision)
                }
        } catch (_: Exception) {
            detector.close()
            translateResolved(text, id, sourceCode, targetCode, token, revision)
        }
    }

    // Explicit offline text preparation: one supported source->target model,
    // never a hidden bulk download or an unreviewed speech recognition model.
    // Existing ML Kit model downloads cannot be stopped by clearing text;
    // stale UI completions are suppressed and the client is always closed.
    // Local read-only inventory. A green speech model is not evidence
    // that both ML Kit TEXT language models are present, or vice versa.
    fun checkSpeechPackNow() {
        if (!offlineAvailable || listening) return
        val tag = source.speechCode
        speechPacks.check(tag) { receipt ->
            if (source.speechCode == receipt.tag &&
                lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                speechPackReceipt = receipt
            }
        }
    }

    fun downloadSpeechPackNow() {
        if (listening || source.speechCode != speechPackReceipt.tag ||
            !speechPackReceipt.canDownload) return
        val tag = source.speechCode
        speechPacks.download(tag) { receipt ->
            if (source.speechCode == receipt.tag &&
                lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                speechPackReceipt = receipt
            }
        }
    }

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

    fun refreshInstalledTextModels() {
        if (modelStorageBusy) return
        modelStorageBusy = true
        modelStorageNotice = "Lokaal gedownloade tekstmodellen lezen…"
        modelStorage.list { codes, message ->
            modelStorageBusy = false
            installedTextModels = codes
            modelStorageNotice = message.ifBlank {
                if (codes.isNullOrEmpty()) "Geen extra ML Kit-tekstmodellen gevonden."
                else "${codes.size} lokaal gedownloade tekstmodellen gevonden."
            }
        }
    }

    fun removeUnusedTextModel(code: String) {
        if (modelStorageBusy || listening || preparingOfflineModel ||
            code !in installedTextModels.orEmpty() ||
            code in setOf(source.translateCode, target.translateCode)) return
        pendingModelDeletion = null
        sourceOpen = false
        targetOpen = false
        modelStorageBusy = true
        modelStorageNotice = "Gekozen tekstmodel verwijderen…"
        modelStorage.removeUnused(
            code,
            selectedPair = { setOf(source.translateCode, target.translateCode) }
        ) { success, message ->
            modelStorageBusy = false
            modelStorageNotice = message
            installedTextModels = null  // never keep presenting an old inventory
            if (success) {
                // The inventory and currently selected pair are independent;
                // always re-read actual model status after storage changes.
                textInventory.invalidate()
                textReceipt = OfflineTextReceipt(source.translateCode, target.translateCode,
                    OfflineTextStage.UNCHECKED, "Opslag gewijzigd: controleer het teksttaalpaar opnieuw.")
            }
        }
    }

    fun prepareOfflinePair() {
        if (preparingOfflineModel) return
        if (modelStorageBusy) return
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
        microphoneLimit.cancel() // No timeout can outlive Stop or background.
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
                        // Only real completed speech recognition triggers a short cue:
                        // not interim results, typed text, retries or translations.
                        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
                            captionHaptics.notifyCompletedSpeechCaption()
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
                            if (editingMeaning?.first == removed.id) {
                                editingMeaning = null
                                meaningDraft = ""
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
            // Continuous speech recognizer restarts must NOT extend a
            // user-chosen capture deadline. Only the original tap arms it.
            if (!forceRestart && !microphoneLimit.arm {
                    if (listening) {
                        stop()
                        error = "Microfoon automatisch gestopt na de ingestelde tijdslimiet. " +
                            "Je huidige gesprek blijft bewaard; tik Luister om opnieuw te beginnen."
                    }
                }) {
                stop()
                error = "De microfoontimer kon niet veilig worden gestart. " +
                    "De microfoon is daarom gestopt."
            }
        } catch (exception: Exception) {
            // startListening may throw after a recognizer was allocated.
            // Do not leave that session eligible for callbacks or reuse.
            microphoneLimit.cancel()
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

    /**
     * Share the exact existing transcript/history and local ML Kit pipeline.
     * Typed input is separate from the microphone and is never automatically
     * persisted or sent to a speech service.
     */
    fun chooseFavoritePair(pair: FavoriteLanguagePair) {
        if (modelStorageBusy || preparingOfflineModel) {
            favoritesNotice = "Wacht tot de huidige modelactie afgerond is."
            return
        }
        // Resolve against the CURRENT allowlists, never trust stored codes.
        val nextSource = spoken.firstOrNull { it.speechCode == pair.speechTag } ?: return
        val nextTarget = TargetCatalog.targets.firstOrNull { it.code == pair.targetCode }
            ?: return
        stop()
        modelStorage.invalidate()
        installedTextModels = null
        pendingModelDeletion = null
        pendingCaptionShare = null
        pendingSpeakerExport = null
        pendingTranscriptExport = null
        pendingArchiveExcerpt = null
        exportStatus = ""
        sourceOpen = false
        targetOpen = false
        targetQuery = ""
        source = nextSource
        target = LanguageChoice(nextTarget.title, nextTarget.code, nextTarget.code)
        textReceipt = OfflineTextReceipt(nextSource.translateCode, nextTarget.code,
            OfflineTextStage.UNCHECKED, "Nieuw favoriet taalpaar: controleer beide lokale tekstmodellen.")
        speechPackReceipt = SpeechPackReceipt(nextSource.speechCode, SpeechPackStage.UNCHECKED,
            "Nieuw favoriet taalpaar: controleer eerst het offline spraakpakket.")
        offlineModelStatus = ""
        error = ""
        val saved = runCatching {
            languageSettings.edit()
                .putString(PrivateLanguageSelectionPolicy.SOURCE_KEY, nextSource.speechCode)
                .putString(PrivateLanguageSelectionPolicy.TARGET_KEY, nextTarget.code)
                .commit()
        }.getOrDefault(false)
        showSettings = true
        conversationToolsOpen = false
        favoritesOpen = false
        favoritesNotice = "Taalpaar gekozen: ${nextSource.label} → ${nextTarget.title}. " +
            "Microfoon gestopt; controleer beide lokale modellen voordat je opnieuw luistert." +
            if (saved) "" else " Deze keuze geldt alleen voor deze sessie."
    }

    /**
     * Shared RAM-only insertion for all user-typed turns. The selected TEXT
     * language pair does not change the microphone's speech-pack selection.
     */
    fun appendTypedTurn(rawText: String, sourceCode: String, targetCode: String): Boolean {
        val sentence = OfflineTypedCaptionPolicy.normalize(rawText) ?: return false
        if (listening || sourceCode !in TargetCatalog.codes ||
            targetCode !in TargetCatalog.codes) return false
        val id = System.nanoTime()
        val token = generation
        lines.add(Line(id, sentence, "Vertalen…", "Bezig", sourceCode, targetCode))
        repeat(CaptionRetentionPolicy.oldestToDrop(lines.size)) {
            val removed = lines.removeAt(0)
            manuallyAssignedSpeakers.remove(removed.id)
            captionPins = captionPins - removed.id
            if (editingCaptionId == removed.id) {
                editingCaptionId = null
                pendingCaptionLanguageChange = null
                captionCorrectionDraft = ""
            }
        }
        // Short manually identified phrases must never be silently relabelled
        // as a different detected language before private local translation.
        translate(sentence, id, targetCode, sourceCode, token,
            explicitlyChosenSource = true)
        return true
    }

    fun submitTypedCaption(): Boolean {
        val saved = appendTypedTurn(manualText, source.translateCode, target.translateCode)
        if (saved) manualText = ""
        return saved
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

    val saveSubtitleDraft = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/x-subrip")
    ) { uri ->
        val pending = pendingTranscriptExport
        pendingTranscriptExport = null
        if (uri != null && pending != null && pending.first == generation) {
            // Do not export an outdated snapshot after the Android picker
            // returns: the live conversation may have been revised meanwhile.
            val current = captureSubtitleDraft()
            if (current == null || !current.contentEquals(pending.second)) {
                exportStatus = "Het gesprek is gewijzigd. Kies opnieuw ondertitels bewaren."
            } else {
                val saved = runCatching {
                    context.contentResolver.openOutputStream(uri, "w")?.use {
                        it.write(pending.second)
                        it.flush()
                        true
                    } ?: false
                }.getOrDefault(false)
                if (saved) {
                    exportStatus = "SRT-ondertitelconcept opgeslagen. Controleer alle tijden in je video-editor."
                } else {
                    error = "SRT niet opgeslagen. Kies een andere locatie."
                }
            }
        }
    }

    // WebVTT uses the same explicit consent, bounded draft and snapshot check
    // as SRT, but preserves the distinct browser/video-editor file format.
    val saveWebVttDraft = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/vtt")
    ) { uri ->
        val pending = pendingTranscriptExport
        pendingTranscriptExport = null
        if (uri != null && pending != null && pending.first == generation) {
            val current = captureSubtitleDraft()
            if (subtitleOptions.format != SubtitleDraftFormat.WEBVTT ||
                current == null || !current.contentEquals(pending.second)) {
                exportStatus = "Het gesprek is gewijzigd. Kies opnieuw ondertitels bewaren."
            } else {
                val saved = runCatching {
                    context.contentResolver.openOutputStream(uri, "w")?.use {
                        it.write(pending.second)
                        it.flush()
                        true
                    } ?: false
                }.getOrDefault(false)
                if (saved) {
                    exportStatus = "WebVTT-concept opgeslagen. Controleer alle tijden in je video-editor."
                } else {
                    error = "WebVTT niet opgeslagen. Kies een andere locatie."
                }
            }
        }
    }

    // The consented snapshot survives SAF's external picker, which causes
    // ON_STOP to clear the read-only archive preview but not the user's export.
    val saveArchivedSubtitleDraft = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/x-subrip")
    ) { uri ->
        val pending = pendingArchiveSubtitleExport
        pendingArchiveSubtitleExport = null
        if (uri != null && pending != null && pending.first == generation) {
            val saved = runCatching {
                context.contentResolver.openOutputStream(uri, "w")?.use {
                    it.write(pending.second)
                    it.flush()
                    true
                } ?: false
            }.getOrDefault(false)
            if (saved) {
                exportStatus = "Archief als SRT-concept opgeslagen. " +
                    "Tijden zijn geschat; lijn de zinnen zelf uit met de audio."
            } else {
                error = "Archief-SRT niet opgeslagen. Kies een andere locatie."
            }
        }
    }

    // An archive is read-only and cleared from the screen on background.
    // Only the consented snapshot and generation survive Android's picker.
    val saveArchivedWebVttDraft = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/vtt")
    ) { uri ->
        val pending = pendingArchiveSubtitleExport
        pendingArchiveSubtitleExport = null
        if (uri != null && pending != null && pending.first == generation) {
            val saved = runCatching {
                context.contentResolver.openOutputStream(uri, "w")?.use {
                    it.write(pending.second)
                    it.flush()
                    true
                } ?: false
            }.getOrDefault(false)
            if (saved) {
                exportStatus = "Archief als WebVTT-concept opgeslagen. " +
                    "Controleer de geschatte tijden zelf in je editor."
            } else {
                error = "Archief-WebVTT niet opgeslagen. Kies een andere locatie."
            }
        }
    }

    val saveCombinedStudyBook = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/html")
    ) { uri ->
        val pending = pendingCombinedStudyBook
        pendingCombinedStudyBook = null
        if (uri != null && pending != null) {
            val current = captureCombinedStudyBook()
            if (current == null || !current.contentEquals(pending)) {
                exportStatus = "Je eigen leermateriaal is gewijzigd. Maak het studieboek opnieuw."
            } else {
                val saved = runCatching {
                    context.contentResolver.openOutputStream(uri, "w")?.use {
                        it.write(pending)
                        it.flush()
                        true
                    } ?: false
                }.getOrDefault(false)
                exportStatus = if (saved)
                    "Het afdrukbare offline studieboek is opgeslagen op je gekozen locatie."
                else "Studieboek niet opgeslagen. Je lokale leermateriaal blijft behouden."
            }
        }
    }

    val savePrintableLive = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/html")
    ) { uri ->
        val pending = pendingPrintableLive
        pendingPrintableLive = null
        if (uri != null && pending != null && pending.first == generation) {
            val fresh = capturePrintableConversation(pending.second)
            if (fresh == null || !fresh.contentEquals(pending.third)) {
                exportStatus = "Het gesprek is gewijzigd. Kies afdrukbaar gesprek opnieuw."
            } else {
                val saved = runCatching {
                    context.contentResolver.openOutputStream(uri, "w")?.use {
                        it.write(pending.third)
                        it.flush()
                        true
                    } ?: false
                }.getOrDefault(false)
                exportStatus = if (saved)
                    "Afdrukbaar gesprek opgeslagen als offline HTML."
                else "Afdrukbaar gesprek niet opgeslagen."
            }
        }
    }

    val savePrintableArchive = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/html")
    ) { uri ->
        val pending = pendingPrintableArchive
        pendingPrintableArchive = null
        // A user-chosen archive closes when Android's picker backgrounds the
        // activity. This user-consented bounded snapshot can still be saved.
        if (uri != null && pending != null && pending.first == generation) {
            val saved = runCatching {
                context.contentResolver.openOutputStream(uri, "w")?.use {
                    it.write(pending.second)
                    it.flush()
                    true
                } ?: false
            }.getOrDefault(false)
            exportStatus = if (saved) "Afdrukbaar privéarchief opgeslagen als offline HTML."
                else "Afdrukbaar privéarchief niet opgeslagen."
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
            archiveSubtitleConsentOpen = false
            pendingArchiveSubtitleExport = null
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
                modelStorage.invalidate()
                modelStorageBusy = false
                pendingModelDeletion = null
                installedTextModels = null
            }
            if (event == Lifecycle.Event.ON_STOP) {
                offlineReadinessOpen = false
                starterPhraseboardOpen = false
                quickPhrasePickerOpen = false
                flashcardsOpen = false
                ownWordsOpen = false
                focusMode = false
                sessionCaptionStudyOpen = false
                sessionCaptionStudyStartId = null
                presentedCaptionId = null
                meaningIndexOpen = false
                quickTypedOpen = false
                twoWayTypedOpen = false // forget private typed draft on background
                sharedTextToTwoWay = null
                draftFromExternalShare = false
                manualText = ""
                previewArchiveRows = null
                archiveStudyOpen = false
                archiveReaderOpen = false
                archiveSearchQuery = ""
                pendingArchiveExcerpt = null
                archiveExcerptNotice = ""
                pendingCaptionShare = null
                pendingSpeakerExport = null
                editingCaptionId = null
                pendingCaptionLanguageChange = null // dismiss sensitive caption tool on background
                captionCorrectionDraft = ""
                pendingSingleCaptionDelete = null
                pendingStudyErase = false
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
            modelStorage.invalidate()
            starterPhraseboardOpen = false
            quickPhrasePickerOpen = false
            flashcardsOpen = false
            ownWordsOpen = false
            twoWayTypedOpen = false
            sharedTextToTwoWay = null
            presentedCaptionId = null
            sessionCaptionStudyOpen = false
            sessionCaptionStudyStartId = null
            pendingCaptionLanguageChange = null
            meaningIndexOpen = false
            stop() // stop() already releases and nulls Android's recognizer
            previewArchiveRows = null
            archiveStudyOpen = false
            archiveReaderOpen = false
            archiveSearchQuery = ""
            pendingArchiveGeneration = null
            pendingArchiveExcerpt = null
            archiveExcerptNotice = ""
            quickTypedOpen = false
            draftFromExternalShare = false
            manualText = ""
            pendingSpeakerExport = null
            editingCaptionId = null
            captionCorrectionDraft = ""
            pendingSingleCaptionDelete = null
            pendingStudyErase = false
            manuallyAssignedSpeakers.clear()
            captionSpeaker.close()
        }
    }

    // Android window flag is present ONLY during a consented listening session.
    // It never acquires a wake lock, starts the microphone or overrides Stop.
    DisposableEffect(screenActivity, listening, keepScreenAwakeOn) {
        val active = screenActivity != null && listening && keepScreenAwakeOn
        if (active) {
            screenActivity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            if (active) screenActivity?.window
                ?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Android Sharesheet text is only offered as an editable RAM draft.
    // No auto-submit, network call, persistence or microphone permission.
    LaunchedEffect(incomingExternalText) {
        val shared = incomingExternalText
        if (shared != null) {
            if (shared.draft == null) {
                error = shared.notice ?: "Deze gedeelde inhoud is geen ondersteunde platte tekst."
                quickTypedOpen = false
                twoWayTypedOpen = false // forget private typed draft on background
                sharedTextToTwoWay = null
                draftFromExternalShare = false
                manualText = ""
            } else {
                stop()
                manualText = shared.draft
                quickTypedOpen = true
                draftFromExternalShare = true
                error = ""
            }
            onExternalTextConsumed()
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
        if (offlineReadinessOpen) {
            OfflineReadinessDialog(
                spokenLabel = source.label,
                targetLabel = target.label,
                speechTag = source.speechCode,
                sourceTextCode = source.translateCode,
                targetTextCode = target.translateCode,
                speechServicePresent = offlineAvailable,
                speech = speechPackReceipt,
                textModel = textReceipt,
                isListening = listening,
                isPreparingText = preparingOfflineModel,
                isModelStorageBusy = modelStorageBusy,
                downloadNotice = offlineModelStatus,
                onCheckBoth = {
                    checkSpeechPackNow()
                    checkTextPair()
                },
                onCheckSpeech = { checkSpeechPackNow() },
                onDownloadSpeech = { downloadSpeechPackNow() },
                onCheckText = { checkTextPair() },
                onPrepareText = { prepareOfflinePair() },
                onEditLanguages = {
                    offlineReadinessOpen = false
                    conversationToolsOpen = false
                    favoritesOpen = false
                    showSettings = true
                },
                onOpenTyped = {
                    offlineReadinessOpen = false
                    quickTypedOpen = true
                },
                onDismiss = { offlineReadinessOpen = false }
            )
        }
        if (twoWayTypedOpen && !listening) {
            TwoWayTypedConversationDialog(
                initialSource = source.translateCode,
                initialTarget = target.translateCode,
                initialSharedDraft = sharedTextToTwoWay,
                recentLines = lines.takeLast(80).map { row ->
                    TypedConversationLine(row.original, row.translated, row.status,
                        row.sourceCode, row.targetCode)
                },
                retryableCount = { from, to ->
                    lines.count { it.sourceCode == from && it.targetCode == to &&
                        it.status == "Model ontbreekt" }
                },
                onRetryMissing = { from, to ->
                    if (listening || from !in TargetCatalog.codes ||
                        to !in TargetCatalog.codes || from == to) 0
                    else {
                        val candidates = lines.filter {
                            it.sourceCode == from && it.targetCode == to &&
                                it.status == "Model ontbreekt"
                        }.take(12)
                        candidates.forEach { row ->
                            val index = lines.indexOfFirst { it.id == row.id }
                            if (index >= 0 && lines[index] == row) {
                                // Fence stale async completions from old attempts.
                                lines[index] = row.copy(translated = "Vertalen…",
                                    status = "Bezig",
                                    translationRevision = row.translationRevision + 1)
                                translate(row.original, row.id, to, from, generation,
                                    explicitlyChosenSource = true)
                            }
                        }
                        candidates.size
                    }
                },
                onSend = { sentence, from, to ->
                    val added = appendTypedTurn(sentence, from, to)
                    if (added) {
                        follow = true
                        sharedTextToTwoWay = null
                    }
                    added
                },
                onSaveCuratedWord = ::saveVerifiedWord,
                readOwnMeaning = ::readOwnWordMeaning,
                onSaveOwnMeaning = ::saveOwnWordMeaning,
                onOpenLimitedPhraseboard = { selected ->
                    if (selected in setOf("kmr", "ckb", "ps")) {
                        // Never add an unsupported code to ML Kit text/speech
                        // language pickers or claim a speech model is present.
                        starterPhraseboardTarget = selected
                        twoWayTypedOpen = false
                        sharedTextToTwoWay = null
                        starterPhraseboardOpen = true
                    }
                },
                onDismiss = {
                    twoWayTypedOpen = false
                    sharedTextToTwoWay = null
                }
            )
        }
        if (quickTypedOpen) {
            AlertDialog(
                onDismissRequest = {
                    quickTypedOpen = false
                    draftFromExternalShare = false
                    manualText = ""
                },
                title = { Text(if (draftFromExternalShare) "Gedeelde tekst · eerst controleren"
                    else "Zin typen · lokaal vertalen") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Van ${source.label} naar ${target.label}. Geen microfoon nodig. " +
                                "Bij ontbrekende ML Kit-tekstmodellen kan eerst wifi nodig zijn.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (draftFromExternalShare) {
                            Text(
                                "Deze tekst kwam uit een andere app. Hij wordt pas na jouw " +
                                    "bevestiging in het tijdelijke gesprek opgenomen en lokaal " +
                                    "vertaald. Controleer de inhoud en taal zelf.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (draftFromExternalShare) {
                            TextButton(onClick = {
                                // Keep the already-reviewed draft in RAM while the
                                // user adjusts the actual target/source picker.
                                quickTypedOpen = false
                                showSettings = true
                                conversationToolsOpen = false
                            }) { Text("Wijzig talen · bewaar dit concept") }
                            OutlinedButton(onClick = {
                                // This is a separate explicit route for all 59
                                // offline ML Kit text languages, not speech ASR.
                                val incoming = OfflineTypedCaptionPolicy.normalize(manualText)
                                if (incoming != null && !listening) {
                                    sharedTextToTwoWay = incoming
                                    manualText = ""
                                    draftFromExternalShare = false
                                    quickTypedOpen = false
                                    twoWayTypedOpen = true
                                }
                            }, enabled = OfflineTypedCaptionPolicy.normalize(manualText) != null,
                                modifier = Modifier.fillMaxWidth()) {
                                Text("⇄ Gebruik in tweeweg-tekstgesprek (59 teksttalen)")
                            }
                        }
                        OutlinedTextField(
                            value = manualText,
                            onValueChange = { candidate ->
                                if (candidate.length <= OfflineTypedCaptionPolicy.MAX_INPUT_UNITS) {
                                    manualText = candidate
                                }
                            },
                            label = { Text("Typ je oorspronkelijke zin") },
                            minLines = 2,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Resultaat verschijnt in de tijdelijke gespreksgeschiedenis. " +
                                "Geen algemene offline spraak-/vertaling voor Kurmanji, Sorani " +
                                "of Pasjtoe; gevoelige teksten vergen menselijke controle.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = !listening &&
                            OfflineTypedCaptionPolicy.normalize(manualText) != null,
                        onClick = {
                            if (submitTypedCaption()) {
                                quickTypedOpen = false
                                draftFromExternalShare = false
                                follow = true
                            }
                        }
                    ) { Text("Vertaal lokaal") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        quickTypedOpen = false
                        draftFromExternalShare = false
                        manualText = ""
                    }) { Text("Annuleren") }
                }
            )
        }
        if (meaningIndexOpen) {
            SessionMeaningIndexDialog(
                rows = lines.mapNotNull { line ->
                    line.personalMeaning?.let { meaning ->
                        SessionMeaningRow(
                            line.id, line.original, line.translated, meaning,
                            line.sourceCode, line.targetCode,
                            (line.status == "Lokaal" || line.status == "Lokaal vertaald") &&
                                line.original.isNotBlank()
                        )
                    }
                },
                onOpenCard = { selectedId ->
                    meaningIndexOpen = false
                    if (listening) stop()
                    captionSpeaker.stop()
                    presentedCaptionId = selectedId
                },
                onEdit = { selectedId ->
                    meaningIndexOpen = false
                    if (listening) stop()
                    val selected = lines.firstOrNull { it.id == selectedId }
                    if (selected != null) {
                        editingMeaning = Triple(selected.id, selected.original,
                            selected.translationRevision)
                        meaningDraft = selected.personalMeaning ?: ""
                        meaningNotice = ""
                    }
                },
                onClose = { meaningIndexOpen = false }
            )
        }
        val presented = presentedCaptionId
        if (presented != null) {
            FullscreenCaptionCard(
                captions = lines.map {
                    FocusCaptionLine(
                        it.id, it.original, it.translated, it.status,
                        it.sourceCode, it.targetCode,
                        manuallyAssignedSpeakers[it.id], it.personalMeaning
                    )
                },
                initialId = presented,
                onDismiss = { presentedCaptionId = null },
                onStudyCaption = { selected ->
                    presentedCaptionId = null
                    captionSpeaker.stop()
                    sessionCaptionStudyStartId = selected
                    sessionCaptionStudyOpen = true
                }
            )
        }
        if (sessionCaptionStudyOpen) {
            SessionCaptionStudyDialog(
                captions = lines.map { line ->
                    FocusCaptionLine(
                        line.id, line.original, line.translated, line.status,
                        line.sourceCode, line.targetCode,
                        manuallyAssignedSpeakers[line.id], line.personalMeaning
                    )
                },
                onDismiss = {
                    sessionCaptionStudyOpen = false
                    sessionCaptionStudyStartId = null
                },
                initialCaptionId = sessionCaptionStudyStartId
            )
        }
        if (focusMode) {
            FocusTranscriptDialog(
                captions = lines.map {
                    FocusCaptionLine(it.id, it.original, it.translated, it.status,
                        it.sourceCode, it.targetCode, manuallyAssignedSpeakers[it.id],
                        it.personalMeaning)
                },
                interimOriginal = partial,
                interimTranslated = partialTranslation,
                listening = listening,
                fontScale = captionSize.scale,
                onStop = { stop() },
                onClose = { focusMode = false }
            )
        }
        if (studyBookConsentOpen) {
            AlertDialog(
                onDismissRequest = { studyBookConsentOpen = false },
                title = { Text("Mijn eigen offline studieboek bewaren?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Het boek bevat momenteel ${savedVocabulary.entries().size} " +
                            "bewaarde basiswoorden, ${ownWordDeck.entries()?.size ?: 0} " +
                            "eigen woordnotities en ${personalPhrases.entries()?.size ?: 0} " +
                            "eigen zinnen. Zelfgeschreven inhoud wordt duidelijk als " +
                            "ongecontroleerd gemarkeerd.")
                        Text("Na jouw aparte bevestiging kiest Android een HTML-opslaglocatie. " +
                            "Die bestandsprovider kan het boek online synchroniseren. " +
                            "Tuddy bewaart of uploadt uit zichzelf geen gesprekstekst. " +
                            "Controleer wat je buiten de app wilt bewaren.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        studyBookConsentOpen = false
                        val snapshot = captureCombinedStudyBook()
                        if (snapshot == null) {
                            exportStatus = "Geen volledig leesbare eigen woorden en zinnen " +
                                "voor een veilig studieboek."
                        } else {
                            pendingCombinedStudyBook = snapshot
                            saveCombinedStudyBook.launch("Tuddy-mijn-offline-studieboek.html")
                        }
                    }) { Text("Ik begrijp dit, kies opslag…") }
                },
                dismissButton = {
                    TextButton(onClick = { studyBookConsentOpen = false }) {
                        Text("Annuleren")
                    }
                }
            )
        }
        if (printableLiveConsentOpen) {
            AlertDialog(
                onDismissRequest = { printableLiveConsentOpen = false },
                title = { Text("Afdrukbaar tweetalig gesprek bewaren?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Je kiest zelf of je dit tijdelijke gesprek als HTML-bestand " +
                            "buiten Tuddy wilt bewaren. Een Android-bestandsprovider kan " +
                            "dit bestand synchroniseren of delen. Controleer de toestemming " +
                            "van betrokken gesprekspartners. Vertalingen zijn niet gecontroleerd.")
                        TextButton(onClick = { printableOnlyFinished = true }) {
                            Text(if (printableOnlyFinished) "Alleen afgeronde zinnen ✓"
                                else "Alleen afgeronde zinnen")
                        }
                        TextButton(onClick = { printableOnlyFinished = false }) {
                            Text(if (!printableOnlyFinished) "Ook originele zinnen zonder vertaling ✓"
                                else "Ook originele zinnen zonder vertaling")
                        }
                        Text("Geen audio, script, externe bronnen of transcriptopslag " +
                            "zonder jouw keuze. Onvoltooide vertalingen worden niet ingevuld.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    TextButton(enabled = !listening, onClick = {
                        printableLiveConsentOpen = false
                        val snapshot = capturePrintableConversation(printableOnlyFinished)
                        if (snapshot == null) {
                            exportStatus = "Geen geldige gespreksregels voor afdrukbare export."
                        } else {
                            pendingPrintableLive = Triple(generation, printableOnlyFinished, snapshot)
                            savePrintableLive.launch("Tuddy-afdrukbaar-gesprek.html")
                        }
                    }) { Text("Ik begrijp dit, kies opslag…") }
                },
                dismissButton = {
                    TextButton(onClick = { printableLiveConsentOpen = false }) {
                        Text("Annuleren")
                    }
                }
            )
        }
        if (subtitleConsentOpen) {
            AlertDialog(
                onDismissRequest = { subtitleConsentOpen = false },
                title = { Text("Ondertitelconcept bewaren (SRT/WebVTT)?") },
                text = {
                    Column(
                        modifier = Modifier.heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Tuddy kent alleen het einde van een herkende zin, " +
                                "niet de echte audiotijdlijn. Kies hieronder tekst, " +
                                "een ruwe begintijdverschuiving en maximale duur. " +
                                "Vertaling-only slaat zinnen zonder voltooide lokale " +
                                "vertaling over. Controleer zelf de daadwerkelijke " +
                                "timing en inhoud in een video-editor. De externe " +
                                "Android-bestandsprovider kan deze inhoud bewaren " +
                                "of synchroniseren. Controleer of je die mag delen."
                        )
                        SubtitleDraftControls(
                            options = subtitleOptions,
                            onChange = { subtitleOptions = it }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        subtitleConsentOpen = false
                        if (!listening) {
                            val snapshot = captureSubtitleDraft()
                            if (snapshot == null) {
                                exportStatus = "Geen veilig afgeronde zinnen voor een ondertitelconcept."
                            } else {
                                exportStatus = ""
                                pendingTranscriptExport = generation to snapshot
                                if (subtitleOptions.format == SubtitleDraftFormat.WEBVTT)
                                    saveWebVttDraft.launch("Tuddy-ondertitels-concept.vtt")
                                else
                                    saveSubtitleDraft.launch("Tuddy-ondertitels-concept.srt")
                            }
                        }
                    }) { Text("Ik begrijp dit, kies opslag…") }
                },
                dismissButton = {
                    TextButton(onClick = { subtitleConsentOpen = false }) {
                        Text("Annuleren")
                    }
                }
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
        val noteRequest = editingMeaning
        if (noteRequest != null) {
            val liveRow = lines.firstOrNull { it.id == noteRequest.first }
            val stillSame = !listening && liveRow != null &&
                liveRow.original == noteRequest.second &&
                liveRow.translationRevision == noteRequest.third
            val trimmed = meaningDraft.trim()
            val validNote = trimmed.isNotEmpty() && trimmed.length <= 280 &&
                !trimmed.any { it.isISOControl() }
            AlertDialog(
                onDismissRequest = {
                    editingMeaning = null
                    meaningDraft = ""
                },
                title = { Text("Eigen betekenis bij één gesprekszin") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Schrijf uitsluitend je eigen interpretatie. " +
                            "De lokale modelvertaling en de herkende woorden veranderen niet. " +
                            "Je notitie wordt niet automatisch bewaard, vertaald, " +
                            "gedeeld of toegevoegd aan het ondertitel-/gespreksarchief.")
                        if (!stillSame) {
                            Text("Deze gesprekszin is gewijzigd of de microfoon staat aan. " +
                                "Sluit dit venster en kies de actuele zin opnieuw.",
                                color = MaterialTheme.colorScheme.error)
                        }
                        OutlinedTextField(
                            value = meaningDraft,
                            onValueChange = { candidate ->
                                if (candidate.length <= 280) meaningDraft = candidate
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Mijn eigen betekenis (max. 280 tekens)") },
                            minLines = 2,
                            maxLines = 5
                        )
                        Text("Alleen tijdelijk in dit gesprek. Niet door Tuddy gecontroleerd.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    TextButton(enabled = stillSame && validNote, onClick = {
                        val position = lines.indexOfFirst { it.id == noteRequest.first }
                        if (position >= 0 && !listening &&
                            lines[position].original == noteRequest.second &&
                            lines[position].translationRevision == noteRequest.third &&
                            validNote) {
                            lines[position] = lines[position].copy(personalMeaning = trimmed)
                            meaningNotice = "Eigen betekenis uitsluitend tijdelijk bij de gekozen regel bewaard."
                        }
                        editingMeaning = null
                        meaningDraft = ""
                    }) { Text("Bewaar mijn eigen uitleg") }
                },
                dismissButton = {
                    Row {
                        if (liveRow?.personalMeaning != null) {
                            TextButton(enabled = stillSame, onClick = {
                                val position = lines.indexOfFirst { it.id == noteRequest.first }
                                if (position >= 0 && !listening &&
                                    lines[position].original == noteRequest.second &&
                                    lines[position].translationRevision == noteRequest.third) {
                                    lines[position] = lines[position].copy(personalMeaning = null)
                                    meaningNotice = "Eigen uitleg uit dit gesprek verwijderd."
                                }
                                editingMeaning = null
                                meaningDraft = ""
                            }) { Text("Wis eigen uitleg") }
                        }
                        TextButton(onClick = {
                            editingMeaning = null
                            meaningDraft = ""
                        }) { Text("Annuleren") }
                    }
                }
            )
        }
        val selectedLanguageCorrection = pendingCaptionLanguageChange
        val sourceLine = selectedLanguageCorrection?.let { request ->
            lines.firstOrNull { it.id == request.first }
        }
        if (selectedLanguageCorrection != null && sourceLine != null) {
            CaptionLanguageCorrectionDialog(
                original = sourceLine.original,
                initialSource = sourceLine.sourceCode,
                initialTarget = sourceLine.targetCode,
                onApply = { newSource, newTarget ->
                    val index = lines.indexOfFirst { it.id == selectedLanguageCorrection.first }
                    if (listening || generation != selectedLanguageCorrection.third ||
                        index < 0 || lines[index].translationRevision !=
                        selectedLanguageCorrection.second ||
                        newSource !in TargetCatalog.codes || newTarget !in TargetCatalog.codes
                    ) {
                        false
                    } else {
                        val previous = lines[index]
                        captionSpeaker.stop()
                        pendingCaptionShare = null
                        pendingTranscriptExport = null
                        exportStatus = ""
                        lines[index] = previous.copy(
                            sourceCode = newSource,
                            targetCode = newTarget,
                            translated = "Vertalen…",
                            status = "Bezig",
                            userCorrected = true,
                            translationRevision = previous.translationRevision + 1
                        )
                        // Explicit source choice overrules automatic short-text
                        // language detection; no model download is performed.
                        translate(previous.original, previous.id, newTarget, newSource,
                            generation, explicitlyChosenSource = true)
                        correctionNotice = "Taalrichting voor deze zin gewijzigd; " +
                            "lokale vertaling aangevraagd."
                        true
                    }
                },
                onDismiss = { pendingCaptionLanguageChange = null }
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
                                    userCorrected = true,
                                    // A newly corrected original invalidates the human meaning.
                                    personalMeaning = null
                                )
                                translate(corrected, previous.id, previous.targetCode,
                                    previous.sourceCode, generation,
                                    explicitlyChosenSource = true)
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
        if (archiveReaderOpen && archive != null) {
            // The source remains the user-selected imported file, NEVER the
            // live recognizer transcript. Reuse the read-only RAM viewer.
            FocusTranscriptDialog(
                captions = matchedArchive.mapIndexed { index, row ->
                    FocusCaptionLine(
                        index.toLong(), row.original, row.translated, row.status,
                        row.sourceCode, row.targetCode
                    )
                },
                interimOriginal = "",
                interimTranslated = "",
                listening = false,
                fontScale = captionSize.scale,
                onStop = {},
                onClose = { archiveReaderOpen = false },
                archiveMode = true
            )
        }
        if (archiveStudyOpen && archive != null) {
            SessionCaptionStudyDialog(
                captions = matchedArchive.mapIndexed { index, row ->
                    FocusCaptionLine(
                        index.toLong(), row.original, row.translated,
                        row.status, row.sourceCode, row.targetCode
                    )
                },
                onDismiss = { archiveStudyOpen = false },
                originLabel = "de gevonden archiefregels"
            )
        }
        if (archive != null && pendingArchiveExcerpt == null &&
            !archiveStudyOpen && !archiveReaderOpen) {
            AlertDialog(
                onDismissRequest = {
                    pendingArchiveExcerpt = null
                    previewArchiveRows = null
                    archiveSubtitleConsentOpen = false
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
                        OutlinedButton(
                            enabled = !listening && matchedArchive.isNotEmpty(),
                            onClick = {
                                captionSpeaker.stop()
                                archiveReaderOpen = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("▣ Lees gevonden archiefregels groot") }
                        OutlinedButton(
                            enabled = !listening && matchedArchive.any { row ->
                                row.status == "Lokaal vertaald" &&
                                    row.original.isNotBlank() &&
                                    row.translated.isNotBlank() &&
                                    row.original != row.translated &&
                                    row.original.length <= 400 &&
                                    row.translated.length <= 400
                            },
                            onClick = {
                                captionSpeaker.stop()
                                archiveStudyOpen = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("🧠 Oefen gevonden archiefzinnen · tijdelijk") }
                        TextButton(
                            enabled = !listening && archive.any {
                                it.status == "Lokaal vertaald" || it.status == "Lokaal"
                            },
                            onClick = { archiveSubtitleConsentOpen = true }
                        ) { Text("Bewaar dit archief als SRT/WebVTT-concept…") }
                        TextButton(
                            enabled = !listening && archive.isNotEmpty(),
                            onClick = {
                                printableOnlyFinished = true
                                printableArchiveConsentOpen = true
                            }
                        ) { Text("▤ Bewaar leesbaar afdrukbaar privéarchief (HTML)…") }
                        Text(
                            "Alleen afgeronde archiefzinnen; geschatte herkenningstijden, " +
                                "niet synchroon met de oorspronkelijke audio.",
                            style = MaterialTheme.typography.bodySmall
                        )
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

        if (printableArchiveConsentOpen && archive != null) {
            AlertDialog(
                onDismissRequest = { printableArchiveConsentOpen = false },
                title = { Text("Afdrukbaar privéarchief bewaren?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Dit is een door jou gekozen, niet onafhankelijk gecontroleerd " +
                            "gespreksarchief. Een HTML-bestand kan door de opslagprovider " +
                            "extern worden bewaard of gedeeld. Controleer toestemming.")
                        TextButton(onClick = { printableOnlyFinished = true }) {
                            Text(if (printableOnlyFinished) "Alleen afgeronde zinnen ✓"
                                else "Alleen afgeronde zinnen")
                        }
                        TextButton(onClick = { printableOnlyFinished = false }) {
                            Text(if (!printableOnlyFinished) "Ook oorspronkelijke zinnen ✓"
                                else "Ook oorspronkelijke zinnen")
                        }
                        Text("Geen onbevestigde vertalingen; geen automatische export.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    TextButton(enabled = !listening, onClick = {
                        printableArchiveConsentOpen = false
                        val snapshot = PrivatePrintableConversation.snapshot(
                            archive, printableOnlyFinished)
                        if (snapshot == null) {
                            archiveExcerptNotice = "Geen veilig archief voor afdrukbare export."
                        } else {
                            pendingPrintableArchive = generation to snapshot
                            savePrintableArchive.launch("Tuddy-afdrukbaar-privearchief.html")
                        }
                    }) { Text("Ik begrijp dit, kies opslag…") }
                },
                dismissButton = {
                    TextButton(onClick = { printableArchiveConsentOpen = false }) {
                        Text("Annuleren")
                    }
                }
            )
        }

        if (archiveSubtitleConsentOpen && archive != null) {
            val finishedCount = archive.count {
                if (subtitleOptions.mode == SubtitleDraftMode.TRANSLATION_ONLY) {
                    it.status == "Lokaal vertaald" && it.translated.isNotBlank()
                } else {
                    it.status == "Lokaal vertaald" || it.status == "Lokaal"
                }
            }
            AlertDialog(
                onDismissRequest = { archiveSubtitleConsentOpen = false },
                title = { Text("Afgerond archief als ondertitelconcept bewaren?") },
                text = {
                    Column(
                        modifier = Modifier.heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Van de " + archive.size + " archiefregels zijn momenteel " +
                            finishedCount + " bruikbaar voor deze tekstkeuze. " +
                            "Dit archief is niet onafhankelijk gecontroleerd. " +
                            "Tuddy kent slechts herkenningsvoltooiing, geen " +
                            "audiobegintijd of exacte woordtiming. De instellingen " +
                            "blijven ruwe schattingen: pas ze na export handmatig " +
                            "aan in je editor. Een externe Android-bestandsprovider " +
                            "kan de inhoud bewaren of synchroniseren. " +
                            "Controleer de toestemming voor dit gesprek."
                        )
                        SubtitleDraftControls(
                            options = subtitleOptions,
                            onChange = { subtitleOptions = it }
                        )
                    }
                },
                confirmButton = {
                    TextButton(enabled = finishedCount > 0 && !listening, onClick = {
                        archiveSubtitleConsentOpen = false
                        // Never merge an imported archive into the live conversation.
                        val snapshot = PrivateSubtitleDraftExport.snapshot(archive, subtitleOptions)
                        if (snapshot == null) {
                            archiveExcerptNotice = "Dit archief kan niet veilig " +
                                "naar een SRT-concept worden omgezet."
                        } else {
                            pendingArchiveSubtitleExport = generation to snapshot
                            if (subtitleOptions.format == SubtitleDraftFormat.WEBVTT)
                                saveArchivedWebVttDraft.launch("Tuddy-archief-ondertitelconcept.vtt")
                            else
                                saveArchivedSubtitleDraft.launch("Tuddy-archief-ondertitelconcept.srt")
                        }
                    }) { Text("Ik begrijp dit, kies opslag…") }
                },
                dismissButton = {
                    TextButton(onClick = { archiveSubtitleConsentOpen = false }) {
                        Text("Annuleren")
                    }
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
        if (flashcardsOpen) {
            OfflineStudyFlashcards(
                savedWords = savedVocabulary.entries(),
                personalPhrases = personalPhrases.entries(),
                ownWords = remember(ownWordRevision) { ownWordDeck.entries() },
                speaker = captionSpeaker,
                onManageWords = {
                    flashcardsOpen = false
                    savedWordsOpen = true
                },
                onManageOwnWords = {
                    flashcardsOpen = false
                    ownWordsOpen = true
                },
                onManagePhrases = {
                    flashcardsOpen = false
                    personalPhrasesOpen = true
                },
                onSaveStudyBook = {
                    captionSpeaker.stop()
                    flashcardsOpen = false
                    studyBookConsentOpen = true
                },
                onDismiss = { flashcardsOpen = false }
            )
        }
        if (starterPhraseboardOpen) {
            OfflineStarterPhraseboard(
                initialTarget = starterPhraseboardTarget,
                onOpenPersonalPhrases = {
                    starterPhraseboardOpen = false
                    personalPhrasesOpen = true
                },
                onDismiss = { starterPhraseboardOpen = false }
            )
        }
        if (ownWordsOpen) {
            MyOwnWordsPanel(
                deck = ownWordDeck,
                initialLanguage = source.translateCode,
                onChanged = { ownWordRevision++ },
                onDismiss = { ownWordsOpen = false }
            )
        }
        if (quickPhrasePickerOpen) {
            OfflinePhraseQuickPicker(
                phrases = personalPhrases.entries(),
                initialLanguage = previewTarget,
                onManage = {
                    quickPhrasePickerOpen = false
                    personalPhrasesOpen = true
                },
                onDismiss = { quickPhrasePickerOpen = false }
            )
        }
        if (personalPhrasesOpen) {
            MyOfflinePhrasesPanel(
                deck = personalPhrases,
                initialLanguage = previewTarget,
                onDismiss = { personalPhrasesOpen = false }
            )
        }
        val deleteRequest = pendingSingleCaptionDelete
        if (deleteRequest != null) {
            val selectedRow = lines.firstOrNull { it.id == deleteRequest.first }
            val stillSelected = selectedRow != null &&
                selectedRow.original == deleteRequest.second &&
                selectedRow.translationRevision == deleteRequest.third
            AlertDialog(
                onDismissRequest = { pendingSingleCaptionDelete = null },
                title = { Text("Eén ondertitel wissen?") },
                text = {
                    Text("Alleen deze regel verdwijnt uit het huidige tijdelijke gesprek. " +
                        "Andere ondertitels en privéwoorden blijven bestaan. " +
                        "Eerder gekopieerde, gedeelde of geëxporteerde bestanden " +
                        "buiten Tuddy kunnen hiermee niet worden gewist.")
                },
                confirmButton = {
                    TextButton(enabled = stillSelected, onClick = {
                        val index = lines.indexOfFirst { it.id == deleteRequest.first }
                        if (index >= 0 && lines[index].original == deleteRequest.second &&
                            lines[index].translationRevision == deleteRequest.third) {
                            val id = lines[index].id
                            captionSpeaker.stop() // Deleted caption must not keep being spoken.
                            // Revoke unconsumed external payloads; already exported
                            // files and the system clipboard are outside our control.
                            pendingTranscriptExport = null
                            pendingSpeakerExport = null
                            pendingCaptionShare = null
                            exportStatus = ""
                            captionPins = captionPins - id
                            manuallyAssignedSpeakers.remove(id)
                            if (editingCaptionId == id) {
                                editingCaptionId = null
                                captionCorrectionDraft = ""
                            }
                            if (lastCopiedCaptionId == id) {
                                lastCopiedCaptionId = null
                                copyStatus = ""
                            }
                            lines.removeAt(index)
                            captionDeleteNotice = "Ondertitel uit dit gesprek gewist."
                        } else {
                            captionDeleteNotice = "Regel is gewijzigd; niets verwijderd."
                        }
                        pendingSingleCaptionDelete = null
                    }) { Text("Wis deze regel") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingSingleCaptionDelete = null }) {
                        Text("Behouden")
                    }
                }
            )
        }
        val deletionCode = pendingModelDeletion
        if (deletionCode != null) {
            val deletionLabel = TargetCatalog.targets.firstOrNull {
                it.code == deletionCode
            }?.title ?: deletionCode
            AlertDialog(
                onDismissRequest = { pendingModelDeletion = null },
                title = { Text("Tekstmodel verwijderen?") },
                text = {
                    Text("Verwijder het lokale tekstmodel $deletionLabel ($deletionCode)? " +
                        "Het kan pas weer worden gebruikt na een nieuwe wifi-download. " +
                        "Gesprekken, opgeslagen woorden en spraakpakketten blijven onaangetast.")
                },
                confirmButton = {
                    TextButton(
                        enabled = !modelStorageBusy && !listening && !preparingOfflineModel &&
                            deletionCode !in setOf(source.translateCode, target.translateCode),
                        onClick = { removeUnusedTextModel(deletionCode) }
                    ) { Text("Verwijder model") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingModelDeletion = null }) { Text("Annuleren") }
                }
            )
        }
        if (pendingStudyErase) {
            val wordTotal = savedVocabulary.entries().size
            val ownWordTotal = ownWordDeck.entries()?.size
            val phraseTotal = personalPhrases.entries()?.size
            val typedFavoriteTotal = typedFavoriteStorage.entries().size
            AlertDialog(
                onDismissRequest = { pendingStudyErase = false },
                title = { Text("Privé-oefengegevens definitief wissen?") },
                text = {
                    Text("Dit wist $wordTotal opgeslagen basiswoorden en " +
                        "${ownWordTotal?.toString() ?: "een onbekend aantal"} eigen woordnotities, " +
                        "${phraseTotal?.toString() ?: "een onbekend aantal"} eigen oefenzinnen " +
                        "en ${favoritePairs.size} spraakfavorieten plus " +
                        "$typedFavoriteTotal favoriete teksttaalparen uit de lokale app-instellingen. " +
                        "Je huidige tijdelijke gesprek en losse exportbestanden blijven staan. " +
                        "Tuddy kan eerdere gedeelde of extern opgeslagen kopieën niet terughalen. " +
                        "Deze actie heeft geen herstelknop.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        pendingStudyErase = false
                        val wordsCleared = savedVocabulary.clear()
                        val ownWordsCleared = ownWordDeck.clear()
                        val phrasesCleared = personalPhrases.clear()
                        val favoritesCleared = favoriteStorage.clear()
                        val typedFavoritesCleared = typedFavoriteStorage.clear()
                        savedWordRevision++
                        ownWordRevision++
                        favoritePairs = favoriteStorage.entries(allowedSpeechTags)
                        savedWordNotice = ""
                        favoritesNotice = ""
                        savedWordsOpen = false
                        ownWordsOpen = false
                        personalPhrasesOpen = false
                        flashcardsOpen = false
                        studyEraseNotice = if (wordsCleared && ownWordsCleared &&
                            phrasesCleared && favoritesCleared && typedFavoritesCleared) {
                            "Opgeslagen basiswoorden, eigen woorden, eigen zinnen, " +
                                "spraakfavorieten en tekstfavorieten gewist. " +
                                "Gesprek, taalkeuze en eerdere externe exports zijn behouden."
                        } else {
                            "Niet alles kon worden gewist. " +
                                "Basiswoorden: ${if (wordsCleared) "gewist" else "mislukt"}, " +
                                "eigen woorden: ${if (ownWordsCleared) "gewist" else "mislukt"}, " +
                                "zinnen: ${if (phrasesCleared) "gewist" else "mislukt"}, " +
                                "spraakfavorieten: ${if (favoritesCleared) "gewist" else "mislukt"}, " +
                                "tekstfavorieten: ${if (typedFavoritesCleared) "gewist" else "mislukt"}. " +
                                "Probeer opnieuw voor de overgebleven gegevens."
                        }
                    }) { Text("Wis mijn oefengegevens") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingStudyErase = false }) {
                        Text("Annuleren")
                    }
                }
            )
        }
        if (savedWordsOpen) {
            SavedWordsPanel(
                deck = savedVocabulary,
                revision = savedWordRevision,
                onChanged = { savedWordRevision++ },
                onDismiss = { savedWordsOpen = false },
                allowSpeech = !listening
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
                val currentSpeechReady = offlineAvailable &&
                    speechPackReceipt.tag == source.speechCode &&
                    speechPackReceipt.stage == SpeechPackStage.READY
                val currentTextReady = textReceipt.source == source.translateCode &&
                    textReceipt.target == target.translateCode &&
                    textReceipt.stage == OfflineTextStage.READY
                OutlinedButton(
                    onClick = {
                        offlineReadinessOpen = true
                        showSettings = false
                        conversationToolsOpen = false
                        favoritesOpen = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (currentSpeechReady && currentTextReady)
                        "Offline startcheck · beide modellen aanwezig"
                    else "Offline startcheck · controleer spraak én tekst")
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
                        enabled = offlineAvailable && !modelStorageBusy,
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
                        editingMeaning = null
                        meaningDraft = ""
                        meaningNotice = ""
                        meaningIndexOpen = false
                        correctionNotice = ""
                        pendingSingleCaptionDelete = null
                        captionDeleteNotice = ""
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
                        quickTypedOpen = false
                        starterPhraseboardOpen = false
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
                        enabled = reverseDirection != null && !modelStorageBusy,
                        onClick = {
                            val reversed = reverseDirection
                            val previousSource = source
                            val nextSource = reversed?.let { next ->
                                spoken.firstOrNull { it.speechCode == next.sourceSpeechTag }
                            }
                            if (reversed != null && nextSource != null) {
                                stop()
                                modelStorage.invalidate()
                                installedTextModels = null
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
                TextButton(
                    onClick = { quickTypedOpen = true },
                    enabled = !listening,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("✍ Typ zin · zonder microfoon") }
                TextButton(
                    enabled = !listening,
                    onClick = {
                        if (!listening) {
                            quickTypedOpen = false
                            twoWayTypedOpen = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("⇄ Tweeweg-tekstgesprek · zonder microfoon") }
                TextButton(
                    onClick = {
                        if (listening) stop()
                        starterPhraseboardOpen = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🌍 5 basiswoorden · Kurmanji, Sorani, Pasjtoe") }
                TextButton(
                    onClick = {
                        if (listening) stop()
                        quickPhrasePickerOpen = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("▣ Mijn gesprekskaarten · direct zoeken en tonen") }
                TextButton(
                    onClick = {
                        if (listening) stop()
                        flashcardsOpen = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🧠 Oefenkaarten · mijn opgeslagen woorden en zinnen") }
                OutlinedButton(
                    enabled = !listening && lines.any { line ->
                        line.status == "Lokaal vertaald" &&
                            line.original.isNotBlank() && line.translated.isNotBlank() &&
                            line.original != line.translated &&
                            line.original.length <= 400 && line.translated.length <= 400
                    },
                    onClick = {
                        captionSpeaker.stop()
                        sessionCaptionStudyStartId = null
                        sessionCaptionStudyOpen = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🧠 Oefen dit gesprek · tijdelijke tweetalige kaarten") }
                TextButton(
                    onClick = {
                        if (listening) stop()
                        ownWordsOpen = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("✎ Mijn eigen woorden · offline betekenissen") }
                OutlinedButton(onClick = {
                    favoritesOpen = !favoritesOpen
                    if (favoritesOpen) {
                        showSettings = false
                        conversationToolsOpen = false
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (favoritesOpen) "Verberg favoriete taalparen"
                        else "☆ Favoriete taalparen (${favoritePairs.size})")
                }
                if (favoritesOpen) {
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 236.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Bewaar maximaal drie taalrichtingen. Een favoriet " +
                            "installeert geen spraak- of tekstmodel op je telefoon.",
                            style = MaterialTheme.typography.bodySmall)
                        val active = FavoriteLanguagePair(source.speechCode, target.translateCode)
                        Button(
                            enabled = active !in favoritePairs &&
                                favoritePairs.size < PrivateLanguagePairFavorites.MAX_PAIRS,
                            onClick = {
                                val saved = favoriteStorage.save(active, allowedSpeechTags)
                                favoritePairs = favoriteStorage.entries(allowedSpeechTags)
                                favoritesNotice = if (saved)
                                    "Dit taalpaar is als privéfavoriet opgeslagen."
                                else "Opslaan mislukt of maximum bereikt. Verwijder eventueel een favoriet."
                            }
                        ) { Text("☆ Bewaar huidig taalpaar") }
                        favoritePairs.forEach { pair ->
                            val from = spoken.firstOrNull { it.speechCode == pair.speechTag }
                            val to = TargetCatalog.targets.firstOrNull { it.code == pair.targetCode }
                            if (from != null && to != null) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("${from.label} → ${to.title}",
                                        style = MaterialTheme.typography.bodySmall)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(
                                            enabled = !modelStorageBusy && !preparingOfflineModel,
                                            onClick = { chooseFavoritePair(pair) }
                                        ) { Text("Kies") }
                                        TextButton(onClick = {
                                            val removed = favoriteStorage.remove(pair, allowedSpeechTags)
                                            if (removed) favoritePairs =
                                                favoriteStorage.entries(allowedSpeechTags)
                                            favoritesNotice = if (removed) "Favoriet verwijderd."
                                                else "Verwijderen mislukt; probeer opnieuw."
                                        }) { Text("Verwijder") }
                                    }
                                }
                            }
                        }
                    }
                }
                if (favoritesNotice.isNotBlank()) {
                    Text(favoritesNotice, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                if (directionNotice.isNotBlank()) {
                    Text(directionNotice, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                OutlinedButton(
                    onClick = {
                        conversationToolsOpen = !conversationToolsOpen
                        if (conversationToolsOpen) {
                            showSettings = false
                            favoritesOpen = false
                        }
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
                    OutlinedButton(
                        enabled = !listening && lines.any {
                            it.status == "Lokaal" || it.status == "Lokaal vertaald"
                        },
                        onClick = { subtitleConsentOpen = true }
                    ) { Text("Bewaar ondertitelconcept (SRT/WebVTT)…") }
                    OutlinedButton(
                        enabled = !listening && lines.isNotEmpty(),
                        onClick = {
                            printableOnlyFinished = true
                            printableLiveConsentOpen = true
                        }
                    ) { Text("▤ Afdrukbaar gesprek (HTML)…") }
                    Text(
                        "Tweetalige zinnen; geschatte tijden uit spraakafloop, " +
                            "niet gesynchroniseerd met audio.",
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
                        if (showSettings) {
                            conversationToolsOpen = false
                            favoritesOpen = false
                        }
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
                        OutlinedButton(onClick = { sourceOpen = true }, enabled = !modelStorageBusy, modifier = Modifier.fillMaxWidth()) {
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
                                    modelStorage.invalidate()
                                    installedTextModels = null
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
                        OutlinedButton(onClick = { targetOpen = true }, enabled = !modelStorageBusy, modifier = Modifier.fillMaxWidth()) {
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
                                    modelStorage.invalidate()
                                    installedTextModels = null
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
                if (source.speechCode == "ar-SY" || source.speechCode == "ar-PS") {
                    Text("Dit is alleen een regionale Android-spraakkandidaat; " +
                        "Tuddy moet het exact passende lokale pakket op dit toestel nog bevestigen. " +
                        "ML Kit-tekstvertaling gebruikt generiek Arabisch (ar), " +
                        "geen bewezen Syrisch/Palestijns dialectmodel.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text("Alleen de dertien regionale spraakkandidaten; elk offline spraakpakket moet op dit toestel echt aanwezig zijn. " +
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
                            follow = true
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
                        OutlinedButton(onClick = { prepareOfflinePair() }, enabled = !preparingOfflineModel && !modelStorageBusy) {
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
                // Storage is local and independent from speech-pack controls.
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    OutlinedButton(onClick = {
                        modelStorageOpen = !modelStorageOpen
                        if (modelStorageOpen && installedTextModels == null)
                            refreshInstalledTextModels()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (modelStorageOpen) "Verberg modelopslag" else "Beheer offline tekstmodelopslag")
                    }
                    if (modelStorageOpen) {
                        Text("Dit zijn echt aanwezige ML Kit-tekstmodellen. Het huidige " +
                            "taalpaar is beschermd; niets wordt automatisch verwijderd.",
                            style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = { refreshInstalledTextModels() },
                            enabled = !modelStorageBusy) {
                            Text(if (modelStorageBusy) "Bezig…" else "Ververs modellen")
                        }
                        if (modelStorageNotice.isNotBlank())
                            Text(modelStorageNotice, style = MaterialTheme.typography.bodySmall)
                        installedTextModels?.forEach { code ->
                            val label = TargetCatalog.targets.firstOrNull { it.code == code }?.title ?: code
                            val isProtected = code in setOf(source.translateCode, target.translateCode)
                            Row(modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("$label ($code)" + if (isProtected) " · actief" else "",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall)
                                TextButton(
                                    enabled = !isProtected && !listening &&
                                        !preparingOfflineModel && !modelStorageBusy,
                                    onClick = { pendingModelDeletion = code }
                                ) { Text("Verwijder") }
                            }
                        }
                        Text("Opnieuw gebruiken vereist eventueel een wifi-download. " +
                            "Tuddy claimt geen nauwkeurige bespaarde opslagruimte.",
                            style = MaterialTheme.typography.bodySmall)
                    }
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
                            onClick = { checkSpeechPackNow() },
                            enabled = offlineAvailable && !listening &&
                                speechPackReceipt.stage !in setOf(
                                    SpeechPackStage.CHECKING, SpeechPackStage.DOWNLOADING)
                        ) { Text("Controleer spraak") }
                        if (speechPackReceipt.tag == source.speechCode && speechPackReceipt.canDownload) {
                            Button(onClick = { downloadSpeechPackNow() }, enabled = !listening) {
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
                HorizontalDivider()
                Text("Toegankelijkheid · scherm aan tijdens live meelezen",
                    style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        if (keepScreenAwakeOn) "Scherm wakker tijdens luisteren · aan"
                        else "Scherm wakker tijdens luisteren · uit",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Switch(
                        checked = keepScreenAwakeOn,
                        enabled = screenActivity != null,
                        onCheckedChange = { requested ->
                            val saved = LiveCaptionScreenAwake.changeEnabled(context, requested)
                            if (saved) keepScreenAwakeOn = requested
                            keepScreenAwakeNotice = if (saved)
                                if (requested) "Scherm blijft aan tijdens actief luisteren."
                                else "Normale schermslaap hersteld."
                            else "Voorkeur kon niet worden opgeslagen."
                        }
                    )
                }
                Text("Uitsluitend terwijl de microfoon actief is en deze keuze aanstaat. " +
                    "Stop, tijdslimiet, achtergrond of sluiten herstellen het normale " +
                    "schermgedrag. Dit gebruikt meer batterij en kan gevoelige " +
                    "ondertitels langer zichtbaar laten. Standaard uit; geen extra " +
                    "microfoonrechten of schermopslag.",
                    style = MaterialTheme.typography.bodySmall)
                if (keepScreenAwakeNotice.isNotBlank())
                    Text(keepScreenAwakeNotice, style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                Text("Toegankelijkheid · melding bij nieuwe spraakzin",
                    style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        if (hapticAlertsOn) "Korte trilling bij afgeronde ondertitel · aan"
                        else "Korte trilling bij afgeronde ondertitel · uit",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Switch(
                        checked = hapticAlertsOn,
                        enabled = captionHaptics.available(),
                        onCheckedChange = { requested ->
                            val saved = captionHaptics.changeEnabled(requested)
                            if (saved) hapticAlertsOn = requested
                            hapticNotice = if (saved) {
                                if (requested) "Trilsignaal aangezet voor volgende afgeronde spraakzin."
                                else "Trilsignaal uitgezet."
                            } else "Deze telefoon kan het trilsignaal niet instellen."
                        }
                    )
                }
                Text(
                    "Alleen na een echt afgeronde on-device spraakzin, maximaal " +
                        "één korte puls per 3 seconden. Geen trilling bij voorlopige " +
                        "woorden, getypte zinnen of vertalingen. Android-instellingen " +
                        "kunnen de trilling verminderen of blokkeren.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (hapticAlertsOn) {
                    TextButton(onClick = {
                        hapticNotice = if (captionHaptics.preview())
                            "Voorbeeldtrilling aangevraagd bij Android."
                        else "Voorbeeldtrilling is op dit toestel niet beschikbaar."
                    }) { Text("Voel een voorbeeldtrilling") }
                }
                if (!captionHaptics.available())
                    Text("Geen trilhardware gemeld door Android.",
                        style = MaterialTheme.typography.bodySmall)
                if (hapticNotice.isNotBlank())
                    Text(hapticNotice, style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                Text("Privacy · privéweergave",
                    style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        if (privateScreenShieldOn) "Screenshots en schermopname blokkeren · aan"
                        else "Screenshots en schermopname blokkeren · uit",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Switch(
                        checked = privateScreenShieldOn,
                        enabled = screenActivity != null,
                        onCheckedChange = { requested ->
                            val updated = screenActivity?.let {
                                PrivateScreenShield.change(it, requested)
                            } == true
                            if (updated) privateScreenShieldOn = requested
                            privateScreenShieldNotice = if (updated)
                                if (requested) "Android-privéweergave ingeschakeld."
                                else "Android-privéweergave uitgeschakeld."
                            else "Schermbescherming kon niet worden gewijzigd."
                        }
                    )
                }
                Text(
                    "Android probeert screenshots, schermopnames en recente-appsvoorbeelden " +
                        "te blokkeren zolang dit aanstaat. Dit kan ook zelf screenshots " +
                        "maken verhinderen; externe camera's blijven mogelijk. " +
                        "Er wordt geen gesprekstekst voor opgeslagen.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (privateScreenShieldNotice.isNotBlank())
                    Text(privateScreenShieldNotice, style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                Text("Privacy · maximale microfoonsessieduur",
                    style = MaterialTheme.typography.titleSmall)
                Text(
                    "Optioneel: stop langdurig on-device luisteren automatisch. " +
                        "Dit geldt voor de hele sessie, ook als Android tussendoor " +
                        "spraakherkenning herstart. Geen limiet is de standaard.",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0 to "Geen", 5 to "5 min", 15 to "15 min", 30 to "30 min")
                        .forEach { (minutes, label) ->
                            TextButton(onClick = {
                                val stored = microphoneLimit.changeMinutes(minutes)
                                if (stored) {
                                    microphoneLimitMinutes = minutes
                                    val armed = !listening || microphoneLimit.arm {
                                        if (listening) {
                                            stop()
                                            error = "Microfoon automatisch gestopt na de ingestelde " +
                                                "tijdslimiet. Je gesprek blijft op dit toestel."
                                        }
                                    }
                                    if (!armed) {
                                        stop()
                                        microphoneLimitNotice =
                                            "Microfoon gestopt: de tijdslimiet kon niet veilig starten."
                                    } else {
                                        microphoneLimitNotice = "Keuze opgeslagen: " +
                                            if (minutes == 0) "geen automatische stop."
                                            else "$minutes minuten. " +
                                                if (listening) "Huidige sessie telt opnieuw vanaf nu."
                                                else "Start nieuwe luistermodus om de limiet te gebruiken."
                                    }
                                } else {
                                    microphoneLimitNotice = "Kon de tijdslimiet niet bewaren."
                                }
                            }) {
                                Text(if (microphoneLimitMinutes == minutes) "$label ✓" else label)
                            }
                        }
                }
                if (microphoneLimitNotice.isNotBlank())
                    Text(microphoneLimitNotice, style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                Text("Privacy · opgeslagen oefengegevens",
                    style = MaterialTheme.typography.titleSmall)
                Text("Verwijder desgewenst bewust opgeslagen basiswoorden, " +
                    "eigen woordbetekenissen, oefenzinnen en favoriete taalparen van dit toestel. " +
                    "Het tijdelijke gesprek en je aparte exportbestanden blijven staan.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedButton(
                    onClick = { pendingStudyErase = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Wis opgeslagen oefengegevens…") }
                if (studyEraseNotice.isNotBlank()) {
                    Text(studyEraseNotice, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
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
                    val ownMeanings = lines.count { it.personalMeaning != null }
                    TextButton(
                        enabled = ownMeanings > 0,
                        onClick = { meaningIndexOpen = true }
                    ) { Text("✎ ${ownMeanings} eigen uitleg") }
                    if (historySearchOpen && normalizedHistoryQuery.isNotBlank()) {
                        Text("${historyMatches.size} gevonden",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (captionDeleteNotice.isNotBlank()) {
                    Text(captionDeleteNotice, style = MaterialTheme.typography.bodySmall)
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
                                line.personalMeaning?.let { meaning ->
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Mijn eigen uitleg · niet geverifieerd",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(meaning, style = MaterialTheme.typography.bodyLarge)
                                        Text("Niet opgenomen in SRT, WebVTT of gespreksarchief.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (meaningNotice.isNotBlank()) {
                                    Text(meaningNotice,
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
                                    TextButton(
                                        enabled = !listening && line.original.isNotBlank(),
                                        onClick = {
                                            editingMeaning = Triple(line.id, line.original, line.translationRevision)
                                            meaningDraft = line.personalMeaning ?: ""
                                            meaningNotice = ""
                                        }
                                    ) {
                                        Text(if (line.personalMeaning == null) "✎ Eigen uitleg"
                                            else "✎ Wijzig eigen uitleg")
                                    }
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
                                                        old.sourceCode, generation,
                                                        explicitlyChosenSource = true)
                                                    correctionNotice = "Lokale vertaling opnieuw aangevraagd."
                                                }
                                            }
                                        ) { Text("↻ Probeer opnieuw") }
                                    }
                                }
                                TextButton(
                                    enabled = !listening && line.original.isNotBlank(),
                                    onClick = {
                                        pendingCaptionLanguageChange = Triple(
                                            line.id, line.translationRevision, generation)
                                        correctionNotice = ""
                                    }
                                ) { Text("🌐 Corrigeer taalrichting van deze zin") }
                                if ((line.status == "Lokaal" ||
                                        line.status == "Lokaal vertaald") &&
                                    line.original.isNotBlank()
                                ) {
                                    OutlinedButton(onClick = {
                                        if (listening) stop()
                                        captionSpeaker.stop()
                                        presentedCaptionId = line.id
                                    }) { Text("▣ Toon deze zin groot") }
                                }
                                TextButton(onClick = {
                                    pendingSingleCaptionDelete = Triple(
                                        line.id, line.original, line.translationRevision)
                                }) { Text("🗑 Wis deze regel") }
                                OfflineWordText(
                                    line.original, line.sourceCode,
                                    MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = MaterialTheme.typography.bodyLarge.fontSize * captionSize.scale
                                    ),
                                    onSaveWord = ::saveVerifiedWord,
                                    readOwnMeaning = ::readOwnWordMeaning,
                                    saveOwnMeaning = ::saveOwnWordMeaning,
                                    allowSpeech = !listening
                                )
                                if (line.status == "Lokaal vertaald" || line.status == "Lokaal") {
                                    OfflineWordText(
                                        line.translated, line.targetCode,
                                        MaterialTheme.typography.titleMedium.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = MaterialTheme.typography.titleMedium.fontSize * captionSize.scale
                                        ),
                                        onSaveWord = ::saveVerifiedWord,
                                        readOwnMeaning = ::readOwnWordMeaning,
                                        saveOwnMeaning = ::saveOwnWordMeaning,
                                        allowSpeech = !listening
                                    )
                                } else {
                                    Text(
                                        line.translated,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = MaterialTheme.typography.titleMedium.fontSize * captionSize.scale
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                LocalCaptionPlaybackMenu(
                                    hasCompletedTranslation =
                                        line.status == "Lokaal vertaald" || line.status == "Lokaal",
                                    enabled = !listening,
                                    onPlayOriginal = { pace ->
                                        if (!listening) captionSpeaker.speak(
                                            line.original, line.sourceCode, pace
                                        ) { pronunciationNotice = it }
                                    },
                                    onPlayTranslation = { pace ->
                                        if (!listening &&
                                            (line.status == "Lokaal vertaald" || line.status == "Lokaal")
                                        ) captionSpeaker.speak(
                                            line.translated, line.targetCode, pace
                                        ) { pronunciationNotice = it }
                                    },
                                    onStop = {
                                        captionSpeaker.stop()
                                        pronunciationNotice = "Lokaal voorlezen gestopt."
                                    }
                                )
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
                                    LiveInterimWordFocus(
                                        text = partial,
                                        fontScale = captionSize.scale,
                                        compact = true
                                    )
                                    Text(
                                        "Laatste tekstwoord benadrukt, niet synchroon met audio.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        "Alleen bewust bewaarde basiswoorden, eigen woordbetekenissen en zelf ingevoerde zinnen worden privé opgeslagen. " +
                        "Eigen betekenissen zijn niet automatisch vertaald of gecontroleerd. Voorlezen gebruikt Android-gemelde offline stemmen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
