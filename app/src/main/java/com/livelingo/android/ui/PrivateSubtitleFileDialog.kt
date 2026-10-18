package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.livelingo.android.PrivateSubtitleCue
import com.livelingo.android.PrivateSubtitleDraftEditor
import com.livelingo.android.SubtitleDraftFormat

/** Explicitly opened, ephemeral subtitle document editor, never the live transcript. */
@Composable
internal fun PrivateSubtitleFileDialog(
    cues: List<PrivateSubtitleCue>,
    onDismiss: () -> Unit,
    onSaveCopy: (List<PrivateSubtitleCue>, SubtitleDraftFormat) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var shownIndex by remember { mutableIntStateOf(-1) }
    var large by remember { mutableStateOf(true) }
    var draft by remember(cues) { mutableStateOf(cues) }
    var edited by remember(cues) { mutableStateOf(false) }
    var editIndex by remember { mutableStateOf<Int?>(null) }
    var deleteIndex by remember { mutableStateOf<Int?>(null) }
    var editValue by remember { mutableStateOf("") }
    var requestSave by remember { mutableStateOf<SubtitleDraftFormat?>(null) }
    var notice by remember { mutableStateOf("") }
    val found = draft.mapIndexed { idx, row -> idx to row }
        .filter { (_, row) -> row.text.contains(query.trim(), ignoreCase = true) }
    val current = found.getOrNull(shownIndex)?.second
    fun nudgeSelected(start: Long, end: Long) {
        val selected = found.getOrNull(shownIndex)?.first ?: return
        val updated = PrivateSubtitleDraftEditor.adjustCue(draft, selected, start, end)
        if (updated == null) notice = "Tijd geweigerd: begin/eind of volgorde ongeldig."
        else { draft = updated; edited = true; notice = "Tijd van deze regel aangepast." }
    }

    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false,
            dismissOnClickOutside = false)) {
        Surface(modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxSize().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Eigen ondertitelconcept · SRT/WebVTT",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Sluiten") }
                }
                Text("Alle tekst en tijdcodes komen uit je gekozen bestand. " +
                    "Geen geverifieerde vertaling of audio-synchronisatie.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = query, onValueChange = {
                    query = it.take(100)
                    shownIndex = -1
                }, label = { Text("Zoek in dit document") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("${found.size} van ${draft.size} regels" +
                    (if (edited) " · niet bewaarde wijzigingen" else ""),
                    style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = {
                        val next = PrivateSubtitleDraftEditor.shift(draft, -500L)
                        if (next == null) notice = "Kan niet eerder: negatieve tijdcode."
                        else { draft = next; edited = true; notice = "0,5 s eerder gezet." }
                    }) { Text("−0,5 s") }
                    TextButton(onClick = {
                        val next = PrivateSubtitleDraftEditor.shift(draft, 500L)
                        if (next == null) notice = "Kan niet later: tijdgrens bereikt."
                        else { draft = next; edited = true; notice = "0,5 s later gezet." }
                    }) { Text("+0,5 s") }
                    TextButton(enabled = edited, onClick = {
                        draft = cues
                        edited = false
                        shownIndex = -1
                        notice = "Bewerkingen weggegooid; origineel ongewijzigd."
                    }) { Text("Herstel") }
                }
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = { requestSave = SubtitleDraftFormat.SRT }) {
                        Text("Bewaar kopie SRT…")
                    }
                    OutlinedButton(onClick = { requestSave = SubtitleDraftFormat.WEBVTT }) {
                        Text("Bewaar kopie VTT…")
                    }
                }
                if (notice.isNotBlank()) Text(notice,
                    style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                if (current == null) {
                    LazyColumn(modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        itemsIndexed(found) { index, (_, cue) ->
                            Column {
                                TextButton(onClick = { shownIndex = index }) {
                                    Text("${index + 1}. ${cue.startLabel} → ${cue.endLabel} · Open")
                                }
                                Text(cue.text,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        textDirection = TextDirection.ContentOrRtl),
                                    maxLines = 2)
                                HorizontalDivider()
                            }
                        }
                    }
                } else {
                    Text("${current.startLabel} → ${current.endLabel}",
                        style = MaterialTheme.typography.labelLarge)
                    Text(current.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = TextDirection.ContentOrRtl),
                        fontSize = if (large) 30.sp else 22.sp,
                        lineHeight = if (large) 40.sp else 30.sp,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                            .verticalScroll(rememberScrollState()))
                    OutlinedButton(onClick = {
                        editIndex = found[shownIndex].first
                        editValue = current.text
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("✎ Corrigeer deze regel handmatig")
                    }
                    Text("Tijden van alleen deze regel · stap 0,5 s",
                        style = MaterialTheme.typography.labelSmall)
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { nudgeSelected(-500L, 0L) }) {
                            Text("Begin −0,5 s")
                        }
                        TextButton(onClick = { nudgeSelected(500L, 0L) }) {
                            Text("Begin +0,5 s")
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { nudgeSelected(0L, -500L) }) {
                            Text("Eind −0,5 s")
                        }
                        TextButton(onClick = { nudgeSelected(0L, 500L) }) {
                            Text("Eind +0,5 s")
                        }
                        TextButton(enabled = draft.size > 1,
                            onClick = { deleteIndex = found[shownIndex].first }) {
                            Text("Wis regel…")
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(enabled = shownIndex > 0,
                            onClick = { shownIndex-- }) { Text("← Vorige") }
                        TextButton(onClick = { large = !large }) {
                            Text(if (large) "Aa kleiner" else "Aa groter")
                        }
                        TextButton(enabled = shownIndex < found.lastIndex,
                            onClick = { shownIndex++ }) { Text("Volgende →") }
                        TextButton(onClick = { shownIndex = -1 }) { Text("Lijst") }
                    }
                }
                Text("Deze bewerkingen blijven in RAM totdat je bewust een nieuw " +
                    "bestand bewaart. Het geopende origineel wordt nooit overschreven. " +
                    "Android-bestandsproviders kunnen extern synchroniseren.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    val requestedDelete = deleteIndex
    if (requestedDelete != null) {
        AlertDialog(onDismissRequest = { deleteIndex = null },
            title = { Text("Deze ondertitelregel verwijderen?") },
            text = { Text("Alleen de tijdelijke documentkopie verandert. " +
                "Het originele bestand blijft ongewijzigd; Herstel kan dit terugdraaien.") },
            confirmButton = {
                TextButton(onClick = {
                    val newDraft = PrivateSubtitleDraftEditor.dropCue(draft, requestedDelete)
                    if (newDraft != null) {
                        draft = newDraft
                        edited = true
                        shownIndex = -1
                        notice = "Regel verwijderd uit tijdelijke kopie."
                    } else notice = "De laatste regel kan niet worden verwijderd."
                    deleteIndex = null
                }) { Text("Wis alleen deze regel") }
            },
            dismissButton = {
                TextButton(onClick = { deleteIndex = null }) { Text("Annuleren") }
            }
        )
    }
    val position = editIndex
    if (position != null) {
        AlertDialog(onDismissRequest = { editIndex = null },
            title = { Text("Handmatige tekstcorrectie") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("De tijdcode blijft gelijk. Tuddy controleert of vertaalt " +
                        "de nieuwe tekst niet automatisch.",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(value = editValue,
                        onValueChange = { if (it.length <= 2400) editValue = it },
                        label = { Text("Ondertiteltekst") },
                        modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 8,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = TextDirection.ContentOrRtl))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val next = PrivateSubtitleDraftEditor.revise(draft, position, editValue)
                    if (next == null) {
                        notice = "Ongeldige tekst. Maximaal acht regels, zonder controlecodes."
                    } else {
                        draft = next
                        edited = true
                        notice = "Tekst aangepast in tijdelijk concept."
                        shownIndex = -1
                    }
                    editIndex = null
                }) { Text("Gebruik correctie") }
            },
            dismissButton = {
                TextButton(onClick = { editIndex = null }) { Text("Annuleren") }
            }
        )
    }
    val chosenFormat = requestSave
    if (chosenFormat != null) {
        AlertDialog(onDismissRequest = { requestSave = null },
            title = { Text("Nieuw ondertitelbestand bewaren?") },
            text = { Text("Je kiest bewust een Android-bestandsprovider. " +
                "Die kan jouw tekst buiten Tuddy synchroniseren. Tijdcodes zijn " +
                "handmatige schattingen en teksten zijn niet geverifieerd. " +
                "Het eerder geopende bestand blijft ongewijzigd.") },
            confirmButton = {
                TextButton(onClick = {
                    requestSave = null
                    if (PrivateSubtitleDraftEditor.snapshot(draft, chosenFormat) == null)
                        notice = "Ongeldig concept: kan geen ondertitelbestand maken."
                    else onSaveCopy(draft.toList(), chosenFormat)
                }) { Text("Kies opslaglocatie…") }
            },
            dismissButton = {
                TextButton(onClick = { requestSave = null }) { Text("Annuleren") }
            }
        )
    }
}
