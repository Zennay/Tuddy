package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livelingo.android.SessionTranslationCorrection
import com.livelingo.android.SessionTranslationCorrections
import com.livelingo.android.LocalCaptionSpeaker
import com.livelingo.android.PrivateOfflineSearch

/**
 * User-reviewed edits to in-memory corrections only. Previously displayed
 * captions and the language pair remain unchanged; nothing is exported.
 */
@Composable
internal fun SessionTranslationCorrectionsDialog(
    entries: List<SessionTranslationCorrection>,
    speaker: LocalCaptionSpeaker,
    onRemove: (SessionTranslationCorrection) -> Unit,
    onRevise: (SessionTranslationCorrection, String, String) -> Boolean,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    DisposableEffect(speaker) { onDispose { speaker.stop() } }
    var confirmClear by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<SessionTranslationCorrection?>(null) }
    var editedSource by remember { mutableStateOf("") }
    var editedTranslation by remember { mutableStateOf("") }
    var editError by remember { mutableStateOf("") }
    val foldedQuery = PrivateOfflineSearch.fold(query.trim())
    val visible = entries.asReversed().filter { entry ->
        foldedQuery.isBlank() ||
            PrivateOfflineSearch.contains(entry.original, foldedQuery) ||
            PrivateOfflineSearch.contains(entry.translation, foldedQuery) ||
            PrivateOfflineSearch.contains(
                entry.sourceCode + " → " + entry.targetCode, foldedQuery
            )
    }
    // An edit is cancelled when its underlying entry was deleted or reset.
    if (editing != null && editing !in entries) {
        editing = null
        editError = ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mijn tijdelijke vertaalcorrecties") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Maximaal 40 eigen, exacte voorstellen in het tijdelijke geheugen. " +
                        "Bewerk een fout zonder eerdere gespreksregels te wijzigen. " +
                        "Geen voorstel wordt automatisch toegepast of opgeslagen.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(60) },
                    label = { Text("Zoek zin, vertaling of taalrichting") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text(
                    "Zichtbaar: " + visible.size + " van " + entries.size,
                    style = MaterialTheme.typography.bodySmall
                )
                if (entries.isEmpty()) Text("Nog geen correcties onthouden.")
                else if (visible.isEmpty()) Text("Geen passend voorstel gevonden.")

                visible.forEach { entry ->
                    HorizontalDivider()
                    Text(
                        entry.sourceCode + " → " + entry.targetCode,
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (editing == entry) {
                        Text(
                            "Alleen dit exacte voorstel wordt aangepast. " +
                                "De taalrichting blijft gelijk.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = editedSource,
                            onValueChange = {
                                editedSource = it.take(SessionTranslationCorrections.MAX_TEXT_UNITS)
                                editError = ""
                            },
                            label = { Text("Oorspronkelijke zin") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4
                        )
                        OutlinedTextField(
                            value = editedTranslation,
                            onValueChange = {
                                editedTranslation = it.take(SessionTranslationCorrections.MAX_TEXT_UNITS)
                                editError = ""
                            },
                            label = { Text("Mijn verbeterde vertaling") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4
                        )
                        if (editError.isNotEmpty()) {
                            Text(editError, color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val original = editedSource.trim()
                                    val translation = editedTranslation.trim()
                                    if (original.isBlank() || translation.isBlank()) {
                                        editError = "Vul beide zinnen in."
                                    } else if (onRevise(entry, original, translation)) {
                                        editing = null
                                        editError = ""
                                    } else {
                                        editError = "Niet opgeslagen: controleer tekst, " +
                                            "taalrichting of een al bestaande identieke bronzin."
                                    }
                                },
                                enabled = editedSource.isNotBlank() &&
                                    editedTranslation.isNotBlank()
                            ) { Text("Bewaar wijziging") }
                            TextButton(onClick = { editing = null; editError = "" }) {
                                Text("Annuleren")
                            }
                        }
                    } else {
                        Text(entry.original, style = MaterialTheme.typography.bodyMedium)
                        Text(entry.translation, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary)
                        SessionCorrectionPlaybackActions(entry, speaker)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                editing = entry
                                editedSource = entry.original
                                editedTranslation = entry.translation
                                editError = ""
                                confirmClear = false
                            }) { Text("Bewerk") }
                            TextButton(onClick = {
                                speaker.stop()
                                onRemove(entry)
                                if (editing == entry) editing = null
                            }) { Text("Vergeet deze zin") }
                        }
                    }
                }
                if (entries.isNotEmpty()) {
                    if (confirmClear) {
                        Text("Wis alle tijdelijke correcties definitief?",
                            color = MaterialTheme.colorScheme.error)
                        OutlinedButton(onClick = {
                            speaker.stop()
                            onClear()
                            editing = null
                            confirmClear = false
                            editError = ""
                        }) { Text("Ja, wis alles") }
                        TextButton(onClick = { confirmClear = false }) { Text("Annuleren") }
                    } else {
                        OutlinedButton(onClick = {
                            confirmClear = true
                            editing = null
                            editError = ""
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Wis alle onthouden correcties…")
                        }
                    }
                }
                Text(
                    "Alle wijzigingen blijven alleen tijdens deze app-sessie bestaan. " +
                        "Bij 'Wis gesprek' of afsluiten verdwijnen ze. " +
                        "Nieuwe vertalingen worden nooit stilzwijgend overschreven.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Sluiten") } }
    )
}
