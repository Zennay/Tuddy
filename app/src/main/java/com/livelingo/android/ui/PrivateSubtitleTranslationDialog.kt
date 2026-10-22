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
import com.livelingo.android.PrivateSubtitleCue
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
    val lifecycleOwner = LocalLifecycleOwner.current
    var sourceCode by remember(cues) { mutableStateOf("") }
    var targetCode by remember(cues) { mutableStateOf("") }
    var receipt by remember { mutableStateOf<OfflineTextReceipt?>(null) }
    var checking by remember { mutableStateOf(false) }
    var translating by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var translatedCues by remember { mutableStateOf<List<PrivateSubtitleCue>?>(null) }
    var message by remember { mutableStateOf("") }

    fun resetForLanguageChange() {
        controller.cancel()
        receipt = null
        checking = false
        translating = false
        progress = 0
        translatedCues = null
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
        progress = 0
        receipt = null
        message = ""
        controller.check(sourceCode, targetCode) { status ->
            receipt = status
            checking = status.stage == OfflineTextStage.CHECKING
            if (status.stage != OfflineTextStage.CHECKING) message = ""
        }
    }

    fun startTranslation() {
        if (sourceCode.isBlank() || targetCode.isBlank() ||
            sourceCode == targetCode || receipt?.stage != OfflineTextStage.READY ||
            translating || translatedCues != null
        ) return
        translating = true
        checking = false
        progress = 0
        message = "Lokale modelstatus wordt opnieuw gecontroleerd…"
        controller.translate(
            cues = cues,
            source = sourceCode,
            target = targetCode,
            onProgress = { completed, _ -> progress = completed },
            onComplete = { texts, error ->
                translating = false
                if (texts == null || texts.size != cues.size) {
                    receipt = null
                    message = error ?: "Vertaling niet voltooid. Het concept is ongewijzigd."
                } else {
                    var updated: List<PrivateSubtitleCue>? = cues.toList()
                    texts.forEachIndexed { index, text ->
                        val current = updated
                        if (current != null) {
                            updated = PrivateSubtitleDraftEditor.revise(current, index, text)
                        }
                    }
                    if (updated == null) {
                        receipt = null
                        message = "Een vertaalde regel past niet binnen de veilige ondertitelgrenzen. " +
                            "Het concept is ongewijzigd."
                    } else {
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
                checking = false
                translating = false
                receipt = null
                translatedCues = null
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
        }
    }

    val preview = translatedCues
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
                        "Lokaal vertalen: $progress / ${cues.size} regels. " +
                            "Er wordt pas iets toegepast als alle regels slagen.",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                if (preview != null) {
                    Text(
                        "Controleer de vertaling voordat je de tijdelijke tekst vervangt. " +
                            "Hieronder staan de eerste ${minOf(5, preview.size)} regels; " +
                            "na toepassen kun je het volledige concept doorzoeken en ongedaan maken.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    preview.take(5).forEachIndexed { index, cue ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Regel ${index + 1} · origineel",
                                style = MaterialTheme.typography.labelSmall)
                            Text(
                                cues[index].text.take(260),
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
                TextButton(onClick = {
                    onApply(preview)
                    closeDialog()
                }) { Text("Vervang tijdelijke tekst") }
            } else {
                TextButton(
                    enabled = receipt?.stage == OfflineTextStage.READY &&
                        sourceCode.isNotBlank() && targetCode.isNotBlank() &&
                        sourceCode != targetCode && !checking && !translating,
                    onClick = { startTranslation() }
                ) { Text(if (translating) "Vertalen…" else "Vertaal ${cues.size} regels") }
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
