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
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.Locale
import com.livelingo.android.ui.LocalCaptionPlaybackMenu
import com.livelingo.android.ui.InstalledLocalVoicePickerDialog

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
    private val detector = LocalLanguageDetector()
    private var generation = 0L
    private var detectorRevision = 0L
    private var localSpeaker: LocalCaptionSpeaker? = null
    private var activeTranslator: Translator? = null
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
    private var status by mutableStateOf("")
    private var playbackNotice by mutableStateOf("")
    private var headphonesOnlyPlayback by mutableStateOf(false)
    private var clipboardExpirySeconds by mutableStateOf(0)
    private var translationResult by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        headphonesOnlyPlayback = PrivatePlaybackRoutingPreference.headphonesOnly(this)
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
                    status = status,
                    playbackNotice = playbackNotice,
                    headphonesOnly = headphonesOnlyPlayback,
                    clipboardExpirySeconds = clipboardExpirySeconds,
                    result = translationResult,
                    onSourceSelected = { code -> changePair(source = code) },
                    onTargetSelected = { code -> changePair(target = code) },
                    onSwapLanguages = { changePair(source = targetCode, target = sourceCode) },
                    onSuggestSource = { suggestSourceLanguage() },
                    onTranslate = { startTranslation() },
                    onCancel = { cancelByUser() },
                    onPlayOriginal = { pace -> speakSelection(incomingText, sourceCode, pace) },
                    onPlayTranslation = { pace ->
                        translationResult?.let { speakSelection(it, targetCode, pace) }
                    },
                    onPlayBilingual = { pace ->
                        speakBilingualSelection(pace)
                    },
                    onStopPlayback = { stopPlayback() },
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
                    onReturnResult = { returnTranslationToCaller() },
                    onOpenTuddy = { openTuddy() },
                    onClose = { closeAndFinish() }
                )
            }
        }
    }

    private fun defaultLanguagePair(): Pair<String, String> {
        val deviceLanguage = Locale.getDefault().language.lowercase(Locale.ROOT)
            .takeIf { it in TargetCatalog.codes } ?: "nl"
        val source = deviceLanguage
        val target = if (source == "en") "nl" else "en"
        return source to target
    }

    private fun changePair(source: String? = null, target: String? = null) {
        detectorRevision++
        clearDetectionTimeout()
        detector.close()
        detectingSource = false
        detectionNotice = ""
        cancelOperation()
        sourceCode = source ?: sourceCode
        targetCode = target ?: targetCode
        translationResult = null
        status = ""
    }

    private fun suggestSourceLanguage() {
        if (busy || detectingSource || inputNotice.isNotBlank()) return
        stopPlayback()
        val sample = incomingText.replace('\n', ' ').trim().take(1200)
        if (sample.length < 8) {
            detectionNotice = "Er staat te weinig tekst in deze selectie om lokaal een brontaal voor te stellen."
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
            val supported = detected?.takeIf { it in TargetCatalog.codes }
            if (supported == null) {
                detectionNotice = if (detected == null) {
                    "Geen bruikbaar lokaal brontaalvoorstel. Kies de taal handmatig."
                } else {
                    "Het lokale voorstel '$detected' valt buiten de tekstvertaalcatalogus. Kies handmatig."
                }
            } else {
                changePair(source = supported)
                val label = TargetCatalog.targets.firstOrNull { it.code == supported }?.title ?: supported
                detectionNotice = "Brontaalvoorstel: $label ($supported). Controleer zelf; er is nog niets vertaald."
            }
            detector.close()
        }
    }

    private fun startTranslation() {
        if (busy || detectingSource || inputNotice.isNotBlank() || incomingText.isBlank()) return
        if (sourceCode !in TargetCatalog.codes || targetCode !in TargetCatalog.codes) {
            status = "Kies een ondersteunde bron- en doeltaal."
            return
        }
        if (sourceCode == targetCode) {
            status = "Kies twee verschillende talen."
            return
        }

        cancelOperation()
        val token = generation
        busy = true
        translationResult = null
        status = "Controleer of de gekozen tekstmodellen lokaal aanwezig zijn…"
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
            try {
                client.translate(incomingText)
                    .addOnSuccessListener { raw ->
                        if (token != generation || isFinishing) return@addOnSuccessListener
                        val normalized = normalizeTranslation(raw)
                        if (normalized == null) {
                            complete(token, null,
                                "Het lokale resultaat was leeg of overschreed de veilige tekstgrenzen.")
                        } else {
                            complete(token, normalized,
                                "Lokale vertaling klaar. Er is nog niets naar de andere app teruggestuurd.")
                        }
                    }
                    .addOnFailureListener {
                        if (token == generation) complete(token, null,
                            "Lokale vertaling mislukt. Geen online fallback en geen resultaat teruggestuurd.")
                    }
            } catch (_: Exception) {
                complete(token, null,
                    "De lokale vertaling kon niet starten. Er is niets teruggestuurd.")
            }
        }
    }

    private fun normalizeTranslation(raw: String): String? {
        val clean = raw.replace("\r\n", "\n").replace('\r', '\n').trim()
        if (clean.isBlank() || clean.length > 8_000 ||
            clean.any { ch -> ch == '\u0000' ||
                (ch.isISOControl() && ch != '\n' && ch != '\t') }
        ) return null
        return clean
    }

    private fun complete(token: Long, result: String?, message: String) {
        if (token != generation || isFinishing) return
        clearTranslationTimeout()
        closeActiveTranslator()
        busy = false
        translationResult = result
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
        detectionNotice = ""
        status = "Actie geannuleerd. Er is niets teruggestuurd, gedeeld of opgeslagen."
    }

    private fun closeActiveTranslator() {
        val current = activeTranslator
        activeTranslator = null
        runCatching { current?.close() }
    }

    private fun speakSelection(text: String, language: String, pace: LocalSpeechPace) {
        if (busy || detectingSource || inputNotice.isNotBlank()) return
        stopPlayback()
        // LocalCaptionSpeaker admits only installed, non-network Android voices.
        // It also rejects overly long/control-filled text without audio fallback.
        val speaker = localSpeaker ?: LocalCaptionSpeaker(this).also { localSpeaker = it }
        speaker.speak(text, language, pace) { notice ->
            if (!isFinishing) playbackNotice = notice
        }
    }

    private fun speakBilingualSelection(pace: LocalSpeechPace) {
        val translated = translationResult ?: return
        if (busy || detectingSource || inputNotice.isNotBlank() ||
            isFinishing || incomingText.isBlank()) return
        stopPlayback()
        val speaker = localSpeaker ?: LocalCaptionSpeaker(this).also { localSpeaker = it }
        speaker.speakBilingualPair(
            incomingText, translated, sourceCode, targetCode, pace
        ) { notice ->
            if (!isFinishing) playbackNotice = notice
        }
    }

    private fun stopPlayback() {
        localSpeaker?.stop()
        playbackNotice = ""
    }

    private fun copyTranslation() {
        val result = translationResult ?: return
        val copied = PrivateClipboardExpiry.copy(
            this, "Tuddy · privévertaling", result
        )
        status = if (copied) {
            "Vertaling naar Android-klembord gekopieerd. Andere apps kunnen dit klembord lezen."
        } else {
            "Kopiëren is mislukt. Er is niets naar het klembord gestuurd."
        }
    }

    private fun shareTranslation() {
        val result = translationResult ?: return
        if (busy || isFinishing) return
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
        val result = translationResult ?: return
        if (selectionReadOnly) return
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
        finish()
    }

    override fun onPause() {
        // A selected-text screen can remain STARTED but visible in split-screen
        // after another application takes focus. Redact ephemeral private text
        // immediately, not only on the later ON_STOP transition.
        detectorRevision++
        clearDetectionTimeout()
        detector.close()
        detectingSource = false
        detectionNotice = ""
        cancelOperation() // Also invalidates translation callbacks and stops TTS.
        if (!isFinishing) {
            incomingText = ""
            translationResult = null
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
        cancelOperation()
        if (!isFinishing) {
            incomingText = ""
            translationResult = null
            status = "De selectie is gewist toen Tuddy naar de achtergrond ging. Selecteer de tekst opnieuw."
        }
        super.onStop()
    }

    private companion object {
        const val TRANSLATION_DEADLINE_MS = 30_000L
        const val DETECTION_DEADLINE_MS = 12_000L
    }

    override fun onDestroy() {
        detectorRevision++
        clearDetectionTimeout()
        detector.close()
        cancelOperation()
        incomingText = ""
        translationResult = null
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
    status: String,
    playbackNotice: String,
    headphonesOnly: Boolean,
    clipboardExpirySeconds: Int,
    result: String?,
    onSourceSelected: (String) -> Unit,
    onTargetSelected: (String) -> Unit,
    onSwapLanguages: () -> Unit,
    onSuggestSource: () -> Unit,
    onTranslate: () -> Unit,
    onCancel: () -> Unit,
    onPlayOriginal: (LocalSpeechPace) -> Unit,
    onPlayTranslation: (LocalSpeechPace) -> Unit,
    onPlayBilingual: (LocalSpeechPace) -> Unit,
    onStopPlayback: () -> Unit,
    onHeadphonesOnlyChange: (Boolean) -> Unit,
    onClipboardExpiryChange: (Int) -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onReturnResult: () -> Unit,
    onOpenTuddy: () -> Unit,
    onClose: () -> Unit
) {
    var sourcePicker by remember { mutableStateOf(false) }
    var targetPicker by remember { mutableStateOf(false) }
    var localVoicePickerCode by remember { mutableStateOf<String?>(null) }
    var confirmExternalShare by remember { mutableStateOf(false) }
    val sourceLabel = TargetCatalog.targets.firstOrNull { it.code == sourceCode }?.title ?: sourceCode
    val targetLabel = TargetCatalog.targets.firstOrNull { it.code == targetCode }?.title ?: targetCode

    Column(
        modifier = Modifier.fillMaxSize()
            .statusBarsPadding().navigationBarsPadding()
            .padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Vertaal geselecteerde tekst", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Alleen lokale ML Kit-tekstvertaling. Controleer zelf de taalrichting. " +
                "Geen microfoon, automatische download, opslag of online fallback.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider()
        Text("Geselecteerde tekst", style = MaterialTheme.typography.titleMedium)
        if (inputNotice.isNotBlank()) {
            Text(inputNotice, color = MaterialTheme.colorScheme.error)
        } else {
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
            enabled = text.length >= 8 && inputNotice.isBlank() && !busy && !detectingSource,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (detectingSource) "Brontaal lokaal voorstellen…" else "Stel brontaal lokaal voor")
        }
        if (detectionNotice.isNotBlank()) {
            Text(detectionNotice, style = MaterialTheme.typography.bodySmall)
        }
        if (sourceCode == targetCode) {
            Text("Kies twee verschillende talen.", color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = onTranslate,
            enabled = text.isNotBlank() && inputNotice.isBlank() && !busy && !detectingSource &&
                sourceCode != targetCode,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (busy) "Lokaal controleren/vertalen…" else "Vertaal lokaal") }
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
            enabled = text.isNotBlank() && inputNotice.isBlank() && !busy && !detectingSource,
            headphonesOnly = headphonesOnly,
            onHeadphonesOnlyChange = onHeadphonesOnlyChange,
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
        if (playbackNotice.isNotBlank()) {
            Text(playbackNotice, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            "Voorlezen werkt alleen voor korte tekst (maximaal 400 tekens) " +
                "met een geïnstalleerde, als offline gemelde Android-stem. " +
                "Geen stemdownload of online fallback.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (result != null) {
            HorizontalDivider()
            Text("Lokale vertaling", style = MaterialTheme.typography.titleMedium)
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
                modifier = Modifier.fillMaxWidth()
            ) { Text("Kopieer vertaling") }
            OutlinedButton(
                onClick = { confirmExternalShare = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Deel vertaling via Android…") }
            if (!readOnly) {
                Button(onClick = onReturnResult, modifier = Modifier.fillMaxWidth()) {
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

    if (confirmExternalShare && result != null) {
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
