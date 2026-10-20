package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.livelingo.android.PlainSubtitleSplitMode
import com.livelingo.android.PrivatePlainTextSubtitleDraft
import com.livelingo.android.PrivateSubtitleCue
import com.livelingo.android.PrivateSubtitleFileReader

/**
 * Intentional manual paste only. No clipboard reads, IO, cloud, transcript
 * edits or persistence. Plain-line timestamps are explicitly synthetic.
 */
@Composable
internal fun SubtitlePasteDialog(
    title: String,
    onDismiss: () -> Unit,
    onAccept: (List<PrivateSubtitleCue>) -> Unit
) {
    var raw by remember { mutableStateOf("") }
    var plainLines by remember { mutableStateOf(false) }
    var splitMode by remember { mutableStateOf(PlainSubtitleSplitMode.LINES) }
    var startMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(3_500L) }
    var durationMenu by remember { mutableStateOf(false) }
    var startMenu by remember { mutableStateOf(false) }
    var inputTooLarge by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var firstRow by remember { mutableStateOf("1") }
    var lastRow by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(0) }
    val parsed = remember(raw, plainLines, startMs, durationMs, splitMode) {
        if (plainLines) PrivatePlainTextSubtitleDraft.preview(raw, startMs, durationMs, splitMode)
        else PrivateSubtitleFileReader.read(raw.toByteArray(Charsets.UTF_8))
    }
    // Explicit 1-based row selection, never quietly clamped after editing.
    // A blank end means all rows through the end of the parsed source.
    val selectedRange = remember(parsed, firstRow, lastRow) {
        val start = firstRow.toIntOrNull()
        val end = if (lastRow.isBlank()) parsed?.size else lastRow.toIntOrNull()
        if (parsed == null || start == null || end == null ||
            start !in 1..parsed.size || end !in start..parsed.size) null
        else start..end
    }
    val filtered = remember(parsed, selectedRange, search) {
        val scope = selectedRange
        if (scope == null) emptyList()
        else parsed.orEmpty().withIndex().filter { row ->
            row.index + 1 in scope &&
                row.value.text.contains(search.trim().take(120), ignoreCase = true)
        }
    }
    val pageSize = 15
    val countPages = maxOf(1, (filtered.size + pageSize - 1) / pageSize)
    val safePage = page.coerceIn(0, countPages - 1)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Typ of plak bewust je eigen tekst. Het klembord wordt nooit " +
                        "automatisch gelezen en er vindt geen vertaling plaats.",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        plainLines = false
                        page = 0
                    }) { Text((if (!plainLines) "✓ " else "") + "SRT/VTT") }
                    OutlinedButton(onClick = {
                        plainLines = true
                        page = 0
                    }) { Text((if (plainLines) "✓ " else "") + "Gewone zinnen") }
                }
                if (plainLines) {
                    Text(
                        "Maak handmatig een ondertitelconcept van losse tekst. " +
                            "Automatische zinsgrenzen zijn een ruwe tekstheuristiek; " +
                            "tijden volgen GEEN opgenomen audio.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            splitMode = PlainSubtitleSplitMode.LINES
                            page = 0
                        }) { Text((if (splitMode == PlainSubtitleSplitMode.LINES) "✓ " else "") +
                            "Per regel") }
                        OutlinedButton(onClick = {
                            splitMode = PlainSubtitleSplitMode.SENTENCES
                            page = 0
                        }) { Text((if (splitMode == PlainSubtitleSplitMode.SENTENCES) "✓ " else "") +
                            "Per zin (schatting)") }
                    }
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                        splitMode = PlainSubtitleSplitMode.PAIRED_COLUMNS
                        page = 0
                    }) {
                        Text((if (splitMode == PlainSubtitleSplitMode.PAIRED_COLUMNS) "✓ " else "") +
                            "Twee handmatige tekstkolommen (tab gescheiden)")
                    }
                    if (splitMode == PlainSubtitleSplitMode.PAIRED_COLUMNS) {
                        Text(
                            "Plak per regel precies twee niet-lege velden gescheiden door " +
                                "een tab (bijvoorbeeld vanuit een spreadsheet): " +
                                "bronzin [TAB] eigen betekenis. Beide teksten verschijnen " +
                                "onder elkaar, zonder automatische vertaling of taalcontrole.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.foundation.layout.Box {
                            OutlinedButton(onClick = { startMenu = true }) {
                                Text("Begin: ${startMs / 1000}s ▾")
                            }
                            DropdownMenu(expanded = startMenu,
                                onDismissRequest = { startMenu = false }) {
                                PrivatePlainTextSubtitleDraft.startOptions.forEach { value ->
                                    DropdownMenuItem(
                                        text = { Text("${value / 1000} seconden") },
                                        onClick = {
                                            startMs = value
                                            startMenu = false
                                            page = 0
                                        }
                                    )
                                }
                            }
                        }
                        androidx.compose.foundation.layout.Box {
                            OutlinedButton(onClick = { durationMenu = true }) {
                                Text("Per zin: ${durationMs / 1000.0}s ▾")
                            }
                            DropdownMenu(expanded = durationMenu,
                                onDismissRequest = { durationMenu = false }) {
                                PrivatePlainTextSubtitleDraft.durationOptions.forEach { value ->
                                    DropdownMenuItem(
                                        text = { Text("${value / 1000.0} seconden") },
                                        onClick = {
                                            durationMs = value
                                            durationMenu = false
                                            page = 0
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        "Voorzie iedere cue van begin- en eindtijd (volledig SRT/VTT). " +
                            "Er wordt geen audio-uitlijning geclaimd.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedTextField(
                    value = raw,
                    onValueChange = { candidate ->
                        val candidateBytes = candidate.toByteArray(Charsets.UTF_8)
                        if (candidateBytes.size <= PrivateSubtitleFileReader.MAX_BYTES) {
                            raw = candidate
                            inputTooLarge = false
                            page = 0
                        } else {
                            inputTooLarge = true
                        }
                    },
                    label = { Text(when {
                        !plainLines -> "SRT/WebVTT-brontekst"
                        splitMode == PlainSubtitleSplitMode.PAIRED_COLUMNS -> "Brontaal [TAB] handmatige betekenis"
                        else -> "Eigen tekst of zinnen"
                    }) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    maxLines = 12
                )
                if (inputTooLarge) {
                    Text("Invoer geweigerd: meer dan 256 KiB. Verklein de bron.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
                if (parsed == null) {
                    Text(
                        if (plainLines)
                            "Geen geldig tekstconcept: 1–500 regels, " +
                                "maximaal 800 tekens per tekstregel. Twee kolommen " +
                                "vereisen precies één tab en beide waarden per regel."
                        else "Geen volledig geldig SRT/WebVTT-document: " +
                            "controleer tijdcodes en maximaal 500 regels.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        "${parsed.size} regels in de oorspronkelijke invoer. " +
                            (if (plainLines) "Tijdcodes zijn volledig geschat." else
                                "Tijdcodes komen uit de geplakte bron."),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = firstRow,
                            onValueChange = {
                                firstRow = it.filter(Char::isDigit).take(4)
                                page = 0
                            },
                            label = { Text("Van regel") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = lastRow,
                            onValueChange = {
                                lastRow = it.filter(Char::isDigit).take(4)
                                page = 0
                            },
                            label = { Text("Tot (leeg = alle)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    if (selectedRange == null) {
                        Text("Kies een geldig bereik tussen 1 en ${parsed.size}; " +
                            "er wordt niets overgeslagen zonder jouw keuze.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text(
                            "Alleen ${selectedRange.count()} gekozen regels worden geopend. " +
                                "Zoeken verandert de selectie niet.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    OutlinedTextField(
                        value = search,
                        onValueChange = {
                            search = it.take(120)
                            page = 0
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Zoek in volledige preview") },
                        singleLine = true
                    )
                    Text(
                        "${filtered.size} zoekresultaten · pagina ${safePage + 1} van ${countPages}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    filtered.drop(safePage * pageSize).take(pageSize).forEach { item ->
                        Text(
                            "Regel ${item.index + 1} · ${item.value.startLabel} → " +
                                "${item.value.endLabel}\n${item.value.text}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (filtered.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(enabled = safePage > 0, onClick = {
                                page = safePage - 1
                            }) { Text("← Vorige") }
                            TextButton(enabled = safePage < countPages - 1, onClick = {
                                page = safePage + 1
                            }) { Text("Volgende →") }
                        }
                    }
                    Text(
                        "Dit venster past niets toe op het live gesprek. " +
                            "Je kunt het tijdelijke concept in de editor controleren, " +
                            "aanpassen of expliciet als nieuwe kopie opslaan. " +
                            "De gekozen Android-bestandsprovider kan extern synchroniseren.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = parsed != null && selectedRange != null &&
                !inputTooLarge, onClick = {
                if (inputTooLarge) return@TextButton
                // Fresh full parse, row range and exact selected cue snapshot
                // at consent, independent of the search query and page.
                val confirmed = if (plainLines)
                    PrivatePlainTextSubtitleDraft.preview(raw, startMs, durationMs, splitMode)
                else PrivateSubtitleFileReader.read(raw.toByteArray(Charsets.UTF_8))
                val start = firstRow.toIntOrNull() ?: return@TextButton
                val end = if (lastRow.isBlank()) confirmed?.size
                    else lastRow.toIntOrNull()
                if (confirmed == null || end == null ||
                    start !in 1..confirmed.size || end !in start..confirmed.size)
                    return@TextButton
                onAccept(confirmed.subList(start - 1, end).toList())
            }) { Text("Open gekozen regels") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        }
    )
}
