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
import com.livelingo.android.PrivateSubtitleDraftEditor
import com.livelingo.android.PrivateSubtitleFilteredExcerpt
import com.livelingo.android.PrivateSubtitleRowSelection
import com.livelingo.android.PrivateTimedSubtitleSheet
import com.livelingo.android.PrivateLrcSubtitleExport
import com.livelingo.android.SubtitleDraftFormat
import com.livelingo.android.SubtitleExcerptTiming
import com.livelingo.android.TimedSubtitleSheetFormat

private enum class SubtitleExcerptSaveFormat { SRT, WEBVTT, CSV, TSV, LRC }

/**
 * One explicit selection, tied to the unfiltered immutable editor draft.
 * Original 1-based row numbers survive searches and paginated display.
 * Export uses the shared Activity-owned save handlers; this dialog has no IO.
 */
@Composable
internal fun PrivateSubtitleExcerptDialog(
    allCues: List<PrivateSubtitleCue>,
    initialSourceRows: List<Int>,
    onSaveCopy: (List<PrivateSubtitleCue>, SubtitleDraftFormat) -> Unit,
    onSaveTimedSheet: (List<PrivateSubtitleCue>, TimedSubtitleSheetFormat) -> Unit,
    onSaveLrc: (List<PrivateSubtitleCue>) -> Unit,
    onDismiss: () -> Unit
) {
    val initialExpression = remember(allCues, initialSourceRows) {
        PrivateSubtitleRowSelection.formatSourceRows(initialSourceRows, allCues.size).orEmpty()
    }
    var expression by remember(allCues, initialExpression) {
        mutableStateOf(initialExpression)
    }
    var timing by remember(allCues) {
        mutableStateOf(SubtitleExcerptTiming.KEEP_SOURCE_TIMES)
    }
    var format by remember(allCues) { mutableStateOf(SubtitleExcerptSaveFormat.SRT) }
    var rowPage by remember(allCues) { mutableIntStateOf(0) }
    val selectedIndices = PrivateSubtitleRowSelection.parse(expression, allCues.size)
    val original = selectedIndices?.map { allCues[it] }
    val proposed = original?.let { PrivateSubtitleFilteredExcerpt.prepare(it, timing) }

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
    val durationMs = if (proposed != null)
        proposed.maxOf { it.endMillis } - proposed.first().startMillis else 0L
    val pageCount = maxOf(1, ((selectedIndices?.size ?: 0) + 5) / 6)
    val currentPage = rowPage.coerceIn(0, pageCount - 1)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eigen ondertitelregels selecteren en bewaren") },
        text = {
            Column(modifier = Modifier.heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Kies de oorspronkelijke bronregelnummers van dit volledige " +
                    "tijdelijke concept. Zoeken en bladerpagina's veranderen de " +
                    "selectie niet ongemerkt; origineel en live spraak blijven intact.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = expression,
                    onValueChange = { expression = it.take(2000); rowPage = 0 },
                    label = { Text("Bronregels, bijvoorbeeld 1-5,9,12-16") },
                    singleLine = true)
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(modifier = Modifier.weight(1f),
                        onClick = {
                            expression = "1-${allCues.size}"
                            rowPage = 0
                        }) { Text("Alle regels") }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = initialExpression.isNotEmpty(),
                        onClick = {
                            expression = initialExpression
                            rowPage = 0
                        }) { Text("Gevonden selectie") }
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedIndices != null &&
                        selectedIndices.size < allCues.size,
                    onClick = {
                        val chosen = selectedIndices?.toSet().orEmpty()
                        val inverse = allCues.indices.filter { it !in chosen }.map { it + 1 }
                        val formatted = PrivateSubtitleRowSelection
                            .formatSourceRows(inverse, allCues.size)
                        if (formatted != null) {
                            expression = formatted
                            rowPage = 0
                        }
                    }) { Text("Kies alle overige, niet-geselecteerde regels") }
                if (selectedIndices == null) {
                    Text("Ongeldige of lege selectie. Gebruik originele nummers " +
                        "1–${allCues.size}, zonder dubbelen, overlap of " +
                        "omgekeerde bereiken.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("${selectedIndices.size} van ${allCues.size} originele " +
                        "regels geselecteerd · pagina ${currentPage + 1}/$pageCount",
                        style = MaterialTheme.typography.titleSmall)
                    selectedIndices.drop(currentPage * 6).take(6).forEach { index ->
                        val cue = allCues[index]
                        Text("Bronregel ${index + 1} · ${cue.startLabel} → " +
                            "${cue.endLabel}\n${cue.text}",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(enabled = currentPage > 0,
                            onClick = { rowPage = currentPage - 1 }) {
                            Text("← Vorige bronregels")
                        }
                        TextButton(enabled = currentPage < pageCount - 1,
                            onClick = { rowPage = currentPage + 1 }) {
                            Text("Volgende →")
                        }
                    }
                }
                OutlinedButton(modifier = Modifier.fillMaxWidth(),
                    onClick = { timing = SubtitleExcerptTiming.KEEP_SOURCE_TIMES }) {
                    Text((if (timing == SubtitleExcerptTiming.KEEP_SOURCE_TIMES)
                        "✓ " else "") + "Behoud oorspronkelijke tijdcodes")
                }
                OutlinedButton(modifier = Modifier.fillMaxWidth(),
                    onClick = { timing = SubtitleExcerptTiming.COMPACT_SELECTED }) {
                    Text((if (timing == SubtitleExcerptTiming.COMPACT_SELECTED)
                        "✓ " else "") + "Maak compact fragment vanaf 00:00")
                }
                Text("Kies het formaat:", style = MaterialTheme.typography.labelMedium)
                SubtitleExcerptSaveFormat.entries.forEach { option ->
                    OutlinedButton(modifier = Modifier.fillMaxWidth(),
                        onClick = { format = option }) {
                        Text((if (format == option) "✓ " else "") + option.name)
                    }
                }
                Text("Compacter maken verwijdert pauzes met 0,25 seconde " +
                    "tussen cues. Dat is geen echte audio-uitlijning. " +
                    "CSV/TSV vereist uniforme eigen tekstkolommen en " +
                    "veilige spreadsheetwaarden; LRC bevat alleen " +
                    "begintijden en kan geen eindtijden bewaren.",
                    style = MaterialTheme.typography.bodySmall)
                if (bytes == null) {
                    Text("Deze selectie kan niet volledig en veilig als " +
                        "${format.name} worden bewaard. Controleer regelkeuze, " +
                        "tekst, tijdcodes en maximaal 256 KiB; verander " +
                        "eventueel het formaat. Er wordt niets weggelaten.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("${proposed!!.size} cues · ${durationMs / 1000.0}s " +
                        "· ${bytes.size} UTF-8 bytes. Volledige werkelijke inhoud:",
                        style = MaterialTheme.typography.titleSmall)
                    PrivateSubtitlePagedExportPreview(bytes)
                }
                Text("Pas na de aparte, expliciete Android-bestandskeuze " +
                    "wordt een nieuw bestand geschreven. Een gekozen " +
                    "externe provider kan zelf gegevens synchroniseren.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(enabled = bytes != null && selectedIndices != null, onClick = {
                // Re-resolve the entire current selection and exact bytes at consent.
                val reselected = PrivateSubtitleRowSelection
                    .parse(expression, allCues.size)
                    ?.map { allCues[it] }
                val fresh = reselected?.let {
                    PrivateSubtitleFilteredExcerpt.prepare(it, timing)
                }
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
            }) { Text("Kies opslaglocatie…") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuleren") } }
    )
}
