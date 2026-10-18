package com.livelingo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.livelingo.android.PrivateSubtitleLineWrap
import com.livelingo.android.PrivateSubtitleClip
import com.livelingo.android.PrivateSubtitleCue
import com.livelingo.android.PrivateSubtitleDocumentCombine
import com.livelingo.android.SubtitleCombineMode
import com.livelingo.android.PrivateSubtitleDraftEditor
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
    onSaveCopy: (List<PrivateSubtitleCue>, SubtitleDraftFormat) -> Unit
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
    var requestSave by remember { mutableStateOf<SubtitleDraftFormat?>(null) }
    var notice by remember { mutableStateOf("") }
    var batchOpen by remember { mutableStateOf(false) }
    var wrapOpen by remember { mutableStateOf(false) }
    var wrapWidth by remember { mutableIntStateOf(42) }
    var batchFind by remember { mutableStateOf("") }
    var batchWith by remember { mutableStateOf("") }
    var batchMatchCase by remember { mutableStateOf(false) }
    var insertOpen by remember { mutableStateOf(false) }
    var insertStart by remember { mutableStateOf("") }
    var insertEnd by remember { mutableStateOf("") }
    var insertContent by remember { mutableStateOf("") }
    var clipOpen by remember { mutableStateOf(false) }
    var clipStart by remember { mutableStateOf("") }
    var clipEnd by remember { mutableStateOf("") }
    var excerptOpen by remember { mutableStateOf(false) }
    var excerptCues by remember { mutableStateOf<List<PrivateSubtitleCue>>(emptyList()) }
    var practiceOpen by remember { mutableStateOf(false) }
    var retimingOpen by remember { mutableStateOf(false) }
    var inspectorOpen by remember { mutableStateOf(false) }
    var twoAnchorOpen by remember { mutableStateOf(false) }
    var practiceCues by remember { mutableStateOf<List<PrivateSubtitleCue>>(emptyList()) }
    var pendingImport by remember { mutableStateOf<PendingSubtitleImport?>(null) }
    var importMode by remember { mutableStateOf(SubtitleCombineMode.APPEND_AFTER_LAST) }
    var secondTrackOffsetMs by remember { mutableStateOf(0L) }
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
                    enabled = query.isNotBlank() && found.isNotEmpty(),
                    onClick = {
                        excerptCues = found.map { it.second }.toList()
                        excerptOpen = true
                    }
                ) { Text("↳ Bewaar alleen gevonden regels als SRT/VTT…") }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { retimingOpen = true }
                ) { Text("⏱ Tijdcodes van alle regels aanpassen…") }
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
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = { requestSave = SubtitleDraftFormat.SRT }) {
                        Text("Bewaar kopie SRT…")
                    }
                    OutlinedButton(onClick = { requestSave = SubtitleDraftFormat.WEBVTT }) {
                        Text("Bewaar kopie VTT…")
                    }
                }
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
                            editIndex = found[shownIndex].first
                            editValue = current.text
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
            selected = excerptCues,
            onSaveCopy = onSaveCopy,
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

    val stagedImport = pendingImport
    if (stagedImport != null) {
        val stillCurrent = draft == stagedImport.existing
        val proposed = if (stillCurrent)
            PrivateSubtitleDocumentCombine.preview(
                draft, stagedImport.imported, importMode, secondTrackOffsetMs
            ) else null
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Tweede ondertitelbestand toevoegen") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(
                        "Tweede bestand: ${stagedImport.imported.size} ondertitelregels. " +
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
                        Box {
                            OutlinedButton(onClick = { offsetChoicesOpen = true }) {
                                Text("Tijdverschuiving tweede spoor: " +
                                    (if (secondTrackOffsetMs > 0L) "+" else "") +
                                    "${secondTrackOffsetMs / 1000} seconden ▾")
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
                                            offsetChoicesOpen = false
                                        })
                                }
                            }
                        }
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
                    Text(
                        if (!stillCurrent)
                            "Het huidige concept is veranderd: voeg opnieuw toe."
                        else if (proposed == null)
                            "Samenvoegen kan niet: maximaal 500 regels, geldige " +
                                "tijdcodes en maximaal 256 KiB per export."
                        else if (importMode == SubtitleCombineMode.PAIR_OVERLAPPING_TIMES)
                            "${proposed.pairedCount} regelparen met gedeelde tijdcodes, " +
                                "${proposed.unpairedSecondTrack} regels uit tweede bestand " +
                                "apart gehouden, ${proposed.cues.size} regels in resultaat. " +
                                "Beoordeel eerst tekst en timing; er is geen " +
                                "audio-/video-uitlijning."
                        else "${proposed.cues.size} regels in de gecombineerde versie. " +
                            "${proposed.adjacentOverlaps} aangrenzende tijdsoverlappingen. " +
                            "Controleer tijdcodes zelf; er is geen audio-/video-uitlijning.",
                        style = MaterialTheme.typography.bodySmall
                    )
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
                    val updated = PrivateSubtitleDocumentCombine.preview(
                        draft, stagedImport.imported, importMode, secondTrackOffsetMs
                    )
                    if (draft != stagedImport.existing || updated == null) {
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
    if (batchOpen) {
        val proposed = PrivateSubtitleBatchReplace.preview(
            draft, batchFind, batchWith, batchMatchCase)
        AlertDialog(
            onDismissRequest = { batchOpen = false },
            title = { Text("Zoek en vervang · tijdelijk") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Letterlijke tekst in alle ondertitelregels; tijdcodes " +
                        "blijven ongewijzigd. Bekijk het aantal vóór je toepast.",
                        style = MaterialTheme.typography.bodySmall)
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
                            "in ${proposed.changedCues} regels.",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("De bron blijft ongewijzigd. Alleen via Bewaar kopie… " +
                        "kan je later een nieuw SRT/VTT-bestand maken.",
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(enabled = proposed != null, onClick = {
                    val confirmed = PrivateSubtitleBatchReplace.preview(
                        draft, batchFind, batchWith, batchMatchCase)
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("De tijdcode blijft gelijk. Tuddy controleert of vertaalt " +
                        "de nieuwe tekst niet automatisch.",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(value = editValue,
                        onValueChange = { if (it.length <= 2400) editValue = it },
                        label = { Text("Ondertiteltekst") },
                        modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 8,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = TextDirection.ContentOrRtl))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val next = PrivateSubtitleDraftEditor.revise(draft, position, editValue)
                    if (next == null) {
                        notice = "Ongeldige tekst. Maximaal acht regels, zonder controlecodes."
                    } else {
                        applyChange(next)
                        notice = "Tekst aangepast in tijdelijk concept."
                        shownIndex = -1
                    }
                    editIndex = null
                }) { Text("Gebruik correctie") }
            },
            dismissButton = {
                TextButton(onClick = { editIndex = null }) { Text("Annuleren") }
            }
        )
    }
    val chosenFormat = requestSave
    if (chosenFormat != null) {
        AlertDialog(onDismissRequest = { requestSave = null },
            title = { Text("Nieuw ondertitelbestand bewaren?") },
            text = { Text("Je kiest bewust een Android-bestandsprovider. " +
                "Die kan jouw tekst buiten Tuddy synchroniseren. Tijdcodes zijn " +
                "handmatige schattingen en teksten zijn niet geverifieerd. " +
                "Het eerder geopende bestand blijft ongewijzigd.") },
            confirmButton = {
                TextButton(onClick = {
                    requestSave = null
                    if (PrivateSubtitleDraftEditor.snapshot(draft, chosenFormat) == null)
                        notice = "Ongeldig concept: kan geen ondertitelbestand maken."
                    else onSaveCopy(draft.toList(), chosenFormat)
                }) { Text("Kies opslaglocatie…") }
            },
            dismissButton = {
                TextButton(onClick = { requestSave = null }) { Text("Annuleren") }
            }
        )
    }
}
