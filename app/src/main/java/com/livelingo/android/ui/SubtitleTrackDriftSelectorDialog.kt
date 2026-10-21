package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livelingo.android.PrivateSubtitleCue
import com.livelingo.android.PrivateSubtitleDriftSelection
import com.livelingo.android.PrivateSubtitleTrackDriftAlignment

/**
 * Author both anchor pairs explicitly. This dialog only returns a selection:
 * its caller recomputes against the current immutable imported source before
 * pairing and again on final consent. No file, microphone or media access.
 */
@Composable
internal fun SubtitleTrackDriftSelectorDialog(
    primary: List<PrivateSubtitleCue>,
    secondary: List<PrivateSubtitleCue>,
    initial: PrivateSubtitleDriftSelection?,
    onChoose: (PrivateSubtitleDriftSelection) -> Unit,
    onDismiss: () -> Unit
) {
    var firstPrimary by remember(primary, secondary) {
        mutableStateOf(initial?.firstPrimaryRow ?: "1")
    }
    var firstSecondary by remember(primary, secondary) {
        mutableStateOf(initial?.firstSecondaryRow ?: "1")
    }
    var lastPrimary by remember(primary, secondary) {
        mutableStateOf(initial?.lastPrimaryRow ?: primary.size.toString())
    }
    var lastSecondary by remember(primary, secondary) {
        mutableStateOf(initial?.lastSecondaryRow ?: secondary.size.toString())
    }
    val selection = PrivateSubtitleDriftSelection(
        firstPrimary, firstSecondary, lastPrimary, lastSecondary)
    val preview = PrivateSubtitleTrackDriftAlignment.preview(
        primary, secondary, firstPrimary, firstSecondary, lastPrimary, lastSecondary)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tweede spoor: geleidelijk tijdverschil corrigeren") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 540.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text("Kies twee bij elkaar horende bronregelparen: één vroeg " +
                    "en één later. Tuddy gebruikt alleen die vier bestaande " +
                    "begintijden om het tweede spoor lineair aan te passen. " +
                    "Er wordt geen geluid geanalyseerd of taal vertaald.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        firstPrimary = "1"
                        firstSecondary = "1"
                        lastPrimary = primary.size.toString()
                        lastSecondary = secondary.size.toString()
                    }
                ) { Text("Vul eerste en laatste regel per spoor in") }
                Text("Vroeg ankerpaar", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f), value = firstPrimary,
                        onValueChange = { firstPrimary = it.take(3) },
                        label = { Text("Spoor 1 · regel") }, singleLine = true)
                    OutlinedTextField(
                        modifier = Modifier.weight(1f), value = firstSecondary,
                        onValueChange = { firstSecondary = it.take(3) },
                        label = { Text("Spoor 2 · regel") }, singleLine = true)
                }
                Text("Later ankerpaar", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f), value = lastPrimary,
                        onValueChange = { lastPrimary = it.take(3) },
                        label = { Text("Spoor 1 · regel") }, singleLine = true)
                    OutlinedTextField(
                        modifier = Modifier.weight(1f), value = lastSecondary,
                        onValueChange = { lastSecondary = it.take(3) },
                        label = { Text("Spoor 2 · regel") }, singleLine = true)
                }
                Text("Of tik rechtstreeks op de gewenste vroege en late " +
                    "ondertitelregel; bladeren verandert je keuze niet vanzelf.",
                    style = MaterialTheme.typography.bodySmall)
                SubtitleTrackAnchorBrowser(
                    label = "Eerste spoor", cues = primary,
                    selectedFirst = firstPrimary, selectedLast = lastPrimary,
                    onFirst = { firstPrimary = it },
                    onLast = { lastPrimary = it })
                SubtitleTrackAnchorBrowser(
                    label = "Tweede spoor", cues = secondary,
                    selectedFirst = firstSecondary, selectedLast = lastSecondary,
                    onFirst = { firstSecondary = it },
                    onLast = { lastSecondary = it })
                if (preview == null) {
                    Text("Controleer bestaande chronologische rijnummers " +
                        "(spoor 1: 1–${primary.size}, spoor 2: 1–${secondary.size}). " +
                        "Beide paren moeten verschillen. De offset mag per " +
                        "ankerpaar maximaal 600 seconden zijn en het verschil " +
                        "tussen beide tijdsduren maximaal een factor twee. " +
                        "Een ongeldige SRT/VTT-tijdlijn wordt geweigerd.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("Spoor 1 vroeg · ${preview.firstPrimary.startLabel}\n" +
                        preview.firstPrimary.text,
                        style = MaterialTheme.typography.bodySmall)
                    Text("Spoor 2 vroeg · ${preview.firstSecondary.startLabel}\n" +
                        preview.firstSecondary.text,
                        style = MaterialTheme.typography.bodySmall)
                    Text("Spoor 1 later · ${preview.lastPrimary.startLabel}\n" +
                        preview.lastPrimary.text,
                        style = MaterialTheme.typography.bodySmall)
                    Text("Spoor 2 later · ${preview.lastSecondary.startLabel}\n" +
                        preview.lastSecondary.text,
                        style = MaterialTheme.typography.bodySmall)
                    Text("Tijdsafstand tweede spoor ${preview.sourceSpanMs} ms → " +
                        "eerste spoor ${preview.targetSpanMs} ms. Deze " +
                        "schatting past ook de eindtijden van andere cues aan.",
                        style = MaterialTheme.typography.titleSmall)
                    Text("Na overnemen volgt nog de complete gekoppelde " +
                        "ondertitelpreview. Alleen bevestigen wijzigt het " +
                        "tijdelijke concept; later bewaren vereist een aparte " +
                        "Android-bestandskeuze.", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = preview != null, onClick = {
                val fresh = PrivateSubtitleTrackDriftAlignment.preview(
                    primary, secondary,
                    selection.firstPrimaryRow, selection.firstSecondaryRow,
                    selection.lastPrimaryRow, selection.lastSecondaryRow)
                if (fresh != null && preview != null &&
                    fresh.adjustedSecondary == preview.adjustedSecondary) onChoose(selection)
            }) { Text("Neem twee ankerparen over") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuleren") } }
    )
}

/** Browse up to 500 original cues without mutating either explicit anchor selection. */
@Composable
private fun SubtitleTrackAnchorBrowser(
    label: String,
    cues: List<PrivateSubtitleCue>,
    selectedFirst: String,
    selectedLast: String,
    onFirst: (String) -> Unit,
    onLast: (String) -> Unit
) {
    var page by remember(cues) { mutableIntStateOf(0) }
    var search by remember(cues) { mutableStateOf("") }
    var jumpToRow by remember(cues) { mutableStateOf("") }
    var expandedRows by remember(cues) { mutableStateOf(emptySet<Int>()) }

    // Search is a local display-only filter, never a cue deletion or reindexing.
    // Every selection continues to use the original 1-based source row.
    val visible = cues.withIndex().filter { item ->
        search.isBlank() || item.value.text.contains(search.trim(), ignoreCase = true)
    }
    val maxPage = if (visible.isEmpty()) 0 else (visible.size - 1) / 3
    val shownPage = page.coerceIn(0, maxPage)
    Text("$label · ${visible.size} van ${cues.size} originele regels · " +
        "pagina ${shownPage + 1}/${maxPage + 1}",
        style = MaterialTheme.typography.titleSmall)
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = search,
        onValueChange = {
            search = it.take(100)
            page = 0
        },
        label = { Text("Zoek in oorspronkelijke tekst") },
        placeholder = { Text("Exact woord of zinsdeel") },
        singleLine = true
    )
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = jumpToRow,
            onValueChange = { jumpToRow = it.filter { c -> c in '0'..'9' }.take(3) },
            label = { Text("Ga naar originele regel") },
            singleLine = true
        )
        val destination = jumpToRow.toIntOrNull()
        OutlinedButton(
            enabled = destination != null && destination in 1..cues.size,
            onClick = {
                val row = jumpToRow.toIntOrNull() ?: return@OutlinedButton
                if (row in 1..cues.size) {
                    // A direct jump reveals that row even when the old filter hid it.
                    search = ""
                    page = (row - 1) / 3
                }
            }
        ) { Text("Ga") }
    }
    if (visible.isEmpty()) {
        Text("Geen bronregels gevonden. Pas de zoektekst aan; de gekozen " +
            "ankerparen blijven ongewijzigd.",
            style = MaterialTheme.typography.bodySmall)
    }
    visible.drop(shownPage * 3).take(3).forEach { item ->
        val rowNumber = item.index + 1
        val row = rowNumber.toString()
        val cue = item.value
        val expanded = rowNumber in expandedRows
        Text("Originele regel $row · ${cue.startLabel} → ${cue.endLabel}\n" +
            (if (expanded) cue.text else cue.text.replace('\n', ' ').take(95)),
            style = MaterialTheme.typography.bodySmall)
        if (cue.text.length > 95 || cue.text.contains('\n')) {
            TextButton(onClick = {
                expandedRows = if (expanded) expandedRows - rowNumber
                    else expandedRows + rowNumber
            }) {
                Text(if (expanded) "Verkort regel $row" else "Lees volledige regel $row")
            }
        }
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(modifier = Modifier.weight(1f),
                onClick = { onFirst(row) }) {
                Text((if (selectedFirst == row) "✓ " else "") + "Kies vroeg")
            }
            OutlinedButton(modifier = Modifier.weight(1f),
                onClick = { onLast(row) }) {
                Text((if (selectedLast == row) "✓ " else "") + "Kies later")
            }
        }
    }
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(enabled = shownPage > 0,
            onClick = { page = shownPage - 1 }) { Text("← Vorige $label") }
        TextButton(enabled = shownPage < maxPage,
            onClick = { page = shownPage + 1 }) { Text("Volgende →") }
    }
}
