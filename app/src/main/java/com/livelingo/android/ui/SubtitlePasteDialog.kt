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
import com.livelingo.android.PrivateAssSubtitleReader
import com.livelingo.android.PrivateSbvSubtitleReader
import com.livelingo.android.PrivateYoutubeJson3SubtitleReader
import com.livelingo.android.PrivateSubtitleRowSelection
import com.livelingo.android.PrivateSubtitleTimelineNavigation

/**
 * Exact bounded editor input returned by the Activity-owned Android picker.
 * Preserved in RAM only across that explicit provider choice; never persisted.
 */
internal data class SubtitlePasteFormat(
    val plainLines: Boolean,
    val lrcMode: Boolean,
    val splitMode: PlainSubtitleSplitMode
)
internal enum class LrcCorrectionTool {
    SHIFT_SELECTED, BULK_EXACT, SINGLE_EXACT, TWO_ANCHORS
}

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
    onSaveEditedLrcSource: ((SubtitlePasteFileContent) -> Unit)? = null,
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
    // Manually typed whole-track LRC offset. Stays transient in this dialog.
    var lrcOffsetInput by remember(initialDocument) { mutableStateOf("") }
    var lrcSelectedShiftInput by remember(initialDocument) { mutableStateOf("") }
    var lrcShiftPreviewPage by remember { mutableIntStateOf(0) }
    var lrcAnchorFirstInput by remember(initialDocument) { mutableStateOf("") }
    var lrcAnchorLastInput by remember(initialDocument) { mutableStateOf("") }
    var lrcAnchorPreviewPage by remember { mutableIntStateOf(0) }
    var lrcExactRowInput by remember(initialDocument) { mutableStateOf("") }
    var lrcExactTimeInput by remember(initialDocument) { mutableStateOf("") }
    var lrcExactPreviewPage by remember { mutableIntStateOf(0) }
    var lrcBatchInput by remember(initialDocument) { mutableStateOf("") }
    var lrcBatchPreviewPage by remember { mutableIntStateOf(0) }
    var lrcBatchTooLarge by remember { mutableStateOf(false) }
    var lrcCorrectionTool by remember(initialDocument) {
        mutableStateOf(LrcCorrectionTool.SHIFT_SELECTED)
    }
    // Only deliberate in-dialog LRC timing actions are undoable here.
    // Bounded to eight RAM snapshots (each input is already <=256 KiB).
    var lrcUndo by remember(initialDocument) { mutableStateOf(emptyList<String>()) }
    var lrcRedo by remember(initialDocument) { mutableStateOf(emptyList<String>()) }
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
    val commitLrcTiming: (String) -> Unit = { revised ->
        if (revised != raw) {
            lrcUndo = (lrcUndo + raw).takeLast(8)
            lrcRedo = emptyList()
            raw = revised
            page = 0
            lrcShiftPreviewPage = 0
        }
    }
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
            ?: PrivateAssSubtitleReader.read(raw)
            ?: PrivateSbvSubtitleReader.read(raw)
            ?: PrivateYoutubeJson3SubtitleReader.read(raw.takeIf { it.length <= 65_536 } ?: "")
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
    // This is only an in-memory proposal; changing text, cue selection or
    // offset invalidates it before an explicit second confirmation.
    val lrcSelectedShiftMillis = lrcSelectedShiftInput.trim().toLongOrNull()
    val lrcShiftProposal = remember(raw, chosenIndices, lrcSelectedShiftMillis, plainLines, lrcMode) {
        if (!plainLines && lrcMode && chosenIndices != null &&
            lrcSelectedShiftMillis != null) {
            PrivateLrcSubtitleDraft.withShiftedRows(
                raw, chosenIndices, lrcSelectedShiftMillis
            )
        } else null
    }
    val lrcShiftAfter = remember(lrcShiftProposal) {
        lrcShiftProposal?.let(PrivateLrcSubtitleDraft::preview)
    }
    val lrcAnchorIsContiguous = chosenIndices != null &&
        chosenIndices.size >= 2 &&
        chosenIndices == (chosenIndices.first()..chosenIndices.last()).toList()
    val lrcAnchorFirstMs = PrivateSubtitleTimelineNavigation.timestamp(lrcAnchorFirstInput)
    val lrcAnchorLastMs = PrivateSubtitleTimelineNavigation.timestamp(lrcAnchorLastInput)
    val lrcAnchorProposal = remember(
        raw, chosenIndices, lrcAnchorFirstMs, lrcAnchorLastMs, plainLines, lrcMode
    ) {
        if (!plainLines && lrcMode && lrcAnchorIsContiguous &&
            chosenIndices != null && lrcAnchorFirstMs != null &&
            lrcAnchorLastMs != null) {
            PrivateLrcSubtitleDraft.withRangeAnchors(
                raw, chosenIndices.first(), chosenIndices.last(),
                lrcAnchorFirstMs, lrcAnchorLastMs
            )
        } else null
    }
    val lrcAnchorAfter = remember(lrcAnchorProposal) {
        lrcAnchorProposal?.let(PrivateLrcSubtitleDraft::preview)
    }
    val lrcExactRowIndex = lrcExactRowInput.toIntOrNull()?.minus(1)
    val lrcExactTimeMillis = PrivateSubtitleTimelineNavigation.timestamp(lrcExactTimeInput)
    val lrcExactProposal = remember(
        raw, lrcExactRowIndex, lrcExactTimeMillis, plainLines, lrcMode
    ) {
        if (!plainLines && lrcMode && lrcExactRowIndex != null &&
            lrcExactTimeMillis != null) {
            PrivateLrcSubtitleDraft.withExactCueStart(
                raw, lrcExactRowIndex, lrcExactTimeMillis
            )
        } else null
    }
    val lrcExactAfter = remember(lrcExactProposal) {
        lrcExactProposal?.let(PrivateLrcSubtitleDraft::preview)
    }
    val lrcBatchProposal = remember(raw, lrcBatchInput, plainLines, lrcMode) {
        if (!plainLines && lrcMode)
            PrivateLrcSubtitleDraft.withExactCueBatch(raw, lrcBatchInput)
        else null
    }
    val lrcBatchAfter = remember(lrcBatchProposal) {
        lrcBatchProposal?.let(PrivateLrcSubtitleDraft::preview)
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
                            "LRC: één of meerdere [mm:ss.xx]-momenten vóór een " +
                                "eigen tekstregel; refreinen mogen dezelfde tekst op " +
                                "andere tijden herhalen. Elk moment wordt een aparte " +
                                "chronologische cue. Het volgende begin bepaalt " +
                                "het einde; het laatste eind is 3 s geschat."
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
                            // Manual typing starts a new unsaved edit history;
                            // do not undo unrelated authored text changes.
                            lrcUndo = emptyList()
                            lrcRedo = emptyList()
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
                        else if (lrcMode) "Ongeldig LRC: gebruik 1–16 aaneengesloten " +
                            "[mm:ss.xx]-tijden vóór dezelfde tekst. Maximaal 500 " +
                            "afzonderlijke cues, 800 teksttekens per regel; " +
                            "dubbele tijden, ongeldige of te grote offsets " +
                            "en ongeldige tekst worden geweigerd."
                        else "Geen volledig geldig SRT/WebVTT-document: " +
                            "controleer tijdcodes en maximaal 500 regels.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        "${parsed.size} " +
                            (if (lrcMode) "chronologisch uitgewerkte LRC-cues. " +
                                "Meerdere tijden per bronregel zijn afzonderlijke cues. "
                            else "regels in de oorspronkelijke invoer. ") +
                            (if (plainLines &&
                                splitMode != PlainSubtitleSplitMode.TIMED_PAIRED_COLUMNS &&
                                splitMode != PlainSubtitleSplitMode.TIMED_TEXT_COLUMNS &&
                                splitMode != PlainSubtitleSplitMode.TIMED_TEXT_CSV &&
                                splitMode != PlainSubtitleSplitMode.TIMED_PAIRED_CSV)
                                "Tijdcodes zijn volledig geschat."
                             else if (lrcMode) "Elke LRC-cue heeft een expliciet " +
                                 "bronbegin; eindtijden volgen uit het volgende " +
                                 "begin (laatste +3 s, geschat). Eventuele " +
                                 "zelf opgegeven offset is verwerkt."
                             else "Tijdcodes komen uit de zelf geplakte bron."),
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (lrcMode) {
                        if (onSaveEditedLrcSource != null) {
                            Text(
                                "Bewaar de volledige eigen LRC-brontekst inclusief " +
                                    "refreintags, zelfgeschreven lyrics, metadata " +
                                    "en eventuele [offset:]. Deze kopie is anders " +
                                    "dan opnieuw uit de afgeleide SRT/VTT-cues " +
                                    "gebouwde LRC; de oorspronkelijke bron " +
                                    "wordt niet overschreven.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    onSaveEditedLrcSource(SubtitlePasteFileContent(
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
                            ) {
                                Text("Bewaar deze volledige LRC-bron als nieuw .lrc-bestand…")
                            }
                        }
                        val declaredOffset = PrivateLrcSubtitleDraft.declaredOffsetMillis(raw)
                        Text(
                            if (declaredOffset == null)
                                "Geen [offset:] opgegeven; oorspronkelijke tijden gebruikt."
                            else "Expliciete LRC-offset verwerkt: " +
                                (if (declaredOffset >= 0L) "+" else "") +
                                declaredOffset + " ms op iedere cue. Positief = later, " +
                                "negatief = eerder. Deze aangepaste tijden " +
                                "staan in de volledige preview.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                enabled = lrcUndo.isNotEmpty(),
                                onClick = {
                                    val previous = lrcUndo.lastOrNull()
                                        ?: return@OutlinedButton
                                    lrcUndo = lrcUndo.dropLast(1)
                                    lrcRedo = (lrcRedo + raw).takeLast(8)
                                    raw = previous
                                    lrcSelectedShiftInput = ""
                                    lrcOffsetInput = ""
                                    page = 0
                                    lrcShiftPreviewPage = 0
                                    documentNotice = "Laatste tijdelijke LRC-tijdcorrectie " +
                                        "ongedaan gemaakt; het bronbestand is intact."
                                }
                            ) { Text("↶ LRC ongedaan (${lrcUndo.size})") }
                            OutlinedButton(
                                enabled = lrcRedo.isNotEmpty(),
                                onClick = {
                                    val next = lrcRedo.lastOrNull()
                                        ?: return@OutlinedButton
                                    lrcRedo = lrcRedo.dropLast(1)
                                    lrcUndo = (lrcUndo + raw).takeLast(8)
                                    raw = next
                                    lrcSelectedShiftInput = ""
                                    lrcOffsetInput = ""
                                    page = 0
                                    lrcShiftPreviewPage = 0
                                    documentNotice = "Tijdelijke LRC-tijdcorrectie opnieuw " +
                                        "toegepast; het bronbestand is intact."
                                }
                            ) { Text("↷ Opnieuw (${lrcRedo.size})") }
                        }
                        Text(
                            "Maximaal 8 bewuste tijdcorrecties terug; vrije bewerking " +
                                "in het tekstvak start een nieuwe geschiedenis. " +
                                "Alleen dit tijdelijke concept verandert.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        val currentOffset = declaredOffset ?: 0L
                        val applyOffset: (Long) -> Unit = { chosen ->
                            val updated = PrivateLrcSubtitleDraft.withDeclaredOffset(raw, chosen)
                            if (updated == null) {
                                documentNotice = "Deze offset geeft ongeldige of " +
                                    "negatieve tijden. De vorige eigen tekst blijft staan."
                            } else {
                                commitLrcTiming(updated)
                                lrcOffsetInput = ""
                                documentNotice = "Alle eigen LRC-cues zijn expliciet " +
                                    "bijgesteld; bekijk de nieuwe chronologische preview. " +
                                    "Het oorspronkelijke bestand is niet veranderd."
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                enabled = currentOffset >= -29_500L,
                                onClick = { applyOffset(currentOffset - 500L) }
                            ) { Text("− 0,5 s") }
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                enabled = currentOffset != 0L,
                                onClick = { applyOffset(0L) }
                            ) { Text("Offset 0") }
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                enabled = currentOffset <= 29_500L,
                                onClick = { applyOffset(currentOffset + 500L) }
                            ) { Text("+ 0,5 s") }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = lrcOffsetInput,
                                onValueChange = { candidate ->
                                    if (candidate.length <= 6 &&
                                        candidate.withIndex().all { (index, char) ->
                                            char in '0'..'9' ||
                                                (index == 0 && (char == '+' || char == '-'))
                                        }
                                    ) lrcOffsetInput = candidate
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text("Exacte offset in ms") },
                                placeholder = { Text("-125 of +750") }
                            )
                            val desired = lrcOffsetInput.toLongOrNull()
                            OutlinedButton(
                                enabled = desired != null && desired in -30_000L..30_000L &&
                                    desired != currentOffset,
                                onClick = { desired?.let(applyOffset) }
                            ) { Text("Pas toe") }
                        }
                        Text(
                            "Positieve milliseconden verschuiven alle woorden later, " +
                                "negatieve eerder (maximaal ±30000 ms). Alleen " +
                                "de huidige lokale LRC-tekst verandert na je tik. " +
                                "Geen audioanalyse of wijziging aan het originele " +
                                "bestand of de live transcriptie.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
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
                            (if (lrcMode)
                                "Kies de uitgewerkte, chronologisch genummerde " +
                                    "LRC-cues, niet de oorspronkelijke fysieke tekstregels. "
                            else "Je kiest alleen jouw originele regelnummers. ") +
                            "Dubbele of overlappende regels worden geweigerd.",
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
                                (if (lrcMode) "(chronologische LRC-cuevolgorde). "
                                 else "(bronvolgorde). ") +
                                "Zoeken verandert deze selectie niet.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (!plainLines && lrcMode) {
                        Text(
                            "Kies eerst één LRC-correctiegereedschap. " +
                                "Alle andere correctievelden blijven verborgen " +
                                "tot je ze zelf kiest; tussentijdse eigen tekst " +
                                "en keuzes verdwijnen niet door wisselen.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    lrcCorrectionTool = LrcCorrectionTool.SHIFT_SELECTED
                                }
                            ) {
                                Text((if (lrcCorrectionTool ==
                                    LrcCorrectionTool.SHIFT_SELECTED) "✓ " else "") +
                                    "Verschuif selectie")
                            }
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = { lrcCorrectionTool = LrcCorrectionTool.BULK_EXACT }
                            ) {
                                Text((if (lrcCorrectionTool ==
                                    LrcCorrectionTool.BULK_EXACT) "✓ " else "") +
                                    "Meerdere exacte tijden")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    lrcCorrectionTool = LrcCorrectionTool.SINGLE_EXACT
                                }
                            ) {
                                Text((if (lrcCorrectionTool ==
                                    LrcCorrectionTool.SINGLE_EXACT) "✓ " else "") +
                                    "Eén exact tijdstip")
                            }
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = { lrcCorrectionTool = LrcCorrectionTool.TWO_ANCHORS }
                            ) {
                                Text((if (lrcCorrectionTool ==
                                    LrcCorrectionTool.TWO_ANCHORS) "✓ " else "") +
                                    "Twee tijdankers")
                            }
                        }
                    }
                    if (!plainLines && lrcMode && chosenIndices != null &&
                        lrcCorrectionTool == LrcCorrectionTool.SHIFT_SELECTED) {
                        Text(
                            "Verschuif alleen de ${chosenIndices.size} bewust gekozen " +
                                "chronologische LRC-cues; andere begintijden en alle " +
                                "eigen woorden blijven ongewijzigd. Eindtijden volgen " +
                                "opnieuw uit de volgende begintijd. De globale " +
                                "[offset:]-header en metadata blijven staan.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = lrcSelectedShiftInput,
                            onValueChange = {
                                if (it.length <= 16) {
                                    lrcSelectedShiftInput = it
                                    lrcShiftPreviewPage = 0
                                }
                            },
                            label = { Text("Alleen geselecteerde regels ± milliseconden") },
                            placeholder = { Text("-750 of +1200 (maximaal ±30000)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (lrcSelectedShiftInput.isNotBlank()) {
                            if (lrcShiftProposal == null || lrcShiftAfter == null) {
                                Text(
                                    "Geen veilige verschuiving mogelijk. Kies ±1–30000 ms; " +
                                        "de nieuwe tijdcodes moeten uniek, oplopend en " +
                                        "exporteerbaar blijven. Er is niets aangepast.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                val after = lrcShiftAfter
                                val previewPages = maxOf(1, (parsed.size + 4) / 5)
                                val visiblePage = lrcShiftPreviewPage.coerceIn(0, previewPages - 1)
                                // A cue before a moved start can gain or lose end
                                // time, so navigate all changed starts AND ends.
                                val affectedPages = parsed.indices.filter { index ->
                                    val old = parsed[index]
                                    val now = after[index]
                                    old.startMillis != now.startMillis ||
                                        old.endMillis != now.endMillis
                                }.map { it / 5 }.distinct()
                                val previousChange = affectedPages.lastOrNull { it < visiblePage }
                                val nextChange = affectedPages.firstOrNull { it > visiblePage }
                                Text(
                                    "Gewijzigde start- of eindtijden op " +
                                        "${affectedPages.size} van ${previewPages} pagina's. " +
                                        "Spring naar de volgende wijziging of blader " +
                                        "zelf door alle regels.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        enabled = previousChange != null,
                                        onClick = {
                                            previousChange?.let { lrcShiftPreviewPage = it }
                                        }
                                    ) { Text("← Vorige wijziging") }
                                    OutlinedButton(
                                        enabled = nextChange != null,
                                        onClick = {
                                            nextChange?.let { lrcShiftPreviewPage = it }
                                        }
                                    ) { Text("Volgende wijziging →") }
                                }
                                OutlinedButton(
                                    enabled = affectedPages.isNotEmpty() &&
                                        visiblePage != affectedPages.first(),
                                    onClick = {
                                        affectedPages.firstOrNull()?.let {
                                            lrcShiftPreviewPage = it
                                        }
                                    }
                                ) { Text("Ga naar eerste gewijzigde regel") }
                                Text(
                                    "Volledige oude → nieuwe tijdlijn: " +
                                        "${parsed.size} regels, pagina " +
                                        "${visiblePage + 1}/${previewPages}. " +
                                        "Ook het nieuwe einde van aangrenzende regels " +
                                        "is zichtbaar; de eigen tekst blijft identiek.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                parsed.indices.drop(visiblePage * 5).take(5).forEach { index ->
                                    val old = parsed[index]
                                    val now = after[index]
                                    Text(
                                        "Regel ${index + 1}" +
                                            (if (index in chosenIndices) " · geselecteerd" else "") +
                                            "\n${old.startLabel} → ${old.endLabel}" +
                                            "\nwordt ${now.startLabel} → ${now.endLabel}" +
                                            "\n${old.text}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        enabled = visiblePage > 0,
                                        onClick = { lrcShiftPreviewPage = visiblePage - 1 }
                                    ) { Text("Vorige 5") }
                                    OutlinedButton(
                                        enabled = visiblePage + 1 < previewPages,
                                        onClick = { lrcShiftPreviewPage = visiblePage + 1 }
                                    ) { Text("Volgende 5") }
                                }
                                OutlinedButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        // The proposal is tied to the current RAM source and
                                        // exact selection; no provider file is overwritten.
                                        commitLrcTiming(lrcShiftProposal)
                                        lrcSelectedShiftInput = ""
                                        lrcShiftPreviewPage = 0
                                        page = 0
                                        documentNotice = "Geselecteerde LRC-begintijden " +
                                            "aangepast in het tijdelijke concept. " +
                                            "Het originele gekozen bestand is intact."
                                    }
                                ) {
                                    Text("Pas deze geselecteerde LRC-verschuiving bewust toe")
                                }
                            }
                        }
                    }
                    if (!plainLines && lrcMode &&
                        lrcCorrectionTool == LrcCorrectionTool.BULK_EXACT) {
                        Text(
                            "Meerdere eigen LRC-begintijden tegelijk corrigeren: " +
                                "schrijf bronnummer = exacte tijd, gescheiden door " +
                                "een puntkomma of een nieuwe regel. Voorbeeld: " +
                                "1=00:00:02.000;4=00:01:07.250. Maximaal 32 " +
                                "expliciete regels; geen automatische audio-uitlijning.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedButton(
                            enabled = chosenIndices != null &&
                                chosenIndices.isNotEmpty() &&
                                chosenIndices.size <= 32,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val indices = chosenIndices ?: return@OutlinedButton
                                val proposed = indices.joinToString(";") { index ->
                                    "${index + 1}=${parsed[index].startLabel}"
                                }
                                if (proposed.length <= 512) {
                                    lrcBatchInput = proposed
                                    lrcBatchPreviewPage = indices.first() / 5
                                    lrcBatchTooLarge = false
                                } else {
                                    lrcBatchTooLarge = true
                                }
                            }
                        ) { Text("Vul gekozen cues met huidige begintijden in") }
                        OutlinedTextField(
                            value = lrcBatchInput,
                            onValueChange = { text ->
                                if (text.length <= 512) {
                                    lrcBatchInput = text
                                    lrcBatchTooLarge = false
                                    lrcBatchPreviewPage = 0
                                } else lrcBatchTooLarge = true
                            },
                            label = { Text("Bronnummer=tijd; bronnummer=tijd (1–32)") },
                            minLines = 2,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (lrcBatchTooLarge) {
                            Text(
                                "Meer dan 512 tekens: de wijziging is geweigerd, " +
                                    "de eerdere invoer staat nog zichtbaar.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (lrcBatchInput.isNotBlank()) {
                            if (lrcBatchTooLarge ||
                                lrcBatchProposal == null || lrcBatchAfter == null) {
                                Text(
                                    "Nog geen geldige multi-cuecorrectie. " +
                                        "Gebruik unieke bestaande bronnummers, maximaal " +
                                        "32 tijdstempels, nieuwe tijden en een gehele " +
                                        "chronologische, exporteerbare uitkomst. " +
                                        "Geen eerdere tekst of tijd wordt gewijzigd.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                val after = lrcBatchAfter
                                val count = maxOf(1, (parsed.size + 4) / 5)
                                val visible = lrcBatchPreviewPage.coerceIn(0, count - 1)
                                val changed = parsed.indices.filter { index ->
                                    parsed[index].startMillis != after[index].startMillis ||
                                        parsed[index].endMillis != after[index].endMillis
                                }.map { it / 5 }.distinct()
                                Text(
                                    "Complete gecombineerde tijdlijn: pagina " +
                                        "${visible + 1}/${count}. Bekijk alle gewijzigde " +
                                        "én ongewijzigde regels vóór bevestiging.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        enabled = changed.any { it < visible },
                                        onClick = {
                                            changed.lastOrNull { it < visible }?.let {
                                                lrcBatchPreviewPage = it
                                            }
                                        }
                                    ) { Text("← Wijziging") }
                                    OutlinedButton(
                                        enabled = changed.any { it > visible },
                                        onClick = {
                                            changed.firstOrNull { it > visible }?.let {
                                                lrcBatchPreviewPage = it
                                            }
                                        }
                                    ) { Text("Wijziging →") }
                                }
                                parsed.indices.drop(visible * 5).take(5).forEach { index ->
                                    val old = parsed[index]
                                    val now = after[index]
                                    Text(
                                        "Regel ${index + 1}" +
                                            (if (old.startMillis != now.startMillis)
                                                " · aangepast" else "") +
                                            "\n${old.startLabel} → ${old.endLabel}" +
                                            "\nwordt ${now.startLabel} → ${now.endLabel}" +
                                            "\n${old.text}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        enabled = visible > 0,
                                        onClick = { lrcBatchPreviewPage = visible - 1 }
                                    ) { Text("Vorige 5") }
                                    OutlinedButton(
                                        enabled = visible + 1 < count,
                                        onClick = { lrcBatchPreviewPage = visible + 1 }
                                    ) { Text("Volgende 5") }
                                }
                                OutlinedButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        commitLrcTiming(lrcBatchProposal)
                                        lrcBatchInput = ""
                                        lrcBatchPreviewPage = 0
                                        lrcBatchTooLarge = false
                                        documentNotice = "Alle expliciete LRC-tijdcorrecties " +
                                            "samen toegepast op het tijdelijke concept. " +
                                            "De originele providerbron blijft intact."
                                    }
                                ) { Text("Bevestig alle exacte LRC-tijdcorrecties") }
                            }
                        }
                    }
                    if (!plainLines && lrcMode &&
                        lrcCorrectionTool == LrcCorrectionTool.SINGLE_EXACT) {
                        Text(
                            "Corrigeer één foutief LRC-begin exact: kies een " +
                                "chronologisch uitgewerkt origineel bronregelnummer " +
                                "en zijn gewenste absolute begintijd. Dit is geen " +
                                "globale verschuiving en geen nieuwe cue-invoer.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = chosenIndices?.isNotEmpty() == true,
                            onClick = {
                                val index = chosenIndices?.firstOrNull()
                                    ?: return@OutlinedButton
                                lrcExactRowInput = (index + 1).toString()
                                lrcExactTimeInput = parsed[index].startLabel
                                lrcExactPreviewPage = index / 5
                            }
                        ) {
                            Text("Vul eerste geselecteerde cue en zijn huidige tijd in")
                        }
                        OutlinedTextField(
                            value = lrcExactRowInput,
                            onValueChange = { value ->
                                if (value.length <= 4 && value.all { it in '0'..'9' }) {
                                    lrcExactRowInput = value
                                    lrcExactPreviewPage = 0
                                }
                            },
                            label = { Text("Exact bronregelnummer (1–${parsed.size})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = lrcExactTimeInput,
                            onValueChange = { value ->
                                if (value.length <= 13) {
                                    lrcExactTimeInput = value
                                    lrcExactPreviewPage = 0
                                }
                            },
                            label = { Text("Nieuwe begintijd (uu:mm:ss.mmm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (lrcExactRowInput.isNotBlank() ||
                            lrcExactTimeInput.isNotBlank()) {
                            if (lrcExactProposal == null || lrcExactAfter == null) {
                                Text(
                                    "Kies één bestaande cue en een gewijzigde, geldige tijd. " +
                                        "Omkeren, gelijke begintijden of een " +
                                        "onexporteerbare tijdlijn is niet toegestaan. " +
                                        "Geen wijziging toegepast.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                val after = lrcExactAfter
                                val pages = maxOf(1, (parsed.size + 4) / 5)
                                val shown = lrcExactPreviewPage.coerceIn(0, pages - 1)
                                val changedPages = parsed.indices.filter { index ->
                                    parsed[index].startMillis != after[index].startMillis ||
                                        parsed[index].endMillis != after[index].endMillis
                                }.map { it / 5 }.distinct()
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        enabled = shown != changedPages.firstOrNull(),
                                        onClick = {
                                            changedPages.firstOrNull()?.let {
                                                lrcExactPreviewPage = it
                                            }
                                        }
                                    ) { Text("Eerste verandering") }
                                    OutlinedButton(
                                        enabled = changedPages.any { it > shown },
                                        onClick = {
                                            changedPages.firstOrNull { it > shown }?.let {
                                                lrcExactPreviewPage = it
                                            }
                                        }
                                    ) { Text("Volgende verandering") }
                                }
                                Text(
                                    "Complete oude/nieuwe preview · pagina ${shown + 1}/${pages}. " +
                                        "Ook de vorige cue kan een andere eindtijd krijgen.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                parsed.indices.drop(shown * 5).take(5).forEach { index ->
                                    val old = parsed[index]
                                    val updated = after[index]
                                    Text(
                                        "Regel ${index + 1}" +
                                            (if (index == lrcExactRowIndex) " · gekozen" else "") +
                                            "\n${old.startLabel} → ${old.endLabel}" +
                                            "\nwordt ${updated.startLabel} → ${updated.endLabel}" +
                                            "\n${old.text}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        enabled = shown > 0,
                                        onClick = { lrcExactPreviewPage = shown - 1 }
                                    ) { Text("Vorige 5") }
                                    OutlinedButton(
                                        enabled = shown + 1 < pages,
                                        onClick = { lrcExactPreviewPage = shown + 1 }
                                    ) { Text("Volgende 5") }
                                }
                                OutlinedButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        commitLrcTiming(lrcExactProposal)
                                        lrcExactRowInput = ""
                                        lrcExactTimeInput = ""
                                        lrcExactPreviewPage = 0
                                        documentNotice = "Eén absolute cue-begintijd bewust " +
                                            "gecorrigeerd in het tijdelijke LRC-concept. " +
                                            "Het oorspronkelijke document is intact."
                                    }
                                ) { Text("Bevestig deze ene exacte LRC-tijdcorrectie") }
                            }
                        }
                    }
                    if (!plainLines && lrcMode && chosenIndices != null &&
                        lrcCorrectionTool == LrcCorrectionTool.TWO_ANCHORS) {
                        Text(
                            "Tweepuntscorrectie van een aaneengesloten LRC-reeks: " +
                                "kies eerst Van/Tot zonder losse gaten, en geef de " +
                                "nieuwe begintijd van de eerste en laatste cue op. " +
                                "De tussenliggende begintijden volgen alleen uit jouw " +
                                "twee ankers (0,5×–2× snelheid). Geen audioanalyse.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (!lrcAnchorIsContiguous) {
                            Text(
                                "Kies minstens twee opeenvolgende chronologische " +
                                    "regels met Van/Tot, of een gesloten reeks. " +
                                    "Losse niet-aangrenzende keuzes zijn voor de " +
                                    "tweepuntscorrectie niet toegestaan.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            val first = chosenIndices.first()
                            val last = chosenIndices.last()
                            Text(
                                "Anker A: bronregel ${first + 1}, huidig " +
                                    "${parsed[first].startLabel} — ${parsed[first].text}" +
                                    "\nAnker B: bronregel ${last + 1}, huidig " +
                                    "${parsed[last].startLabel} — ${parsed[last].text}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    lrcAnchorFirstInput = parsed[first].startLabel
                                    lrcAnchorLastInput = parsed[last].startLabel
                                    lrcAnchorPreviewPage = 0
                                }
                            ) { Text("Vul de twee huidige begintijden in") }
                            OutlinedTextField(
                                value = lrcAnchorFirstInput,
                                onValueChange = {
                                    if (it.length <= 13) {
                                        lrcAnchorFirstInput = it
                                        lrcAnchorPreviewPage = 0
                                    }
                                },
                                label = { Text("Nieuwe tijd anker A (uu:mm:ss.mmm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = lrcAnchorLastInput,
                                onValueChange = {
                                    if (it.length <= 13) {
                                        lrcAnchorLastInput = it
                                        lrcAnchorPreviewPage = 0
                                    }
                                },
                                label = { Text("Nieuwe tijd anker B (uu:mm:ss.mmm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (lrcAnchorFirstInput.isNotBlank() ||
                                lrcAnchorLastInput.isNotBlank()) {
                                if (lrcAnchorProposal == null || lrcAnchorAfter == null) {
                                    Text(
                                        "Nog geen veilige nieuwe tijdlijn. Vul beide tijden " +
                                            "expliciet in, met een later tweede anker, " +
                                            "0,5×–2× tijdverhouding en zonder overlap " +
                                            "met ongewijzigde cues erbuiten. " +
                                            "Ongewijzigde of niet-exporteerbare " +
                                            "voorstellen worden geweigerd.",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else {
                                    val after = lrcAnchorAfter
                                    val pages = maxOf(1, (parsed.size + 4) / 5)
                                    val visible = lrcAnchorPreviewPage.coerceIn(0, pages - 1)
                                    val changedPages = parsed.indices.filter { index ->
                                        parsed[index].startMillis != after[index].startMillis ||
                                            parsed[index].endMillis != after[index].endMillis
                                    }.map { it / 5 }.distinct()
                                    val previous = changedPages.lastOrNull { it < visible }
                                    val next = changedPages.firstOrNull { it > visible }
                                    Text(
                                        "Bekijk alle originele en gecorrigeerde " +
                                            "tijdcodes: pagina ${visible + 1}/${pages}. " +
                                            "Ook aangrenzende eindtijden worden getoond.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            enabled = previous != null,
                                            onClick = { previous?.let { lrcAnchorPreviewPage = it } }
                                        ) { Text("← Vorige wijziging") }
                                        OutlinedButton(
                                            enabled = next != null,
                                            onClick = { next?.let { lrcAnchorPreviewPage = it } }
                                        ) { Text("Volgende wijziging →") }
                                    }
                                    parsed.indices.drop(visible * 5).take(5).forEach { index ->
                                        val before = parsed[index]
                                        val changed = after[index]
                                        Text(
                                            "Regel ${index + 1}" +
                                                (if (index in first..last) " · binnen ankers" else "") +
                                                "\n${before.startLabel} → ${before.endLabel}" +
                                                "\nwordt ${changed.startLabel} → ${changed.endLabel}" +
                                                "\n${before.text}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            enabled = visible > 0,
                                            onClick = { lrcAnchorPreviewPage = visible - 1 }
                                        ) { Text("Vorige 5") }
                                        OutlinedButton(
                                            enabled = visible + 1 < pages,
                                            onClick = { lrcAnchorPreviewPage = visible + 1 }
                                        ) { Text("Volgende 5") }
                                    }
                                    OutlinedButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            commitLrcTiming(lrcAnchorProposal)
                                            lrcAnchorFirstInput = ""
                                            lrcAnchorLastInput = ""
                                            lrcAnchorPreviewPage = 0
                                            documentNotice = "Tweepuntscorrectie alleen in " +
                                                "het tijdelijke LRC-concept toegepast. " +
                                                "Het gekozen bronbestand is intact."
                                        }
                                    ) {
                                        Text("Bevestig deze volledige tweepuntscorrectie")
                                    }
                                }
                            }
                        }
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
                    ?: PrivateSbvSubtitleReader.read(raw)
                    ?: PrivateYoutubeJson3SubtitleReader.read(raw.takeIf { it.length <= 65_536 } ?: "")
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
