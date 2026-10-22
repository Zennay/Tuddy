package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.livelingo.android.OfflineTextReceipt
import com.livelingo.android.OfflineTextStage
import com.livelingo.android.LocalLanguageDetector
import com.livelingo.android.PrivateSubtitleCue
import com.livelingo.android.PrivateSubtitleRowSelection
import com.livelingo.android.PrivateSubtitleDraftEditor
import com.livelingo.android.PrivateSubtitleTranslationController
import com.livelingo.android.TargetCatalog

/** Explicit, local-only translation of a selected subtitle document draft. */
@Composable
internal fun PrivateSubtitleTranslationDialog(
    cues: List<PrivateSubtitleCue>,
    onApply: (List<PrivateSubtitleCue>) -> Unit,
    onDismiss: () -> Unit
) {
    val controller = remember { PrivateSubtitleTranslationController() }
    val detector = remember { LocalLanguageDetector() }
    val lifecycleOwner = LocalLifecycleOwner.current
    var sourceCode by remember(cues) { mutableStateOf("") }
    var targetCode by remember(cues) { mutableStateOf("") }
    var receipt by remember { mutableStateOf<OfflineTextReceipt?>(null) }
    var checking by remember { mutableStateOf(false) }
    var translating by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var translatedCues by remember { mutableStateOf<List<PrivateSubtitleCue>?>(null) }
    var translatedIndices by remember(cues) { mutableStateOf<List<Int>>(emptyList()) }
    var previewPage by remember(cues) { mutableIntStateOf(0) }
    var cueSelectionExpression by remember(cues) { mutableStateOf("1-${cues.size}") }
    var bilingualOutput by remember(cues) { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var detectingSource by remember { mutableStateOf(false) }
    var detectionMessage by remember { mutableStateOf("") }
    val selectedIndices = remember(cueSelectionExpression, cues.size) {
        PrivateSubtitleRowSelection.parse(cueSelectionExpression, cues.size)
    }
    val validSelectedIndices = selectedIndices.orEmpty()

    fun resetForLanguageChange() {
        controller.cancel()
        detector.close()
        detectingSource = false
        detectionMessage = ""
        receipt = null
        checking = false
        translating = false
        progress = 0
        translatedCues = null
        translatedIndices = emptyList()
        bilingualOutput = false
        message = ""
    }

    fun closeDialog() {
        controller.cancel()
        onDismiss()
    }

    fun checkModels() {
        if (sourceCode.isBlank() || targetCode.isBlank()) {
            message = "Kies eerst een bron- en doeltaal."
            return
        }
        if (sourceCode == targetCode) {
            receipt = OfflineTextReceipt(
                sourceCode, targetCode, OfflineTextStage.UNSUPPORTED,
                "Bron- en doeltaal zijn gelijk. Kies twee verschillende talen."
            )
            return
        }
        checking = true
        translating = false
        translatedCues = null
        translatedIndices = emptyList()
        progress = 0
        receipt = null
        message = ""
        controller.check(sourceCode, targetCode) { status ->
            receipt = status
            checking = status.stage == OfflineTextStage.CHECKING
            if (status.stage != OfflineTextStage.CHECKING) message = ""
        }
    }

    fun detectSourceLanguage() {
        if (detectingSource || checking || translating || translatedCues != null) return
        val sample = cues.asSequence()
            .map { it.text.replace('\n', ' ').trim() }
            .filter { it.isNotBlank() }
            .take(12)
            .joinToString(" ")
            .take(1200)
        if (sample.length < 8) {
            detectionMessage = "Er staat te weinig tekst in dit bestand om de brontaal lokaal te schatten."
            return
        }
        detector.close()
        detectingSource = true
        detectionMessage = "Taalvoorstel lokaal berekenen…"
        detector.identify(sample) { detected ->
            detectingSource = false
            val supported = detected?.takeIf { it in TargetCatalog.codes }
            if (supported == null) {
                detectionMessage = if (detected == null) {
                    "De brontaal kon niet betrouwbaar worden voorgesteld. Kies de taal handmatig."
                } else {
                    "De herkenning gaf '$detected', buiten de ML Kit-vertaalcatalogus. Kies de taal handmatig."
                }
            } else {
                controller.cancel()
                receipt = null
                checking = false
                translating = false
                progress = 0
                translatedCues = null
                translatedIndices = emptyList()
                bilingualOutput = false
                message = ""
                sourceCode = supported
                val label = TargetCatalog.targets.firstOrNull { it.code == supported }?.title ?: supported
                detectionMessage = "Brontaalvoorstel: $label ($supported). Controleer dit zelf; daarna moet je de lokale modellen apart controleren."
            }
        }
    }

    fun startTranslation() {
        val indices = validSelectedIndices
        if (sourceCode.isBlank() || targetCode.isBlank() ||
            sourceCode == targetCode || receipt?.stage != OfflineTextStage.READY ||
            translating || translatedCues != null || indices.isEmpty()
        ) {
            if (indices.isEmpty()) {
                message = "Kies een geldige selectie binnen de tijdgecodeerde regels."
            }
            return
        }
        val selectedCues = indices.map { cues[it] }
        translating = true
        checking = false
        progress = 0
        previewPage = 0
        message = "Lokale modelstatus wordt opnieuw gecontroleerd…"
        controller.translate(
            cues = selectedCues,
            source = sourceCode,
            target = targetCode,
            onProgress = { completed, _ -> progress = completed },
            onComplete = { texts, error ->
                translating = false
                if (texts == null || texts.size != selectedCues.size) {
                    receipt = null
                    translatedIndices = emptyList()
                    message = error ?: "Vertaling niet voltooid. Het concept is ongewijzigd."
                } else {
                    var updated: List<PrivateSubtitleCue>? = selectedCues.toList()
                    texts.forEachIndexed { index, text ->
                        val current = updated
                        if (current != null) {
                            updated = PrivateSubtitleDraftEditor.revise(current, index, text)
                        }
                    }
                    if (updated == null) {
                        receipt = null
                        translatedIndices = emptyList()
                        message = "Een vertaalde regel past niet binnen de veilige ondertitelgrenzen. " +
                            "Het concept is ongewijzigd."
                    } else {
                        translatedIndices = indices
                        translatedCues = updated
                        message = ""
                    }
                }
            }
        )
    }

    DisposableEffect(controller, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_PAUSE) {
                val wasBusy = checking || translating
                controller.cancel()
                detector.close()
                detectingSource = false
                checking = false
                translating = false
                receipt = null
                translatedCues = null
                translatedIndices = emptyList()
                if (wasBusy) {
                    message = "Vertaling gestopt omdat Tuddy naar de achtergrond ging. " +
                        "Het ondertitelconcept is ongewijzigd."
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.cancel()
            detector.close()
        }
    }

    val preview = translatedCues
    val previewPageSize = 5
    val previewPageCount = preview?.let {
        (it.size + previewPageSize - 1) / previewPageSize
    } ?: 0
    val safePreviewPage = previewPage.coerceIn(0, (previewPageCount - 1).coerceAtLeast(0))
    val previewStartIndex = safePreviewPage * previewPageSize
    val previewItems = preview?.drop(previewStartIndex)?.take(previewPageSize).orEmpty()
    val outputCues = if (preview == null || preview.size != translatedIndices.size) {
        null
    } else {
        var updated: List<PrivateSubtitleCue>? = cues.toList()
        preview.forEachIndexed { index, translated ->
            val originalIndex = translatedIndices.getOrNull(index)
            val current = updated
            if (current != null && originalIndex != null && originalIndex in cues.indices) {
                val outputText = if (bilingualOutput) {
                    cues[originalIndex].text.trim() + "\n" + translated.text
                } else {
                    translated.text
                }
                updated = PrivateSubtitleDraftEditor.revise(current, originalIndex, outputText)
            } else {
                updated = null
            }
        }
        updated
    }
    AlertDialog(
        onDismissRequest = { closeDialog() },
        title = { Text("Vertaal ondertitelconcept lokaal") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(
                    "${cues.size} tijdgecodeerde regels · maximaal 500. " +
                        "De originele bronfile wordt niet overschreven.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = cueSelectionExpression,
                    onValueChange = { input ->
                        cueSelectionExpression = input.take(2_000)
                        message = ""
                    },
                    label = { Text("Regels (bijv. 1-5,8,11-12)") },
                    placeholder = { Text("1-${cues.size}") },
                    enabled = !checking && !translating && preview == null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text(
                    if (selectedIndices == null) {
                        "Ongeldige selectie. Gebruik regelnummers van 1 tot ${cues.size}, bijvoorbeeld 1-5,8,11-12. Overlappende of dubbele regels zijn niet toegestaan."
                    } else {
                        "Geselecteerd: ${PrivateSubtitleRowSelection.formatSourceRows(validSelectedIndices.map { it + 1 }, cues.size) ?: "—"} " +
                            "· ${validSelectedIndices.size} cues. Alle andere regels blijven ongewijzigd."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selectedIndices == null) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        enabled = !checking && !translating && preview == null,
                        onClick = {
                            cueSelectionExpression = "1-${cues.size}"
                            message = ""
                        }
                    ) { Text("Hele bestand kiezen") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TargetLanguageMenu(
                        label = "Brontaal",
                        selectedCode = sourceCode,
                        enabled = !checking && !translating && preview == null,
                        modifier = Modifier.weight(1f),
                        onSelected = {
                            resetForLanguageChange()
                            sourceCode = it
                        }
                    )
                    TargetLanguageMenu(
                        label = "Doeltaal",
                        selectedCode = targetCode,
                        enabled = !checking && !translating && preview == null,
                        modifier = Modifier.weight(1f),
                        onSelected = {
                            resetForLanguageChange()
                            targetCode = it
                        }
                    )
                }
                OutlinedButton(
                    enabled = !detectingSource && !checking && !translating && preview == null,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { detectSourceLanguage() }
                ) {
                    Text(if (detectingSource) "Brontaal lokaal herkennen…"
                        else "Stel brontaal voor op basis van dit bestand")
                }
                Text(
                    "De suggestie gebruikt maximaal de eerste 12 niet-lege regels (tot 1.200 tekens), " +
                        "lokaal op dit toestel. Het is een voorstel, geen automatische taalkeuze.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (detectionMessage.isNotBlank()) {
                    Text(
                        detectionMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (detectionMessage.startsWith("Brontaalvoorstel:"))
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "Tuddy gebruikt uitsluitend reeds geïnstalleerde ML Kit-tekstmodellen. " +
                        "Er wordt niets gedownload, geen microfoon gestart en geen tekst naar een clouddienst gestuurd. " +
                        "Kurmanji, Sorani en Pasjtoe zijn geen ML Kit-doelen.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedButton(
                    enabled = sourceCode.isNotBlank() && targetCode.isNotBlank() &&
                        sourceCode != targetCode && !checking && !translating && preview == null,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { checkModels() }
                ) { Text(if (checking) "Lokale modellen controleren…" else "Controleer lokale modellen") }

                val status = receipt
                if (status != null) {
                    Text(
                        "Modelstatus: ${status.stage.name.lowercase().replace('_', ' ')}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (status.stage == OfflineTextStage.READY)
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(status.detail, style = MaterialTheme.typography.bodySmall)
                    if (status.stage == OfflineTextStage.MISSING) {
                        Text(
                            "Sluit deze dialoog en bereid het ontbrekende paar bewust voor via " +
                                "Offline startcheck → Tekstmodel offline voorbereiden → wifi. " +
                                "Open daarna het ondertitelbestand opnieuw.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (translating) {
                    Text(
                        "Lokaal vertalen: $progress / ${validSelectedIndices.size} geselecteerde regels. " +
                            "Er wordt pas iets toegepast als alle geselecteerde regels slagen.",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                if (preview != null) {
                    Text(
                        "Controleer de vertaling voordat je de tijdelijke tekst vervangt. " +
                            "Voorbeeld ${previewStartIndex + 1}–" +
                            "${previewStartIndex + previewItems.size} van ${preview.size} vertaalde regels " +
                            "uit bereik ${translatedIndices.firstOrNull()?.plus(1) ?: "?"}–" +
                            "${translatedIndices.lastOrNull()?.plus(1) ?: "?"}. " +
                            "Andere regels blijven ongewijzigd.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            modifier = Modifier.weight(1f),
                            enabled = safePreviewPage > 0,
                            onClick = { previewPage = safePreviewPage - 1 }
                        ) { Text("Vorige voorbeeldregels") }
                        TextButton(
                            modifier = Modifier.weight(1f),
                            enabled = safePreviewPage + 1 < previewPageCount,
                            onClick = { previewPage = safePreviewPage + 1 }
                        ) { Text("Volgende voorbeeldregels") }
                    }
                    TextButton(
                        onClick = {
                            translatedCues = null
                            translatedIndices = emptyList()
                            previewPage = 0
                            progress = 0
                            bilingualOutput = false
                            message = ""
                        }
                    ) { Text("Ander bereik kiezen") }
                    Text(
                        "Kies hoe de nieuwe tekst in iedere tijdgecodeerde regel wordt opgebouwd:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { bilingualOutput = false }) {
                            Text((if (!bilingualOutput) "✓ " else "") + "Alleen vertaling")
                        }
                        TextButton(onClick = { bilingualOutput = true }) {
                            Text((if (bilingualOutput) "✓ " else "") + "Origineel + vertaling")
                        }
                    }
                    Text(
                        if (bilingualOutput)
                            "Tweetalig: origineel boven, vertaling eronder. Beide blijven in dezelfde cue met dezelfde tijdcodes."
                        else
                            "Alleen de vertaalde tekst wordt toegepast; het oorspronkelijke bestand blijft onaangeraakt.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (bilingualOutput && outputCues == null) {
                        Text(
                            "Tweetalige uitvoer overschrijdt bij minstens één regel de bestaande tekst- of regellimieten. " +
                                "Kies Alleen vertaling; er wordt niets toegepast.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    previewItems.forEachIndexed { pageOffset, cue ->
                        val previewIndex = previewStartIndex + pageOffset
                        val originalIndex = translatedIndices[previewIndex]
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Regel ${originalIndex + 1} · origineel",
                                style = MaterialTheme.typography.labelSmall)
                            Text(
                                cues[originalIndex].text.take(260),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textDirection = if (isRtl(sourceCode)) TextDirection.Rtl
                                    else TextDirection.Ltr
                                )
                            )
                            Text("Vertaling",
                                style = MaterialTheme.typography.labelSmall)
                            Text(
                                cue.text.take(260),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textDirection = if (isRtl(targetCode)) TextDirection.Rtl
                                    else TextDirection.Ltr
                                )
                            )
                        }
                    }
                }
                if (message.isNotBlank()) {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            if (preview != null) {
                TextButton(
                    enabled = outputCues != null,
                    onClick = {
                        val ready = outputCues
                        if (ready != null) {
                            onApply(ready)
                            closeDialog()
                        }
                    }
                ) {
                    Text(if (bilingualOutput) "Vervang door tweetalige cues"
                        else "Vervang tijdelijke tekst")
                }
            } else {
                TextButton(
                    enabled = receipt?.stage == OfflineTextStage.READY &&
                        sourceCode.isNotBlank() && targetCode.isNotBlank() &&
                        sourceCode != targetCode && validSelectedIndices.isNotEmpty() &&
                        !detectingSource && !checking && !translating,
                    onClick = { startTranslation() }
                ) {
                    Text(when {
                        translating -> "Vertalen…"
                        selectedIndices == null -> "Kies geldige selectie"
                        else -> "Vertaal ${validSelectedIndices.size} regels"
                    })
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (translating || checking) {
                    controller.cancel()
                    translating = false
                    checking = false
                    receipt = null
                    translatedCues = null
                    translatedIndices = emptyList()
                    message = "Geannuleerd. Het tijdelijke ondertitelconcept is ongewijzigd."
                } else {
                    closeDialog()
                }
            }) { Text(if (translating || checking) "Annuleer" else "Sluiten") }
        }
    )
}

@Composable
private fun TargetLanguageMenu(
    label: String,
    selectedCode: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = TargetCatalog.targets.firstOrNull { it.code == selectedCode }
    Box(modifier = modifier) {
        OutlinedButton(
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            onClick = { expanded = true }
        ) {
            Text(selected?.let { "$label: ${it.title}" } ?: "Kies $label")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TargetCatalog.targets.forEach { target ->
                DropdownMenuItem(
                    text = { Text("${target.title} (${target.code})") },
                    onClick = {
                        expanded = false
                        onSelected(target.code)
                    }
                )
            }
        }
    }
}

private fun isRtl(code: String): Boolean = code in setOf("ar", "fa", "he", "ur")
