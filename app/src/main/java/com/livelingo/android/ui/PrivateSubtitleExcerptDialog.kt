package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.livelingo.android.PrivateTimedSubtitleSheet
import com.livelingo.android.PrivateLrcSubtitleExport
import com.livelingo.android.SubtitleDraftFormat
import com.livelingo.android.SubtitleExcerptTiming
import com.livelingo.android.TimedSubtitleSheetFormat

private enum class SubtitleExcerptSaveFormat {
    SRT, WEBVTT, CSV, TSV, LRC
}

/** Preview only explicitly matched original cues; no changes to open/live files. */
@Composable
internal fun PrivateSubtitleExcerptDialog(
    selected: List<PrivateSubtitleCue>,
    onSaveCopy: (List<PrivateSubtitleCue>, SubtitleDraftFormat) -> Unit,
    onSaveTimedSheet: (List<PrivateSubtitleCue>, TimedSubtitleSheetFormat) -> Unit,
    onSaveLrc: (List<PrivateSubtitleCue>) -> Unit,
    onDismiss: () -> Unit
) {
    var timing by remember(selected) {
        mutableStateOf(SubtitleExcerptTiming.KEEP_SOURCE_TIMES)
    }
    var format by remember(selected) { mutableStateOf(SubtitleExcerptSaveFormat.SRT) }
    val proposed = PrivateSubtitleFilteredExcerpt.prepare(selected, timing)

    fun snapshot(cues: List<PrivateSubtitleCue>?): ByteArray? {
        if (cues == null) return null
        return when (format) {
            SubtitleExcerptSaveFormat.SRT ->
                PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.SRT)
            SubtitleExcerptSaveFormat.WEBVTT ->
                PrivateSubtitleDraftEditor.snapshot(cues, SubtitleDraftFormat.WEBVTT)
            SubtitleExcerptSaveFormat.CSV ->
                PrivateTimedSubtitleSheet.snapshot(cues, TimedSubtitleSheetFormat.CSV)
            SubtitleExcerptSaveFormat.TSV ->
                PrivateTimedSubtitleSheet.snapshot(cues, TimedSubtitleSheetFormat.TSV)
            SubtitleExcerptSaveFormat.LRC -> PrivateLrcSubtitleExport.snapshot(cues)
        }
    }

    val bytes = snapshot(proposed)
    val lengthMillis = if (proposed != null)
        proposed.maxOf { it.endMillis } - proposed.first().startMillis else 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bewaar alleen gevonden ondertitels") },
        text = {
            Column(modifier = Modifier.heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Alleen de ${selected.size} gevonden eigen ondertitelregels " +
                    "gaan in deze bewuste export. Het bestaande document, " +
                    "niet-gevonden regels en het live gesprek blijven intact.",
                    style = MaterialTheme.typography.bodyMedium)
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
                Text("Kies één uitwisselformaat:",
                    style = MaterialTheme.typography.labelMedium)
                SubtitleExcerptSaveFormat.entries.forEach { option ->
                    OutlinedButton(modifier = Modifier.fillMaxWidth(),
                        onClick = { format = option }) {
                        Text((if (format == option) "✓ " else "") + option.name)
                    }
                }
                Text("Compact verkort niet-geselecteerde pauzes en laat 0,25 " +
                    "seconde tussen gekozen cues. Dit is geen audiotiming. " +
                    "CSV/TSV vereist overal één of precies twee tekstregels " +
                    "zonder spreadsheetcode; LRC bewaart GEEN eindtijden " +
                    "en vereist één regel met strikt oplopende begintijden.",
                    style = MaterialTheme.typography.bodySmall)
                if (bytes == null) {
                    Text("Deze selectie kan niet veilig als ${format.name} " +
                        "worden opgeslagen. Kies een ander formaat of " +
                        "tijdcode-optie; tekst en overige gegevens " +
                        "worden nooit stilzwijgend verwijderd.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("${proposed!!.size} cues · lengte " +
                        "${lengthMillis / 1000.0}s · ${bytes.size} UTF-8 bytes. " +
                        "Bekijk de volledige werkelijke bestandstekst:",
                        style = MaterialTheme.typography.titleSmall)
                    PrivateSubtitlePagedExportPreview(bytes)
                }
                Text("Opslag vereist daarna jouw expliciete Android-" +
                    "bestandsproviderkeuze. Een externe provider kan zelf " +
                    "synchroniseren. Er wordt niets automatisch gedeeld.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(enabled = bytes != null, onClick = {
                val fresh = PrivateSubtitleFilteredExcerpt.prepare(selected, timing)
                val exact = snapshot(fresh)
                if (fresh != null && bytes != null && exact != null &&
                    exact.contentEquals(bytes)) {
                    onDismiss()
                    when (format) {
                        SubtitleExcerptSaveFormat.SRT ->
                            onSaveCopy(fresh.toList(), SubtitleDraftFormat.SRT)
                        SubtitleExcerptSaveFormat.WEBVTT ->
                            onSaveCopy(fresh.toList(), SubtitleDraftFormat.WEBVTT)
                        SubtitleExcerptSaveFormat.CSV ->
                            onSaveTimedSheet(fresh.toList(), TimedSubtitleSheetFormat.CSV)
                        SubtitleExcerptSaveFormat.TSV ->
                            onSaveTimedSheet(fresh.toList(), TimedSubtitleSheetFormat.TSV)
                        SubtitleExcerptSaveFormat.LRC -> onSaveLrc(fresh.toList())
                    }
                }
            }) { Text("Kies locatie voor gefilterde kopie…") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        }
    )
}
