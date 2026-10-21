package com.livelingo.android.ui

import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.livelingo.android.PlainSubtitleSplitMode
import com.livelingo.android.PrivateLrcSubtitleDraft
import com.livelingo.android.PrivatePlainTextSubtitleDraft
import com.livelingo.android.PrivateSubtitleCue
import com.livelingo.android.PrivateSubtitleFileReader
import com.livelingo.android.PrivateSubtitleRowSelection

/**
 * Exact bounded editor input returned by the Activity-owned Android picker.
 * Preserved in RAM only across that explicit provider choice; never persisted.
 */
internal data class SubtitlePasteFormat(
    val plainLines: Boolean,
    val lrcMode: Boolean,
    val splitMode: PlainSubtitleSplitMode
)
internal data class SubtitlePasteFileContent(
    val text: String,
    val format: SubtitlePasteFormat,
    val ownMeaning: String = "",
    val notice: String = "",
    val startMs: Long = 0L,
    val durationMs: Long = 3_500L,
    val manualTiming: Boolean = false,
    val startSeconds: String = "0",
    val durationSeconds: String = "3,5",
    val firstRow: String = "1",
    val lastRow: String = "",
    val explicitRows: String = "",
    val search: String = ""
)

/**
 * Intentional manual paste. No automatic clipboard reads, URI rights,
 * conversation mutation or persistence. Plain-line times are estimates.
 */
@Composable
internal fun SubtitlePasteDialog(
    title: String,
    onDismiss: () -> Unit,
    onAccept: (List<PrivateSubtitleCue>) -> Unit,
    onChooseDocument: (SubtitlePasteFileContent) -> Unit,
    initialDocument: SubtitlePasteFileContent? = null
) {
    var raw by remember(initialDocument) { mutableStateOf(initialDocument?.text.orEmpty()) }
    var ownMeaning by remember(initialDocument) {
        mutableStateOf(initialDocument?.ownMeaning.orEmpty())
    }
    var plainLines by remember(initialDocument) {
        mutableStateOf(initialDocument?.format?.plainLines ?: false)
    }
    var lrcMode by remember(initialDocument) {
        mutableStateOf(initialDocument?.format?.lrcMode ?: false)
    }
    var splitMode by remember(initialDocument) {
        mutableStateOf(initialDocument?.format?.splitMode ?: PlainSubtitleSplitMode.LINES)
    }
    var startMs by remember(initialDocument) {
        mutableStateOf(initialDocument?.startMs ?: 0L)
    }
    var durationMs by remember(initialDocument) {
        mutableStateOf(initialDocument?.durationMs ?: 3_500L)
    }
    var manualTiming by remember(initialDocument) {
        mutableStateOf(initialDocument?.manualTiming ?: false)
    }
    var startSecondsInput by remember(initialDocument) {
        mutableStateOf(initialDocument?.startSeconds ?: "0")
    }
    var durationSecondsInput by remember(initialDocument) {
        mutableStateOf(initialDocument?.durationSeconds ?: "3,5")
    }
    var durationMenu by remember { mutableStateOf(false) }
    var startMenu by remember { mutableStateOf(false) }
    var inputTooLarge by remember { mutableStateOf(false) }
    var search by remember(initialDocument) {
        mutableStateOf(initialDocument?.search.orEmpty())
    }
    var firstRow by remember(initialDocument) {
        mutableStateOf(initialDocument?.firstRow ?: "1")
    }
    var lastRow by remember(initialDocument) {
        mutableStateOf(initialDocument?.lastRow.orEmpty())
    }
    var explicitRows by remember(initialDocument) {
        mutableStateOf(initialDocument?.explicitRows.orEmpty())
    }
    var page by remember { mutableIntStateOf(0) }
    var documentNotice by remember(initialDocument) {
        mutableStateOf(initialDocument?.notice.orEmpty())
    }
    val timedInspection = remember(raw, plainLines, splitMode) {
        if (plainLines && splitMode in setOf(
            PlainSubtitleSplitMode.TIMED_TEXT_COLUMNS,
            PlainSubtitleSplitMode.TIMED_PAIRED_COLUMNS,
            PlainSubtitleSplitMode.TIMED_TEXT_CSV,
            PlainSubtitleSplitMode.TIMED_PAIRED_CSV
        )) PrivatePlainTextSubtitleDraft.inspectTimed(
            raw,
            splitMode == PlainSubtitleSplitMode.TIMED_PAIRED_COLUMNS ||
                splitMode == PlainSubtitleSplitMode.TIMED_PAIRED_CSV,
            splitMode == PlainSubtitleSplitMode.TIMED_TEXT_CSV ||
                splitMode == PlainSubtitleSplitMode.TIMED_PAIRED_CSV
        ) else null
    }
    val csvPairInspection = remember(raw, plainLines, splitMode) {
        if (plainLines && splitMode == PlainSubtitleSplitMode.PAIRED_CSV)
            PrivatePlainTextSubtitleDraft.inspectCsvPairs(raw)
        else null
    }
    val pairedInspection = remember(raw, ownMeaning, plainLines, splitMode) {
        if (plainLines && splitMode == PlainSubtitleSplitMode.PAIRED_TEXTBOXES)
            PrivatePlainTextSubtitleDraft.inspectPaired(raw, ownMeaning)
        else null
    }
    val estimatedMode = plainLines && splitMode !in setOf(
        PlainSubtitleSplitMode.TIMED_TEXT_COLUMNS,
        PlainSubtitleSplitMode.TIMED_PAIRED_COLUMNS,
        PlainSubtitleSplitMode.TIMED_TEXT_CSV,
        PlainSubtitleSplitMode.TIMED_PAIRED_CSV
    )
    val chosenStartMs = if (estimatedMode && manualTiming)
        PrivatePlainTextSubtitleDraft.manualSecondsMillis(
            startSecondsInput, 0L, PrivatePlainTextSubtitleDraft.MAX_USER_START_MS
        ) else startMs
    val chosenDurationMs = if (estimatedMode && manualTiming)
        PrivatePlainTextSubtitleDraft.manualSecondsMillis(
            durationSecondsInput,
            PrivatePlainTextSubtitleDraft.MIN_USER_DURATION_MS,
            PrivatePlainTextSubtitleDraft.MAX_USER_DURATION_MS
        ) else durationMs
    val parsed = remember(raw, ownMeaning, plainLines, lrcMode, chosenStartMs, chosenDurationMs, splitMode) {
        if (plainLines && chosenStartMs != null && chosenDurationMs != null)
            PrivatePlainTextSubtitleDraft.preview(
                raw, chosenStartMs, chosenDurationMs, splitMode,
                if (splitMode == PlainSubtitleSplitMode.PAIRED_TEXTBOXES) ownMeaning else ""
            )
        else if (!plainLines && lrcMode) PrivateLrcSubtitleDraft.preview(raw)
        else if (!plainLines) PrivateSubtitleFileReader.read(raw.toByteArray(Charsets.UTF_8))
        else null
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
    // Only one explicit row-selection mode is in force. A nonempty list
    // overrides the contiguous range; search filters PREVIEW only.
    val chosenIndices = remember(parsed, selectedRange, explicitRows) {
        if (parsed == null) null
        else if (explicitRows.isNotBlank())
            PrivateSubtitleRowSelection.parse(explicitRows, parsed.size)
        else selectedRange?.map { it - 1 }
    }
    val filtered = remember(parsed, chosenIndices, search) {
        val scope = chosenIndices?.toSet()
        if (scope == null) emptyList()
        else parsed.orEmpty().withIndex().filter { row ->
            row.index in scope &&
                row.value.text.contains(search.trim().take(120), ignoreCase = true)
        }
    }
    val foundRowsExpression = remember(parsed, filtered, search) {
        if (search.isBlank() || parsed == null) null
        else PrivateSubtitleRowSelection.formatSourceRows(
            filtered.map { it.index + 1 }, parsed.size
        )
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
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onChooseDocument(SubtitlePasteFileContent(
                            text = raw,
                            format = SubtitlePasteFormat(plainLines, lrcMode, splitMode),
                            ownMeaning = ownMeaning,
                            startMs = startMs,
                            durationMs = durationMs,
                            manualTiming = manualTiming,
                            startSeconds = startSecondsInput,
                            durationSeconds = durationSecondsInput,
                            firstRow = firstRow,
                            lastRow = lastRow,
                            explicitRows = explicitRows,
                            search = search
                        ))
                    }
                ) { Text("📂 Kies eigen UTF-8 SRT, VTT, LRC, CSV, TSV of TXT-document…") }
                Text(
                    "Selecteer hieronder eerst de passende SRT/VTT-, LRC- of tekstmodus. " +
                        "Het gekozen document vervangt alleen het tekstvak en wordt " +
                        "begrensd in RAM gelezen (maximaal 256 KiB). " +
                        "Tuddy schrijft niets terug; je gekozen Android-provider " +
                        "kan zelf online of gesynchroniseerd zijn.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (documentNotice.isNotBlank()) {
                    Text(documentNotice, style = MaterialTheme.typography.bodySmall,
                        color = if (documentNotice.startsWith("Document lokaal"))
                            MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.error)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        plainLines = false
                        lrcMode = false
                        page = 0
                    }) { Text((if (!plainLines && !lrcMode) "✓ " else "") + "SRT/VTT") }
                    OutlinedButton(onClick = {
                        plainLines = true
                        page = 0
                    }) { Text((if (plainLines) "✓ " else "") + "Gewone zinnen") }
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { plainLines = false; lrcMode = true; page = 0 }
                ) { Text((if (!plainLines && lrcMode) "✓ " else "") +
                    "LRC: eigen begintijden per regel") }
                if (plainLines) {
                    Text(
                        "Maak handmatig een ondertitelconcept van losse tekst. " +
                            "Automatische zinsgrenzen zijn een ruwe tekstheuristiek; " +
                            "tijden volgen GEEN opgenomen audio.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                            splitMode = PlainSubtitleSplitMode.LINES
                            page = 0
                        }) { Text((if (splitMode == PlainSubtitleSplitMode.LINES) "✓ " else "") +
                            "Per eigen regel") }
                        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                            splitMode = PlainSubtitleSplitMode.SENTENCES
                            page = 0
                        }) { Text((if (splitMode == PlainSubtitleSplitMode.SENTENCES) "✓ " else "") +
                            "Geschatte zinsscheiding") }
                    }
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                        splitMode = PlainSubtitleSplitMode.PAIRED_TEXTBOXES
                        page = 0
                    }) {
                        Text((if (splitMode == PlainSubtitleSplitMode.PAIRED_TEXTBOXES)
                            "✓ " else "") + "Twee losse tekstvakken: bron en eigen betekenis")
                    }
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                        splitMode = PlainSubtitleSplitMode.PAIRED_COLUMNS
                        page = 0
                    }) {
                        Text((if (splitMode == PlainSubtitleSplitMode.PAIRED_COLUMNS) "✓ " else "") +
                            "Twee handmatige tekstkolommen (tab gescheiden)")
                    }
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                        splitMode = PlainSubtitleSplitMode.TIMED_TEXT_COLUMNS
                        page = 0
                    }) {
                        Text((if (splitMode == PlainSubtitleSplitMode.TIMED_TEXT_COLUMNS)
                            "✓ " else "") +
                            "Drie kolommen: begin · einde · eigen tekst")
                    }
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                        splitMode = PlainSubtitleSplitMode.TIMED_PAIRED_COLUMNS
                        page = 0
                    }) {
                        Text((if (splitMode == PlainSubtitleSplitMode.TIMED_PAIRED_COLUMNS)
                            "✓ " else "") +
                            "Vier kolommen: begin · einde · bron · betekenis")
                    }
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                        splitMode = PlainSubtitleSplitMode.PAIRED_CSV
                        page = 0
                    }) {
                        Text((if (splitMode == PlainSubtitleSplitMode.PAIRED_CSV)
                            "✓ " else "") + "CSV: brontekst, eigen betekenis (tijden geschat)")
                    }
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                        splitMode = PlainSubtitleSplitMode.TIMED_TEXT_CSV
                        page = 0
                    }) {
                        Text((if (splitMode == PlainSubtitleSplitMode.TIMED_TEXT_CSV)
                            "✓ " else "") + "CSV: begintijd, eindtijd, eigen tekst")
                    }
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                        splitMode = PlainSubtitleSplitMode.TIMED_PAIRED_CSV
                        page = 0
                    }) {
                        Text((if (splitMode == PlainSubtitleSplitMode.TIMED_PAIRED_CSV)
                            "✓ " else "") + "CSV: begintijd, eindtijd, bron, betekenis")
                    }
                    if (splitMode == PlainSubtitleSplitMode.PAIRED_TEXTBOXES) {
                        Text(
                            "Plak originele zinnen in het eerste vak en je eigen " +
                                "betekenissen in het tweede. Elke positie hoort bij elkaar. " +
                                "Beide vakken moeten precies evenveel regels hebben; " +
                                "lege regels worden niet stilzwijgend overgeslagen.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (splitMode == PlainSubtitleSplitMode.PAIRED_CSV ||
                        splitMode == PlainSubtitleSplitMode.TIMED_TEXT_CSV ||
                        splitMode == PlainSubtitleSplitMode.TIMED_PAIRED_CSV) {
                        Text(
                            "Plak gewone komma-gescheiden CSV, één kaart per fysieke regel. " +
                                "Zet een veld met komma tussen dubbele aanhalingstekens; " +
                                "verdubbel binnenste aanhalingstekens. Tijdcodes met " +
                                "decimale komma moeten ook tussen aanhalingstekens. " +
                                "CSV met regelovergangen binnen een veld wordt geweigerd. " +
                                "Bron en eventuele betekenis zijn alleen jouw eigen tekst.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (splitMode == PlainSubtitleSplitMode.TIMED_TEXT_COLUMNS) {
                        Text(
                            "Plak drie tabgescheiden velden per regel: begin [TAB] einde " +
                                "[TAB] jouw eigen transcripttekst. Tijdcodes blijven exact " +
                                "zoals ingevoerd; er wordt geen ontbrekende vertaling " +
                                "gegenereerd. Corrigeer ongeldige bronrijen hieronder.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (splitMode == PlainSubtitleSplitMode.TIMED_PAIRED_COLUMNS) {
                        Text(
                            "Plak vier tabgescheiden velden per regel: " +
                                "begin [TAB] einde [TAB] eigen bronzin [TAB] eigen betekenis. " +
                                "Tijden bijvoorbeeld 00:00:12.500 of 01:23,456, met " +
                                "einde later dan begin. Bestaande tijden worden overgenomen, " +
                                "niet uit audio berekend.",
                            style = MaterialTheme.typography.bodySmall
                        )
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
                    if (splitMode !in setOf(
                            PlainSubtitleSplitMode.TIMED_PAIRED_COLUMNS,
                            PlainSubtitleSplitMode.TIMED_TEXT_COLUMNS,
                            PlainSubtitleSplitMode.TIMED_TEXT_CSV,
                            PlainSubtitleSplitMode.TIMED_PAIRED_CSV
                        )) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { manualTiming = !manualTiming; page = 0 }
                        ) {
                            Text(if (manualTiming) "✓ Aangepaste tijden (in seconden)" else
                                "Aangepaste begin- en duurinstelling…")
                        }
                        if (manualTiming) {
                            OutlinedTextField(
                                value = startSecondsInput,
                                onValueChange = { startSecondsInput = it.take(12); page = 0 },
                                label = { Text("Eerste cue begint na (0–21600 seconden)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = durationSecondsInput,
                                onValueChange = { durationSecondsInput = it.take(12); page = 0 },
                                label = { Text("Duur per kaart (0,5–30 seconden)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (chosenStartMs == null || chosenDurationMs == null) {
                                Text(
                                    "Vul geldige getallen in, eventueel met komma of punt " +
                                        "en maximaal drie decimalen. De tijden zijn " +
                                        "uitsluitend handmatige schattingen.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            androidx.compose.foundation.layout.Box {
                                OutlinedButton(modifier = Modifier.fillMaxWidth(),
                                    onClick = { startMenu = true }) {
                                    Text("Schatting begint na ${startMs / 1000} seconden ▾")
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
                                OutlinedButton(modifier = Modifier.fillMaxWidth(),
                                    onClick = { durationMenu = true }) {
                                    Text("Geschatte duur per kaart: ${durationMs / 1000.0}s ▾")
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
                        }
                    }
                } else {
                    Text(
                        if (lrcMode)
                            "LRC: één [mm:ss.xx]-begintijd per eigen regel. " +
                                "Elk einde wordt de volgende begintijd; de laatste " +
                                "eindtijd is geschat op 3 seconden, niet gemeten."
                        else "Voorzie iedere cue van begin- en eindtijd (volledig SRT/VTT). " +
                            "Er wordt geen audio-uitlijning geclaimd.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedTextField(
                    value = raw,
                    onValueChange = { candidate ->
                        val candidateBytes = candidate.toByteArray(Charsets.UTF_8)
                        val totalBytes = candidateBytes.size + (if (plainLines &&
                            splitMode == PlainSubtitleSplitMode.PAIRED_TEXTBOXES)
                            ownMeaning.toByteArray(Charsets.UTF_8).size else 0)
                        if (totalBytes <= PrivateSubtitleFileReader.MAX_BYTES) {
                            raw = candidate
                            documentNotice = ""
                            inputTooLarge = false
                            page = 0
                        } else {
                            inputTooLarge = true
                        }
                    },
                    label = { Text(when {
                        !plainLines && lrcMode -> "LRC: [mm:ss.xx] eigen tekst"
                        !plainLines -> "SRT/WebVTT-brontekst"
                        splitMode == PlainSubtitleSplitMode.PAIRED_TEXTBOXES ->
                            "Jouw originele zinnen (één per regel)"
                        splitMode == PlainSubtitleSplitMode.PAIRED_COLUMNS -> "Brontaal [TAB] handmatige betekenis"
                        splitMode == PlainSubtitleSplitMode.TIMED_PAIRED_COLUMNS ->
                            "Begin [TAB] einde [TAB] bron [TAB] betekenis"
                        splitMode == PlainSubtitleSplitMode.TIMED_TEXT_COLUMNS ->
                            "Begin [TAB] einde [TAB] zelf geschreven tekst"
                        splitMode == PlainSubtitleSplitMode.PAIRED_CSV ->
                            "Eigen bron, eigen betekenis (CSV, 2 kolommen)"
                        splitMode == PlainSubtitleSplitMode.TIMED_TEXT_CSV ->
                            "Begin,einde,tekst (CSV, 3 kolommen)"
                        splitMode == PlainSubtitleSplitMode.TIMED_PAIRED_CSV ->
                            "Begin,einde,bron,betekenis (CSV, 4 kolommen)"
                        else -> "Eigen tekst of zinnen"
                    }) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    maxLines = 12
                )
                if (plainLines && splitMode == PlainSubtitleSplitMode.PAIRED_TEXTBOXES) {
                    OutlinedTextField(
                        value = ownMeaning,
                        onValueChange = { candidate ->
                            val combined = raw.toByteArray(Charsets.UTF_8).size +
                                candidate.toByteArray(Charsets.UTF_8).size
                            if (combined <= PrivateSubtitleFileReader.MAX_BYTES) {
                                ownMeaning = candidate
                                inputTooLarge = false
                                page = 0
                            } else inputTooLarge = true
                        },
                        label = { Text("Jouw eigen betekenissen (één per bronregel)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5,
                        maxLines = 12
                    )
                    val inspection = pairedInspection
                    if (inspection != null) {
                        Text("Bron: ${inspection.sourceRows} regels · eigen betekenis: " +
                            "${inspection.meaningRows} regels.",
                            style = MaterialTheme.typography.bodySmall)
                        if (inspection.message != null) {
                            Text(inspection.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("Alle handmatige tekstparen zijn compleet. Bekijk " +
                                "de gezamenlijke preview hieronder voordat je opent.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (csvPairInspection != null) {
                    Text("${csvPairInspection.rowCount} CSV-tekstparen uit eigen invoer.",
                        style = MaterialTheme.typography.bodySmall)
                    csvPairInspection.message?.let { message ->
                        Text(message, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (timedInspection != null) {
                    Text(
                        "${timedInspection.rowCount} tijdregels uit eigen invoer.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    timedInspection.message?.let { error ->
                        Text(error, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (inputTooLarge) {
                    Text("Invoer geweigerd: meer dan 256 KiB. Verklein de bron.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
                if (parsed == null) {
                    Text(
                        if (plainLines)
                            "Geen geldig tekstconcept: 1–500 cues, maximaal 800 " +
                                "tekens per kolom. CSV kan 2 kolommen met eigen " +
                                "betekenis bevatten. Drie kolommen: begin/einde/bron; " +
                                "vier kolommen: begin/einde/bron/betekenis. " +
                                "Alle velden moeten expliciet geldig zijn."
                        else if (lrcMode) "Ongeldig LRC: één oplopende [mm:ss.xx] " +
                            "per niet-lege regel (max. 500 regels, 800 tekens); " +
                            "geen offsets of dubbele tijdstempels."
                        else "Geen volledig geldig SRT/WebVTT-document: " +
                            "controleer tijdcodes en maximaal 500 regels.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        "${parsed.size} regels in de oorspronkelijke invoer. " +
                            (if (plainLines &&
                                splitMode != PlainSubtitleSplitMode.TIMED_PAIRED_COLUMNS &&
                                splitMode != PlainSubtitleSplitMode.TIMED_TEXT_COLUMNS &&
                                splitMode != PlainSubtitleSplitMode.TIMED_TEXT_CSV &&
                                splitMode != PlainSubtitleSplitMode.TIMED_PAIRED_CSV)
                                "Tijdcodes zijn volledig geschat."
                             else if (lrcMode) "Beginmomenten uit LRC; " +
                                 "de laatste eindtijd is 3 s geschat."
                             else "Tijdcodes komen uit de zelf geplakte bron."),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = firstRow,
                            onValueChange = {
                                firstRow = it.filter { character -> character in '0'..'9' }.take(4)
                                page = 0
                            },
                            label = { Text("Van regel") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = lastRow,
                            onValueChange = {
                                lastRow = it.filter { character -> character in '0'..'9' }.take(4)
                                page = 0
                            },
                            label = { Text("Tot (leeg = alle)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = explicitRows,
                        onValueChange = { input ->
                            explicitRows = input.take(2_000)
                            page = 0
                        },
                        label = { Text("Losse regels (optioneel): 1-3,7,9-11") },
                        minLines = 1,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Als dit veld is ingevuld, vervangt het de selectie Van/Tot. " +
                            "Je kiest alleen jouw originele regelnummers; " +
                            "dubbele of overlappende regels worden geweigerd.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (chosenIndices == null) {
                        Text("Ongeldige selectie. Kies 1–${parsed.size} met Van/Tot, " +
                            "of geef unieke komma-gescheiden regels/bereiken op. " +
                            "Er wordt niets stilzwijgend weggelaten.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text(
                            "Exact ${chosenIndices.size} regels geselecteerd uit ${parsed.size} " +
                                "(bronvolgorde). Zoeken verandert deze selectie niet.",
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
                    if (search.isNotBlank()) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = foundRowsExpression != null && filtered.isNotEmpty(),
                            onClick = {
                                // User explicitly promotes visible search hits to
                                // the import selection, which remains editable.
                                val exact = foundRowsExpression ?: return@OutlinedButton
                                explicitRows = exact
                                page = 0
                            }
                        ) {
                            Text("Gebruik ${filtered.size} gevonden regels als selectie")
                        }
                        Text(
                            "Dit vervangt bewust de huidige importselectie door de " +
                                "gevonden originele bronregels. De knop opent of " +
                                "bewaart nog niets; bevestig dat apart hieronder.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(onClick = {
                            explicitRows = ""
                            firstRow = "1"
                            lastRow = ""
                            page = 0
                        }) { Text("Doorzoek opnieuw alle bronregels") }
                    }
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
            TextButton(enabled = parsed != null && chosenIndices != null &&
                !inputTooLarge, onClick = {
                if (inputTooLarge) return@TextButton
                // Fresh full parse, row range and exact selected cue snapshot
                // at consent, independent of the search query and page.
                val confirmed = if (plainLines)
                    PrivatePlainTextSubtitleDraft.preview(
                        raw, chosenStartMs ?: return@TextButton,
                        chosenDurationMs ?: return@TextButton, splitMode,
                        if (splitMode == PlainSubtitleSplitMode.PAIRED_TEXTBOXES) ownMeaning else ""
                    )
                else if (lrcMode) PrivateLrcSubtitleDraft.preview(raw)
                else PrivateSubtitleFileReader.read(raw.toByteArray(Charsets.UTF_8))
                if (confirmed == null) return@TextButton
                val indices = if (explicitRows.isNotBlank())
                    PrivateSubtitleRowSelection.parse(explicitRows, confirmed.size)
                else {
                    val start = firstRow.toIntOrNull() ?: return@TextButton
                    val end = if (lastRow.isBlank()) confirmed.size
                        else lastRow.toIntOrNull() ?: return@TextButton
                    if (start !in 1..confirmed.size || end !in start..confirmed.size)
                        return@TextButton
                    (start - 1 until end).toList()
                } ?: return@TextButton
                onAccept(indices.map { index -> confirmed[index] })
            }) { Text("Open gekozen regels") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        }
    )
}
