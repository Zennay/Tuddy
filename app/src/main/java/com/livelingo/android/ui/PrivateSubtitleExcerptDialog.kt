package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livelingo.android.PrivateSubtitleCue
import com.livelingo.android.PrivateSubtitleDraftEditor
import com.livelingo.android.PrivateSubtitleFilteredExcerpt
import com.livelingo.android.SubtitleDraftFormat
import com.livelingo.android.SubtitleExcerptTiming

/** Preview of exactly the matched subtitle cues; never edits the open file. */
@Composable
internal fun PrivateSubtitleExcerptDialog(
    selected: List<PrivateSubtitleCue>,
    onSaveCopy: (List<PrivateSubtitleCue>, SubtitleDraftFormat) -> Unit,
    onDismiss: () -> Unit
) {
    var timing by remember(selected) {
        mutableStateOf(SubtitleExcerptTiming.KEEP_SOURCE_TIMES)
    }
    var format by remember(selected) { mutableStateOf(SubtitleDraftFormat.SRT) }
    val proposed = PrivateSubtitleFilteredExcerpt.prepare(selected, timing)
    val bytes = proposed?.let { PrivateSubtitleDraftEditor.snapshot(it, format) }
    val lengthMillis = if (proposed != null)
        proposed.maxOf { it.endMillis } - proposed.first().startMillis else 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bewaar alleen gevonden ondertitels") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Je hebt bewust ${selected.size} gevonden regels gekozen. " +
                        "Niet-overeenkomende regels komen niet in de nieuwe kopie. " +
                        "Het geopende document blijft ongewijzigd.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(modifier = Modifier.fillMaxWidth(),
                    onClick = { timing = SubtitleExcerptTiming.KEEP_SOURCE_TIMES }) {
                    Text((if (timing == SubtitleExcerptTiming.KEEP_SOURCE_TIMES)
                        "✓ " else "") + "Behoud oorspronkelijke tijdcodes")
                }
                OutlinedButton(modifier = Modifier.fillMaxWidth(),
                    onClick = { timing = SubtitleExcerptTiming.COMPACT_SELECTED }) {
                    Text((if (timing == SubtitleExcerptTiming.COMPACT_SELECTED)
                        "✓ " else "") + "Maak aaneengesloten excerpt vanaf 00:00")
                }
                OutlinedButton(modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        format = if (format == SubtitleDraftFormat.SRT)
                            SubtitleDraftFormat.WEBVTT else SubtitleDraftFormat.SRT
                    }) {
                    Text("Bestand: " + (if (format == SubtitleDraftFormat.SRT)
                        "SRT · wissel naar VTT" else "WebVTT · wissel naar SRT"))
                }
                Text(
                    if (bytes == null)
                        "Deze selectie kan niet veilig worden opgeslagen. " +
                            "Controleer de regels, tijdcodes en grootte."
                    else "Voorbeeld: ${proposed!!.size} regels · lengte " +
                        "${lengthMillis / 1000} seconden · ${bytes.size} bytes.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Compact betekent dat alle niet-geselecteerde pauzes vervallen; " +
                        "er blijft 0,25 seconde tussen de geselecteerde tekstregels. " +
                        "Dit is GEEN audio-uitlijning of gecontroleerde vertaling.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Een volgende, afzonderlijke Android-bestandskeuze is vereist. " +
                        "Je provider kan de geëxporteerde tekst extern synchroniseren.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(enabled = bytes != null, onClick = {
                val fresh = PrivateSubtitleFilteredExcerpt.prepare(selected, timing)
                if (fresh != null && PrivateSubtitleDraftEditor.snapshot(fresh, format) != null) {
                    onDismiss()
                    onSaveCopy(fresh.toList(), format)
                }
            }) { Text("Kies locatie voor gefilterde kopie…") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        }
    )
}
