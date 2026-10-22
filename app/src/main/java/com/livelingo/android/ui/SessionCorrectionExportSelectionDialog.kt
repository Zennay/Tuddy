package com.livelingo.android.ui

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livelingo.android.SessionTranslationCorrection
import com.livelingo.android.PrivateOfflineSearch

/**
 * Explicitly choose a subset of the user's own correction proposals for one
 * user-created JSON document. The selection exists in this dialog's RAM only.
 */
@Composable
internal fun SessionCorrectionExportSelectionDialog(
    entries: List<SessionTranslationCorrection>,
    onExport: (List<SessionTranslationCorrection>) -> Unit,
    onExportCsv: (List<SessionTranslationCorrection>) -> Unit,
    onExportHtml: (List<SessionTranslationCorrection>, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    // A frozen snapshot prevents a silent change to the export list while open.
    val snapshot = remember { entries.toList() }
    var selected by remember { mutableStateOf(snapshot.indices.toSet()) }
    var pairMenuOpen by remember { mutableStateOf(false) }
    var selectionQuery by remember { mutableStateOf("") }
    var showAnswers by remember { mutableStateOf(false) }
    val languagePairs = snapshot.map { it.sourceCode to it.targetCode }.distinct()
    val chosen = snapshot.filterIndexed { index, _ -> index in selected }
    val folded = PrivateOfflineSearch.fold(selectionQuery.trim())
    val visibleIndexes = snapshot.indices.filter { index ->
        val row = snapshot[index]
        folded.isEmpty() ||
            PrivateOfflineSearch.contains(row.original, folded) ||
            PrivateOfflineSearch.contains(row.translation, folded) ||
            PrivateOfflineSearch.contains(row.sourceCode + " → " + row.targetCode, folded)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eigen correcties voor export kiezen") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 470.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Kies uitsluitend de zinnen die in jouw eigen JSON- of CSV-bestand " +
                        "komen. Het opslaan gebeurt pas na keuze van een Android-" +
                        "bestandslocatie. Die provider kan zelf naar de cloud synchroniseren.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    selected.size.toString() + " van " + snapshot.size +
                        " eigen correcties voor export gekozen.",
                    style = MaterialTheme.typography.labelMedium
                )
                OutlinedTextField(
                    value = selectionQuery,
                    onValueChange = { selectionQuery = it.take(80) },
                    label = { Text("Zoek eigen correcties voor export") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text(
                    "Zichtbaar: " + visibleIndexes.size + " van " + snapshot.size +
                        ". Verborgen aangevinkte correcties blijven geselecteerd.",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = { selected = snapshot.indices.toSet() }) {
                        Text("Kies alles")
                    }
                    TextButton(onClick = { selected = emptySet() }) {
                        Text("Kies niets")
                    }
                    TextButton(
                        enabled = visibleIndexes.isNotEmpty(),
                        onClick = { selected = selected + visibleIndexes }
                    ) { Text("Kies zichtbare") }
                    TextButton(
                        enabled = visibleIndexes.isNotEmpty(),
                        onClick = { selected = selected - visibleIndexes.toSet() }
                    ) { Text("Haal zichtbare weg") }
                }
                if (languagePairs.size > 1) {
                    Box {
                        TextButton(onClick = { pairMenuOpen = true }) {
                            Text("Kies alleen één taalpaar… ▾")
                        }
                        DropdownMenu(
                            expanded = pairMenuOpen,
                            onDismissRequest = { pairMenuOpen = false }
                        ) {
                            languagePairs.forEach { pair ->
                                DropdownMenuItem(
                                    text = { Text(pair.first + " → " + pair.second) },
                                    onClick = {
                                        selected = snapshot.indices.filter { index ->
                                            val row = snapshot[index]
                                            row.sourceCode == pair.first &&
                                                row.targetCode == pair.second
                                        }.toSet()
                                        pairMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
                visibleIndexes.forEach { index ->
                    val entry = snapshot[index]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = index in selected,
                            onCheckedChange = { checked ->
                                selected = if (checked) selected + index else selected - index
                            }
                        )
                        Text(
                            entry.sourceCode + " → " + entry.targetCode +
                                "\n" + entry.original + " → " + entry.translation,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (chosen.isEmpty()) {
                    Text("Kies minstens één zin om een bestand te bewaren.",
                        style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    enabled = chosen.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onExport(chosen.toList()) }
                ) { Text("Bewaar selectie als JSON…") }
                Button(
                    enabled = chosen.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onExportCsv(chosen.toList()) }
                ) { Text("Bewaar selectie als bewerkbare CSV…") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(
                        checked = showAnswers,
                        onCheckedChange = { showAnswers = it }
                    )
                    Text(
                        "Toon mijn eigen vertalingen in het afdrukbare blad",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Button(
                    enabled = chosen.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onExportHtml(chosen.toList(), showAnswers) }
                ) { Text(if (showAnswers) "Bewaar privaat antwoordblad als HTML…"
                    else "Bewaar invulblad zonder vertalingen als HTML…") }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        }
    )
}
