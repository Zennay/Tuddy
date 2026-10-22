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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livelingo.android.SessionTranslationCorrection
import com.livelingo.android.SessionTranslationCorrections
import com.livelingo.android.SessionCorrectionLanguageCatalog
import com.livelingo.android.LocalCaptionSpeaker
import com.livelingo.android.PrivateOfflineSearch

/**
 * User-reviewed edits to in-memory corrections only. Previously displayed
 * captions and the language pair remain unchanged. Any document transfer is
 * initiated and reviewed by the human, never automatically persisted.
 */
@Composable
internal fun SessionTranslationCorrectionsDialog(
    entries: List<SessionTranslationCorrection>,
    initialTargetCode: String? = null,
    speaker: LocalCaptionSpeaker,
    onRemove: (SessionTranslationCorrection) -> Unit,
    onRevise: (SessionTranslationCorrection, String, String) -> Boolean,
    onReplace: (List<SessionTranslationCorrection>) -> Boolean,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
    incomingTransferText: String? = null,
    onIncomingTransferTextConsumed: (String) -> Unit = {}
) {
    DisposableEffect(speaker) { onDispose { speaker.stop() } }
    var practiceOpen by remember { mutableStateOf(false) }
    var transferOpen by remember { mutableStateOf(false) }
    var manualAddOpen by remember { mutableStateOf(false) }
    var manualBatchOpen by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var batchMode by remember { mutableStateOf(false) }
    var chosenCorrections by remember { mutableStateOf(emptySet<SessionTranslationCorrection>()) }
    var batchSnapshot by remember { mutableStateOf<List<SessionTranslationCorrection>?>(null) }
    var confirmBatch by remember { mutableStateOf(false) }
    var bulkPairMenuOpen by remember { mutableStateOf(false) }
    var batchNotice by remember { mutableStateOf("") }
    var batchUndoBefore by remember {
        mutableStateOf<List<SessionTranslationCorrection>?>(null)
    }
    var batchUndoAfter by remember {
        mutableStateOf<List<SessionTranslationCorrection>?>(null)
    }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<SessionTranslationCorrection?>(null) }
    var editedSource by remember { mutableStateOf("") }
    var editedTranslation by remember { mutableStateOf("") }
    var editError by remember { mutableStateOf("") }
    val foldedQuery = PrivateOfflineSearch.fold(query.trim())
    LaunchedEffect(incomingTransferText) {
        if (incomingTransferText != null) {
            speaker.stop()
            practiceOpen = false
            manualAddOpen = false
            manualBatchOpen = false
            confirmClear = false
            batchMode = false
            batchSnapshot = null
            chosenCorrections = emptySet()
            confirmBatch = false
            editing = null
            transferOpen = true
        }
    }
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
                if (initialTargetCode != null &&
                    initialTargetCode in SessionCorrectionLanguageCatalog.manualOnlyCodes) {
                    Text(
                        "Je opende dit vanuit de preview-only taal " + initialTargetCode +
                            ". Je kunt hier eigen zinnen schrijven of importeren, maar " +
                            "dit maakt geen spraakherkenning, machinevertaling of live " +
                            "ondersteuning voor die taal beschikbaar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
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
                if (entries.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { speaker.stop(); practiceOpen = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Oefen mijn correcties · tijdelijk") }
                }
                OutlinedButton(
                    onClick = {
                        speaker.stop()
                        editing = null
                        confirmClear = false
                        manualAddOpen = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Schrijf eigen correctie zonder microfoon…") }
                OutlinedButton(
                    onClick = {
                        speaker.stop()
                        editing = null
                        confirmClear = false
                        manualBatchOpen = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Schrijf meerdere eigen correcties tegelijk…") }
                OutlinedButton(
                    onClick = {
                        speaker.stop()
                        transferOpen = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Eigen correcties bewust importeren / exporteren…") }
                if (entries.isNotEmpty() && !batchMode) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            speaker.stop()
                            editing = null
                            confirmClear = false
                            chosenCorrections = emptySet()
                            batchSnapshot = entries.toList()
                            confirmBatch = false
                            batchMode = true
                        }
                    ) { Text("Kies meerdere correcties om te vergeten…") }
                }
                if (batchMode) {
                    Text(
                        chosenCorrections.size.toString() + " van " + entries.size +
                            " correcties gekozen om te vergeten. Selecteer alleen " +
                            "zichtbare zoekresultaten, of annuleer. Er wordt niets " +
                            "gewijzigd zonder bevestiging.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            chosenCorrections = chosenCorrections + visible
                            confirmBatch = false
                        }) { Text("Kies zichtbare") }
                        TextButton(onClick = {
                            chosenCorrections = emptySet()
                            confirmBatch = false
                        }) { Text("Kies niets") }
                    }
                    val exactPairs = entries.map {
                        it.sourceCode to it.targetCode
                    }.distinct()
                    if (exactPairs.size > 1) {
                        Box {
                            TextButton(
                                enabled = batchSnapshot == entries,
                                onClick = { bulkPairMenuOpen = true }
                            ) { Text("Voeg één exact taalpaar toe… ▾") }
                            DropdownMenu(
                                expanded = bulkPairMenuOpen,
                                onDismissRequest = { bulkPairMenuOpen = false }
                            ) {
                                exactPairs.forEach { pair ->
                                    DropdownMenuItem(
                                        text = { Text(pair.first + " → " + pair.second) },
                                        onClick = {
                                            chosenCorrections = chosenCorrections +
                                                entries.filter { item ->
                                                    item.sourceCode == pair.first &&
                                                        item.targetCode == pair.second
                                                }
                                            confirmBatch = false
                                            bulkPairMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    if (batchSnapshot != entries) {
                        Text(
                            "De correctielijst is veranderd. Annuleer en selecteer " +
                                "opnieuw om de nieuwste regels te zien.",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (confirmBatch && chosenCorrections.isNotEmpty()) {
                        Text(
                            "Vergeet precies " + chosenCorrections.size +
                                " gekozen correcties? De overige blijven behouden.",
                            color = MaterialTheme.colorScheme.error
                        )
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (entries != batchSnapshot ||
                                    !entries.containsAll(chosenCorrections)) {
                                    batchNotice = "Selectie verouderd; er is niets gewijzigd."
                                } else {
                                    val before = entries.toList()
                                    val after = before.filterNot { it in chosenCorrections }
                                    speaker.stop()
                                    if (onReplace(after)) {
                                        batchUndoBefore = before
                                        batchUndoAfter = after
                                        batchNotice = "Gekozen correcties vergeten. " +
                                            "De vorige lijst is tijdelijk herstelbaar."
                                        chosenCorrections = emptySet()
                                        batchMode = false
                                        batchSnapshot = null
                                        confirmBatch = false
                                    } else batchNotice = "Verwijderen geweigerd; niets gewijzigd."
                                }
                            }
                        ) { Text("Ja, vergeet uitsluitend deze correcties") }
                        TextButton(onClick = { confirmBatch = false }) {
                            Text("Nog niet verwijderen")
                        }
                    } else {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = chosenCorrections.isNotEmpty() &&
                                batchSnapshot == entries,
                            onClick = { confirmBatch = true }
                        ) { Text("Controleer mijn verwijderselectie…") }
                    }
                    TextButton(onClick = {
                        batchMode = false
                        batchSnapshot = null
                        chosenCorrections = emptySet()
                        confirmBatch = false
                    }) { Text("Annuleer meervoudige selectie") }
                }
                if (batchNotice.isNotBlank()) {
                    Text(batchNotice, style = MaterialTheme.typography.bodySmall)
                }
                val rollback = batchUndoBefore
                if (!batchMode && rollback != null && entries == batchUndoAfter) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            speaker.stop()
                            if (onReplace(rollback)) {
                                batchUndoBefore = null
                                batchUndoAfter = null
                                batchNotice = "Vorige tijdelijke correcties hersteld."
                            } else batchNotice = "Herstellen geweigerd; niets gewijzigd."
                        }
                    ) { Text("↶ Maak meervoudig vergeten ongedaan") }
                }
                if (entries.isEmpty()) Text("Nog geen correcties onthouden.")
                else if (visible.isEmpty()) Text("Geen passend voorstel gevonden.")

                visible.forEach { entry ->
                    HorizontalDivider()
                    if (batchMode) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Checkbox(
                                checked = entry in chosenCorrections,
                                enabled = entries == batchSnapshot,
                                onCheckedChange = { checked ->
                                    chosenCorrections = if (checked)
                                        chosenCorrections + entry
                                    else chosenCorrections - entry
                                    confirmBatch = false
                                }
                            )
                            Text("Selecteer om te vergeten",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text(
                        entry.sourceCode + " → " + entry.targetCode,
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (batchMode) {
                        Text(entry.original, style = MaterialTheme.typography.bodyMedium)
                        Text(entry.translation, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary)
                    } else if (editing == entry) {
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
                                speaker.stop()
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
                if (entries.isNotEmpty() && !batchMode) {
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
    if (practiceOpen) {
        SessionCorrectionPracticeDialog(entries, speaker) { practiceOpen = false }
    }
    if (transferOpen) {
        SessionCorrectionTransferDialog(
            entries = entries,
            initialPastedText = incomingTransferText,
            onInitialPastedTextConsumed = onIncomingTransferTextConsumed,
            onReplace = { imported ->
                speaker.stop()
                onReplace(imported).also { changed ->
                    if (changed) {
                        editing = null
                        editError = ""
                        confirmClear = false
                    }
                }
            },
            onDismiss = { transferOpen = false }
        )
    }
    if (manualAddOpen) {
        SessionCorrectionManualAddDialog(
            entries = entries,
            initialTargetCode = initialTargetCode,
            onAdd = { snapshot, candidate ->
                if (entries != snapshot ||
                    !SessionTranslationCorrections.validEntries(snapshot + candidate)) {
                    false
                } else {
                    speaker.stop()
                    onReplace(snapshot + candidate).also { changed ->
                        if (changed) {
                            editing = null
                            editError = ""
                            confirmClear = false
                        }
                    }
                }
            },
            onDismiss = { manualAddOpen = false }
        )
    }
    if (manualBatchOpen) {
        SessionCorrectionManualBatchDialog(
            entries = entries,
            initialTargetCode = initialTargetCode,
            onAddBatch = { snapshot, additions ->
                if (entries != snapshot || additions.isEmpty() ||
                    additions.size > 10 ||
                    !SessionTranslationCorrections.validEntries(snapshot + additions)) {
                    false
                } else {
                    speaker.stop()
                    onReplace(snapshot + additions).also { changed ->
                        if (changed) {
                            editing = null
                            editError = ""
                            confirmClear = false
                        }
                    }
                }
            },
            onDismiss = { manualBatchOpen = false }
        )
    }
}
