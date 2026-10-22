package com.livelingo.android

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.provider.Settings
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.livelingo.android.model.SafeCommunityPhrasebook
import com.livelingo.android.ui.OfflineWordText
import com.livelingo.android.ui.DailyStudyHub
import com.livelingo.android.ui.SavedWordsPanel
import com.livelingo.android.ui.MyOfflinePhrasesPanel
import com.livelingo.android.ui.ArchivePrivatePhraseCaptureDialog
import com.livelingo.android.ui.OfflinePhraseQuickPicker
import com.livelingo.android.ui.MyOwnWordsPanel
import com.livelingo.android.ui.SessionMeaningIndexDialog
import com.livelingo.android.ui.SessionMeaningRow
import com.livelingo.android.ui.PrivateSubtitleFileDialog
import com.livelingo.android.ui.SubtitlePasteDialog
import com.livelingo.android.ui.SubtitlePasteFileContent
import com.livelingo.android.ui.SubtitlePasteFormat
import com.livelingo.android.ui.LocalSubtitleAudioPreviewDialog
import com.livelingo.android.ui.FaceToFaceConversationDialog
import com.livelingo.android.ui.FocusTranscriptDialog
import com.livelingo.android.ui.SubtitleDraftControls
import com.livelingo.android.ui.FullscreenCaptionCard
import com.livelingo.android.ui.FocusCaptionLine
import com.livelingo.android.ui.SessionCaptionStudyDialog
import com.livelingo.android.ui.LiveInterimWordFocus
import com.livelingo.android.ui.OfflineStarterPhraseboard
import com.livelingo.android.ui.OfflinePersonalPhraseTurnsDialog
import com.livelingo.android.ui.OfflineStudyFlashcards
import com.livelingo.android.ui.LocalCaptionPlaybackMenu
import com.livelingo.android.ui.OfflineReadinessDialog
import com.livelingo.android.ui.SpeechPackBrowserDialog
import com.livelingo.android.ui.SpeechPackBrowserOption
import com.livelingo.android.ui.LanguageSupportDialog
import com.livelingo.android.ui.TwoWayTypedConversationDialog
import com.livelingo.android.ui.TypedConversationLine
import com.livelingo.android.ui.CaptionLanguageCorrectionDialog
import java.io.FileInputStream
import java.security.MessageDigest
import java.text.DateFormat
import java.util.Date

/**
 * Hash the installed base APK for a user-requested device-readiness report.
 * Call from Dispatchers.IO; never block the Compose main thread on APK I/O.
 */
private fun installedBaseApkSha256(context: Context): String? = runCatching {
    val sourcePath = context.packageManager.getApplicationInfo(context.packageName, 0).sourceDir
    val digest = MessageDigest.getInstance("SHA-256")
    FileInputStream(sourcePath).use { input ->
        val buffer = ByteArray(32 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
}.getOrNull()

/** Explicitly authored SRT/VTT copy while Android document chooser takes focus. */
private data class PendingEditedSubtitleSave(
    val session: Int,
    val editorDraft: List<PrivateSubtitleCue>,
    val cues: List<PrivateSubtitleCue>,
    val format: SubtitleDraftFormat,
    val bytes: ByteArray
)

/** An exact user-selected LRC start-timestamp snapshot for a SAF handoff. */
private data class PendingLrcSubtitleSave(
    val session: Int,
    val editorDraft: List<PrivateSubtitleCue>,
    val cues: List<PrivateSubtitleCue>,
    val bytes: ByteArray
)

/** Private TTML copy, valid only across Android's user-selected save picker. */
private data class PendingTtmlSubtitleSave(
    val session: Int,
    val editorDraft: List<PrivateSubtitleCue>,
    val cues: List<PrivateSubtitleCue>,
    val bytes: ByteArray
)

/** An immutable, opt-in ASS snapshot across Android's document picker. */
private data class PendingAssSubtitleSave(
    val session: Int,
    val editorDraft: List<PrivateSubtitleCue>,
    val cues: List<PrivateSubtitleCue>,
    val bytes: ByteArray
)

/** Explicit new SBV copy after opt-in full preview and Android SAF consent. */
private data class PendingSbvSubtitleSave(
    val session: Int,
    val editorDraft: List<PrivateSubtitleCue>,
    val cues: List<PrivateSubtitleCue>,
    val bytes: ByteArray
)

/** Exact user-consented timed spreadsheet saved after Android's provider choice. */
private data class PendingTimedSubtitleSheetSave(
    val session: Int,
    val editorDraft: List<PrivateSubtitleCue>,
    val cues: List<PrivateSubtitleCue>,
    val format: TimedSubtitleSheetFormat,
    val bytes: ByteArray
)

/**
 * A user-picked file has no trustworthy MIME/extension. Suggest an input mode
 * only after its complete bounded content passes the corresponding parser.
 * Unrecognized plain text keeps the user's explicit existing mode instead.
 */
private fun detectedSubtitlePasteFormat(raw: String): SubtitlePasteFormat? {
    if (raw.isEmpty() || raw.length > PrivateSubtitleFileReader.MAX_BYTES) return null
    val bytes = raw.toByteArray(Charsets.UTF_8)
    if (bytes.isEmpty() || bytes.size > PrivateSubtitleFileReader.MAX_BYTES) return null
    if (raw.contains("-->") && PrivateSubtitleFileReader.read(bytes) != null)
        return SubtitlePasteFormat(false, false, PlainSubtitleSplitMode.LINES)
    if (raw.contains('[') && PrivateLrcSubtitleDraft.preview(raw) != null)
        return SubtitlePasteFormat(false, true, PlainSubtitleSplitMode.LINES)
    if (PrivateTtmlSubtitleReader.read(raw) != null ||
        PrivateAssSubtitleReader.read(raw) != null ||
        PrivateSbvSubtitleReader.read(raw) != null ||
        PrivateYoutubeJson3SubtitleReader.read(raw) != null)
        return SubtitlePasteFormat(false, false, PlainSubtitleSplitMode.LINES)
    val timed = PrivateTimedSubtitleSheet.readAuthored(raw) ?: return null
    val paired = timed.firstOrNull()?.text?.contains('\n') == true
    val mode = if ('\t' in raw) {
        if (paired) PlainSubtitleSplitMode.TIMED_PAIRED_COLUMNS
        else PlainSubtitleSplitMode.TIMED_TEXT_COLUMNS
    } else {
        if (paired) PlainSubtitleSplitMode.TIMED_PAIRED_CSV
        else PlainSubtitleSplitMode.TIMED_TEXT_CSV
    }
    return SubtitlePasteFormat(true, false, mode)
}

/** Explicit Android file selection with a RAM-only draft recovery point. */
private data class PendingManualSubtitleDocument(
    val session: Int,
    val previous: SubtitlePasteFileContent,
    val secondDraft: List<PrivateSubtitleCue>?
)

/** Explicit save of the original authored LRC syntax, including its headers. */
private data class PendingAuthoredLrcSourceSave(
    val session: Int,
    val previous: SubtitlePasteFileContent,
    val bytes: ByteArray
)

/** One explicit HTML worksheet save handoff; never persisted by Tuddy. */
private data class PendingSubtitleWorksheetSave(
    val session: Int,
    val draft: List<PrivateSubtitleCue>,
    val selected: List<PrivateSubtitleCue>,
    val bytes: ByteArray
)

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

private val spokenSearchAliases = mapOf(
    "nl-NL" to "nederlands hollands dutch",
    "en-US" to "engels english",
    "de-DE" to "duits german",
    "fr-FR" to "frans french",
    "es-ES" to "spaans spanish",
    "ar-SA" to "arabisch arabic",
    "ar-SY" to "syrisch syrie syria levantijns",
    "ar-PS" to "palestijns palestina palestine",
    "fa-IR" to "perzisch persian farsi iran",
    "uk-UA" to "oekraïens oekraïne ukrainian",
    "tr-TR" to "turks turkish",
    "ru-RU" to "russisch russian",
    "ur-PK" to "urdu pakistan"
)

private fun searchSpokenLanguages(query: String): List<LanguageChoice> {
    if (LanguageSearch.normalize(query).isEmpty()) return spoken
    return spoken.filter { choice ->
        val candidates = listOf(
            choice.label,
            choice.speechCode,
            choice.translateCode,
            spokenSearchAliases[choice.speechCode].orEmpty()
        ) + LanguageSearch.localizedNames(choice.speechCode)
        LanguageSearch.matches(query, candidates)
    }
}
/** One temporary, exact-caption identity for deliberate personal note adoption. */
private data class PendingLivePhraseCapture(
    val captionId: Long,
    val original: String,
    val revision: Int,
    val generation: Int
)

private data class PendingArchivePhraseCapture(
    val generation: Int,
    val archive: List<TranscriptExportRow>,
    val row: TranscriptExportRow
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
    private var pendingViewedSubtitle by mutableStateOf<IncomingViewedSubtitleDocument?>(null)
    private var nextViewedSubtitleRequest = 0L
    private var pendingPersonalCsv by mutableStateOf<IncomingPersonalCsvDocument?>(null)
    private var nextPersonalCsvRequest = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrivateScreenShield.restore(this)
        // A configuration recreation must never re-import private text from
        // the original Intent after it was already dismissed or consumed.
        // A deliberate single-file share takes precedence over text/plain:
        // it cannot be misread as a translation prompt or rejected by the
        // ordinary text-only sharesheet ingress.
        pendingPersonalCsv = if (savedInstanceState == null)
            ExternalPersonalCsvIntent.read(intent, ++nextPersonalCsvRequest) else null
        pendingViewedSubtitle = if (savedInstanceState == null &&
            pendingPersonalCsv == null)
            ExternalSubtitleDocumentIntent.read(intent, ++nextViewedSubtitleRequest) else null
        pendingExternalText = if (savedInstanceState == null &&
            pendingPersonalCsv == null &&
            pendingViewedSubtitle == null) SharedPlainTextIntent.read(intent) else null
        // Do not keep the original other-app text in Activity Intent extras.
        intent?.removeExtra(Intent.EXTRA_TEXT)
        intent?.removeExtra(Intent.EXTRA_PROCESS_TEXT)
        intent?.removeExtra(Intent.EXTRA_STREAM)
        if (intent?.action == Intent.ACTION_SEND) intent.clipData = null
        // Do not leave an externally supplied URI in the retained Activity Intent.
        if (intent?.action == Intent.ACTION_VIEW) intent.data = null
        setContent {
            TuddyScreen(
                incomingExternalText = pendingExternalText,
                onExternalTextConsumed = { pendingExternalText = null },
                incomingViewedSubtitle = pendingViewedSubtitle,
                onViewedSubtitleConsumed = { id ->
                    if (pendingViewedSubtitle?.requestId == id) pendingViewedSubtitle = null
                },
                incomingPersonalCsv = pendingPersonalCsv,
                onPersonalCsvConsumed = { id ->
                    if (pendingPersonalCsv?.requestId == id) pendingPersonalCsv = null
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingPersonalCsv = ExternalPersonalCsvIntent.read(
            intent, ++nextPersonalCsvRequest
        )
        pendingViewedSubtitle = if (pendingPersonalCsv == null)
            ExternalSubtitleDocumentIntent.read(intent, ++nextViewedSubtitleRequest)
            else null
        pendingExternalText = if (pendingPersonalCsv == null &&
            pendingViewedSubtitle == null) SharedPlainTextIntent.read(intent) else null
        if (intent.action == Intent.ACTION_VIEW) intent.data = null
        intent.removeExtra(Intent.EXTRA_TEXT)
        intent.removeExtra(Intent.EXTRA_PROCESS_TEXT)
        intent.removeExtra(Intent.EXTRA_STREAM)
        if (intent.action == Intent.ACTION_SEND) intent.clipData = null
        setIntent(intent)
    }
}

@Composable
private fun TuddyScreen(
    incomingExternalText: IncomingSharedText?,
    onExternalTextConsumed: () -> Unit,
    incomingViewedSubtitle: IncomingViewedSubtitleDocument?,
    onViewedSubtitleConsumed: (Long) -> Unit,
    incomingPersonalCsv: IncomingPersonalCsvDocument?,
    onPersonalCsvConsumed: (Long) -> Unit
) {
    val context = LocalContext.current
    // Reuse ML Kit's bundled on-device language detector across live captions;
    // callbacks are invalidated when the screen leaves composition.
    val localLanguageDetector = remember { LocalLanguageDetector() }
    // Keep a small, session-scoped cache of already-authorized local translators.
    val localTranslatorPool = remember { LocalTranslatorPool() }
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
    // Null means every pinned caption is selected; a non-null set is an
    // explicit temporary selection for export or in-app practice.
    var pinnedChosenIds by remember { mutableStateOf<Set<Long>?>(null) }
    // Single-step recovery of a mistakenly removed star or all temporary stars.
    var pinnedRemovedUndo by remember { mutableStateOf(emptySet<Long>()) }
    var pinnedSearchTerm by remember { mutableStateOf("") }
    var pinnedManageScope by remember { mutableStateOf(PinnedCaptionScope.ALL) }
    var pinnedManagePair by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pinnedPairMenuOpen by remember { mutableStateOf(false) }
    var pinnedNewestFirst by remember { mutableStateOf(false) }
    // These manually authored corrections apply ONLY to temporary study
    // cards. The source transcript, JSON, HTML and subtitles stay untouched.
    val pinnedStudyEdits = remember { mutableStateMapOf<Long, PinnedStudyCorrection>() }
    var pinnedStudyEditId by remember { mutableStateOf<Long?>(null) }
    var pinnedStudyEditSignature by remember { mutableStateOf<List<String>?>(null) }
    var pinnedStudyDraft by remember { mutableStateOf("") }
    var pinnedStudyEditError by remember { mutableStateOf("") }
    var pinnedExportOpen by remember { mutableStateOf(false) }
    var pinnedPracticeOpen by remember { mutableStateOf(false) }
    var pinnedPracticeOnlyPersonal by remember { mutableStateOf(false) }
    var pinnedExportNotice by remember { mutableStateOf("") }
    var pinnedExportFormat by remember { mutableStateOf(PinnedCaptionFormat.JSON) }
    var pinnedExportScope by remember { mutableStateOf(PinnedCaptionScope.ALL) }
    var pinnedStudyDirection by remember { mutableStateOf(PinnedStudyDirection.SOURCE_TO_TARGET) }
    var pinnedClozeDifficulty by remember { mutableStateOf(PinnedClozeDifficulty.NORMAL) }
    var pinnedClozeHintMode by remember { mutableStateOf(PinnedClozeHintMode.BLANK) }
    var pinnedClozeAnswerSpace by remember { mutableStateOf(PinnedClozeAnswerSpace.STANDARD) }
    var pinnedClozeAudience by remember { mutableStateOf(PinnedClozeAudience.SELF_STUDY) }
    var pinnedClozePass by remember { mutableStateOf(PinnedClozePass.FIRST) }
    var pinnedClozeCardOrder by remember { mutableStateOf(PinnedClozeCardOrder.SOURCE) }
    var pinnedOnlyPersonalStudy by remember { mutableStateOf(false) }
    var pinnedStudyLanguageTags by remember { mutableStateOf(false) }
    var pinnedLanguagePair by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pinnedExportShowAll by remember { mutableStateOf(false) }
    var pinnedExactPreviewOpen by remember { mutableStateOf(false) }
    var pinnedSubtitleOptions by remember { mutableStateOf(SubtitleDraftOptions()) }
    var pendingPinnedExport by remember { mutableStateOf<PendingPinnedCaptionExport?>(null) }
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
    val scheduledReviewDueCount = remember(savedWordRevision) {
        if (savedVocabulary.reviewsEnabled()) savedVocabulary.scheduledReviews()
            .count { it.dueDay <= java.time.LocalDate.now().toEpochDay() }
        else 0
    }
    var savedWordsOpen by remember { mutableStateOf(false) }
    var dailyStudyHubOpen by remember { mutableStateOf(false) }
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
    var sourceQuery by remember { mutableStateOf("") }
    var targetOpen by remember { mutableStateOf(false) }
    var targetQuery by remember { mutableStateOf("") }
    // A deliberately separate offline phrase preview: never passed to ML Kit or ASR.
    var previewTarget by remember { mutableStateOf("kmr") }
    val personalPhrases = remember(context) { PrivatePhraseDeck(context) }
    var personalPhrasesOpen by remember { mutableStateOf(false) }
    // A user explicitly opened one private CSV: expose its preview-only
    // phrasebook screen. Do not read, translate, or save the external bytes here.
    LaunchedEffect(incomingPersonalCsv?.requestId) {
        if (incomingPersonalCsv != null) personalPhrasesOpen = true
    }
    var quickPhrasePickerOpen by remember { mutableStateOf(false) }
    var starterPhraseboardOpen by remember { mutableStateOf(false) }
    var offlinePersonalTurnsOpen by remember { mutableStateOf(false) }
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
    // Explicitly opened timed subtitle text including user JSON3; never live speech.
    var pendingSubtitleOpenGeneration by remember { mutableStateOf<Int?>(null) }
    var subtitleFileCues by remember { mutableStateOf<List<PrivateSubtitleCue>?>(null) }
    // Android Open-with is a user-initiated, read-only, preview-first route.
    var viewedSubtitlePreview by remember {
        mutableStateOf<List<PrivateSubtitleCue>?>(null)
    }
    var subtitlePasteOpen by remember { mutableStateOf(false) }
    var pendingManualSubtitleDocument by remember {
        mutableStateOf<PendingManualSubtitleDocument?>(null)
    }
    var pendingAuthoredLrcSourceSave by remember {
        mutableStateOf<PendingAuthoredLrcSourceSave?>(null)
    }
    var readyFirstPasteDocument by remember {
        mutableStateOf<SubtitlePasteFileContent?>(null)
    }
    var readySecondPasteDocument by remember {
        mutableStateOf<SubtitlePasteFileContent?>(null)
    }
    // Second timed subtitle picker: one explicit RAM-only document pair, never disk.
    var pendingSecondSubtitle by remember {
        mutableStateOf<Pair<Int, List<PrivateSubtitleCue>>?>(null)
    }
    var importedSecondSubtitle by remember {
        mutableStateOf<Pair<List<PrivateSubtitleCue>, List<PrivateSubtitleCue>>?>(null)
    }
    var secondSubtitleError by remember { mutableStateOf<String?>(null) }
    var pendingSubtitleWorksheet by remember {
        mutableStateOf<PendingSubtitleWorksheetSave?>(null)
    }
    var subtitleWorksheetNotice by remember { mutableStateOf<String?>(null) }
    // Exact, user-authorized draft retained only across Android's audio picker.
    var pendingSubtitleAudio by remember {
        mutableStateOf<Pair<Int, List<PrivateSubtitleCue>>?>(null)
    }
    // Chosen URI and cue snapshot are RAM-only, removed on background.
    var subtitleAudioPreview by remember {
        mutableStateOf<Pair<List<PrivateSubtitleCue>, Uri>?>(null)
    }
    // A user-confirmed immutable copy survives the Android document picker only.
    var pendingSubtitleDraft by remember { mutableStateOf<PendingEditedSubtitleSave?>(null) }
    var pendingLrcSubtitleSave by remember {
        mutableStateOf<PendingLrcSubtitleSave?>(null)
    }
    var pendingSbvSubtitleSave by remember {
        mutableStateOf<PendingSbvSubtitleSave?>(null)
    }
    var pendingAssSubtitleSave by remember {
        mutableStateOf<PendingAssSubtitleSave?>(null)
    }
    var pendingTtmlSubtitleSave by remember {
        mutableStateOf<PendingTtmlSubtitleSave?>(null)
    }
    var pendingTimedSubtitleSheet by remember {
        mutableStateOf<PendingTimedSubtitleSheetSave?>(null)
    }
    var editedSubtitleSaveNotice by remember { mutableStateOf<String?>(null) }
    // The user selects one completed row from a read-only, unverified
    // imported archive. No clipboard/share side effects before consent.
    var pendingArchiveExcerpt by remember {
        mutableStateOf<Triple<Int, TranscriptExportRow, String>?>(null)
    }
    var pendingArchivePhraseCapture by remember {
        mutableStateOf<PendingArchivePhraseCapture?>(null)
    }
    var archivePhraseCaptureNotice by remember { mutableStateOf("") }
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
    // A second recognizer controller is used only for read-only browsing;
    // the active source and its download receipt must never be overwritten by a scan.
    val speechCatalogController = remember(context) { OnDeviceSpeechPackController(context) }
    val speechRequestHistory = remember(context) {
        PrivateSpeechPackRequests(context, spoken.map { it.speechCode }.toSet())
    }
    val speechRequestedAttempts = remember(context) {
        mutableStateMapOf<String, Long>().apply {
            putAll(speechRequestHistory.entries())
        }
    }
    val speechCatalogTimeoutHandler = remember { Handler(Looper.getMainLooper()) }
    val speechCatalogReceipts = remember { mutableStateMapOf<String, SpeechPackReceipt>() }
    var speechCatalogOpen by remember { mutableStateOf(false) }
    var speechCatalogBusy by remember { mutableStateOf(false) }
    var speechCatalogDownloadTag by remember { mutableStateOf<String?>(null) }
    var speechCatalogScanned by remember { mutableIntStateOf(0) }
    var speechCatalogEpoch by remember { mutableIntStateOf(0) }
    var follow by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    var offlineReadinessOpen by remember { mutableStateOf(false) }
    var installedApkSha256 by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(offlineReadinessOpen) {
        if (offlineReadinessOpen) {
            installedApkSha256 = withContext(Dispatchers.IO) {
                installedBaseApkSha256(context) ?: "unavailable"
            }
        }
    }
    var offlineReportPickerOpen by remember { mutableStateOf(false) }
    // Return to the explicit startcheck only after the user opened Android speech settings.
    var pendingSpeechSettingsReturnTag by remember { mutableStateOf<String?>(null) }
    var languageSupportOpen by remember { mutableStateOf(false) }
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
    var liveTableOpen by remember { mutableStateOf(false) }
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
    // Captions never become permanent phrasebook entries without the user's
    // own two text fields and a separate explicit save confirmation.
    var pendingLivePhraseCapture by remember { mutableStateOf<PendingLivePhraseCapture?>(null) }
    var capturedPhraseLanguage by remember { mutableStateOf("kmr") }
    var capturedPhraseText by remember { mutableStateOf("") }
    var capturedMeaningLanguage by remember { mutableStateOf("nl") }
    var capturedMeaningText by remember { mutableStateOf("") }
    var capturedPhraseNotice by remember { mutableStateOf("") }
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
    // Apply the same search-only script folding as the private phrasebook.
    // Stored captions and translations are never normalized or rewritten.
    val normalizedHistoryQuery = PrivatePhraseScriptSearch.fold(historySearchQuery.trim())
    fun historySearchMatches(line: Line): Boolean =
        normalizedHistoryQuery.isNotBlank() && (
            PrivatePhraseScriptSearch.contains(line.original, normalizedHistoryQuery) ||
                PrivatePhraseScriptSearch.contains(line.translated, normalizedHistoryQuery) ||
                line.personalMeaning?.let {
                    PrivatePhraseScriptSearch.contains(it, normalizedHistoryQuery)
                } == true
        )
    val historyMatches = if (normalizedHistoryQuery.isBlank()) emptyList<Int>()
        else lines.indices.filter { historySearchMatches(lines[it]) }

    fun captureCombinedStudyBook(): ByteArray? =
        PrivateCombinedStudyBook.snapshot(
            savedVocabulary.entries(), ownWordDeck.entries(), personalPhrases.entries()
        )

    fun capturePrintableConversation(onlyFinished: Boolean): ByteArray? =
        PrivatePrintableConversation.snapshot(lines.map { line ->
            TranscriptExportRow(line.original, line.translated, line.status,
                line.sourceCode, line.targetCode, line.recordedAtMillis)
        }, onlyFinished)

    fun currentPinnedSelection(): List<PinnedCaptionRow> = lines
        .filter { it.id in captionPins &&
            (pinnedChosenIds == null || it.id in pinnedChosenIds.orEmpty()) }
        .map { line ->
            PinnedCaptionRow(line.id, TranscriptExportRow(
                line.original, line.translated, line.status,
                line.sourceCode, line.targetCode, line.recordedAtMillis
            ))
        }

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
                        localTranslatorPool.acquire(from, targetCode)
                    } catch (_: Exception) {
                        updateLine(id, SafeLocalFailureText.TRANSLATION, "Fout", token, revision)
                        return@check
                    }
                    try {
                        translator.translate(text)
                            .addOnSuccessListener { output ->
                                try {
                                    updateLine(id, output, "Lokaal vertaald", token, revision)
                                } finally {
                                    translator.close()
                                }
                            }
                            .addOnFailureListener {
                                try {
                                    updateLine(id, SafeLocalFailureText.TRANSLATION, "Fout", token, revision)
                                } finally {
                                    translator.close()
                                }
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
        localLanguageDetector.identify(text) { guess ->
            if (!translationStillRelevant(id, token, revision)) return@identify
            val detected = guess
                ?.let { TranslateLanguage.fromLanguageTag(it) }
                ?.takeIf { it in TargetCatalog.codes }
            translateResolved(text, id, detected ?: sourceCode,
                targetCode, token, revision)
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

    // Stop only Tuddy's observer. Android may continue a system-owned queued download.
    fun cancelSpeechPackFollow() {
        val tag = speechPackReceipt.tag
        speechPacks.cancel()
        speechPackReceipt = SpeechPackReceipt(tag, SpeechPackStage.UNKNOWN,
            "Tuddy is gestopt met volgen. Android kan een ingeplande download nog afronden; controleer later opnieuw.")
    }

    fun openSpeechSettings() {
        val voiceInputSettings = Intent("android.settings.VOICE_INPUT_SETTINGS")
        val target = if (voiceInputSettings.resolveActivity(context.packageManager) != null)
            voiceInputSettings else Intent(Settings.ACTION_SETTINGS)
        runCatching {
            context.startActivity(target)
            pendingSpeechSettingsReturnTag = source.speechCode
        }.onFailure {
            pendingSpeechSettingsReturnTag = null
            speechPackReceipt = speechPackReceipt.copy(
                detail = "Android-spraakinstellingen konden niet worden geopend. Open ze handmatig via Instellingen."
            )
        }
    }

    // User-triggered, privacy-safe diagnostics; never include caption/audio content.
    fun offlineReadinessReportSnapshot(): ByteArray? = runCatching {
        val installedApkHash = installedApkSha256 ?: return@runCatching null
        val appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        val microphonePermissionGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val airplaneMode = runCatching {
            Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
        }.getOrNull()?.toString() ?: "unknown"
        val report = buildString {
            appendLine("Tuddy offline readiness report")
            appendLine("App package: ${context.packageName}")
            appendLine("App version: $appVersion")
            appendLine("Installed base APK SHA-256: $installedApkHash")
            appendLine("Microphone permission granted: $microphonePermissionGranted")
            appendLine("Build source SHA: ${BuildConfig.TUDDY_SOURCE_SHA}")
            appendLine("GitHub workflow run ID: ${BuildConfig.TUDDY_BUILD_RUN_ID}")
            appendLine("Android API: ${Build.VERSION.SDK_INT}")
            appendLine("Airplane mode: $airplaneMode")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Source speech tag: ${source.speechCode}")
            appendLine("Target text code: ${target.translateCode}")
            appendLine("On-device speech service available: $offlineAvailable")
            appendLine("Speech pack state: ${speechPackReceipt.stage}")
            appendLine("Speech pack detail: ${speechPackReceipt.detail}")
            appendLine("Text model pair: ${textReceipt.source} -> ${textReceipt.target}")
            appendLine("Text model state: ${textReceipt.stage}")
            appendLine("Text model detail: ${textReceipt.detail}")
            appendLine("Listening at export: $listening")
            appendLine("Transcript/audio included: no")
        }
        report.toByteArray(Charsets.UTF_8).takeIf { it.size in 1..8192 }
    }.getOrNull()

    fun copyOfflineReadinessReport(): Boolean = runCatching {
        val report = offlineReadinessReportSnapshot()
            ?.toString(Charsets.UTF_8) ?: return@runCatching false
        val board = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Tuddy offline readiness (no transcript/audio)", report)
        clip.description.extras = PersistableBundle().apply {
            putBoolean("android.content.extra.IS_SENSITIVE", true)
        }
        board.setPrimaryClip(clip)
        true
    }.getOrDefault(false)

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
                localTranslatorPool.invalidate()
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
                    localTranslatorPool.invalidate()
                    preparingOfflineModel = false
                    if (offlinePreparationEpoch.isCurrent(requestEpoch)) {
                        offlineModelStatus = "Download afgerond; aanwezigheid opnieuw controleren…"
                        checkTextPair() // Only installed models can justify a READY label.
                    }
                }
                .addOnFailureListener {
                    translator.close()
                    localTranslatorPool.invalidate()
                    preparingOfflineModel = false
                    if (offlinePreparationEpoch.isCurrent(requestEpoch)) {
                        offlineModelStatus = "Tekstmodel niet beschikbaar. Controleer wifi en opslag; probeer opnieuw."
                    }
                }
        } catch (_: Exception) {
            translator.close()
            localTranslatorPool.invalidate()
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
            interimTranslationBusy = true
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

    fun closeSpeechCatalog() {
        speechCatalogTimeoutHandler.removeCallbacksAndMessages(null)
        speechCatalogEpoch++
        speechCatalogController.cancel()
        speechCatalogDownloadTag = null
        speechCatalogBusy = false
        speechCatalogOpen = false
        speechCatalogReceipts.clear()
        speechCatalogScanned = 0
    }

    // The catalog uses ONE transient on-device controller for either scanning
    // or a single user-requested package operation, never both concurrently.
    // Stop only detaches the app's observer: Android may keep its download.
    fun cancelCatalogDownloadFollow() {
        val tag = speechCatalogDownloadTag ?: return
        speechCatalogEpoch++
        speechCatalogTimeoutHandler.removeCallbacksAndMessages(null)
        speechCatalogController.cancel()
        speechCatalogDownloadTag = null
        speechCatalogReceipts[tag] = SpeechPackReceipt(tag, SpeechPackStage.UNKNOWN,
            "Tuddy volgt dit pakket niet meer. Een Android-download kan nog doorlopen; controleer later opnieuw.")
    }

    fun checkCatalogSpeechPack(tag: String) {
        if (listening || !offlineAvailable || !speechCatalogOpen ||
            speechCatalogBusy || speechCatalogDownloadTag != null ||
            spoken.none { it.speechCode == tag } ||
            !lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        speechCatalogController.cancel()
        speechCatalogEpoch++
        val token = speechCatalogEpoch
        speechCatalogDownloadTag = tag
        // A broken Android speech service cannot leave the single-language UI
        // stuck indefinitely. A late vendor callback is invalidated on timeout.
        val timeout = Runnable {
            if (speechCatalogEpoch == token && speechCatalogDownloadTag == tag) {
                speechCatalogController.cancel()
                speechCatalogEpoch++
                speechCatalogDownloadTag = null
                speechCatalogReceipts[tag] = SpeechPackReceipt(tag, SpeechPackStage.UNKNOWN,
                    "Android reageerde niet binnen 8 seconden op deze pakketcontrole.")
            }
        }
        speechCatalogTimeoutHandler.postDelayed(timeout, 8_000L)
        speechCatalogController.check(tag) { receipt ->
            if (token != speechCatalogEpoch || !speechCatalogOpen ||
                speechCatalogDownloadTag != tag ||
                !lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
                return@check
            speechCatalogReceipts[tag] = receipt
            if (receipt.stage != SpeechPackStage.CHECKING) {
                speechCatalogTimeoutHandler.removeCallbacks(timeout)
                speechCatalogDownloadTag = null
            }
        }
    }

    fun downloadCatalogSpeechPack(tag: String) {
        if (listening || !offlineAvailable || !speechCatalogOpen ||
            speechCatalogBusy || speechCatalogDownloadTag != null ||
            spoken.none { it.speechCode == tag } ||
            speechCatalogReceipts[tag]?.stage != SpeechPackStage.DOWNLOADABLE ||
            !lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        speechCatalogController.cancel()
        speechCatalogEpoch++
        val token = speechCatalogEpoch
        speechCatalogDownloadTag = tag
        var attemptRecorded = false
        // OnDeviceSpeechPackController enforces active Wi-Fi and uses Android's
        // on-device recognizer; never a network recognizer or app-hosted URL.
        speechCatalogController.download(tag) { receipt ->
            if (token != speechCatalogEpoch || !speechCatalogOpen ||
                speechCatalogDownloadTag != tag ||
                !lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
                return@download
            speechCatalogReceipts[tag] = receipt
            // DOWNLOADING is emitted only after the device passed the Wi-Fi gate.
            // Remember a *request attempt*, never an installed-model claim.
            if (!attemptRecorded && receipt.stage == SpeechPackStage.DOWNLOADING) {
                attemptRecorded = true
                if (speechRequestHistory.recordAttempt(tag)) {
                    speechRequestedAttempts.clear()
                    speechRequestedAttempts.putAll(speechRequestHistory.entries())
                }
            }
            if (receipt.stage !in setOf(
                    SpeechPackStage.CHECKING, SpeechPackStage.DOWNLOADING
                )) {
                speechCatalogDownloadTag = null
            }
        }
    }

    fun scanSpeechCatalog() {
        if (!speechCatalogOpen || speechCatalogBusy || speechCatalogDownloadTag != null ||
            listening || !offlineAvailable) return
        speechCatalogController.cancel()
        speechCatalogEpoch++
        val scanEpoch = speechCatalogEpoch
        speechCatalogReceipts.clear()
        speechCatalogScanned = 0
        speechCatalogBusy = true

        // One transient on-device recognizer at a time; no parallel service calls,
        // microphone captures, system model requests, downloads or network access.
        fun next(index: Int) {
            if (scanEpoch != speechCatalogEpoch || !speechCatalogOpen ||
                !lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
            if (index >= spoken.size) {
                speechCatalogBusy = false
                return
            }
            val tag = spoken[index].speechCode
            // Some vendor recognition services never call back. Bound each locale
            // rather than leaving the entire list stalled on one broken package.
            val watchdog = Runnable {
                if (scanEpoch == speechCatalogEpoch && speechCatalogOpen &&
                    lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    speechCatalogController.cancel()
                    speechCatalogReceipts[tag] = SpeechPackReceipt(tag, SpeechPackStage.UNKNOWN,
                        "De lokale spraakdienst antwoordde niet binnen 8 seconden.")
                    speechCatalogScanned = index + 1
                    next(index + 1)
                }
            }
            speechCatalogTimeoutHandler.postDelayed(watchdog, 8_000L)
            speechCatalogController.check(tag) { receipt ->
                if (scanEpoch != speechCatalogEpoch || !speechCatalogOpen ||
                    !lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return@check
                speechCatalogReceipts[tag] = receipt
                if (receipt.stage != SpeechPackStage.CHECKING) {
                    speechCatalogTimeoutHandler.removeCallbacks(watchdog)
                    speechCatalogScanned = index + 1
                    next(index + 1)
                }
            }
        }
        next(0)
    }

    // Explicit, read-only evidence handoff; never includes audio or captions.
    fun copySpeechCatalogReport(): Boolean = runCatching {
        if (!speechCatalogOpen || speechCatalogBusy || speechCatalogReceipts.isEmpty()) {
            return@runCatching false
        }
        val snapshot = buildString {
            appendLine("Tuddy on-device speech catalog")
            appendLine("Source SHA: ${BuildConfig.TUDDY_SOURCE_SHA}")
            appendLine("Build run: ${BuildConfig.TUDDY_BUILD_RUN_ID}")
            appendLine("Android API: ${Build.VERSION.SDK_INT}")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("On-device recognition service present: $offlineAvailable")
            appendLine("Manually checked: ${speechCatalogScanned} of ${spoken.size}")
            spoken.forEach { option ->
                val receipt = speechCatalogReceipts[option.speechCode]
                appendLine("${option.label} [${option.speechCode}]: " +
                    (receipt?.stage?.name ?: "UNCHECKED"))
            }
            appendLine("Speech transcript or audio: not included")
            appendLine("Device-reported only; not an airplane-mode acceptance record.")
        }
        val board = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Tuddy speech packs (no transcripts)", snapshot)
        clip.description.extras = PersistableBundle().apply {
            putBoolean("android.content.extra.IS_SENSITIVE", true)
        }
        board.setPrimaryClip(clip)
        true
    }.getOrDefault(false)

    fun selectBrowserSpeechSource(tag: String) {
        val nextSource = spoken.firstOrNull { it.speechCode == tag } ?: return
        closeSpeechCatalog()
        stop()
        modelStorage.invalidate()
        installedTextModels = null
        source = nextSource
        sourceQuery = ""
        sourceOpen = false
        val saved = runCatching {
            languageSettings.edit()
                .putString(PrivateLanguageSelectionPolicy.SOURCE_KEY, tag)
                .commit()
        }.getOrDefault(false)
        offlineModelStatus = ""
        textReceipt = OfflineTextReceipt(nextSource.translateCode, target.translateCode,
            OfflineTextStage.UNCHECKED, "Spreektaal gewijzigd: controleer het tekstmodel.")
        speechPackReceipt = SpeechPackReceipt(tag, SpeechPackStage.UNCHECKED,
            "Controleer dit lokale spraakpakket opnieuw voordat je begint.")
        error = if (saved) "" else
            "Spreektaal alleen voor deze sessie gekozen; opslaan op het toestel lukte niet."
        offlineReadinessOpen = true
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
                            pinnedStudyEdits.remove(removed.id)
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
        pendingPinnedExport = null
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
                            pinnedStudyEdits.remove(removed.id)
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

    // A pinned selection is rechecked after the external picker returns; a
    // changed pin, edited sentence or new session invalidates the handoff.
    val savePinnedCaptionFile = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val pending = pendingPinnedExport
        pendingPinnedExport = null
        if (result.resultCode == Activity.RESULT_OK &&
            result.data?.data != null && pending != null
        ) {
            val currentSelection = currentPinnedSelection()
            val currentBytes = PrivatePinnedCaptionExport.snapshot(
                currentSelection, pending.format, pending.subtitleOptions, pending.scope,
                pending.languagePair, pending.studyDirection,
                pinnedStudyEdits.toMap(), pending.onlyPersonallyCorrected,
                pending.showLanguageTags, pending.clozeDifficulty, pending.clozeHintMode,
                pending.clozeAnswerSpace, pending.clozeAudience, pending.clozePass,
                pending.clozeCardOrder
            )
            if (pending.generation != generation || listening ||
                pending.format != pinnedExportFormat ||
                pending.scope != pinnedExportScope ||
                pending.languagePair != pinnedLanguagePair ||
                pending.studyDirection != pinnedStudyDirection ||
                pending.studyEdits != pinnedStudyEdits.toMap() ||
                pending.onlyPersonallyCorrected != pinnedOnlyPersonalStudy ||
                pending.showLanguageTags != pinnedStudyLanguageTags ||
                pending.clozeDifficulty != pinnedClozeDifficulty ||
                pending.clozeHintMode != pinnedClozeHintMode ||
                pending.clozeAnswerSpace != pinnedClozeAnswerSpace ||
                pending.clozeAudience != pinnedClozeAudience ||
                pending.clozePass != pinnedClozePass ||
                pending.clozeCardOrder != pinnedClozeCardOrder ||
                pending.subtitleOptions != pinnedSubtitleOptions ||
                currentSelection != pending.selection || currentBytes == null ||
                !currentBytes.contentEquals(pending.bytes)
            ) {
                exportStatus = "De gemarkeerde regels zijn gewijzigd. Kies opnieuw om te bewaren."
            } else {
                val uri = result.data?.data
                val saved = runCatching {
                    if (uri == null) false else
                        context.contentResolver.openOutputStream(uri, "w")?.use {
                            it.write(pending.bytes)
                            it.flush()
                            true
                        } ?: false
                }.getOrDefault(false)
                if (saved) {
                    exportStatus = "Alleen de geselecteerde regels zijn bewust bewaard als " +
                        pending.format.title + ". Controleer externe opslag en inhoud."
                } else {
                    error = "Gemarkeerde regels niet opgeslagen. Kies een andere opslaglocatie."
                }
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

    val saveEditedSubtitle = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val pending = pendingSubtitleDraft
        pendingSubtitleDraft = null
        if (pending != null && pending.session == generation) {
            // Android closed the editor while DocumentsUI selected a location.
            // Reopen only the exact user-authored temporary draft.
            subtitleFileCues = pending.editorDraft
            val fresh = PrivateSubtitleDraftEditor.snapshot(pending.cues, pending.format)
            editedSubtitleSaveNotice = when {
                fresh == null || !fresh.contentEquals(pending.bytes) ->
                    "Ondertitelkopie geweigerd: het tijdelijke concept is gewijzigd."
                uri == null -> "Opslaan geannuleerd; je tijdelijke concept is heropend."
                uri.scheme != "content" -> "Ongeldige opslaglocatie; niets opgeslagen."
                else -> {
                    val saved = runCatching {
                        context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                            stream.write(pending.bytes)
                            stream.flush()
                            true
                        } ?: false
                    }.getOrDefault(false)
                    if (saved) "Nieuwe SRT/VTT-kopie bewaard; tijdelijk concept blijft bewerkbaar."
                    else "Opslaan mislukt; tijdelijk concept en origineel ongewijzigd."
                }
            }
        }
    }

    fun completeTimedSubtitleSheetSave(uri: Uri?) {
        val pending = pendingTimedSubtitleSheet
        pendingTimedSubtitleSheet = null
        if (pending != null && pending.session == generation) {
            // The editor closes on Android ON_STOP; preserve only this
            // user-approved bounded snapshot across the external picker.
            subtitleFileCues = pending.editorDraft
            val current = PrivateTimedSubtitleSheet.snapshot(pending.cues, pending.format)
            editedSubtitleSaveNotice = when {
                current == null || !current.contentEquals(pending.bytes) ->
                    "Spreadsheetkopie geweigerd: het tijdelijke concept is gewijzigd."
                uri == null -> "CSV/TSV-opslag geannuleerd; concept heropend."
                uri.scheme != "content" -> "Ongeldige locatie; geen CSV/TSV opgeslagen."
                else -> {
                    val saved = runCatching {
                        context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                            stream.write(pending.bytes)
                            stream.flush()
                            true
                        } ?: false
                    }.getOrDefault(false)
                    if (saved) "Eigen tijdcodes bewaard als " +
                        pending.format.name + "; tijdelijk concept blijft beschikbaar."
                    else "CSV/TSV-opslag mislukt; concept en origineel blijven intact."
                }
            }
        }
    }

    // Let Android's document provider classify each interoperable format.
    val saveTimedSubtitleCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> completeTimedSubtitleSheetSave(uri) }
    val saveTimedSubtitleTsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/tab-separated-values")
    ) { uri -> completeTimedSubtitleSheetSave(uri) }

    val saveLrcSubtitle = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val pending = pendingLrcSubtitleSave
        pendingLrcSubtitleSave = null
        if (pending != null && pending.session == generation) {
            subtitleFileCues = pending.editorDraft
            val current = PrivateLrcSubtitleExport.snapshot(pending.cues)
            editedSubtitleSaveNotice = when {
                current == null || !current.contentEquals(pending.bytes) ->
                    "LRC-opslag geweigerd: de bevestigde bron is niet meer geldig."
                uri == null -> "LRC-bestandskeuze geannuleerd; concept heropend."
                uri.scheme != "content" -> "Ongeldige locatie; geen LRC geschreven."
                else -> {
                    val saved = runCatching {
                        context.contentResolver.openOutputStream(uri, "w")?.use { out ->
                            out.write(pending.bytes)
                            out.flush()
                            true
                        } ?: false
                    }.getOrDefault(false)
                    if (saved) "Nieuwe LRC-kopie met eigen begintijden opgeslagen."
                    else "LRC kon niet worden opgeslagen; het concept is heropend."
                }
            }
        }
    }

    val saveSbvSubtitle = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val pending = pendingSbvSubtitleSave
        pendingSbvSubtitleSave = null
        if (pending != null && pending.session == generation) {
            // SAF temporarily stops the editor; the authorized snapshot
            // survives only until the document picker returns.
            subtitleFileCues = pending.editorDraft
            val fresh = PrivateSbvSubtitleExport.snapshot(pending.cues)
            editedSubtitleSaveNotice = when {
                fresh == null || !fresh.contentEquals(pending.bytes) ->
                    "SBV-opslag geweigerd: de bevestigde preview is veranderd."
                uri == null -> "SBV-opslag geannuleerd; concept heropend."
                uri.scheme != "content" -> "Ongeldige locatie; geen SBV geschreven."
                else -> {
                    val saved = runCatching {
                        context.contentResolver.openOutputStream(uri, "w")?.use { out ->
                            out.write(pending.bytes)
                            out.flush()
                            true
                        } ?: false
                    }.getOrDefault(false)
                    if (saved) "Nieuwe SBV-kopie met eigen begin- en eindtijden opgeslagen."
                    else "SBV kon niet worden opgeslagen; origineel ongewijzigd."
                }
            }
        }
    }

    val saveAssSubtitle = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/x-ssa")
    ) { uri ->
        val pending = pendingAssSubtitleSave
        pendingAssSubtitleSave = null
        if (pending != null && pending.session == generation) {
            subtitleFileCues = pending.editorDraft
            val fresh = PrivateAssSubtitleExport.snapshot(pending.cues)
            editedSubtitleSaveNotice = when {
                fresh == null || !fresh.contentEquals(pending.bytes) ->
                    "ASS-opslag geweigerd: de bevestigde inhoud is gewijzigd."
                uri == null -> "ASS-opslag geannuleerd; concept heropend."
                uri.scheme != "content" -> "Ongeldige opslaglocatie; geen ASS geschreven."
                else -> {
                    val saved = runCatching {
                        context.contentResolver.openOutputStream(uri, "w")?.use { out ->
                            out.write(pending.bytes)
                            out.flush()
                            true
                        } ?: false
                    }.getOrDefault(false)
                    if (saved) "Nieuwe ASS-kopie met exacte centiseconden opgeslagen."
                    else "ASS niet opgeslagen; het originele document bleef intact."
                }
            }
        }
    }

    val saveTtmlSubtitle = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/ttml+xml")
    ) { uri ->
        val pending = pendingTtmlSubtitleSave
        pendingTtmlSubtitleSave = null
        if (pending != null && pending.session == generation) {
            subtitleFileCues = pending.editorDraft
            val fresh = PrivateTtmlSubtitleExport.snapshot(pending.cues)
            editedSubtitleSaveNotice = when {
                fresh == null || !fresh.contentEquals(pending.bytes) ->
                    "TTML-opslag geweigerd: het tijdelijke concept is gewijzigd."
                uri == null -> "TTML-opslag geannuleerd; concept heropend."
                uri.scheme != "content" -> "Ongeldige Android-opslaglocatie."
                else -> {
                    val saved = runCatching {
                        context.contentResolver.openOutputStream(uri, "w")?.use { out ->
                            out.write(pending.bytes)
                            out.flush()
                            true
                        } ?: false
                    }.getOrDefault(false)
                    if (saved) "Nieuwe TTML-kopie met eigen tijdvakken opgeslagen."
                    else "TTML niet opgeslagen; het concept blijft ongewijzigd."
                }
            }
        }
    }

    // CreateDocument, unlike OpenDocument, asks the user where a NEW
    // complete LRC source copy should be written. The immutable authorized
    // source survives DocumentsUI ON_STOP only until this picker returns.
    val saveAuthoredLrcSource = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val pending = pendingAuthoredLrcSourceSave
        pendingAuthoredLrcSourceSave = null
        if (pending != null && pending.session == generation) {
            val fresh = PrivateLrcSubtitleDraft.authoredSourceBytes(pending.previous.text)
            val message = when {
                !pending.previous.format.lrcMode || pending.previous.format.plainLines ||
                    fresh == null || !fresh.contentEquals(pending.bytes) ->
                    "LRC-bronopslag geweigerd: de oorspronkelijke momentopname is ongeldig."
                uri == null ->
                    "LRC-bestandskeuze geannuleerd; je tijdelijke invoer is heropend."
                uri.scheme != "content" ->
                    "Ongeldige Android-locatie; geen LRC geschreven."
                else -> {
                    val saved = runCatching {
                        context.contentResolver.openOutputStream(uri, "w")?.use { output ->
                            output.write(pending.bytes)
                            output.flush()
                            true
                        } ?: false
                    }.getOrDefault(false)
                    if (saved)
                        "Document lokaal als nieuwe volledige LRC-kopie opgeslagen; metadata behouden."
                    else
                        "Nieuwe LRC-kopie kon niet worden bewaard; eigen tekst heropend."
                }
            }
            readyFirstPasteDocument = pending.previous.copy(notice = message)
            subtitlePasteOpen = true
        }
    }

    val openTranscriptArchive = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val intendedGeneration = pendingArchiveGeneration
        pendingArchiveGeneration = null
        if (uri != null && intendedGeneration != null && intendedGeneration == generation) {
            // Android document providers may be network-backed or return zero
            // bytes for a bulk read. Never block the UI thread or spin on a
            // provider here; reuse the bounded, strict UTF-8 document reader.
            coroutineScope.launch {
                val (loaded, readNotice) = withContext(Dispatchers.IO) {
                    if (uri.scheme != "content") {
                        null to "Ongeldige Android-bestandslocatie."
                    } else when (val result = PrivateSubtitleImportDocument.read(
                        context.contentResolver, uri
                    )) {
                        is PrivateSubtitleImportDocument.Result.Rejected ->
                            null to result.reason
                        is PrivateSubtitleImportDocument.Result.Ready ->
                            PrivateTranscriptArchive.read(
                                result.text.toByteArray(Charsets.UTF_8)
                            ) to "Geen geldig privégespreksarchief."
                    }
                }
                // A newer picker, erased session or background transition must
                // never resurrect a private read-only archive on the screen.
                if (intendedGeneration != generation ||
                    pendingArchiveGeneration != null ||
                    !lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
                    return@launch
                pendingArchiveExcerpt = null
                archiveSubtitleConsentOpen = false
                pendingArchiveSubtitleExport = null
                archiveExcerptNotice = ""
                if (loaded == null) {
                    previewArchiveRows = null
                    pendingArchivePhraseCapture = null
                    archivePhraseCaptureNotice = ""
                    error = readNotice + " Het huidige gesprek is niet gewijzigd."
                } else {
                    previewArchiveRows = loaded
                    pendingArchivePhraseCapture = null
                    archivePhraseCaptureNotice = ""
                    archiveSearchQuery = ""
                    exportStatus = ""
                    error = ""
                }
            }
        }
    }

    // The external Android provider can be backed by network storage.
    // Bounded IO and timed subtitle parsing never run on the UI thread.
    suspend fun readSelectedSubtitle(uri: Uri): Pair<List<PrivateSubtitleCue>?, String> {
        if (uri.scheme != "content") return null to
            "Geen geldige Android-documentprovider; bestaande ondertitels behouden."
        return withContext(Dispatchers.IO) {
            when (val selected = PrivateSubtitleImportDocument.read(
                context.contentResolver, uri
            )) {
                is PrivateSubtitleImportDocument.Result.Rejected ->
                    null to selected.reason
                is PrivateSubtitleImportDocument.Result.Ready -> {
                    // These are user-supplied timestamps. For LRC the end of
                    // each lyric is derived from the next start; only the
                    // final lyric receives an explicit +3s estimate. Never
                    // infer word alignment, translations or speech from LRC.
                    val parsed = PrivateSubtitleFileReader.read(
                        selected.text.toByteArray(Charsets.UTF_8)
                    ) ?: PrivateAssSubtitleReader.read(selected.text)
                        ?: PrivateTtmlSubtitleReader.read(selected.text)
                        ?: PrivateLrcSubtitleDraft.preview(selected.text)
                        ?: PrivateTimedSubtitleSheet.readAuthored(selected.text)
                        ?: PrivateSbvSubtitleReader.read(selected.text)
                        ?: PrivateYoutubeJson3SubtitleReader.read(selected.text)
                    if (parsed == null)
                        null to "Geen geldig SRT/VTT/ASS/SSA/TTML/LRC/CSV/TSV/SBV/JSON3-document: controleer " +
                            "tijdcodes, tekstregels en maximaal 500 cues."
                    else parsed to ""
                }
            }
        }
    }

    val openSubtitleFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val expected = pendingSubtitleOpenGeneration
        pendingSubtitleOpenGeneration = null
        if (expected != null && expected == generation) {
            when {
                uri == null -> {
                    // Android cancellation leaves the current live session alone.
                    error = "Bestandskeuze geannuleerd; er is niets geopend."
                }
                else -> coroutineScope.launch {
                    val (loaded, reason) = readSelectedSubtitle(uri)
                    // A newer picker or erased session supersedes this read.
                    if (expected != generation ||
                        pendingSubtitleOpenGeneration != null) return@launch
                    if (loaded == null) {
                        error = reason + " Het huidige gesprek blijft ongewijzigd."
                    } else {
                        subtitleFileCues = loaded.toList()
                        error = ""
                    }
                }
            }
        }
    }

    // Picker MUST be Activity-owned: ON_STOP dismisses all private dialogs.
    // Recovery is restricted to the exact user-selected bounded UTF-8 input.
    val openManualSubtitleDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val pending = pendingManualSubtitleDocument
        pendingManualSubtitleDocument = null
        if (pending != null && pending.session == generation) {
            coroutineScope.launch {
                val result = if (uri == null) {
                    pending.previous.copy(notice =
                        "Bestandskeuze geannuleerd; je eerdere invoer is heropend.")
                } else if (uri.scheme != "content") {
                    pending.previous.copy(notice =
                        "Ongeldige Android-bestandslocatie; je eerdere invoer is behouden.")
                } else {
                    val (loaded, detectedFormat) = withContext(Dispatchers.IO) {
                        val selected = PrivateSubtitleImportDocument.read(
                            context.contentResolver, uri
                        )
                        selected to if (selected is PrivateSubtitleImportDocument.Result.Ready)
                            detectedSubtitlePasteFormat(selected.text) else null
                    }
                    when (loaded) {
                        is PrivateSubtitleImportDocument.Result.Ready ->
                            pending.previous.copy(
                                text = loaded.text,
                                format = detectedFormat ?: pending.previous.format,
                                ownMeaning = "",
                                firstRow = "1",
                                lastRow = "",
                                explicitRows = "",
                                search = "",
                                notice = "Document lokaal ingelezen (${loaded.sizeBytes} bytes). " +
                                    (if (detectedFormat != null)
                                        "Geldig ondertitelformaat herkend; bekijk de gekozen modus. "
                                     else "Geen ondertitelformaat vastgesteld; eerdere modus behouden. ") +
                                    "Controleer eerst alle regels; niets is toegepast."
                            )
                        is PrivateSubtitleImportDocument.Result.Rejected ->
                            pending.previous.copy(notice =
                                loaded.reason + " Je eerdere invoer is behouden.")
                    }
                }
                if (pending.session != generation ||
                    pendingManualSubtitleDocument != null) return@launch
                if (pending.secondDraft != null) {
                    subtitleFileCues = pending.secondDraft.toList()
                    readySecondPasteDocument = result
                } else {
                    readyFirstPasteDocument = result
                    subtitlePasteOpen = true
                }
            }
        }
    }

    val openSecondSubtitleFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val pending = pendingSecondSubtitle
        pendingSecondSubtitle = null
        if (pending != null && pending.first == generation) {
            // The existing editor was intentionally dismissed while Android
            // took focus. Reopen exactly that RAM-only snapshot.
            subtitleFileCues = pending.second.toList()
            importedSecondSubtitle = null
            secondSubtitleError = null
            if (uri != null) coroutineScope.launch {
                val (loaded, reason) = readSelectedSubtitle(uri)
                if (pending.first != generation ||
                    pendingSecondSubtitle != null) return@launch
                // A user could edit/replace the draft while the provider reads.
                if (subtitleFileCues != pending.second) return@launch
                if (loaded == null) {
                    secondSubtitleError = reason + " Het bestaande concept is behouden."
                } else {
                    importedSecondSubtitle = pending.second.toList() to loaded.toList()
                }
            }
        }
    }

    // CreateDocument must outlive its editor Composable: Android may
    // trigger ON_STOP while DocumentsUI chooses a destination.
    val saveSubtitleWorksheet = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/html")
    ) { uri ->
        val pending = pendingSubtitleWorksheet
        pendingSubtitleWorksheet = null
        if (pending != null && pending.session == generation) {
            subtitleFileCues = pending.draft
            val fresh = PrivateSubtitleRecallWorksheet.snapshot(pending.selected)
            if (fresh == null || !fresh.contentEquals(pending.bytes)) {
                subtitleWorksheetNotice = "Oefenblad niet opgeslagen: de selectie is gewijzigd."
            } else if (uri == null) {
                subtitleWorksheetNotice = "Opslaan geannuleerd; ondertitelconcept intact."
            } else if (uri.scheme != "content") {
                subtitleWorksheetNotice = "Ongeldige documentlocatie; niets opgeslagen."
            } else {
                val saved = runCatching {
                    context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                        stream.write(pending.bytes)
                        stream.flush()
                        true
                    } ?: false
                }.getOrDefault(false)
                subtitleWorksheetNotice = if (saved)
                    "Het offline oefenblad is bewaard op je gekozen locatie."
                else "Opslaan is mislukt; je ondertitelconcept is ongewijzigd."
            }
        }
    }

    val openSubtitleAudio = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val pending = pendingSubtitleAudio
        pendingSubtitleAudio = null
        if (pending != null && pending.first == generation) {
            if (uri?.scheme == "content") {
                subtitleAudioPreview = pending.second to uri
                subtitleFileCues = null
            } else {
                // Cancelled/invalid SAF choice: restore only the exact user-opened
                // temporary draft rather than losing it after ON_STOP.
                subtitleFileCues = pending.second
                error = if (uri == null) "" else "Alleen Android-bestandskeuzes zijn toegestaan."
            }
        }
    }

    // Activity remaining composed in the background must NOT keep the mic open.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                val requestedTag = pendingSpeechSettingsReturnTag
                if (requestedTag != null) {
                    pendingSpeechSettingsReturnTag = null
                    if (requestedTag == source.speechCode && !listening) {
                        offlineReadinessOpen = true
                        // Read-only refresh. Never re-open a microphone or download a model.
                        checkSpeechPackNow()
                        checkTextPair()
                    }
                }
            }
            if (event == Lifecycle.Event.ON_STOP) stop()
            if (event == Lifecycle.Event.ON_STOP) closeSpeechCatalog()
            if (event == Lifecycle.Event.ON_STOP) {
                localTranslatorPool.invalidate()
                modelStorage.invalidate()
                modelStorageBusy = false
                pendingModelDeletion = null
                installedTextModels = null
            }
            if (event == Lifecycle.Event.ON_STOP) {
                if (!offlineReportPickerOpen) offlineReadinessOpen = false
                starterPhraseboardOpen = false
                quickPhrasePickerOpen = false
                flashcardsOpen = false
                ownWordsOpen = false
                focusMode = false
                liveTableOpen = false
                sessionCaptionStudyOpen = false
                sessionCaptionStudyStartId = null
                presentedCaptionId = null
                meaningIndexOpen = false
                pendingLivePhraseCapture = null
                capturedPhraseText = ""
                capturedMeaningText = ""
                quickTypedOpen = false
                twoWayTypedOpen = false // forget private typed draft on background
                sharedTextToTwoWay = null
                draftFromExternalShare = false
                manualText = ""
                previewArchiveRows = null
                pendingArchivePhraseCapture = null
                archivePhraseCaptureNotice = ""
                subtitleFileCues = null
                viewedSubtitlePreview = null
                subtitlePasteOpen = false
                readyFirstPasteDocument = null
                readySecondPasteDocument = null
                importedSecondSubtitle = null
                secondSubtitleError = null
                subtitleWorksheetNotice = null
                editedSubtitleSaveNotice = null
                subtitleAudioPreview = null
                archiveStudyOpen = false
                archiveReaderOpen = false
                archiveSearchQuery = ""
                pinnedExportOpen = false
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
            pendingSpeechSettingsReturnTag = null
            generation++
            modelStorage.invalidate()
            starterPhraseboardOpen = false
            quickPhrasePickerOpen = false
            flashcardsOpen = false
            ownWordsOpen = false
            twoWayTypedOpen = false
            liveTableOpen = false
            sharedTextToTwoWay = null
            presentedCaptionId = null
            sessionCaptionStudyOpen = false
            sessionCaptionStudyStartId = null
            pendingCaptionLanguageChange = null
            meaningIndexOpen = false
            stop() // stop() already releases and nulls Android's recognizer
            previewArchiveRows = null
            pendingArchivePhraseCapture = null
            archivePhraseCaptureNotice = ""
            subtitleFileCues = null
            viewedSubtitlePreview = null
            subtitlePasteOpen = false
            pendingManualSubtitleDocument = null
            readyFirstPasteDocument = null
            readySecondPasteDocument = null
            pendingSecondSubtitle = null
            importedSecondSubtitle = null
            secondSubtitleError = null
            pendingSubtitleWorksheet = null
            subtitleWorksheetNotice = null
            pendingSubtitleAudio = null
            subtitleAudioPreview = null
            pendingSubtitleOpenGeneration = null
            archiveStudyOpen = false
            archiveReaderOpen = false
            archiveSearchQuery = ""
            pendingArchiveGeneration = null
            pendingSubtitleDraft = null
            pendingTimedSubtitleSheet = null
            pendingLrcSubtitleSave = null
            pendingSbvSubtitleSave = null
            pendingAssSubtitleSave = null
            pendingTtmlSubtitleSave = null
            editedSubtitleSaveNotice = null
            pendingArchiveExcerpt = null
            archiveExcerptNotice = ""
            quickTypedOpen = false
            draftFromExternalShare = false
            manualText = ""
            pendingSpeakerExport = null
            pendingPinnedExport = null
            pinnedExportOpen = false
            editingCaptionId = null
            captionCorrectionDraft = ""
            pendingLivePhraseCapture = null
            capturedPhraseText = ""
            capturedMeaningText = ""
            pendingSingleCaptionDelete = null
            pendingStudyErase = false
            manuallyAssignedSpeakers.clear()
            localLanguageDetector.close()
            localTranslatorPool.close()
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

    // A user explicitly chose "Open with Tuddy" or single-file sharing. A
    // content:// stream is bounded and decoded entirely outside the UI.
    // Never follow file://, https:// or Android EXTRA_STREAM attachments.
    LaunchedEffect(incomingViewedSubtitle) {
        val request = incomingViewedSubtitle ?: return@LaunchedEffect
        val currentGeneration = generation
        if (request.uri == null) {
            error = request.notice ?: "Geen ondersteund Android-ondertitelbestand."
        } else if (subtitleFileCues != null || subtitlePasteOpen ||
            viewedSubtitlePreview != null || pendingManualSubtitleDocument != null) {
            error = "Er is al een ondertitelconcept open. Sluit dat eerst en open opnieuw."
        } else {
            val (loaded, reason) = readSelectedSubtitle(request.uri)
            if (currentGeneration == generation &&
                lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) &&
                incomingViewedSubtitle?.requestId == request.requestId) {
                if (loaded == null) {
                    error = reason + " Het huidige gesprek blijft ongewijzigd."
                } else {
                    stop() // Do not combine a file preview with live microphone capture.
                    viewedSubtitlePreview = loaded.toList()
                    error = ""
                }
            }
        }
        onViewedSubtitleConsumed(request.requestId)
    }

    // Android Sharesheet text is only offered as an editable RAM draft.
    // No auto-submit, network call, persistence or microphone permission.
    LaunchedEffect(incomingExternalText) {
        val shared = incomingExternalText
        if (shared != null) {
            if (shared.subtitleDraft != null) {
                // A validated sharesheet subtitle is ONLY a draft for the
                // existing explicit preview / acceptance UI. Never replace
                // another open subtitle editor or silently mutate a session.
                if (subtitleFileCues != null || subtitlePasteOpen ||
                    pendingManualSubtitleDocument != null) {
                    error = "Er is al een ondertitelconcept open. Sluit dat eerst en deel opnieuw."
                } else {
                    stop()
                    quickTypedOpen = false
                    twoWayTypedOpen = false
                    sharedTextToTwoWay = null
                    draftFromExternalShare = false
                    manualText = ""
                    readyFirstPasteDocument = SubtitlePasteFileContent(
                        text = shared.subtitleDraft,
                        format = SubtitlePasteFormat(
                            plainLines = shared.subtitleTimedSheetMode != null,
                            lrcMode = shared.subtitleIsLrc,
                            splitMode = shared.subtitleTimedSheetMode
                                ?: PlainSubtitleSplitMode.LINES
                        ),
                        notice = (when {
                            shared.subtitleIsLrc ->
                                "Gedeelde LRC-lyrics hebben alleen zelf opgegeven begintijden; " +
                                    "eindtijden blijven geschat. "
                            shared.subtitleTimedSheetMode != null ->
                                "Gedeelde CSV/TSV heeft expliciete begin- en eindtijden; " +
                                    "een tweede tekstkolom is geen geverifieerde vertaling. "
                            else -> "Gedeelde SRT/WebVTT/ASS/SSA/SBV/JSON3 is alleen een tijdelijk voorstel. "
                        }) + "Controleer inhoud en tijdcodes en bevestig apart; " +
                            "er wordt niets automatisch opgeslagen of vertaald."
                    )
                    subtitlePasteOpen = true
                    error = ""
                }
            } else if (shared.draft == null) {
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
        if (languageSupportOpen) {
            LanguageSupportDialog(
                spokenCandidates = spoken.map { it.label + " (" + it.speechCode + ")" },
                onPreviewLanguage = { code ->
                    languageSupportOpen = false
                    starterPhraseboardTarget = code
                    starterPhraseboardOpen = true
                },
                onDismiss = { languageSupportOpen = false }
            )
        }
        if (speechCatalogOpen && !listening) {
            SpeechPackBrowserDialog(
                options = spoken.map { SpeechPackBrowserOption(it.label, it.speechCode) },
                currentTag = source.speechCode,
                receipts = speechCatalogReceipts,
                requestedAttempts = speechRequestedAttempts,
                scanning = speechCatalogBusy,
                downloadTag = speechCatalogDownloadTag,
                scannedCount = speechCatalogScanned,
                speechServiceAvailable = offlineAvailable,
                onScan = { scanSpeechCatalog() },
                onStopScan = {
                    speechCatalogEpoch++
                    speechCatalogController.cancel()
                    speechCatalogTimeoutHandler.removeCallbacksAndMessages(null)
                    speechCatalogReceipts.entries.toList().forEach { (tag, receipt) ->
                        if (receipt.stage == SpeechPackStage.CHECKING) {
                            speechCatalogReceipts[tag] = receipt.copy(
                                stage = SpeechPackStage.UNKNOWN,
                                detail = "Controle handmatig gestopt; probeer het later opnieuw.")
                        }
                    }
                    speechCatalogBusy = false
                },
                onPrepare = { tag -> downloadCatalogSpeechPack(tag) },
                onCancelFollow = { cancelCatalogDownloadFollow() },
                onCheckOne = { tag -> checkCatalogSpeechPack(tag) },
                onForgetRequest = { tag ->
                    if (speechRequestHistory.forget(tag)) {
                        speechRequestedAttempts.remove(tag)
                    }
                },
                onCopyCatalog = { copySpeechCatalogReport() },
                onSelect = { tag -> selectBrowserSpeechSource(tag) },
                onDismiss = { closeSpeechCatalog() }
            )
        }
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
                onCancelSpeech = { cancelSpeechPackFollow() },
                onOpenSpeechSettings = { openSpeechSettings() },
                onBrowseSpeechPacks = {
                    offlineReadinessOpen = false
                    speechCatalogOpen = true
                },
                onCopyDiagnostics = { copyOfflineReadinessReport() },
                onReportSnapshot = { offlineReadinessReportSnapshot() },
                onReportPickerStarted = { offlineReportPickerOpen = true },
                onReportPickerFinished = { offlineReportPickerOpen = false },
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
        val phraseCapture = pendingLivePhraseCapture
        if (phraseCapture != null) {
            val sourceRow = lines.firstOrNull { it.id == phraseCapture.captionId }
            val stillSameCaption = !listening && generation == phraseCapture.generation &&
                sourceRow != null && sourceRow.original == phraseCapture.original &&
                sourceRow.translationRevision == phraseCapture.revision
            val personalSentence = capturedPhraseText.trim()
            val personalMeaning = capturedMeaningText.trim()
            val fieldsValid = PrivatePhraseTextPolicy.valid(personalSentence, 120) &&
                PrivatePhraseTextPolicy.valid(personalMeaning, 180)
            val duplicate = personalPhrases.entries()?.any {
                it.language == capturedPhraseLanguage &&
                    it.phrase.equals(personalSentence, ignoreCase = true)
            } == true
            AlertDialog(
                onDismissRequest = {
                    pendingLivePhraseCapture = null
                    capturedPhraseText = ""
                    capturedMeaningText = ""
                },
                title = { Text("Maak een eigen offline taalkaart") },
                text = {
                    Column(
                        Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Gespreksregel ter referentie (geen gecontroleerde vertaling):")
                        Text(phraseCapture.original, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Schrijf zélf de Kurmanji-, Sorani- of Pasjtoezin en " +
                                "de Nederlandse of Engelse betekenis. Tuddy vertaalt niets " +
                                "automatisch. Pas na je bevestiging wordt deze kaart " +
                                "blijvend in het privézinnenboek van deze app opgeslagen. " +
                                "Andere gespreksregels en audio worden niet opgeslagen.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (!stillSameCaption) Text(
                            "De regel is veranderd, verwijderd of de microfoon staat aan. " +
                                "Sluit en open de actuele regel opnieuw.",
                            color = MaterialTheme.colorScheme.error
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            listOf("kmr" to "Kurmanji", "ckb" to "Sorani",
                                "ps" to "Pasjtoe").forEach { (code, label) ->
                                TextButton(onClick = { capturedPhraseLanguage = code }) {
                                    Text((if (capturedPhraseLanguage == code) "● " else "○ ") + label)
                                }
                            }
                        }
                        OutlinedTextField(
                            value = capturedPhraseText,
                            onValueChange = { if (it.length <= 120) capturedPhraseText = it },
                            label = { Text("Mijn eigen zin (max. 120 tekens)") },
                            minLines = 2, maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("nl" to "Nederlands", "en" to "Engels").forEach { (code, label) ->
                                TextButton(onClick = { capturedMeaningLanguage = code }) {
                                    Text((if (capturedMeaningLanguage == code) "● " else "○ ") + label)
                                }
                            }
                        }
                        OutlinedTextField(
                            value = capturedMeaningText,
                            onValueChange = { if (it.length <= 180) capturedMeaningText = it },
                            label = { Text("Mijn zelfgeschreven betekenis (max. 180 tekens)") },
                            minLines = 2, maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (duplicate) Text(
                            "Deze zin bestaat al voor de gekozen taal. " +
                                "Open Mijn eigen offline zinnen om die kaart bewust te wijzigen.",
                            color = MaterialTheme.colorScheme.error
                        )
                        if (capturedPhraseNotice.isNotBlank()) Text(
                            capturedPhraseNotice, color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "Bewaart alleen deze twee door jou geschreven velden op je apparaat. " +
                                "Geen cloud, ASR-model, transcript of automatische vertaalclaim.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = stillSameCaption && fieldsValid && !duplicate,
                        onClick = {
                            val latest = lines.firstOrNull { it.id == phraseCapture.captionId }
                            if (!listening && generation == phraseCapture.generation &&
                                latest != null && latest.original == phraseCapture.original &&
                                latest.translationRevision == phraseCapture.revision &&
                                PrivatePhraseTextPolicy.valid(personalSentence, 120) &&
                                PrivatePhraseTextPolicy.valid(personalMeaning, 180) &&
                                personalPhrases.saveNew(capturedPhraseLanguage,
                                    personalSentence, capturedMeaningLanguage, personalMeaning)) {
                                capturedPhraseNotice =
                                    "Eigen taalkaart bewust opgeslagen. Open Mijn eigen offline zinnen om die te bekijken."
                                pendingLivePhraseCapture = null
                                capturedPhraseText = ""
                                capturedMeaningText = ""
                            } else {
                                capturedPhraseNotice =
                                    "Niet opgeslagen: controleer invoer, beschikbare ruimte of bestaande kaarten."
                            }
                        }
                    ) { Text("Bewaar deze eigen privékaart") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        pendingLivePhraseCapture = null
                        capturedPhraseText = ""
                        capturedMeaningText = ""
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
        if (pinnedPracticeOpen) {
            SessionCaptionStudyDialog(
                captions = lines.filter { line ->
                    val selected = line.id in captionPins &&
                        (pinnedChosenIds == null || line.id in pinnedChosenIds.orEmpty())
                    val correction = pinnedStudyEdits[line.id]?.appliesTo(
                        TranscriptExportRow(line.original, line.translated, line.status,
                            line.sourceCode, line.targetCode, line.recordedAtMillis)
                    ) == true
                    selected && (!pinnedPracticeOnlyPersonal || correction)
                }.map { line ->
                    FocusCaptionLine(
                        line.id, line.original,
                        pinnedStudyEdits[line.id]
                            ?.takeIf { it.appliesTo(TranscriptExportRow(
                                line.original, line.translated, line.status,
                                line.sourceCode, line.targetCode, line.recordedAtMillis
                            )) }?.answer ?: line.translated,
                        line.status,
                        line.sourceCode, line.targetCode,
                        manuallyAssignedSpeakers[line.id], line.personalMeaning
                    )
                },
                onDismiss = { pinnedPracticeOpen = false },
                originLabel = "alleen gemarkeerde zinnen",
                manualStudyAnswers = lines.filter { line ->
                    pinnedStudyEdits[line.id]?.appliesTo(TranscriptExportRow(
                        line.original, line.translated, line.status,
                        line.sourceCode, line.targetCode, line.recordedAtMillis
                    )) == true
                }.map { it.id }.toSet()
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
        if (subtitlePasteOpen) {
            SubtitlePasteDialog(
                title = "Eigen ondertitels plakken of importeren",
                initialDocument = readyFirstPasteDocument,
                onChooseDocument = { request ->
                    pendingManualSubtitleDocument = PendingManualSubtitleDocument(
                        generation, request, null
                    )
                    subtitlePasteOpen = false
                    readyFirstPasteDocument = null
                    openManualSubtitleDocument.launch(arrayOf("*/*"))
                },
                onSaveEditedLrcSource = { request ->
                    val bytes = if (!request.format.plainLines && request.format.lrcMode)
                        PrivateLrcSubtitleDraft.authoredSourceBytes(request.text)
                    else null
                    if (bytes == null) {
                        readyFirstPasteDocument = request.copy(
                            notice = "Ongeldige LRC-bron: geen bestand bewaard."
                        )
                        subtitlePasteOpen = true
                    } else {
                        pendingAuthoredLrcSourceSave = PendingAuthoredLrcSourceSave(
                            generation, request, bytes
                        )
                        subtitlePasteOpen = false
                        readyFirstPasteDocument = null
                        saveAuthoredLrcSource.launch("Tuddy-gecorrigeerde-eigen-bron.lrc")
                    }
                },
                onDismiss = {
                    subtitlePasteOpen = false
                    readyFirstPasteDocument = null
                },
                onAccept = { imported ->
                    subtitlePasteOpen = false
                    readyFirstPasteDocument = null
                    subtitleFileCues = imported.toList()
                    error = ""
                }
            )
        }
        viewedSubtitlePreview?.let { preview ->
            AlertDialog(
                onDismissRequest = { viewedSubtitlePreview = null },
                title = { Text("Bestand openen met Tuddy?") },
                text = {
                    Column(
                        modifier = Modifier.heightIn(max = 500.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Android heeft een document aangeboden. Hieronder " +
                            "staan de aangeleverde tijdcodes en alle lokale tekstregels. " +
                            "Bekijk ze vóór je het tijdelijke concept opent. " +
                            "Er wordt niets automatisch vertaald, opgeslagen of " +
                            "naar een server gestuurd.",
                            style = MaterialTheme.typography.bodySmall)
                        Text("${preview.size} ondertitelregels",
                            style = MaterialTheme.typography.titleSmall)
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 355.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(preview.size) { index ->
                                val cue = preview[index]
                                Text("${index + 1}. ${cue.startLabel} → ${cue.endLabel}\n${cue.text}",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        subtitleFileCues = preview.toList()
                        viewedSubtitlePreview = null
                        error = ""
                    }) { Text("Open tijdelijk in editor") }
                },
                dismissButton = {
                    TextButton(onClick = { viewedSubtitlePreview = null }) {
                        Text("Annuleren")
                    }
                }
            )
        }
        subtitleFileCues?.let { opened ->
            PrivateSubtitleFileDialog(
                cues = opened,
                onDismiss = { subtitleFileCues = null },
                onChooseAudio = { draftCues ->
                    pendingSubtitleAudio = generation to draftCues.toList()
                    openSubtitleAudio.launch(arrayOf("audio/*"))
                },
                onChooseSecondFile = { draftCues ->
                    pendingSecondSubtitle = generation to draftCues.toList()
                    importedSecondSubtitle = null
                    secondSubtitleError = null
                    // SAF providers do not agree on timed subtitle MIME types.
                    openSecondSubtitleFile.launch(arrayOf("*/*"))
                },
                incomingSecondPaste = readySecondPasteDocument,
                onChoosePasteDocument = { draftCues, request ->
                    pendingManualSubtitleDocument = PendingManualSubtitleDocument(
                        generation, request, draftCues.toList()
                    )
                    readySecondPasteDocument = null
                    subtitleFileCues = null
                    openManualSubtitleDocument.launch(arrayOf("*/*"))
                },
                onSecondPasteConsumed = { readySecondPasteDocument = null },
                pendingSecondFile = importedSecondSubtitle,
                secondFileError = secondSubtitleError,
                onSecondFileReceived = {
                    importedSecondSubtitle = null
                    secondSubtitleError = null
                },
                worksheetNotice = subtitleWorksheetNotice,
                onWorksheetNoticeRead = { subtitleWorksheetNotice = null },
                saveCopyNotice = editedSubtitleSaveNotice,
                onSaveCopyNoticeRead = { editedSubtitleSaveNotice = null },
                onSaveWorksheetCopy = { draftCues, selectedCues ->
                    val chosen = selectedCues.take(24).toList()
                    val bytes = PrivateSubtitleRecallWorksheet.snapshot(chosen)
                    if (bytes == null) {
                        error = "Ongeldige ondertitels; kan geen veilig oefenblad maken."
                    } else {
                        pendingSubtitleWorksheet = PendingSubtitleWorksheetSave(
                            generation, draftCues.toList(), chosen, bytes
                        )
                        saveSubtitleWorksheet.launch("Tuddy-ondertitels-oefenblad.html")
                    }
                },
                onSaveCopy = { editorDraft, exportCues, format ->
                    val snapshot = PrivateSubtitleDraftEditor.snapshot(exportCues, format)
                    if (snapshot == null) {
                        error = "Kan geen veilige nieuwe ondertitelkopie maken."
                    } else {
                        pendingSubtitleDraft = PendingEditedSubtitleSave(
                            generation, editorDraft.toList(), exportCues.toList(),
                            format, snapshot
                        )
                        saveEditedSubtitle.launch(if (format == SubtitleDraftFormat.WEBVTT)
                            "Tuddy-handmatig-ondertitelconcept.vtt"
                        else "Tuddy-handmatig-ondertitelconcept.srt")
                    }
                },
                onSaveLrc = { editorDraft, exportCues ->
                    val bytes = PrivateLrcSubtitleExport.snapshot(exportCues)
                    if (bytes == null) {
                        error = "Ongeldig LRC-concept: bewaar volledige tijden als SRT/VTT."
                    } else {
                        pendingLrcSubtitleSave = PendingLrcSubtitleSave(
                            generation, editorDraft.toList(), exportCues.toList(), bytes
                        )
                        saveLrcSubtitle.launch("Tuddy-eigen-begintijden.lrc")
                    }
                },
                onSaveSbv = { editorDraft, exportCues ->
                    val bytes = PrivateSbvSubtitleExport.snapshot(exportCues)
                    if (bytes == null) {
                        error = "Geen veilige SBV-kopie mogelijk; gebruik SRT/VTT."
                    } else {
                        pendingSbvSubtitleSave = PendingSbvSubtitleSave(
                            generation, editorDraft.toList(), exportCues.toList(), bytes
                        )
                        saveSbvSubtitle.launch("Tuddy-eigen-tijdvakken.sbv")
                    }
                },
                onSaveAss = { editorDraft, exportCues ->
                    val bytes = PrivateAssSubtitleExport.snapshot(exportCues)
                    if (bytes == null) {
                        error = "ASS bewaart alleen centiseconden. " +
                            "Controleer de volledige tijden of gebruik SRT/VTT."
                    } else {
                        pendingAssSubtitleSave = PendingAssSubtitleSave(
                            generation, editorDraft.toList(), exportCues.toList(), bytes
                        )
                        saveAssSubtitle.launch("Tuddy-eigen-ondertitels.ass")
                    }
                },
                onSaveTtml = { editorDraft, exportCues ->
                    val bytes = PrivateTtmlSubtitleExport.snapshot(exportCues)
                    if (bytes == null) {
                        error = "Geen verliesvrije TTML-kopie mogelijk; gebruik SRT/VTT."
                    } else {
                        pendingTtmlSubtitleSave = PendingTtmlSubtitleSave(
                            generation, editorDraft.toList(), exportCues.toList(), bytes
                        )
                        saveTtmlSubtitle.launch("Tuddy-eigen-tijdvakken.ttml")
                    }
                },
                onShareAssExcerpt = { selectedCues ->
                    // Explicit, small literal ASS text selection only. No
                    // attachment, URI permission, microphone or transcript dump.
                    val payload = PrivateSubtitleOutgoingShare.assText(selectedCues)
                    if (payload == null) {
                        error = "ASS-delen geweigerd: alleen exacte centiseconden, " +
                            "maximaal 24 regels en 16 KiB zijn toegestaan."
                    } else {
                        val outbound = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, payload)
                        }
                        val chooser = Intent.createChooser(
                            outbound, "Deel geselecteerde eigen ASS-tekst"
                        )
                        if (!runCatching {
                                context.startActivity(chooser)
                                true
                            }.getOrDefault(false)) {
                            error = "Android kon geen deelmenu openen; niets gedeeld."
                        }
                    }
                },
                onSavePersonalPhrase = { language, phrase, meaningLanguage, ownMeaning ->
                    // Explicit one-cue note; no overwrite of an existing
                    // user-authored meaning or implicit transcript capture.
                    personalPhrases.saveNew(language, phrase, meaningLanguage, ownMeaning)
                },
                onSavePersonalPhraseBatch = { notes ->
                    // One atomic all-or-nothing private transaction.
                    // Never overwrite existing authored meanings.
                    personalPhrases.saveNewBatch(notes)
                },
                onShareTtmlExcerpt = { selectedCues ->
                    // Literal XML text only, never a source-file URI, grant,
                    // recording, account identifier or entire live transcript.
                    val payload = PrivateSubtitleOutgoingShare.ttmlText(selectedCues)
                    if (payload == null) {
                        error = "TTML-delen geweigerd: kies maximaal 24 veilige regels " +
                            "van samen maximaal 16 KiB."
                    } else {
                        val outbound = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, payload)
                        }
                        val chooser = Intent.createChooser(
                            outbound, "Deel geselecteerde eigen TTML-tekst"
                        )
                        if (!runCatching {
                                context.startActivity(chooser)
                                true
                            }.getOrDefault(false)) {
                            error = "Android kan geen deelmenu openen; niets gedeeld."
                        }
                    }
                },
                onShareExcerpt = { selectedCues, format ->
                    // Explicitly chosen original rows from the private editor.
                    // No URI, audio or complete live conversation is transferred.
                    val payload = PrivateSubtitleOutgoingShare.text(selectedCues, format)
                    if (payload == null) {
                        error = "Delen geweigerd: kies maximaal 24 veilige SRT/VTT-" +
                            "regels van samen hooguit 16 KiB."
                    } else {
                        val outbound = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, payload)
                        }
                        val chooser = Intent.createChooser(
                            outbound, "Deel geselecteerde eigen ondertitels"
                        )
                        val launched = runCatching {
                            context.startActivity(chooser)
                            true
                        }.getOrDefault(false)
                        if (!launched) {
                            error = "Android kan geen deelmenu openen. " +
                                "De geselecteerde tekst is niet gedeeld."
                        }
                    }
                },
                onSaveTimedSheet = { editorDraft, exportCues, format ->
                    val bytes = PrivateTimedSubtitleSheet.snapshot(exportCues, format)
                    if (bytes == null) {
                        error = "Geen veilige volledige CSV/TSV-kopie mogelijk."
                    } else {
                        pendingTimedSubtitleSheet = PendingTimedSubtitleSheetSave(
                            generation, editorDraft.toList(), exportCues.toList(),
                            format, bytes
                        )
                        if (format == TimedSubtitleSheetFormat.CSV)
                            saveTimedSubtitleCsv.launch("Tuddy-eigen-tijdcodes.csv")
                        else saveTimedSubtitleTsv.launch("Tuddy-eigen-tijdcodes.tsv")
                    }
                }
            )
        }
        subtitleAudioPreview?.let { (audioCues, audioUri) ->
            LocalSubtitleAudioPreviewDialog(
                cues = audioCues,
                audioUri = audioUri,
                onChangeAudio = {
                    pendingSubtitleAudio = generation to audioCues.toList()
                    subtitleAudioPreview = null
                    openSubtitleAudio.launch(arrayOf("audio/*"))
                },
                readOwnMeaning = ::readOwnWordMeaning,
                saveOwnMeaning = ::saveOwnWordMeaning,
                savePersonalPhrase = { language, phrase, meaningLanguage, ownMeaning ->
                    // This is a deliberately new personally-authored card, never
                    // a silent replacement of an existing private phrase note.
                    personalPhrases.saveNew(language, phrase, meaningLanguage, ownMeaning)
                },
                onApplyAdjustedCues = { adjustedCues ->
                    // A separate RAM-only candidate from explicit audio-preview consent.
                    // Existing editor owns its own undo/redo and explicit SAF save flow.
                    subtitleAudioPreview = null
                    subtitleFileCues = adjustedCues.toList()
                    editedSubtitleSaveNotice = null
                },
                onDismiss = {
                    subtitleAudioPreview = null
                    // Reopen the exact temporary draft as an editable copy.
                    // The originally picked SRT/VTT is never overwritten.
                    subtitleFileCues = audioCues
                }
            )
        }
        if (liveTableOpen) {
            FaceToFaceConversationDialog(
                lines = lines.takeLast(80).map { line ->
                    TypedConversationLine(
                        original = line.original,
                        translated = line.translated,
                        status = line.status,
                        source = line.sourceCode,
                        target = line.targetCode
                    )
                },
                onBack = { liveTableOpen = false }
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
                onClose = { focusMode = false },
                canStartOffline = offlineAvailable && !modelStorageBusy,
                onStart = {
                    if (ContextCompat.checkSelfPermission(context,
                            Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED) {
                        start()
                    } else {
                        microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onCycleManualSpeaker = { id ->
                    // Manual, ephemeral labels; never automatic diarization.
                    if (lines.any { it.id == id }) {
                        when (manuallyAssignedSpeakers[id]) {
                            null -> manuallyAssignedSpeakers[id] = "A"
                            "A" -> manuallyAssignedSpeakers[id] = "B"
                            else -> manuallyAssignedSpeakers.remove(id)
                        }
                    }
                },
                pinnedCaptionIds = captionPins,
                onTogglePin = { id ->
                    val before = captionPins
                    val currentlyVisible = lines.map { it.id }.toSet()
                    captionPins = SessionCaptionPins.toggle(before, id, currentlyVisible)
                    pinsNotice = when {
                        id in before -> "Markering verwijderd uit dit gesprek."
                        id in captionPins -> "Regel tijdelijk gemarkeerd voor dit gesprek."
                        else -> "Maximaal ${SessionCaptionPins.MAX_PINS} sessieregels te markeren."
                    }
                    pinsNotice
                }
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
            pendingArchivePhraseCapture == null && !archiveStudyOpen && !archiveReaderOpen) {
            AlertDialog(
                onDismissRequest = {
                    pendingArchiveExcerpt = null
                    pendingArchivePhraseCapture = null
                    archivePhraseCaptureNotice = ""
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
                        if (archivePhraseCaptureNotice.isNotBlank()) {
                            Text(archivePhraseCaptureNotice,
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
                                    TextButton(
                                        enabled = !listening && row.original.isNotBlank(),
                                        onClick = {
                                            pendingArchivePhraseCapture = PendingArchivePhraseCapture(
                                                generation, archive, row
                                            )
                                            archivePhraseCaptureNotice = ""
                                        }
                                    ) { Text("＋ Maak eigen privékaart…") }
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
                        pendingArchivePhraseCapture = null
                        archivePhraseCaptureNotice = ""
                        archiveExcerptNotice = ""
                        previewArchiveRows = null
                        archiveSearchQuery = ""
                    }) { Text("Sluiten") }
                }
            )
        }

        val archivePhraseRequest = pendingArchivePhraseCapture
        if (archivePhraseRequest != null) {
            val request = archivePhraseRequest
            val stillCurrent = !listening && generation == request.generation &&
                previewArchiveRows === request.archive && request.row in request.archive
            ArchivePrivatePhraseCaptureDialog(
                row = request.row,
                stillCurrent = stillCurrent,
                existingPhrases = personalPhrases.entries(),
                onSave = { language, phrase, meaningLanguage, meaning ->
                    val current = pendingArchivePhraseCapture
                    if (current != null && !listening &&
                        generation == current.generation &&
                        previewArchiveRows === current.archive &&
                        current.row == request.row && current.row in current.archive &&
                        personalPhrases.saveNew(language, phrase, meaningLanguage, meaning)) {
                        archivePhraseCaptureNotice =
                            "Eigen privékaart bewust opgeslagen. Open Mijn eigen offline zinnen om die te bekijken."
                        pendingArchivePhraseCapture = null
                        true
                    } else false
                },
                onDismiss = { pendingArchivePhraseCapture = null }
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
            val chosenPins = pinnedLines.filter {
                pinnedChosenIds == null || it.id in pinnedChosenIds.orEmpty()
            }
            val term = pinnedSearchTerm.trim()
            val pinnedPairs = pinnedLines.map { it.sourceCode to it.targetCode }.distinct()
            val filteredPins = pinnedLines.filter { line ->
                val pairMatches = pinnedManagePair == null ||
                    (line.sourceCode to line.targetCode) == pinnedManagePair
                val scopeMatches = when (pinnedManageScope) {
                    PinnedCaptionScope.ALL -> true
                    PinnedCaptionScope.COMPLETED ->
                        line.status == "Lokaal" || line.status == "Lokaal vertaald"
                    PinnedCaptionScope.TRANSLATED ->
                        line.status == "Lokaal vertaald" && line.translated.isNotBlank()
                }
                pairMatches && scopeMatches && (term.isBlank() ||
                    line.original.contains(term, ignoreCase = true) ||
                    line.translated.contains(term, ignoreCase = true) ||
                    line.sourceCode.contains(term, ignoreCase = true) ||
                    line.targetCode.contains(term, ignoreCase = true))
            }.let { if (pinnedNewestFirst) it.reversed() else it }
            val visiblePinIds = filteredPins.map { it.id }.toSet()
            val studyReadyPins = chosenPins.count {
                val row = TranscriptExportRow(it.original, it.translated, it.status,
                    it.sourceCode, it.targetCode, it.recordedAtMillis)
                val answer = pinnedStudyEdits[it.id]?.takeIf { edit ->
                    edit.appliesTo(row)
                }?.answer ?: it.translated
                it.status == "Lokaal vertaald" &&
                    (!pinnedPracticeOnlyPersonal ||
                        pinnedStudyEdits[it.id]?.appliesTo(row) == true) &&
                    it.original.isNotBlank() && answer.isNotBlank() &&
                    it.original != answer &&
                    it.original.codePointCount(0, it.original.length) <= 400 &&
                    answer.codePointCount(0, answer.length) <= 400
            }
            AlertDialog(
                onDismissRequest = { pinsOpen = false },
                title = { Text("Gemarkeerde gespreksregels") },
                text = {
                    // One scroll owner: on small phones the filters, all selected
                    // rows and the practice action remain reachable.
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(key = "intro") {
                            Text(
                                "Alleen in dit gesprek, maximaal ${SessionCaptionPins.MAX_PINS}. " +
                                    "Sluiten wist de markeringen niet; Wis gesprek wel.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (pinnedLines.isEmpty()) {
                            item(key = "empty") {
                                Text("Geen gemarkeerde zinnen in dit gesprek.")
                            }
                        } else {
                            item(key = "filters") {
                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    OutlinedTextField(
                                        value = pinnedSearchTerm,
                                        onValueChange = { pinnedSearchTerm = it.take(120) },
                                        label = { Text("Zoek mijn gemarkeerde zinnen") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        "${chosenPins.size} van ${pinnedLines.size} gekozen · " +
                                            "${filteredPins.size} zichtbaar. Zoeken en filters " +
                                            "veranderen je gekozen selectie niet.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        TextButton(
                                            onClick = { pinnedPairMenuOpen = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Taalrichting: " +
                                                (pinnedManagePair?.let {
                                                    it.first + " → " + it.second
                                                } ?: "Alle taalrichtingen") + " ▾")
                                        }
                                        DropdownMenu(
                                            expanded = pinnedPairMenuOpen,
                                            onDismissRequest = { pinnedPairMenuOpen = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Alle taalrichtingen") },
                                                onClick = {
                                                    pinnedManagePair = null
                                                    pinnedPairMenuOpen = false
                                                }
                                            )
                                            pinnedPairs.forEach { pair ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(pair.first + " → " + pair.second)
                                                    },
                                                    onClick = {
                                                        pinnedManagePair = pair
                                                        pinnedPairMenuOpen = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    PinnedCaptionScope.entries.forEach { scope ->
                                        TextButton(
                                            onClick = { pinnedManageScope = scope },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text((if (pinnedManageScope == scope) "● " else "○ ") +
                                                scope.title)
                                        }
                                    }
                                    TextButton(onClick = {
                                        pinnedNewestFirst = !pinnedNewestFirst
                                    }, modifier = Modifier.fillMaxWidth()) {
                                        Text(if (pinnedNewestFirst) "Nieuwste eerst ✓"
                                            else "Oudste eerst ✓ · tik voor nieuwste")
                                    }
                                    TextButton(
                                        enabled = visiblePinIds.isNotEmpty(),
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = { pinnedChosenIds = visiblePinIds }
                                    ) { Text("Kies uitsluitend ${visiblePinIds.size} zichtbare regels") }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(
                                            enabled = visiblePinIds.isNotEmpty(),
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                pinnedChosenIds =
                                                    (pinnedChosenIds ?: captionPins) + visiblePinIds
                                            }
                                        ) { Text("Voeg zichtbare toe") }
                                        TextButton(
                                            enabled = visiblePinIds.isNotEmpty(),
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                pinnedChosenIds =
                                                    (pinnedChosenIds ?: captionPins) - visiblePinIds
                                            }
                                        ) { Text("Deselecteer zichtbare") }
                                    }
                                    TextButton(
                                        onClick = { pinnedChosenIds = null },
                                        enabled = chosenPins.size < pinnedLines.size,
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Kies alle markeringen") }
                                }
                            }
                            if (filteredPins.isEmpty()) {
                                item(key = "no_matches") {
                                    Text("Geen resultaten. Pas je zoekterm of filter aan.")
                                }
                            }
                            items(filteredPins, key = { it.id }) { line ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = pinnedChosenIds == null ||
                                                line.id in pinnedChosenIds.orEmpty(),
                                            onCheckedChange = { checked ->
                                                val selected = pinnedChosenIds ?: captionPins
                                                pinnedChosenIds = if (checked)
                                                    selected + line.id else selected - line.id
                                            }
                                        )
                                        Text("In mijn oefen-/exportselectie",
                                            style = MaterialTheme.typography.labelSmall)
                                    }
                                    Text(
                                        DateFormat.getTimeInstance(DateFormat.SHORT)
                                            .format(Date(line.recordedAtMillis)) +
                                            " · " + line.sourceCode + " → " + line.targetCode,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(line.original,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = MaterialTheme.typography.bodyMedium.fontSize *
                                                captionSize.scale
                                        ))
                                    if (line.status == "Lokaal vertaald" ||
                                        line.status == "Lokaal") {
                                        Text(line.translated,
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontSize =
                                                    MaterialTheme.typography.bodyLarge.fontSize *
                                                        captionSize.scale
                                            ))
                                        if (line.status == "Lokaal vertaald" &&
                                            line.translated.isNotBlank()) {
                                            val originalRow = TranscriptExportRow(
                                                line.original, line.translated, line.status,
                                                line.sourceCode, line.targetCode,
                                                line.recordedAtMillis
                                            )
                                            val authored = pinnedStudyEdits[line.id]?.takeIf {
                                                it.appliesTo(originalRow)
                                            }
                                            if (authored != null) {
                                                Text("Mijn gecorrigeerde studieantwoord:",
                                                    style = MaterialTheme.typography.labelMedium)
                                                Text(authored.answer,
                                                    style = MaterialTheme.typography.bodyLarge)
                                            } else {
                                                Text("Modelantwoord: niet onafhankelijk geverifieerd.",
                                                    style = MaterialTheme.typography.bodySmall)
                                            }
                                            TextButton(enabled = !listening, onClick = {
                                                captionSpeaker.stop()
                                                pinnedStudyEditId = line.id
                                                pinnedStudyEditSignature = listOf(
                                                    line.original, line.translated,
                                                    line.sourceCode, line.targetCode
                                                )
                                                pinnedStudyDraft = authored?.answer
                                                    ?: line.translated
                                                pinnedStudyEditError = ""
                                                pinsOpen = false
                                            }) { Text(if (authored == null)
                                                "✎ Corrigeer alleen studieantwoord"
                                                else "✎ Bewerk mijn studieantwoord") }
                                            if (authored != null) {
                                                TextButton(onClick = {
                                                    pinnedStudyEdits.remove(line.id)
                                                    pinsNotice =
                                                        "Eigen studiecorrectie hersteld naar model."
                                                }) { Text("Gebruik weer modelvertaling") }
                                            }
                                        }
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
                                    TextButton(onClick = {
                                        pinnedRemovedUndo = setOf(line.id)
                                        captionPins = captionPins - line.id
                                        pinnedChosenIds = pinnedChosenIds?.minus(line.id)
                                        pinsNotice = "Ster verwijderd. Herstel hieronder indien nodig."
                                    }) { Text("Verwijder alleen deze ster") }
                                    HorizontalDivider()
                                }
                            }
                            item(key = "practice") {
                                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = pinnedPracticeOnlyPersonal,
                                            onCheckedChange = {
                                                pinnedPracticeOnlyPersonal = it
                                            }
                                        )
                                        Text("Oefen alleen mijn handmatig gecorrigeerde antwoorden",
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (pinnedPracticeOnlyPersonal && studyReadyPins == 0) {
                                        Text("Geen geschikte eigen studiecorrecties " +
                                            "in de gekozen zinnen. Corrigeer eerst een " +
                                            "modelantwoord of zet het filter uit.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error)
                                    }
                                    OutlinedButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = studyReadyPins > 0 && !listening,
                                        onClick = {
                                            captionSpeaker.stop()
                                            pinsOpen = false
                                            pinnedPracticeOpen = true
                                        }
                                    ) {
                                        Text("Oefen ${studyReadyPins} gekozen vertaalparen")
                                    }
                                    Text(
                                        "Tijdelijke leerkaarten; alleen echt vertaalde zinnen. " +
                                            "Sluiten wist de oefenronde.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    TextButton(onClick = {
                                        pinnedRemovedUndo = captionPins
                                        captionPins = emptySet()
                                        pinnedChosenIds = null
                                        pinsNotice = "Alle sterren gewist. Herstel de laatste " +
                                            "verwijdering hieronder zolang dit scherm open is."
                                    }) { Text("Wis markeringen") }
                                }
                            }
                        }
                        if (pinsNotice.isNotBlank()) {
                            item(key = "notice") {
                                Text(pinsNotice, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (pinnedRemovedUndo.isNotEmpty()) {
                            item(key = "undo_removed") {
                                val availableIds = lines.map { it.id }.toSet()
                                val restore = pinnedRemovedUndo.intersect(availableIds)
                                val withinLimit = (captionPins + restore).size <=
                                    SessionCaptionPins.MAX_PINS
                                TextButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = restore.isNotEmpty() && withinLimit,
                                    onClick = {
                                        captionPins = captionPins + restore
                                        pinnedChosenIds = null
                                        pinnedRemovedUndo = emptySet()
                                        pinsNotice = "${restore.size} ster(ren) hersteld."
                                    }
                                ) { Text("↶ Herstel ${restore.size} laatst verwijderde ster(ren)") }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { pinsOpen = false }) { Text("Sluiten") }
                },
                dismissButton = {
                    TextButton(
                        enabled = chosenPins.isNotEmpty() && !listening,
                        onClick = {
                            pinsNotice = ""
                            pinnedExportNotice = ""
                            pinnedExactPreviewOpen = false
                            pinnedExportOpen = true
                            pinsOpen = false
                        }
                    ) { Text("Exporteer ${chosenPins.size} gekozen…") }
                }
            )
        }
        if (pinnedStudyEditId != null) {
            val editId = pinnedStudyEditId
            val editLine = lines.firstOrNull { it.id == editId && it.id in captionPins }
            if (editLine == null || editLine.status != "Lokaal vertaald" ||
                editLine.translated.isBlank()) {
                LaunchedEffect(editId) {
                    pinnedStudyEditId = null
                    pinnedStudyEditSignature = null
                    pinnedStudyEditError = ""
                }
            } else {
                AlertDialog(
                    onDismissRequest = {
                        pinnedStudyEditId = null
                        pinnedStudyEditError = ""
                        pinsOpen = true
                    },
                    title = { Text("Eigen studieantwoord · alleen voor oefenkaarten") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Originele zin: " + editLine.original,
                                style = MaterialTheme.typography.bodyMedium)
                            Text("Modelvertaling: " + editLine.translated,
                                style = MaterialTheme.typography.bodySmall)
                            OutlinedTextField(
                                value = pinnedStudyDraft,
                                onValueChange = {
                                    pinnedStudyDraft = it.take(850)
                                    pinnedStudyEditError = ""
                                },
                                label = { Text("Mijn eigen betekenis · max. 400 tekens") },
                                minLines = 2,
                                maxLines = 5,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("Dit is jouw eigen notitie, geen gecontroleerde vertaling. " +
                                "Wijzigt nooit de herkende tekst, modelvertaling, " +
                                "privé-JSON/HTML of ondertitels. Alleen in RAM.",
                                style = MaterialTheme.typography.bodySmall)
                            if (pinnedStudyEditError.isNotBlank()) {
                                Text(pinnedStudyEditError,
                                    color = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val answer = pinnedStudyDraft.trim()
                            val stillOriginal = pinnedStudyEditSignature == listOf(
                                editLine.original, editLine.translated,
                                editLine.sourceCode, editLine.targetCode
                            )
                            if (!stillOriginal) {
                                pinnedStudyEditError =
                                    "De oorspronkelijke zin of vertaling is veranderd. " +
                                        "Annuleer en open de correctie opnieuw."
                            } else if (!PrivatePinnedCaptionExport.validStudyEdit(answer)) {
                                pinnedStudyEditError =
                                    "Vul één veilige tekstregel in (maximaal 400 Unicode-tekens)."
                            } else {
                                if (answer == editLine.translated) {
                                    pinnedStudyEdits.remove(editLine.id)
                                } else {
                                    pinnedStudyEdits[editLine.id] = PinnedStudyCorrection(
                                        editLine.original, editLine.translated,
                                        editLine.sourceCode, editLine.targetCode, answer
                                    )
                                }
                                pinnedStudyEditId = null
                                pinnedStudyEditSignature = null
                                pinnedStudyEditError = ""
                                pinsNotice = "Studieantwoord aangepast, bron ongewijzigd."
                                pinsOpen = true
                            }
                        }) { Text("Gebruik in mijn oefenkaarten") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            pinnedStudyEditId = null
                            pinnedStudyEditError = ""
                            pinsOpen = true
                        }) { Text("Annuleren") }
                    }
                )
            }
        }
        if (pinnedExportOpen && !pinnedExactPreviewOpen) {
            val selection = currentPinnedSelection()
            val included = remember(selection, pinnedExportScope,
                pinnedExportFormat, pinnedSubtitleOptions, pinnedLanguagePair,
                pinnedStudyEdits.toMap(), pinnedOnlyPersonalStudy,
                pinnedStudyLanguageTags
            ) {
                PrivatePinnedCaptionExport.includedRows(
                    selection, pinnedExportFormat, pinnedSubtitleOptions,
                    pinnedExportScope, pinnedLanguagePair,
                    pinnedStudyEdits.toMap(), pinnedOnlyPersonalStudy
                )
            }
            val computedPreview = remember(selection, pinnedExportScope,
                pinnedExportFormat, pinnedSubtitleOptions, pinnedLanguagePair,
                pinnedStudyDirection, pinnedStudyEdits.toMap(),
                pinnedOnlyPersonalStudy, pinnedStudyLanguageTags, pinnedClozeDifficulty, pinnedClozeHintMode, pinnedClozeAnswerSpace, pinnedClozeAudience, pinnedClozePass, pinnedClozeCardOrder
            ) {
                PrivatePinnedCaptionExport.snapshot(
                    selection, pinnedExportFormat, pinnedSubtitleOptions,
                    pinnedExportScope, pinnedLanguagePair, pinnedStudyDirection,
                    pinnedStudyEdits.toMap(), pinnedOnlyPersonalStudy,
                    pinnedStudyLanguageTags, pinnedClozeDifficulty, pinnedClozeHintMode, pinnedClozeAnswerSpace, pinnedClozeAudience, pinnedClozePass, pinnedClozeCardOrder
                )
            }
            AlertDialog(
                onDismissRequest = {
                    pinnedExportOpen = false
                    pinnedExactPreviewOpen = false
                },
                title = { Text("Alleen gemarkeerde regels bewaren") },
                text = {
                    Column(
                        modifier = Modifier.heightIn(max = 470.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "${selection.size} gekozen regels uit maximaal ${SessionCaptionPins.MAX_PINS}. " +
                                "Alle andere zinnen blijven buiten het bestand. " +
                                "Controleer hieronder het formaat. De gekozen Android-provider " +
                                "kan deze gevoelige tekst opslaan, delen of synchroniseren.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        PinnedCaptionFormat.entries.forEach { format ->
                            TextButton(onClick = {
                                pinnedExportFormat = format
                                pinnedExportNotice = ""
                            }) {
                                Text((if (pinnedExportFormat == format) "● " else "○ ") +
                                    format.title)
                            }
                        }
                        if ((pinnedExportFormat == PinnedCaptionFormat.TSV ||
                            pinnedExportFormat == PinnedCaptionFormat.CSV ||
                            pinnedExportFormat == PinnedCaptionFormat.STUDY_HTML ||
                            pinnedExportFormat == PinnedCaptionFormat.FOLD_HTML ||
                            pinnedExportFormat == PinnedCaptionFormat.CLOZE_HTML)) {
                            HorizontalDivider()
                            Text("Kies hoe je de tweetalige modelvertalingen wilt oefenen",
                                style = MaterialTheme.typography.titleSmall)
                            PinnedStudyDirection.entries.forEach { direction ->
                                TextButton(onClick = {
                                    pinnedStudyDirection = direction
                                    pinnedExportNotice = ""
                                }) {
                                    Text((if (pinnedStudyDirection == direction) "● " else "○ ") +
                                        direction.title)
                                }
                            }
                            if (pinnedExportFormat == PinnedCaptionFormat.CLOZE_HTML) {
                                HorizontalDivider()
                                Text("Hoeveel woorden wil je zelf aanvullen?",
                                    style = MaterialTheme.typography.titleSmall)
                                PinnedClozeDifficulty.entries.forEach { difficulty ->
                                    TextButton(onClick = {
                                        pinnedClozeDifficulty = difficulty
                                        pinnedExportNotice = ""
                                    }) {
                                        Text((if (pinnedClozeDifficulty == difficulty) "● "
                                            else "○ ") + difficulty.title)
                                    }
                                }
                                Text("Hoeveel hulp bieden de verborgen woorden?",
                                    style = MaterialTheme.typography.titleSmall)
                                PinnedClozeHintMode.entries.forEach { hintMode ->
                                    TextButton(onClick = {
                                        pinnedClozeHintMode = hintMode
                                        pinnedExportNotice = ""
                                    }) {
                                        Text((if (pinnedClozeHintMode == hintMode) "● "
                                            else "○ ") + hintMode.title)
                                    }
                                }
                                HorizontalDivider()
                                Text("Hoeveel schrijfruimte wil je op papier?",
                                    style = MaterialTheme.typography.titleSmall)
                                PinnedClozeAnswerSpace.entries.forEach { space ->
                                    TextButton(onClick = {
                                        pinnedClozeAnswerSpace = space
                                        pinnedExportNotice = ""
                                    }) {
                                        Text((if (pinnedClozeAnswerSpace == space) "● "
                                            else "○ ") + space.title)
                                    }
                                }
                                HorizontalDivider()
                                Text("Voor wie is dit oefenbestand?",
                                    style = MaterialTheme.typography.titleSmall)
                                PinnedClozeAudience.entries.forEach { audience ->
                                    TextButton(onClick = {
                                        pinnedClozeAudience = audience
                                        pinnedExportNotice = ""
                                    }) {
                                        Text((if (pinnedClozeAudience == audience) "● "
                                            else "○ ") + audience.title)
                                    }
                                }
                                Text(if (pinnedClozeAudience == PinnedClozeAudience.STUDENT_HANDOUT)
                                    "Leerlingenblad: geen volledige vertaling of antwoorden, " +
                                        "ook niet verborgen in de HTML-broncode. Bewaar " +
                                        "voor jezelf zo nodig een aparte zelfstudieversie."
                                else "Zelfstudie: antwoorden achter openklapkeuzes. " +
                                    "Het antwoordblad is in de browser aanvankelijk verborgen " +
                                    "en wordt bij afdrukken alleen toegevoegd als je daar " +
                                    "eerst zelf het vakje 'Print ook het antwoordblad' aanvinkt.",
                                    style = MaterialTheme.typography.bodySmall)
                                HorizontalDivider()
                                Text("Welke ronde(s) wil je oefenen?",
                                    style = MaterialTheme.typography.titleSmall)
                                PinnedClozePass.entries.forEach { pass ->
                                    TextButton(onClick = {
                                        pinnedClozePass = pass
                                        pinnedExportNotice = ""
                                    }) {
                                        Text((if (pinnedClozePass == pass) "● " else "○ ") +
                                            pass.title)
                                    }
                                }
                                HorizontalDivider()
                                Text("In welke volgorde wil je de kaarten?",
                                    style = MaterialTheme.typography.titleSmall)
                                PinnedClozeCardOrder.entries.forEach { cardOrder ->
                                    TextButton(onClick = {
                                        pinnedClozeCardOrder = cardOrder
                                        pinnedExportNotice = ""
                                    }) {
                                        Text((if (pinnedClozeCardOrder == cardOrder) "● "
                                            else "○ ") + cardOrder.title)
                                    }
                                }
                                Text("Afwisselend: ronde 1 in bronvolgorde, ronde 2 " +
                                    "achterstevoren en ronde 3 met verschoven begin. " +
                                    "Eén kaart kan uiteraard niet van positie wisselen.",
                                    style = MaterialTheme.typography.bodySmall)
                                Text("Bij meerdere rondes komen dezelfde zinnen terug " +
                                    "met andere ontbrekende woorden. De volgorde ligt " +
                                    "vast; er worden geen nieuwe vertalingen gemaakt.",
                                    style = MaterialTheme.typography.bodySmall)
                                Text("De gekozen moeilijkheid en exacte invulhints " +
                                    "komen ook op je eigen oefenblad. Dit is geen " +
                                    "geautomatiseerde taal- of juistheidsbeoordeling.",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = pinnedOnlyPersonalStudy,
                                    onCheckedChange = {
                                        pinnedOnlyPersonalStudy = it
                                        pinnedExportNotice = ""
                                    }
                                )
                                Text("Alleen mijn handmatig gecorrigeerde antwoorden",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = pinnedStudyLanguageTags,
                                    onCheckedChange = {
                                        pinnedStudyLanguageTags = it
                                        pinnedExportNotice = ""
                                    }
                                )
                                Text("Toon taalcode bij elke kaartzijde",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            Text("Zelf corrigeren is geen onafhankelijke verificatie. " +
                                "De oorspronkelijke modelvertalingen blijven ongewijzigd.",
                                style = MaterialTheme.typography.bodySmall)
                            Text(
                                "Alleen daadwerkelijk voltooide lokale vertalingen komen in " +
                                    "de kaarten. Geen onvoltooide betekenissen, automatische " +
                                    "kwaliteitscontrole of leerprofiel. Handmatig importeren " +
                                    (when (pinnedExportFormat) {
                                        PinnedCaptionFormat.CSV ->
                                            "als UTF-8 CSV met twee gequote kolommen."
                                        PinnedCaptionFormat.STUDY_HTML ->
                                            "als zelfstandig HTML-oefenblad: per vraag antwoord " +
                                                "openklappen, met aparte afdrukpagina."
                                        PinnedCaptionFormat.FOLD_HTML ->
                                            "als afdrukbare HTML-vouwkaartjes: een zijde bedrukken, " +
                                                "knippen en de rechter betekenis achter de vraag vouwen."
                                        PinnedCaptionFormat.CLOZE_HTML ->
                                            "als lokaal HTML-aanvulblad: ontbrekende woorden, " +
                                                "handmatig te openen antwoorden en apart antwoordblad."
                                        else -> "als UTF-8 TSV met twee tabgescheiden velden " +
                                            "(voor- en achterkant)."
                                    }),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        HorizontalDivider()
                        Text("Welke taalrichting wil je bewaren?",
                            style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = {
                            pinnedLanguagePair = null
                            pinnedExportNotice = ""
                        }) {
                            Text((if (pinnedLanguagePair == null) "● " else "○ ") +
                                "Alle taalrichtingen")
                        }
                        val languagePairs = selection.map {
                            it.caption.sourceCode to it.caption.targetCode
                        }.distinct()
                        languagePairs.forEach { pair ->
                            TextButton(onClick = {
                                pinnedLanguagePair = pair
                                pinnedExportNotice = ""
                            }) {
                                Text((if (pinnedLanguagePair == pair) "● " else "○ ") +
                                    pair.first + " → " + pair.second)
                            }
                        }
                        HorizontalDivider()
                        Text("Welke gemarkeerde regels wil je meenemen?",
                            style = MaterialTheme.typography.titleSmall)
                        PinnedCaptionScope.entries.forEach { scope ->
                            TextButton(onClick = {
                                pinnedExportScope = scope
                                pinnedExportNotice = ""
                            }) {
                                Text((if (pinnedExportScope == scope) "● " else "○ ") +
                                    scope.title)
                            }
                        }
                        Text(
                            if ((pinnedExportFormat == PinnedCaptionFormat.TSV ||
                            pinnedExportFormat == PinnedCaptionFormat.CSV ||
                            pinnedExportFormat == PinnedCaptionFormat.STUDY_HTML ||
                            pinnedExportFormat == PinnedCaptionFormat.FOLD_HTML ||
                            pinnedExportFormat == PinnedCaptionFormat.CLOZE_HTML))
                                "${included.size} unieke, werkelijk vertaalde paren van " +
                                    "${selection.size} markeringen (identieke paren worden " +
                                    "niet dubbel geëxporteerd)."
                            else
                                "${included.size} van ${selection.size} gemarkeerde regels " +
                                    "komen in de export. Een niet-voltooide vertaling wordt " +
                                    "nooit als afgerond voorgesteld.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if ((pinnedExportFormat == PinnedCaptionFormat.TSV ||
                            pinnedExportFormat == PinnedCaptionFormat.CSV ||
                            pinnedExportFormat == PinnedCaptionFormat.STUDY_HTML ||
                            pinnedExportFormat == PinnedCaptionFormat.FOLD_HTML ||
                            pinnedExportFormat == PinnedCaptionFormat.CLOZE_HTML)) {
                            val cards = included.size *
                                (if (pinnedStudyDirection == PinnedStudyDirection.BOTH) 2 else 1) *
                                (if (pinnedExportFormat == PinnedCaptionFormat.CLOZE_HTML)
                                    pinnedClozePass.indices.size else 1)
                            Text(
                                "$cards studiekaarten in " +
                                    (when (pinnedExportFormat) {
                                        PinnedCaptionFormat.CSV -> "CSV"
                                        PinnedCaptionFormat.STUDY_HTML -> "oefen-HTML"
                                        PinnedCaptionFormat.FOLD_HTML -> "vouw-HTML"
                                        PinnedCaptionFormat.CLOZE_HTML -> "aanvul-HTML"
                                        else -> "TSV"
                                    }) + (when (pinnedExportFormat) {
                                        PinnedCaptionFormat.STUDY_HTML ->
                                            " met antwoorden om open te klappen en aparte afdrukpagina. "
                                        PinnedCaptionFormat.FOLD_HTML ->
                                            " met linker vragen en rechter vouwantwoorden. "
                                        PinnedCaptionFormat.CLOZE_HTML ->
                                            " (" + pinnedClozeDifficulty.title + "; " +
                                                pinnedClozeHintMode.title + "; " + pinnedClozeAnswerSpace.title + "; " + pinnedClozeAudience.title +
                                                "; " + pinnedClozePass.title +
                                                "; " + pinnedClozeCardOrder.title +
                                                ") met invulwoorden " +
                                                (if (pinnedClozeAudience == PinnedClozeAudience.SELF_STUDY)
                                                    "en optioneel afdrukbaar antwoordblad. "
                                                else "zonder volledige antwoorden. ")
                                        else -> " met twee tekstkolommen. "
                                    }) +
                                    (if (pinnedOnlyPersonalStudy)
                                        "Alleen handmatig gecorrigeerde antwoorden; " +
                                            "nog steeds niet onafhankelijk geverifieerd."
                                    else "Overige lokale modelvertalingen zijn " +
                                        "niet onafhankelijk gecontroleerd.") +
                                    " Open het exportbestand alleen in vertrouwde apps.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (pinnedExportFormat == PinnedCaptionFormat.CLOZE_HTML &&
                                included.isNotEmpty()
                            ) {
                                val perRound = included.size *
                                    (if (pinnedStudyDirection == PinnedStudyDirection.BOTH)
                                        2 else 1)
                                pinnedClozePass.indices.forEach { round ->
                                    val cardNumbers =
                                        PrivatePinnedCaptionClozeHtml.orderedCardIndices(
                                            perRound, round, pinnedClozeCardOrder
                                        )
                                    Text("Ronde ${round + 1} · kaartvolgorde uit " +
                                        "de oorspronkelijke selectie: " +
                                        cardNumbers.take(12).joinToString(" → ") {
                                            (it + 1).toString()
                                        } + (if (cardNumbers.size > 12)
                                            " … (${cardNumbers.size} kaarten)" else "") +
                                        ". De vraag- en antwoordnummers worden " +
                                        "in de gekozen volgorde opnieuw genummerd.",
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (pinnedOnlyPersonalStudy && included.isEmpty()) {
                                Text("Er zijn geen geschikte persoonlijke correcties " +
                                    "in deze selectie. Keer terug naar ★ gemarkeerd " +
                                    "om je studieantwoorden te verbeteren.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (pinnedExportFormat == PinnedCaptionFormat.SRT ||
                            pinnedExportFormat == PinnedCaptionFormat.WEBVTT
                        ) {
                            Text(
                                "Geschatte ondertiteltijden: geen audio-opname, " +
                                    "geen automatische synchronisatie. Corrigeer eerst in je editor.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            SubtitleDraftControls(
                                options = pinnedSubtitleOptions.copy(
                                    format = if (pinnedExportFormat == PinnedCaptionFormat.SRT)
                                        SubtitleDraftFormat.SRT else SubtitleDraftFormat.WEBVTT
                                ),
                                onChange = {
                                    pinnedSubtitleOptions = it
                                    pinnedExportFormat = if (it.format == SubtitleDraftFormat.SRT)
                                        PinnedCaptionFormat.SRT else PinnedCaptionFormat.WEBVTT
                                    pinnedExportNotice = ""
                                }
                            )
                        }
                        HorizontalDivider()
                        Text("Controleer de inhoud van de export",
                            style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (computedPreview == null) "Geen geldig document met deze keuzes."
                            else "Berekende bestandsgrootte: ${computedPreview.size} bytes.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (included.isNotEmpty()) {
                            val visible = if (pinnedExportShowAll) included else included.take(5)
                            visible.forEachIndexed { index, record ->
                                Text(
                                    "${index + 1}. ${record.caption.sourceCode} → " +
                                        "${record.caption.targetCode} · ${record.caption.status}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if ((pinnedExportFormat == PinnedCaptionFormat.TSV ||
                            pinnedExportFormat == PinnedCaptionFormat.CSV ||
                            pinnedExportFormat == PinnedCaptionFormat.STUDY_HTML ||
                            pinnedExportFormat == PinnedCaptionFormat.FOLD_HTML ||
                            pinnedExportFormat == PinnedCaptionFormat.CLOZE_HTML)) {
                                    val originalRow = selection.firstOrNull {
                                        it.id == record.id
                                    }?.caption
                                    val manuallyEdited = originalRow?.let {
                                        pinnedStudyEdits[record.id]?.appliesTo(it)
                                    } == true
                                    Text(if (manuallyEdited)
                                        "✓ Zelf geschreven studieantwoord"
                                        else "⚠ Ongeverifieerde modelvertaling",
                                        style = MaterialTheme.typography.labelSmall)
                                    val source = (if (pinnedStudyLanguageTags)
                                        "[${record.caption.sourceCode}] " else "") +
                                        PrivatePinnedCaptionExport.preview(record.caption.original)
                                    val meaning = (if (pinnedStudyLanguageTags)
                                        "[${record.caption.targetCode}] " else "") +
                                        PrivatePinnedCaptionExport.preview(record.caption.translated)
                                    val reverse = pinnedStudyDirection ==
                                        PinnedStudyDirection.TARGET_TO_SOURCE
                                    val studentCopy = pinnedExportFormat ==
                                        PinnedCaptionFormat.CLOZE_HTML &&
                                        pinnedClozeAudience == PinnedClozeAudience.STUDENT_HANDOUT
                                    val frontCode = if (reverse)
                                        record.caption.targetCode else record.caption.sourceCode
                                    val backCode = if (reverse)
                                        record.caption.sourceCode else record.caption.targetCode
                                    val frontDirection = if (
                                        PrivatePinnedCaptionClozeHtml.isRightToLeft(frontCode))
                                        TextDirection.Rtl else TextDirection.Ltr
                                    val backDirection = if (
                                        PrivatePinnedCaptionClozeHtml.isRightToLeft(backCode))
                                        TextDirection.Rtl else TextDirection.Ltr
                                    Text(if (studentCopy) "Zichtbare opdrachtzin:"
                                        else "Voorzijde:",
                                        style = MaterialTheme.typography.labelSmall)
                                    Text(if (reverse) meaning else source,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            textDirection = frontDirection))
                                    if (studentCopy) {
                                        Text("De volledige achterkant komt NIET in het " +
                                            "leerlingenbestand of de HTML-bron terecht. " +
                                            "Alleen de gekozen invulhint wordt getoond.",
                                            style = MaterialTheme.typography.bodySmall)
                                    } else {
                                        Text("Achterzijde:",
                                            style = MaterialTheme.typography.labelSmall)
                                        Text(if (reverse) source else meaning,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                textDirection = backDirection))
                                    }
                                    if (pinnedStudyDirection == PinnedStudyDirection.BOTH) {
                                        Text(if (studentCopy)
                                            "Ook omgekeerd: opdrachtzin (zonder antwoord)"
                                        else "Omgekeerde kaart:",
                                            style = MaterialTheme.typography.labelSmall)
                                        Text(meaning, style =
                                            MaterialTheme.typography.bodySmall.copy(
                                                textDirection = if (
                                                    PrivatePinnedCaptionClozeHtml.isRightToLeft(
                                                        record.caption.targetCode))
                                                    TextDirection.Rtl else TextDirection.Ltr))
                                        if (!studentCopy) {
                                            Text(source, style =
                                                MaterialTheme.typography.bodySmall.copy(
                                                    textDirection = if (
                                                        PrivatePinnedCaptionClozeHtml.isRightToLeft(
                                                            record.caption.sourceCode))
                                                        TextDirection.Rtl else TextDirection.Ltr))
                                        }
                                    }
                                    if (pinnedExportFormat == PinnedCaptionFormat.CLOZE_HTML) {
                                        pinnedClozePass.indices.forEach { round ->
                                            val targetHint = PrivatePinnedCaptionClozeHtml.previewHint(
                                                if (reverse) record.caption.original
                                                else record.caption.translated,
                                                if (!pinnedStudyLanguageTags) null
                                                else if (reverse) record.caption.sourceCode
                                                else record.caption.targetCode,
                                                pinnedClozeDifficulty, pinnedClozeHintMode, round
                                            )
                                            Text("Ronde ${round + 1} · aanvulhint:",
                                                style = MaterialTheme.typography.labelSmall)
                                            Text(targetHint,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    textDirection = backDirection))
                                            if (pinnedStudyDirection == PinnedStudyDirection.BOTH) {
                                                Text("Ronde ${round + 1} · omgekeerde aanvulhint:",
                                                    style = MaterialTheme.typography.labelSmall)
                                                Text(PrivatePinnedCaptionClozeHtml.previewHint(
                                                    record.caption.original,
                                                    if (pinnedStudyLanguageTags)
                                                        record.caption.sourceCode else null,
                                                    pinnedClozeDifficulty,
                                                    pinnedClozeHintMode, round
                                                ), style = MaterialTheme.typography.bodySmall.copy(
                                                    textDirection = frontDirection))
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        PrivatePinnedCaptionExport.preview(record.caption.original),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (record.caption.status == "Lokaal vertaald") {
                                        Text("↳ " + PrivatePinnedCaptionExport.preview(
                                            record.caption.translated),
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            if (included.size > 5) {
                                TextButton(onClick = {
                                    pinnedExportShowAll = !pinnedExportShowAll
                                }) {
                                    Text(if (pinnedExportShowAll) "Toon minder"
                                    else "Bekijk alle ${included.size} regels")
                                }
                            }
                        }
                        OutlinedButton(
                            enabled = computedPreview != null,
                            onClick = { pinnedExactPreviewOpen = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Bekijk exacte bestandsinhoud…") }
                        if (pinnedExportNotice.isNotBlank()) {
                            Text(pinnedExportNotice, color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            "Er wordt pas geschreven nadat je zelf op bewaren drukt " +
                                "en een bestand kiest. Een gewijzigde selectie wordt geweigerd.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = !listening && selection.isNotEmpty(),
                        onClick = {
                            val format = pinnedExportFormat
                            val options = pinnedSubtitleOptions
                            val scope = pinnedExportScope
                            val languagePair = pinnedLanguagePair
                            val bytes = PrivatePinnedCaptionExport.snapshot(
                                selection, format, options, scope, languagePair,
                                pinnedStudyDirection, pinnedStudyEdits.toMap(),
                                pinnedOnlyPersonalStudy, pinnedStudyLanguageTags,
                                pinnedClozeDifficulty, pinnedClozeHintMode, pinnedClozeAnswerSpace, pinnedClozeAudience, pinnedClozePass, pinnedClozeCardOrder
                            )
                            if (bytes == null) {
                                pinnedExportNotice = "Geen geldige selectie voor dit formaat. " +
                                    "Controleer voltooide vertalingen en tijdcodes."
                            } else {
                                pendingPinnedExport = PendingPinnedCaptionExport(
                                    generation, selection, format, scope,
                                    languagePair, pinnedStudyDirection,
                                    pinnedStudyEdits.toMap(), pinnedOnlyPersonalStudy,
                                    pinnedStudyLanguageTags, pinnedClozeDifficulty, pinnedClozeHintMode, pinnedClozeAnswerSpace, pinnedClozeAudience, pinnedClozePass, pinnedClozeCardOrder,
                                    options, bytes
                                )
                                exportStatus = ""
                                pinnedExportOpen = false
                                savePinnedCaptionFile.launch(
                                    Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = format.mimeType
                                        putExtra(Intent.EXTRA_TITLE,
                                            if (format == PinnedCaptionFormat.CLOZE_HTML &&
                                                pinnedClozeAudience == PinnedClozeAudience.STUDENT_HANDOUT)
                                                "Tuddy-leerlingenblad-zonder-antwoorden.html"
                                            else format.fileName)
                                    }
                                )
                            }
                        }
                    ) { Text("Kies opslag…") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        pinnedExportOpen = false
                        pinnedExactPreviewOpen = false
                    }) {
                        Text("Annuleren")
                    }
                }
            )
        }
        if (pinnedExportOpen && pinnedExactPreviewOpen) {
            val exactBytes = PrivatePinnedCaptionExport.snapshot(
                currentPinnedSelection(), pinnedExportFormat,
                pinnedSubtitleOptions, pinnedExportScope, pinnedLanguagePair,
                pinnedStudyDirection, pinnedStudyEdits.toMap(),
                pinnedOnlyPersonalStudy, pinnedStudyLanguageTags,
                pinnedClozeDifficulty, pinnedClozeHintMode, pinnedClozeAnswerSpace, pinnedClozeAudience, pinnedClozePass, pinnedClozeCardOrder
            )
            val previewLines = remember(exactBytes?.contentHashCode(),
                exactBytes?.size
            ) {
                exactBytes?.toString(Charsets.UTF_8)?.lineSequence()?.toList()
                    ?: emptyList()
            }
            AlertDialog(
                onDismissRequest = { pinnedExactPreviewOpen = false },
                title = { Text("Exact te bewaren document · alleen lezen") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Dit is de ruwe inhoud van het gekozen bestand, " +
                                "inclusief JSON/HTML-opmaak indien gekozen. " +
                                "Niets is nog opgeslagen of gedeeld.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (exactBytes == null) {
                            Text("Selectie gewijzigd of ongeldig; kies opnieuw.",
                                color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("${exactBytes.size} bytes · volledig lokaal",
                                style = MaterialTheme.typography.labelSmall)
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)
                            ) {
                                items(previewLines) { line ->
                                    Text(line, fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { pinnedExactPreviewOpen = false }) {
                        Text("Terug naar exportkeuze")
                    }
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
        if (offlinePersonalTurnsOpen) {
            OfflinePersonalPhraseTurnsDialog(
                initialLanguage = previewTarget,
                onManagePhrases = {
                    offlinePersonalTurnsOpen = false
                    personalPhrasesOpen = true
                },
                onDismiss = { offlinePersonalTurnsOpen = false }
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
                incomingCsvDocument = incomingPersonalCsv,
                onIncomingCsvConsumed = onPersonalCsvConsumed,
                onDismiss = {
                    personalPhrasesOpen = false
                    incomingPersonalCsv?.let { onPersonalCsvConsumed(it.requestId) }
                }
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
                            pinnedStudyEdits.remove(id)
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
                        dailyStudyHubOpen = false
                        ownWordsOpen = false
                        personalPhrasesOpen = false
                        flashcardsOpen = false
                        studyEraseNotice = if (wordsCleared && ownWordsCleared &&
                            phrasesCleared && favoritesCleared && typedFavoritesCleared) {
                            "Opgeslagen basiswoorden, hun herhaalplanning, eigen woorden, eigen zinnen, " +
                                "spraakfavorieten en tekstfavorieten gewist. " +
                                "Gesprek, taalkeuze en eerdere externe exports zijn behouden."
                        } else {
                            "Niet alles kon worden gewist. " +
                                "Basiswoorden en herhaalplanning: ${if (wordsCleared) "gewist" else "mislukt"}, " +
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
        if (dailyStudyHubOpen) {
            DailyStudyHub(
                words = savedVocabulary,
                phrases = personalPhrases,
                ownWords = ownWordDeck,
                onManageWords = {
                    dailyStudyHubOpen = false
                    savedWordsOpen = true
                },
                onManagePhrases = {
                    dailyStudyHubOpen = false
                    personalPhrasesOpen = true
                },
                onManageOwnWords = {
                    dailyStudyHubOpen = false
                    ownWordsOpen = true
                },
                onWordChanged = { savedWordRevision++ },
                onOwnWordChanged = { ownWordRevision++ },
                onDismiss = { dailyStudyHubOpen = false }
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
                        // A conscious conversation wipe also discards snapshots
                        // held solely for a pending external document picker.
                        pendingManualSubtitleDocument = null
                        pendingAuthoredLrcSourceSave = null
                        readyFirstPasteDocument = null
                        readySecondPasteDocument = null
                        pendingTimedSubtitleSheet = null
                        pendingLrcSubtitleSave = null
                        pendingSbvSubtitleSave = null
                        pendingAssSubtitleSave = null
                        pendingTtmlSubtitleSave = null
                        pendingTranscriptExport = null
                        pendingPinnedExport = null
                        pinnedExportOpen = false
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
                        pendingLivePhraseCapture = null
                        capturedPhraseText = ""
                        capturedMeaningText = ""
                        capturedPhraseNotice = ""
                        correctionNotice = ""
                        pendingSingleCaptionDelete = null
                        captionDeleteNotice = ""
                        stop()
                        offlineModelStatus = ""
                        lines.clear()
                        manuallyAssignedSpeakers.clear()
                        captionPins = emptySet()
                        pinnedChosenIds = null
                        pinnedRemovedUndo = emptySet()
                        pinnedStudyEdits.clear()
                        pinnedStudyEditId = null
                        pinnedSearchTerm = ""
                        pinsOpen = false
                        pinnedExportOpen = false
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
                        enabled = lines.any {
                            it.status == "Lokaal vertaald" &&
                                it.original.isNotBlank() && it.translated.isNotBlank()
                        },
                        onClick = {
                            stop() // table presentation never keeps the mic active
                            captionSpeaker.stop()
                            focusMode = false
                            liveTableOpen = true
                        }
                    ) { Text("↕ Tafelmodus") }
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
                    enabled = !listening,
                    onClick = { offlinePersonalTurnsOpen = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("⇄ Offline gesprek met mijn eigen Kurmanji/Sorani/Pasjtoe-zinnen") }
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
                    TextButton(enabled = !listening, onClick = {
                        pendingSubtitleOpenGeneration = generation
                        subtitleFileCues = null
                        // Android document providers inconsistently report
                        // Text subtitle files including SBV can use unexpected provider MIME types.
                        // Let the user intentionally choose a file; the strict
                        // bounded parser remains the only acceptance gate.
                        openSubtitleFile.launch(arrayOf("*/*"))
                    }) { Text("Open ondertitelbestand…") }
                    Text(
                        "Bekijk een bewaard gesprek zonder je live gesprek te vervangen.",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !listening,
                    onClick = { subtitlePasteOpen = true }
                ) { Text("Plak of importeer ondertiteltekst…") }

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
                OutlinedButton(
                    onClick = {
                        if (listening) stop()
                        dailyStudyHubOpen = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🧠 Vandaag offline oefenen · woorden en eigen zinnen")
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { savedWordsOpen = true }) {
                        Text("Mijn woorden ($savedWordCount)")
                    }
                    Text(
                        if (savedVocabulary.reviewsEnabled())
                            "Zelf bewaarde basiswoorden · offline · $scheduledReviewDueCount vandaag"
                        else "Zelf bewaarde basiswoorden · offline",
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
                            OutlinedTextField(
                                value = sourceQuery,
                                onValueChange = { sourceQuery = it },
                                label = { Text("Zoek spreektaal") },
                                modifier = Modifier.width(310.dp).padding(horizontal = 10.dp),
                                singleLine = true
                            )
                            val matchingSpoken = searchSpokenLanguages(sourceQuery)
                            if (matchingSpoken.isEmpty()) {
                                Text(
                                    "Geen spreektaal gevonden.",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            matchingSpoken.forEach {
                                DropdownMenuItem(text = { Text(it.label) }, onClick = {
                                    selectBrowserSpeechSource(it.speechCode)
                                    offlineReadinessOpen = false
                                })
                            }
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(onClick = { targetOpen = true }, enabled = !modelStorageBusy, modifier = Modifier.fillMaxWidth()) {
                            Text("Naar: ${target.label}", maxLines = 1)
                        }
                        DropdownMenu(expanded = targetOpen, onDismissRequest = { targetOpen = false }) {
                            Text("59 vertaaltalen · offline na modelvoorbereiding", modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
                            OutlinedTextField(
                                value = targetQuery,
                                onValueChange = { targetQuery = it },
                                label = { Text("Zoek taal") },
                                modifier = Modifier.width(310.dp).padding(horizontal = 10.dp),
                                singleLine = true
                            )
                            val matchingTargets = TargetCatalog.search(targetQuery).take(35)
                            if (matchingTargets.isEmpty()) {
                                val previewSuggestion = CommunityLanguageAvailabilityHint.targetForQuery(targetQuery)
                                Text(
                                    CommunityLanguageAvailabilityHint.forQuery(targetQuery)
                                        ?: "Geen vertaaltaal gevonden.",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (previewSuggestion != null) {
                                    val previewLabel = when (previewSuggestion) {
                                        "kmr" -> "Kurmanji"
                                        "ckb" -> "Sorani"
                                        else -> "Pasjtoe"
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Selecteer $previewLabel voor de basiswoordenpreview") },
                                        onClick = {
                                            previewTarget = previewSuggestion
                                            previewResult = null
                                            targetQuery = ""
                                            targetOpen = false
                                        }
                                    )
                                }
                            }
                            matchingTargets.forEach { choice ->
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
                OutlinedButton(
                    onClick = { languageSupportOpen = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Bekijk alle taalondersteuning")
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
                            pinnedStudyEdits.remove(removed.id)
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
                        onClick = {
                            pinnedChosenIds = null
                            pinnedRemovedUndo = emptySet()
                            pinnedSearchTerm = ""
                            pinnedManageScope = PinnedCaptionScope.ALL
                            pinnedManagePair = null
                            pinnedPairMenuOpen = false
                            pinnedPracticeOnlyPersonal = false
                            pinnedNewestFirst = false
                            pinsOpen = true
                        }
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
                        label = { Text("Zoek tekst, vertaling of eigen uitleg") },
                        supportingText = {
                            Text("Zoekt ook met veelvoorkomende Arabisch/Perzische toetsenbordvarianten; opgeslagen tekst blijft exact behouden.")
                        },
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
                if (capturedPhraseNotice.isNotBlank()) {
                    Text(capturedPhraseNotice, style = MaterialTheme.typography.bodySmall,
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
                        val isHistorySearchMatch = historySearchMatches(line)
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(
                                if (isHistorySearchMatch) 2.dp else 1.dp,
                                if (isHistorySearchMatch) MaterialTheme.colorScheme.primary
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
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(0.dp)
                                ) {
                                    val speaker = manuallyAssignedSpeakers[line.id]
                                    Text(
                                        if (speaker == null) "Spreker: onbekend (handmatig)"
                                        else "Spreker $speaker (handmatig)",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    TextButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            when (speaker) {
                                                null -> manuallyAssignedSpeakers[line.id] = "A"
                                                "A" -> manuallyAssignedSpeakers[line.id] = "B"
                                                else -> manuallyAssignedSpeakers.remove(line.id)
                                            }
                                        }
                                    ) { Text("Wijzig spreker (A / B / onbekend)") }
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
                                // Lazy-list scoped only: recycled, off-screen captions do not
                                // retain an open editing menu or a persisted preference.
                                var captionToolsExpanded by remember(line.id) {
                                    mutableStateOf(false)
                                }
                                TextButton(
                                    onClick = { captionToolsExpanded = !captionToolsExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (captionToolsExpanded) "⌃ Sluit bewerk- en bewaaropties"
                                        else "⋯ Corrigeer, maak privékaart en meer")
                                }
                                if (captionToolsExpanded) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text("Alle acties gelden uitsluitend voor deze gespreksregel.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        TextButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !listening,
                                        onClick = {
                                            editingCaptionId = line.id
                                            captionCorrectionDraft = line.original
                                            correctionNotice = ""
                                        }
                                    ) { Text("✎ Corrigeer tekst") }
                                    TextButton(
                                        modifier = Modifier.fillMaxWidth(),
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
                                    TextButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !listening && line.original.isNotBlank(),
                                        onClick = {
                                            pendingLivePhraseCapture = PendingLivePhraseCapture(
                                                line.id, line.original, line.translationRevision, generation
                                            )
                                            capturedPhraseLanguage = "kmr"
                                            capturedMeaningLanguage =
                                                if (line.sourceCode == "en") "en" else "nl"
                                            capturedPhraseText = ""
                                            capturedMeaningText = ""
                                            capturedPhraseNotice = ""
                                        }
                                    ) { Text("＋ Maak eigen Kurdische/Pasjtoe-kaart") }
                                    if (line.status == "Fout" || line.status == "Model ontbreekt") {
                                        TextButton(
                                            modifier = Modifier.fillMaxWidth(),
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
                                TextButton(
                                    modifier = Modifier.fillMaxWidth(),
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
                                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                                        if (listening) stop()
                                        captionSpeaker.stop()
                                        presentedCaptionId = line.id
                                    }) { Text("▣ Toon deze zin groot") }
                                }
                                TextButton(modifier = Modifier.fillMaxWidth(), onClick = {
                                    pendingSingleCaptionDelete = Triple(
                                        line.id, line.original, line.translationRevision)
                                }) { Text("🗑 Wis deze regel") }
                                }
                                }
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
