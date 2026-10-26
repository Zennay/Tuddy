package com.livelingo.android

import android.content.Intent
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.Locale
import com.livelingo.android.ui.LocalCaptionPlaybackMenu
import com.livelingo.android.ui.InstalledLocalVoicePickerDialog
import com.livelingo.android.ui.InstalledDeviceVoiceCatalogDialog

// Internal-only hints for Tuddy's reviewed ACTION_SEND draft handoff.
// The QuickTranslateActivity is not exported, and every hint is validated
// against the actual ML Kit target catalog before display.
internal const val QUICK_SOURCE_CODE_EXTRA = "com.livelingo.android.extra.QUICK_SOURCE_CODE"
internal const val QUICK_TARGET_CODE_EXTRA = "com.livelingo.android.extra.QUICK_TARGET_CODE"

/**
 * Android's selected-text action. Translation is explicit, local-only and
 * returns to the originating editor only after a separate user tap.
 */
internal class QuickTranslateActivity : ComponentActivity() {
    private val inventory = OfflineTextModelInventory()
    private val screenLockGate by lazy { LocalScreenLockPrivacyGate(this) }
    private val detector = LocalLanguageDetector()
    private var generation = 0L
    private var detectorRevision = 0L
    private var localSpeaker: LocalCaptionSpeaker? = null
    private var playbackRevision = 0L
    private var activeTranslator: Translator? = null
    // A submitted ML Kit Task may keep running after the user cancels or
    // Android pauses the screen. Never close its native translator early.
    private val clientsWithSubmittedTasks = mutableSetOf<Translator>()
    private val clientsWaitingToClose = mutableSetOf<Translator>()
    // Private Android selection stays in RAM only, and the initiating app
    // receives no text until a separate explicit user decision.
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private var translationTimeout: Runnable? = null
    private var detectionTimeout: Runnable? = null

    private fun clearTranslationTimeout() {
        translationTimeout?.let(timeoutHandler::removeCallbacks)
        translationTimeout = null
    }

    private fun clearDetectionTimeout() {
        detectionTimeout?.let(timeoutHandler::removeCallbacks)
        detectionTimeout = null
    }

    private fun armTranslationTimeout(token: Long): Boolean {
        clearTranslationTimeout()
        val timeout = Runnable {
            translationTimeout = null
            if (token == generation && busy && !isFinishing) {
                complete(token, null,
                    "De lokale vertaling antwoordde niet binnen 30 seconden. " +
                        "Probeer opnieuw of beheer de offline modellen in Tuddy. " +
                        "Er is niets naar de bronapp teruggestuurd.")
            }
        }
        translationTimeout = timeout
        val accepted = timeoutHandler.postDelayed(timeout, TRANSLATION_DEADLINE_MS)
        if (!accepted) clearTranslationTimeout()
        return accepted
    }

    private fun armDetectionTimeout(requestRevision: Long): Boolean {
        clearDetectionTimeout()
        val timeout = Runnable {
            detectionTimeout = null
            if (requestRevision == detectorRevision && detectingSource && !isFinishing) {
                detectorRevision++
                detector.close()
                detectingSource = false
                detectionNotice = "De lokale brontaalherkenning antwoordde niet binnen 12 seconden. " +
                    "Kies de brontaal handmatig of probeer opnieuw."
            }
        }
        detectionTimeout = timeout
        val accepted = timeoutHandler.postDelayed(timeout, DETECTION_DEADLINE_MS)
        if (!accepted) clearDetectionTimeout()
        return accepted
    }

    private var incomingText by mutableStateOf("")
    private var inputNotice by mutableStateOf("")
    private var selectionReadOnly by mutableStateOf(true)
    private var sourceCode by mutableStateOf("nl")
    private var targetCode by mutableStateOf("en")
    private var busy by mutableStateOf(false)
    private var detectingSource by mutableStateOf(false)
    private var detectionNotice by mutableStateOf("")
    // Never apply local language detection silently to a private selection.
    private var suggestedSourceCode by mutableStateOf<String?>(null)
    private var status by mutableStateOf("")
    private var playbackNotice by mutableStateOf("")
    private var headphonesOnlyPlayback by mutableStateOf(false)
    private var respectQuietModePlayback by mutableStateOf(false)
    private var clipboardExpirySeconds by mutableStateOf(0)
    private var privateScreenShieldOn by mutableStateOf(false)
    private var privateScreenShieldNotice by mutableStateOf("")
    private var translationResult by mutableStateOf<String?>(null)
    // Not presented or exported until the ENTIRE selected-text translation
    // succeeds. An incomplete local prefix is kept in foreground RAM only.
    private var retryCheckpoint by
        mutableStateOf<PrivateSelectedTranslationCheckpoint.State?>(null)
    // Original ML Kit output is retained only while this Activity stays in
    // the foreground. A corrected draft is never persisted or sent silently.
    private var machineTranslationResult by mutableStateOf<String?>(null)
    private var reviewingTranslation by mutableStateOf(false)
    private var reviewDraft by mutableStateOf("")
    private var manuallyAdjustedTranslation by mutableStateOf(false)
    private var reviewNotice by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The main conversation and selected-text Activity share this opt-in.
        // Shield the *very first* selected-text frame, before reading its text.
        PrivateScreenShield.restore(this)
        privateScreenShieldOn = PrivateScreenShield.enabled(this)
        headphonesOnlyPlayback = PrivatePlaybackRoutingPreference.headphonesOnly(this)
        respectQuietModePlayback = PrivatePlaybackRoutingPreference.respectQuietMode(this)
        clipboardExpirySeconds = PrivateClipboardExpiry.selectedSeconds(this)

        val acceptedAction = intent.action == Intent.ACTION_PROCESS_TEXT &&
            intent.type?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT) == "text/plain"
        val supplied = if (acceptedAction) runCatching {
            intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
        }.getOrNull() else null
        val textReadOnly = intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
        val incoming = if (acceptedAction) SharedPlainTextIntent.read(intent) else null

        // Do not retain a private selection in this Activity's Intent.
        intent.removeExtra(Intent.EXTRA_PROCESS_TEXT)
        intent.removeExtra(Intent.EXTRA_TEXT)

        if (incoming != null && incoming.draft == null && incoming.notice == null &&
            supplied != null
        ) {
            // Structured Tuddy backups and subtitle documents still go through
            // their existing strict preview/import routes, never the translator.
            startActivity(Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_PROCESS_TEXT
                type = "text/plain"
                putExtra(Intent.EXTRA_PROCESS_TEXT, supplied)
                putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, textReadOnly)
            })
            finish()
            return
        }

        val defaults = defaultLanguagePair()
        val hintedSource = runCatching {
            intent.getStringExtra(QUICK_SOURCE_CODE_EXTRA)
        }.getOrNull()?.takeIf { it in TargetCatalog.codes }
        val hintedTarget = runCatching {
            intent.getStringExtra(QUICK_TARGET_CODE_EXTRA)
        }.getOrNull()?.takeIf { it in TargetCatalog.codes }
        sourceCode = hintedSource ?: defaults.first
        targetCode = hintedTarget ?: defaults.second
        intent.removeExtra(QUICK_SOURCE_CODE_EXTRA)
        intent.removeExtra(QUICK_TARGET_CODE_EXTRA)
        selectionReadOnly = textReadOnly
        incomingText = incoming?.draft.orEmpty()
        inputNotice = incoming?.notice ?: if (!acceptedAction) {
            "Deze actie verwacht geselecteerde platte tekst uit een Android-app."
        } else {
            ""
        }

        setContent {
            MaterialTheme {
                QuickTranslateScreen(
                    text = incomingText,
                    inputNotice = inputNotice,
                    readOnly = selectionReadOnly,
                    sourceCode = sourceCode,
                    targetCode = targetCode,
                    busy = busy,
                    detectingSource = detectingSource,
                    detectionNotice = detectionNotice,
                    suggestedSourceCode = suggestedSourceCode,
                    status = status,
                    playbackNotice = playbackNotice,
                    headphonesOnly = headphonesOnlyPlayback,
                    respectQuietMode = respectQuietModePlayback,
                    clipboardExpirySeconds = clipboardExpirySeconds,
                    privateScreenShieldOn = privateScreenShieldOn,
                    privateScreenShieldNotice = privateScreenShieldNotice,
                    result = translationResult,
                    resumePartNumber = retryCheckpoint?.completedChunks?.plus(1),
                    resumeTotalParts = retryCheckpoint?.totalChunks ?: 0,
                    reviewingTranslation = reviewingTranslation,
                    reviewDraft = reviewDraft,
                    reviewNotice = reviewNotice,
                    manuallyAdjustedTranslation = manuallyAdjustedTranslation,
                    onBeginReview = { beginReviewTranslation() },
                    onReviewDraftChange = { updateReviewDraft(it) },
                    onSaveReview = { saveReviewedTranslation() },
                    onCancelReview = { cancelReviewTranslation() },
                    onRestoreMachineTranslation = { restoreMachineTranslation() },
                    onSourceSelected = { code -> changePair(source = code) },
                    onTargetSelected = { code -> changePair(target = code) },
                    onSwapLanguages = { changePair(source = targetCode, target = sourceCode) },
                    onSuggestSource = { suggestSourceLanguage() },
                    onTranslate = { startTranslation() },
                    onResumeFailedPart = { startTranslation(resume = true) },
                    onCancel = { cancelByUser() },
                    onPlayOriginal = { pace -> speakSelection(incomingText, sourceCode, pace) },
                    onPlayTranslation = { pace ->
                        translationResult?.let { speakSelection(it, targetCode, pace) }
                    },
                    onPlayBilingual = { pace ->
                        speakBilingualSelection(pace)
                    },
                    onStopPlayback = { stopPlayback() },
                    onRespectQuietModeChange = { selected ->
                        stopPlayback()
                        val saved = PrivatePlaybackRoutingPreference.setRespectQuietMode(
                            this, selected
                        )
                        respectQuietModePlayback =
                            PrivatePlaybackRoutingPreference.respectQuietMode(this)
                        playbackNotice = if (!saved)
                            "Stille-modusvoorkeur kon niet worden opgeslagen; " +
                                "vorige keuze blijft actief."
                        else if (respectQuietModePlayback)
                            "Stil/trillen respecteren aan: geen lokale spraak in " +
                                "deze Android-belstand, ook niet via de hoofdtelefoon."
                        else "Stille-modusbeveiliging uit; alleen expliciet Play start TTS."
                    },
                    onHeadphonesOnlyChange = { selected ->
                        stopPlayback()
                        val saved = PrivatePlaybackRoutingPreference.setHeadphonesOnly(
                            this, selected
                        )
                        headphonesOnlyPlayback =
                            PrivatePlaybackRoutingPreference.headphonesOnly(this)
                        playbackNotice = if (!saved)
                            "Audiovoorkeur kon niet worden opgeslagen; vorige keuze blijft actief."
                        else if (selected)
                            "Alleen-hoofdtelefoonstand gekozen. Aangesloten Android-headset " +
                                "vereist vóór elk lokaal voorlezen."
                        else "Hoofdtelefoonvereiste uit. Nieuwe afspeelactie kan luidspreker gebruiken."
                    },
                    onPrivateScreenShieldChange = { requested ->
                        // Android window flag is applied before storing the
                        // cross-screen preference; failure leaves it unchanged.
                        val saved = PrivateScreenShield.change(this, requested)
                        privateScreenShieldOn = PrivateScreenShield.enabled(this)
                        privateScreenShieldNotice = if (!saved) {
                            "Android-schermbeveiliging kon niet worden gewijzigd."
                        } else if (privateScreenShieldOn) {
                            "Privéweergave aan: screenshots, schermopname en appvoorbeelden " +
                                "worden door Android zoveel mogelijk geblokkeerd."
                        } else {
                            "Privéweergave uit: de tekst kan zichtbaar zijn in " +
                                "screenshots, schermopname of Android-appvoorbeelden."
                        }
                    },
                    onClipboardExpiryChange = { seconds ->
                        val saved = PrivateClipboardExpiry.changeSeconds(this, seconds)
                        clipboardExpirySeconds = PrivateClipboardExpiry.selectedSeconds(this)
                        status = if (saved)
                            "Klembordtimer gewijzigd voor volgende kopieeracties. " +
                                "Wissen is afhankelijk van Android en of Tuddy nog actief is."
                        else "Klembordtimer kon niet worden opgeslagen."
                    },
                    onCopy = { copyTranslation() },
                    onShare = { shareTranslation() },
                    onCopyBilingual = { copyBilingualHandoff() },
                    onShareBilingual = { shareBilingualHandoff() },
                    onReturnResult = { returnTranslationToCaller() },
                    onOpenTuddy = { openTuddy() },
                    onClose = { closeAndFinish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Even before ON_PAUSE, a protected Android SCREEN_OFF event clears
        // imported selection, reviewed translation and all local audio.
        // Never repopulate them automatically after the device unlocks.
        if (!screenLockGate.arm { redactForScreenLock() }) {
            redactForScreenLock()
        }
        // A setting change in the other Tuddy screen must not leave stale
        // protection or a misleading checkbox on this Activity.
        PrivateScreenShield.restore(this)
        privateScreenShieldOn = PrivateScreenShield.enabled(this)
        headphonesOnlyPlayback = PrivatePlaybackRoutingPreference.headphonesOnly(this)
        respectQuietModePlayback = PrivatePlaybackRoutingPreference.respectQuietMode(this)
    }

    private fun defaultLanguagePair(): Pair<String, String> {
        val deviceLanguage = Locale.getDefault().language.lowercase(Locale.ROOT)
            .takeIf { it in TargetCatalog.codes } ?: "nl"
        val source = deviceLanguage
        val target = if (source == "en") "nl" else "en"
        return source to target
    }

    private fun discardReview() {
        reviewingTranslation = false
        reviewDraft = ""
        reviewNotice = ""
        manuallyAdjustedTranslation = false
        machineTranslationResult = null
    }

    private fun beginReviewTranslation() {
        val completed = translationResult ?: return
        if (busy || detectingSource || isFinishing) return
        stopPlayback()
        reviewDraft = completed
        reviewNotice = ""
        reviewingTranslation = true
    }

    private fun updateReviewDraft(text: String) {
        if (!reviewingTranslation) return
        // Keep an extra UTF-16 unit for a visible validation error, never
        // silently truncate a correction to a different intended sentence.
        if (text.length > LocalTranslationReviewPolicy.MAX_EDIT_BUFFER_UNITS) {
            reviewNotice = "Maximaal 8.000 tekens. Er is niets opgeslagen."
            return
        }
        reviewDraft = text
        // Immediate, content-free explanation rather than an inexplicable
        // disabled Save button. Do not persist or copy rejected characters.
        reviewNotice = if (text.isBlank()) "" else
            LocalTranslationReviewPolicy.validationNotice(text).orEmpty()
    }

    private fun saveReviewedTranslation() {
        if (!reviewingTranslation || busy || detectingSource || isFinishing ||
            translationResult == null || machineTranslationResult == null) return
        val normalized = LocalTranslationReviewPolicy.normalize(reviewDraft)
        if (normalized == null) {
            reviewNotice = LocalTranslationReviewPolicy.validationNotice(reviewDraft)
                ?: "Deze vertaling voldoet niet aan de veilige Android-tekstgrens."
            return
        }
        stopPlayback()
        translationResult = normalized
        manuallyAdjustedTranslation = normalized != machineTranslationResult
        reviewingTranslation = false
        reviewDraft = ""
        reviewNotice = if (manuallyAdjustedTranslation)
            "Je eigen tekst is lokaal overgenomen; niet door Tuddy geverifieerd. " +
                "Alleen een volgende bewuste actie kan deze tekst extern delen."
        else "De oorspronkelijke lokale ML Kit-vertaling is ongewijzigd gebleven."
    }

    private fun cancelReviewTranslation() {
        if (!reviewingTranslation) return
        reviewDraft = ""
        reviewingTranslation = false
        reviewNotice = "Wijziging geannuleerd; de vorige vertaling blijft beschikbaar."
    }

    private fun restoreMachineTranslation() {
        val original = machineTranslationResult ?: return
        if (busy || detectingSource || isFinishing) return
        stopPlayback()
        translationResult = original
        manuallyAdjustedTranslation = false
        reviewingTranslation = false
        reviewDraft = ""
        reviewNotice = "Oorspronkelijke lokale ML Kit-uitkomst hersteld. Niet automatisch gedeeld."
    }

    private fun changePair(source: String? = null, target: String? = null) {
        discardReview()
        detectorRevision++
        clearDetectionTimeout()
        detector.close()
        detectingSource = false
        detectionNotice = ""
        suggestedSourceCode = null
        cancelOperation()
        sourceCode = source ?: sourceCode
        targetCode = target ?: targetCode
        translationResult = null
        status = ""
    }

    private fun suggestSourceLanguage() {
        if (busy || detectingSource || inputNotice.isNotBlank()) return
        stopPlayback()
        // ML Kit language ID itself only inspects up to 200 characters.
        // Never imply that a prediction has classified an entire long
        // multilingual selection; the visible proposal is advisory only.
        val sample = incomingText.replace('\n', ' ').trim().take(200)
        suggestedSourceCode = null
        if (sample.codePointCount(0, sample.length) < 20) {
            detectionNotice = "Voor een betrouwbaar lokaal brontaalvoorstel zijn minimaal 20 tekens nodig. " +
                "Kies bij korte teksten de taal zelf."
            return
        }
        detector.close()
        val requestRevision = ++detectorRevision
        detectingSource = true
        detectionNotice = "Brontaalvoorstel lokaal berekenen…"
        if (!armDetectionTimeout(requestRevision)) {
            detectorRevision++
            detector.close()
            detectingSource = false
            detectionNotice = "Lokale brontaalherkenning kon niet veilig starten."
            return
        }
        detector.identify(sample) { detected ->
            if (isFinishing || requestRevision != detectorRevision) return@identify
            clearDetectionTimeout()
            detectingSource = false
            val supported = detected
                ?.let(TranslateLanguage::fromLanguageTag)
                ?.takeIf { it in TargetCatalog.codes }
            if (supported == null) {
                detectionNotice = if (detected == null) {
                    "Geen bruikbaar lokaal brontaalvoorstel. Kies de taal handmatig."
                } else {
                    "Het lokale voorstel '$detected' valt buiten de tekstvertaalcatalogus. Kies handmatig."
                }
            } else {
                suggestedSourceCode = supported.takeIf { it != sourceCode }
                val label = TargetCatalog.targets.firstOrNull { it.code == supported }?.title ?: supported
                detectionNotice = if (suggestedSourceCode == null)
                    "De lokale brontaalinschatting is $label; dit is al je gekozen brontaal."
                else "Lokaal brontaalvoorstel: $label ($supported). " +
                    "Controleer de suggestie en tik op Gebruik brontaal om je taalkeuze te wijzigen. " +
                    "De geselecteerde taal is nog niet aangepast of vertaald."
            }
            detector.close()
        }
    }

    private fun startTranslation(resume: Boolean = false) {
        if (busy || detectingSource || inputNotice.isNotBlank() || incomingText.isBlank()) return
        if (sourceCode !in TargetCatalog.codes || targetCode !in TargetCatalog.codes) {
            status = "Kies een ondersteunde bron- en doeltaal."
            return
        }
        if (sourceCode == targetCode) {
            status = "Kies twee verschillende talen."
            return
        }

        val planned = LocalSelectedParagraphPlan.fromSelection(incomingText)
        if (planned == null) {
            status = "Selecteer maximaal 8.000 geldige platte teksttekens. " +
                "Te lange of ongeldige selecties worden niet gedeeltelijk vertaald."
            return
        }
        val previous = if (resume) retryCheckpoint?.takeIf {
            PrivateSelectedTranslationCheckpoint.isUsable(
                it, incomingText, sourceCode, targetCode, planned
            )
        } else null
        if (resume && previous == null) {
            retryCheckpoint = null
            status = "Het eerdere resultaat hoort niet meer bij deze selectie of taalrichting. " +
                "Tik op Vertaal lokaal voor een nieuwe volledige poging."
            return
        }
        cancelOperation()
        // Deliberate same-selection retry is the ONLY route allowed to retain
        // a completed prefix after the old provider/client has been invalidated.
        retryCheckpoint = previous
        discardReview()
        val token = generation
        busy = true
        translationResult = null
        status = "Geïnstalleerde lokale modellen opnieuw controleren…"
        if (!armTranslationTimeout(token)) {
            complete(token, null, "Lokale vertaling kon niet veilig worden gestart.")
            return
        }

        inventory.check(sourceCode, targetCode, TargetCatalog.codes) { receipt ->
            if (token != generation || isFinishing) return@check
            if (receipt.stage == OfflineTextStage.CHECKING) {
                status = receipt.detail
                return@check
            }
            if (receipt.stage != OfflineTextStage.READY) {
                val nextStep = if (receipt.stage == OfflineTextStage.MISSING) {
                    " Open Tuddy → Offline startcheck → Tekst via wifi; deze selectieactie downloadt niets."
                } else {
                    " Er is geen online vertaalfallback."
                }
                complete(token, null, receipt.detail + nextStep)
                return@check
            }

            val client = runCatching {
                Translation.getClient(
                    TranslatorOptions.Builder()
                        .setSourceLanguage(sourceCode)
                        .setTargetLanguage(targetCode)
                        .build()
                )
            }.getOrNull()
            if (client == null) {
                complete(token, null,
                    "De lokale vertaler kon niet worden geopend. Er is niets teruggestuurd.")
                return@check
            }

            activeTranslator = client
            // Retry only the failed and remaining parts of the same exact
            // selection. The private prefix is not shown or shared here.
            translatePlannedPart(
                client, planned, token, previous?.completedChunks ?: 0,
                StringBuilder(previous?.translatedPrefix.orEmpty())
            )
        }
    }

    /**
     * Process paragraph pieces sequentially with one global ML Kit admission
     * slot. No intermediate translation is shown, shared or returned to the
     * originating Android editor. User cancel/background invalidates the
     * generation, and no additional chunks start afterwards.
     */
    private fun translatePlannedPart(
        client: Translator,
        chunks: List<LocalSelectedParagraphPlan.Chunk>,
        token: Long,
        index: Int,
        assembled: StringBuilder
    ) {
        if (token != generation || isFinishing || !busy) return
        status = "Gedeelte ${index + 1} van ${chunks.size} lokaal vertalen… " +
            "Pas na alle gedeelten komt de volledige vertaling beschikbaar."
        val slot = LocalMlKitWorkBudget.shared.acquire()
        if (slot == null) {
            complete(token, null, "Android verwerkt te veel lokale vertaalopdrachten. " +
                "Geen gedeeltelijke tekst gedeeld. Probeer later opnieuw.")
            return
        }
        val task = try {
            client.translate(chunks[index].text)
        } catch (_: Exception) {
            slot.release()
            complete(token, null, "Dit tekstgedeelte kon niet lokaal starten. " +
                "Er is geen gedeeltelijke vertaling gedeeld.")
            return
        }
        clientsWithSubmittedTasks.add(client)
        try {
            task.addOnCompleteListener { finished ->
                slot.release()
                clientsWithSubmittedTasks.remove(client)
                val delayedClose = clientsWaitingToClose.remove(client)
                if (delayedClose) runCatching { client.close() }
                if (token != generation || isFinishing || !busy) return@addOnCompleteListener

                val output = if (finished.isSuccessful)
                    runCatching { finished.result }.getOrNull()?.let(::normalizeTranslation)
                else null
                if (output == null) {
                    complete(token, null, "Lokaal gedeelte ${index + 1}/${chunks.size} " +
                        "mislukt of gaf geen bruikbare tekst. " +
                        "Geen gedeeltelijke vertaling gedeeld of teruggestuurd.")
                    return@addOnCompleteListener
                }
                if (assembled.length + chunks[index].separatorBefore.length +
                    output.length > MAX_RESULT_UNITS) {
                    retryCheckpoint = null
                    complete(token, null, "De volledige lokale vertaling werd groter " +
                        "dan de veilige 8.000-tekensgrens. Er is niets gedeeld.")
                    return@addOnCompleteListener
                }
                assembled.append(chunks[index].separatorBefore).append(output)
                if (index + 1 == chunks.size) {
                    complete(token, assembled.toString(),
                        "Alle ${chunks.size} tekstgedeelten lokaal vertaald. " +
                            "Er is nog niets naar een andere app gestuurd.")
                } else {
                    retryCheckpoint = PrivateSelectedTranslationCheckpoint.completed(
                        incomingText, sourceCode, targetCode,
                        chunks, index + 1, assembled.toString()
                    )
                    translatePlannedPart(client, chunks, token, index + 1, assembled)
                }
            }
        } catch (_: Exception) {
            // Task might have started despite listener-registration failure.
            // Retain its native client and global slot until process exit.
            complete(token, null, "Android kon dit lokale tekstgedeelte niet volgen. " +
                "Geen gedeeltelijke vertaling gedeeld.")
        }
    }

    private companion object {
        const val MAX_RESULT_UNITS = 8_000
        const val TRANSLATION_DEADLINE_MS = 30_000L
        const val DETECTION_DEADLINE_MS = 12_000L
    }

    private fun normalizeTranslation(raw: String): String? {
        // Preserve paragraph breaks, refuse malformed Unicode and control
        // payloads before a result is rendered, copied, spoken or shared.
        return LocalTextBoundaryPolicy.selectedTranslation(raw)
    }

    private fun complete(token: Long, result: String?, message: String) {
        if (token != generation || isFinishing) return
        clearTranslationTimeout()
        closeActiveTranslator()
        busy = false
        translationResult = result
        if (result != null) retryCheckpoint = null
        machineTranslationResult = result
        reviewingTranslation = false
        reviewDraft = ""
        reviewNotice = ""
        manuallyAdjustedTranslation = false
        status = message
        generation++
    }

    private fun cancelOperation() {
        stopPlayback()
        clearTranslationTimeout()
        clearDetectionTimeout()
        generation++
        inventory.invalidate()
        closeActiveTranslator()
        retryCheckpoint = null
        busy = false
    }

    private fun cancelByUser() {
        if (!busy && !detectingSource) return
        detectorRevision++
        detector.close()
        detectingSource = false
        clearDetectionTimeout()
        cancelOperation()
        translationResult = null
        discardReview()
        detectionNotice = ""
        suggestedSourceCode = null
        status = "Actie geannuleerd. Er is niets teruggestuurd, gedeeld of opgeslagen."
    }

    private fun closeActiveTranslator() {
        val current = activeTranslator
        activeTranslator = null
        if (current != null) {
            if (current in clientsWithSubmittedTasks) {
                // Do not close a client while an actual native task is running.
                clientsWaitingToClose.add(current)
            } else {
                runCatching { current.close() }
            }
        }
    }

    private fun speakSelection(text: String, language: String, pace: LocalSpeechPace) {
        if (busy || detectingSource || reviewingTranslation ||
            inputNotice.isNotBlank() || isFinishing) return
        stopPlayback()
        // Validate ALL text before speaking the first word. A corrupt or
        // oversized selection cannot be spoken partially or via online TTS.
        val planned = LocalSelectedSpeechPlan.fromSelection(text)
        if (planned == null) {
            playbackNotice = "Deze tekst is te lang of bevat ongeldige tekens " +
                "voor veilig lokaal voorlezen (maximaal 8.000 tekens)."
            return
        }
        val revision = playbackRevision
        val speaker = localSpeaker ?: LocalCaptionSpeaker(this).also { localSpeaker = it }
        if (planned.size == 1 && planned[0].length <= 400) {
            speaker.speak(planned[0], language, pace) { notice ->
                if (revision == playbackRevision && !isFinishing) playbackNotice = notice
            }
            return
        }
        speaker.speakSequence(
            lines = planned,
            languageTag = language,
            pace = pace,
            onProgress = { completed, total ->
                if (revision == playbackRevision && !isFinishing) {
                    playbackNotice = "Lokaal afgespeeld: $completed van $total tekstdelen."
                }
            },
            feedback = { notice ->
                if (revision == playbackRevision && !isFinishing) playbackNotice = notice
            },
            onFinished = { complete ->
                if (revision == playbackRevision && !isFinishing && complete) {
                    playbackNotice = "Voorlezen voltooid: ${planned.size} lokale tekstdelen."
                }
            },
            onLineStarted = { index ->
                if (revision == playbackRevision && !isFinishing) {
                    playbackNotice = "Android spreekt lokaal deel ${index + 1} van ${planned.size}…"
                }
            }
        )
    }

    private fun speakBilingualSelection(pace: LocalSpeechPace) {
        val translated = translationResult ?: return
        if (busy || detectingSource || reviewingTranslation ||
            inputNotice.isNotBlank() || isFinishing || incomingText.isBlank()) return
        stopPlayback()
        val speaker = localSpeaker ?: LocalCaptionSpeaker(this).also { localSpeaker = it }
        speaker.speakBilingualPair(
            incomingText, translated, sourceCode, targetCode, pace
        ) { notice ->
            if (!isFinishing) playbackNotice = notice
        }
    }

    private fun stopPlayback() {
        playbackRevision++ // Fence old sequence progress, TTS and lifecycle callbacks.
        localSpeaker?.stop()
        playbackNotice = ""
    }

    /**
     * Every outbound Android path must revalidate the precise current text.
     * A manually corrected result must have the SAME Unicode controls as the
     * original local ML Kit output; no truncation or silent sanitization.
     */
    private fun outboundTranslationOrExplain(): String? {
        val result = translationResult ?: return null
        if (LocalTranslationReviewPolicy.normalize(result) != result) {
            status = LocalTranslationReviewPolicy.validationNotice(result)
                ?: "De uitgaande vertaling is gewijzigd of bevat onveilige tekens. " +
                    "Er is niets naar Android verstuurd."
            return null
        }
        return result
    }

    private fun copyTranslation() {
        if (busy || reviewingTranslation || isFinishing) return
        val result = outboundTranslationOrExplain() ?: return
        val copied = PrivateClipboardExpiry.copy(
            this, "Tuddy · privévertaling", result
        )
        status = if (copied) {
            "Vertaling naar Android-klembord gekopieerd. Andere apps kunnen dit klembord lezen."
        } else {
            "Kopiëren is mislukt. Er is niets naar het klembord gestuurd."
        }
    }

    private fun bilingualHandoffText(): String? {
        if (busy || detectingSource || reviewingTranslation ||
            isFinishing || inputNotice.isNotBlank()) return null
        val translation = translationResult ?: return null
        val payload = PrivateBilingualHandoffPolicy.format(
            incomingText, translation, sourceCode, targetCode, manuallyAdjustedTranslation
        )
        if (payload == null) {
            status = "Bron of vertaling voldoet niet aan de veilige Unicode-grens. " +
                "Er is niets gekopieerd of gedeeld."
        }
        return payload
    }

    private fun copyBilingualHandoff() {
        val text = bilingualHandoffText() ?: return
        val copied = PrivateClipboardExpiry.copy(
            this, "Tuddy · bron en lokale vertaling", text
        )
        status = if (copied)
            "De gecontroleerde bron + vertaling staan op het Android-klembord. " +
                "Andere apps kunnen beide teksten lezen."
        else "Bron + vertaling kopiëren is mislukt; er is niets op het klembord gezet."
    }

    private fun shareBilingualHandoff() {
        // Only called from the explicit, exact-payload preview. The chooser
        // may open another app, which can store or synchronize BOTH texts.
        val text = bilingualHandoffText() ?: return
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        runCatching {
            startActivity(Intent.createChooser(send, "Deel bron en vertaling met…"))
        }.onFailure {
            status = "Android kon het deelmenu niet openen. Er is niets automatisch gedeeld."
        }
    }

    private fun shareTranslation() {
        if (busy || reviewingTranslation || isFinishing) return
        val result = outboundTranslationOrExplain() ?: return
        // This only runs after a separate in-app confirmation. Android's
        // receiver may store/sync text, so never share silently.
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, result)
        }
        runCatching {
            startActivity(Intent.createChooser(send, "Deel je vertaling met…"))
        }.onFailure {
            status = "Het Android-deelmenu kon niet worden geopend. De tekst is niet automatisch gedeeld."
        }
    }

    private fun returnTranslationToCaller() {
        if (busy || reviewingTranslation || isFinishing || selectionReadOnly) return
        val result = outboundTranslationOrExplain() ?: return
        setResult(RESULT_OK, Intent().putExtra(Intent.EXTRA_PROCESS_TEXT, result))
        closeAndFinish()
    }

    private fun openTuddy() {
        cancelOperation()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun closeAndFinish() {
        cancelOperation()
        incomingText = ""
        translationResult = null
        discardReview()
        finish()
    }

    private fun redactForScreenLock() {
        detectorRevision++
        clearDetectionTimeout()
        detector.close()
        detectingSource = false
        detectionNotice = ""
        cancelOperation()
        incomingText = ""
        translationResult = null
        discardReview()
        inputNotice = "Het Android-scherm is vergrendeld of kon niet veilig " +
            "worden bewaakt. De privéselectie is gewist. Selecteer tekst opnieuw."
        status = "Er is niets automatisch gedeeld, gekopieerd of teruggestuurd."
    }

    /**
     * Android may reclaim memory while a reviewed selected-text translation is
     * active. Cancel locally in the UI, but never close a native ML Kit client
     * whose submitted task is still executing: cancelOperation() already
     * defers that close until the task reports back.
     */
    private fun respondToUrgentMemoryPressure() {
        val hadPendingWork = busy || detectingSource
        val hadPlayback = playbackNotice.isNotBlank()
        detectorRevision++
        clearDetectionTimeout()
        detector.close()
        detectingSource = false
        cancelOperation()
        if (hadPendingWork) {
            translationResult = null
            discardReview()
            status = "Android meldt kritiek geheugentekort. Lokale vertaling gestopt; " +
                "je geselecteerde tekst blijft alleen tijdelijk hier zichtbaar. " +
                "Tik later opnieuw op Vertalen."
        } else if (hadPlayback) {
            playbackNotice = "Voorlezen gestopt vanwege kritiek Android-geheugentekort."
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        respondToUrgentMemoryPressure()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (LocalMemoryPressurePolicy.needsUrgentStop(this, level)) {
            respondToUrgentMemoryPressure()
        } else if (LocalMemoryPressurePolicy.shouldTrimCachedModels(level) && isFinishing) {
            // The normal onPause/onStop path owns any running translation.
            stopPlayback()
        }
    }

    override fun onPause() {
        screenLockGate.disarm()
        // A selected-text screen can remain STARTED but visible in split-screen
        // after another application takes focus. Redact ephemeral private text
        // immediately, not only on the later ON_STOP transition.
        detectorRevision++
        clearDetectionTimeout()
        detector.close()
        detectingSource = false
        detectionNotice = ""
        suggestedSourceCode = null
        cancelOperation() // Also invalidates translation callbacks and stops TTS.
        if (!isFinishing) {
            incomingText = ""
            translationResult = null
            discardReview()
            inputNotice = "Android heeft de focus verplaatst. De privéselectie is gewist; " +
                "selecteer je tekst opnieuw om lokaal te vertalen."
            status = "Er is niets automatisch gedeeld of teruggestuurd."
        }
        super.onPause()
    }

    override fun onStop() {
        detectorRevision++
        clearDetectionTimeout()
        detector.close()
        detectingSource = false
        detectionNotice = ""
        suggestedSourceCode = null
        cancelOperation()
        if (!isFinishing) {
            incomingText = ""
            translationResult = null
            discardReview()
            status = "De selectie is gewist toen Tuddy naar de achtergrond ging. Selecteer de tekst opnieuw."
        }
        super.onStop()
    }

    override fun onDestroy() {
        screenLockGate.disarm()
        detectorRevision++
        clearDetectionTimeout()
        detector.close()
        cancelOperation()
        incomingText = ""
        translationResult = null
        discardReview()
        super.onDestroy()
    }
}

@Composable
private fun QuickTranslateScreen(
    text: String,
    inputNotice: String,
    readOnly: Boolean,
    sourceCode: String,
    targetCode: String,
    busy: Boolean,
    detectingSource: Boolean,
    detectionNotice: String,
    suggestedSourceCode: String?,
    status: String,
    playbackNotice: String,
    headphonesOnly: Boolean,
    respectQuietMode: Boolean,
    clipboardExpirySeconds: Int,
    privateScreenShieldOn: Boolean,
    privateScreenShieldNotice: String,
    result: String?,
    resumePartNumber: Int?,
    resumeTotalParts: Int,
    reviewingTranslation: Boolean,
    reviewDraft: String,
    reviewNotice: String,
    manuallyAdjustedTranslation: Boolean,
    onBeginReview: () -> Unit,
    onReviewDraftChange: (String) -> Unit,
    onSaveReview: () -> Unit,
    onCancelReview: () -> Unit,
    onRestoreMachineTranslation: () -> Unit,
    onSourceSelected: (String) -> Unit,
    onTargetSelected: (String) -> Unit,
    onSwapLanguages: () -> Unit,
    onSuggestSource: () -> Unit,
    onTranslate: () -> Unit,
    onResumeFailedPart: () -> Unit,
    onCancel: () -> Unit,
    onPlayOriginal: (LocalSpeechPace) -> Unit,
    onPlayTranslation: (LocalSpeechPace) -> Unit,
    onPlayBilingual: (LocalSpeechPace) -> Unit,
    onStopPlayback: () -> Unit,
    onHeadphonesOnlyChange: (Boolean) -> Unit,
    onRespectQuietModeChange: (Boolean) -> Unit,
    onClipboardExpiryChange: (Int) -> Unit,
    onPrivateScreenShieldChange: (Boolean) -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onCopyBilingual: () -> Unit,
    onShareBilingual: () -> Unit,
    onReturnResult: () -> Unit,
    onOpenTuddy: () -> Unit,
    onClose: () -> Unit
) {
    var sourcePicker by remember { mutableStateOf(false) }
    var targetPicker by remember { mutableStateOf(false) }
    var localVoicePickerCode by remember { mutableStateOf<String?>(null) }
    var deviceVoiceCatalogOpen by remember { mutableStateOf(false) }
    var confirmExternalShare by remember { mutableStateOf(false) }
    var previewBilingualHandoff by remember { mutableStateOf(false) }
    val bilingualHandoff = if (!busy && !detectingSource && !reviewingTranslation &&
        result != null && inputNotice.isBlank()
    ) PrivateBilingualHandoffPolicy.format(
        text, result, sourceCode, targetCode, manuallyAdjustedTranslation
    ) else null
    val sourceLabel = TargetCatalog.targets.firstOrNull { it.code == sourceCode }?.title ?: sourceCode
    val targetLabel = TargetCatalog.targets.firstOrNull { it.code == targetCode }?.title ?: targetCode

    Column(
        modifier = Modifier.fillMaxSize()
            .statusBarsPadding().navigationBarsPadding()
            .padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Vertaal geselecteerde tekst", style = MaterialTheme.typography.headlineSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                if (privateScreenShieldOn) "Android-privéweergave · aan"
                else "Android-privéweergave · uit",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall
            )
            Switch(
                checked = privateScreenShieldOn,
                onCheckedChange = onPrivateScreenShieldChange
            )
        }
        Text(
            "Ook voor Tuddy's hoofdscherm: Android probeert screenshots, " +
                "schermopname en recente-appsvoorbeelden te blokkeren wanneer dit aanstaat. " +
                "Hiermee kun je zelf ook geen screenshot maken; " +
                "een externe camera blijft mogelijk. Geen automatische opslag.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (privateScreenShieldNotice.isNotBlank()) {
            Text(privateScreenShieldNotice, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            "Alleen lokale ML Kit-tekstvertaling. Maximaal 8.000 tekens, " +
                "langere selecties worden in opeenvolgende gedeelten vertaald. " +
                "De complete vertaling verschijnt pas als alles geslaagd is. " +
                "Controleer zelf de taalrichting. Geen microfoon, automatische download, " +
                "opslag of online fallback.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider()
        Text("Geselecteerde tekst", style = MaterialTheme.typography.titleMedium)
        if (inputNotice.isNotBlank()) {
            Text(inputNotice, color = MaterialTheme.colorScheme.error)
        } else {
            if (text.length > LocalSelectedParagraphPlan.MAX_SOURCE_UNITS) {
                Text(
                    "Deze selectie is langer dan 8.000 tekens. Selecteer een kleiner gedeelte; " +
                        "Tuddy start geen gedeeltelijke vertaling.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = TextDirection.ContentOrRtl
                    )
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { sourcePicker = true },
                enabled = !busy,
                modifier = Modifier.weight(1f)
            ) { Text("Van · ${sourceCode.uppercase()}") }
            OutlinedButton(
                onClick = { targetPicker = true },
                enabled = !busy,
                modifier = Modifier.weight(1f)
            ) { Text("Naar · ${targetCode.uppercase()}") }
        }
        Text(
            "$sourceLabel → $targetLabel",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(
            onClick = onSwapLanguages,
            enabled = !busy && !detectingSource,
            modifier = Modifier.fillMaxWidth()
        ) { Text("⇄ Wissel talen") }
        OutlinedButton(
            onClick = onSuggestSource,
            enabled = text.codePointCount(0, text.length) >= 20 &&
                inputNotice.isBlank() && !busy && !detectingSource,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (detectingSource) "Brontaal lokaal voorstellen…" else "Stel brontaal lokaal voor")
        }
        if (detectionNotice.isNotBlank()) {
            Text(detectionNotice, style = MaterialTheme.typography.bodySmall)
        }
        if (suggestedSourceCode != null && !busy && !detectingSource) {
            OutlinedButton(
                onClick = { onSourceSelected(suggestedSourceCode) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Gebruik voorgestelde brontaal · " +
                    suggestedSourceCode.uppercase(Locale.ROOT))
            }
        }
        if (sourceCode == targetCode) {
            Text("Kies twee verschillende talen.", color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = onTranslate,
            enabled = text.isNotBlank() &&
                text.length <= LocalSelectedParagraphPlan.MAX_SOURCE_UNITS &&
                inputNotice.isBlank() && !busy && !detectingSource &&
                sourceCode != targetCode,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (busy) "Lokaal controleren/vertalen…" else "Vertaal lokaal") }
        if (resumePartNumber != null && resumePartNumber in 2..resumeTotalParts &&
            !busy && !detectingSource && result == null && inputNotice.isBlank()
        ) {
            OutlinedButton(
                onClick = onResumeFailedPart,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Herstel vanaf gedeelte $resumePartNumber van $resumeTotalParts")
            }
            Text(
                "Al vertaalde delen blijven tijdelijk privé in het geheugen. " +
                    "Tuddy controleert de offline modellen opnieuw en deelt pas iets " +
                    "wanneer alle gedeelten voltooid zijn.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (busy || detectingSource) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text("Annuleer lokale verwerking")
            }
        }
        if (status.isNotBlank()) {
            Text(status, style = MaterialTheme.typography.bodySmall)
            if (result == null && !busy &&
                (status.contains("ontbreekt", ignoreCase = true) ||
                    status.contains("niet beschikbaar", ignoreCase = true) ||
                    status.contains("Open Tuddy", ignoreCase = true))
            ) {
                OutlinedButton(onClick = onOpenTuddy, modifier = Modifier.fillMaxWidth()) {
                    Text("Open Tuddy om offline modellen te beheren")
                }
            }
        }
        // No microphone permission, voice download, or network TTS path:
        // the shared installed-local-voice policy remains the single gate.
        LocalCaptionPlaybackMenu(
            hasCompletedTranslation = result != null,
            bilingualAvailable = text.length <= 400 && (result?.length ?: 0) <= 400,
            enabled = text.isNotBlank() && inputNotice.isBlank() &&
                !busy && !detectingSource && !reviewingTranslation,
            headphonesOnly = headphonesOnly,
            onHeadphonesOnlyChange = onHeadphonesOnlyChange,
            respectQuietMode = respectQuietMode,
            onRespectQuietModeChange = onRespectQuietModeChange,
            onPlayOriginal = onPlayOriginal,
            onPlayTranslation = onPlayTranslation,
            onPlayBilingual = onPlayBilingual,
            onStop = onStopPlayback
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                enabled = !busy && !detectingSource,
                onClick = {
                    onStopPlayback()
                    localVoicePickerCode = sourceCode
                }
            ) { Text("Stem bron kiezen") }
            TextButton(
                enabled = !busy && !detectingSource,
                onClick = {
                    onStopPlayback()
                    localVoicePickerCode = targetCode
                }
            ) { Text("Stem vertaling kiezen") }
        }
        TextButton(
            enabled = !busy && !detectingSource,
            onClick = {
                onStopPlayback()
                localVoicePickerCode = null
                deviceVoiceCatalogOpen = true
            }
        ) { Text("Bekijk alle lokale Android-stemmen…") }
        if (playbackNotice.isNotBlank()) {
            Text(playbackNotice, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            "Origineel en vertaling kunnen nu afzonderlijk lokaal worden " +
                "voorgelezen, ook bij lange selecties tot 8.000 tekens, in " +
                "maximaal 100 fragmenten. Stop stopt de rest onmiddellijk. " +
                "Tweetalig achter elkaar afspelen blijft beperkt tot twee " +
                "korte regels van maximaal 400 tekens. " +
                "Alleen geïnstalleerde Android-stemmen, geen online fallback.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (result != null) {
            HorizontalDivider()
            Text(
                if (manuallyAdjustedTranslation)
                    "Handmatig aangepaste vertaling" else "Lokale vertaling",
                style = MaterialTheme.typography.titleMedium
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    result,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = TextDirection.ContentOrRtl
                    )
                )
            }
            if (reviewingTranslation) {
                OutlinedTextField(
                    value = reviewDraft,
                    onValueChange = onReviewDraftChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Je eigen correctie · tijdelijk op dit toestel") },
                    minLines = 4,
                    maxLines = 12,
                    isError = reviewDraft.isNotBlank() &&
                        LocalTranslationReviewPolicy.normalize(reviewDraft) == null,
                    supportingText = {
                        val reason = if (reviewDraft.isNotBlank())
                            LocalTranslationReviewPolicy.validationNotice(reviewDraft) else null
                        Text(reason ?: ("${reviewDraft.length} / 8.000 tekens. " +
                            "De originele ML Kit-vertaling blijft herstelbaar."))
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onSaveReview,
                        enabled = LocalTranslationReviewPolicy.normalize(reviewDraft) != null,
                        modifier = Modifier.weight(1f)
                    ) { Text("Neem correctie over") }
                    OutlinedButton(onClick = onCancelReview, modifier = Modifier.weight(1f)) {
                        Text("Annuleer")
                    }
                }
                Text(
                    "Gewone RTL-schriften blijven toegestaan; alleen onveilige " +
                        "Unicode-opmaak wordt geweigerd. Kopiëren, voorlezen, " +
                        "extern delen en terugplaatsen blijven geblokkeerd " +
                        "totdat je de correctie hebt overgenomen.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                OutlinedButton(onClick = onBeginReview, modifier = Modifier.fillMaxWidth()) {
                    Text("Controleer of pas vertaling handmatig aan")
                }
                if (manuallyAdjustedTranslation) {
                    Text(
                        "Door jou aangepast · geen automatische taal- of kwaliteitscontrole. " +
                            "De tekst is nog nergens extern verstuurd.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(
                        onClick = onRestoreMachineTranslation,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Herstel oorspronkelijke ML Kit-vertaling") }
                }
            }
            if (reviewNotice.isNotBlank()) {
                Text(reviewNotice, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(
                onClick = {
                    val next = when (clipboardExpirySeconds) {
                        0 -> 60
                        60 -> 300
                        else -> 0
                    }
                    onClipboardExpiryChange(next)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Klembordtimer: " + when (clipboardExpirySeconds) {
                    60 -> "1 minuut"
                    300 -> "5 minuten"
                    else -> "uit"
                } + " · wijzig")
            }
            Text(
                "Na kopiëren kunnen andere apps tekst meteen lezen. " +
                    "Automatisch legen is optioneel en werkt alleen zolang Android " +
                    "Tuddy toestaat zijn eigen klembord te controleren.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(
                onClick = onCopy,
                enabled = !reviewingTranslation,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Kopieer vertaling") }
            OutlinedButton(
                onClick = { confirmExternalShare = true },
                enabled = !reviewingTranslation,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Deel vertaling via Android…") }
            if (bilingualHandoff != null) {
                HorizontalDivider()
                Text(
                    "Bron én vertaling samen overdragen? Bekijk eerst de exacte " +
                        "tekst, talen en herkomst. De gewone kopieer- en deelknoppen " +
                        "hierboven bevatten alleen de vertaling.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedButton(
                    onClick = { previewBilingualHandoff = true },
                    enabled = !reviewingTranslation,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Bekijk bron + vertaling vóór kopiëren/delen…") }
            }
            if (!readOnly) {
                Button(
                    onClick = onReturnResult,
                    enabled = !reviewingTranslation,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Vervang geselecteerde tekst in de andere app")
                }
            } else {
                Text(
                    "De bronapp heeft deze selectie als alleen-lezen gemeld. " +
                        "Je kunt de vertaling kopiëren, maar niet automatisch terugplaatsen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider()
        TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Sluiten")
        }
    }

    if (previewBilingualHandoff && bilingualHandoff != null) {
        AlertDialog(
            onDismissRequest = { previewBilingualHandoff = false },
            title = { Text("Bekijk je tweetalige overdracht") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Je ziet hieronder precies welke bron en vertaling je kunt " +
                            "kopiëren of via Android delen. De ontvangende app kan beide " +
                            "teksten bewaren of online verwerken. Er gebeurt niets " +
                            "automatisch. De knop 'Vervang geselecteerde tekst' " +
                            "blijft alleen de vertaling teruggeven.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            bilingualHandoff,
                            modifier = Modifier.padding(12.dp)
                                .verticalScroll(rememberScrollState()),
                            style = MaterialTheme.typography.bodySmall.copy(
                                textDirection = TextDirection.ContentOrRtl
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Column {
                    TextButton(onClick = {
                        previewBilingualHandoff = false
                        onCopyBilingual()
                    }) { Text("Kopieer beide") }
                    TextButton(onClick = {
                        previewBilingualHandoff = false
                        onShareBilingual()
                    }) { Text("Deel beide via Android…") }
                }
            },
            dismissButton = {
                TextButton(onClick = { previewBilingualHandoff = false }) {
                    Text("Niets overdragen")
                }
            }
        )
    }

    if (confirmExternalShare && result != null && !reviewingTranslation) {
        AlertDialog(
            onDismissRequest = { confirmExternalShare = false },
            title = { Text("Vertaling buiten Tuddy delen?") },
            text = {
                Text(
                    "Je deelt alleen de vertaalde tekst met een Android-app die je zelf kiest. " +
                        "Die app kan de tekst opslaan of online verwerken. " +
                        "Tuddy kan die kopie daarna niet verwijderen. " +
                        "Annuleer om de vertaling alleen hier te houden."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmExternalShare = false
                    onShare()
                }) { Text("Open Android-deelmenu") }
            },
            dismissButton = {
                TextButton(onClick = { confirmExternalShare = false }) {
                    Text("Niet delen")
                }
            }
        )
    }
    if (deviceVoiceCatalogOpen) {
        InstalledDeviceVoiceCatalogDialog(
            onDismiss = { deviceVoiceCatalogOpen = false }
        )
    }

    localVoicePickerCode?.let { code ->
        InstalledLocalVoicePickerDialog(
            languageTag = code,
            onDismiss = { localVoicePickerCode = null }
        )
    }

    if (sourcePicker) {
        LanguagePickerDialog(
            title = "Brontaal",
            selectedCode = sourceCode,
            onDismiss = { sourcePicker = false },
            onSelect = { code ->
                sourcePicker = false
                onSourceSelected(code)
            }
        )
    }
    if (targetPicker) {
        LanguagePickerDialog(
            title = "Doeltaal",
            selectedCode = targetCode,
            onDismiss = { targetPicker = false },
            onSelect = { code ->
                targetPicker = false
                onTargetSelected(code)
            }
        )
    }
}

@Composable
private fun LanguagePickerDialog(
    title: String,
    selectedCode: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(80) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Zoek taal of code") },
                    singleLine = true
                )
                LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                    items(TargetCatalog.search(query), key = { it.code }) { target ->
                        TextButton(
                            onClick = { onSelect(target.code) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(target.title)
                                Text(
                                    if (target.code == selectedCode) "${target.code.uppercase()} · geselecteerd"
                                    else target.code.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Sluiten") }
        }
    )
}
