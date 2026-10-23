package com.livelingo.android

import android.content.Intent
import android.os.Bundle
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.Locale

/**
 * Android's selected-text action. Translation is explicit, local-only and
 * returns to the originating editor only after a separate user tap.
 */
internal class QuickTranslateActivity : ComponentActivity() {
    private val inventory = OfflineTextModelInventory()
    private val detector = LocalLanguageDetector()
    private var generation = 0L
    private var activeTranslator: Translator? = null

    private var incomingText by mutableStateOf("")
    private var inputNotice by mutableStateOf("")
    private var selectionReadOnly by mutableStateOf(true)
    private var sourceCode by mutableStateOf("nl")
    private var targetCode by mutableStateOf("en")
    private var busy by mutableStateOf(false)
    private var detectingSource by mutableStateOf(false)
    private var detectionNotice by mutableStateOf("")
    private var status by mutableStateOf("")
    private var translationResult by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        sourceCode = defaults.first
        targetCode = defaults.second
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
                result = translationResult,
                onSourceSelected = { code -> changePair(source = code) },
                onTargetSelected = { code -> changePair(target = code) },
                onSwapLanguages = { changePair(source = targetCode, target = sourceCode) },
                onSuggestSource = { suggestSourceLanguage() },
                onTranslate = { startTranslation() },
                onCopy = { status = "Vertaling gekopieerd. Tuddy bewaart geen kopie." },
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
        val sample = incomingText.replace('\n', ' ').trim().take(1200)
        if (sample.length < 8) {
            detectionNotice = "Er staat te weinig tekst in deze selectie om lokaal een brontaal voor te stellen."
            return
        }
        detector.close()
        detectingSource = true
        detectionNotice = "Brontaalvoorstel lokaal berekenen…"
        detector.identify(sample) { detected ->
            if (isFinishing) return@identify
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
        closeActiveTranslator()
        busy = false
        translationResult = result
        status = message
        generation++
    }

    private fun cancelOperation() {
        generation++
        inventory.invalidate()
        closeActiveTranslator()
        busy = false
    }

    private fun closeActiveTranslator() {
        val current = activeTranslator
        activeTranslator = null
        runCatching { current?.close() }
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

    override fun onStop() {
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

    override fun onDestroy() {
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
    result: String?,
    onSourceSelected: (String) -> Unit,
    onTargetSelected: (String) -> Unit,
    onSwapLanguages: () -> Unit,
    onSuggestSource: () -> Unit,
    onTranslate: () -> Unit,
    onCopy: () -> Unit,
    onReturnResult: () -> Unit,
    onOpenTuddy: () -> Unit,
    onClose: () -> Unit
) {
    var sourcePicker by remember { mutableStateOf(false) }
    var targetPicker by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
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
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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
            OutlinedButton(
                onClick = {
                    clipboard.setText(AnnotatedString(result))
                    onCopy()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Kopieer vertaling") }
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
