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
import com.livelingo.android.PrivateSubtitleRowSelection
import com.livelingo.android.PrivateSubtitleScopedShift

/**
 * Targeted manual correction. The new timeline is complete and must satisfy
 * strict SRT and WebVTT export boundaries before the user can apply it.
 */
@Composable
internal fun SubtitleScopedShiftDialog(
    draft: List<PrivateSubtitleCue>,
    foundSourceRows: List<Int>,
    onApply: (List<PrivateSubtitleCue>) -> Unit,
    onDismiss: () -> Unit
) {
    val initialFound = remember(draft, foundSourceRows) {
        PrivateSubtitleRowSelection.formatSourceRows(foundSourceRows, draft.size)
    }
    var rows by remember(draft) { mutableStateOf(initialFound.orEmpty()) }
    var offset by remember(draft) { mutableStateOf(500L) }
    var page by remember(draft) { mutableIntStateOf(0) }
    val preview = PrivateSubtitleScopedShift.preview(draft, rows, offset)
    val pageCount = maxOf(1, ((preview?.movedRows?.size ?: 0) + 4) / 5)
    val shown = page.coerceIn(0, pageCount - 1)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tijd verschuiven · gekozen bronregels") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 530.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Verplaats alleen de oorspronkelijke, door jou " +
                    "aangewezen ondertitels. Alle overige tijdcodes en " +
                    "teksten blijven exact gelijk. Geen automatische " +
                    "audio-uitlijning of woordtiming.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = rows,
                    onValueChange = { rows = it.take(2000); page = 0 },
                    label = { Text("Originele rijen: 2-4,7,12") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true)
                if (initialFound != null) {
                    OutlinedButton(modifier = Modifier.fillMaxWidth(),
                        onClick = { rows = initialFound; page = 0 }) {
                        Text("Selecteer ${foundSourceRows.size} gevonden bronregels")
                    }
                }
                Text("Handmatige verschuiving:",
                    style = MaterialTheme.typography.labelMedium)
                listOf(
                    listOf(-5000L, -2000L, -500L),
                    listOf(500L, 2000L, 5000L)
                ).forEach { choices ->
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        choices.forEach { ms ->
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = { offset = ms; page = 0 }
                            ) {
                                val seconds = if (kotlin.math.abs(ms) == 500L)
                                    (if (ms < 0) "-0,5s" else "+0,5s")
                                else (if (ms > 0) "+" else "") +
                                    "${ms / 1000}s"
                                Text((if (offset == ms) "✓ " else "") + seconds)
                            }
                        }
                    }
                }
                if (preview == null) {
                    Text("Geen geldige wijziging: controleer bronregels, " +
                        "tijdgrenzen en de volgorde van alle cues. Als de " +
                        "verschuiving een andere regel voorbijloopt, wordt " +
                        "de hele handeling geweigerd.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("${preview.movedRows.size} gekozen bronregels · " +
                        "pagina ${shown + 1}/$pageCount. " +
                        "Overlappende opeenvolgende cues na deze correctie: " +
                        "${preview.newOverlapPairs}.",
                        style = MaterialTheme.typography.titleSmall)
                    preview.movedRows.drop(shown * 5).take(5).forEach { row ->
                        val previous = draft[row - 1]
                        val updated = preview.updated[row - 1]
                        Text("Regel $row\n" +
                            "VOOR: ${previous.startLabel} → ${previous.endLabel}\n" +
                            "NA: ${updated.startLabel} → ${updated.endLabel}\n" +
                            previous.text,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(enabled = shown > 0,
                            onClick = { page = shown - 1 }) { Text("← Vorige") }
                        TextButton(enabled = shown < pageCount - 1,
                            onClick = { page = shown + 1 }) { Text("Volgende →") }
                    }
                }
                Text("Pas alleen toe op je tijdelijke concept, als één " +
                    "herstelbare Terug/Opnieuw-stap. Een nieuwe opslagkopie " +
                    "vereist apart jouw Android-bestandskeuze.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(enabled = preview != null, onClick = {
                val confirmed = PrivateSubtitleScopedShift.preview(draft, rows, offset)
                if (confirmed != null && preview != null &&
                    confirmed.movedRows == preview.movedRows &&
                    confirmed.updated == preview.updated) {
                    onApply(confirmed.updated)
                }
            }) { Text("Verschuif gekozen regels tijdelijk") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuleren") } }
    )
}
