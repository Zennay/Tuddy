package com.livelingo.android.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
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
import com.livelingo.android.PrivatePlainTextSubtitleDraft
import com.livelingo.android.PrivateSubtitleCue
import com.livelingo.android.PrivateSubtitleFileReader
import com.livelingo.android.PrivateSubtitleImportDocument
import com.livelingo.android.PrivateSubtitleRowSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var ownMeaning by remember { mutableStateOf("") }
    var plainLines by remember { mutableStateOf(false) }
    var splitMode by remember { mutableStateOf(PlainSubtitleSplitMode.LINES) }
    var startMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(3_500L) }
    var manualTiming by remember { mutableStateOf(false) }
    var startSecondsInput by remember { mutableStateOf("0") }
    var durationSecondsInput by remember { mutableStateOf("3,5") }
    var durationMenu by remember { mutableStateOf(false) }
    var startMenu by remember { mutableStateOf(false) }
    var inputTooLarge by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var firstRow by remember { mutableStateOf("1") }
    var lastRow by remember { mutableStateOf("") }
    var explicitRows by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    var documentNotice by remember { mutableStateOf("") }
    val importScope = rememberCoroutineScope()
    var importRequestId by remember { mutableIntStateOf(0) }
    var importReading by remember { mutableStateOf(false) }
    // Document providers may be remote: never block Android's UI thread while
    // reading, and ignore a result if the input or mode was changed meanwhile.
    val openDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { selected ->
        if (selected == null) {
            importReading = false
            documentNotice = "Bestandskeuze geannuleerd; bestaande invoer behouden."
        } else {
            val request = importRequestId
            val beforeRaw = raw
            val beforeMeaning = ownMeaning
            val beforeMode = splitMode
            val beforePlain = plainLines
            importScope.launch {
                importReading = true
                val imported = withContext(Dispatchers.IO) {
                    PrivateSubtitleImportDocument.read(context.contentResolver, selected)
                }
                if (request != importRequestId || raw != beforeRaw ||
                    ownMeaning != beforeMeaning || splitMode != beforeMode ||
                    plainLines != beforePlain) {
                    importReading = false
                    documentNotice = "Invoer of formaat is ondertussen veranderd: " +
                        "het document is niet overgenomen."
                    return@launch
                }
                importReading = false
                when (imported) {
                    is PrivateSubtitleImportDocument.Result.Ready -> {
                        raw = imported.text
                        ownMeaning = ""
                        firstRow = "1"
                        lastRow = ""
                        explicitRows = ""
                        search = ""
                        page = 0
                        inputTooLarge = false
                        documentNotice = "Document lokaal ingelezen (${imported.sizeBytes} bytes). " +
                            "Controleer de gekozen invoermodus, alle regels en het voorbeeld. " +
                            "Er is nog niets toegepast."
                    }
                    is PrivateSubtitleImportDocument.Result.Rejected -> {
                        documentNotice = imported.reason + " De vorige invoer is behouden."
                    }
                }
            }
        }
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
    val parsed = remember(raw, ownMeaning, plainLines, chosenStartMs, chosenDurationMs, splitMode) {
        if (plainLines && chosenStartMs != null && chosenDurationMs != null)
            PrivatePlainTextSubtitleDraft.preview(
                raw, chosenStartMs, chosenDurationMs, splitMode,
                if (splitMode == PlainSubtitleSplitMode.PAIRED_TEXTBOXES) ownMeaning else ""
            )
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
                    enabled = !importReading,
                    onClick = {
                        importRequestId += 1
                        documentNotice = ""
                        openDocument.launch(arrayOf("*/*"))
                    }
                ) { Text("📂 Kies eigen UTF-8 SRT, VTT, CSV, TSV of TXT-document…") }
                Text(
                    "Selecteer eerst hieronder de bijpassende invoermodus. " +
                        "Het gekozen document vervangt alleen het tekstvak en wordt " +
                        "begrensd in RAM gelezen (maximaal 256 KiB). " +
                        "Tuddy schrijft niets terug; je gekozen Android-provider " +
                        "kan zelf online of gesynchroniseerd zijn.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (importReading) {
                    Text("Gekozen document begrensd aan het inlezen…",
                        style = MaterialTheme.typography.bodySmall)
                }
                if (documentNotice.isNotBlank()) {
                    Text(documentNotice, style = MaterialTheme.typography.bodySmall,
                        color = if (documentNotice.startsWith("Document lokaal"))
                            MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.error)
                }
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
                        "Voorzie iedere cue van begin- en eindtijd (volledig SRT/VTT). " +
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
