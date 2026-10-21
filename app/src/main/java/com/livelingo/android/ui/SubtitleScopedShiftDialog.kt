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
import com.livelingo.android.PrivateSubtitleTimeWindowRows
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
    var customOffset by remember(draft) { mutableStateOf("") }
    var page by remember(draft) { mutableIntStateOf(0) }
    var windowStart by remember(draft) { mutableStateOf("") }
    var windowEnd by remember(draft) { mutableStateOf("") }
    var windowRanges by remember(draft) { mutableStateOf("") }
    val activeOffset = if (customOffset.isBlank()) offset
        else manualSubtitleOffsetMillis(customOffset)
    val preview = activeOffset?.let { PrivateSubtitleScopedShift.preview(draft, rows, it) }
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
                Text("Of selecteer volledige cues op basis van een " +
                    "zelf ingevuld overlappend tijdvenster (geen audioanalyse):",
                    style = MaterialTheme.typography.bodySmall)
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(value = windowStart,
                        onValueChange = { windowStart = it.take(13) },
                        label = { Text("Vanaf mm:ss") },
                        modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = windowEnd,
                        onValueChange = { windowEnd = it.take(13) },
                        label = { Text("Tot mm:ss") },
                        modifier = Modifier.weight(1f), singleLine = true)
                }
                val selectedWindow = PrivateSubtitleTimeWindowRows.expression(
                    draft, windowStart, windowEnd)
                OutlinedButton(modifier = Modifier.fillMaxWidth(),
                    enabled = selectedWindow != null,
                    onClick = {
                        if (selectedWindow != null) {
                            rows = selectedWindow
                            page = 0
                        }
                    }) { Text("Selecteer cues die tijdvenster overlappen") }

                Text("Meerdere losse tijdvensters kiezen, bijvoorbeeld " +
                    "00:10-00:20;01:00-01:15. Maximaal 16 zonder overlap; " +
                    "een cue die beide vensters raakt, wordt één keer gekozen.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = windowRanges,
                    onValueChange = { windowRanges = it.take(512) },
                    label = { Text("Tijdvensters: vanaf-tot;vanaf-tot") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true)
                val multiWindowRows = PrivateSubtitleTimeWindowRows
                    .expressionForWindows(draft, windowRanges)
                OutlinedButton(modifier = Modifier.fillMaxWidth(),
                    enabled = multiWindowRows != null,
                    onClick = {
                        if (multiWindowRows != null) {
                            rows = multiWindowRows
                            page = 0
                        }
                    }) { Text("Neem cues uit alle gekozen tijdvensters") }
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
                                onClick = { offset = ms; customOffset = ""; page = 0 }
                            ) {
                                val seconds = if (kotlin.math.abs(ms) == 500L)
                                    (if (ms < 0) "-0,5s" else "+0,5s")
                                else (if (ms > 0) "+" else "") +
                                    "${ms / 1000}s"
                                Text((if (customOffset.isBlank() && offset == ms) "✓ " else "") + seconds)
                            }
                        }
                    }
                }
                Text("Of stel zelf een exacte verschuiving in: positief is " +
                    "later, negatief is eerder. Maximaal 600 seconden in " +
                    "beide richtingen, met maximaal drie decimalen.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = customOffset,
                    onValueChange = { customOffset = it.take(10); page = 0 },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Eigen seconden, bijv. -7,250") },
                    singleLine = true)
                if (customOffset.isNotBlank() && activeOffset == null) {
                    Text("Gebruik -600 tot +600 seconden (niet nul), " +
                        "met maximaal drie decimalen.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
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
                val confirmed = activeOffset?.let {
                    PrivateSubtitleScopedShift.preview(draft, rows, it)
                }
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

/** Exact millisecond conversion without floating-point rounding or locale guesses. */
private fun manualSubtitleOffsetMillis(raw: String): Long? {
    val match = Regex("""^([+-]?)([0-9]{1,3})(?:[.,]([0-9]{1,3}))?$""")
        .matchEntire(raw.trim()) ?: return null
    val seconds = match.groupValues[2].toLongOrNull() ?: return null
    val millis = match.groupValues[3].padEnd(3, '0')
        .ifEmpty { "0" }.toLongOrNull() ?: return null
    val absolute = seconds * 1_000L + millis
    if (absolute !in 1L..600_000L) return null
    return if (match.groupValues[1] == "-") -absolute else absolute
}
