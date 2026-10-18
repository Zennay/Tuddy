package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import com.livelingo.android.PrivateSubtitleRetiming

/** User-approved batch edit, using the editor's undo and separate SAF export. */
@Composable
internal fun SubtitleRetimingDialog(
    draft: List<PrivateSubtitleCue>,
    onApply: (List<PrivateSubtitleCue>) -> Unit,
    onDismiss: () -> Unit
) {
    var percentage by remember { mutableIntStateOf(100) }
    var offsetMs by remember { mutableStateOf(0L) }
    var anchorFirst by remember { mutableStateOf(true) }
    val preview = PrivateSubtitleRetiming.preview(draft, percentage, offsetMs, anchorFirst)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tijdcodes van alle regels corrigeren") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Schaal of verschuif je ondertiteltijden handmatig. " +
                    "Geen automatische afstemming op video of audio.",
                    style = MaterialTheme.typography.bodySmall)
                Text("Schaal de tijdlijn", style = MaterialTheme.typography.labelLarge)
                listOf(listOf(80, 90, 95, 100), listOf(105, 110, 125)).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        row.forEach { value ->
                            TextButton(onClick = { percentage = value }) {
                                Text(value.toString() + "%" +
                                    if (percentage == value) " ✓" else "")
                            }
                        }
                    }
                }
                Text("Extra verschuiving", style = MaterialTheme.typography.labelLarge)
                listOf(listOf(-5000L, -2000L, -500L, 0L),
                    listOf(500L, 2000L, 5000L)).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        row.forEach { value ->
                            val seconds = when (value) {
                                -500L -> "-0,5s"
                                500L -> "+0,5s"
                                else -> (if (value > 0L) "+" else "") +
                                    (value / 1000L).toString() + "s"
                            }
                            TextButton(onClick = { offsetMs = value }) {
                                Text(seconds + if (offsetMs == value) " ✓" else "")
                            }
                        }
                    }
                }
                Text("Schaal vanaf", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { anchorFirst = true }) {
                        Text("Eerste regel" + if (anchorFirst) " ✓" else "")
                    }
                    TextButton(onClick = { anchorFirst = false }) {
                        Text("00:00:00" + if (!anchorFirst) " ✓" else "")
                    }
                }
                if (preview == null) {
                    Text("Geen toepasbaar voorbeeld: geen wijziging, een " +
                        "tijdgrens bereikt of een ongeldige cue. Kies andere waarden.",
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("Voorbeeld · " + preview.updated.size + " regels",
                        style = MaterialTheme.typography.titleSmall)
                    Text("Eerste start: " +
                        PrivateSubtitleRetiming.label(preview.oldFirstMs) + " → " +
                        PrivateSubtitleRetiming.label(preview.newFirstMs))
                    Text("Laatste einde: " +
                        PrivateSubtitleRetiming.label(preview.oldLastMs) + " → " +
                        PrivateSubtitleRetiming.label(preview.newLastMs))
                    Text("Overlappende buurregels: " + preview.overlappingPairs,
                        style = MaterialTheme.typography.bodySmall)
                }
                Text("Alleen het tijdelijke concept wijzigt. Draai één " +
                    "bewerking terug via Terug; bewaar daarna desgewenst een " +
                    "nieuwe SRT/VTT-kopie. Je origineel blijft intact.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(enabled = preview != null, onClick = {
                PrivateSubtitleRetiming.preview(draft, percentage, offsetMs, anchorFirst)
                    ?.let(onApply)
            }) { Text("Pas toe in tijdelijk concept") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuleren") } }
    )
}
