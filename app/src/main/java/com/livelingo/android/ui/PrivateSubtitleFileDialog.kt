package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.livelingo.android.PrivateSubtitleBatchReplace
import com.livelingo.android.PrivateSubtitleBulkRemoval
import com.livelingo.android.PrivateSubtitleRowSelection
import com.livelingo.android.PrivateSubtitleTimeWindowRows
import com.livelingo.android.SubtitleReplaceField
import com.livelingo.android.PrivateSubtitleLineWrap
import com.livelingo.android.PrivateSubtitleClip
import com.livelingo.android.PrivateSubtitleCue
import com.livelingo.android.PrivateSubtitleDocumentCombine
import com.livelingo.android.SubtitleManualPair
import com.livelingo.android.PrivateSubtitleManualPairSelection
import com.livelingo.android.PrivateSubtitleOffset
import com.livelingo.android.PrivateSubtitleTrackAnchorOffset
import com.livelingo.android.PrivateSubtitleTrackDriftAlignment
import com.livelingo.android.PrivateSubtitleTrackMultiAnchorAlignment
import com.livelingo.android.PrivateSubtitleMultiAnchor
import com.livelingo.android.PrivateSubtitleDriftSelection
import com.livelingo.android.SubtitleCombineMode
import com.livelingo.android.PrivateSubtitleDraftEditor
import com.livelingo.android.PrivateLrcSubtitleExport
import com.livelingo.android.PrivateTimedSubtitleSheet
import com.livelingo.android.TimedSubtitleSheetFormat
import com.livelingo.android.PrivateSubtitleTimelineNavigation
import com.livelingo.android.SubtitleDraftFormat
import com.livelingo.android.TargetCatalog
import com.livelingo.android.LocalCaptionSpeaker
import com.livelingo.android.LocalSpeechPace

private data class PendingSubtitleImport(
    val existing: List<PrivateSubtitleCue>,
    val imported: List<PrivateSubtitleCue>
)

/** Explicitly opened, ephemeral subtitle document editor, never the live transcript. */
@Composable
internal fun PrivateSubtitleFileDialog(
    cues: List<PrivateSubtitleCue>,
    onDismiss: () -> Unit,
    onChooseAudio: (List<PrivateSubtitleCue>) -> Unit,
    onChooseSecondFile: (List<PrivateSubtitleCue>) -> Unit,
    pendingSecondFile: Pair<List<PrivateSubtitleCue>, List<PrivateSubtitleCue>>?,
    secondFileError: String?,
    onSecondFileReceived: () -> Unit,
    worksheetNotice: String?,
    onWorksheetNoticeRead: () -> Unit,
    saveCopyNotice: String?,
    onSaveCopyNoticeRead: () -> Unit,
    onSaveWorksheetCopy: (List<PrivateSubtitleCue>, List<PrivateSubtitleCue>) -> Unit,
    onSaveCopy: (List<PrivateSubtitleCue>, List<PrivateSubtitleCue>, SubtitleDraftFormat) -> Unit,
    onSaveTimedSheet: (List<PrivateSubtitleCue>, List<PrivateSubtitleCue>, TimedSubtitleSheetFormat) -> Unit,
    onSaveLrc: (List<PrivateSubtitleCue>, List<PrivateSubtitleCue>) -> Unit,
    incomingSecondPaste: SubtitlePasteFileContent?,
    onChoosePasteDocument: (List<PrivateSubtitleCue>, SubtitlePasteFileContent) -> Unit,
    onSecondPasteConsumed: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var jumpToTime by remember { mutableStateOf("") }
    var shownIndex by remember { mutableIntStateOf(-1) }
    var large by remember { mutableStateOf(true) }
    var draft by remember(cues) { mutableStateOf(cues) }
    var edited by remember(cues) { mutableStateOf(false) }
    var undoHistory by remember(cues) { mutableStateOf(emptyList<List<PrivateSubtitleCue>>()) }
    var redoHistory by remember(cues) { mutableStateOf(emptyList<List<PrivateSubtitleCue>>()) }
    var editIndex by remember { mutableStateOf<Int?>(null) }
    var exactTimeIndex by remember { mutableStateOf<Int?>(null) }
    var deleteIndex by remember { mutableStateOf<Int?>(null) }
    var splitIndex by remember { mutableStateOf<Int?>(null) }
    var mergeIndex by remember { mutableStateOf<Int?>(null) }
    var splitFirst by remember { mutableStateOf("") }
    var splitSecond by remember { mutableStateOf("") }
    var editValue by remember { mutableStateOf("") }
    var editSourceLine by remember { mutableStateOf("") }
    var editMeaningLine by remember { mutableStateOf("") }
    var editAsPairedLines by remember { mutableStateOf(false) }
    var editError by remember { mutableStateOf("") }
    var requestSave by remember { mutableStateOf<SubtitleDraftFormat?>(null) }
    var requestTimedSheet by remember { mutableStateOf<TimedSubtitleSheetFormat?>(null) }
    var requestLrcSave by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var batchOpen by remember { mutableStateOf(false) }
    var removeOpen by remember { mutableStateOf(false) }
    var removeRows by remember { mutableStateOf("") }
    var removeWindowStart by remember { mutableStateOf("") }
    var removeWindowEnd by remember { mutableStateOf("") }
    var removeWindowRanges by remember { mutableStateOf("") }
    var removeReviewPage by remember { mutableIntStateOf(0) }
    var wrapOpen by remember { mutableStateOf(false) }
    var wrapWidth by remember { mutableIntStateOf(42) }
    var batchFind by remember { mutableStateOf("") }
    var batchWith by remember { mutableStateOf("") }
    var batchMatchCase by remember { mutableStateOf(false) }
    var batchField by remember { mutableStateOf(SubtitleReplaceField.ALL_TEXT) }
    var batchReviewPage by remember { mutableIntStateOf(0) }
    var batchFirstRow by remember { mutableStateOf("1") }
    var batchLastRow by remember { mutableStateOf("") }
    var batchExplicitRows by remember { mutableStateOf("") }
    var batchWindowStart by remember { mutableStateOf("") }
    var batchWindowEnd by remember { mutableStateOf("") }
    var batchWindowRanges by remember { mutableStateOf("") }
    var insertOpen by remember { mutableStateOf(false) }
    var insertStart by remember { mutableStateOf("") }
    var insertEnd by remember { mutableStateOf("") }
    var insertContent by remember { mutableStateOf("") }
    var clipOpen by remember { mutableStateOf(false) }
    var clipStart by remember { mutableStateOf("") }
    var clipEnd by remember { mutableStateOf("") }
    var excerptOpen by remember { mutableStateOf(false) }
    var excerptSourceRows by remember { mutableStateOf<List<Int>>(emptyList()) }
    var practiceOpen by remember { mutableStateOf(false) }
    var retimingOpen by remember { mutableStateOf(false) }
    var selectiveRetimingOpen by remember { mutableStateOf(false) }
    var inspectorOpen by remember { mutableStateOf(false) }
    var twoAnchorOpen by remember { mutableStateOf(false) }
    var practiceCues by remember { mutableStateOf<List<PrivateSubtitleCue>>(emptyList()) }
    var pendingImport by remember { mutableStateOf<PendingSubtitleImport?>(null) }
    var pasteSecondOpen by remember { mutableStateOf(false) }
    var importMode by remember { mutableStateOf(SubtitleCombineMode.APPEND_AFTER_LAST) }
    var secondTrackOffsetMs by remember { mutableStateOf(0L) }
    var secondTrackCustomOffset by remember { mutableStateOf("") }
    var primaryAnchorRow by remember { mutableStateOf("1") }
    var secondaryAnchorRow by remember { mutableStateOf("1") }
    var primaryAnchorPage by remember { mutableIntStateOf(0) }
    var secondaryAnchorPage by remember { mutableIntStateOf(0) }
    var anchorSelectionNeedsApply by remember { mutableStateOf(false) }
    var secondTrackPreviewPage by remember { mutableIntStateOf(0) }
    var pairReviewPage by remember { mutableIntStateOf(0) }
    var excludedReviewPage by remember { mutableIntStateOf(0) }
    var pairReviewOpen by remember { mutableStateOf(false) }
    var unmatchedReviewOpen by remember { mutableStateOf(false) }
    var unmatchedReviewPage by remember { mutableIntStateOf(0) }
    var unmatchedReviewQuery by remember { mutableStateOf("") }
    var unmatchedReviewTrack by remember { mutableIntStateOf(0) }
    var resultPreviewJump by remember { mutableStateOf("") }
    var pairReviewQuery by remember { mutableStateOf("") }
    var pairReviewSecondaryRow by remember { mutableStateOf("") }
    var excludedSecondRows by remember { mutableStateOf(setOf<Int>()) }
    var manualPairs by remember { mutableStateOf<List<SubtitleManualPair>>(emptyList()) }
    var manualFirstRow by remember { mutableStateOf("") }
    var manualSecondRow by remember { mutableStateOf("") }
    var manualReviewPage by remember { mutableIntStateOf(0) }
    var manualBulkExpression by remember { mutableStateOf("") }
    var manualBulkPage by remember { mutableIntStateOf(0) }
    var excludeRowExpression by remember { mutableStateOf("") }
    var excludeWindowExpression by remember { mutableStateOf("") }
    var driftSelectorOpen by remember { mutableStateOf(false) }
    var selectedDrift by remember { mutableStateOf<PrivateSubtitleDriftSelection?>(null) }
    var multiAnchorSelectorOpen by remember { mutableStateOf(false) }
    var selectedMultiAnchors by remember {
        mutableStateOf<List<PrivateSubtitleMultiAnchor>?>(null)
    }
    var offsetChoicesOpen by remember { mutableStateOf(false) }
    val found = draft.mapIndexed { idx, row -> idx to row }
        .filter { (_, row) -> row.text.contains(query.trim(), ignoreCase = true) }
    val current = found.getOrNull(shownIndex)?.second
    val context = LocalContext.current
    // MainActivity survives Android's external document picker; this editor
    // is intentionally cleared on ON_STOP. Restore only the user-selected
    // bounded draft plus a parsed second source after that chooser returns.
    LaunchedEffect(pendingSecondFile, secondFileError) {
        if (pendingSecondFile != null) {
            val (previous, imported) = pendingSecondFile
            if (draft == previous) {
                pendingImport = PendingSubtitleImport(previous.toList(), imported.toList())
                importMode = SubtitleCombineMode.APPEND_AFTER_LAST
                secondTrackOffsetMs = 0L
                secondTrackCustomOffset = ""
                primaryAnchorRow = "1"
                secondaryAnchorRow = "1"
                primaryAnchorPage = 0
                secondaryAnchorPage = 0
                anchorSelectionNeedsApply = false
                secondTrackPreviewPage = 0
                driftSelectorOpen = false
                selectedDrift = null
                multiAnchorSelectorOpen = false
                selectedMultiAnchors = null
                excludedSecondRows = emptySet()
                manualPairs = emptyList()
                manualFirstRow = ""
                manualSecondRow = ""
                manualReviewPage = 0
                manualBulkExpression = ""
                manualBulkPage = 0
                excludeRowExpression = ""
                excludeWindowExpression = ""
                excludedReviewPage = 0
                pairReviewPage = 0
                pairReviewOpen = false
                unmatchedReviewOpen = false
                unmatchedReviewPage = 0
                pairReviewQuery = ""
                pairReviewSecondaryRow = ""
                unmatchedReviewQuery = ""
                unmatchedReviewTrack = 0
                resultPreviewJump = ""
                notice = ""
            } else {
                notice = "Het concept veranderde tijdens de bestandskeuze. Kies opnieuw."
            }
            onSecondFileReceived()
        } else if (secondFileError != null) {
            notice = secondFileError
            onSecondFileReceived()
        }
    }
    LaunchedEffect(worksheetNotice) {
        if (worksheetNotice != null) {
            notice = worksheetNotice
            onWorksheetNoticeRead()
        }
    }
    LaunchedEffect(saveCopyNotice) {
        if (saveCopyNotice != null) {
            notice = saveCopyNotice
            onSaveCopyNoticeRead()
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val speaker = remember(context) { LocalCaptionSpeaker(context) }
    var chosenVoiceLanguage by remember { mutableStateOf<String?>(null) }
    var voicePickerOpen by remember { mutableStateOf(false) }
    var spokenNotice by remember { mutableStateOf("") }
    DisposableEffect(speaker, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_PAUSE)
                speaker.stop()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); speaker.close() }
    }
    LaunchedEffect(current?.text, current?.startMillis, current?.endMillis) {
        speaker.stop()
        spokenNotice = ""
    }
    fun pronounceCue(slow: Boolean) {
        val lang = chosenVoiceLanguage ?: return
        val safe = current?.text?.replace('\n', ' ')?.trim().orEmpty()
        if (safe.isBlank() || safe.length > 400) {
            spokenNotice = "Alleen geselecteerde tekst tot 400 tekens kan lokaal worden voorgelezen."
        } else speaker.speak(safe, lang,
            if (slow) LocalSpeechPace.SLOW else LocalSpeechPace.NORMAL) {
            spokenNotice = it
        }
    }

    fun applyChange(next: List<PrivateSubtitleCue>) {
        if (next == draft) return
        undoHistory = (undoHistory + listOf(draft.toList())).takeLast(20)
        redoHistory = emptyList()
        draft = next
        edited = draft != cues
    }

    fun undoChange() {
        if (undoHistory.isEmpty()) return
        val previous = undoHistory.last()
        undoHistory = undoHistory.dropLast(1)
        redoHistory = (redoHistory + listOf(draft.toList())).takeLast(20)
        draft = previous
        edited = draft != cues
        shownIndex = -1
        notice = "Laatste wijziging teruggedraaid; origineel ongewijzigd."
    }

    fun redoChange() {
        if (redoHistory.isEmpty()) return
        val next = redoHistory.last()
        redoHistory = redoHistory.dropLast(1)
        undoHistory = (undoHistory + listOf(draft.toList())).takeLast(20)
        draft = next
        edited = draft != cues
        shownIndex = -1
        notice = "Bewerking opnieuw toegepast in tijdelijke kopie."
    }

    fun nudgeSelected(start: Long, end: Long) {
        val selected = found.getOrNull(shownIndex)?.first ?: return
        val updated = PrivateSubtitleDraftEditor.adjustCue(draft, selected, start, end)
        if (updated == null) notice = "Tijd geweigerd: begin/eind of volgorde ongeldig."
        else { applyChange(updated); notice = "Tijd van deze regel aangepast." }
    }

    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false,
            dismissOnClickOutside = false)) {
        Surface(modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxSize().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Eigen ondertitelconcept · SRT/WebVTT",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Sluiten") }
                }
                Text("Alle tekst en tijdcodes komen uit je gekozen bestand. " +
                    "Geen geverifieerde vertaling of automatische audio-synchronisatie.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = query, onValueChange = {
                    query = it.take(100)
                    shownIndex = -1
                }, label = { Text("Zoek in dit document") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = jumpToTime,
                        onValueChange = { jumpToTime = it.take(13) },
                        label = { Text("Ga naar tijd · mm:ss") },
                        singleLine = true,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        val target = PrivateSubtitleTimelineNavigation.nearest(draft, jumpToTime)
                        if (target == null) notice = "Tijd ongeldig. Voorbeeld: 01:23 of 00:01:23.500."
                        else {
                            query = ""
                            shownIndex = target
                            notice = "Dichtstbijzijnde cue op of na de gekozen tijd geopend."
                        }
                    }) { Text("Spring →") }
                }
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${found.size} van ${draft.size} regels" +
                        (if (edited) " · niet bewaard" else ""),
                        style = MaterialTheme.typography.labelMedium)
                    PrivateSubtitleDocumentMenu(
                        canInsert = draft.size < 500,
                        canRestore = edited,
                        onFindReplace = {
                            batchFind = query.take(100)
                            batchOpen = true
                        },
                        onWrapLines = { wrapWidth = 42; wrapOpen = true },
                        onShiftEarlier = {
                            val result = PrivateSubtitleDraftEditor.shift(draft, -500L)
                            if (result == null) notice = "Kan niet eerder: negatieve tijdcode."
                            else {
                                applyChange(result)
                                notice = "Alle tijden 0,5 s eerder."
                            }
                        },
                        onShiftLater = {
                            val result = PrivateSubtitleDraftEditor.shift(draft, 500L)
                            if (result == null) notice = "Kan niet later: tijdgrens bereikt."
                            else {
                                applyChange(result)
                                notice = "Alle tijden 0,5 s later."
                            }
                        },
                        onRestore = {
                            draft = cues
                            edited = false
                            undoHistory = emptyList()
                            redoHistory = emptyList()
                            shownIndex = -1
                            notice = "Origineel hersteld; tijdelijke bewerkingen weggegooid."
                        },
                        onInsert = {
                            insertStart = current?.endLabel?.replace(',', '.') ?: "00:00:00.000"
                            insertEnd = ""
                            insertContent = ""
                            insertOpen = true
                        },
                        onClip = {
                            clipStart = "00:00:00.000"
                            clipEnd = draft.lastOrNull()?.endLabel?.replace(',', '.') ?: ""
                            clipOpen = true
                        }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = undoHistory.isNotEmpty(),
                        onClick = { undoChange() }) {
                        Text("↶ Terug (${undoHistory.size})")
                    }
                    OutlinedButton(enabled = redoHistory.isNotEmpty(),
                        onClick = { redoChange() }) {
                        Text("↷ Opnieuw (${redoHistory.size})")
                    }
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = found.isNotEmpty(),
                    onClick = {
                        practiceCues = found.map { it.second }.take(24).toList()
                        practiceOpen = true
                    }
                ) { Text("▣ Oefen deze gevonden ondertitelregels…") }
                PrivateSubtitleWorksheetExportAction(
                    selected = found.map { it.second }.take(24),
                    onExport = { chosen -> onSaveWorksheetCopy(draft.toList(), chosen) }
                )
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = found.isNotEmpty() && query.isNotBlank(),
                    onClick = {
                        excerptSourceRows = found.map { it.first + 1 }
                        excerptOpen = true
                    }
                ) { Text("↳ Bewaar gevonden regels als SRT, VTT, CSV, TSV of LRC…") }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = draft.isNotEmpty(),
                    onClick = {
                        excerptSourceRows = (1..draft.size).toList()
                        excerptOpen = true
                    }
                ) { Text("↳ Kies zelf bronregels om te bewaren…") }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = draft.size > 1,
                    onClick = {
                        removeRows = ""
                        removeWindowStart = ""
                        removeWindowEnd = ""
                        removeReviewPage = 0
                        removeOpen = true
                    }
                ) { Text("✂ Meerdere eigen ondertitelregels verwijderen…") }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { retimingOpen = true }
                ) { Text("⏱ Tijdcodes van alle regels aanpassen…") }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { selectiveRetimingOpen = true }
                ) { Text("⏱ Alleen gekozen bronregels in tijd verschuiven…") }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { inspectorOpen = true }
                ) { Text("⌁ Bekijk de ondertiteltijdlijn…") }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        speaker.stop()
                        onChooseAudio(draft.toList())
                    }
                ) { Text("▶ Eigen audio met tijdcodes afspelen…") }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = draft.size >= 2,
                    onClick = { twoAnchorOpen = true }
                ) { Text("⌁ Tijdlijn uitlijnen met twee ankers…") }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = draft.size < 500,
                    onClick = {
                        notice = ""
                        // Android's external picker is Activity-owned to survive
                        // ON_STOP; parser and merge preview remain fail-closed.
                        onChooseSecondFile(draft.toList())
                    }
                ) { Text("＋ Tweede SRT/VTT-document toevoegen…") }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = draft.size < 500,
                    onClick = { pasteSecondOpen = true }
                ) { Text("＋ Tweede ondertitelspoor zelf plakken…") }
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = { requestSave = SubtitleDraftFormat.SRT }) {
                        Text("Bewaar kopie SRT…")
                    }
                    OutlinedButton(onClick = { requestSave = SubtitleDraftFormat.WEBVTT }) {
                        Text("Bewaar kopie VTT…")
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(modifier = Modifier.weight(1f),
                        onClick = { requestTimedSheet = TimedSubtitleSheetFormat.CSV }) {
                        Text("Bewaar tijdcodes als CSV…")
                    }
                    OutlinedButton(modifier = Modifier.weight(1f),
                        onClick = { requestTimedSheet = TimedSubtitleSheetFormat.TSV }) {
                        Text("Bewaar tijdcodes als TSV…")
                    }
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { requestLrcSave = true }
                ) { Text("Bewaar eigen begintijden als LRC…") }
                if (notice.isNotBlank()) Text(notice,
                    style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                if (current == null) {
                    LazyColumn(modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        itemsIndexed(found) { index, (_, cue) ->
                            Column {
                                TextButton(onClick = { shownIndex = index }) {
                                    Text("${index + 1}. ${cue.startLabel} → ${cue.endLabel} · Open")
                                }
                                Text(cue.text,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        textDirection = TextDirection.ContentOrRtl),
                                    maxLines = 2)
                                HorizontalDivider()
                            }
                        }
                    }
                } else {
                    Text("${current.startLabel} → ${current.endLabel}",
                        style = MaterialTheme.typography.labelLarge)
                    Box(modifier = Modifier.fillMaxWidth().weight(1f)
                        .verticalScroll(rememberScrollState())) {
                        OfflineWordText(
                            text = current.text,
                            language = chosenVoiceLanguage,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                textDirection = TextDirection.ContentOrRtl,
                                fontSize = if (large) 30.sp else 22.sp,
                                lineHeight = if (large) 40.sp else 30.sp),
                            allowSpeech = false
                        )
                    }
                    Text("Tik een woord voor een beperkte lokale woordenkaart. " +
                        "Kies zelf de teksttaal; geen internetwoordenboek of " +
                        "geverifieerde betekenis.",
                        style = MaterialTheme.typography.labelSmall)
                    Text("Woordkaart en uitspraak · kies de teksttaal zelf",
                        style = MaterialTheme.typography.labelSmall)
                    Box {
                        TextButton(onClick = { voicePickerOpen = true }) {
                            val label = TargetCatalog.targets.firstOrNull {
                                it.code == chosenVoiceLanguage
                            }?.title ?: "Kies eerst taal voor lokaal voorlezen…"
                            Text(label)
                        }
                        DropdownMenu(
                            expanded = voicePickerOpen,
                            onDismissRequest = { voicePickerOpen = false }
                        ) {
                            TargetCatalog.targets.forEach { language ->
                                DropdownMenuItem(
                                    text = { Text(language.title) },
                                    onClick = {
                                        speaker.stop()
                                        chosenVoiceLanguage = language.code
                                        spokenNotice = ""
                                        voicePickerOpen = false
                                    }
                                )
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(enabled = chosenVoiceLanguage != null &&
                            current.text.length <= 400,
                            onClick = { pronounceCue(false) }) { Text("🔊 Hoor lokaal") }
                        TextButton(enabled = chosenVoiceLanguage != null &&
                            current.text.length <= 400,
                            onClick = { pronounceCue(true) }) { Text("Rustig") }
                        TextButton(onClick = {
                            speaker.stop()
                            spokenNotice = "Uitspraak gestopt."
                        }) { Text("■ Stop") }
                    }
                    if (spokenNotice.isNotBlank()) {
                        Text(spokenNotice, style = MaterialTheme.typography.bodySmall)
                    }
                    PrivateSubtitleCueMenu(
                        canSplit = draft.size < 500,
                        canMerge = found[shownIndex].first < draft.lastIndex,
                        canDelete = draft.size > 1,
                        onCorrectText = {
                            editError = ""
                            editIndex = found[shownIndex].first
                            editValue = current.text
                            val lines = current.text.lines()
                            editAsPairedLines = lines.size == 2
                            editSourceLine = lines.first()
                            editMeaningLine = lines.getOrNull(1).orEmpty()
                        },
                        onExactTime = { exactTimeIndex = found[shownIndex].first },
                        onStartEarlier = { nudgeSelected(-500L, 0L) },
                        onStartLater = { nudgeSelected(500L, 0L) },
                        onEndEarlier = { nudgeSelected(0L, -500L) },
                        onEndLater = { nudgeSelected(0L, 500L) },
                        onSplit = {
                            val index = found[shownIndex].first
                            val cut = current.text.indexOf(' ', current.text.length / 2)
                            splitFirst = if (cut > 0) current.text.substring(0, cut) else current.text
                            splitSecond = if (cut > 0) current.text.substring(cut + 1) else ""
                            splitIndex = index
                        },
                        onMerge = { mergeIndex = found[shownIndex].first },
                        onDelete = { deleteIndex = found[shownIndex].first }
                    )
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(enabled = shownIndex > 0,
                            onClick = { shownIndex-- }) { Text("← Vorige") }
                        TextButton(onClick = { large = !large }) {
                            Text(if (large) "Aa kleiner" else "Aa groter")
                        }
                        TextButton(enabled = shownIndex < found.lastIndex,
                            onClick = { shownIndex++ }) { Text("Volgende →") }
                        TextButton(onClick = { shownIndex = -1 }) { Text("Lijst") }
                    }
                }
                Text("Deze bewerkingen blijven in RAM totdat je bewust een nieuw " +
                    "bestand bewaart. Het geopende origineel wordt nooit overschreven. " +
                    "Android-bestandsproviders kunnen extern synchroniseren.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (excerptOpen) {
        PrivateSubtitleExcerptDialog(
            allCues = draft.toList(),
            initialSourceRows = excerptSourceRows,
            // Export ONLY selected rows, but retain the full authored editor
            // timeline across Android's external DocumentsUI/ON_STOP.
            onSaveCopy = { exportCues, format ->
                onSaveCopy(draft.toList(), exportCues.toList(), format)
            },
            onSaveTimedSheet = { exportCues, format ->
                onSaveTimedSheet(draft.toList(), exportCues.toList(), format)
            },
            onSaveLrc = { exportCues ->
                onSaveLrc(draft.toList(), exportCues.toList())
            },
            onDismiss = { excerptOpen = false }
        )
    }

    if (twoAnchorOpen) {
        SubtitleTwoAnchorTimelineDialog(
            draft = draft,
            onApply = { updated ->
                applyChange(updated)
                shownIndex = -1
                notice = "Twee handmatige tijdankers toegepast. Herstel via Terug."
                twoAnchorOpen = false
            },
            onDismiss = { twoAnchorOpen = false }
        )
    }

    if (inspectorOpen) {
        SubtitleTimelineInspectorDialog(
            cues = draft,
            onSelectCue = { selected ->
                query = ""
                shownIndex = selected
                notice = "Regel " + (selected + 1) + " geopend vanuit tijdlijnoverzicht."
                inspectorOpen = false
            },
            onDismiss = { inspectorOpen = false }
        )
    }

    if (selectiveRetimingOpen) {
        SubtitleScopedShiftDialog(
            draft = draft,
            foundSourceRows = if (query.isNotBlank())
                found.map { it.first + 1 } else emptyList(),
            onApply = { updated ->
                applyChange(updated)
                shownIndex = -1
                notice = "Gekozen oorspronkelijke regels verschoven; " +
                    "Terug herstelt alle tijdcodes in één stap."
                selectiveRetimingOpen = false
            },
            onDismiss = { selectiveRetimingOpen = false }
        )
    }
    if (retimingOpen) {
        SubtitleRetimingDialog(
            draft = draft,
            onApply = { updated ->
                applyChange(updated)
                shownIndex = -1
                notice = "Tijdlijn aangepast. Je kunt dit terugdraaien met Terug."
                retimingOpen = false
            },
            onDismiss = { retimingOpen = false }
        )
    }

    if (practiceOpen) {
        OfflineSubtitleRecallDialog(
            selected = practiceCues,
            onDismiss = { practiceOpen = false }
        )
    }

    if (pasteSecondOpen || incomingSecondPaste != null) {
        SubtitlePasteDialog(
            title = "Tweede SRT/VTT-spoor zelf plakken",
            initialDocument = incomingSecondPaste,
            onChooseDocument = { request ->
                onChoosePasteDocument(draft.toList(), request)
                pasteSecondOpen = false
                onSecondPasteConsumed()
            },
            onDismiss = {
                pasteSecondOpen = false
                onSecondPasteConsumed()
            },
            onAccept = { imported ->
                pendingImport = PendingSubtitleImport(draft.toList(), imported.toList())
                importMode = SubtitleCombineMode.APPEND_AFTER_LAST
                secondTrackOffsetMs = 0L
                secondTrackCustomOffset = ""
                primaryAnchorRow = "1"
                secondaryAnchorRow = "1"
                primaryAnchorPage = 0
                secondaryAnchorPage = 0
                anchorSelectionNeedsApply = false
                secondTrackPreviewPage = 0
                driftSelectorOpen = false
                selectedDrift = null
                multiAnchorSelectorOpen = false
                selectedMultiAnchors = null
                excludedSecondRows = emptySet()
                manualPairs = emptyList()
                manualFirstRow = ""
                manualSecondRow = ""
                manualReviewPage = 0
                manualBulkExpression = ""
                manualBulkPage = 0
                excludeRowExpression = ""
                excludeWindowExpression = ""
                excludedReviewPage = 0
                pairReviewPage = 0
                pairReviewOpen = false
                unmatchedReviewOpen = false
                unmatchedReviewPage = 0
                pairReviewQuery = ""
                pairReviewSecondaryRow = ""
                unmatchedReviewQuery = ""
                unmatchedReviewTrack = 0
                resultPreviewJump = ""
                notice = ""
                pasteSecondOpen = false
                onSecondPasteConsumed()
            }
        )
    }
    val stagedImport = pendingImport
    if (stagedImport != null) {
        val stillCurrent = draft == stagedImport.existing
        // Computing full-document drift and 500×500 chronological pairing
        // is bounded but not cheap. Cache while the actual authored sources,
        // selection, or merge mode remain identical; paging/scrolling must
        // not recompute this work.
        val drift = remember(draft, stagedImport.imported, selectedDrift,
            importMode, stillCurrent) {
            selectedDrift?.takeIf {
                importMode == SubtitleCombineMode.PAIR_OVERLAPPING_TIMES && stillCurrent
            }?.let {
                PrivateSubtitleTrackDriftAlignment.preview(
                    draft, stagedImport.imported,
                    it.firstPrimaryRow, it.firstSecondaryRow,
                    it.lastPrimaryRow, it.lastSecondaryRow)
            }
        }
        val selectedPairOffset = when {
            importMode != SubtitleCombineMode.PAIR_OVERLAPPING_TIMES -> secondTrackOffsetMs
            selectedDrift != null -> if (drift != null) 0L else null
            anchorSelectionNeedsApply -> null
            secondTrackCustomOffset.isBlank() -> secondTrackOffsetMs
            else -> PrivateSubtitleOffset.parseSeconds(
                secondTrackCustomOffset, allowZero = true)
        }
        val proposedIncoming = if (
            selectedDrift != null && importMode == SubtitleCombineMode.PAIR_OVERLAPPING_TIMES
        ) drift?.adjustedSecondary else stagedImport.imported
        val proposed = remember(draft, proposedIncoming,
            importMode, selectedPairOffset, stillCurrent, excludedSecondRows,
            manualPairs) {
            if (stillCurrent && selectedPairOffset != null &&
                proposedIncoming != null
            ) PrivateSubtitleDocumentCombine.preview(
                draft, proposedIncoming, importMode,
                selectedPairOffset,
                if (importMode == SubtitleCombineMode.PAIR_OVERLAPPING_TIMES)
                    excludedSecondRows else emptySet(),
                if (importMode == SubtitleCombineMode.PAIR_OVERLAPPING_TIMES)
                    manualPairs else emptyList()
            ) else null
        }
        val previewRows = proposed?.cues.orEmpty()
        val previewPages = maxOf(1, (previewRows.size + 4) / 5)
        val shownPairPage = secondTrackPreviewPage.coerceIn(0, previewPages - 1)
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Tweede ondertitelspoor toevoegen") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(
                        "Tweede spoor: ${stagedImport.imported.size} ondertitelregels. " +
                            "Huidig concept: ${draft.size} regels. Alleen tijdelijk in " +
                            "het geheugen; niets overschrijft je originele bestanden.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { importMode = SubtitleCombineMode.APPEND_AFTER_LAST }
                    ) {
                        Text((if (importMode == SubtitleCombineMode.APPEND_AFTER_LAST)
                            "✓ " else "") + "Aansluiten na laatste regel (+1 seconde)")
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { importMode = SubtitleCombineMode.KEEP_SOURCE_TIMES }
                    ) {
                        Text((if (importMode == SubtitleCombineMode.KEEP_SOURCE_TIMES)
                            "✓ " else "") + "Originele tijdcodes naast elkaar")
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { importMode = SubtitleCombineMode.PAIR_OVERLAPPING_TIMES }
                    ) {
                        Text((if (importMode == SubtitleCombineMode.PAIR_OVERLAPPING_TIMES)
                            "✓ " else "") + "Combineer twee teksten bij overlappende tijdcodes")
                    }
                    if (importMode == SubtitleCombineMode.PAIR_OVERLAPPING_TIMES) {
                        OutlinedButton(modifier = Modifier.fillMaxWidth(),
                            onClick = { driftSelectorOpen = true }) {
                            Text((if (selectedDrift != null) "✓ " else "") +
                                "Twee cueparen: corrigeer geleidelijk tijdverloop…")
                        }
                        if (selectedDrift != null) {
                            val selected = selectedDrift
                            Text(
                                if (drift == null)
                                    "Dubbele ankers ongeldig voor deze tijdlijn. " +
                                        "Kies ze opnieuw of gebruik één verschuiving."
                                else "Dubbele ankers actief: regels " +
                                    "${selected?.firstPrimaryRow}/${selected?.firstSecondaryRow}" +
                                    " → ${selected?.lastPrimaryRow}/${selected?.lastSecondaryRow}. " +
                                    "Bekijk hieronder het volledige nieuwe resultaat.",
                                style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    selectedDrift = null
                                    secondTrackPreviewPage = 0
                                }) { Text("Gebruik weer één constante verschuiving") }
                        } else {
                        Box {
                            OutlinedButton(onClick = { offsetChoicesOpen = true }) {
                                Text("Tijdverschuiving tweede spoor: " +
                                    PrivateSubtitleOffset.formatSeconds(secondTrackOffsetMs) +
                                    " seconden ▾")
                            }
                            DropdownMenu(expanded = offsetChoicesOpen,
                                onDismissRequest = { offsetChoicesOpen = false }) {
                                listOf(-10_000L, -5_000L, -2_000L, 0L,
                                    2_000L, 5_000L, 10_000L).forEach { value ->
                                    DropdownMenuItem(
                                        text = { Text((if (value > 0L) "+" else "") +
                                            "${value / 1000} seconden") },
                                        onClick = {
                                            secondTrackOffsetMs = value
                                            secondTrackCustomOffset = ""
                                            anchorSelectionNeedsApply = false
                                            secondTrackPreviewPage = 0
                                            offsetChoicesOpen = false
                                        })
                                }
                            }
                        }
                        Text("Of geef handmatig het exacte tijdverschil op. " +
                            "Punt of komma, maximaal drie decimalen; nul is hier " +
                            "toegestaan. Maximaal 600 seconden in beide richtingen.",
                            style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = secondTrackCustomOffset,
                            onValueChange = {
                                secondTrackCustomOffset = it.take(10)
                                anchorSelectionNeedsApply = false
                                secondTrackPreviewPage = 0
                            },
                            label = { Text("Eigen seconden tweede spoor, bv. -7,250") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true)
                        if (secondTrackCustomOffset.isNotBlank() &&
                            selectedPairOffset == null) {
                            Text("Geen geldige verschuiving. Gebruik bijvoorbeeld " +
                                "-7,250 of +12.125, binnen -600 en +600 seconden.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Text("Anker twee zelf gekozen cue-begintijden: " +
                            "geef het nummer van een bekende corresponderende " +
                            "regel in elk spoor op. Dit bepaalt alleen een " +
                            "relatieve verschuiving; de app herkent geen audio of taal.",
                            style = MaterialTheme.typography.bodySmall)
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(value = primaryAnchorRow,
                                onValueChange = {
                                    primaryAnchorRow = it.take(3)
                                    anchorSelectionNeedsApply = true
                                    secondTrackCustomOffset = ""
                                    secondTrackPreviewPage = 0
                                }, modifier = Modifier.weight(1f),
                                label = { Text("Regel eerste spoor") }, singleLine = true)
                            OutlinedTextField(value = secondaryAnchorRow,
                                onValueChange = {
                                    secondaryAnchorRow = it.take(3)
                                    anchorSelectionNeedsApply = true
                                    secondTrackCustomOffset = ""
                                    secondTrackPreviewPage = 0
                                }, modifier = Modifier.weight(1f),
                                label = { Text("Regel tweede spoor") }, singleLine = true)
                        }

                        Text("Of blader rechtstreeks door elk eigen spoor en " +
                            "tik op een regel om die als anker aan te wijzen. " +
                            "De volledige gekozen tekst verschijnt hieronder; " +
                            "alleen een korte regelhint staat in de bladerlijst.",
                            style = MaterialTheme.typography.bodySmall)
                        listOf(
                            Triple("Eerste spoor", draft, true),
                            Triple("Tweede spoor", stagedImport.imported, false)
                        ).forEach { (label, sourceCues, isPrimary) ->
                            val lastPage = maxOf(0, (sourceCues.size - 1) / 3)
                            val selectedPage = if (isPrimary) primaryAnchorPage
                                else secondaryAnchorPage
                            val shownSourcePage = selectedPage.coerceIn(0, lastPage)
                            Text("$label · bronregels, pagina " +
                                "${shownSourcePage + 1}/${lastPage + 1}",
                                style = MaterialTheme.typography.titleSmall)
                            sourceCues.withIndex().drop(shownSourcePage * 3).take(3)
                                .forEach { item ->
                                    val number = item.index + 1
                                    OutlinedButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            if (isPrimary) primaryAnchorRow = number.toString()
                                            else secondaryAnchorRow = number.toString()
                                            anchorSelectionNeedsApply = true
                                            secondTrackCustomOffset = ""
                                            secondTrackPreviewPage = 0
                                        }
                                    ) {
                                        Text("Kies regel $number · ${item.value.startLabel} · " +
                                            item.value.text.replace('\n', ' ').take(85))
                                    }
                                }
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                TextButton(enabled = shownSourcePage > 0, onClick = {
                                    if (isPrimary) primaryAnchorPage = shownSourcePage - 1
                                    else secondaryAnchorPage = shownSourcePage - 1
                                }) { Text("← Vorige $label") }
                                TextButton(enabled = shownSourcePage < lastPage, onClick = {
                                    if (isPrimary) primaryAnchorPage = shownSourcePage + 1
                                    else secondaryAnchorPage = shownSourcePage + 1
                                }) { Text("Volgende →") }
                            }
                        }
                        val anchorPreview = PrivateSubtitleTrackAnchorOffset.preview(
                            draft, stagedImport.imported,
                            primaryAnchorRow, secondaryAnchorRow)
                        val anchorShift = anchorPreview?.offsetMillis
                        if (anchorSelectionNeedsApply) {
                            Text("Je hebt de ankerregels gewijzigd. Kies " +
                                "opnieuw berekenen, vul zelf een exact " +
                                "verschil in of kies bewust een preset. " +
                                "Tot dan kan deze koppeling niet worden bevestigd.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        if (anchorPreview != null) {
                            Text("Eerste spoor, bronregel ${anchorPreview.primaryRow}: " +
                                "${anchorPreview.primaryCue.startLabel} → " +
                                "${anchorPreview.primaryCue.endLabel}\n" +
                                anchorPreview.primaryCue.text,
                                style = MaterialTheme.typography.bodySmall)
                            Text("Tweede spoor, bronregel ${anchorPreview.secondaryRow}: " +
                                "${anchorPreview.secondaryCue.startLabel} → " +
                                "${anchorPreview.secondaryCue.endLabel}\n" +
                                anchorPreview.secondaryCue.text,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        if (anchorShift != null) {
                            Text("Voorgestelde verschuiving op twee handmatig " +
                                "gekozen startmomenten: " +
                                PrivateSubtitleOffset.formatSeconds(anchorShift) +
                                " seconden. Dit is geen bevestigde zinsovereenkomst.",
                                style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("Kies bestaande rijnummers 1–${draft.size} en " +
                                "1–${stagedImport.imported.size}. Hun startverschil " +
                                "moet binnen ±600 seconden vallen.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(modifier = Modifier.fillMaxWidth(),
                            enabled = anchorShift != null,
                            onClick = {
                                if (anchorShift != null) {
                                    secondTrackCustomOffset =
                                        PrivateSubtitleOffset.formatSeconds(anchorShift)
                                    anchorSelectionNeedsApply = false
                                    secondTrackPreviewPage = 0
                                }
                            }) { Text("Neem deze twee startmomenten als verschuiving") }
                        Text("Kies desgewenst de verschuiving van het tweede " +
                            "spoor; een negatieve tijd of tijd buiten de bestandsgrens " +
                            "wordt geweigerd.", style = MaterialTheme.typography.bodySmall)
                        Text("De eerste tekst blijft bovenaan; overlappende tekst uit " +
                            "bestand twee komt op een tweede regel. Alleen minstens " +
                            "half overlappende tijdvakken worden één-op-één gekoppeld. " +
                            "Niet gekoppelde regels blijven apart. Controleer taal en " +
                            "timing zelf: dit bewijst geen vertaling of synchronisatie.",
                            style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text(
                        if (!stillCurrent)
                            "Het huidige concept is veranderd: voeg opnieuw toe."
                        else if (proposed == null)
                            "Samenvoegen kan niet: maximaal 500 regels, geldige " +
                                "tijdcodes en maximaal 256 KiB per export."
                        else if (importMode == SubtitleCombineMode.PAIR_OVERLAPPING_TIMES)
                            "${proposed.pairedCount} tijd- of handmatig gekozen bronparen, " +
                                "${proposed.unpairedSecondTrack} regels uit tweede spoor apart; " +
                                "${proposed.unpairedFirstTrack} regels uit eerste spoor " +
                                "zonder tweede tekst. ${proposed.cues.size} regels in resultaat. " +
                                "Beoordeel eerst tekst en timing; er is geen " +
                                "audio-/video-uitlijning."
                        else "${proposed.cues.size} regels in de gecombineerde versie. " +
                            "${proposed.adjacentOverlaps} aangrenzende tijdsoverlappingen. " +
                            "Controleer tijdcodes zelf; er is geen audio-/video-uitlijning.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (importMode == SubtitleCombineMode.PAIR_OVERLAPPING_TIMES) {
                        Text("Koppel zelf twee volledige oorspronkelijke bronregels, " +
                            "ook als tijdcodes onvoldoende overlappen. Dit is een " +
                            "bewuste inhoudskeuze, geen automatische vertaling. " +
                            "Het gecombineerde paar gebruikt tijdcodes van spoor 1.",
                            style = MaterialTheme.typography.titleSmall)
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                modifier = Modifier.weight(1f),
                                value = manualFirstRow,
                                onValueChange = {
                                    manualFirstRow =
                                        it.filter { char -> char in '0'..'9' }.take(4)
                                },
                                label = { Text("Spoor 1 · bronregel") },
                                singleLine = true
                            )
                            OutlinedTextField(
                                modifier = Modifier.weight(1f),
                                value = manualSecondRow,
                                onValueChange = {
                                    manualSecondRow =
                                        it.filter { char -> char in '0'..'9' }.take(4)
                                },
                                label = { Text("Spoor 2 · bronregel") },
                                singleLine = true
                            )
                        }
                        val manualPrimary = manualFirstRow.toIntOrNull()
                        val manualSecondary = manualSecondRow.toIntOrNull()
                        val candidate = if (manualPrimary != null &&
                            manualSecondary != null &&
                            manualPrimary in 1..draft.size &&
                            manualSecondary in 1..stagedImport.imported.size &&
                            manualPairs.size < 32 &&
                            manualPairs.none { it.primaryRow == manualPrimary ||
                                it.secondaryRow == manualSecondary } &&
                            manualSecondary !in excludedSecondRows
                        ) SubtitleManualPair(manualPrimary, manualSecondary) else null

                        if (candidate != null) {
                            val firstCue = draft[candidate.primaryRow - 1]
                            val secondCue =
                                stagedImport.imported[candidate.secondaryRow - 1]
                            Text("Spoor 1 · bronregel ${candidate.primaryRow} · " +
                                "${firstCue.startLabel} → ${firstCue.endLabel}\n" +
                                firstCue.text + "\nSpoor 2 · originele bronregel " +
                                "${candidate.secondaryRow} · " +
                                "${secondCue.startLabel} → ${secondCue.endLabel}\n" +
                                secondCue.text,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        val candidatePreview = remember(draft, proposedIncoming,
                            selectedPairOffset, stillCurrent, excludedSecondRows,
                            manualPairs, candidate) {
                            if (candidate != null && stillCurrent &&
                                selectedPairOffset != null && proposedIncoming != null
                            ) PrivateSubtitleDocumentCombine.preview(
                                draft, proposedIncoming,
                                SubtitleCombineMode.PAIR_OVERLAPPING_TIMES,
                                selectedPairOffset, excludedSecondRows,
                                manualPairs + candidate
                            ) else null
                        }
                        if (manualFirstRow.isNotBlank() &&
                            manualSecondRow.isNotBlank() && candidatePreview == null) {
                            Text("Deze keuze is niet mogelijk. Kies twee geldige " +
                                "verschillende, nog niet vastgekoppelde bronregels; " +
                                "de handmatige paren moeten in beide sporen " +
                                "dezelfde volgorde houden. De volledige SRT/VTT-" +
                                "exportgrenzen blijven verplicht.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = candidatePreview != null,
                            onClick = {
                                val exact = candidate
                                if (exact != null && candidatePreview != null) {
                                    manualPairs = manualPairs + exact
                                    manualFirstRow = ""
                                    manualSecondRow = ""
                                    manualReviewPage = 0
                                    pairReviewPage = 0
                                    secondTrackPreviewPage = 0
                                }
                            }
                        ) { Text("Voeg dit exacte eigen bronpaar toe aan het voorstel") }
                        Text("Of voeg meerdere exact gekozen bronparen toe: " +
                            "1:2,3-5:7-9 koppelt regel 1 aan 2 en regels 3–5 " +
                            "aan 7–9. Alleen even lange, strikt oplopende " +
                            "bereiken in beide oorspronkelijke sporen.",
                            style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = manualBulkExpression,
                            onValueChange = {
                                // Keep a sentinel over-limit character: a long
                                // paste must be invalid, not silently become
                                // another valid partial expression.
                                manualBulkExpression = it.take(
                                    PrivateSubtitleManualPairSelection.MAX_EXPRESSION_LENGTH + 1)
                                manualBulkPage = 0
                            },
                            label = { Text("Bronpaarreeksen, bv. 1:2,3-5:7-9") },
                            minLines = 1, maxLines = 3
                        )
                        val bulkManualPairs = remember(manualBulkExpression,
                            draft.size, stagedImport.imported.size) {
                            PrivateSubtitleManualPairSelection.parse(
                                manualBulkExpression, draft.size,
                                stagedImport.imported.size)
                        }
                        val bulkProposal = remember(bulkManualPairs, manualPairs,
                            proposedIncoming, selectedPairOffset, stillCurrent,
                            excludedSecondRows, draft) {
                            if (bulkManualPairs != null &&
                                selectedPairOffset != null && proposedIncoming != null &&
                                stillCurrent
                            ) PrivateSubtitleDocumentCombine.preview(
                                draft, proposedIncoming,
                                SubtitleCombineMode.PAIR_OVERLAPPING_TIMES,
                                selectedPairOffset, excludedSecondRows,
                                manualPairs + bulkManualPairs
                            ) else null
                        }
                        if (manualBulkExpression.isNotBlank()) {
                            Text(if (manualBulkExpression.length >
                                PrivateSubtitleManualPairSelection.MAX_EXPRESSION_LENGTH)
                                "Te lange bronpaarinvoer: maximaal 512 tekens. " +
                                    "Kort de invoer bewust in."
                                else if (bulkProposal == null)
                                "Geen geldige bundel: controleer originele " +
                                    "bronnummers, oplopende volgorde, unieke " +
                                    "paarregels, bestaande uitsluitingen en " +
                                    "de volledige SRT/VTT-exportgrenzen."
                                else "${bulkManualPairs?.size ?: 0} nieuwe " +
                                    "handmatige paren, volledig gecontroleerd " +
                                    "tegen alle huidige keuzes.",
                                color = if (bulkProposal == null)
                                    MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        if (bulkProposal != null && bulkManualPairs != null) {
                            val pages = maxOf(1, (bulkManualPairs.size + 3) / 4)
                            val page = manualBulkPage.coerceIn(0, pages - 1)
                            Text("Eigen bronpaar-bundel · pagina ${page + 1}/$pages. " +
                                "Bekijk alle volledige originele zinnen vóór toepassen.",
                                style = MaterialTheme.typography.titleSmall)
                            bulkManualPairs.drop(page * 4).take(4).forEach { pin ->
                                val firstCue = draft[pin.primaryRow - 1]
                                val secondCue =
                                    stagedImport.imported[pin.secondaryRow - 1]
                                Text("Spoor 1 · bronregel ${pin.primaryRow} · " +
                                    "${firstCue.startLabel}\n" + firstCue.text +
                                    "\nSpoor 2 · bronregel ${pin.secondaryRow} · " +
                                    "${secondCue.startLabel}\n" + secondCue.text,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                TextButton(enabled = page > 0,
                                    onClick = { manualBulkPage = page - 1 }) {
                                    Text("← Vorige bundel")
                                }
                                TextButton(enabled = page < pages - 1,
                                    onClick = { manualBulkPage = page + 1 }) {
                                    Text("Volgende bundel →")
                                }
                            }
                        }
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = bulkProposal != null,
                            onClick = {
                                val current = PrivateSubtitleManualPairSelection.parse(
                                    manualBulkExpression, draft.size,
                                    stagedImport.imported.size)
                                if (current != null && current == bulkManualPairs &&
                                    bulkProposal != null
                                ) {
                                    manualPairs = manualPairs + current
                                    manualBulkExpression = ""
                                    manualBulkPage = 0
                                    manualReviewPage = 0
                                    pairReviewPage = 0
                                    secondTrackPreviewPage = 0
                                }
                            }
                        ) { Text("Neem hele gecontroleerde bronpaar-bundel over") }
                        if (manualPairs.isNotEmpty()) {
                            val orderedPins = manualPairs.sortedBy { it.primaryRow }
                            val pageCount = maxOf(1, (orderedPins.size + 3) / 4)
                            val page = manualReviewPage.coerceIn(0, pageCount - 1)
                            Text("${orderedPins.size} bewust gekozen bronparen " +
                                "(maximaal 32) · pagina ${page + 1}/$pageCount. " +
                                "Verwijderen herberekent de overige tijd-overlapparen.",
                                style = MaterialTheme.typography.bodySmall)
                            orderedPins.drop(page * 4).take(4).forEach { pin ->
                                val firstCue = draft[pin.primaryRow - 1]
                                val secondCue =
                                    stagedImport.imported[pin.secondaryRow - 1]
                                Text("Spoor 1 · originele regel ${pin.primaryRow}\n" +
                                    firstCue.text + "\nSpoor 2 · originele regel " +
                                    "${pin.secondaryRow}\n" + secondCue.text,
                                    style = MaterialTheme.typography.bodySmall)
                                OutlinedButton(modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        manualPairs = manualPairs - pin
                                        manualReviewPage = 0
                                        pairReviewPage = 0
                                        secondTrackPreviewPage = 0
                                    }) {
                                    Text("Herstel automatische koppeling voor dit paar")
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                TextButton(enabled = page > 0,
                                    onClick = { manualReviewPage = page - 1 }) {
                                    Text("← Vorige handmatige")
                                }
                                TextButton(enabled = page < pageCount - 1,
                                    onClick = { manualReviewPage = page + 1 }) {
                                    Text("Volgende handmatige →")
                                }
                            }
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    manualPairs = emptyList()
                                    manualReviewPage = 0
                                    manualBulkPage = 0
                                    pairReviewPage = 0
                                    secondTrackPreviewPage = 0
                                }
                            ) {
                                Text("Herstel de tijdmatcher voor alle handmatige paren")
                            }
                        }
                    }
                    if (importMode == SubtitleCombineMode.PAIR_OVERLAPPING_TIMES) {
                        Text("Wil je meerdere originele regels uit spoor twee " +
                            "uitsluiten van koppeling? Ze blijven allemaal " +
                            "zichtbaar als eigen, losse ondertitels.",
                            style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = excludeRowExpression,
                            onValueChange = { excludeRowExpression = it.take(2000) },
                            label = { Text("Bronregels tweede spoor, bv. 2-5,9,12-15") },
                            singleLine = true)
                        Text("Of typ maximaal 16 losse tijdvensters uit het " +
                            "tweede spoor, bv. 00:10-00:20;01:30-01:45. " +
                            "Een venster wordt alleen na jouw keuze omgezet.",
                            style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = excludeWindowExpression,
                            onValueChange = { excludeWindowExpression = it.take(512) },
                            label = { Text("Tijdvensters tweede spoor") },
                            singleLine = true)
                        val windowSelection = PrivateSubtitleTimeWindowRows
                            .expressionForWindows(
                                stagedImport.imported, excludeWindowExpression)
                        OutlinedButton(modifier = Modifier.fillMaxWidth(),
                            enabled = windowSelection != null,
                            onClick = {
                                if (windowSelection != null)
                                    excludeRowExpression = windowSelection
                            }) { Text("Neem volledige bronregels uit tijdvensters") }
                        val bulkIndices = PrivateSubtitleRowSelection.parse(
                            excludeRowExpression, stagedImport.imported.size)
                        val overlapsPinned = bulkIndices?.any { zeroIndex ->
                            manualPairs.any { it.secondaryRow == zeroIndex + 1 }
                        } == true
                        if (excludeRowExpression.isNotBlank()) {
                            Text(
                                if (overlapsPinned)
                                    "Deze selectie bevat al bewust vastgekoppelde " +
                                        "bronregels. Herstel eerst die handmatige " +
                                        "paren voordat je ze los kunt houden."
                                else if (bulkIndices == null)
                                    "Geen geldige bronregelselectie. " +
                                        "Gebruik 1–${stagedImport.imported.size} " +
                                        "zonder dubbele of overlappende bereiken."
                                else "${bulkIndices.size} oorspronkelijke bronregels " +
                                    "klaar om bewust uit de koppeling uit te sluiten.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(modifier = Modifier.fillMaxWidth(),
                            enabled = bulkIndices != null && !overlapsPinned,
                            onClick = {
                                val selected = PrivateSubtitleRowSelection.parse(
                                    excludeRowExpression, stagedImport.imported.size)
                                if (selected != null && selected == bulkIndices &&
                                    selected.none { row -> manualPairs.any {
                                        it.secondaryRow == row + 1 } }) {
                                    excludedSecondRows =
                                        excludedSecondRows + selected.map { it + 1 }
                                    excludeRowExpression = ""
                                    excludedReviewPage = 0
                                    pairReviewPage = 0
                                    secondTrackPreviewPage = 0
                                }
                            }) { Text("Houd deze bronregels los, zonder tekstverlies") }
                    }
                    if (importMode == SubtitleCombineMode.PAIR_OVERLAPPING_TIMES &&
                        excludedSecondRows.isNotEmpty()) {
                        val excludedSorted = excludedSecondRows.sorted()
                        val excludedPages = maxOf(1, (excludedSorted.size + 3) / 4)
                        val shownExcludedPage =
                            excludedReviewPage.coerceIn(0, excludedPages - 1)
                        Text("${excludedSorted.size} bronregels uit spoor twee " +
                            "expliciet als losse cues behouden · pagina " +
                            "${shownExcludedPage + 1}/$excludedPages. " +
                            "Geen van hun teksten wordt verwijderd.",
                            style = MaterialTheme.typography.bodySmall)
                        excludedSorted.drop(shownExcludedPage * 4).take(4)
                            .forEach { sourceRow ->
                                val sourceCue = stagedImport.imported.getOrNull(sourceRow - 1)
                                if (sourceCue != null) {
                                    Text("Spoor 2 · bronregel $sourceRow · " +
                                        "${sourceCue.startLabel} · " +
                                        sourceCue.text.replace('\n', ' ').take(120),
                                        style = MaterialTheme.typography.bodySmall)
                                    OutlinedButton(modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            excludedSecondRows =
                                                excludedSecondRows - sourceRow
                                            secondTrackPreviewPage = 0
                                        }) { Text("Sta koppeling van regel $sourceRow weer toe") }
                                }
                            }
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(enabled = shownExcludedPage > 0,
                                onClick = {
                                    excludedReviewPage = shownExcludedPage - 1
                                }) { Text("← Vorige losse regels") }
                            TextButton(enabled = shownExcludedPage < excludedPages - 1,
                                onClick = {
                                    excludedReviewPage = shownExcludedPage + 1
                                }) { Text("Volgende →") }
                        }
                        OutlinedButton(modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                excludedSecondRows = emptySet()
                                excludedReviewPage = 0
                                pairReviewPage = 0
                                secondTrackPreviewPage = 0
                            }) { Text("Sta alle eerder uitgesloten koppelingen weer toe") }
                    }
                    if (proposed != null &&
                        importMode == SubtitleCombineMode.PAIR_OVERLAPPING_TIMES &&
                        (proposed.unpairedPrimaryRows.isNotEmpty() ||
                            proposed.unpairedSecondaryRows.isNotEmpty())) {
                        OutlinedButton(modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                unmatchedReviewOpen = !unmatchedReviewOpen
                                unmatchedReviewPage = 0
                            }) {
                            Text((if (unmatchedReviewOpen) "▼ " else "▶ ") +
                                "Inspecteer niet-gekoppelde bronregels " +
                                "(${proposed.unpairedPrimaryRows.size} + " +
                                "${proposed.unpairedSecondaryRows.size})")
                        }
                        if (unmatchedReviewOpen) {
                            // Keep original document row indices. This read-only
                            // review does not alter matching, exclusions or files.
                            val allUnmatched = proposed.unpairedPrimaryRows.map { 1 to it } +
                                proposed.unpairedSecondaryRows.map { 2 to it }
                            val unmatched = allUnmatched.filter { (track, row) ->
                                (unmatchedReviewTrack == 0 || track == unmatchedReviewTrack) &&
                                    (unmatchedReviewQuery.isBlank() ||
                                        (if (track == 1) draft.getOrNull(row - 1)
                                        else stagedImport.imported.getOrNull(row - 1))
                                            ?.text?.contains(unmatchedReviewQuery.trim(),
                                                ignoreCase = true) == true)
                            }
                            val pageCount = maxOf(1, (unmatched.size + 4) / 5)
                            val shownPage = unmatchedReviewPage.coerceIn(0, pageCount - 1)
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = unmatchedReviewQuery,
                                onValueChange = {
                                    unmatchedReviewQuery = it.take(100)
                                    unmatchedReviewPage = 0
                                },
                                label = { Text("Zoek in losse originele brontekst") },
                                singleLine = true
                            )
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                listOf(0 to "Beide", 1 to "Spoor 1",
                                    2 to "Spoor 2").forEach { (track, label) ->
                                    OutlinedButton(modifier = Modifier.weight(1f),
                                        onClick = {
                                            unmatchedReviewTrack = track
                                            unmatchedReviewPage = 0
                                        }) {
                                        Text((if (unmatchedReviewTrack == track) "✓ "
                                            else "") + label)
                                    }
                                }
                            }
                            Text("${unmatched.size} van ${allUnmatched.size} losse " +
                                "originele bronregels · pagina ${shownPage + 1}/$pageCount. " +
                                "De tweede spoor-tijdcorrectie is afzonderlijk " +
                                "in de resultaatpreview te zien.",
                                style = MaterialTheme.typography.bodySmall)
                            if (unmatched.isEmpty()) Text(
                                "Geen ongekoppelde bronregel met deze zoekkeuze.",
                                style = MaterialTheme.typography.bodySmall)
                            unmatched.drop(shownPage * 5).take(5).forEach { (track, row) ->
                                val cue = if (track == 1) draft.getOrNull(row - 1)
                                    else stagedImport.imported.getOrNull(row - 1)
                                if (cue != null) {
                                    Text("Spoor $track · originele regel $row · " +
                                        "${cue.startLabel} → ${cue.endLabel}\n" +
                                        cue.text,
                                        style = MaterialTheme.typography.bodySmall)
                                    OutlinedButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = if (track == 1)
                                            manualPairs.none { it.primaryRow == row }
                                            else manualPairs.none { it.secondaryRow == row } &&
                                                row !in excludedSecondRows,
                                        onClick = {
                                            if (track == 1) manualFirstRow = row.toString()
                                            else manualSecondRow = row.toString()
                                        }
                                    ) {
                                        Text("Neem originele regel $row uit spoor $track " +
                                            "over voor handmatig koppelen (nog niet toepassen)")
                                    }
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                TextButton(enabled = shownPage > 0,
                                    onClick = { unmatchedReviewPage = shownPage - 1 }) {
                                    Text("← Vorige losse")
                                }
                                TextButton(enabled = shownPage < pageCount - 1,
                                    onClick = { unmatchedReviewPage = shownPage + 1 }) {
                                    Text("Volgende losse →")
                                }
                            }
                        }
                    }
                    if (proposed != null &&
                        importMode == SubtitleCombineMode.PAIR_OVERLAPPING_TIMES) {
                        OutlinedButton(modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                pairReviewOpen = !pairReviewOpen
                                pairReviewPage = 0
                            }) {
                            Text((if (pairReviewOpen) "▼ " else "▶ ") +
                                "Controleer elke bronregel-koppeling " +
                                "(${proposed.pairedLinks.size} paren)")
                        }
                        if (pairReviewOpen) {
                            // A text or exact source-row filter is display-only.
                            // Selection/exclusion still uses the original secondary row.
                            val sourceRow = pairReviewSecondaryRow.toIntOrNull()
                            val filteredLinks = proposed.pairedLinks.filter { link ->
                                (pairReviewQuery.isBlank() ||
                                    link.primaryText.contains(pairReviewQuery.trim(),
                                        ignoreCase = true) ||
                                    link.secondaryText.contains(pairReviewQuery.trim(),
                                        ignoreCase = true)) &&
                                    (pairReviewSecondaryRow.isBlank() ||
                                        sourceRow == link.secondarySourceRow)
                            }
                            val pages = maxOf(1, (filteredLinks.size + 3) / 4)
                            val currentPairPage = pairReviewPage.coerceIn(0, pages - 1)
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = pairReviewQuery,
                                onValueChange = {
                                    pairReviewQuery = it.take(100)
                                    pairReviewPage = 0
                                },
                                label = { Text("Zoek in beide eigen bronzinnen") },
                                singleLine = true
                            )
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = pairReviewSecondaryRow,
                                onValueChange = {
                                    pairReviewSecondaryRow =
                                        it.filter { char -> char in '0'..'9' }.take(3)
                                    pairReviewPage = 0
                                },
                                label = { Text("Alleen originele regelnummer spoor 2 (optioneel)") },
                                singleLine = true
                            )
                            Text("${filteredLinks.size} van ${proposed.pairedLinks.size} " +
                                "gekoppelde bronparen · pagina " +
                                "${currentPairPage + 1}/$pages. Alleen " +
                                "tijdsoverlap: dit bewijst geen overeenkomst " +
                                "van taal of betekenis.",
                                style = MaterialTheme.typography.bodySmall)
                            if (filteredLinks.isEmpty()) {
                                Text("Geen gekoppeld paar met deze invoer. " +
                                    "Een ongekoppelde of uitgesloten bronregel " +
                                    "verschijnt niet als paar; wijzig de filters.",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            filteredLinks.drop(currentPairPage * 4).take(4)
                                .forEach { link ->
                                    val secondSourceStart = stagedImport.imported
                                        .getOrNull(link.secondarySourceRow - 1)?.startLabel
                                        ?: link.secondaryStart
                                    Text((if (link.manuallyChosen)
                                        "✓ Bewust handmatig gekozen; " +
                                            "de eerste-spoortijd bepaalt de " +
                                            "tijdcodes van het gecombineerde paar.\n"
                                        else "Automatisch voorgesteld uit tijdsoverlap.\n") +
                                        "Spoor 1 · bronregel ${link.primarySourceRow} · " +
                                        "${link.primaryStart}\n${link.primaryText}\n" +
                                        "Spoor 2 · bronregel ${link.secondarySourceRow} · " +
                                        "origineel ${secondSourceStart} → " +
                                        "na timing ${link.secondaryStart}\n" +
                                        "${link.secondaryText}\n" +
                                        "Overlappende tijd: ${link.overlapMillis} ms.",
                                        style = MaterialTheme.typography.bodySmall)
                                    if (!link.manuallyChosen) {
                                        OutlinedButton(
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = {
                                                manualFirstRow =
                                                    link.primarySourceRow.toString()
                                                manualSecondRow =
                                                    link.secondarySourceRow.toString()
                                            }
                                        ) {
                                            Text("Neem beide bronnummers over om " +
                                                "de koppeling bewust vast te zetten")
                                        }
                                    }
                                    OutlinedButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            if (link.manuallyChosen) {
                                                manualPairs = manualPairs.filterNot {
                                                    it.primaryRow == link.primarySourceRow &&
                                                        it.secondaryRow == link.secondarySourceRow
                                                }
                                                manualReviewPage = 0
                                            } else {
                                                excludedSecondRows =
                                                    excludedSecondRows + link.secondarySourceRow
                                            }
                                            pairReviewPage = 0
                                            excludedReviewPage = 0
                                            secondTrackPreviewPage = 0
                                        }) {
                                        Text(if (link.manuallyChosen)
                                            "Verwijder handmatig paar; laat tijdmatcher kiezen"
                                        else "Laat tweede bronregel " +
                                            "${link.secondarySourceRow} als losse cue staan")
                                    }
                                }
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                TextButton(enabled = currentPairPage > 0,
                                    onClick = { pairReviewPage = currentPairPage - 1 }) {
                                    Text("← Vorige paren")
                                }
                                TextButton(enabled = currentPairPage < pages - 1,
                                    onClick = { pairReviewPage = currentPairPage + 1 }) {
                                    Text("Volgende paren →")
                                }
                            }
                        }
                    }
                    if (proposed != null) {
                        Text("Volledige resultaat-preview, vijf cues per pagina " +
                            "(niet alleen de tekstparen): pagina ${shownPairPage + 1}/" +
                            "$previewPages.", style = MaterialTheme.typography.titleSmall)
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                modifier = Modifier.weight(1f),
                                value = resultPreviewJump,
                                onValueChange = {
                                    resultPreviewJump =
                                        it.filter { char -> char in '0'..'9' }.take(3)
                                },
                                label = { Text("Ga naar resultaatregel") },
                                singleLine = true
                            )
                            val destination = resultPreviewJump.toIntOrNull()
                            OutlinedButton(
                                enabled = destination != null &&
                                    destination in 1..previewRows.size,
                                onClick = {
                                    val chosen = resultPreviewJump.toIntOrNull()
                                    if (chosen != null && chosen in 1..previewRows.size)
                                        secondTrackPreviewPage = (chosen - 1) / 5
                                }
                            ) { Text("Ga") }
                        }
                        previewRows.drop(shownPairPage * 5).take(5)
                            .forEachIndexed { localIndex, cue ->
                                val sourceNumber = shownPairPage * 5 + localIndex + 1
                                Text("Resultaatregel $sourceNumber · " +
                                    "${cue.startLabel} → ${cue.endLabel}\n" + cue.text,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(enabled = shownPairPage > 0,
                                onClick = { secondTrackPreviewPage = shownPairPage - 1 }) {
                                Text("← Vorige")
                            }
                            TextButton(enabled = shownPairPage < previewPages - 1,
                                onClick = { secondTrackPreviewPage = shownPairPage + 1 }) {
                                Text("Volgende →")
                            }
                        }
                    }
                    Text(
                        "SRT en VTT zijn beide leesbaar. Alleen expliciet bewaren als " +
                            "nieuwe SRT/VTT-kopie schrijft naar je gekozen " +
                            "Android-bestandsprovider, die extern kan synchroniseren.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(enabled = proposed != null && stillCurrent, onClick = {
                    val latestDrift = selectedDrift?.takeIf {
                        importMode == SubtitleCombineMode.PAIR_OVERLAPPING_TIMES
                    }?.let {
                        PrivateSubtitleTrackDriftAlignment.preview(
                            draft, stagedImport.imported,
                            it.firstPrimaryRow, it.firstSecondaryRow,
                            it.lastPrimaryRow, it.lastSecondaryRow)
                    }
                    val latestOffset = when {
                        importMode != SubtitleCombineMode.PAIR_OVERLAPPING_TIMES ->
                            secondTrackOffsetMs
                        selectedDrift != null -> if (latestDrift != null) 0L else null
                        anchorSelectionNeedsApply -> null
                        secondTrackCustomOffset.isBlank() -> secondTrackOffsetMs
                        else -> PrivateSubtitleOffset.parseSeconds(
                            secondTrackCustomOffset, allowZero = true)
                    }
                    val latestIncoming = if (
                        selectedDrift != null &&
                        importMode == SubtitleCombineMode.PAIR_OVERLAPPING_TIMES
                    ) latestDrift?.adjustedSecondary else stagedImport.imported
                    val updated = if (latestOffset != null && latestIncoming != null)
                        PrivateSubtitleDocumentCombine.preview(
                            draft, latestIncoming, importMode, latestOffset,
                            if (importMode == SubtitleCombineMode.PAIR_OVERLAPPING_TIMES)
                                excludedSecondRows else emptySet(),
                            if (importMode == SubtitleCombineMode.PAIR_OVERLAPPING_TIMES)
                                manualPairs else emptyList())
                    else null
                    if (draft != stagedImport.existing || updated == null ||
                        proposed == null || updated.cues != proposed.cues ||
                        updated.pairedCount != proposed.pairedCount ||
                        updated.pairedLinks != proposed.pairedLinks) {
                        notice = "Het concept is veranderd of de gecombineerde " +
                            "ondertitels zijn ongeldig. Probeer opnieuw."
                    } else {
                        applyChange(updated.cues)
                        query = ""
                        shownIndex = -1
                        notice = if (importMode == SubtitleCombineMode.PAIR_OVERLAPPING_TIMES)
                            "${updated.pairedCount} tekstparen gekoppeld; " +
                                "${updated.unpairedSecondTrack} regels apart behouden. " +
                                "Gebruik Bewaar kopie… voor een nieuw bestand."
                        else "${updated.importedCount} regels toegevoegd aan " +
                            "tijdelijk concept. Gebruik Bewaar kopie… voor een nieuw bestand."
                    }
                    pendingImport = null
                }) { Text("Voeg alleen toe aan tijdelijk concept") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) { Text("Annuleren") }
            }
        )
        if (driftSelectorOpen) {
            SubtitleTrackDriftSelectorDialog(
                primary = draft,
                secondary = stagedImport.imported,
                initial = selectedDrift,
                onChoose = { selection ->
                    selectedDrift = selection
                    driftSelectorOpen = false
                    secondTrackPreviewPage = 0
                },
                onDismiss = { driftSelectorOpen = false }
            )
        }
    }

    if (insertOpen) {
        val start = PrivateSubtitleTimelineNavigation.timestamp(insertStart)
        val end = PrivateSubtitleTimelineNavigation.timestamp(insertEnd)
        val ready = start != null && end != null && end > start &&
            insertContent.trim().isNotBlank()
        AlertDialog(
            onDismissRequest = { insertOpen = false },
            title = { Text("Nieuwe ondertitel · zelf toevoegen") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Typ begin, einde en eigen tekst. Er is geen audio-, " +
                        "taal- of tijdsynchronisatie; alle waarden kies je zelf.",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = insertStart,
                        onValueChange = { insertStart = it.take(13) },
                        label = { Text("Begintijd · hh:mm:ss.000") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = insertEnd,
                        onValueChange = { insertEnd = it.take(13) },
                        label = { Text("Eindtijd · hh:mm:ss.000") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = insertContent,
                        onValueChange = { if (it.length <= 2400) insertContent = it },
                        label = { Text("Zelfgeschreven ondertitelregel") },
                        minLines = 2, maxLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = TextDirection.ContentOrRtl))
                    Text(
                        if (!ready) "Voer een geldige begintijd, een latere eindtijd " +
                            "en niet-lege tekst in."
                        else "Tijdcodes worden op volgorde gezet; bestaande " +
                            "regels en hun tijdcodes blijven intact.",
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(enabled = ready, onClick = {
                    val updated = PrivateSubtitleDraftEditor.insertCue(
                        draft, start!!, end!!, insertContent.trim())
                    if (updated == null) {
                        notice = "Nieuwe ondertitel geweigerd: te lang of documentgrens bereikt."
                    } else {
                        applyChange(updated)
                        query = ""
                        shownIndex = updated.indexOfFirst {
                            it.startMillis == start && it.endMillis == end &&
                                it.text == insertContent.trim()
                        }
                        notice = "Nieuwe handmatige cue toegevoegd; niet bewaard."
                    }
                    insertOpen = false
                }) { Text("Voeg alleen aan concept toe") }
            },
            dismissButton = {
                TextButton(onClick = { insertOpen = false }) { Text("Annuleren") }
            }
        )
    }
    if (clipOpen) {
        val from = PrivateSubtitleTimelineNavigation.timestamp(clipStart)
        val to = PrivateSubtitleTimelineNavigation.timestamp(clipEnd)
        val preview = if (from != null && to != null)
            PrivateSubtitleClip.extract(draft, from, to) else null
        AlertDialog(
            onDismissRequest = { clipOpen = false },
            title = { Text("Ondertitels voor een clip uitsnijden") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Kies zelf een tijdvenster. Regels buiten dit venster " +
                        "vallen uit het tijdelijke concept; deels overlappende " +
                        "regels worden op de clipranden afgekapt. Alles begint " +
                        "daarna vanaf 00:00. Dit is geen audio-/videotimingcontrole.",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = clipStart,
                        onValueChange = { clipStart = it.take(13) },
                        label = { Text("Clip begint bij · hh:mm:ss.000") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = clipEnd,
                        onValueChange = { clipEnd = it.take(13) },
                        label = { Text("Clip eindigt bij · hh:mm:ss.000") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text(
                        if (preview == null)
                            "Geen geldige clip. Kies begin < einde binnen de " +
                                "huidige ondertitelregels."
                        else "${preview.retainedCount} regels blijven over, " +
                            "${preview.clippedCount} aan begin/einde ingekort. " +
                            "Clip duurt ${preview.windowDurationMs / 1000} seconden.",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("Dit verandert alleen het RAM-concept. Herstel haalt " +
                        "de oorspronkelijke regels terug; voor een nieuwe SRT/VTT-" +
                        "kopie moet je daarna zelf Bewaar kopie… kiezen.",
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(enabled = preview != null, onClick = {
                    val confirmed = PrivateSubtitleClip.extract(
                        draft, from!!, to!!)
                    if (confirmed != null) {
                        applyChange(confirmed.cues)
                        query = ""
                        shownIndex = -1
                        notice = "Clip met ${confirmed.retainedCount} regels als " +
                            "tijdelijk concept ingesteld. Bewaar eventueel een nieuwe kopie."
                    }
                    clipOpen = false
                }) { Text("Maak tijdelijke ondertitelclip") }
            },
            dismissButton = {
                TextButton(onClick = { clipOpen = false }) { Text("Annuleren") }
            }
        )
    }
    if (wrapOpen) {
        val proposed = PrivateSubtitleLineWrap.preview(draft, wrapWidth)
        AlertDialog(
            onDismissRequest = { wrapOpen = false },
            title = { Text("Lange ondertitelregels afbreken") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Voeg alleen op gewone spaties regelafbrekingen toe. " +
                        "Tekst, woordvolgorde en tijdcodes blijven behouden. " +
                        "Kies een gewenste maximale regelbreedte; langere woorden " +
                        "blijven intact. Dit is geen taalkundige of audioanalyse.",
                        style = MaterialTheme.typography.bodySmall)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(32, 42, 50).forEach { option ->
                            OutlinedButton(modifier = Modifier.fillMaxWidth(),
                                onClick = { wrapWidth = option }) {
                                Text((if (wrapWidth == option) "✓ " else "") +
                                    "$option tekens")
                            }
                        }
                    }
                    Text(if (proposed == null)
                        "Dit voorstel overschrijdt de veilige documentgrenzen."
                        else "${proposed.changedCues} regels aangepast, " +
                            "${proposed.addedBreaks} nieuwe regelafbrekingen. " +
                            "${proposed.remainingLongCues} regels blijven langer " +
                            "dan de gekozen breedte (bijvoorbeeld lange woorden " +
                            "of de limiet van acht regels per cue).",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("Alleen dit tijdelijke concept verandert. Terug/Opnieuw " +
                        "blijven werken; uitsluitend Bewaar kopie… schrijft " +
                        "later naar een door jou gekozen bestandsprovider.",
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(enabled = proposed != null && proposed.changedCues > 0,
                    onClick = {
                        val confirmed = PrivateSubtitleLineWrap.preview(draft, wrapWidth)
                        if (confirmed != null && confirmed.changedCues > 0) {
                            applyChange(confirmed.cues)
                            shownIndex = -1
                            notice = "${confirmed.addedBreaks} afbrekingen toegevoegd " +
                                "aan ${confirmed.changedCues} regels in tijdelijk concept."
                        }
                        wrapOpen = false
                    }) { Text("Pas afbrekingen tijdelijk toe") }
            },
            dismissButton = {
                TextButton(onClick = { wrapOpen = false }) { Text("Annuleren") }
            }
        )
    }
    if (removeOpen) {
        // Original 1-based draft rows, never the filtered list's positions.
        LaunchedEffect(draft, removeRows) { removeReviewPage = 0 }
        val proposed = PrivateSubtitleBulkRemoval.preview(draft, removeRows)
        val removed = proposed?.removedRows.orEmpty()
        val countPages = maxOf(1, (removed.size + 4) / 5)
        val safePage = removeReviewPage.coerceIn(0, countPages - 1)
        AlertDialog(
            onDismissRequest = { removeOpen = false },
            title = { Text("Regels verwijderen · alleen concept") },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 530.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Geef originele regelnummers op, bijvoorbeeld 2-4,7,12. " +
                        "De zoekbalk boven dit venster verandert de nummers niet. " +
                        "Overlappende reeksen of de hele tekst wissen worden geweigerd.",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = removeRows,
                        onValueChange = { removeRows = it.take(2000) },
                        label = { Text("Verwijder bronregels: 2-4,7,12") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true)
                    val foundExpression = if (query.isNotBlank() && found.isNotEmpty())
                        PrivateSubtitleRowSelection.formatSourceRows(
                            found.map { it.first + 1 }, draft.size)
                    else null
                    if (foundExpression != null) {
                        OutlinedButton(
                            onClick = { removeRows = foundExpression },
                            modifier = Modifier.fillMaxWidth()) {
                            Text("Neem ${found.size} gevonden regels als verwijderselectie")
                        }
                    }
                    Text("Of neem bewust volledige regels uit een tijdvenster. " +
                        "Hun oorspronkelijke tijdcodes blijven onverkort " +
                        "zichtbaar vóór verwijdering.",
                        style = MaterialTheme.typography.bodySmall)
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = removeWindowStart,
                            onValueChange = { removeWindowStart = it.take(13) },
                            label = { Text("Vanaf mm:ss") },
                            modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(
                            value = removeWindowEnd,
                            onValueChange = { removeWindowEnd = it.take(13) },
                            label = { Text("Tot mm:ss") },
                            modifier = Modifier.weight(1f), singleLine = true)
                    }
                    val timeSelection = PrivateSubtitleTimeWindowRows.expression(
                        draft, removeWindowStart, removeWindowEnd)
                    OutlinedButton(modifier = Modifier.fillMaxWidth(),
                        enabled = timeSelection != null,
                        onClick = {
                            if (timeSelection != null) {
                                removeRows = timeSelection
                                removeReviewPage = 0
                            }
                        }) { Text("Gebruik overlappende tijdvensterregels") }

                    Text("Of kies maximaal 16 losse tijdvensters zonder overlap. " +
                        "Een cue die twee vensters raakt, wordt één keer geselecteerd.",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = removeWindowRanges,
                        onValueChange = { removeWindowRanges = it.take(512) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Verwijderselectie: 00:10-00:20;01:00-01:15") })
                    val windowSet = PrivateSubtitleTimeWindowRows.expressionForWindows(
                        draft, removeWindowRanges)
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = windowSet != null,
                        onClick = {
                            if (windowSet != null) {
                                removeRows = windowSet
                                removeReviewPage = 0
                            }
                        }) { Text("Neem alle tijdvensters als bronregelselectie") }
                    if (proposed == null) {
                        Text("Geen geldige verwijdering. Controleer regelnummers " +
                            "(1–${draft.size}), dubbelen en behoud minstens één regel.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("Voorbeeld: ${removed.size} regels verdwijnen, " +
                            "${proposed.remaining.size} blijven met bestaande " +
                            "tijden en tekst behouden.",
                            style = MaterialTheme.typography.titleSmall)
                        Text("Te verwijderen: pagina ${safePage + 1}/$countPages. " +
                            "Ieder getoond nummer verwijst naar de originele " +
                            "regel vóór deze actie.",
                            style = MaterialTheme.typography.bodySmall)
                        removed.drop(safePage * 5).take(5).forEach { originalRow ->
                            val cue = draft[originalRow - 1]
                            Text("Regel $originalRow · ${cue.startLabel} → ${cue.endLabel}\n" +
                                cue.text, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(enabled = safePage > 0,
                                onClick = { removeReviewPage = safePage - 1 }) {
                                Text("← Vorige")
                            }
                            TextButton(enabled = safePage < countPages - 1,
                                onClick = { removeReviewPage = safePage + 1 }) {
                                Text("Volgende →")
                            }
                        }
                    }
                    Text("Alleen je tijdelijke kopie verandert, als één " +
                        "herstelbare Terug/Opnieuw-stap. Opslaan gebeurt " +
                        "apart na je bewuste Android-bestandskeuze.",
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(enabled = proposed != null, onClick = {
                    val confirmed = PrivateSubtitleBulkRemoval.preview(draft, removeRows)
                    if (confirmed == null || proposed == null ||
                        confirmed.removedRows != proposed.removedRows ||
                        confirmed.remaining != proposed.remaining) {
                        notice = "Concept of selectie gewijzigd: open de preview opnieuw."
                    } else {
                        applyChange(confirmed.remaining)
                        shownIndex = -1
                        notice = "${confirmed.removedRows.size} regels verwijderd " +
                            "uit tijdelijke kopie; Terug maakt dit ongedaan."
                    }
                    removeOpen = false
                }) { Text("Verwijder gekozen regels tijdelijk") }
            },
            dismissButton = {
                TextButton(onClick = { removeOpen = false }) { Text("Annuleren") }
            }
        )
    }
    if (batchOpen) {
        // A changed search, translation line, or selected range always starts
        // review at the first actual changed cue, not an old preview page.
        LaunchedEffect(draft, batchFind, batchWith, batchMatchCase,
            batchField, batchFirstRow, batchLastRow, batchExplicitRows) {
            batchReviewPage = 0
        }
        val scopeFrom = batchFirstRow.toIntOrNull()
        val scopeTo = if (batchLastRow.isBlank()) draft.size
            else batchLastRow.toIntOrNull()
        val explicitBatch = batchExplicitRows.isNotBlank()
        val proposed = if (explicitBatch || (scopeFrom != null && scopeTo != null))
            PrivateSubtitleBatchReplace.preview(
                draft, batchFind, batchWith, batchMatchCase,
                if (explicitBatch) 1 else scopeFrom!!,
                if (explicitBatch) draft.size else scopeTo!!,
                batchField, batchExplicitRows)
        else null
        val changes = proposed?.updated?.withIndex()?.filter { (i, cue) ->
            cue.text != draft[i].text
        }.orEmpty()
        val reviewPageCount = maxOf(1, (changes.size + 4) / 5)
        val reviewPage = batchReviewPage.coerceIn(0, reviewPageCount - 1)
        AlertDialog(
            onDismissRequest = { batchOpen = false },
            title = { Text("Zoek en vervang · tijdelijk") },
            text = {
                Column(modifier = Modifier.heightIn(max = 530.dp)
                    .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Letterlijke tekst binnen het gekozen regelbereik. " +
                        "Kies ook welk tekstdeel wordt aangepast; tijden blijven gelijk.",
                        style = MaterialTheme.typography.bodySmall)
                    Text("Tekstdeel:", style = MaterialTheme.typography.labelMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        listOf(
                            SubtitleReplaceField.ALL_TEXT to "Alle tekstregels",
                            SubtitleReplaceField.FIRST_LINE to "Alleen eerste regel (bron)",
                            SubtitleReplaceField.SECOND_LINE to "Alleen tweede regel (eigen betekenis)"
                        ).forEach { (option, label) ->
                            OutlinedButton(modifier = Modifier.fillMaxWidth(),
                                onClick = { batchField = option }) {
                                Text((if (batchField == option) "✓ " else "") + label)
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = batchFirstRow,
                            onValueChange = { batchFirstRow =
                                it.filter { c -> c in '0'..'9' }.take(4) },
                            label = { Text("Van regel") },
                            singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(
                            value = batchLastRow,
                            onValueChange = { batchLastRow =
                                it.filter { c -> c in '0'..'9' }.take(4) },
                            label = { Text("Tot (leeg = alle)") },
                            singleLine = true, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(
                        value = batchExplicitRows,
                        onValueChange = { batchExplicitRows = it.take(2000) },
                        label = { Text("Losse bronregels (optioneel): 1-3,7,12") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true)
                    Text("Met losse bronregels worden Van/Tot tijdelijk genegeerd. " +
                        "Zonder losse bronregels geldt alleen Van/Tot. " +
                        "Overlapping en onbestaande nummers zijn ongeldig.",
                        style = MaterialTheme.typography.bodySmall)
                    val batchFoundExpression =
                        if (query.isNotBlank() && found.isNotEmpty())
                            PrivateSubtitleRowSelection.formatSourceRows(
                                found.map { it.first + 1 }, draft.size)
                        else null
                    if (batchFoundExpression != null) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { batchExplicitRows = batchFoundExpression }
                        ) { Text("Kies ${found.size} gevonden bronregels als bereik") }
                    }
                    Text("Of kies bewust alle complete cues die een tijdvenster " +
                        "overlappen als bereik voor deze correctie.",
                        style = MaterialTheme.typography.bodySmall)
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = batchWindowStart,
                            onValueChange = { batchWindowStart = it.take(13) },
                            label = { Text("Vanaf mm:ss") },
                            modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = batchWindowEnd,
                            onValueChange = { batchWindowEnd = it.take(13) },
                            label = { Text("Tot mm:ss") },
                            modifier = Modifier.weight(1f), singleLine = true)
                    }
                    val windowBatchRows = PrivateSubtitleTimeWindowRows.expression(
                        draft, batchWindowStart, batchWindowEnd)
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = windowBatchRows != null,
                        onClick = {
                            if (windowBatchRows != null)
                                batchExplicitRows = windowBatchRows
                        }) { Text("Kies overlappende tijdvensterregels") }

                    Text("Of kies maximaal 16 losse tijdvensters zonder overlap. " +
                        "Een cue die twee vensters raakt, wordt één keer geselecteerd.",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = batchWindowRanges,
                        onValueChange = { batchWindowRanges = it.take(512) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Correctiebereik: 00:10-00:20;01:00-01:15") })
                    val batchWindowSet = PrivateSubtitleTimeWindowRows.expressionForWindows(
                        draft, batchWindowRanges)
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = batchWindowSet != null,
                        onClick = {
                            if (batchWindowSet != null) {
                                batchExplicitRows = batchWindowSet
                                batchReviewPage = 0
                            }
                        }) { Text("Neem alle tijdvensters als bronregelselectie") }
                    OutlinedTextField(
                        value = batchFind,
                        onValueChange = { batchFind = it.take(100) },
                        label = { Text("Zoek tekst · max. 100 tekens") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = batchWith,
                        onValueChange = { batchWith = it.take(200) },
                        label = { Text("Vervang door · leeg mag") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = batchMatchCase,
                            onCheckedChange = { batchMatchCase = it })
                        Text("Hoofdletters onderscheiden")
                    }
                    Text(
                        if (proposed == null)
                            "Geen geldige wijziging; controleer zoekterm, " +
                                "regelgrenzen en documentlimieten."
                        else "Voorbeeld: ${proposed.replacements} vervangingen " +
                            "in ${proposed.changedCues} regels binnen " +
                            (if (explicitBatch) "losse bronregels $batchExplicitRows."
                             else "${scopeFrom}–${scopeTo}."),

                        style = MaterialTheme.typography.bodyMedium)
                    if (changes.isNotEmpty()) {
                        Text("Previewpagina ${reviewPage + 1} van ${reviewPageCount} " +
                            "· ${changes.size} veranderde regels (volledige tekst)",
                            style = MaterialTheme.typography.labelMedium)
                        changes.drop(reviewPage * 5).take(5).forEach { (index, cue) ->
                            Text("Regel ${index + 1}:\n" +
                                "VOOR:\n${draft[index].text}\n" +
                                "NA:\n${cue.text}",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(enabled = reviewPage > 0, onClick = {
                                batchReviewPage = reviewPage - 1
                            }) { Text("← Vorige") }
                            TextButton(enabled = reviewPage < reviewPageCount - 1,
                                onClick = { batchReviewPage = reviewPage + 1 }) {
                                Text("Volgende →")
                            }
                        }
                    }
                    Text("De bron blijft ongewijzigd. Alleen via Bewaar kopie… " +
                        "kan je later een nieuw SRT/VTT-bestand maken.",
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(enabled = proposed != null, onClick = {
                    val confirmedFrom = batchFirstRow.toIntOrNull()
                    val confirmedTo = if (batchLastRow.isBlank()) draft.size
                        else batchLastRow.toIntOrNull()
                    val explicitConfirmed = batchExplicitRows.isNotBlank()
                    val confirmed = if (explicitConfirmed ||
                        (confirmedFrom != null && confirmedTo != null))
                        PrivateSubtitleBatchReplace.preview(
                            draft, batchFind, batchWith, batchMatchCase,
                            if (explicitConfirmed) 1 else confirmedFrom!!,
                            if (explicitConfirmed) draft.size else confirmedTo!!,
                            batchField, batchExplicitRows)
                    else null
                    if (confirmed != null) {
                        applyChange(confirmed.updated)
                        shownIndex = -1
                        notice = "${confirmed.replacements} tekstvervangingen toegepast " +
                            "in ${confirmed.changedCues} regels. Herstel blijft mogelijk."
                    }
                    batchOpen = false
                }) { Text("Pas toe in tijdelijk concept") }
            },
            dismissButton = {
                TextButton(onClick = { batchOpen = false }) { Text("Annuleren") }
            }
        )
    }
    val splitting = splitIndex
    if (splitting != null) {
        AlertDialog(onDismissRequest = { splitIndex = null },
            title = { Text("Splits één ondertitel in twee") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Kies zelf twee zinsdelen. De bestaande tijdsduur " +
                        "wordt gelijk gedeeld; dit is geen gecontroleerde audiotiming.",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = splitFirst,
                        onValueChange = { if (it.length <= 2400) splitFirst = it },
                        label = { Text("Eerste deel") }, modifier = Modifier.fillMaxWidth(),
                        minLines = 2, maxLines = 4)
                    OutlinedTextField(
                        value = splitSecond,
                        onValueChange = { if (it.length <= 2400) splitSecond = it },
                        label = { Text("Tweede deel") }, modifier = Modifier.fillMaxWidth(),
                        minLines = 2, maxLines = 4)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val next = PrivateSubtitleDraftEditor.splitCue(
                        draft, splitting, splitFirst.trim(), splitSecond.trim())
                    if (next == null) {
                        notice = "Splitsing geweigerd: tekst/tijdsduur of cuevolgorde ongeldig."
                    } else {
                        applyChange(next)
                        shownIndex = -1
                        notice = "Regel in twee bewerkbare regels gesplitst."
                    }
                    splitIndex = null
                }) { Text("Splits tijdelijk") }
            },
            dismissButton = {
                TextButton(onClick = { splitIndex = null }) { Text("Annuleren") }
            }
        )
    }
    val merging = mergeIndex
    if (merging != null) {
        AlertDialog(onDismissRequest = { mergeIndex = null },
            title = { Text("Volgende ondertitel samenvoegen?") },
            text = { Text("De twee teksten worden samengevoegd in één cue, " +
                "van het eerste begin tot het tweede einde. Dit is alleen " +
                "een wijziging in je tijdelijke document, geen audio-uitlijning. " +
                "Herstel zet het oorspronkelijke bestand terug.") },
            confirmButton = {
                TextButton(onClick = {
                    val next = PrivateSubtitleDraftEditor.mergeWithNext(draft, merging)
                    if (next == null) notice = "Samenvoegen geweigerd: te veel tekst of ongeldige tijd."
                    else {
                        applyChange(next)
                        shownIndex = -1
                        notice = "Beide regels tot één tijdelijke cue samengevoegd."
                    }
                    mergeIndex = null
                }) { Text("Voeg samen") }
            },
            dismissButton = {
                TextButton(onClick = { mergeIndex = null }) { Text("Annuleren") }
            }
        )
    }
    val requestedDelete = deleteIndex
    if (requestedDelete != null) {
        AlertDialog(onDismissRequest = { deleteIndex = null },
            title = { Text("Deze ondertitelregel verwijderen?") },
            text = { Text("Alleen de tijdelijke documentkopie verandert. " +
                "Het originele bestand blijft ongewijzigd; Herstel kan dit terugdraaien.") },
            confirmButton = {
                TextButton(onClick = {
                    val newDraft = PrivateSubtitleDraftEditor.dropCue(draft, requestedDelete)
                    if (newDraft != null) {
                        applyChange(newDraft)
                        shownIndex = -1
                        notice = "Regel verwijderd uit tijdelijke kopie."
                    } else notice = "De laatste regel kan niet worden verwijderd."
                    deleteIndex = null
                }) { Text("Wis alleen deze regel") }
            },
            dismissButton = {
                TextButton(onClick = { deleteIndex = null }) { Text("Annuleren") }
            }
        )
    }
    val timedIndex = exactTimeIndex
    if (timedIndex != null) {
        SubtitleExactCueTimingDialog(
            draft = draft,
            selectedIndex = timedIndex,
            onApply = { updated ->
                applyChange(updated)
                shownIndex = -1
                notice = "Exacte ondertiteltijd aangepast; wijziging is herstelbaar."
                exactTimeIndex = null
            },
            onDismiss = { exactTimeIndex = null }
        )
    }

    val position = editIndex
    if (position != null) {
        AlertDialog(onDismissRequest = { editIndex = null },
            title = { Text("Handmatige tekstcorrectie") },
            text = {
                Column(modifier = Modifier.heightIn(max = 530.dp)
                    .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("De tijdcode blijft gelijk. Tuddy controleert of vertaalt " +
                        "de nieuwe tekst niet automatisch.",
                        style = MaterialTheme.typography.bodySmall)
                    if (editAsPairedLines) {
                        Text("Deze kaart heeft precies twee tekstregels. Je kunt " +
                            "beide handmatig en apart aanpassen. Tuddy bepaalt niet " +
                            "of de eerste regel daadwerkelijk een brontaal is.",
                            style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(value = editSourceLine,
                            onValueChange = {
                                if (it.length <= 800 && '\n' !in it && '\r' !in it)
                                    editSourceLine = it
                            },
                            label = { Text("Eerste tekstregel (eigen bron)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2, maxLines = 4,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                textDirection = TextDirection.ContentOrRtl))
                        OutlinedTextField(value = editMeaningLine,
                            onValueChange = {
                                if (it.length <= 800 && '\n' !in it && '\r' !in it)
                                    editMeaningLine = it
                            },
                            label = { Text("Tweede tekstregel (eigen betekenis)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2, maxLines = 4,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                textDirection = TextDirection.ContentOrRtl))
                    } else {
                        OutlinedTextField(value = editValue,
                            onValueChange = { if (it.length <= 2400) editValue = it },
                            label = { Text("Ondertiteltekst") },
                            modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 8,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                textDirection = TextDirection.ContentOrRtl))
                    }
                    if (editError.isNotBlank()) {
                        Text(editError, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val updatedText = if (editAsPairedLines)
                        editSourceLine + "\n" + editMeaningLine else editValue
                    val next = PrivateSubtitleDraftEditor.revise(draft, position, updatedText)
                    if (next == null) {
                        editError = "Ongeldige tekst: geen lege regels, " +
                            "voor- of achterliggende spaties of controlecodes. " +
                            "Maximaal 800 tekens per regel; corrigeer je invoer."
                    } else {
                        applyChange(next)
                        notice = "Tekst aangepast in tijdelijk concept."
                        shownIndex = -1
                        editError = ""
                        editIndex = null
                    }
                }) { Text("Gebruik correctie") }
            },
            dismissButton = {
                TextButton(onClick = { editIndex = null }) { Text("Annuleren") }
            }
        )
    }


    if (requestLrcSave) {
        val exactLrc = PrivateLrcSubtitleExport.snapshot(draft)
        AlertDialog(
            onDismissRequest = { requestLrcSave = false },
            title = { Text("Eigen begintijden als LRC bewaren?") },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("LRC bevat alleen de begintijd van één eigen tekstregel. " +
                        "Eindtijden, overlap en woordmomenten passen niet in LRC " +
                        "en worden NIET meegenomen. Voor exacte eindtijden " +
                        "gebruik je SRT, VTT, CSV of TSV.",
                        style = MaterialTheme.typography.bodySmall)
                    if (exactLrc == null) {
                        Text("Geen verliesvrije LRC-tekstregels mogelijk: gebruik " +
                            "uitsluitend enkelregelige cues met strikt oplopende " +
                            "begintijden, maximaal 999 minuten en 500 regels. " +
                            "Het bestaande concept blijft ongewijzigd.",
                            color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("${draft.size} eigen regels · ${exactLrc.size} UTF-8 bytes. " +
                            "Werkelijke LRC-inhoud, volledig op deelpagina's:",
                            style = MaterialTheme.typography.titleSmall)
                        PrivateSubtitlePagedExportPreview(exactLrc)
                    }
                    Text("Deze preview past niets toe op het live gesprek. " +
                        "Alleen na jouw volgende expliciete keuze van een " +
                        "Android-bestandsprovider wordt een nieuwe kopie " +
                        "geschreven; die provider kan zelf extern synchroniseren.",
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(enabled = exactLrc != null, onClick = {
                    val fresh = PrivateLrcSubtitleExport.snapshot(draft)
                    if (exactLrc == null || fresh == null ||
                        !fresh.contentEquals(exactLrc)) {
                        notice = "Concept is veranderd: open de LRC-preview opnieuw."
                    } else onSaveLrc(draft.toList(), draft.toList())
                    requestLrcSave = false
                }) { Text("Kies opslaglocatie…") }
            },
            dismissButton = {
                TextButton(onClick = { requestLrcSave = false }) { Text("Annuleren") }
            }
        )
    }
    val chosenTimedSheet = requestTimedSheet
    if (chosenTimedSheet != null) {
        val snapshot = PrivateTimedSubtitleSheet.snapshot(draft, chosenTimedSheet)
        val columns = PrivateTimedSubtitleSheet.columnCount(draft)
        AlertDialog(
            onDismissRequest = { requestTimedSheet = null },
            title = { Text("Bewerkbare ondertiteltijdcodes als ${chosenTimedSheet.name}") },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Geen verborgen herindeling: alle cues moeten één eigen " +
                        "tekstregel (3 kolommen) of allemaal precies twee eigen " +
                        "tekstregels (4 kolommen) hebben. " +
                        "Gemengde en meerregelige cues worden niet ingekort; " +
                        "bewaar die als SRT/VTT. Er wordt geen betekenis vertaald.",
                        style = MaterialTheme.typography.bodySmall)
                    if (snapshot == null) {
                        Text("CSV/TSV niet beschikbaar: controleer de gelijke " +
                            "kolomindeling, de maximale documentgrootte en " +
                            "teksten die als spreadsheetformules zouden kunnen " +
                            "worden uitgevoerd. Je oorspronkelijke concept is " +
                            "ongewijzigd en SRT/VTT-opslag blijft beschikbaar.",
                            color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("${draft.size} originele cues · ${columns ?: 0} kolommen · " +
                            "${snapshot.size} UTF-8 bytes. De volledige werkelijke " +
                            "bestandsinhoud is te bekijken per pagina.",
                            style = MaterialTheme.typography.titleSmall)
                        PrivateSubtitlePagedExportPreview(snapshot)
                    }
                    Text("Je kiest zelf een Android-bestandsprovider; die kan " +
                        "extern synchroniseren. Origineel en live gesprek " +
                        "worden niet gewijzigd. Controleer deze exacte preview " +
                        "voordat je apart kiest om een kopie op te slaan.",
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(enabled = snapshot != null, onClick = {
                    val current = PrivateTimedSubtitleSheet.snapshot(draft, chosenTimedSheet)
                    if (snapshot == null || current == null ||
                        !current.contentEquals(snapshot)) {
                        notice = "De tijdlijn veranderde: open CSV/TSV-preview opnieuw."
                    } else {
                        onSaveTimedSheet(draft.toList(), draft.toList(), chosenTimedSheet)
                    }
                    requestTimedSheet = null
                }) { Text("Kies opslaglocatie…") }
            },
            dismissButton = {
                TextButton(onClick = { requestTimedSheet = null }) { Text("Annuleren") }
            }
        )
    }
    val chosenFormat = requestSave
    if (chosenFormat != null) {
        val exactSnapshot = PrivateSubtitleDraftEditor.snapshot(draft, chosenFormat)
        AlertDialog(onDismissRequest = { requestSave = null },
            title = { Text("Nieuw ${chosenFormat.name}-bestand controleren") },
            text = {
                Column(modifier = Modifier.heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Controleer alle originele tekstregels en tijdcodes " +
                        "voordat je een nieuw eigen ondertitelbestand opslaat. " +
                        "Dit is geen automatische audio-uitlijning of vertaling.",
                        style = MaterialTheme.typography.bodySmall)
                    if (exactSnapshot == null) {
                        Text("SRT/VTT niet beschikbaar: het huidige concept " +
                            "overschrijdt de bestaande grenzen of bevat ongeldige " +
                            "tijdcodes/tekst. Het originele document blijft intact.",
                            color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("${draft.size} ondertitels · ${exactSnapshot.size} " +
                            "UTF-8 bytes. De volledige exacte bestandsinhoud " +
                            "is per deelpagina te bekijken.",
                            style = MaterialTheme.typography.titleSmall)
                        PrivateSubtitlePagedExportPreview(exactSnapshot)
                    }
                    Text("Het live gesprek en eerder geopende bestand blijven " +
                        "ongewijzigd. Alleen na jouw keuze wordt een nieuwe " +
                        "kopie bij een Android-bestandsprovider geschreven. " +
                        "Die provider kan zelf extern synchroniseren.",
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(enabled = exactSnapshot != null, onClick = {
                    val current = PrivateSubtitleDraftEditor.snapshot(draft, chosenFormat)
                    if (exactSnapshot == null || current == null ||
                        !current.contentEquals(exactSnapshot)) {
                        notice = "Concept veranderd: controleer de SRT/VTT-preview opnieuw."
                    } else onSaveCopy(draft.toList(), draft.toList(), chosenFormat)
                    requestSave = null
                }) { Text("Kies opslaglocatie…") }
            },
            dismissButton = {
                TextButton(onClick = { requestSave = null }) { Text("Annuleren") }
            }
        )
    }
}
