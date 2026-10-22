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
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livelingo.android.SessionTranslationCorrection
import com.livelingo.android.SessionTranslationCorrections
import com.livelingo.android.SessionCorrectionLanguageCatalog

private data class HumanBatchDraft(val original: String, val translation: String)

/**
 * Explicit hand-authoring of a bounded batch with one exact text language pair.
 * All corrections are reviewed, then atomically appended in RAM by the parent.
 * No microphone, translator, clipboard or background storage.
 */
@Composable
internal fun SessionCorrectionManualBatchDialog(
    entries: List<SessionTranslationCorrection>,
    initialTargetCode: String? = null,
    onAddBatch: (List<SessionTranslationCorrection>, List<SessionTranslationCorrection>) -> Boolean,
    onDismiss: () -> Unit
) {
    val snapshot = remember { entries.toList() }
    val drafts = remember { mutableStateListOf(HumanBatchDraft("", "")) }
    val capacity = (SessionTranslationCorrections.MAX_ENTRIES - snapshot.size).coerceAtLeast(0)
    val maxBatch = minOf(10, capacity)
    var sourceCode by remember { mutableStateOf("nl") }
    var targetCode by remember {
        mutableStateOf(initialTargetCode?.takeIf {
            it in SessionCorrectionLanguageCatalog.manualOnlyCodes
        } ?: "en")
    }
    var sourcePickerOpen by remember { mutableStateOf(false) }
    var targetPickerOpen by remember { mutableStateOf(false) }
    var confirmed by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    val labels = SessionCorrectionLanguageCatalog.targets.associate { it.code to it.title }
    val additions = drafts.map {
        SessionTranslationCorrection(it.original, it.translation, sourceCode, targetCode)
    }
    val acceptable = capacity >= drafts.size && drafts.isNotEmpty() &&
        drafts.all { it.original.isNotBlank() && it.translation.isNotBlank() } &&
        sourceCode != targetCode &&
        SessionTranslationCorrections.validEntries(snapshot + additions)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schrijf meerdere eigen correcties") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Schrijf tot 10 eigen zinnen in dezelfde taalrichting. " +
                        "Tuddy maakt geen spraak of machinevertaling aan: dit zijn " +
                        "alleen jouw niet-geverifieerde eigen voorstellen. " +
                        "Niets wordt opgeslagen totdat jij het complete overzicht " +
                        "expliciet bevestigt; de bestaande gespreksregels blijven intact.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    snapshot.size.toString() + " van " +
                        SessionTranslationCorrections.MAX_ENTRIES + " plaatsen in gebruik. " +
                        "Beschikbare ruimte voor deze batch: " + maxBatch + ".",
                    style = MaterialTheme.typography.labelMedium
                )
                if (sourceCode in SessionCorrectionLanguageCatalog.manualOnlyCodes ||
                    targetCode in SessionCorrectionLanguageCatalog.manualOnlyCodes) {
                    Text(
                        "Handmatige eigen zinnen voor een preview-only taal: geen ASR/MT " +
                            "model, geen live vertaling en geen gecontroleerde vertaling. " +
                            "Alle regels blijven voorstellen die jij zelf hebt geschreven.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Box {
                    TextButton(onClick = { sourcePickerOpen = true }) {
                        Text("Brontaal: " + (labels[sourceCode] ?: sourceCode) + " ▾")
                    }
                    DropdownMenu(
                        expanded = sourcePickerOpen,
                        onDismissRequest = { sourcePickerOpen = false }
                    ) {
                        SessionCorrectionLanguageCatalog.targets.forEach { language ->
                            DropdownMenuItem(
                                text = { Text(language.title + " (" + language.code + ")") },
                                onClick = {
                                    sourceCode = language.code
                                    sourcePickerOpen = false
                                    confirmed = false
                                }
                            )
                        }
                    }
                }
                Box {
                    TextButton(onClick = { targetPickerOpen = true }) {
                        Text("Doeltaal: " + (labels[targetCode] ?: targetCode) + " ▾")
                    }
                    DropdownMenu(
                        expanded = targetPickerOpen,
                        onDismissRequest = { targetPickerOpen = false }
                    ) {
                        TargetCatalog.targets.forEach { language ->
                            DropdownMenuItem(
                                text = { Text(language.title + " (" + language.code + ")") },
                                onClick = {
                                    targetCode = language.code
                                    targetPickerOpen = false
                                    confirmed = false
                                }
                            )
                        }
                    }
                }
                drafts.forEachIndexed { index, draft ->
                    Text(
                        "Mijn zin " + (index + 1) + " · " + sourceCode + " → " + targetCode,
                        style = MaterialTheme.typography.labelMedium
                    )
                    OutlinedTextField(
                        value = draft.original,
                        onValueChange = { value ->
                            drafts[index] = draft.copy(original =
                                value.take(SessionTranslationCorrections.MAX_TEXT_UNITS))
                            confirmed = false
                            notice = ""
                        },
                        label = { Text("Eigen originele zin") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    OutlinedTextField(
                        value = draft.translation,
                        onValueChange = { value ->
                            drafts[index] = draft.copy(translation =
                                value.take(SessionTranslationCorrections.MAX_TEXT_UNITS))
                            confirmed = false
                            notice = ""
                        },
                        label = { Text("Mijn eigen vertaling (niet gecontroleerd)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    if (drafts.size > 1) {
                        TextButton(onClick = {
                            drafts.removeAt(index)
                            confirmed = false
                        }) { Text("Haal deze onbewaarde zin weg") }
                    }
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = drafts.size < maxBatch,
                    onClick = {
                        drafts.add(HumanBatchDraft("", ""))
                        confirmed = false
                    }
                ) { Text("＋ Voeg nog een eigen zin toe") }
                if (!acceptable) {
                    Text(
                        "Controleer alle velden en taalrichting. Een dubbele bronzin, " +
                            "ongeldige invoer of te volle lijst wordt volledig geweigerd; " +
                            "er worden nooit oude correcties weggegooid.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (confirmed && acceptable) {
                    Text(
                        "Controle: voeg precies " + additions.size +
                            " eigen correcties samen toe? Er wordt niets automatisch " +
                            "vertaald en alle huidige " + snapshot.size +
                            " correcties blijven behouden.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (onAddBatch(snapshot, additions.toList())) onDismiss()
                            else {
                                confirmed = false
                                notice = "Niet toegevoegd: de sessielijst is veranderd of " +
                                    "de inhoud voldoet niet meer. Niets is overschreven."
                            }
                        }
                    ) { Text("Ja, voeg alle gecontroleerde zinnen tijdelijk toe") }
                } else {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = acceptable,
                        onClick = { confirmed = true }
                    ) { Text("Controleer complete batch vóór toevoegen…") }
                }
                if (notice.isNotBlank()) Text(
                    notice,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Annuleren") } }
    )
}
